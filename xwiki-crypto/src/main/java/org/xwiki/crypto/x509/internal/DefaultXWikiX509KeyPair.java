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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.crypto.Cipher;

import org.xwiki.crypto.x509.XWikiX509Certificate;
import org.xwiki.crypto.x509.XWikiX509KeyPair;
import org.xwiki.crypto.internal.Convert;


/**
 * Wrapper for storing a {@link PrivateKey} and the corresponding {@link XWikiX509Certificate}.
 * 
 * @version $Id$
 * @since 2.5
 */
public final class DefaultXWikiX509KeyPair implements XWikiX509KeyPair
{
    /** Marks the beginning of a private credential in a string. */
    private static final String BEGIN_CREDENTIAL = "-----BEGIN PKCS12-----";

    /** Marks the end of a private credential in a string. */
    private static final String END_CREDENTIAL = "-----END PKCS12-----";

    /** Hash algorithm used to mangle the key store password. */
    private static final String HASH_ALGORITHM = "SHA-256";

    /** The encryption provider (Bouncycastle). */
    private final String provider = "BC";

    /** The type of key store. */
    private final String keyStoreType = "PKCS12-3DES-3DES";

    /** Binary form of the PKCS#12 container which this class wraps. */
    private final byte[] pkcs12Bytes;

    /** 
     * Array of certificates starting with the cert matching the private key at index 0 and 
     * ascending to the root certificate authority at the highest index.
     */
    private final XWikiX509Certificate[] certChain;

    /** Maximal allowed key length in bit. By default, Java restricts it to 128 bit due to US export regulations. */
    private final int maxKeyLength;

    /**
     * Create new {@link XWikiX509KeyPair}.
     * 
     * @param key the private key to use.
     * @param certificates an array containing at index 0 the certificate matching the private key, and at indexes
     *                     above that index, the chain of certificates ascending up to the root certificate authority.
     * @param password the password to require if a user wants to extract the private key.
     *                 This password will also be applied to the PKCS#12 output if this is exported.
     *                 The strength of the PKCS#12 cryptography grows with the length of the password and symmetrical 
     *                 encryption using keys larger than 128 bits is not allowed by Java unless you install Unlimited 
     *                 Strength Jurisdiction Policy Files available on the Sun Microsystems website.
     * @throws GeneralSecurityException if initializing or saving of the key store fails.
     */
    public DefaultXWikiX509KeyPair(final PrivateKey key,
                                   final String password,
                                   final X509Certificate... certificates)
        throws GeneralSecurityException
    {
        if (certificates == null || certificates.length < 1 || password == null || key == null) {
            throw new IllegalArgumentException("You must provide a private key, a password and "
                                               + "at least one certificate.");
        }
        final Certificate[] nonX509Certs = new Certificate[certificates.length];
        for (int i = 0; i < certificates.length; i++) {
            nonX509Certs[i] = certificates[i];
        }

        this.maxKeyLength = calculateMaximalKeyLength();
        final KeyStore store = KeyStore.getInstance(keyStoreType, provider);

        try {
            store.load(null, null);
            store.setKeyEntry("",
                              key,
                              null,
                              nonX509Certs);
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            store.store(os, preparePassword(password));
            this.pkcs12Bytes = os.toByteArray();
        } catch (IOException e) {
            // If it's a wrapped illegal key size exception then we should give the lecture about import regulations
            // and Java Cryptography Extension Policy Files.
            if (e.getMessage().contains("encrypting data - java.security.InvalidKeyException: Illegal key size")) {
                throw new GeneralSecurityException(e.getMessage() + "\nThis is probably because you are trying to use "
                                                   + "a password longer than 7 characters and you have not imported "
                                                   + "the Java Cryptography Extension Unlimited Strength Jurisdiction "
                                                   + "Policy Files.\nSome localities have import regulations on "
                                                   + "cryptography with keys larger than a given size. PKCS#7 keys "
                                                   + "grow with the length of the password and as a result passwords "
                                                   + "longer than 7 characters are not possible without installing the "
                                                   + "Unlimited Strength Jurisdiction Policy Files which are available "
                                                   + "on the Sun Microsystems website.", e);
            }
            throw new GeneralSecurityException("Failed to initialize or save key store", e);
        } finally {
            //DefaultXWikiX509KeyPair.cleanStore(store);
        }

        this.certChain = new XWikiX509Certificate[certificates.length];
        for (int i = 0; i < certificates.length; i++) {
            this.certChain[i] = new XWikiX509Certificate(certificates[i]);
        }
    }

    /**
     * Create new {@link XWikiX509KeyPair} from a PKCS#12 store in base 64 String format.
     * 
     * @param pkcs12Base64String this String will be searched for {@link BEGIN_CREDENTIAL} and {@link END_CREDENTIAL}
     *                           anything between those will be assumed to be base64 encoded PKCS#12 data representing
     *                           the credential to import. If there are multiple credentials, only the first will be
     *                           parsed.
     * @param password the password to use to open the PKCS#12 store.
     * @throws GeneralSecurityException if reading the PKCS#12 store fails.
     */
    public DefaultXWikiX509KeyPair(final String pkcs12Base64String,
                                   final String password)
        throws GeneralSecurityException
    {
        this(Convert.fromBase64String(pkcs12Base64String, BEGIN_CREDENTIAL, END_CREDENTIAL), password);
    }

