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
/**
 * Unsupported browser panel.
 *
 * @since 2.8
 */
Ext.define('NX.view.UnsupportedBrowser', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-unsupported-browser',

  layout: 'border',

  items: [
    {
      xtype: 'panel',
      region: 'center',
      layout: {
        type: 'vbox',
        align: 'center',
        pack: 'center'
      },
      items: [
        {
          xtype: 'label',
          html: 'Unsupported browser detected',
          style: {
            'color': '#000000',
            'font-size': '20px',
            'font-weight': 'bold',
            'text-align': 'center',
            'padding': '20px'
          }
        },
        { xtype: 'button', text: 'I\'m feeling lucky', action: 'lucky' }
      ]
    },
    {
      xtype: 'nx-footer',
      region: 'south',
      hidden: false
    },
    {
      xtype: 'nx-dev-panel',
      region: 'south',
      collapsible: true,
      collapsed: true,
      resizable: true,
      resizeHandles: 'n',

      // keep initial constraints to prevent huge panels
      height: 300,

      // default to hidden, only show if debug enabled
      hidden: true
    }
  ]

});