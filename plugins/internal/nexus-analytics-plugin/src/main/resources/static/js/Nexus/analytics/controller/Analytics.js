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
/*global NX, Ext, Sonatype, Nexus*/

/**
 * Analytics controller.
 *
 * @since 2.8
 */
NX.define('Nexus.analytics.controller.Analytics', {
  extend: 'Nexus.controller.Controller',

  requires: [
    'Nexus.siesta',
    'Nexus.analytics.Icons',
    'Nexus.analytics.view.Panel',
    'Nexus.util.DownloadHelper'
  ],

  init: function() {
    var me = this;

    me.control({
      '#nx-analytics-view-events': {
        'activate': me.loadEvents
      },
      '#nx-analytics-view-events-button-refresh': {
        'click': me.refreshEvents
      },
      '#nx-analytics-view-events-button-clear': {
        'click': me.clearEvents
      },
      '#nx-analytics-view-events-button-export': {
        'click': me.exportEvents
      },
      '#nx-analytics-view-events-button-submit': {
        'click': me.submitEvents
      }
    });

    me.addNavigationMenu();
  },

  /**
   * Install panel into main navigation menu.
   *
   * @private
   */
  addNavigationMenu: function() {
    Sonatype.Events.on('nexusNavigationInit', function(panel) {
      var sp = Sonatype.lib.Permissions;

      panel.add({
        enabled: sp.checkPermission('nexus:analytics', sp.READ),
        sectionId: 'st-nexus-config',
        title: 'Analytics',
        tabId: 'analytics',
        tabCode: function() {
          return Ext.create({ xtype: 'nx-analytics-view-panel', id: 'analytics' });
        }
      });
    });
  },

  /**
   * Helper to show Analytics message.
   *
   * @private
   */
  showMessage: function(message) {
    Nexus.messages.show('Analytics', message);
  },

  /**
   * Load events.
   *
   * @private
   */
  loadEvents: function(panel) {
    var store = panel.getGrid().getStore();
    store.load({
      params: {
        start: 0,
        limit: Nexus.analytics.store.Events.PAGE_SIZE
      }
    });
  },

  /**
   * Refresh events panel.
   *
   * @private
   */
  refreshEvents: function(button) {
    var me = this,
        panel = button.up('nx-analytics-view-events');
    me.loadEvents(panel);
  },

  /**
   * Clear all events.
   *
   * @private
   */
  clearEvents: function(button) {
    var me = this,
        icons = Nexus.analytics.Icons,
        store = button.up('nx-analytics-view-events').getGrid().getStore();

    Ext.Msg.show({
      title: 'Clear all events',
      msg: 'Clear all analytics event data?',
      buttons: Ext.Msg.OKCANCEL,
      icon: icons.get('clear').variant('x32').cls,
      fn: function (btn) {
        if (btn === 'ok') {
          Ext.Ajax.request({
            url: Nexus.siesta.basePath + '/analytics/events',
            method: 'DELETE',
            suppressStatus: true,
            callback: function () {
              store.load();
            },
            success: function () {
              me.showMessage('Event data has been cleared');
            },
            failure: function (response) {
              me.showMessage('Failed to clear event data: ' + me.parseExceptionMessage(response));
            }
          });
        }
      }
    });
  },

  /**
   * Export and download events.
   *
   * @private
   */
  exportEvents: function(button) {
    // TODO: show options dialog ([_] clear after, [_] anonymize)
  },

  /**
   * Submit events to Sonatype.
   *
   * @private
   */
  submitEvents: function(button) {
    // TODO: show options dialog ([_] clear after)
  }
});