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

import java.io.File;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

public class HmacSha1Signature {

    //TODO Add more as required
    public static final String ALGORITHM = "HmacSHA1";


    public static void main(String[] args) {
        System.out.println("16e815d41ae493afca08ec4b8428f79e54dbeeba");
        try {
            //byte[] payloadBytes = Files.readAllBytes(new File("C:/workspaces/mdw6/mdw-common/src/com/centurylink/mdw/util/pay-output.json").toPath());
            byte[] payloadBytes = Files.readAllBytes(new File("C:/Users/aa56486/Downloads/webhook.json").toPath());
            String hexEncoded = HmacSha1Signature.getHMACHexdigestSignature(payloadBytes,"mdwcoreteam");
            System.out.println(hexEncoded);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }
    public static String getHMACHexdigestSignature (byte[] payloadBytes, String key) {
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), ALGORITHM);
        String hexEncoded = "";
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(payloadBytes);
            hexEncoded = Hex.encodeHexString(rawHmac);
        }
        catch (InvalidKeyException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hexEncoded;
    }
}