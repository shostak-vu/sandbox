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
package org.xwiki.component.wiki.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentInvocationHandler;
import org.xwiki.component.wiki.WikiComponentManager;

/**
 * Default implementation of {@link WikiComponentManager}. Creates proxy objects which method invocation handler keeps a
 * reference on a set of declared method and associated wiki content to "execute".
 * 
 * @since 2.4-M2
 * @version $Id$
 */
@Component
public class DefaultWikiComponentManager extends AbstractLogEnabled implements WikiComponentManager
{
    /**
     * Component manager against which wiki component will be registered.
     */
    @Requirement
    private ComponentManager mainComponentManager;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void registerWikiComponent(WikiComponent component) throws WikiComponentException
    {
        try {
            // Get the component role interface
            Class< ? > role = component.getRole();

            // Create the method invocation handler of the proxy
            InvocationHandler handler =
                new WikiComponentInvocationHandler(component.getDocumentReference(), component.getHandledMethods(),
                    mainComponentManager);

            // Prepare array of all interfaces the component implementation declares, that is the interface declared as
            // component role
            // plus possible extra other interfaces
            Class< ? >[] allImplementedInterfaces = new Class< ? >[component.getImplementedInterfaces().length + 1];
            System.arraycopy(component.getImplementedInterfaces(), 0, allImplementedInterfaces, 0, component
                .getImplementedInterfaces().length);
            allImplementedInterfaces[component.getImplementedInterfaces().length] = role;

            // Create the component instance and its descritor
            Object instance = Proxy.newProxyInstance(role.getClassLoader(), allImplementedInterfaces, handler);
            ComponentDescriptor componentDescriptor = this.createComponentDescriptor(role, component.getRoleHint());

            // Since we are responsible to create the component instance,
            // we also are responsible of its initialization (if needed)
            if (this.isInitializable(allImplementedInterfaces)) {
                try {
                    ((Initializable) instance).initialize();
                } catch (InitializationException e) {
                    getLogger().error("Failed to initialize wiki component", e);
                }
            }

            // Finally, register the component against the CM
            this.mainComponentManager.registerComponent(componentDescriptor, role.cast(instance));
        } catch (ComponentRepositoryException e) {
            throw new WikiComponentException("Failed to register wiki component against component repository", e);
        }
    }

    /**
     * Helper method to create a component descriptor from role and hint.
     * 
     * @param role the component role of the descriptor to create
     * @param roleHint the hint of the implementation for the descriptor to create
     * @return the constructed {@link ComponentDescriptor}
     */
    @SuppressWarnings("unchecked")
    private ComponentDescriptor createComponentDescriptor(Class role, String roleHint)
    {
        DefaultComponentDescriptor cd = new DefaultComponentDescriptor();
        cd.setRole(role);
        cd.setRoleHint(roleHint);
        return cd;
    }

    /**
     * Helper method that checks if at least one of an array of interfaces is the {@link Initializable} class.
     * 
     * @param interfaces the array of interfaces to test
     * @return true if at least one of the passed interfaces is the is the {@link Initializable} class.
     */
    private boolean isInitializable(Class< ? >[] interfaces)
    {
        for (Class< ? > iface : interfaces) {
            if (Initializable.class.equals(iface)) {
                return true;
            }
        }
        return false;
    }
}