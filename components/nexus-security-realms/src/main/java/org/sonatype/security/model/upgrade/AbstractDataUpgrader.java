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

package org.sonatype.security.model.upgrade;

import org.sonatype.configuration.upgrade.ConfigurationIsCorruptedException;

/**
 * An abstract SingleVersionDataUpgrader that is used to validate the <code>configuration</code> object's class and
 * correctly cast it.
 */
public abstract class AbstractDataUpgrader<C>
    implements SingleVersionDataUpgrader
{

  public abstract void doUpgrade(C configuration)
      throws ConfigurationIsCorruptedException;

  @SuppressWarnings("unchecked")
  public void upgrade(Object configuration)
      throws ConfigurationIsCorruptedException
  {
    // TODO type check

    // template
    this.doUpgrade((C) configuration);
  }
}
