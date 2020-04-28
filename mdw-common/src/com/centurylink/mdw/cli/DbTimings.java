package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.io.IOException;
import java.sql.*;

@Parameters(commandNames = "dbtimings", commandDescription = "Populate db instance_timing table", separators = "=")
public class DbTimings extends DbOperation {

    @Parameter(names="--owner", description="Owner type for INSTANCE_TIMING.  Currently ACTIVITY_INSTANCE is supported.")
    private String owner;
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    @Parameter(names="--row-limit", description="Max instance rows to populate")
    private int rowLimit = 50000;
    public int getRowLimit() { return rowLimit; }
    public void setRowLimit(int rowLimit) { this.rowLimit = rowLimit; }

    @Override
    public Operation run(ProgressMonitor... monitors) throws IOException {
        super.run(monitors);

        for (ProgressMonitor monitor : monitors)
            monitor.progress(0);

        long count = getRowCount();
        for (ProgressMonitor monitor : monitors)
            monitor.progress(5);

        Connection conn = null;
        try {
            String select = getElapsedMsQuery();
            conn = getDbConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(select);
            String where = "where owner_type = ? and instance_id = ?";
            String check = "select instance_id from INSTANCE_TIMING " + where;
            String update = "update INSTANCE_TIMING set elapsed_ms = ? " + where;
            String insert = "insert into INSTANCE_TIMING (instance_id, owner_type, elapsed_ms) values (?, ?, ?)";
            int i = 0;
            int progress = 5;
            while (rs.next()) {
                Long instanceId = rs.getLong("INSTANCE_ID");
                Long elapsedMs = rs.getLong("ELAPSED_MS");
                PreparedStatement checkStmt = conn.prepareStatement(check);
                checkStmt.setString(1, owner);
                checkStmt.setLong(2, instanceId);
                if (checkStmt.executeQuery().next()) {
                    PreparedStatement updateStmt = conn.prepareStatement(update);
                    updateStmt.setLong(1, elapsedMs);
                    updateStmt.setString(2, owner);
                    updateStmt.setLong(3, instanceId);
                    updateStmt.executeUpdate();
                }
                else {
                    PreparedStatement insertStmt = conn.prepareStatement(insert);
                    insertStmt.setLong(1, instanceId);
                    insertStmt.setString(2, owner);
                    insertStmt.setLong(3, elapsedMs);
                    insertStmt.executeUpdate();
                }
                i++;
                int newProg = 5 + (int) Math.round(((double)i / count) * 95);
                if (newProg != progress) {
                    progress = newProg;
                    for (ProgressMonitor monitor : monitors)
                        monitor.progress(progress);
                }
            }

            for (ProgressMonitor monitor : monitors)
                monitor.progress(100);
            System.out.println(count + " instance timings populated for owner " + owner);
        } catch (SQLException ex) {
            throw new IOException("Error populating timings for owner " + owner, ex);
        } finally {
            closeConnection(conn);
        }

        return this;
    }

    private long getRowCount() throws IOException {
        Connection conn = null;
        try {
            conn = getDbConnection();
            ResultSet rs = conn.createStatement().executeQuery("select count(*) from " + getInstanceTable()
                    + " where START_DT is not null && END_DT is not null");
            rs.next();
            long rows = rs.getLong(1);
            return rows > rowLimit ? rowLimit : rows;
        } catch (SQLException ex) {
            throw new IOException("Error counting instances for owner " + owner, ex);
        } finally {
            closeConnection(conn);
        }
    }

    private void closeConnection(Connection conn) throws IOException {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ex) {
                throw new IOException(ex); // NOSONAR
            }
        }
    }

    protected String getElapsedMsQuery() throws IOException {
        if (isOracle()) {
            return "SELECT " + getInstanceIdColumn() + " as INSTANCE_ID, EXTRACT(day FROM DIFF)*24*60*60*1000 + " +
                   "EXTRACT(hour FROM DIFF)*60*60*1000 + " +
                   "EXTRACT(minute FROM DIFF)*60*1000 + " +
                   "EXTRACT(second FROM DIFF)*1000 as ELAPSED_MS" +
                   " FROM (SELECT (END_DT - START_DT) AS DIFF" +
                   " from " + getInstanceTable() +
                   " where START_DT is not null && END_DT is not null" +
                   " and rownum <= rowLimit" +
                   " order by START_DT desc";
        }
        else {
            return "select " + getInstanceIdColumn() + " as INSTANCE_ID, TIMESTAMPDIFF(MICROSECOND, START_DT, END_DT)/1000 as ELAPSED_MS" +
                    " from " + getInstanceTable() +
                    " where START_DT is not null && END_DT is not null" +
                    " order by START_DT desc" +
                    " limit " + rowLimit;
        }
    }

    protected String getInstanceTable() throws IOException {
        if ("ACTIVITY_INSTANCE".equals(owner))
            return "ACTIVITY_INSTANCE";
        else if ("PROCESS_INSTANCE".equals(owner))
            return "PROCESS_INSTANCE";
        else
            throw new IOException("Unsupported timings owner " + owner);
    }

    protected String getInstanceIdColumn() throws IOException {
        if ("ACTIVITY_INSTANCE".equals(owner))
            return "ACTIVITY_INSTANCE_ID";
        else if ("PROCESS_INSTANCE".equals(owner))
            return "PROCESS_INSTANCE_ID";
        else
            throw new IOException("Unsupported timings owner " + owner);
    }
}
