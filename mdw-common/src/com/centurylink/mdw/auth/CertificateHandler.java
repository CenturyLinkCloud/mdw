/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.centurylink.mdw.auth.CertificateChainInfo.Status;

public class CertificateHandler {
    
    private char[] keystorePassphrase;
    
    public CertificateHandler() {
        keystorePassphrase = "changeit".toCharArray();
    }
    
    public CertificateHandler(String keystorePassphrase) {
        this.keystorePassphrase = keystorePassphrase.toCharArray();
    }
    
    public CertificateChainInfo getCertificateInfo(String host, int port) throws IOException, MdwSecurityException {

        CertificateChainInfo certChainInfo = new CertificateChainInfo();
        
        try {

            SSLContext context = SSLContext.getInstance("TLS");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(getKeyStore());
            X509TrustManager defaultTrustManager = (X509TrustManager)tmf.getTrustManagers()[0];
            SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
            context.init(null, new TrustManager[]{tm}, null);
            SSLSocketFactory factory = context.getSocketFactory();
    
            // open an SSL connection
            SSLSocket socket = null;
            try {
                socket = (SSLSocket)factory.createSocket(host, port);
                socket.setSoTimeout(10000);
                // starting ssl handshake
                socket.startHandshake();
                certChainInfo.setStatus(Status.TRUSTED);
            }
            catch (SSLException ex) {
                certChainInfo.setStatus(Status.UNTRUSTED);
                certChainInfo.setException(ex);
            }
            finally {
                if (socket != null)
                    socket.close();
            }
    
            certChainInfo.setCertificateChain(tm.chain);
            if (certChainInfo.getCertificateChain() == null) {
                certChainInfo.setStatus(Status.ERROR);
                certChainInfo.setMessage("Could not obtain server certificate chain");
            }
            
            return certChainInfo;
        }
        catch (GeneralSecurityException ex) {
            throw new MdwSecurityException(ex.getMessage(), ex);
        }
    }
    
    public void importCertificateChain(X509Certificate cert, String alias) throws IOException, MdwSecurityException {

        KeyStore ks = getKeyStore();

        OutputStream out = null;
        try {
            ks.setCertificateEntry(alias, cert);
            out = new FileOutputStream(findKeyStoreFile());
            ks.store(out, keystorePassphrase);
        }
        catch (GeneralSecurityException ex) {
            throw new MdwSecurityException(ex.getMessage(), ex);
        }
        finally {
            if (out != null)
                out.close();
        }
    }

    private KeyStore getKeyStore() throws IOException, MdwSecurityException {
        
        InputStream in = null;
        try {
            // load keystore
            in = new FileInputStream(findKeyStoreFile());
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(in, keystorePassphrase);
            return keystore;
        }
        catch (GeneralSecurityException ex) {
            throw new MdwSecurityException(ex.getMessage(), ex);
        }
        finally {
            if (in != null) {
                  in.close();
            }
        }
    }
    
    private File findKeyStoreFile() {
      File file = new File("jssecacerts");
      if (file.isFile() == false) {
          File dir = new File(System.getProperty("java.home") + "/lib/security");
          file = new File(dir, "jssecacerts");
          if (file.isFile() == false) {
            file = new File(dir, "cacerts");
          }
      }
      return file;
    }
    
    private class SavingTrustManager implements X509TrustManager {

        private final X509TrustManager tm;
        private X509Certificate[] chain;

        SavingTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        public X509Certificate[] getAcceptedIssuers() {
            throw new UnsupportedOperationException();
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
            throw new UnsupportedOperationException();
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
            this.chain = chain;
            tm.checkServerTrusted(chain, authType);
        }
    }
}
