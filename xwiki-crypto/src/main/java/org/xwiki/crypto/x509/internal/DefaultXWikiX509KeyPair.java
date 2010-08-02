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
package org.xwiki.crypto.x509.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import org.xwiki.crypto.internal.Convert;
import org.xwiki.crypto.internal.SerializationUtils;
import org.xwiki.crypto.passwd.PasswordCiphertext;
import org.xwiki.crypto.passwd.PasswordCryptoService;
import org.xwiki.crypto.x509.XWikiX509Certificate;
import org.xwiki.crypto.x509.XWikiX509KeyPair;


/**
 * Wrapper for storing a {@link PrivateKey} and the corresponding {@link XWikiX509Certificate}.
 * 
 * @version $Id$
 * @since 2.5
 */
public class DefaultXWikiX509KeyPair implements XWikiX509KeyPair
{
    /** Serial version ID. */
    private static final long serialVersionUID = 1L;

    /** This will be at the beginning of the output from {@link #serializeAsBase64()}. */
    private static final transient String BASE64_HEADER = "-----BEGIN XWIKI CERTIFICATE AND PRIVATE KEY-----\n";

    /** This will be at the end of the output from {@link #serializeAsBase64()}. */
    private static final transient String BASE64_FOOTER = "-----END XWIKI CERTIFICATE AND PRIVATE KEY-----\n";

    /** @serial The algorithm of the private key. Something like "RSA". */
    private final String privateKeyAlgorithm;

    /** @serial Private key, base-64 encoded as a string then password encrypted. */
    private final byte[] passwordEncryptedPrivateKey;

    /** @serial User's certificate which is encoded to be serializable. */
    private final byte[] encodedCertificate;

    /** User's certificate. */
    private transient XWikiX509Certificate certificate;


    /**
     * Create new {@link XWikiX509KeyPair}.
     * 
     * @param certificate a certificate matching the private key, this will be stored unencrypted.
     * @param key the private key to use, this will be password encrypted.

     * @param password the password to require if a user wants to extract the private key.
     * @param passwordCryptoService the service to use for encrypting the private key so this object can safely be
     *                              serialized without allowing the private key to be read from the database.
     * @throws GeneralSecurityException if encrypting the private key fails.
     */
    public DefaultXWikiX509KeyPair(final X509Certificate certificate,
                                   final PrivateKey key,
                                   final String password,
                                   final PasswordCryptoService passwordCryptoService)
        throws GeneralSecurityException
    {
        this.privateKeyAlgorithm = key.getAlgorithm();
        this.certificate = new XWikiX509Certificate(certificate);
        this.encodedCertificate = certificate.getEncoded();

        // We have to convert the key into a PKCS8EncodedKeySpec because this can be made into a byte[] which will be
        // able to be password encrypted and will be safe to serialize.
        final KeyFactory converter = KeyFactory.getInstance(this.privateKeyAlgorithm);
        final byte[] encodedKey;
        try {
            encodedKey = converter.getKeySpec(key, PKCS8EncodedKeySpec.class).getEncoded();
        } catch (InvalidKeySpecException e) {
            // I don't think this is likely since we are encoding, not decoding.
            throw new GeneralSecurityException("Failed to encode private key", e);
        }

        // Ideally it would be possible to encrypt a byte[] and get a byte[] in return.
        // This is a workaround.
        final String encryptedPrivateKeyString =
            passwordCryptoService.encryptText(Convert.toBase64String(encodedKey), password);

        this.passwordEncryptedPrivateKey = Convert.fromBase64String(encryptedPrivateKeyString, "-----\n", "\n-----");
    }

    /**
     * Deserialize a key pair from a base64 encoded representation.
     * 
     * @param encoded a base64 encoded serialized key pair, as produced by {@link #serializeAsBase64()}
     * @return a key pair created from the given input
     * @throws IOException on errors
     */
    public static XWikiX509KeyPair deserializeFromBase64(String encoded) throws IOException
    {
        byte[] serialized = Convert.fromBase64String(encoded, BASE64_HEADER, BASE64_FOOTER);
        try {
            return (XWikiX509KeyPair) SerializationUtils.deserialize(serialized);
        } catch (ClassNotFoundException exception) {
            throw new IOException(exception.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#serializeAsBase64()
     */
    public String serializeAsBase64() throws IOException
    {
        return BASE64_HEADER
               + Convert.toChunkedBase64String(this.serialize())
               + BASE64_FOOTER;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#serialize()
     */
    public byte[] serialize() throws IOException
    {
        return SerializationUtils.serialize(this);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#hashCode()
     */
    @Override
    public int hashCode()
    {
        return this.encodedCertificate.hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (obj instanceof DefaultXWikiX509KeyPair) {
            final DefaultXWikiX509KeyPair other = (DefaultXWikiX509KeyPair) obj;
            if (!Arrays.equals(this.passwordEncryptedPrivateKey, other.passwordEncryptedPrivateKey)) {
                return false;
            }
            if (!this.privateKeyAlgorithm.equals(other.privateKeyAlgorithm)) {
                return false;
            }
            if (Arrays.equals(this.encodedCertificate, other.encodedCertificate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#toString()
     */
    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("XWikiPrivateCredential\n");
        builder.append("------------\n");
        try {
            builder.append(this.getCertificate().toString());
        } catch (GeneralSecurityException e) {
            builder.append("ERROR: Failed to load certificate: " + e.getMessage());
        }
        builder.append("Private key cannot be shown without a password.");
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#getCertificate()
     */
    public XWikiX509Certificate getCertificate() throws GeneralSecurityException
    {
        if (this.certificate == null) {
            final X509Certificate cert;
            try {
                final CertificateFactory converter = CertificateFactory.getInstance("X509");
                cert = 
                    (X509Certificate) converter.generateCertificate(new ByteArrayInputStream(this.encodedCertificate));
            } catch (CertificateException e) {
                throw new GeneralSecurityException(e);
            }

            this.certificate = new XWikiX509Certificate(cert);
        }
        return this.certificate;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#getPublicKey()
     */
    public PublicKey getPublicKey() throws GeneralSecurityException
    {
        return this.getCertificate().getPublicKey();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#getPrivateKey()
     */
    public PrivateKey getPrivateKey(final String password)
        throws GeneralSecurityException
    {
        final PasswordCiphertext ct;
        try {
            ct = (PasswordCiphertext) SerializationUtils.deserialize(this.passwordEncryptedPrivateKey);
        } catch (Exception e) {
            // IOException or ClassNotFoundException
            throw new GeneralSecurityException("Could not deserialize private key ", e);
        }

        final String privateKeyBase64 = ct.decryptText(password);
        if (privateKeyBase64 == null) {
            // Wrong password.
            throw new GeneralSecurityException("Could not decrypt private key, wrong password or corrupted file.");
        }
        final byte[] privateKeyBytes = Convert.fromBase64String(privateKeyBase64);
        final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
        final KeyFactory converter = KeyFactory.getInstance(this.privateKeyAlgorithm);
        return converter.generatePrivate(spec);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#getFingerprint()
     */
    public String getFingerprint() throws GeneralSecurityException
    {
        return this.getCertificate().getFingerprint();
    }
}
