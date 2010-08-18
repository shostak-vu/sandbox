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
package org.xwiki.extension.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.ExtensionType;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryFactory;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.LocalExtensionRepository;

/**
 * TODO: cut installation process in steps (create and validate an install plan, install, etc.)
 */
@Component
public class DefaultExtensionManager implements ExtensionManager, Initializable
{
    @Requirement
    private ExtensionRepositoryManager repositoryManager;

    @Requirement(role = ExtensionRepositoryFactory.class)
    private List<ExtensionRepositoryFactory> extensionRepositoryFactory;

    @Requirement
    private LocalExtensionRepository localExtensionRepository;

    /**
     * extensions than can't be upgraded (generally because it's "core module" and not part of the extension management
     * system)
     */
    private Map<String, ExtensionId> lockedExtensions = new HashMap<String, ExtensionId>();

    public void initialize() throws InitializationException
    {
        for (ExtensionRepositoryFactory repositoryFactory : this.extensionRepositoryFactory) {
            for (ExtensionRepository repository : repositoryFactory.getDefaultExtensionRepositories()) {
                this.repositoryManager.addRepository(repository);
            }
        }
    }

    public int coundAvailableExtensions()
    {
        // TODO
        return 0;
    }

    public int coundInstalledExtensions()
    {
        // TODO
        return 0;
    }

    public List<Extension> getAvailableExtensions(int nb, int offset)
    {
        // TODO
        return null;
    }

    public List<? extends Extension> getInstalledExtensions(int nb, int offset)
    {
        return this.localExtensionRepository.getExtensions(nb, offset);
    }

    public void installExtension(ExtensionId extensionId) throws InstallException
    {
        installExtension(extensionId, false);
    }

    private void installExtension(ExtensionId extensionId, boolean dependency) throws InstallException
    {
        try {
            Extension remoteExtension = this.repositoryManager.resolve(extensionId);

            for (ExtensionDependency dependencyDependency : remoteExtension.getDependencies()) {
                installExtension(new ExtensionId(dependencyDependency.getName(), dependencyDependency.getVersion()),
                    true);
            }

            LocalExtension localExtension = this.localExtensionRepository.installExtension(remoteExtension, dependency);

            // TODO: inject extension, what about some kind of ExtensionLoader with the ExtensionType string as role
            // hint ?
            // We should probably change the type for a plain String to make possible to support any "type" of
            // extension, pretty sure we would have use case for this.
            if (localExtension.getType() == ExtensionType.PAGES) {
                // TODO import xar
            } else if (localExtension.getType() == ExtensionType.JAR) {
                // TODO load jar components
            }

            // etc.
        } catch (ResolveException e) {
            throw new InstallException("Failed to resolve extension", e);
        }
    }

    public void uninstallExtension(ExtensionId extensionId) throws UninstallException
    {
        try {
            LocalExtension extension = this.localExtensionRepository.getLocalExtension(extensionId);

            this.localExtensionRepository.uninstallExtension(extension);
        } catch (ResolveException e) {
            throw new UninstallException("Failed to resolve extension", e);
        }
    }
}
