/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.util;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MiniCrypter {

    private static final String algorithm = "Blowfish";    // or "DES", "DESede"
    private static final String defaultKeyString = "MDWInternal";
    private static SecretKey defaultKey = null;

    /**
     * Encrypt a string using a default key
     * @param input data to encrypt
     * @return encrypted string
     */
    public static String encrypt(String input) {
        try {
            return encrypt(input, null);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Encrypt a string
     * @param input data to encrypt
     * @param strkey a default key will be used when passing null in
     * @return encrypted string
     */
    protected static String encrypt(String input, String strkey)
    throws GeneralSecurityException {
        SecretKey key;
        if (strkey!=null) key = new SecretKeySpec(strkey.getBytes(), algorithm);
        else {
            if (defaultKey==null) defaultKey =
                new SecretKeySpec(defaultKeyString.getBytes(), algorithm);
            key = defaultKey;
        }
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] inputBytes = input.getBytes();
        byte[] encrypted = cipher.doFinal(inputBytes);
        return encodeAlpha(encrypted);
    }

    /**
     * Decrypt a string using the default key
     * @param encrypted encrypted string to be decrypted
     * @return decrypted original data
     */
    public static String decrypt(String encrypted) {
        try {
            return decrypt(encrypted, null);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypt a string
     * @param encrypted encrypted string to be decrypted
     * @param strkey a default key will be used when passing null in
     * @return decrypted original data
     */
    protected static String decrypt(String encrypted, String strkey)
    throws GeneralSecurityException {
        SecretKey key;
        if (strkey!=null) key = new SecretKeySpec(strkey.getBytes(), algorithm);
        else {
            if (defaultKey==null) defaultKey =
                new SecretKeySpec(defaultKeyString.getBytes(), algorithm);
            key = defaultKey;
        }
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] encryptionBytes = decodeAlpha(encrypted);
        byte[] recoveredBytes = cipher.doFinal(encryptionBytes);
        String recovered = new String(recoveredBytes);
        return recovered;
    }

    public static byte[] decodeAlpha(String inputString) {
        int i, n = inputString.length();
         n = n/2;
         byte[] bytes = new byte[n];
         for (i=0; i<n; i++) {
             char hi = inputString.charAt(2*i);
             char lo = inputString.charAt(2*i+1);
             bytes[i] = (byte)(((hi-65)<<4)|(lo-65));
         }
         return bytes;
    }

    public static String encodeAlpha(byte[] inputBytes) {
         StringBuffer sb = new StringBuffer(inputBytes.length*2);
         for (int i=0; i<inputBytes.length; i++) {
             int hi = (inputBytes[i]&0xf0) >> 4;
             int lo = inputBytes[i] & 0xf;
             sb.append((char)(hi+65));
             sb.append((char)(lo+65));
         }
         return sb.toString();
    }

    public static void main(String[] args) {
        if (args.length == 0 || args.length > 2) {
            System.err.println("arguments: (d|e) stringToEncrypt/Decrypt");
        }
        else {
            boolean decryptMode = false;
            String input = args[0];
            if (args.length == 2)
            {
              decryptMode = args[0].equals("d") ? true : false;
              input = args[1];
            }
            if (decryptMode) {
                String decrypted = MiniCrypter.decrypt(input);
                System.out.println("decrypted: '" + decrypted + "'");
                String encrypted = MiniCrypter.encrypt(decrypted);
                System.out.println("encrypted: '" + encrypted + "'");
            }
            else {
                String encrypted = MiniCrypter.encrypt(input);
                System.out.println("encrypted: '" + encrypted + "'");
                String decrypted = MiniCrypter.decrypt(encrypted);
                System.out.println("decrypted: '" + decrypted + "'");
            }
        }
    }
}

