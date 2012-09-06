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
package org.xwiki.model.internal;

import java.net.MalformedURLException;
import java.util.List;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.model.Entity;
import org.xwiki.model.EntityManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelException;
import org.xwiki.model.UniqueReference;
import org.xwiki.model.Version;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class BridgedEntityManager implements EntityManager
{
    private XWikiContext xcontext;

    /**
     * Cache holding modified entities not yet saved to persistent storage.
     */
    private Cache<Entity> modifiedEntityCache;

    public BridgedEntityManager(CacheManager cacheManager, XWikiContext xcontext)
    {
        this.xcontext = xcontext;

        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setConfigurationId("model");
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        cacheConfiguration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

        try {
            this.modifiedEntityCache = cacheManager.getCacheFactory().newCache(cacheConfiguration);
        } catch (Exception e) {
            throw new ModelException("Failed to create Entity Cache", e);
        }
    }

    @Override
    public <T extends Entity> T getEntity(UniqueReference uniqueReference)
    {
        T result = null;

        // Verify first if the entity requested is in the modified entity cache
        // TODO: why doesn't the cache api support an Object as key? This makes it complex for us here...
        // FTM doing something extra simple...
        T modifiedEntity = (T) this.modifiedEntityCache.get(uniqueReference.toString());
        if (modifiedEntity != null) {
            return modifiedEntity;
        }

        EntityReference reference = uniqueReference.getReference();
        switch (reference.getType()) {
            case DOCUMENT:
                result = (T) new BridgedDocument(getXWikiDocument(reference));
                break;
            case SPACE:
                // A space exists if there's at least one document in it.
                try {
                    List<String> spaces = getXWiki().getSpaces(getXWikiContext());
                    if (spaces.contains(reference.getName())) {
                        result = (T) new BridgedSpace();
                    }
                } catch (XWikiException e) {
                    throw new ModelException("Error verifying existence of space [" + reference + "]", e);
                }
                break;
            case WIKI:
                // TODO: Need to load the wiki details. FTM only checking if it exists
                if (hasEntity(uniqueReference)) {
                    result = (T) new BridgedWiki(getXWikiContext());
                }
                break;
            case OBJECT:
                result = (T) getXWikiObject(reference);
                break;
            case OBJECT_PROPERTY:
                BaseObject xobject = getXWikiObject(reference.getParent());
                if (xobject != null) {
                    /*
                    try {
                        result = (T) new BridgedObjectProperty(xobject.get(reference.getName()));
                    } catch (XWikiException e) {
                        // TODDO
                    }
                    */
                }
                break;
            default:
                throw new ModelException("Not supported");
        }

        return result;
    }

    private BaseObject getXWikiObject(EntityReference reference)
    {
        BaseObject result = null;

        // Find the reference to the document containing the object (it's the parent of the passed
        // reference) and Load the parent document since objects are loaded at the same time in the old model.
        XWikiDocument xdoc = getXWikiDocument(reference.getParent());
        // Get the requested object if the document isn't new...
        if (xdoc != null && !xdoc.isNew()) {
            BaseObject object = xdoc.getXObject(new ObjectReference(reference));
            if (object != null) {
                result = object;
            }
        }

        return result;
    }

    private XWikiDocument getXWikiDocument(EntityReference reference)
    {
        XWikiDocument result = null;

        try {
            // Since the old model API always return a XWikiDocument even if it doesn't exist, we need to check
            // if the document is new or not.
            XWikiDocument xdoc = getXWiki().getDocument(new DocumentReference(reference), getXWikiContext());
            if (!xdoc.isNew()) {
                result = xdoc;
            }
        } catch (XWikiException e) {
            throw new ModelException("Error loading document [" + reference + "]", e);
        }

        return result;
    }

    @Override
    public boolean hasEntity(UniqueReference uniqueReference)
    {
        boolean result;

        // TODO: should we return true if the entity has been created but not saved in the DB?

        EntityReference reference = uniqueReference.getReference();
        switch (reference.getType()) {
            case DOCUMENT:
                result = getXWiki().exists(new DocumentReference(reference), getXWikiContext());
                break;
            case WIKI:
                try {
                    result = getXWiki().getServerURL(new WikiReference(reference).getName(), getXWikiContext()) != null;
                } catch (MalformedURLException e) {
                    result = false;
                }
                break;
            default:
                throw new ModelException("Not supported");
        }
        return result;
    }

    @Override
    public <T extends Entity> T addEntity(UniqueReference uniqueReference)
    {
        T result;

        EntityReference reference = uniqueReference.getReference();
        if (reference.getType().equals(EntityType.WIKI)) {
            result = (T) new BridgedWiki(getXWikiContext());
        } else {
            throw new ModelException("Not supported");
        }

        // Save the Entity in the cache
        // TODO: in the future send it through an event
        this.modifiedEntityCache.set(uniqueReference.toString(), result);

        return result;
    }

    @Override
    public void removeEntity(UniqueReference uniqueReference)
    {
        throw new ModelException("Not supported");
    }

    @Override
    public void rollback(Version versionToRollbackTo)
    {
        throw new ModelException("Not supported");
    }

    public XWiki getXWiki()
    {
        return this.xcontext.getWiki();
    }

    public XWikiContext getXWikiContext()
    {
        return this.xcontext;
    }
}
