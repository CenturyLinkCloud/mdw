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
package com.centurylink.mdw.auth;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLException;

public class CertificateChainInfo {
    
    public enum Status {
        TRUSTED,
        UNTRUSTED,
        ERROR
    }
    
    private Status status;
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    private String message;
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    private SSLException exception;
    public SSLException getException() { return exception; }
    public void setException(SSLException ex) { this.exception = ex; }
    
    private X509Certificate[] certificateChain;
    public X509Certificate[] getCertificateChain() { return certificateChain; }
    public void setCertificateChain(X509Certificate[] chain) { this.certificateChain = chain; }
    

    public String getSummary() throws MdwSecurityException {
        StringBuilder info = new StringBuilder();
        info.append("Server sent ").append(certificateChain.length).append(" certificate(s):");
        for (int i = 0; i < certificateChain.length; i++) {
            info.append(" ").append(i + 1).append("\n");
            info.append(getCertSummary(i)).append("\n");
        }
        return info.toString();
    }
    
    public String getCertSummary(int certIndex) throws MdwSecurityException {
        StringBuilder info = new StringBuilder();
        
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            MessageDigest md5 = MessageDigest.getInstance("MD5");
    
            X509Certificate cert = certificateChain[certIndex];
            info.append("Subject: ").append(cert.getSubjectDN()).append("\n");
            info.append("Issuer: ").append(cert.getIssuerDN()).append("\n");
            sha1.update(cert.getEncoded());
            info.append("   SHA1: ").append(toHexString(sha1.digest())).append("\n");
            md5.update(cert.getEncoded());
            info.append("   MD5: ").append(toHexString(md5.digest())).append("\n");
            return info.toString();
        }
        catch (GeneralSecurityException ex) {
            throw new MdwSecurityException(ex.getMessage(), ex);
        }
    }
    
    private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

    private String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (int b : bytes) {
            b &= 0xff;
            sb.append(HEXDIGITS[b >> 4]);
            sb.append(HEXDIGITS[b & 15]);
            sb.append(' ');
        }
        return sb.toString();
    }
    
}
