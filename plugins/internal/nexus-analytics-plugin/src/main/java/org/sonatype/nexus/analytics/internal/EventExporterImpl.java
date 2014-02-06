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
import io.kazuki.v0.store.journal.JournalStore;
import io.kazuki.v0.store.journal.PartitionInfo;
import io.kazuki.v0.store.journal.PartitionInfoSnapshot;

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
      checkState(exportLock.tryLock(), "Already locked for export");
      return doExport(dropAfterExport);
    }
    finally {
      if (exportLock.isHeldByCurrentThread()) {
        exportLock.unlock();
      }
    }
  }

  private File doExport(final boolean dropAfterExport) throws Exception {
    JournalStore journal = eventStore.getJournalStore();

    // Close the current partition, so that any new events are separate from those that exist already
    journal.closeActivePartition();

    // TODO: Sort out max for each zip file
    File file = File.createTempFile("analytics-", ".zip");
    log.info("Exporting to: {}", file);

    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    JsonFactory jsonFactory = mapper.getFactory();

    // TODO: Write out a metadata.json with common, format + version shits?

    int i = 0;
    try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(file))) {

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
          generator.writeObject(anonymize(events.next()));
        }
        generator.flush();

        if (dropAfterExport) {
          journal.dropPartition(partition.getPartitionId());
        }

        generator.writeEndArray();
        generator.flush();
        output.closeEntry();
      }
    }
    // TODO: Move file to support dir

    log.info("Exported {} partitions to: {}", i, file);
    return file;
  }

  /**
   * Anonymize sensitive event data.
   */
  private Object anonymize(final EventData event) {
    event.setUserId(anonymize(event.getUserId()));
    event.setSessionId(anonymize(event.getSessionId()));
    return event;
  }

  private String anonymize(final String text) {
    if (text != null) {
      // TODO: Probably want to setup a tiny cache here to avoid re-anonymizing the same data over and over
      // TODO: the 2 main datapoints to anonymize (userId an sessionId) are likely to reoccur many times
      return anonymizer.anonymize(text);
    }
    return null;
  }
}
