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
package org.xwiki.signedscripts.internal;

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;

/**
 * An internal component used to store, load and modify certificates and key pairs stored in XWiki documents.
 * 
 * @version $Id$
 * @since 2.5
 */
@ComponentRole
public interface CryptoStorageUtils
{
    /**
     * Get the X509Certificate fingerprints for the named user.
     *
     * @param userName the string representation of the document reference for the user document.
     * @return A list of all of this user's authorized certificate fingerprints.
     */
    List<String> getCertificateFingerprintsForUser(final String userName);

    /**
     * Add a fingerprint to the list of certificate fingerprints of the given user.
     * 
     * @param userName reference to the user document
     * @param fingerprint the certificate fingerprint to add
     * @throws Exception on errors
     */
    void addCertificateFingerprint(String userName, String fingerprint) throws Exception;
}