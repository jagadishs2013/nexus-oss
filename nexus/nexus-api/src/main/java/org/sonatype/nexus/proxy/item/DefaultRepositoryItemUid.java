/**
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://www.sonatype.com/products/nexus/attributions.
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.proxy.item;

import org.sonatype.nexus.proxy.item.uid.Attribute;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * The Class RepositoryItemUid. This class represents unique and constant label of all items/files originating from a
 * Repository, thus backed by some storage (eg. Filesystem).
 */
public class DefaultRepositoryItemUid
    implements RepositoryItemUid
{
    /** The factory. */
    private final RepositoryItemUidFactory factory;

    /** The repository. */
    private final Repository repository;

    /** The path. */
    private final String path;

    /** the string representation, that is immutable just as UID instance is */
    private final String stringRepresentation;

    /** Lazily created */
    private RepositoryItemUidLock lock;

    protected DefaultRepositoryItemUid( final RepositoryItemUidFactory factory, final Repository repository,
                                        final String path )
    {
        super();

        this.factory = factory;

        this.repository = repository;

        this.path = path;

        this.stringRepresentation = getRepository().getId() + ":" + getPath();

        this.lock = null;
    }

    public RepositoryItemUidFactory getRepositoryItemUidFactory()
    {
        return factory;
    }

    @Override
    public String getKey()
    {
        return stringRepresentation;
    }

    @Override
    public Repository getRepository()
    {
        return repository;
    }

    @Override
    public String getPath()
    {
        return path;
    }

    @Override
    public synchronized RepositoryItemUidLock getLock()
    {
        if ( lock == null )
        {
            lock = factory.createUidLock( this );
        }

        return lock;
    }
    
    @Override
    public synchronized RepositoryItemUidLock getAttributeLock()
    {
        if ( lock == null )
        {
            lock = factory.createUidAttributeLock( this );
        }

        return lock;
    }

    @Override
    public <A extends Attribute<?>> A getAttribute( Class<A> attrClass )
    {
        return getRepository().getRepositoryItemUidAttributeManager().getAttribute( attrClass, this );
    }

    @Override
    public <A extends Attribute<V>, V> V getAttributeValue( Class<A> attrClass )
    {
        A attr = getAttribute( attrClass );

        if ( attr != null )
        {
            return attr.getValueFor( this );
        }
        else
        {
            return null;
        }
    }

    @Override
    public <A extends Attribute<Boolean>> boolean getBooleanAttributeValue( Class<A> attr )
    {
        Boolean bool = getAttributeValue( attr );

        if ( bool != null && bool.booleanValue() )
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * toString() will return a "string representation" of this UID in form of repoId + ":" + path
     */
    @Override
    public String toString()
    {
        return stringRepresentation;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( stringRepresentation == null ) ? 0 : stringRepresentation.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        DefaultRepositoryItemUid other = (DefaultRepositoryItemUid) obj;
        if ( stringRepresentation == null )
        {
            if ( other.stringRepresentation != null )
                return false;
        }
        else if ( !stringRepresentation.equals( other.stringRepresentation ) )
            return false;
        return true;
    }
}