package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.model.request.ServicePath;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Parameters(commandNames="paths", commandDescription="Display and optionally normalize unique service paths", separators="=")
public class Paths extends Setup {

    static final String SELECT = "select path from DOCUMENT where owner_type = ? and path is not null group by path";
    static final String UPDATE = "";
    static final int BATCH_SIZE = 5000;

    @Parameter(names="--outbound", description="Show/update outbound request paths (otherwise inbound paths are shown/updated")
    private boolean outbound;
    public boolean isOutbound() { return outbound; }
    public void setOutbound(boolean outbound) { this.outbound = outbound; }

    @Parameter(names="--dryrun", description="(With --normalize) show what would be updated")
    private boolean dryrun;
    public boolean isDryrun() { return dryrun; }
    public void setDryrun(boolean dryrun) { this.dryrun = dryrun; }

    @Parameter(names="--normalize", description="Update reponse paths in DOCUMENT table")
    private boolean normalize;
    public boolean isNormalize() { return normalize; }
    public void setNormalize(boolean normalize) { this.normalize = normalize; }

    @Parameter(names="--swagger", description="Swagger URL or file path for normalization")
    private String swagger;
    public String getSwagger() {
        return swagger;
    }
    public void setSwagger(String swagger) {
        this.swagger = swagger;
    }

    @Parameter(names="--path", description="Specific path to normalize (always dry run)")
    private String path;
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }

    private DbInfo db;
    private List<ServicePath> swaggerPaths;

    @Override
    public Paths run(ProgressMonitor... monitors) throws IOException {

        db = new DbInfo(new Props(this));

        Map<String,Long> dbDependencies = DbInfo.getDependencies(db.getUrl());
        for (String dep : dbDependencies.keySet()) {
            new Dependency(Setup.MAVEN_CENTRAL_URL, dep, dbDependencies.get(dep)).run(monitors);
        }

        if (normalize) {
            swaggerPaths = getSwaggerPaths();
            if (isDebug()) {
                System.out.println("Swagger paths:");
                for (ServicePath swaggerPath : swaggerPaths) {
                    System.out.println("  - " + swaggerPath);
                }
            }
        }

        HashSet<String> uniquePaths = normalize && isDebug() ? new HashSet<>() : null;

        if (path != null) {
            ServicePath normalized = new ServicePath(path).normalize(swaggerPaths);
            if (path.equals(normalized.getPath()))
                System.out.println("  - " + path);
            else
                System.out.println("  - " + path + " -> " + normalized);
        }
        else {
            System.out.println(outbound ? "Outbound paths: " : "Inbound paths:");
            // group by is much faster than select distinct on mysql
            try (Connection conn = db.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(SELECT)) {
                stmt.setString(1, outbound ? "ADAPTER_RESPONSE" : "LISTENER_RESPONSE");
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ServicePath servicePath = ServicePath.parse(rs.getString(1));
                        if (normalize) {
                            ServicePath normalized = servicePath.normalize(swaggerPaths);
                            if (normalized.getPath().equals(servicePath.getPath())) {
                                System.out.println("  - " + servicePath);
                            }
                            else {
                                System.out.println("  - " + servicePath + " -> " + normalized);
                                if (!dryrun) {
                                    // TODO batch update
                                }
                            }
                            if (uniquePaths != null)
                                uniquePaths.add(normalized.getPath());
                        }
                        else {
                            System.out.println("  - " + path);
                        }
                    }
                }
            }
            catch (SQLException ex) {
                throw new IOException(ex);
            }
        }

        if (uniquePaths != null) {
            List<String> uniqueList = new ArrayList<>();
            uniqueList.addAll(Arrays.asList(uniquePaths.toArray(new String[0])));
            uniqueList.sort(String::compareToIgnoreCase);
            System.out.println("Unique normalized paths:");
            for (String uniquePath : uniqueList)
                System.out.println("  - " + uniquePath);
        }

        return this;
    }

    private List<ServicePath> getSwaggerPaths(ProgressMonitor... monitors) throws IOException {
        if (swagger == null) {
            if (outbound) {
                throw new IOException("--swagger is required for normalizing outbound request paths.");
            }
            else {
                swagger = getBaseUrl() + "/api-docs";
            }
        }
        String swaggerContent;
        if (swagger.startsWith("http://") || swagger.startsWith("https://")) {
            swaggerContent = new Fetch(new URL(swagger)).run(monitors).getData();
        }
        else {
            swaggerContent = new String(Files.readAllBytes(new File(swagger).toPath()));
        }
        List<ServicePath> servicePaths = new ArrayList<>();
        JSONObject swaggerJson = new JSONObject(swaggerContent);
        for (String path : JSONObject.getNames(swaggerJson.getJSONObject("paths"))) {
            servicePaths.add(new ServicePath(path));
        }
        Collections.sort(servicePaths);
        return servicePaths;
    }

    private void update(boolean isOutbound) throws IOException {
//        String query = "";
//        db.prepareStatement(query);
//        Object[] args = new Object[4];
//        for (Attribute vo : pAttributes) {
//            String v = vo.getAttributeValue();
//            if (v==null||v.length()==0) continue;
//            args[0] = pOwner;
//            args[1] = pOwnerId;
//            args[2] = vo.getAttributeName();
//            args[3] = v;
//            db.addToBatch(args);
//        }
//        db.runBatchUpdate();

    }

}
