/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.model.reference;

import org.xwiki.model.EntityType;

/**
 * Represents a reference to a wiki (wiki name). This is the topmost reference and it doesn't have a parent reference.
 * 
 * @version $Id: WikiReference.java 26135 2010-01-13 21:28:52Z vmassol $
 * @since 2.2M1
 */
public class WikiReference extends EntityReference
{
    /**
     * Special constructor that transforms a generic entity reference into a {@link WikiReference}. It checks the
     * validity of the passed reference (ie correct type).
     *
     * @exception IllegalArgumentException if the passed reference is not a valid wiki reference
     */
    public WikiReference(EntityReference reference)
    {
        super(reference.getName(), reference.getType());    
    }

    public WikiReference(String wikiName)
    {
        super(wikiName, EntityType.WIKI);
    }

    /**
     * {@inheritDoc}
     *
     * Overridden in order to verify the validity of the passed type
     *
     * @see org.xwiki.model.reference.EntityReference#setType(org.xwiki.model.EntityType)
     * @exception IllegalArgumentException if the passed type is not a wiki type
     */
    @Override public void setType(EntityType type)
    {
        if (type != EntityType.WIKI) {
            throw new IllegalArgumentException("Invalid type [" + type + "] for a wiki reference");
        }

        super.setType(EntityType.WIKI);
    }
}
