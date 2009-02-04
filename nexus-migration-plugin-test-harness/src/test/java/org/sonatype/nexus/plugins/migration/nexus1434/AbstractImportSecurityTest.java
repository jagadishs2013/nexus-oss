/**
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.nexus.plugins.migration.nexus1434;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.sonatype.nexus.plugins.migration.AbstractMigrationIntegrationTest;
import org.sonatype.nexus.plugins.migration.util.PlexusUserMessageUtil;
import org.sonatype.nexus.rest.model.PlexusRoleResource;
import org.sonatype.nexus.rest.model.PlexusUserResource;
import org.sonatype.nexus.rest.model.PrivilegeBaseStatusResource;
import org.sonatype.nexus.rest.model.RepositoryTargetListResource;
import org.sonatype.nexus.rest.model.RoleResource;
import org.sonatype.nexus.test.utils.GroupMessageUtil;
import org.sonatype.nexus.test.utils.PrivilegesMessageUtil;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.RoleMessageUtil;
import org.sonatype.nexus.test.utils.TargetMessageUtil;

public abstract class AbstractImportSecurityTest
    extends AbstractMigrationIntegrationTest
{

    protected PlexusUserMessageUtil userUtil;

    protected TargetMessageUtil repoTargetUtil;

    protected RoleMessageUtil roleUtil;

    protected PrivilegesMessageUtil privilegeUtil;

    protected RepositoryMessageUtil repoUtil;

    protected GroupMessageUtil groupUtil;

    /**
     * System user, role, privilege, repoTarget, before importing artifactory
     */
    protected List<PrivilegeBaseStatusResource> prePrivilegeList;

    protected List<RoleResource> preRoleList;

    protected List<PlexusUserResource> preUserList;

    protected List<RepositoryTargetListResource> preTargetList;

    public AbstractImportSecurityTest()
    {
        // initialize the utils
        userUtil = new PlexusUserMessageUtil( );
        repoTargetUtil = new TargetMessageUtil( getXMLXStream(), MediaType.APPLICATION_XML );
        privilegeUtil = new PrivilegesMessageUtil( getXMLXStream(), MediaType.APPLICATION_XML );
        roleUtil = new RoleMessageUtil( getXMLXStream(), MediaType.APPLICATION_XML );
        try
        {
            repoUtil = new RepositoryMessageUtil( getXMLXStream(), MediaType.APPLICATION_XML, this.getRepositoryTypeRegistry() );
        }
        catch ( ComponentLookupException e )
        {
            Assert.fail( "Failed to lookup component: "+ e.getMessage() );
        }
        groupUtil = new GroupMessageUtil( getXMLXStream(), MediaType.APPLICATION_XML );
    }

    abstract protected void importSecurity()
        throws Exception;

    abstract protected void verifySecurity()
        throws Exception;

    @Test
    public void testImportSecurity()
        throws Exception
    {
        loadPreResources();

        importSecurity();

        verifySecurity();
    }

    @SuppressWarnings( "static-access" )
    protected void loadPreResources()
        throws Exception
    {
        // load PREs
        preUserList = userUtil.getList();
        prePrivilegeList = privilegeUtil
            .getResourceListFromResponse( privilegeUtil.sendMessage( Method.GET, null, "" ) );
        preRoleList = roleUtil.getList();
        preTargetList = repoTargetUtil.getList();
    }

    @SuppressWarnings( "static-access" )
    protected List<RepositoryTargetListResource> getImportedRepoTargetList()
        throws Exception
    {
        List<RepositoryTargetListResource> targetList = repoTargetUtil.getList();

        List<RepositoryTargetListResource> addedList = new ArrayList<RepositoryTargetListResource>();

        for ( RepositoryTargetListResource target : targetList )
        {
            if ( !containRepoTarget( preTargetList, target.getId() ) )
            {
                addedList.add( target );
            }
        }
        return addedList;
    }

    protected List<RoleResource> getImportedRoleList()
        throws Exception
    {
        List<RoleResource> roleList = roleUtil.getList();

        List<RoleResource> addedList = new ArrayList<RoleResource>();

        for ( RoleResource role : roleList )
        {
            if ( !containRole( preRoleList, role.getId() ) )
            {
                addedList.add( role );
            }
        }
        return addedList;
    }

    protected List<PrivilegeBaseStatusResource> getImportedPrivilegeList()
        throws Exception
    {
        List<PrivilegeBaseStatusResource> privilegeList = privilegeUtil.getResourceListFromResponse( privilegeUtil
            .sendMessage( Method.GET, null, "" ) );

        List<PrivilegeBaseStatusResource> addedList = new ArrayList<PrivilegeBaseStatusResource>();

        for ( PrivilegeBaseStatusResource privilege : privilegeList )
        {
            if ( !containPrivilege( prePrivilegeList, privilege.getId() ) )
            {
                addedList.add( privilege );
            }
        }
        return addedList;
    }

    protected List<PlexusUserResource> getImportedUserList()
        throws Exception
    {
        List<PlexusUserResource> userList = userUtil.getList();

        List<PlexusUserResource> addedList = new ArrayList<PlexusUserResource>();

        for ( PlexusUserResource user : userList )
        {
            if ( !containUser( preUserList, user.getUserId() ) )
            {
                addedList.add( user );
            }
        }
        return addedList;
    }

    protected boolean containRepoTarget( List<RepositoryTargetListResource> repoTargetList, String repoTargetId )
    {
        for ( RepositoryTargetListResource target : repoTargetList )
        {
            if ( target.getId().equals( repoTargetId ) )
            {
                return true;
            }
        }
        return false;
    }

    protected boolean containRole( List<RoleResource> roleList, String roleId )
    {
        for ( RoleResource role : roleList )
        {
            if ( role.getId().equals( roleId ) )
            {
                return true;
            }
        }
        return false;
    }

    protected boolean containPlexusRole( List<PlexusRoleResource> roleList, String roleId )
    {
        for ( PlexusRoleResource role : roleList )
        {
            if ( role.getRoleId().equals( roleId ) )
            {
                return true;
            }
        }
        return false;
    }

    protected boolean containUser( List<PlexusUserResource> userList, String userId )
    {
        for ( PlexusUserResource user : userList )
        {
            if ( user.getUserId().equals( userId ) )
            {
                return true;
            }
        }
        return false;
    }

    protected boolean containPrivilege( List<PrivilegeBaseStatusResource> privList, String privId )
    {
        for ( PrivilegeBaseStatusResource priv : privList )
        {
            if ( priv.getId().equals( privId ) )
            {
                return true;
            }
        }
        return false;
    }

    protected boolean containPrivilegeName( List<PrivilegeBaseStatusResource> privList, String privName )
    {
        for ( PrivilegeBaseStatusResource priv : privList )
        {
            if ( priv.getName().equals( privName ) )
            {
                return true;
            }
        }
        return false;
    }

    protected PlexusUserResource getUserById( List<PlexusUserResource> userList, String id )
    {
        for ( PlexusUserResource user : userList )
        {
            if ( user.getUserId().equals( id ) )
            {
                return user;
            }
        }
        return null;
    }

    protected RoleResource getRoleById( List<RoleResource> roleList, String id )
    {
        for ( RoleResource role : roleList )
        {
            if ( role.getId().equals( id ) )
            {
                return role;
            }
        }
        return null;
    }

    protected boolean containRoleEndWith( List<PlexusRoleResource> roleList, String suffix )
    {
        for ( PlexusRoleResource role : roleList )
        {
            if ( role.equals( suffix ) )
            {
                return true;
            }
        }
        return false;
    }

}
