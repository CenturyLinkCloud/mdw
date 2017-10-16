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
                    if (rev == null) {
                        rev = new AssetRevision();
                        rev.setVersion(0);
                        rev.setModDate(new Date());
                    }
                    // logical path
                    String name = pkgName + "/" + file.getName() + " " + rev.getFormattedVersion();
                    refs.add(new AssetRef(name, versionControl.getId(new File(name)), commit));
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
        String select = "select definition_id, name, ref from asset_ref where name = ?";
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
        String select = "select definition_id, name, ref from asset_ref where definition_id = ?";
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
        String select = "select definition_id, name, ref from asset_ref ";
        if (cutoffDate != null)
            select += "where ARCHIVE_DT >= ? ";
        select += "order by ARCHIVE_DT desc";
        try (Connection conn = getDbConnection();
                PreparedStatement stmt = conn.prepareStatement(select)) {
            stmt.setTimestamp(1, new Timestamp(cutoffDate.getTime()));
            try (ResultSet rs = stmt.executeQuery()) {
                assetRefList = new ArrayList<AssetRef>();
                while (rs.next()) {
                    assetRefList.add(new AssetRef(rs.getString("name"), rs.getLong("definition_id"), rs.getString("ref")));
                }
            }
        }
        return assetRefList;
    }

    /**
     * Update refs in db.
     */
    public void updateRefs() throws SQLException, IOException {
        List<AssetRef> refs = getCurrentRefs();
        String select = "select name, ref from asset_ref where definition_id = ?";
        try (Connection conn = getDbConnection();
                PreparedStatement stmt = conn.prepareStatement(select)) {
            for (AssetRef ref : refs) {
                stmt.setLong(1, ref.getDefinitionId());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String name = rs.getString("name");
                        if (!ref.getName().equals(name)) {
                            throw new IOException("Unexpected name for id=" + ref.getDefinitionId()
                                    + " (expected '" + ref.getName() + "', found '" + name + "'");
                        }
                        String oldRef = rs.getString("ref");
                        if (!oldRef.equals(ref.getRef())) {
                            String update = "update asset_ref set ref = ? where definition_id = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(update)) {
                                updateStmt.setString(1, ref.getRef());
                                updateStmt.setLong(2, ref.getDefinitionId());
                                updateStmt.executeUpdate();
                                conn.commit();
                            }
                        }
                    }
                    else {
                        String insert = "insert into asset_ref (definition_id, name, ref) values (?, ?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insert)) {
                            insertStmt.setLong(1, ref.getDefinitionId());
                            insertStmt.setString(2, ref.getName());
                            insertStmt.setString(3, ref.getRef());
                            insertStmt.executeUpdate();
                            conn.commit();
                        }
                    }
                }
            }
        }
    }

    public void updateRef(AssetRef ref) throws SQLException, IOException {
        String select = "select name, ref from asset_ref where name = ?";
        try (Connection conn = getDbConnection();
                PreparedStatement stmt = conn.prepareStatement(select)) {
            stmt.setString(1, ref.getName());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String update = "update asset_ref set definition_id = ?, ref = ? where name = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(update)) {
                        updateStmt.setLong(1, ref.getDefinitionId());
                        updateStmt.setString(2, ref.getRef());
                        updateStmt.setString(3, ref.getName());
                        updateStmt.executeUpdate();
                        conn.commit();
                    }
                }
                else {
                    String insert = "insert into asset_ref (definition_id, name, ref) values (?, ?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insert)) {
                        insertStmt.setLong(1, ref.getDefinitionId());
                        insertStmt.setString(2, ref.getName());
                        insertStmt.setString(3, ref.getRef());
                        insertStmt.executeUpdate();
                        conn.commit();
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
