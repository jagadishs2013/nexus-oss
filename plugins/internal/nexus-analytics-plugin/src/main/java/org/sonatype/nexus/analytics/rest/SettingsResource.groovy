/**
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

package org.sonatype.nexus.analytics.rest

import groovy.transform.ToString
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.sonatype.sisu.goodies.common.ComponentSupport
import org.sonatype.sisu.siesta.common.Resource

import javax.inject.Named
import javax.inject.Singleton
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * Analytics settings resource.
 *
 * @since 2.8
 */
@Named
@Singleton
@Path(SettingsResource.RESOURCE_URI)
class SettingsResource
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_URI = '/analytics/settings'

  @ToString(includePackage = false, includeNames = true)
  static class SettingsXO
  {
    boolean collection

    boolean autosubmit
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RequiresPermissions('nexus:analytics')
  SettingsXO get() {
    return new SettingsXO(
        collection: false,
        autosubmit: false
    )
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @RequiresPermissions('nexus:analytics')
  void put(SettingsXO settings) {
    log.info("Update settings: $settings")
  }
}