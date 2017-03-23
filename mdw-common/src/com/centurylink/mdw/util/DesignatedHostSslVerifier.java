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

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

/**
 * Enables SSL trust of a specific hostname or IP with a self-signed certificate as
 * long as the CN equals the trusted host.
 */
public class DesignatedHostSslVerifier {

    private static List<String> sslVerificationHosts;

    /**
     * Idempotent.
     */
    public static synchronized void setupSslVerification(String host) throws Exception {
        if (sslVerificationHosts == null)
            sslVerificationHosts = new ArrayList<String>();

        if (!sslVerificationHosts.contains(host)) {

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            // initialize tmf with the default trust store
            tmf.init((KeyStore)null);

            // get the default trust manager
            X509TrustManager defaultTm = null;
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    defaultTm = (X509TrustManager) tm;
                    break;
                }
            }

            TrustManager[] trustManager = new TrustManager[] { new BlindTrustManager(defaultTm, host) };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustManager, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier defaultHv = HttpsURLConnection.getDefaultHostnameVerifier();
            HostnameVerifier hostnameVerifier = new DesignatedHostnameVerifier(defaultHv, host);
            HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);

            sslVerificationHosts.add(host);
        }
    }

}

class BlindTrustManager implements X509TrustManager {

    private X509TrustManager defaultTrustManager;
    private String trustedHost;

    BlindTrustManager(X509TrustManager defaultTrustManager, String trustedHost) {
        this.defaultTrustManager = defaultTrustManager;
        this.trustedHost = trustedHost;
    }

    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return defaultTrustManager.getAcceptedIssuers();
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        defaultTrustManager.checkClientTrusted(chain, authType);
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            X500Principal subject = chain[0].getSubjectX500Principal();
            X500Principal issuer = chain[0].getIssuerX500Principal();
            if (!trustedHost.equals(getCn(subject.getName())) || !trustedHost.equals(getCn(issuer.getName())))
                defaultTrustManager.checkServerTrusted(chain, authType);
    }

    private String getCn(String x500name) {
        String cn = null;
        for (String seg : x500name.split(",")) {
            if (seg.startsWith("CN="))
                cn = seg.substring(3);
        }
        return cn;
    }
}

class DesignatedHostnameVerifier implements HostnameVerifier {

    private HostnameVerifier defaultVerifier;
    private String designatedHost;

    DesignatedHostnameVerifier(HostnameVerifier defaultVerifier, String designatedHost) {
        this.defaultVerifier = defaultVerifier;
        this.designatedHost = designatedHost;
    }

    public boolean verify(String hostname, SSLSession session) {
        if (designatedHost.equals(hostname))
            return true;
        else
            return defaultVerifier.verify(hostname, session);
    }
}