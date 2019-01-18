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
package com.centurylink.mdw.cli;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.dataaccess.AssetRef;
import com.centurylink.mdw.dataaccess.AssetRevision;
import com.centurylink.mdw.dataaccess.VersionControl;

/**
 * Capture current asset version info to DB.
 */
public class Checkpoint extends Setup {

    private String mavenRepoUrl;
    private VcInfo vcInfo;
    private File assetRoot;
    private DbInfo dbInfo;
    private VersionControl versionControl;
    private String commit;
    private Connection pooledConn;

    public Checkpoint(String mavenRepoUrl, VcInfo vcInfo, File assetRoot, DbInfo dbInfo) {
        this.mavenRepoUrl = mavenRepoUrl;
        this.vcInfo = vcInfo;
        this.assetRoot = assetRoot;
        this.dbInfo = dbInfo;
    }

    public Checkpoint(File assetLoc, VersionControl vc, String commit, Connection conn) {
        this.assetRoot = assetLoc;
        this.pooledConn = conn;
        this.versionControl = vc;
        this.commit = commit;
    }

    @Override
    public Checkpoint run(ProgressMonitor... progressMonitors) throws IOException {

        Map<String,Long> dbDependencies = DbInfo.getDependencies(dbInfo.getUrl());
        for (String dep : dbDependencies.keySet()) {
            new Dependency(mavenRepoUrl, dep, dbDependencies.get(dep)).run(progressMonitors);
        }

        Git git = new Git(mavenRepoUrl, vcInfo, "getCommit");
        git.run(progressMonitors); // connect
        commit = (String) git.getResult();
        versionControl = git.getVersionControl();
        return this;
    }

    public List<AssetRef> getCurrentRefs() throws IOException {
        return getCurrentRefs(assetRoot);
    }

