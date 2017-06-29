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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import com.centurylink.mdw.util.file.FileHelper;


/**
 * The <tt>HmacSha1Signature</tt> shows how to calculate
 * a message authentication code using HMAC-SHA1 algorithm.
 */
public class HmacSha1Signature {
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    public static String calculateRFC2104HMAC(String data, String key)
        throws SignatureException, NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, IOException
    {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        // Compute the hmac on input data bytes
        byte[] payload = Files.readAllBytes(Paths.get(new File("C:/workspaces/mdw6/mdw-common/src/com/centurylink/mdw/util/payload.json").getPath()));
        System.out.println("Payload read from file:\n" + new String(payload, "UTF-8"));
        Files.write(Paths.get(new File("C:/workspaces/mdw6/mdw-common/src/com/centurylink/mdw/util/pay-output.json").getPath()), payload, java.nio.file.StandardOpenOption.WRITE);

        byte[] rawHmac = mac.doFinal(payload);

        // Convert raw bytes to Hex
        byte[] hexBytes = new Hex().encode(rawHmac);

        //  Covert array of Hex bytes to a String
        return new String(hexBytes, "UTF-8");
    }

    public static void main(String[] args) throws Exception {
        String payload = FileHelper.getFileContents("C:/workspaces/mdw6/mdw-common/src/com/centurylink/mdw/util/payload.json");

        //System.out.println("Payload copied from GitHub" + payload);

        String hmacApache = "sha1=" + org.apache.commons.codec.digest.HmacUtils.hmacSha1Hex("mdwcoreteam", payload);
        System.out.println(hmacApache);

        String hmac = "sha1=" + calculateRFC2104HMAC(payload, "mdwcoreteam");

        System.out.println(hmac);
        assert hmac.equals("sha1=ff6e51f82099331750ee0a6030a44f56cdd47cca");

    }
}