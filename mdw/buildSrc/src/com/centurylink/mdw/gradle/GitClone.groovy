import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.BuildException

import java.net.URL
import java.io.File

import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git

import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.ArrayList
import java.util.List

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal

class GitClone extends DefaultTask {

    private Object fromUrl
    public void fromUrl(Object fromUrl) { this.fromUrl = fromUrl; }
    
    private Object into
    public void into(Object into) { this.into = into; }
    
    private String branch = 'master'
    public void branch(String branch) { this.branch = branch; }

    private String trustedHost
    public void trustedHost(String trustedHost) { this.trustedHost = trustedHost; }
    
    private static List<String> sslVerificationHosts

    @TaskAction
    def perform() {
        
        if (fromUrl == null)
            throw new IllegalArgumentException('Missing fromUrl')

        if (into == null)
            throw new IllegalArgumentException('Missing into')

        try {
            URL url = fromUrl instanceof URL ? fromUrl : new URL(fromUrl) 
            
            File dest = into instanceof File ? into : getProject().file(into)
            if (!dest.isDirectory() && !dest.mkdirs())
                throw new IOException('Cannot create directory: ' + dest)
            
            if (trustedHost != null)
                setupSslVerification(trustedHost);
                
            println 'cloning from: ' + url + ' into: ' + dest
    
            CloneCommand cloneCommand = Git.cloneRepository().setURI(url.toString()).setDirectory(dest);
            if (branch != null)
                cloneCommand.setBranch(branch);

            cloneCommand.call();        
        }
        catch (Exception ex) {
            throw new BuildException(ex.getMessage(), ex)
        }
    }
    
    // copied from com.centurylink.mdw.common.utilities.DesignatedHostSslVerifier
    public static synchronized void setupSslVerification(String host) throws Exception {
        if (sslVerificationHosts == null)
            sslVerificationHosts = new ArrayList<String>()

        if (!sslVerificationHosts.contains(host)) {

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            // initialize tmf with the default trust store
            tmf.init((KeyStore)null)

            // get the default trust manager
            X509TrustManager defaultTm = null
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    defaultTm = (X509TrustManager) tm
                    break
                }
            }

            TrustManager[] trustManager = [ new BlindTrustManager(defaultTm, host) ] as TrustManager[]
            SSLContext sc = SSLContext.getInstance("SSL")
            sc.init(null, trustManager, new java.security.SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())

            HostnameVerifier defaultHv = HttpsURLConnection.getDefaultHostnameVerifier()
            HostnameVerifier hostnameVerifier = new DesignatedHostnameVerifier(defaultHv, host)
            HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier)

            sslVerificationHosts.add(host);
        }
    }
}

class BlindTrustManager implements X509TrustManager {

    private X509TrustManager defaultTrustManager
    private String trustedHost

    BlindTrustManager(X509TrustManager defaultTrustManager, String trustedHost) {
        this.defaultTrustManager = defaultTrustManager
        this.trustedHost = trustedHost
    }

    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return defaultTrustManager.getAcceptedIssuers()
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        defaultTrustManager.checkClientTrusted(chain, authType)
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            X500Principal subject = chain[0].getSubjectX500Principal()
            X500Principal issuer = chain[0].getIssuerX500Principal()
            if (!trustedHost.equals(getCn(subject.getName())) || !trustedHost.equals(getCn(issuer.getName())))
                defaultTrustManager.checkServerTrusted(chain, authType)
    }

    private String getCn(String x500name) {
        String cn = null
        for (String seg : x500name.split(",")) {
            if (seg.startsWith("CN="))
                cn = seg.substring(3)
        }
        return cn
    }
}

class DesignatedHostnameVerifier implements HostnameVerifier {

    private HostnameVerifier defaultVerifier
    private String designatedHost

    DesignatedHostnameVerifier(HostnameVerifier defaultVerifier, String designatedHost) {
        this.defaultVerifier = defaultVerifier
        this.designatedHost = designatedHost
    }

    public boolean verify(String hostname, SSLSession session) {
        if (designatedHost.equals(hostname))
            return true
        else
            return defaultVerifier.verify(hostname, session)
    }
}