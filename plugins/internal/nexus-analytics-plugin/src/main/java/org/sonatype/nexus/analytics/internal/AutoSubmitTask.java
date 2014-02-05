package org.sonatype.nexus.analytics.internal;

import javax.inject.Named;

import org.sonatype.nexus.scheduling.NexusTaskSupport;

/**
 * Event automatic submission task.
 *
 * @since 2.8
 */
@Named
public class AutoSubmitTask
  extends NexusTaskSupport
{
  // TODO: probably need a descriptor for this guy

  @Override
  protected String getMessage() {
    return "Automatically submitting analytics events";
  }

  @Override
  protected void execute() throws Exception {
    // TODO:
  }
}
