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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

import com.google.common.base.Throwables;

import io.kazuki.v0.internal.v2schema.Attribute;
import io.kazuki.v0.internal.v2schema.Schema;
import io.kazuki.v0.store.KazukiException;
import io.kazuki.v0.store.Key;
import io.kazuki.v0.store.keyvalue.KeyValuePair;
import io.kazuki.v0.store.keyvalue.KeyValueStore;
import io.kazuki.v0.store.schema.SchemaStore;
import io.kazuki.v0.store.schema.TypeValidation;
import static com.google.common.base.Preconditions.checkNotNull;
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
  public CapabilityIdentity add(final CapabilityStorageItem item) throws IOException {
    try {
      lock.lock();

      CCapability capability = asCCapability(item);

      log.info("Adding capability {}", capability);

      Key newKey = keyValueStore.create(CAPABILITY_SCHEMA, CCapability.class, capability, TypeValidation.STRICT);
      CapabilityIdentity newId = getCapabilityIdentity(newKey);

      log.info("Added capability {} -> {}", newId, capability);

      return getCapabilityIdentity(newKey);
    }
    catch (KazukiException e) {
      throw Throwables.propagate(e);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public boolean update(final CapabilityIdentity id, final CapabilityStorageItem item) throws IOException {
    try {
      lock.lock();

      final CCapability capability = asCCapability(item);

      log.info("Updating capability '{}' : {}", id, capability);

      boolean success = keyValueStore.update(getKey(id), CCapability.class, capability);

      if (success) {
        log.info("Updated capability '{}' : {}", id, capability);
      }
      else {
        log.info("Update failed - capability not updated '{}' {}", id, capability);
      }

      return success;
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

      log.info("Removing capability '{}'", id);

      boolean success = keyValueStore.delete(getKey(id));
      
      if (success) {
        log.info("Removed capability '{}'", id);
      }
      else {
        log.info("Remove failed - capability '{}'", id);
      }

      return success;
    } catch (KazukiException e) {
      throw Throwables.propagate(e);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public Map<CapabilityIdentity, CapabilityStorageItem> getAll() throws IOException {
    Map<CapabilityIdentity, CapabilityStorageItem> items = new LinkedHashMap<CapabilityIdentity, CapabilityStorageItem>();

    for (KeyValuePair<CCapability> entry : keyValueStore.iterators().entries(CAPABILITY_SCHEMA, CCapability.class)) {
      items.put(getCapabilityIdentity(entry.getKey()), asCapabilityStorageItem(entry.getValue()));
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

  private Key getKey(CapabilityIdentity id) {
    return Key.valueOf("@" + CAPABILITY_SCHEMA + ":" + id.toString());
  }

  private CapabilityIdentity getCapabilityIdentity(Key id) {
    return new CapabilityIdentity(id.getEncryptedIdentifier().split(":")[1]);
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
        capabilityType(capability.getTypeId()),
        capability.isEnabled(),
        capability.getNotes(),
        properties
    );
  }

  private CCapability asCCapability(final CapabilityStorageItem item) {
    final CCapability capability = new CCapability();
    capability.setVersion(item.version());
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