    /**
     * Create new {@link XWikiX509KeyPair} from a PKCS#12 store.
     * 
     * @param pkcs12Bytes the PKCS#12 to get the certificate and private key from.
     *                    the certificate in this container must be an {@link X509Certificate}
     * @param password the password to use to open the PKCS#12 store, 
     *                 this password will also be demanded to get at the private key.
     * @throws GeneralSecurityException if loading the key store fails.
     */
    public DefaultXWikiX509KeyPair(final byte[] pkcs12Bytes,
                                   final String password)
        throws GeneralSecurityException
    {
        this.maxKeyLength = calculateMaximalKeyLength();
        final KeyStore store = KeyStore.getInstance(keyStoreType, provider);
        X509Certificate[] certificates = null;

        try {
            store.load(new ByteArrayInputStream(pkcs12Bytes), preparePassword(password));
            certificates = (X509Certificate[]) store.getCertificateChain("");
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Only PKCS#12 containers with X509Certificates are accepted.");
        } catch (IOException e) {
            throw new GeneralSecurityException("Failed to load key store to parse PKCS#12 container", e);
        } finally {
            DefaultXWikiX509KeyPair.cleanStore(store);
        }

        this.certChain = new XWikiX509Certificate[certificates.length];
        for (int i = 0; i < certificates.length; i++) {
            this.certChain[i] = new XWikiX509Certificate(certificates[i]);
        }

        this.pkcs12Bytes = pkcs12Bytes;
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#hashCode()
     */
    @Override
    public int hashCode()
    {
        return getCertificate().hashCode();
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (obj instanceof DefaultXWikiX509KeyPair) {
            return this.pkcs12Bytes.equals(((DefaultXWikiX509KeyPair) obj).pkcs12Bytes);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#toString()
     */
    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("XWikiPrivateCredential\n");
        builder.append("------------\n");
        builder.append(getCertificate().toString());
        builder.append("Private key cannot be shown without a password.");
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#getCertificates()
     */
    public XWikiX509Certificate[] getCertificates()
    {
        return this.certChain;
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#getCertificate()
     */
    public XWikiX509Certificate getCertificate()
    {
        return this.getCertificates()[0];
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#getPublicKey()
     */
    public PublicKey getPublicKey()
    {
        return getCertificate().getPublicKey();
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#getPrivateKey()
     * @throws GeneralSecurityException if loading the key store fails.
     */
    public PrivateKey getPrivateKey(final String password)
        throws GeneralSecurityException
    {
        final KeyStore store = KeyStore.getInstance(keyStoreType, provider);
        try {
            store.load(new ByteArrayInputStream(pkcs12Bytes), preparePassword(password));
            return (PrivateKey) store.getKey("", null);
        } catch (IOException e) {
            throw new GeneralSecurityException("Failed to load key store to get the private key", e);
        } finally {
            DefaultXWikiX509KeyPair.cleanStore(store);
        }
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#getFingerprint()
     */
    public String getFingerprint()
    {
        return getCertificate().getFingerprint();
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#toPKCS12()
     */
    public byte[] toPKCS12()
    {
        return this.pkcs12Bytes;
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.crypto.data.XWikiPrivateCredential#toBase64PKCS12()
     */
    public String toBase64PKCS12()
    {
        return BEGIN_CREDENTIAL
             + Convert.getNewline()
             + Convert.toChunkedBase64String(this.pkcs12Bytes)
             + END_CREDENTIAL
             + Convert.getNewline();
    }

    /**
     * Make sure there are no keys left in the key store.
     * The store should not leak but by deleting the entries here, we can rest easier.
     *
     * @param store the KeyStore to clean of all it's keys.
     */
    private static void cleanStore(final KeyStore store)
    {
        try {
            final Enumeration<String> aliases = store.aliases();
            while (aliases.hasMoreElements()) {
                store.deleteEntry(aliases.nextElement());
            }
        } catch (KeyStoreException e) {
            // there are 2 cases where exception can be thrown
            // 1. the key store is not initialized, we cannot clean it anyway
            // 2. a "no such entry" exception, safe to ignore since the key is still dropped
            // we ignore the errors, because they hide the real error when this method is used
            // in finally and some operation on the key store fails
        }
    }

    /**
     * Convert the given password string to the character array as required by the key store. This method
     * uses a cryptographic hash function to improve the password entropy.
     * <p>
     * Note that the length of the password is limited to 128 bit unless you install Unlimited 
     * Strength Jurisdiction Policy Files available on the Sun Microsystems website.</p>
     * 
     * @param password the string representation of the password
     * @return character array with the password hash (do not convert to string, invalid UTF-16)
     * @throws GeneralSecurityException if something goes wrong (very unlikely)
     */
    private char[] preparePassword(String password) throws GeneralSecurityException
    {
        MessageDigest hash = MessageDigest.getInstance(HASH_ALGORITHM);
        // the key length should be strictly smaller than the maximal length
        int length = this.maxKeyLength - 1;
        if (length > hash.getDigestLength() * Byte.SIZE) {
            length = hash.getDigestLength() * Byte.SIZE;
        }
        length /= Character.SIZE;
        if (length < 1) {
            throw new GeneralSecurityException("Invalid maximal key length");
        }
        byte[] buf = hash.digest(Convert.stringToBytes(password));
        char[] result = new char[length];
        for (int i = 0; i < length; i++) {
            // assumes that Character.SIZE = 2 * Byte.SIZE
            // may produce invalid UTF-16
            result[i] = (char) (((char) buf[i * 2] << Byte.SIZE) | buf[(i * 2) + 1]);
        }
        return result;
    }

    /**
     * Calculate the currently allowed maximal key length.
     * 
     * @return the maximal key length
     * @throws NoSuchAlgorithmException if the 3DES algorithm is not available (very, very unlikely)
     */
    private int calculateMaximalKeyLength() throws NoSuchAlgorithmException
    {
        return Cipher.getMaxAllowedKeyLength("3DES");
    }
}