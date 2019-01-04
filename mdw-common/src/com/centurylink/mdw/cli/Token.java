package com.centurylink.mdw.cli;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Example commands:
 * mdw token --user=mdwapp --password=******
 * mdw token --verify --user-token=[output_from_above]
 */
@Parameters(commandNames = "token", commandDescription = "Retrieve or validate an MDW Auth access token.", separators="=")
public class Token extends Setup {

    @Parameter(names="--central-url", description="MDW Central URL")
    private String centralUrl;

    @Parameter(names="--app-id", description="MDW App ID")
    private String appId;

    @Parameter(names="--user", description="MDW User")
    private String user;

    @Parameter(names="--password", description="MDW User Password")
    private String password;

    @Parameter(names="--app-token", description="MDW App Token")
    private String appToken;

    @Parameter(names="--user-token", description="User Token (for verifying)")
    private String userToken;


    @Parameter(names="--verify", description="Verify a user token")
    private boolean verify;
    public boolean isVerify() { return verify; }

    @Override
    public Token run(ProgressMonitor... progressMonitors) throws IOException {

        if (verify) {
            String mavenUrl = "http://repo.maven.apache.org/maven2";
            Map<String,Long> dependencies = getVerificationDependencies();
            for (String dep : dependencies.keySet()) {
                new Dependency(mavenUrl, dep, dependencies.get(dep)).run(progressMonitors);
            }
            verify();
        }
        else {
            String token = retrieve();
            System.out.println("MDW Access Token:\n" + token);
        }

        return this;
    }

    private String retrieve() throws IOException {
        Props props = new Props(this);
        String mdwCentralUrl = centralUrl;
        if (mdwCentralUrl == null)
            mdwCentralUrl = props.get(Props.CENTRAL_URL);

        if (mdwCentralUrl == null)
            throw new IOException("--central-url param or mdw.central.url prop required");

        if (user == null || password == null)
            throw new IOException("--user and --password are required to retrieve a token");

        String mdwAppId = appId;
        if (mdwAppId == null)
            mdwAppId = props.get(Props.APP_ID);
        if (mdwAppId == null)
            throw new IOException("--app-id param or mdw.app.id prop required");

        String mdwAppToken = appToken;
        if (mdwAppToken == null)
            mdwAppToken = System.getenv("MDW_APP_TOKEN");
        if (mdwAppToken == null)
            throw new IOException("--app-token param or MDW_APP_TOKEN environment variable required");

        System.out.println("Retrieving token for app " + mdwAppId + " user " + user + " from " + mdwCentralUrl + " ...");

        JSONObject json = new JSONObject();
        json.put("mdwAppToken", mdwAppToken);
        json.put("appId", mdwAppId);
        json.put("user", user);
        json.put("password", password);

        if (!mdwCentralUrl.endsWith("/"))
            mdwCentralUrl += "/";
        URL tokenUrl = new URL(mdwCentralUrl + "api/auth");

        HttpURLConnection connection = (HttpURLConnection) tokenUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        try (OutputStream urlOut = connection.getOutputStream()) {
            urlOut.write(json.toString().getBytes());
            urlOut.flush();
            InputStream urlIn = connection.getInputStream();
            ByteArrayOutputStream resp = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = urlIn.read(buffer);
            while (len >= 0) {
                resp.write(buffer, 0, len);
                len = urlIn.read(buffer);
            }
            JSONObject responseJson = new JSONObject(resp.toString());
            return responseJson.getString("mdwauth");
        }
    }

    private void verify() throws IOException {
        Props props = new Props(this);

        String mdwAppId = appId;
        if (mdwAppId == null)
            mdwAppId = props.get(Props.APP_ID);
        if (mdwAppId == null)
            throw new IOException("--app-id param or mdw.app.id prop required");

        if (userToken == null)
            throw new IOException("--user-token required for verification");

        String mdwAppToken = appToken;
        if (mdwAppToken == null)
            mdwAppToken = System.getenv("MDW_APP_TOKEN");
        if (mdwAppToken == null)
            throw new IOException("--app-token param or MDW_APP_TOKEN environment variable required");

        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(mdwAppToken))
                .withIssuer("mdwAuth")
                .withAudience(mdwAppId)
                .build();

        DecodedJWT jwt = verifier.verify(userToken);
        String subject = jwt.getSubject();
        System.out.println("Token verified for app " + mdwAppId + " and user " + subject);
    }

    public static Map<String,Long> getVerificationDependencies() {
        Map<String,Long> dependencies = new HashMap<>();
        dependencies.put("com/auth0/java-jwt/3.3.0/java-jwt-3.3.0.jar", 50327L);
        dependencies.put("com/fasterxml/jackson/core/jackson-core/2.9.5/jackson-core-2.9.5.jar", 321590L);
        dependencies.put("com/fasterxml/jackson/core/jackson-databind/2.9.5/jackson-databind-2.9.5.jar", 1342410L);
        dependencies.put("com/fasterxml/jackson/core/jackson-annotations/2.9.5/jackson-annotations-2.9.5.jar", 1342410L);
        dependencies.put("commons-codec/commons-codec/1.11/commons-codec-1.11.jar", 335042L);
        return dependencies;
    }

}
