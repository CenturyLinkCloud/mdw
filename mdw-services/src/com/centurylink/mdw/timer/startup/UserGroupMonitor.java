package com.centurylink.mdw.timer.startup;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DbAccess;
import com.centurylink.mdw.service.data.user.UserGroupCache;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.startup.StartupException;
import com.centurylink.mdw.startup.StartupService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Checks for asset imports performed on other instances
 */
public class UserGroupMonitor implements StartupService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static boolean _terminating;
    private static UserGroupMonitor monitor = null;
    private static Thread thread = null;

    /**
     * Invoked when the server starts up.
     */
    public void onStartup() throws StartupException {
        if (monitor == null) {
            monitor = this;
            thread = new Thread() {
                @Override
                public void run() {
                    this.setName("UserGroupMonitor-thread");
                    monitor.start();
                }
            };
            thread.start();
        }
    }

    public void onShutdown() {
        _terminating = true;
        thread.interrupt();
    }

    public void start() {
        try {
            Long interval = PropertyManager.getLongProperty(PropertyNames.MDW_USERGROUP_MONITOR_INTERVAL, 120000); //Defaults to checking every 120 seconds
            _terminating = false;

            while (!_terminating) {
                try {
                    Thread.sleep(interval);

                    // Check if it needs to trigger a UserGroup cache refresh
                    String select = "select mod_dt from VALUE where name= ? and owner_type= ? and owner_id= ?";
                    try (DbAccess dbAccess = new DbAccess(); PreparedStatement stmt = dbAccess.getConnection().prepareStatement(select)) {
                        stmt.setString(1, "LastUserGroupChange");
                        stmt.setString(2, "UserGroupAdmin");
                        stmt.setString(3, "0");
                        long latestChange = 0;
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                latestChange = rs.getTimestamp("mod_dt").getTime();
                            }
                        }
                        // Proceed if user changes have occurred on another instance
                        if (latestChange > 0 && latestChange > UserGroupCache.getLastUserSync()) {
                            logger.info("Detected User/Group changes.  Refreshing...");
                            CacheRegistration.getInstance().refreshCache("UserGroupCache");
                        }
                    }
                }
                catch (InterruptedException e) {
                    if (!_terminating) throw e;
                    logger.info(this.getClass().getName() + " stopping.");
                }
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        finally {
            if (!_terminating) this.start();  // Restart if a failure occurred, besides instance is shutting down
        }
    }
}
