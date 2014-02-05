package org.sonatype.nexus.analytics.internal;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.analytics.EventExporter;
import org.sonatype.nexus.scheduling.NexusTaskSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event submission task.
 *
 * @since 2.8
 */
@Named
public class SubmitTask
  extends NexusTaskSupport
{
  private final EventExporter eventExporter;

  @Inject
  public SubmitTask(final EventExporter eventExporter) {
    this.eventExporter = checkNotNull(eventExporter);
  }

  @Override
  protected String getMessage() {
    return "Submitting analytics events";
  }

  @Override
  protected void execute() throws Exception {
    // HACK: for now simply export with drop
    eventExporter.export(true);
  }
}
