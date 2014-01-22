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
Ext.define('NX.controller.User', {
  extend: 'Ext.app.Controller',
  requires: [
    'NX.util.Base64',
    'NX.util.Permissions'
  ],
  mixins: {
    logAware: 'NX.LogAware'
  },

  views: [
    'Login'
  ],

  stores: [
    'Permission'
  ],

  refs: [
    {
      ref: 'header',
      selector: 'nx-header'
    }
  ],

  user: {
  },

  init: function () {
    var me = this;

    me.listen({
      controller: {
        '#Status': {
          user: me.updateUser
        }
      },
      store: {
        '#Permission': {
          load: me.onPermissionLoad
        }
      },
      component: {
        'nx-header button[action=login]': {
          click: me.showLoginWindow
        },
        'nx-login button[action=login]': {
          click: me.login
        },
        'nx-header button[action=user] menuitem[action=logout]': {
          click: me.logout
        }
      }
    });
  },

  /**
   * @private
   */
  updateUser: function (user) {
    var me = this,
        loginButton = me.getHeader().down('button[action=login]'),
        userButton = me.getHeader().down('button[action=user]');

    if (user) {
      if (me.user.hash != user.hash) {
        if (me.user.id != user.id) {
          me.getApplication().getMessageController().addMessage({text: 'User logged in: ' + user.id, type: 'success' });
          loginButton.hide();
          userButton.setText(user.id);
          userButton.show();
        }
        me.user = user;
        me.getPermissionStore().load();
      }
    }
    else {
      if (me.user.hash) {
        me.getApplication().getMessageController().addMessage({text: 'User logged out', type: 'success' });
        loginButton.show();
        userButton.hide();

        me.user = {};
        me.getPermissionStore().removeAll();
        me.onPermissionLoad();
      }
    }
  },

  /**
   * @private
   */
  showLoginWindow: function () {
    var dialog = Ext.widget('nx-login');
    dialog.down('field[name=username]').focus();
  },

  /**
   * @private
   */
  login: function (button) {
    var me = this,
        win = button.up('window'),
        form = button.up('form'),
        values = form.getValues(),
        userName = NX.util.Base64.encode(values.username),
        userPass = NX.util.Base64.encode(values.password);

    win.getEl().mask("Logging you in...");

    me.logDebug('Login...');

    NX.direct.Application.login(userName, userPass, function (response, status) {
      if (!NX.util.ExtDirect.showExceptionIfPresent('User could not be logged in', response, status)) {
        if (Ext.isDefined(response)) {
          if (response.success) {
            me.getApplication().getStatusController().refresh();
            win.getEl().unmask();
            win.close();
          }
          else {
            win.getEl().unmask();
          }
        }
      }
      else {
        win.getEl().unmask();
      }
    });
  },

  /**
   * @private
   */
  logout: function () {
    var me = this;

    me.logDebug('Logout...');

    NX.direct.Application.logout(function (response, status) {
      if (!NX.util.ExtDirect.showExceptionIfPresent('User could not be logged out', response, status)) {
        if (Ext.isDefined(response)) {
          if (response.success) {
            me.getApplication().getStatusController().refresh();
          }
        }
      }
    });
  },

  /**
   * @private
   */
  onPermissionLoad: function () {
    var me = this;

    NX.util.Permissions.setPermissions(me.getPermissions());
    me.fireEvent('permissionsChanged', NX.util.Permissions);
  },

  /**
   * @private
   */
  getPermissions: function () {
    var me = this,
        perms = {};

    me.getPermissionStore().each(function (rec) {
      perms[rec.get('id')] = rec.get('value');
    });

    return perms;
  }

});