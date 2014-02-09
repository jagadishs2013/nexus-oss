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

import java.util.Map;

import javax.inject.Named;

import org.sonatype.nexus.capability.support.CapabilitySupport;
import org.sonatype.nexus.plugins.capabilities.Condition;
import org.sonatype.sisu.goodies.i18n.I18N;
import org.sonatype.sisu.goodies.i18n.MessageBundle;

/**
 * Analytics automatic submission capability.
 *
 * @since 2.8
 */
@Named(AutoSubmitCapabilityDescriptor.TYPE_ID)
public class AutoSubmitCapability
    extends CapabilitySupport<AutoSubmitCapabilityConfiguration>
{
  private static interface Messages
      extends MessageBundle
  {
    // TODO: change this to last submission time or something
    @DefaultMessage("Automatic submission is enabled")
    String description();

    @DefaultMessage("Automatic submission is disabled")
    String disabledDescription();
  }

  private static final Messages messages = I18N.create(Messages.class);

  @Override
  protected AutoSubmitCapabilityConfiguration createConfig(final Map<String, String> properties) throws Exception {
    return new AutoSubmitCapabilityConfiguration(properties);
  }

  @Override
  public Condition activationCondition() {
    return conditions().logical().and(
        // collection capability must be active
        conditions().capabilities().capabilityOfTypeActive(CollectionCapabilityDescriptor.TYPE),
        conditions().capabilities().passivateCapabilityDuringUpdate()
    );
  }

  @Override
  protected void onActivate(final AutoSubmitCapabilityConfiguration config) throws Exception {
    // TODO: ensure auto-submit task exists
    // TODO: sanity check existing task is not manual?  Perhaps only bitch?
    // TODO: enable task
  }

  @Override
  protected void onPassivate(final AutoSubmitCapabilityConfiguration config) throws Exception {
    // TODO: disable auto-submit task
  }

  @Override
  protected String renderDescription() throws Exception {
    if (!context().isActive()) {
      return messages.disabledDescription();
    }

    return messages.description();
  }
}
