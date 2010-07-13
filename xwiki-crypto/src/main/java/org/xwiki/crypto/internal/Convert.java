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

import java.io.UnsupportedEncodingException;

import org.bouncycastle.util.encoders.Base64;


/**
 * Utility class for various string conversions, such as base64 encoding and decoding and conversion
 * to byte array. Supports conversion of strings containing base64 encoded data. The conversion uses
 * UTF-8 because it is always available and base64 alphabet is a subset of UTF-8.
 * 
 * @version $Id$
 * @since 2.5
 */
public final class Convert
{
    /** Charset used for String <-> byte[] conversion. */
    private static final String CHARSET = "utf-8";

    /** Default line length for {@link #toChunkedBase64String(byte[])}. */
    private static final int DEFAULT_LINE_LENGTH = 64;

    /** New line separator. */
    private static final String NEWLINE = System.getProperty("line.separator", "\n");

    /**
     * Encode given data and return the base64 encoded result as string (no line breaks).
     * 
     * @param data the data to encode
     * @return base64 encoded data
     */
    public static String toBase64String(byte[] data)
    {
        try {
            return new String(toBase64(data), CHARSET);
        } catch (UnsupportedEncodingException exception) {
            // cannot happen
            throw new RuntimeException(exception);
        }
    }

    /**
     * Encode given data and return the base64 encoded result as string, chunking it into several lines
     * of the default length (64).
     * 
     * @param data the data to encode
     * @return base64 encoded data
     */
    public static String toChunkedBase64String(byte[] data)
    {
        return toChunkedBase64String(data, DEFAULT_LINE_LENGTH);
    }

    /**
     * Encode given data and return the base64 encoded result as string, chunking it into several lines
     * of the given length.
     * 
     * @param data the data to encode
     * @param lineLength maximal line length
     * @return base64 encoded data
     */
    public static String toChunkedBase64String(byte[] data, int lineLength)
    {
        StringBuilder result = new StringBuilder();
        String encoded = toBase64String(data);
        int begin = 0;
        int end = lineLength;
        while (end < encoded.length()) {
            result.append(encoded.substring(begin, end));
            result.append(NEWLINE);
            begin = end;
            end += lineLength;
        }
        result.append(encoded.substring(begin));
        result.append(NEWLINE);
        return result.toString();
    }

    /**
     * Encode given data and return the base64 encoded result as a byte array.
     * 
     * @param data the data to encode
     * @return base64 encoded data array
     */
    public static byte[] toBase64(byte[] data)
    {
        return Base64.encode(data);
    }

    /**
     * Decode the base64 encoded data represented as string.
     * 
     * @param base64Encoded base64 encoded data string
     * @return the decoded data array
     */
    public static byte[] fromBase64String(String base64Encoded)
    {
        try {
            return fromBase64(base64Encoded.getBytes(CHARSET));
        } catch (UnsupportedEncodingException exception) {
            // cannot happen
            throw new RuntimeException(exception);
        }
    }

    /**
     * Decode the base64 encoded data array.
     * 
     * @param base64Encoded base64 encoded data
     * @return the decoded data array
     */
    public static byte[] fromBase64(byte[] base64Encoded)
    {
        return Base64.decode(base64Encoded);
    }

    /**
     * Convert string to byte array using the same encoding as for base64 conversion.
     * 
     * @param string the string to convert
     * @return byte array containing the characters from the string
     */
    public static byte[] stringToBytes(String string)
    {
        try {
            return string.getBytes(CHARSET);
        } catch (UnsupportedEncodingException exception) {
            // cannot happen
            throw new RuntimeException(exception);
        }
    }

    /**
     * Private default constructor to prevent instantiation.
     */
    private Convert()
    {
        // this class is not supposed to be instantiated
    }
}
