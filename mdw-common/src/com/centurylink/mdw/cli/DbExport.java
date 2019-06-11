package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Lists;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * TODO: Handle DocumentDb
 */
@Parameters(commandNames="dbexport", commandDescription="Export database tables to json", separators="=")
public class DbExport extends DbOperation {

    @Parameter(names="--output", description="Filename of the exported output", required=true)
    private File output;
    public File getOutput() {
        return output;
    }
    public void setOutput(File file) { this.output = file; }

    @Parameter(names="--row-limit", description="Max rows to export for any table")
    private int rowLimit = 25000;
    public int getRowLimit() { return rowLimit; }
    public void setRowLimit(int rowLimit) { this.rowLimit = rowLimit; }

    @Parameter(names="--process-limit", description="Only export latest <limit> main (master/error) processes")
    private int processLimit = 1000;
    public int getProcessLimit() { return processLimit; }
    public void setProcessLimit(int processLimit) { this.processLimit = processLimit; }

    @Parameter(names="--process-where", description="Additional where clause for limiting main processes")
    private String processWhere;
    public String getProcessWhere() { return processWhere; }
    public void setRowLimit(String processWhere) { this.processWhere = processWhere; }

    private List<Long> processInstanceIds = new ArrayList<>();

    @Override
    public Operation run(ProgressMonitor... monitors) throws IOException {

        super.run(monitors);

        for (ProgressMonitor monitor : monitors)
            monitor.progress(0);

        Connection conn = null;
        try {
            conn = getDbConnection();

            // find master process instance ids (5% progress)
            String extraWhere = processWhere == null ? "" : processWhere.trim();
            if (extraWhere.toLowerCase().startsWith("where "))
                extraWhere = extraWhere.substring(6);
            if (!extraWhere.isEmpty() && !extraWhere.toLowerCase().startsWith("and "))
                extraWhere = "and " + extraWhere;
            String procInstsSelect;
            if (isOracle()) {
                procInstsSelect = "select * from (select * from PROCESS_INSTANCE order by PROCESS_INSTANCE_ID desc)" +
                        " where OWNER not in ('PROCESS_INSTANCE', 'MAIN_PROCESS_INSTANCE') " + extraWhere +
                        " and rownum <= " + processLimit;
            }
            else {
                procInstsSelect = "select PROCESS_INSTANCE_ID from PROCESS_INSTANCE " +
                        " where OWNER not in ('PROCESS_INSTANCE', 'MAIN_PROCESS_INSTANCE') " + extraWhere +
                        " order by PROCESS_INSTANCE_ID desc limit " + processLimit;
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(procInstsSelect)) {
                while (rs.next()) {
                    processInstanceIds.add(rs.getLong("PROCESS_INSTANCE_ID"));
                }
                for (ProgressMonitor monitor : monitors)
                    monitor.progress(5);
            }

            // find all subprocess instance ids (5%)
            List<Long> subprocessInstanceIds = addSubprocessIds(conn, processInstanceIds);
            while (!subprocessInstanceIds.isEmpty()) {
                subprocessInstanceIds = addSubprocessIds(conn, subprocessInstanceIds);
            }

            for (ProgressMonitor monitor : monitors)
                monitor.progress(10);

            List<String> tables = getTables();
            // count rows (5% progress)
            int allRows = 0;
            for (int i = 0; i < tables.size(); i++) {
                String table = tables.get(i);
                String select = "select count(*) from " + table + " " + getWhere(table);
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(select)) {
                    rs.next();
                    int rows = rs.getInt(1);
                    if (rows > rowLimit)
                        throw new IOException("Table " + table + " exceeds --row-limit of " + rowLimit);
                    int prog = 10 + ((int)Math.floor((i * 5d)/tables.size()));
                    for (ProgressMonitor monitor : monitors)
                        monitor.progress(prog);
                    allRows += rows;
                }
            }

            // retrieve/write rows (85% progress)
            try (FileWriter fw = new FileWriter(output);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write("{");
                int overallRow = 0;
                for (int i = 0; i < tables.size(); i++) {
                    String table = tables.get(i);
                    bw.write("\n  \"" + table + "\": [");
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("select * from " + table + " " + getWhere(table))) {
                        ResultSetMetaData rsmd = rs.getMetaData();
                        int columnCount = rsmd.getColumnCount();
                        int tableRow = 0;
                        while (rs.next()) {
                            if (tableRow > 0)
                                bw.write(",");
                            bw.write("\n    { ");
                            boolean hasOne = false;
                            for (int j = 1; j <= columnCount; j++) {
                                String column = rsmd.getColumnName(j);
                                Object value = rs.getObject(column);
                                if (value != null) {
                                    if (hasOne)
                                        bw.write(", ");
                                    bw.write("\"" + column + "\":");
                                    if (value instanceof Number || value instanceof Boolean)
                                        bw.write(" " + value);
                                    else if (isOracle() && value instanceof Clob)
                                        bw.write(" " + JSONObject.quote(rs.getString(column)));
                                    else
                                        bw.write(" " + JSONObject.quote("" + value));
                                    hasOne = true;
                                }
                            }
                            bw.write(" }");
                            tableRow++;
                            int prog = 15 + ((int)Math.floor((overallRow * 85d) / allRows));
                            for (ProgressMonitor monitor : monitors)
                                monitor.progress(prog);
                            overallRow++;
                        }
                        bw.write("\n  ]");
                        if (i < tables.size() - 1)
                            bw.write(",\n");
                    }
                }
                bw.write("\n}");
                for (ProgressMonitor monitor : monitors)
                    monitor.progress(100);
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace(getErr());
                }
            }
        }

        return this;
    }

    /**
     * Find all direct subprocesses for the list of callers.
     */
    private List<Long> addSubprocessIds(Connection conn, List<Long> callers) throws SQLException, IOException {
        List<Long> subprocessInstanceIds = new ArrayList<>();
        String subprocInstsSelect = "select PROCESS_INSTANCE_ID from PROCESS_INSTANCE " +
                "where OWNER in ('PROCESS_INSTANCE', 'MAIN_PROCESS_INSTANCE') and " +
                getInstanceIdsIn("OWNER_ID", callers);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(subprocInstsSelect)) {
            while (rs.next()) {
                if (processInstanceIds.size() > rowLimit)
                    throw new IOException("PROCESS_INSTANCES exceeds --row-limit of " + rowLimit);
                Long instanceId = rs.getLong(1);
                processInstanceIds.add(instanceId);
                subprocessInstanceIds.add(instanceId);
            }
            return subprocessInstanceIds;
        }
    }

    private String getInstanceIdsIn(String column, List<Long> instanceIds) {
        if (isOracle() && instanceIds.size() > 1000) {
            // workaround for ORA-01795: maximum number of expressions in a list is 1000
            boolean hasOne = false;
            StringBuilder in = new StringBuilder().append("(");
            for (List<Long> sublist : Lists.partition(instanceIds, 1000)) {
                if (hasOne)
                    in.append(" or ");
                in.append(getInstanceIdsIn(column, sublist));
                hasOne = true;
            }
            in.append(")");
            return in.toString();
        }
        return column + " in (" + instanceIds.stream().map(Object::toString).collect(Collectors.joining(",")) + ")";
    }

    /**
     * TODO: EVENT_INSTANCE, EVENT_WAIT_INSTANCE
     * TODO: INSTANCE_NOTE, ATTACHMENT
     * TODO: SOLUTION, SOLUTION_MAP
     */
    private String getWhere(String table) {
        if (table.equals("PROCESS_INSTANCE")) {
            return "where " + getInstanceIdsIn("PROCESS_INSTANCE_ID", processInstanceIds);
        }
        else if (table.equals("ACTIVITY_INSTANCE")) {
            return "where " + getInstanceIdsIn("PROCESS_INSTANCE_ID", processInstanceIds);
        }
        else if (table.equals("WORK_TRANSITION_INSTANCE") || table.equals("VARIABLE_INSTANCE")) {
            return "where " + getInstanceIdsIn("PROCESS_INST_ID", processInstanceIds);
        }
        else if (table.equals("TASK_INSTANCE")) {
            return getTaskInstanceWhere();
        }
        else if (table.equals("TASK_INST_GRP_MAPP")) {
            return "where TASK_INSTANCE_ID in (select TASK_INSTANCE_ID from TASK_INSTANCE " + getTaskInstanceWhere() + ")";
        }
        else if (table.equals("INSTANCE_INDEX")) {
            return "where OWNER_TYPE = 'TASK_INSTANCE' and INSTANCE_ID in (select TASK_INSTANCE_ID from TASK_INSTANCE " + getTaskInstanceWhere() + ")";
        }
        else if (table.equals("DOCUMENT")) {
            return getDocumentWhere();
        }
        else if (table.equals("DOCUMENT_CONTENT")) {
            return "where DOCUMENT_ID in (select DOCUMENT_ID from DOCUMENT " + getDocumentWhere() + ")";
        }
        else if (table.equals("EVENT_INSTANCE")) {
            // exclude all scheduled jobs
            return "where STATUS_CD != 5";
            // TODO: restrict waits/timers to exported process instances
        }

        return "";
    }

    private String getTaskInstanceWhere() {
        return "where TASK_INSTANCE_OWNER != 'PROCESS_INSTANCE' or " + getInstanceIdsIn("TASK_INSTANCE_OWNER_ID", processInstanceIds);
    }

    private String getDocumentWhere() {
        // avoid repeated execution for same column name
        String processInstanceIdsIn = getInstanceIdsIn("PROCESS_INSTANCE_ID", processInstanceIds);
        return "where \n" +
                "(OWNER_TYPE != 'VARIABLE_INSTANCE' and OWNER_TYPE != 'ADAPTER_REQUEST' and OWNER_TYPE != 'ADAPTER_REQUEST_META' and OWNER_TYPE != 'ADAPTER_RESPONSE' and OWNER_TYPE != 'ADAPTER_RESPONSE_META'\n" +
                "  and OWNER_TYPE != 'LISTENER_REQUEST' and OWNER_TYPE != 'LISTENER_REQUEST_META' and OWNER_TYPE != 'LISTENER_RESPONSE' and OWNER_TYPE != 'LISTENER_RESPONSE_META' and OWNER_TYPE != 'ACTIVITY_INSTANCE') or\n" +
                "(\n" +
                " (OWNER_TYPE = 'VARIABLE_INSTANCE' and OWNER_ID in (select VARIABLE_INST_ID from VARIABLE_INSTANCE where " + getInstanceIdsIn("PROCESS_INST_ID", processInstanceIds) + ")) or\n" +
                " (OWNER_TYPE = 'ACTIVITY_INSTANCE' and OWNER_ID in (select ACTIVITY_INSTANCE_ID from ACTIVITY_INSTANCE where " + processInstanceIdsIn + ") or\n" +
                " ((OWNER_TYPE = 'ADAPTER_REQUEST' or OWNER_TYPE = 'ADAPTER_RESPONSE') and OWNER_ID in (select ACTIVITY_INSTANCE_ID from ACTIVITY_INSTANCE where " + processInstanceIdsIn + ")) or\n" +
                " (OWNER_TYPE = 'ADAPTER_REQUEST_META' and OWNER_ID in (select DOCUMENT_ID from DOCUMENT d where d.OWNER_TYPE = 'ADAPTER_REQUEST' and d.OWNER_ID in (select ACTIVITY_INSTANCE_ID from ACTIVITY_INSTANCE where " + processInstanceIdsIn + "))) or\n" +
                " (OWNER_TYPE = 'ADAPTER_RESPONSE_META' and OWNER_ID in (select DOCUMENT_ID from DOCUMENT d where d.OWNER_TYPE = 'ADAPTER_RESPONSE' and d.OWNER_ID in (select ACTIVITY_INSTANCE_ID from ACTIVITY_INSTANCE where " + processInstanceIdsIn + "))) or\n" +
                " (OWNER_TYPE = 'TASK_INSTANCE' and OWNER_ID in (select TASK_INSTANCE_ID from TASK_INSTANCE where TASK_INSTANCE_OWNER != 'PROCESS_INSTANCE' or TASK_INSTANCE_OWNER_ID in (select ACTIVITY_INSTANCE_ID from ACTIVITY_INSTANCE where " + processInstanceIdsIn + "))) or\n" +
                " (OWNER_TYPE = 'LISTENER_REQUEST' and " + getInstanceIdsIn("OWNER_ID", processInstanceIds) + ") or\n" +
                " ((OWNER_TYPE = 'LISTENER_RESPONSE' or OWNER_TYPE = 'LISTENER_REQUEST_META') and OWNER_ID in (select DOCUMENT_ID from DOCUMENT d where d.OWNER_TYPE = 'LISTENER_REQUEST' and " + getInstanceIdsIn("d.OWNER_ID", processInstanceIds) + ")) or\n" +
                " (OWNER_TYPE = 'LISTENER_RESPONSE_META' and OWNER_ID in (select DOCUMENT_ID from DOCUMENT resp where resp.OWNER_TYPE = 'LISTENER_RESPONSE' and resp.OWNER_ID in (select DOCUMENT_ID from DOCUMENT req where req.OWNER_TYPE = 'LISTENER_REQUEST' and " + getInstanceIdsIn("req.OWNER_ID", processInstanceIds) + "))))\n" +
                ") or \n" +
                "DOCUMENT_ID in (select DOCUMENT_ID from EVENT_INSTANCE)";
    }
}