    public List<AssetRef> getCurrentRefs(File dir) throws IOException {
        List<AssetRef> refs = new ArrayList<>();
        String pkgName = null;
        if (new File(dir + "/.mdw").isDirectory()) {
            if (pooledConn == null)
                pkgName = getAssetPath(dir).replace('/', '.');
            else
                pkgName = dir.getAbsolutePath().substring(assetRoot.getAbsolutePath().length() + 1).replace('\\', '/').replace('/', '.');
        }
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (pkgName != null && file.isFile()) {
                    AssetRevision rev = versionControl.getRevision(file);
                    /*  Do NOT add any non-versioned assets, otherwise we end up with v0 entries
                    if (rev == null) {
                        rev = new AssetRevision();
                        rev.setVersion(0);
                        rev.setModDate(new Date());
                    }  */
                    if (rev != null) {  // Only add versioned assets
                        // logical path
                        String name = pkgName + "/" + file.getName() + " " + rev.getFormattedVersion();
                        refs.add(new AssetRef(name, versionControl.getId(new File(name)), commit));
                    }
                }
                if (file.isDirectory() && !file.getName().equals("Archive")) {
                    refs.addAll(getCurrentRefs(file));
                }
            }
        }
        return refs;
    }

    public AssetRef getCurrentRef(String name) throws IOException {
        for (AssetRef ref : getCurrentRefs()) {
            if (ref.getName().equals(name) || ref.getName().matches(name + " v[0-9\\.\\[,\\)]*$"))
                return ref;
        }
        return null;
    }

    /**
     * Finds an asset ref from the database by asset name.
     */
    public AssetRef retrieveRef(String name) throws IOException, SQLException {
        String select = "select definition_id, name, ref from ASSET_REF where name = ?";
        try (Connection conn = getDbConnection();
                PreparedStatement stmt = conn.prepareStatement(select)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AssetRef(name, rs.getLong("definition_id"), rs.getString("ref"));
                }
            }
        }
        return null;
    }

    /**
     * Finds an asset ref from the database by definitionID.
     */
    public AssetRef retrieveRef(Long id) throws IOException, SQLException {
        String select = "select definition_id, name, ref from ASSET_REF where definition_id = ?";
        try (Connection conn = getDbConnection();
                PreparedStatement stmt = conn.prepareStatement(select)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AssetRef(rs.getString("name"), id, rs.getString("ref"));
                }
            }
        }
        return null;
    }

    /**
     * Finds all asset refs from the database since cutoffDate, or all if cutoffDate is null.
     */
    public List<AssetRef> retrieveAllRefs(Date cutoffDate) throws IOException, SQLException {
        List<AssetRef> assetRefList = null;
        String select = "select definition_id, name, ref from ASSET_REF ";
        if (cutoffDate != null)
            select += "where ARCHIVE_DT >= ? ";
        select += "order by ARCHIVE_DT desc";
        try (Connection conn = getDbConnection();
                PreparedStatement stmt = conn.prepareStatement(select)) {
            if (cutoffDate != null)
                stmt.setTimestamp(1, new Timestamp(cutoffDate.getTime()));
            try (ResultSet rs = stmt.executeQuery()) {
                assetRefList = new ArrayList<AssetRef>();
                while (rs.next()) {
                    String name = rs.getString("name");
                    if (name != null && !name.endsWith("v0")) // Ignore version 0 assets
                        assetRefList.add(new AssetRef(rs.getString("name"), rs.getLong("definition_id"), rs.getString("ref")));
                }
            }
        }
        return assetRefList;
    }

    public void updateRefs() throws SQLException, IOException {
        updateRefs(false);
    }
    /**
     * Update refs in db.
     */
    public void updateRefs(boolean assetImport) throws SQLException, IOException {
        List<AssetRef> refs = getCurrentRefs();
        if (refs == null || refs.isEmpty())
            System.out.println("Skipping ASSET_REF table insert/update due to empty current assets");
        else {
            String select = "select name, ref from ASSET_REF where definition_id = ?";
            try (Connection conn = getDbConnection();
                    PreparedStatement stmt = conn.prepareStatement(select)) {
                for (AssetRef ref : refs) {
                    stmt.setLong(1, ref.getDefinitionId());
                    try (ResultSet rs = stmt.executeQuery()) {
                        // DO NOT update existing refs with newer commitID
                        // Doing so can obliterate previous commitID from ASSET_REF table
                        // which will prevent auto-import detection in cluster envs
                        if (!rs.next()) {
                            String insert = "insert into ASSET_REF (definition_id, name, ref) values (?, ?, ?)";
                            try (PreparedStatement insertStmt = conn.prepareStatement(insert)) {
                                insertStmt.setLong(1, ref.getDefinitionId());
                                insertStmt.setString(2, ref.getName());
                                insertStmt.setString(3, ref.getRef());
                                insertStmt.executeUpdate();
                                if (!conn.getAutoCommit()) conn.commit();
                            }
                        }
                    }
                }
                if (assetImport)
                    updateRefValue();
            }
        }
    }

    public void updateRef(AssetRef ref) throws SQLException, IOException {
        String select = "select name, ref from ASSET_REF where name = ?";
        try (Connection conn = getDbConnection();
                PreparedStatement stmt = conn.prepareStatement(select)) {
            stmt.setString(1, ref.getName());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String update = "update ASSET_REF set definition_id = ?, ref = ? where name = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(update)) {
                        updateStmt.setLong(1, ref.getDefinitionId());
                        updateStmt.setString(2, ref.getRef());
                        updateStmt.setString(3, ref.getName());
                        updateStmt.executeUpdate();
                        if (!conn.getAutoCommit()) conn.commit();
                    }
                }
                else {
                    String insert = "insert into ASSET_REF (definition_id, name, ref) values (?, ?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insert)) {
                        insertStmt.setLong(1, ref.getDefinitionId());
                        insertStmt.setString(2, ref.getName());
                        insertStmt.setString(3, ref.getRef());
                        insertStmt.executeUpdate();
                        if (!conn.getAutoCommit()) conn.commit();
                    }
                }
            }
        }
    }

    public void updateRefValue() throws SQLException, IOException {
        String select = "select value from VALUE where name = ? and owner_type = ? and owner_id = ?";
        try (Connection conn = getDbConnection();
                PreparedStatement stmt = conn.prepareStatement(select)) {
            stmt.setString(1, "CommitID");
            stmt.setString(2, "AssetImport");
            stmt.setString(3, "0");
            try (ResultSet rs = stmt.executeQuery()) {
                Timestamp currentDate = new Timestamp(System.currentTimeMillis());
                if (rs.next()) {
                    if (!commit.equals(rs.getString("value"))) {
                        String update = "update VALUE set value = ?, mod_dt = ? where name = ? and owner_type = ? and owner_id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(update)) {
                            updateStmt.setString(1, commit);
                            updateStmt.setTimestamp(2, currentDate);
                            updateStmt.setString(3, "CommitID");
                            updateStmt.setString(4, "AssetImport");
                            updateStmt.setString(5, "0");
                            updateStmt.executeUpdate();
                            if (!conn.getAutoCommit()) conn.commit();
                        }
                    }
                }
                else {
                    String insert = "insert into VALUE (value, name, owner_type, owner_id, create_dt, create_usr, mod_dt, mod_usr, comments) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insert)) {
                        insertStmt.setString(1, commit);
                        insertStmt.setString(2, "CommitID");
                        insertStmt.setString(3, "AssetImport");
                        insertStmt.setString(4, "0");
                        insertStmt.setTimestamp(5, currentDate);
                        insertStmt.setString(6, "MDWEngine");
                        insertStmt.setTimestamp(7, currentDate);
                        insertStmt.setString(8, "MDWEngine");
                        insertStmt.setString(9, "Represents the last time assets were imported");
                        insertStmt.executeUpdate();
                        if (!conn.getAutoCommit()) conn.commit();
                    }
                }
            }
        }
    }


    private void loadDbDriver() throws IOException {
        try {
            Class.forName(DbInfo.getDatabaseDriver(dbInfo.getUrl()));
        }
        catch (ClassNotFoundException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    private Connection getDbConnection() throws SQLException, IOException {
        if (pooledConn == null) {
            loadDbDriver();
            return DriverManager.getConnection(dbInfo.getUrl(), dbInfo.getUser(), dbInfo.getPassword());
        }
        else
            return pooledConn;
    }
}
