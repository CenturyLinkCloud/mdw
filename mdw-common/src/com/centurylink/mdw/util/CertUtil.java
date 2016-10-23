/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

import com.centurylink.mdw.auth.CertificateChainInfo;
import com.centurylink.mdw.auth.CertificateHandler;

public class CertUtil {
    
    public static void main(String[] args) {
        CertUtil certUtil = new CertUtil();
        String help = "argument required: URL for certificate\n";
        if (args.length != 1) {
            System.out.println(help);
        }
        else if ("help".equals(args[0])) {
            System.out.println(help);
        }
        else {
            // ldap://ldapt.dev.qintra.com:1636/dc=mnet,dc=qintra,dc=com
            String url = args[0];
            certUtil.importCertificate(url);
        }
    }
    
    private void importCertificate(String url) {
        
        // try and import the certificate
        try
        {
          String lowerCaseUrl = url.toLowerCase();
          int port = 443;  // assuming https default port
          if (lowerCaseUrl.startsWith("http://"))
              port = 80;
          if (lowerCaseUrl.startsWith("ldap://"))
            port = 389;
          else if (lowerCaseUrl.startsWith("ldaps://"))
            port = 636;
          else
            throw new MalformedURLException("Bad URL: " + url);
          
          int hostBegin = url.indexOf("://");
          int hostEnd = url.indexOf(":", hostBegin + 3);
          if (hostEnd < 0)
          {
            hostEnd = url.indexOf("/", hostBegin + 3);
            if (hostEnd < 0)
              hostEnd = url.length();
          }
          else
          {
            int portEnd = url.indexOf("/", hostEnd + 1);
            if (portEnd < 0)
              portEnd = url.length();
            try
            {
              port = Integer.parseInt(url.substring(hostEnd + 1, portEnd));
            }
            catch (NumberFormatException ex)
            {
              throw new MalformedURLException("Bad URL: " + url);
            }
          }
          String host = url.substring(hostBegin + 3, hostEnd);
          
          CertificateHandler certHandler = new CertificateHandler();
          CertificateChainInfo chainInfo = certHandler.getCertificateInfo(host, port);
          System.out.println("Certificate Chain:");
          for (int i = 0; i < chainInfo.getCertificateChain().length; i++)
              System.out.println(chainInfo.getCertSummary(i));
          System.out.print("Do you trust the preceding certificate chain presented by the LDAP server? ");
          BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
          String confirm = null;
          confirm = br.readLine();
          if ("yes".equalsIgnoreCase(confirm) || "y".equalsIgnoreCase(confirm))
          {
            // import the certificate(s)
            for (int i = 0; i < chainInfo.getCertificateChain().length; i++)
              certHandler.importCertificateChain(chainInfo.getCertificateChain()[i], host + "-" + (i + 1));
          }
          System.out.println("Certificate(s) imported.");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }        

}
