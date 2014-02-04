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

import org.apache.shiro.authz.annotation.RequiresPermissions
import org.sonatype.nexus.analytics.EventData
import org.sonatype.nexus.analytics.EventRecorder
import org.sonatype.nexus.analytics.EventStore
import org.sonatype.sisu.goodies.common.ComponentSupport
import org.sonatype.sisu.siesta.common.Resource

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

import static com.google.common.base.Preconditions.checkNotNull

/**
 * Analytics events resource.
 *
 * @since 2.8
 */
@Named
@Singleton
@Path(EventsResource.RESOURCE_URI)
class EventsResource
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_URI = '/analytics/events'

  private final EventRecorder eventRecorder

  private final EventStore eventStore

  @Inject
  EventsResource(final EventRecorder eventRecorder,
                 final EventStore eventStore)
  {
    this.eventRecorder = checkNotNull(eventRecorder);
    this.eventStore = checkNotNull(eventStore)
  }

  /**
   * List events in range.
   *
   * @param start Starting index
   * @param limit Limit number of events, or -1 for unlimited.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RequiresPermissions('nexus:analytics')
  Map list(final @QueryParam('start') @DefaultValue('0') int start,
           final @QueryParam('limit') @DefaultValue('-1') int limit)
  {
    log.debug "Listing events; start=$start limit=$limit"

    List<EventData> events = []
    def iter = eventStore.iterator(start)
    def count = 0
    while (iter.hasNext()) {
      events << iter.next()
      count++
      if (limit > 0 && count >= limit) {
        break
      }
    }

    return [
        // total number of records (approximate) needed for paging support
        total: eventStore.approximateSize(),
        events: events
    ]
  }

  /**
   * Clear all event data.
   */
  @DELETE
  @RequiresPermissions('nexus:analytics')
  void clear() {
    eventStore.clear()
  }

  /**
   * Submit all event data.
   */
  @POST
  @Path('submit')
  @RequiresPermissions('nexus:analytics')
  void submit() {
    // TODO: submit data, probably should fire up a background task.
    // TODO: we may need an automatic task, and a manual task impl?
  }

  /**
   * Export all event data.
   */
  @POST
  @Path('export')
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RequiresPermissions('nexus:analytics')
  Map export(final Map params) {
    // TODO: export data based on request params, return ref for download
    return [
        file: '/foo/bar/baz/fixme.zip',
        name: 'fixme.zip',
        size: 1234
    ]
  }

  // TODO: Add 'download' protected by authtoken, see SupportZipResource

  /**
   * Append events.  This requires no permissions.
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  void append(List<EventData> events) {
    if (!eventRecorder.enabled) {
      log.warn 'Ignoring events; recording is disabled'
      return
    }
    events.each {
      eventRecorder.record(it)
    }
  }
}