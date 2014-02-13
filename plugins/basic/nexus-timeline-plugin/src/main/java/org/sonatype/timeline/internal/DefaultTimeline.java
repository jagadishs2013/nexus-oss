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

package org.sonatype.timeline.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.timeline.TimelinePlugin;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.timeline.Timeline;
import org.sonatype.timeline.TimelineCallback;
import org.sonatype.timeline.TimelineConfiguration;
import org.sonatype.timeline.TimelineFilter;
import org.sonatype.timeline.TimelineRecord;

import com.google.common.annotations.VisibleForTesting;
import io.kazuki.v0.internal.v2schema.Attribute;
import io.kazuki.v0.internal.v2schema.Schema;
import io.kazuki.v0.store.KazukiException;
import io.kazuki.v0.store.journal.JournalStore;
import io.kazuki.v0.store.keyvalue.KeyValuePair;
import io.kazuki.v0.store.lifecycle.Lifecycle;
import io.kazuki.v0.store.schema.SchemaStore;
import io.kazuki.v0.store.schema.TypeValidation;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@Named("default")
public class DefaultTimeline
    extends ComponentSupport
    implements Timeline
{
  public static final String TIMELINE_SCHEMA = "timeline";

  private final Lifecycle lifecycle;

  private final JournalStore journalStore;

  private final SchemaStore schemaStore;

  private final ReentrantReadWriteLock timelineLock;

  private boolean started;

  @Inject
  public DefaultTimeline(final @Named(TimelinePlugin.ARTIFACT_ID) Lifecycle lifecycle,
                         final @Named(TimelinePlugin.ARTIFACT_ID) JournalStore journalStore,
                         final @Named(TimelinePlugin.ARTIFACT_ID) SchemaStore schemaStore)
  {
    this.lifecycle = checkNotNull(lifecycle);
    this.journalStore = checkNotNull(journalStore);
    this.schemaStore = checkNotNull(schemaStore);

    this.timelineLock = new ReentrantReadWriteLock();
    this.started = false;
  }

  // ==
  // Public API

  @Override
  public void start(final TimelineConfiguration configuration)
      throws IOException
  {
    log.debug("Starting Timeline...");
    timelineLock.writeLock().lock();
    try {
      if (!started) {
        lifecycle.init();
        lifecycle.start();

        // create schema if needed
        if (schemaStore.retrieveSchema(TIMELINE_SCHEMA) == null) {
          log.info("Creating schema for {} type", TIMELINE_SCHEMA);
          final Schema schema = new Schema(Collections.<Attribute>emptyList());
          schemaStore.createSchema(TIMELINE_SCHEMA, schema);
        }
        log.info("Started Timeline...");
        started = true;
      }
    }
    catch (KazukiException e) {
      throw new IOException("Could not start Timeline", e);
    }
    finally {
      timelineLock.writeLock().unlock();
    }
  }

  @Override
  public void stop()
      throws IOException
  {
    log.debug("Stopping Timeline...");
    timelineLock.writeLock().lock();
    try {
      if (started) {
        started = false;
        lifecycle.shutdown();
        lifecycle.stop();
        log.info("Stopped Timeline...");
      }
    }
    finally {
      timelineLock.writeLock().unlock();
    }
  }

  @VisibleForTesting
  public boolean isStarted() {
    return started;
  }

  @Override
  public void add(final TimelineRecord... records) {
    if (!started) {
      return;
    }
    try {
      for (TimelineRecord record : records) {
        journalStore.append(TIMELINE_SCHEMA, TimelineRecord.class, record, TypeValidation.STRICT);
      }
    }
    catch (KazukiException e) {
      log.warn("Failed to append a Timeline record", e);
    }
  }

  @Override
  public int purgeOlderThan(final int days) {
    if (!started) {
      return 0;
    }
    // TODO: How to delete selectively the head of journal? Event if we neglect "days"..
    // Basically, purge was needed to lessen Lucene index and it's impact on resources.
    // If Kazuki "behaves" way better, we can simply tell users to remove their "Purge Timeline" tasks
    // as that task becomes obsolete.
    return 0;
  }

  @Override
  public void retrieve(int fromItem, int count, Set<String> types, Set<String> subTypes, TimelineFilter filter,
                       TimelineCallback callback)
  {
    if (!started) {
      return;
    }
    try {
      final Iterable<KeyValuePair<TimelineRecord>> kvs = journalStore
          .entriesRelative(TIMELINE_SCHEMA, TimelineRecord.class, (long) fromItem, (long) count);
      for (KeyValuePair<TimelineRecord> kv : kvs) {
        final TimelineRecord record = kv.getValue();
        if (types != null && !types.contains(record.getType())) {
          continue; // skip it
        }
        if (subTypes != null && !subTypes.contains(record.getSubType())) {
          continue; // skip it
        }
        if (filter != null && !filter.accept(record)) {
          callback.processNext(record);
        }
      }
    }
    catch (IOException e) {
      log.warn("Failed to process Timeline record in callback", e);
    }
    catch (KazukiException e) {
      log.warn("Failed to iterate Timeline store", e);
    }
  }

}
