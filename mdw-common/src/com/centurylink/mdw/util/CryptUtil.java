/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

import java.security.GeneralSecurityException;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class CryptUtil {
    
    public static final int MAX_LENGTH = 24;
    
    private static SecretKey key64 = new SecretKeySpec(new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 }, "Blowfish");
    private static Cipher cipher;

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        
        try {
            cipher = Cipher.getInstance("Blowfish/ECB/NoPadding");
        }
        catch (GeneralSecurityException ex) {
            ex.printStackTrace();
        }
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
            try {
                if (decryptMode) {
                    String decrypted = CryptUtil.decrypt(input);
                    System.out.println("decrypted: '" + decrypted + "'");
                    String encrypted = CryptUtil.encrypt(decrypted);
                    System.out.println("encrypted: '" + encrypted + "'");
                }
                else {
                    String encrypted = CryptUtil.encrypt(input);
                    System.out.println("encrypted: '" + encrypted + "'");
                    String decrypted = CryptUtil.decrypt(encrypted);
                    System.out.println("decrypted: '" + decrypted + "'");
                }
            }
            catch (GeneralSecurityException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static String encrypt(String input) 
    throws GeneralSecurityException {
        String padded = pad(input, MAX_LENGTH);
        cipher.init(Cipher.ENCRYPT_MODE, key64);
        byte[] encrypted = cipher.doFinal(padded.getBytes());
        return encodeBase64(encrypted);
    }
    
    public static String decrypt(String input)
    throws GeneralSecurityException {
        byte[] decoded = decodeBase64(input);
        cipher.init(Cipher.DECRYPT_MODE, key64);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted).trim();
    }
    
    private static byte[] decodeBase64(String inputString) {
        return Base64.decodeBase64(inputString.getBytes());
    }

    private static String encodeBase64(byte[] inputBytes) {
        return new String(Base64.encodeBase64(inputBytes));
    }
    
    private static String pad(String input, int len) {
//        return StringUtils.rightPad(input, MAX_LENGTH);
        StringBuffer padded = new StringBuffer(input);
        for (int i = 0; i < len - input.length(); i++) {
            padded.append(" ");
        }
        return padded.toString();
    }
    
}
