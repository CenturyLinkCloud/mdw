package com.centurylink.mdw.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

public class HmacSha1Signature {

    //TODO Add more as required
    public static final String ALGORITHM = "HmacSHA1";

    public static String getHMACHexdigestSignature (byte[] payloadBytes, String key)
    throws InvalidKeyException, NoSuchAlgorithmException {
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), ALGORITHM);
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(keySpec);
        byte[] rawHmac = mac.doFinal(payloadBytes);
        return Hex.encodeHexString(rawHmac);
    }
}