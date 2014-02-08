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

package org.sonatype.nexus.plugins.capabilities.internal.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.capabilities.CapabilityIdentity;
import org.sonatype.nexus.plugins.capabilities.internal.config.persistence.CCapability;
import org.sonatype.nexus.plugins.capabilities.internal.config.persistence.CCapabilityProperty;
import org.sonatype.nexus.plugins.capabilities.internal.config.persistence.Configuration;
import org.sonatype.sisu.goodies.lifecycle.LifecycleSupport;

import com.google.common.base.Equivalence;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.kazuki.v0.internal.v2schema.Attribute;
import io.kazuki.v0.internal.v2schema.Schema;
import io.kazuki.v0.store.KazukiException;
import io.kazuki.v0.store.Key;
import io.kazuki.v0.store.keyvalue.KeyValuePair;
import io.kazuki.v0.store.keyvalue.KeyValueStore;
import io.kazuki.v0.store.schema.SchemaStore;
import io.kazuki.v0.store.schema.TypeValidation;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.plugins.capabilities.CapabilityIdentity.capabilityIdentity;
import static org.sonatype.nexus.plugins.capabilities.CapabilityType.capabilityType;

/**
 * Handles persistence of capabilities configuration.
 */
@Singleton
@Named
public class DefaultCapabilityStorage
    extends LifecycleSupport
    implements CapabilityStorage
{
  public static final String CAPABILITY_SCHEMA = "capability";

  private final KeyValueStore keyValueStore;

  private final SchemaStore schemaStore;

  private final ReentrantLock lock = new ReentrantLock();

  @Inject
  public DefaultCapabilityStorage(final @Named("nexuscapability") KeyValueStore keyValueStore,
                                  final @Named("nexuscapability") SchemaStore schemaStore)
  {
    this.keyValueStore = checkNotNull(keyValueStore);
    this.schemaStore = checkNotNull(schemaStore);
  }

  @Override
  protected void doStart() throws Exception {
    if (schemaStore.retrieveSchema(CAPABILITY_SCHEMA) == null) {
      Schema schema = new Schema(Collections.<Attribute>emptyList());

      log.info("Creating schema for 'capability' type");

      schemaStore.createSchema(CAPABILITY_SCHEMA, schema);
    }
  }

  @Override
  public void add(final CapabilityStorageItem item) throws IOException {
    try {
      lock.lock();

      CCapability capability = asCCapability(item);

      for (CCapability current : keyValueStore.iterators().values(CAPABILITY_SCHEMA, CCapability.class)) {
        if (current.getId() != null && current.getId().equals(capability.getId())) {
          throw new IllegalArgumentException("already exists");
        }
      }

      log.info("Adding capability {}", capability);

      keyValueStore.create(CAPABILITY_SCHEMA, CCapability.class, capability, TypeValidation.STRICT);
    }
    catch (KazukiException e) {
      throw Throwables.propagate(e);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public boolean update(final CapabilityStorageItem item) throws IOException {
    try {
      lock.lock();

      final CCapability capability = asCCapability(item);

      log.info("Updating capability {}", capability);

      Key found = null;

      Predicate<Object> equiv = Equivalence.equals().equivalentTo(capability.getId());

      for (KeyValuePair<CCapability> kvEntry : keyValueStore.iterators()
          .entries(CAPABILITY_SCHEMA, CCapability.class)) {
        if (equiv.equals(kvEntry.getValue().getId())) {
          found = kvEntry.getKey();
          break;
        }
      }

      if (found == null) {
        log.info("Update - capability not found {}", capability);
        return false;
      }

      boolean result = keyValueStore.update(found, CCapability.class, capability);

      if (result) {
        log.info("Updated capability {}", capability);
      }
      else {
        log.info("Update failed - capability not updated {}", capability);
      }

      return result;
    }
    catch (KazukiException e) {
      throw Throwables.propagate(e);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public boolean remove(final CapabilityIdentity id) throws IOException {
    try {
      lock.lock();

      log.info("Removing capability {}", id);

      Iterator<CCapability> iter = keyValueStore.iterators().iterator(CAPABILITY_SCHEMA, CCapability.class);
      Predicate<Object> equiv = Equivalence.equals().equivalentTo(id.toString());

      while (iter.hasNext()) {
        final CCapability current = iter.next();

        if (equiv.equals(current.getId())) {
          iter.remove();
          log.debug("Removed capability {}", id);

          return true;
        }
      }

      log.info("Remove failed - capability {}", id);

      return false;
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public Collection<CapabilityStorageItem> getAll() throws IOException {
    Collection<CapabilityStorageItem> items = Lists.newArrayList();

    for (CCapability capability : keyValueStore.iterators().values(CAPABILITY_SCHEMA, CCapability.class)) {
      items.add(asCapabilityStorageItem(capability));
    }

    log.info("Get all capabilities {}", items);

    return items;
  }

  public Configuration load() throws IOException {
    Configuration config = new Configuration();
    config.setVersion(Configuration.MODEL_VERSION);

    for (CCapability capability : keyValueStore.iterators().values(CAPABILITY_SCHEMA, CCapability.class)) {
      config.addCapability(capability);
    }

    log.info("Loaded capabilities {}", config.getCapabilities());

    return config;
  }

  private CapabilityStorageItem asCapabilityStorageItem(final CCapability capability) {
    final Map<String, String> properties = new HashMap<String, String>();
    if (capability.getProperties() != null) {
      for (final CCapabilityProperty property : capability.getProperties()) {
        properties.put(property.getKey(), property.getValue());
      }
    }

    return new CapabilityStorageItem(
        capability.getVersion(),
        capabilityIdentity(capability.getId()),
        capabilityType(capability.getTypeId()),
        capability.isEnabled(),
        capability.getNotes(),
        properties
    );
  }

  private CCapability asCCapability(final CapabilityStorageItem item) {
    final CCapability capability = new CCapability();
    capability.setVersion(item.version());
    capability.setId(item.id().toString());
    capability.setTypeId(item.type().toString());
    capability.setEnabled(item.isEnabled());
    capability.setNotes(item.notes());
    if (item.properties() != null) {
      for (Map.Entry<String, String> entry : item.properties().entrySet()) {
        final CCapabilityProperty property = new CCapabilityProperty();
        property.setKey(entry.getKey());
        property.setValue(entry.getValue());
        capability.addProperty(property);
      }
    }
    return capability;
  }

}
