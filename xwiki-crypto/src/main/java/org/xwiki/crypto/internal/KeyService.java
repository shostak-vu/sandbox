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
package org.xwiki.crypto.internal;

import java.security.GeneralSecurityException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

import org.bouncycastle.jce.netscape.NetscapeCertRequest;
import org.xwiki.crypto.data.XWikiX509Certificate;
import org.xwiki.crypto.data.XWikiX509KeyPair;
import org.xwiki.crypto.data.internal.DefaultXWikiX509KeyPair;

/**
 * Service allowing a user to create keys and X509 certificates.
 * 
 * @version $Id$
 * @since 2.5
 */
public class KeyService
{
    /** Used for the actual key making, also holds any secrets. */
    private final Keymaker keymaker = new Keymaker();

    /**
     * @param spkacSerialization a <a href="http://en.wikipedia.org/wiki/Spkac">SPKAC</a> Certificate Signing Request
     * @param daysOfValidity number of days before the certificate should become invalid.
     * @param webID the URL of the user's page. Used for FOAFSSL compatabulity.
     * @param userName the String serialization of the user's page name.
     * @return 2 certificartes, one a client cert and the other an authority cert which signed the client cert.
     * @see org.xwiki.crypto.CryptoService#certsFromSpkac(String, int)
     */
    public XWikiX509Certificate[] certsFromSpkac(final String spkacSerialization,
                                                 final int daysOfValidity,
                                                 final String webID,
                                                 final String userName)
        throws GeneralSecurityException
    {
        if (spkacSerialization == null) {
            throw new InvalidParameterException("SPKAC parameter is null");
        }
        NetscapeCertRequest certRequest = new NetscapeCertRequest(Convert.stringToBytes(spkacSerialization));

        // Determine the webId by asking who's creating the cert (needed only for FOAFSSL compatibility)
        String userName = userDocUtils.getCurrentUser();
        String webID = userDocUtils.getUserDocURL(userName);

        return this.keymaker.makeClientAndAuthorityCertificates(certRequest.getPublicKey(),
                                                                   daysOfValidity,
                                                                   true,
                                                                   webID,
                                                                   userName);
    }

    /**
     * @param daysOfValidity number of days before the certificate should become invalid.
     * @param webID the URL of the user's page. Used for FOAFSSL compatabulity.
     * @param userName the String serialization of the user's page name.
     * @return a certificate and matching private key in an XWikiX509KeyPair object.
     * @see org.xwiki.crypto.CryptoService#newCertAndPrivateKey(int)
     */
    public XWikiX509KeyPair newCertAndPrivateKey(final int daysOfValidity,
                                                 final String webID,
                                                 final String userName)
    {
        // Determine the webId by asking who's creating the cert (needed only for FOAFSSL compatibility)
        String userName = userDocUtils.getCurrentUser();
        String webID = userDocUtils.getUserDocURL(userName);

        KeyPair pair = this.keymaker.newKeyPair();

        // In this case the non-repudiation bit is cleared because the private key is made on the server 
        // which is less secure.
        X509Certificate certificate = this.keymaker.makeClientCertificate(pair.getPublic(),
                                                                        pair,
                                                                        daysOfValidity,
                                                                        false,
                                                                        webID,
                                                                        userName);

        return new DefaultXWikiX509KeyPair(pair.getPrivate(), new XWikiX509Certificate(certificate));
    }
}