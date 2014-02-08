/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

package org.sonatype.nexus.analytics.internal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.analytics.Anonymizer;
import org.sonatype.nexus.analytics.EventData;
import org.sonatype.nexus.analytics.EventExporter;
import org.sonatype.nexus.analytics.EventStore;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.kazuki.v0.store.journal.JournalStore;
import io.kazuki.v0.store.journal.PartitionInfo;
import io.kazuki.v0.store.journal.PartitionInfoSnapshot;
import org.apache.commons.lang.time.StopWatch;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Default {@link EventExporter} implementation.
 *
 * @since 2.8
 */
@Named
@Singleton
public class EventExporterImpl
    extends ComponentSupport
    implements EventExporter
{
  private final EventStoreImpl eventStore;

  private final Anonymizer anonymizer;

  private final ReentrantLock exportLock = new ReentrantLock();

  @Inject
  public EventExporterImpl(final EventStoreImpl eventStore,
                           final Anonymizer anonymizer)
  {
    this.eventStore = checkNotNull(eventStore);
    this.anonymizer = checkNotNull(anonymizer);
  }

  /**
   * @throws IllegalStateException If an export is already in progress.
   */
  @Override
  public File export(final boolean dropAfterExport) throws Exception {
    try {
      checkState(exportLock.tryLock(), "Export already in progress");
      return doExport(dropAfterExport);
    }
    finally {
      if (exportLock.isHeldByCurrentThread()) {
        exportLock.unlock();
      }
    }
  }

  /**
   * Helper to anonymize sensitive event data.
   */
  private class AnonymizerHelper
  {
    private final Cache<String, String> cache = CacheBuilder.newBuilder()
        .maximumSize(200)
        .build();

    public EventData anonymize(final EventData event) {
      event.setUserId(anonymize(event.getUserId()));
      event.setSessionId(anonymize(event.getSessionId()));
      return event;
    }

    private String anonymize(final String text) {
      if (text != null) {
        String result = cache.getIfPresent(text);
        if (result == null) {
          result = anonymizer.anonymize(text);
          cache.put(text, result);
        }
        return result;
      }
      return null;
    }
  }

  private File doExport(final boolean dropAfterExport) throws Exception {
    StopWatch watch = new StopWatch();
    watch.start();

    JournalStore journal = eventStore.getJournalStore();

    // Close the current partition, so that any new events are separate from those that exist already
    journal.closeActivePartition();

    // TODO: Sort out max for each zip file
    File file = File.createTempFile("analytics-", ".zip");
    log.info("Exporting to: {}", file);

    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    JsonFactory jsonFactory = mapper.getFactory();

    AnonymizerHelper anonymizerHelper = new AnonymizerHelper();

    int i = 0;
    try (ZipOutputStream output = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
      // TODO: Write out a EventHeader to header.json

      // write each partition to its own file in the zip
      Iterator<PartitionInfoSnapshot> partitions = journal.getAllPartitions();
      while (partitions.hasNext()) {
        PartitionInfo partition = partitions.next();
        if (!partition.isClosed()) {
          // skip new open partitions, this is new data _after_ the export was requested
          break;
        }

        // new entry in the zip for each partition
        ZipEntry entry = new ZipEntry("events-" + i++ + ".json");
        output.putNextEntry(entry);
        log.info("Writing entry: {}, partition: {}", entry.getName(), partition.getPartitionId());

        JsonGenerator generator = jsonFactory.createGenerator(output);
        generator.writeStartArray();

        Iterator<EventData> events = journal.getIteratorRelative(
            EventStore.SCHEMA_NAME, EventData.class, 0L, partition.getSize());

        while (events.hasNext()) {
          generator.writeObject(anonymizerHelper.anonymize(events.next()));
        }

        if (dropAfterExport) {
          journal.dropPartition(partition.getPartitionId());
        }

        generator.writeEndArray();
        generator.flush();
        output.closeEntry();
      }
    }
    // TODO: Move file to support dir

    log.info("Exported {} partitions to: {}, took: {}", i, file, watch);
    return file;
  }
}
