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

import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;

// FIXME: Move to goodies, not used anymore here but generally still useful

/**
 * Cyclic counter.
 *
 * @since 2.8
 */
public class CyclicCounter
{
  private final long max;

  private final AtomicLong value;

  public CyclicCounter(final long max) {
    this.max = max;
    checkArgument(max > 0, "max (%s) must be > 0", max);
    this.value = new AtomicLong(0);
  }

  public void reset() {
    value.set(0);
  }

  public long get() {
    return value.get();
  }

  public long next() {
    long current, next;
    do {
      current = value.get();
      next = (current + 1) % max;
    }
    while (!value.compareAndSet(current, next));
    return next;
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
