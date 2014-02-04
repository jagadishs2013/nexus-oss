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
 * Events panel.
 *
 * @since 2.8
 */
NX.define('Nexus.analytics.view.Events', {
  extend: 'Ext.Panel',

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  requires: [
    'Nexus.analytics.Icons',
    'Nexus.analytics.store.Events'
  ],

  xtype: 'nx-analytics-view-events',
  title: 'Events',
  id: 'nx-analytics-view-events',
  cls: 'nx-analytics-view-events',

  border: false,
  layout: 'fit',

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
        icons = Nexus.analytics.Icons,
        store = NX.create('Nexus.analytics.store.Events'),
        grid;

    me.grid = NX.create('Ext.grid.GridPanel', {
      border: false,
      autoScroll: true,

      loadMask: {
        msg: 'Loading...',
        msgCls: 'loading-indicator'
      },

      store: store,
      stripeRows: true,

      autoExpandColumn: 'sessionId',

      colModel: NX.create('Ext.grid.ColumnModel', {
        defaults: {
          sortable: true
        },
        columns: [
          {
            width: 30,
            resizable: false,
            sortable: false,
            fixed: true,
            hideable: false,
            menuDisabled: true,
            renderer: function (value, metaData, record) {
              return icons.forType(record.get('type')).img;
            }
          },
          {
            id: 'type',
            header: 'Type',
            dataIndex: 'type'
          },
          {
            id: 'timestamp',
            header: 'Timestamp',
            dataIndex: 'timestamp',
          },
          {
            id: 'sequence',
            header: 'Sequence',
            dataIndex: 'sequence'
          },
          {
            id: 'orgId',
            header: 'Organization',
            dataIndex: 'orgId',
            hidden: true
          },
          {
            id: 'hostId',
            header: 'Host',
            dataIndex: 'hostId',
            hidden: true
          },
          {
            id: 'userId',
            header: 'User',
            dataIndex: 'userId'
          },
          {
            id: 'sessionId',
            header: 'Session',
            dataIndex: 'sessionId'
          }
        ]
      }),

      bbar: NX.create('Ext.PagingToolbar', {
        pageSize: Nexus.analytics.store.Events.PAGE_SIZE,
        store: store,
        displayInfo: true,
        displayMsg: 'Displaying events {0} - {1} of {2}',
        emptyMsg: 'No events to display'
      })
    });

    Ext.apply(me, {
      items: [
        me.grid
      ],

      tbar: [
        {
          xtype: 'button',
          id: 'nx-analytics-view-events-button-refresh',
          text: 'Refresh',
          tooltip: 'Refresh event data',
          iconCls: icons.get('refresh').cls
        },
        {
          xtype: 'button',
          id: 'nx-analytics-view-events-button-clear',
          text: 'Clear',
          tooltip: 'Clear all event data',
          iconCls: icons.get('clear').cls
        },
        {
          xtype: 'button',
          id: 'nx-analytics-view-events-button-export',
          text: 'Export',
          tooltip: 'Export and download event data',
          iconCls: icons.get('export').cls
        },
        '-',
        {
          xtype: 'button',
          id: 'nx-analytics-view-events-button-submit',
          text: 'Submit',
          tooltip: 'Submit event data to Sonatype',
          iconCls: icons.get('submit').cls
        },
        '->',
        {
          xtype: 'nx-grid-filter-box',
          filteredGrid: me.grid
        }
      ]
    });

    me.constructor.superclass.initComponent.apply(me, arguments);
  },

  /**
   * Returns the events grid.
   *
   * @public
   */
  getGrid: function () {
    return this.grid;
  }
});