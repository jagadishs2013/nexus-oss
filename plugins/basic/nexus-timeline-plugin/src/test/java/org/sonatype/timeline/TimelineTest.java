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

package org.sonatype.timeline;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimelineTest
    extends AbstractTimelineTestCase
{

  @Test
  public void testConfigureTimeline()
      throws Exception
  {
    try {
      timeline.start(new TimelineConfiguration());
    }
    finally {
      timeline.stop();
    }
  }

  @Test
  public void testSimpleAddAndRetrieve()
      throws Exception
  {
    timeline.start(new TimelineConfiguration());
    try {

      Map<String, String> data = new HashMap<String, String>();
      data.put("k1", "v1");
      data.put("k2", "v2");
      data.put("k3", "v3");
      timeline.add(createTimelineRecord(System.currentTimeMillis(), "typeA", "subType", data));

      Set<String> types = new HashSet<String>();
      types.add("typeA");
      AsList cb = new AsList();
      timeline.retrieve(0, 10, types, null, null, cb);
      List<TimelineRecord> results = cb.getRecords();

      assertEquals(1, results.size());
      assertEquals(data, results.get(0).getData());
    }
    finally {
      timeline.stop();
    }
  }
}
