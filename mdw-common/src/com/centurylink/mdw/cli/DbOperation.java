package com.centurylink.mdw.cli;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class DbOperation extends Setup {

    private static String ORACLE_DRIVER_JAR = "ojdbc6-12.1.0.2.0.jar";
    private static String ORACLE_DRIVER_ASSET = "com.centurylink.mdw.oracle/" + ORACLE_DRIVER_JAR;

    @Override
    public Operation run(ProgressMonitor... monitors) throws IOException {
        // db dependencies
        Props props = new Props(this);
        DbInfo dbInfo = new DbInfo(props);
        Map<String,Long> dbDependencies = DbInfo.getDependencies(dbInfo.getUrl());
        for (String dep : dbDependencies.keySet()) {
            new Dependency(getReleasesUrl(), dep, dbDependencies.get(dep)).run(monitors);
        }
        File oracleDriverAsset = getAssetFile(ORACLE_DRIVER_ASSET);
        if (oracleDriverAsset != null && oracleDriverAsset.isFile()) {
            File libDir = Dependency.getLibDir();
            File oraJar = new File(libDir + "/" + ORACLE_DRIVER_JAR);
            if (!oraJar.exists()) {
                new Copy(oracleDriverAsset, oraJar).run();
            }
        }
        return this;
    }

    protected void loadDbDriver() throws IOException {
        try {
            Class.forName(DbInfo.getDatabaseDriver(getDatabaseUrl()));
        }
        catch (ClassNotFoundException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    protected Connection getDbConnection() throws SQLException, IOException {
        loadDbDriver();
        return DriverManager.getConnection(getDatabaseUrl(), getDatabaseUser(), getDatabasePassword());
    }

    private Boolean oracle;
    protected boolean isOracle() {
        if (oracle == null)
            oracle = getDatabaseUrl().startsWith("jdbc:oracle");
        return oracle;
    }

    private List<String> tables;
    protected List<String> getTables() {
        if (tables == null) {
            tables = getProject().getData().getDbTables();
        }
        return tables;
    }

    private List<String> excludedTables;
    protected List<String> getExcludedTables() {
        if (excludedTables == null) {
            excludedTables = getProject().getData().getExcludedTables();
        }
        return excludedTables;
    }
}
