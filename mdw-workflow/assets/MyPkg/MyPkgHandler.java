/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */

package MyPkg;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.event.EventHandlerException;
import com.centurylink.mdw.listener.ExternalEventHandlerBase;

/**
 * Dynamic Java workflow asset.
 */
public class MyPkgHandler extends ExternalEventHandlerBase
{

    /* (non-Javadoc)
     * @see com.centurylink.mdw.event.ExternalEventHandler#handleEventMessage(java.lang.String, java.lang.Object, java.util.Map)
     */
    @Override
    public String handleEventMessage(String msg, Object msgobj, Map<String, String> metainfo)
            throws EventHandlerException {
        DatabaseAccess myDB = new DatabaseAccess("myDBSource2");
        try {
            Connection conn = myDB.openConnection();
        }
        catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            myDB.closeConnection();
        }
        return null;
    }
}
