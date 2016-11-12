/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.service.MdwServiceRegistry;
import com.centurylink.mdw.container.EmbeddedDb;
import com.centurylink.mdw.container.EmbeddedDbExtension;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.util.JsonUtil;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Embedded db data access.
 */
public class EmbeddedDataAccess {

    public static final String MDW_EMBEDDED_DB_CLASS = "MariaDBEmbeddedDb";  // hardcoded for now

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private String url;
    private String user;
    private String password;
    private EmbeddedDb embeddedDb;
    private ClassLoader cloudClassLoader;
    private Thread shutdownHook;

    private boolean extensionsNeedInitialization;

    public void create(String url, String user, String password, String assetLoc, String baseLoc, String dataLoc) throws DataAccessException {
        this.url = url;
        this.user = user;
        this.password = password;
        String embeddedDbClass = EmbeddedDb.DB_ASSET_PACKAGE + "." + MDW_EMBEDDED_DB_CLASS;
        try {
            Package pkg = PackageCache.getPackage(EmbeddedDb.DB_ASSET_PACKAGE);
            if (pkg == null)
                throw new DataAccessException("Missing required asset package: " + EmbeddedDb.DB_ASSET_PACKAGE);
            cloudClassLoader = pkg.getCloudClassLoader();
            embeddedDb = cloudClassLoader.loadClass(embeddedDbClass).asSubclass(EmbeddedDb.class).newInstance();
            embeddedDb.init(url, user, password, assetLoc, baseLoc, dataLoc);
        }
        catch (Exception ex) {
            throw new DataAccessException("Error creating embedded DB: " + embeddedDbClass, ex);
        }
    }

    /**
     * Starts the db if not running and connects to confirm process_instance table exists,
     * and creates the schema if the table is not found.
     */
    public void run() throws SQLException {

        if (ApplicationContext.isDevelopment() && embeddedDb.checkRunning()) {
            // only checked in development (otherwise let db startup file due to locked resources)
            logger.severe("\n***WARNING***\nEmbedded DB appears to be running already.  This can happen due to an unclean previous shutdown.\n***WARNING***");
            return;
        }

        if (shutdownHook != null) {
            embeddedDb.shutdown();
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }

        shutdownHook = new Thread(new Runnable() {
            public void run() {
                try {
                    embeddedDb.shutdown();
                }
                catch (SQLException ex) {
                    System.err.println("ERROR: Cannot shut down embedded db cleanly");
                    ex.printStackTrace();
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        embeddedDb.startup();

        if (!embeddedDb.checkMdwSchema()) {
            embeddedDb.createMdwSchema();

            // seed users
            try {
                String usersJson = JsonUtil.read("seed_users.json");
                if (usersJson != null) {
                    logger.info("Loading seed users into " + MDW_EMBEDDED_DB_CLASS);
                    JSONObject usersObj = new JSONObject(usersJson);
                    if (usersObj.has("users")) {
                        JSONArray usersArr = usersObj.getJSONArray("users");
                        for (int i = 0; i < usersArr.length(); i++)
                            embeddedDb.insertUser(new User(usersArr.getJSONObject(i)));
                    }
                }
            }
            catch (Exception ex) {
                throw new SQLException("Error inserting from seed_users.json", ex);
            }

            extensionsNeedInitialization = true;
        }
    }

    /**
     * Initialize embedded db extensions (this one-time step is only performed when
     * the mdw embedded db tables are not present at startup).
     */
    public void initializeExtensions() throws SQLException {
        if (extensionsNeedInitialization) {
            for (EmbeddedDbExtension ext : MdwServiceRegistry.getInstance().getEmbeddedDbExtensions()) {
                logger.info("Initializing embedded db extension: " + ext);
                List<String> sources = ext.getSqlSourceAssets();
                if (sources != null) {
                    Connection connection = null;
                    try {
                        Class.forName(embeddedDb.getDriverClass());
                        connection = DriverManager.getConnection(url, user, password);

                        for (String source : sources) {
                            PreparedStatement preparedStatement = null;
                            String asset = source;
                            if (asset.indexOf('/') == -1)
                                asset = ext.getClass().getPackage().getName() + "/" + asset;
                            String sql = AssetCache.getAsset(asset).getStringContent();
                            preparedStatement = connection.prepareStatement(sql);
                            preparedStatement.executeUpdate();
                        }
                    }
                    catch (Exception ex) {
                        throw new SQLException("Error processing db extension " + ext.getClass(), ex);
                    }
                    finally {
                        if (connection != null)
                            connection.close();
                    }
                }
                ext.initialize();
            }
            extensionsNeedInitialization = false;
        }
    }

}
