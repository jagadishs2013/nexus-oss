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

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.analytics.EventData;
import org.sonatype.nexus.analytics.EventStore;
import org.sonatype.sisu.goodies.lifecycle.LifecycleSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.kazuki.v0.internal.v2schema.Attribute;
import io.kazuki.v0.internal.v2schema.Attribute.Type;
import io.kazuki.v0.internal.v2schema.Schema;
import io.kazuki.v0.store.journal.JournalStore;
import io.kazuki.v0.store.journal.PartitionInfo;
import io.kazuki.v0.store.journal.PartitionInfoSnapshot;
import io.kazuki.v0.store.lifecycle.Lifecycle;
import io.kazuki.v0.store.schema.SchemaStore;
import io.kazuki.v0.store.schema.TypeValidation;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link EventStore} implementation.
 *
 * @since 2.8
 */
@Named
@Singleton
public class EventStoreImpl
  extends LifecycleSupport
  implements EventStore
{
  private static final String SCHEMA_NAME = "event_data";

  private final JournalStore store;

  private final SchemaStore schema;

  private final Lifecycle lifecycle;

  private final ReentrantLock exportLock = new ReentrantLock();

  @Inject
  public EventStoreImpl(final @Named("nexusanalytics") JournalStore store,
                        final @Named("nexusanalytics") SchemaStore schema,
                        final @Named("nexusanalytics") Lifecycle lifecycle)
  {
    this.store = checkNotNull(store);
    this.schema = checkNotNull(schema);
    this.lifecycle = checkNotNull(lifecycle);
  }
  
  @Override
  protected void doStart() throws Exception {
    lifecycle.init();
    lifecycle.start();
    
    if (schema.retrieveSchema(SCHEMA_NAME) == null) {
      Schema eventSchema = new Schema(ImmutableList.of(
          new Attribute("type", Type.UTF8_SMALLSTRING, null, false),
          new Attribute("timestamp", Type.I64, null, true),
          new Attribute("sequence", Type.I64, null, true),
          new Attribute("userId", Type.UTF8_SMALLSTRING, null, true),
          new Attribute("sessionId", Type.UTF8_SMALLSTRING, null, true),
          new Attribute("attributes", Type.MAP, null, true)));
      
      schema.createSchema(SCHEMA_NAME, eventSchema);
    }
  }

  @Override
  protected void doStop() throws Exception {
    lifecycle.shutdown();
    lifecycle.stop();
  }

  @Override
  public void add(final EventData data) throws Exception {
    checkNotNull(data);
    store.append(SCHEMA_NAME, EventData.class, data, TypeValidation.STRICT);
  }

  @Override
  public void clear() throws Exception {
    store.clear();
    log.debug("Cleared");
  }

  @Override
  public long approximateSize() throws Exception {
    return store.approximateSize();
  }

  @Override
  public Iterator<EventData> iterator(final long index) throws Exception {
    return store.getIteratorRelative(SCHEMA_NAME, EventData.class, index, null);
  }
  
  private void exportAllData(PrintWriter writer, ObjectMapper mapper, boolean dropPartitions) throws Exception {
    try {
      if (!exportLock.tryLock()) {
        throw new IllegalStateException("Already locked for export");
      }

      store.closeActivePartition();
  
      Iterator<PartitionInfoSnapshot> partitions = store.getAllPartitions();
      while (partitions.hasNext()) {
        PartitionInfo partition = partitions.next();
  
        if (!partition.isClosed()) {
          break;
        }
        
        Iterator<EventData> events = store.getIteratorRelative(SCHEMA_NAME, EventData.class, 0L, partition.getSize());

        while (events.hasNext()) {
          mapper.writeValue(writer, events.next());
        }

        writer.flush();

        if (dropPartitions) {
          store.dropPartition(partition.getPartitionId());
        }
      }
    } finally {
      if (exportLock.isHeldByCurrentThread()) {
        exportLock.unlock();
      }
    }
  }
}
