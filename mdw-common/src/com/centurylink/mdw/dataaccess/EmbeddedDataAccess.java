/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONObject;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.JsonUtil;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.container.EmbeddedDb;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.user.UserVO;

/**
 * TODO: users
 */
public class EmbeddedDataAccess {

    public static final String EMBEDDED_DB_CLASS = "MariaDBEmbeddedDb";  // hardcoded for now

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private EmbeddedDb embeddedDb;
    private ClassLoader cloudClassLoader;
    private Thread shutdownHook;

    public void create(String url, String user, String password, String assetLoc, String baseLoc, String dataLoc) throws DataAccessException {
        // implementors must be in this package
        PackageVO pkg = PackageVOCache.getPackage(EmbeddedDb.DB_ASSET_PACKAGE);
        if (pkg == null)
            throw new DataAccessException("Missing required asset package: " + EmbeddedDb.DB_ASSET_PACKAGE);

        String embeddedDbClass = EmbeddedDb.DB_ASSET_PACKAGE + "." + EMBEDDED_DB_CLASS;
        try {
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
                String usersJson = JsonUtil.read("seed_users");
                if (usersJson != null) {
                    logger.info("Loading seed users into " + EMBEDDED_DB_CLASS);
                    JSONObject usersObj = new JSONObject(usersJson);
                    if (usersObj.has("users")) {
                        JSONArray usersArr = usersObj.getJSONArray("users");
                        for (int i = 0; i < usersArr.length(); i++)
                            embeddedDb.insertUser(new UserVO(usersArr.getJSONObject(i)));
                    }
                }
            }
            catch (Exception ex) {
                throw new SQLException("Error parsing mdwUsers.json", ex);
            }
        }
    }

}
