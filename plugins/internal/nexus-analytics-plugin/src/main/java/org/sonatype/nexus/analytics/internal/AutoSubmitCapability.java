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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.nexus.capability.support.CapabilitySupport;
import org.sonatype.nexus.plugins.capabilities.Condition;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.scheduling.schedules.ManualRunSchedule;
import org.sonatype.sisu.goodies.i18n.I18N;
import org.sonatype.sisu.goodies.i18n.MessageBundle;

import static com.google.common.base.Preconditions.checkNotNull;

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

  private final NexusScheduler scheduler;

  private final Provider<AutoSubmitTask> taskFactory;

  private String taskId;

  @Inject
  public AutoSubmitCapability(final NexusScheduler scheduler,
                              final Provider<AutoSubmitTask> taskFactory)
  {
    this.scheduler = checkNotNull(scheduler);
    this.taskFactory = checkNotNull(taskFactory);
  }

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
    ScheduledTask scheduled;

    // automatically create task if needed
    List<ScheduledTask<?>> tasks = tasksForTypeId(AutoSubmitTask.ID);
    if (tasks.isEmpty()) {
      AutoSubmitTask task = taskFactory.get();
      scheduled = scheduler.schedule(
          "Automatically submit analytics events",
          task,
          new ManualRunSchedule()); // FIXME: Set default schedule to daily
      log.debug("Scheduled new task: {}", scheduled);
    }
    else {
      // else reference existing task
      // TODO: Sort out multiple tasks
      scheduled = tasks.get(0);
      log.debug("Resolved existing task: {}", scheduled);
    }

    // HACK: Find better way to manage ref
    taskId = scheduled.getId();
    log.debug("Task ID: {}", taskId);

    // TODO: Complain if schedule is manual?

    // enable task and save
    scheduled.setEnabled(true);
    scheduler.updateSchedule(scheduled);
  }

  /**
   * Helper to get all tasks for a given scheduled task type-id.
   */
  private List<ScheduledTask<?>> tasksForTypeId(final String typeId) {
    for (Entry<String, List<ScheduledTask<?>>> entry : scheduler.getActiveTasks().entrySet()) {
      if (typeId.equals(entry.getKey())) {
        return entry.getValue();
      }
    }
    return Collections.emptyList();
  }

  @Override
  protected void onPassivate(final AutoSubmitCapabilityConfiguration config) throws Exception {
    ScheduledTask scheduled = scheduler.getTaskById(taskId);

    // TODO: Sort out handling stopping task if its running?

    // disable the task
    scheduled.setEnabled(false);
    scheduler.updateSchedule(scheduled);
  }

  @Override
  protected String renderDescription() throws Exception {
    if (!context().isActive()) {
      return messages.disabledDescription();
    }

    return messages.description();
  }
}
