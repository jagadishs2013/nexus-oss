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

package org.sonatype.nexus.security.ldap.realms.api.dto;

import javax.xml.bind.annotation.XmlRootElement;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias(value = "connectionInfo")
@XmlRootElement(name = "connectionInfo")
public class LdapConnectionInfoResponse
{

  LdapConnectionInfoDTO data;

  /**
   * @return the connectionInfo
   */
  public LdapConnectionInfoDTO getData() {
    return data;
  }

  /**
   * @param connectionInfo the connectionInfo to set
   */
  public void setData(LdapConnectionInfoDTO connectionInfo) {
    this.data = connectionInfo;
  }

}
