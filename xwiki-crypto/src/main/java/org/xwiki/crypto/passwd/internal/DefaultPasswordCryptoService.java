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
package org.xwiki.crypto.passwd.internal;

import java.util.Properties;
import java.io.IOException;
import java.security.GeneralSecurityException;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import org.xwiki.crypto.internal.Convert;
import org.xwiki.crypto.internal.SerializationUtils;
import org.xwiki.crypto.passwd.PasswordCryptoService;
import org.xwiki.crypto.passwd.PasswordCiphertext;
import org.xwiki.crypto.passwd.KeyDerivationFunction;
import org.xwiki.crypto.passwd.PasswordVerificationFunction;
import org.xwiki.crypto.passwd.PasswordCryptoServiceConfiguration;


/**
 * This class allows the user to encrypt and decrypt text using a password
 * ciphertext might look as follows:
 * <pre>
 * -----BEGIN PASSWORD CIPHERTEXT-----
 * rO0ABXNyADhvcmcueHdpa2kuY3J5cHRvLnBhc3N3ZC5pbnRlcm5hbC5DQVNUNVBh
 * c3N3b3JkQ2lwaGVydGV4dGBjanGyQ5IzAgAAeHIAO29yZy54d2lraS5jcnlwdG8u
 * cGFzc3dkLmludGVybmFsLkFic3RyYWN0UGFzc3dvcmRDaXBoZXJ0ZXh0wxB+AJ0R
 * Z6ACAAJbAApjaXBoZXJ0ZXh0dAACW0JMAAtrZXlGdW5jdGlvbnQAL0xvcmcveHdp
 * a2kvY3J5cHRvL3Bhc3N3ZC9LZXlEZXJpdmF0aW9uRnVuY3Rpb247eHB1cgACW0Ks
 * 8xf4BghU4AIAAHhwAAABGPyIkxLgotOse8w/uihvcuHCV9XdFdKzQ7KQDtr0N6Tx
 * /cG7npgtTF6+9FAtONY7lg==
 * -----END PASSWORD CIPHERTEXT-----
 * </pre>
 *
 * Users can also protect a password or other secret information so that it can be verified but not
 * recovered. The output is a string of base-64 text without any header or footer as with encrypt.
 *
 * @version $Id$
 * @since 2.5
 */
@Component
public class DefaultPasswordCryptoService implements PasswordCryptoService
{
    /** Text which indicates the beginning of password based ciphertext. */
    private final String ciphertextHeader = "-----BEGIN PASSWORD CIPHERTEXT-----\n";

    /** Text which indicates the end of password based ciphertext. */
    private final String ciphertextFooter = "-----END PASSWORD CIPHERTEXT-----";

    /** For deciding which classes to use for cipher and key functions. */
    @Requirement
    private PasswordCryptoServiceConfiguration config;

    /**
     * {@inheritDoc}
     * @see org.xwiki.crypto.passwd.PasswdCryptoService#encryptText(String, String)
     */
    public synchronized String encryptText(final String plaintext, final String password)
        throws GeneralSecurityException
    {
        try {
            final KeyDerivationFunction keyFunction = 
                this.config.getKeyDerivationFunctionClassForEncryption().newInstance();
            final Properties keyFunctionProps =
                this.config.getKeyDerivationFunctionPropertiesForEncryption();

            // need to set derivedKeyLength property based on which ciphertext is used.
            final PasswordCiphertext ciphertext = this.config.getCipherClass().newInstance();
            keyFunctionProps.setProperty("derivedKeyLength",
                                         Integer.valueOf(ciphertext.getRequiredKeySize()).toString());

            keyFunction.init(keyFunctionProps);
            ciphertext.init(plaintext, password, keyFunction);

            try {
                return   this.ciphertextHeader
                       + Convert.toChunkedBase64String(ciphertext.serialize())
                       + this.ciphertextFooter;
            } catch (IOException e) {
                throw new GeneralSecurityException("Failed to serialize ciphertext", e);
            }
        } catch (Exception e) {
            throw new GeneralSecurityException("Failed to encrypt text", e);
        }
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.crypto.passwd.PasswdCryptoService#decryptText(String, String)
     */
    public synchronized String decryptText(final String containingBase64Ciphertext, final String password)
        throws GeneralSecurityException
    {
        final byte[] serial = Convert.fromBase64String(containingBase64Ciphertext,
                                                       this.ciphertextHeader,
                                                       this.ciphertextFooter);
        try {
            final PasswordCiphertext ciphertext = (PasswordCiphertext) SerializationUtils.deserialize(serial);
            return ciphertext.decryptText(password);
        } catch (IOException e) {
            throw new GeneralSecurityException("Failed to deserialize ciphertext", e);
        } catch (ClassNotFoundException e) {
            throw new GeneralSecurityException("Apparently this ciphertext was encrypted using a cipher which is not "
                                               + "available on this installation, was this imported from a newer "
                                               + "version?", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.crypto.passwd.PasswdCryptoService#protectPassword(String)
     */
    public String protectPassword(final String password)
        throws GeneralSecurityException
    {
        try {
            final KeyDerivationFunction keyFunction = 
                this.config.getKeyDerivationFunctionClassForPasswordVerification().newInstance();
            final Properties keyFunctionProps =
                this.config.getKeyDerivationFunctionPropertiesForPasswordVerification();

            keyFunction.init(keyFunctionProps);

            final PasswordVerificationFunction passwordFunction = 
                this.config.getPasswordVerificationFunctionClass().newInstance();

            passwordFunction.init(keyFunction, Convert.stringToBytes(password));
            return Convert.toBase64String(passwordFunction.serialize());
        } catch (Exception e) {
            throw new GeneralSecurityException("Unable to protect password", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.crypto.passwd.PasswdCryptoService#isPasswordCorrect(String, String)
     */
    public boolean isPasswordCorrect(final String password, final String protectedPassword)
        throws GeneralSecurityException
    {
        try {
            final byte[] serial = Convert.fromBase64String(protectedPassword);

            final PasswordVerificationFunction pvf =
                (PasswordVerificationFunction) SerializationUtils.deserialize(serial);

            return pvf.isPasswordCorrect(Convert.stringToBytes(password));
        } catch (Exception e) {
            throw new GeneralSecurityException("Unable to verify password", e);
        }
    }
}
