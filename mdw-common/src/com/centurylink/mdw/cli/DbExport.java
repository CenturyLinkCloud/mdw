package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.List;


/**
 * TODO: handle DocumentDb
 */
@Parameters(commandNames = "dbexport", commandDescription = "Export database tables to json (to stdout)", separators = "=")
public class DbExport extends DbOperation {

    @Parameter(names = "--output", description = "Filename of the exported output", required=true)
    private File output;
    public File getOutput() {
        return output;
    }
    public void setOutput(File file) { this.output = file; }

    @Parameter(names = "--row-limit", description = "Max rows to export for any table")
    private int rowLimit = 100000;
    public int getRowLimit() { return rowLimit; }
    public void setRowLimit(int rowLimit) { this.rowLimit = rowLimit; }

    @Parameter(names = "--process-limit", description = "Max **master** processes to export")
    private int processLimit = 10000;
    public int getProcessLimit() { return processLimit; }
    public void setProcessLimit(int processLimit) { this.processLimit = processLimit; }

    @Override
    public Operation run(ProgressMonitor... monitors) throws IOException {

        super.run(monitors);

        for (ProgressMonitor monitor : monitors)
            monitor.progress(0);

        Connection conn = null;
        try {
            conn = getDbConnection();

            // find process instance ids (10% progress)
            String procInstsSelect = "select PROCESS_INSTANCE_ID from PROCESS_INSTANCE " +
                    " where OWNER not in ('PROCESS_INSTANCE', 'MAIN_PROCESS_INSTANCE')";
            if (isOracle())
                procInstsSelect += " and rownum <= " + processLimit + " order by PROCESS_INSTANCE_ID desc";
            else
                procInstsSelect += " order by PROCESS_INSTANCE_ID desc limit " + processLimit;
            StringBuilder processInstanceIds = new StringBuilder().append("(");
            int procInstRows = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(procInstsSelect)) {
                while (rs.next()) {
                    if (procInstRows > 0)
                        processInstanceIds.append(",");
                    processInstanceIds.append(rs.getLong(1));
                    procInstRows += 1;
                }
                for (ProgressMonitor monitor : monitors)
                    monitor.progress(5);
            }
            // include all subprocs for these masters
            String subprocInstsSelect = "select PROCESS_INSTANCE_ID from PROCESS_INSTANCE " +
                    "where OWNER in ('PROCESS_INSTANCE', 'MAIN_PROCESS_INSTANCE') and OWNER_ID in " +
                    processInstanceIds.toString() + ")";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(subprocInstsSelect)) {
                while (rs.next()) {
                    if (procInstRows > rowLimit)
                        throw new IOException("PROCESS_INSTANCES exceeds --row-limit of " + rowLimit);
                    processInstanceIds.append(",");
                    processInstanceIds.append(rs.getLong(1));
                    procInstRows += 1;
                }
                for (ProgressMonitor monitor : monitors)
                    monitor.progress(10);
            }
            processInstanceIds.append(")");

            String instanceIds = processInstanceIds.toString();

            List<String> tables = getTables();
            // count rows (5% progress)
            int allRows = 0;
            for (int i = 0; i < tables.size(); i++) {
                String table = tables.get(i);
                String select = "select count(*) from " + table + " " + getWhere(table, instanceIds);
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(select)) {
                    rs.next();
                    int rows = rs.getInt(1);
                    if (rows > rowLimit)
                        throw new IOException("Table " + table + " exceeds --row-limit of " + rowLimit);
                    int prog = 10 + ((int)Math.floor((i * 5)/tables.size()));
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
                         ResultSet rs = stmt.executeQuery("select * from " + table + " " + getWhere(table, instanceIds))) {
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
                                    else
                                        bw.write(" " + JSONObject.quote("" + value));
                                    hasOne = true;
                                }
                            }
                            bw.write(" }");
                            tableRow++;
                            int prog = 15 + ((int)Math.floor((overallRow * 85) / allRows));
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
                    throw new IOException(ex);
                }
            }
        }

        return this;
    }

    private String getWhere(String table, String processInstanceIds) {
        if (table.equals("PROCESS_INSTANCE")) {
            return "where PROCESS_INSTANCE_ID in " + processInstanceIds;
        }
        else if (table.equals("ACTIVITY_INSTANCE")) {
            return "where PROCESS_INSTANCE_ID in " + processInstanceIds;
        }
        else if (table.equals("WORK_TRANSITION_INSTANCE") || table.equals("VARIABLE_INSTANCE")) {
            return "where PROCESS_INST_ID in " + processInstanceIds;
        }
        else if (table.equals("TASK_INSTANCE")) {
            return getTaskInstanceWhere(processInstanceIds);
        }
        else if (table.equals("TASK_INST_GRP_MAPP")) {
            return "where TASK_INSTANCE_ID in (select TASK_INSTANCE_ID from TASK_INSTANCE " + getTaskInstanceWhere(processInstanceIds) + ")";
        }
        else if (table.equals("DOCUMENT")) {
            return getDocumentWhere(processInstanceIds);
        }
        else if (table.equals("DOCUMENT_CONTENT")) {
            return "where DOCUMENT_ID in (select DOCUMENT_ID from DOCUMENT " + getDocumentWhere(processInstanceIds) + ")";
        }

        return "";
    }

    private String getTaskInstanceWhere(String processInstanceIds) {
        return "where TASK_INSTANCE_OWNER != 'PROCESS_INSTANCE' or TASK_INSTANCE_OWNER_ID in " + processInstanceIds;
    }

    private String getDocumentWhere(String processInstanceIds) {
        return "where \n" +
                "(OWNER_TYPE != 'VARIABLE_INSTANCE' and OWNER_TYPE != 'ADAPTER_REQUEST' and OWNER_TYPE != 'ADAPTER_REQUEST_META' and OWNER_TYPE != 'ADAPTER_RESPONSE' and OWNER_TYPE != 'ADAPTER_RESPONSE_META'\n" +
                "  and OWNER_TYPE != 'LISTENER_REQUEST' and OWNER_TYPE != 'LISTENER_REQUEST_META' and OWNER_TYPE != 'LISTENER_RESPONSE' and OWNER_TYPE != 'LISTENER_RESPONSE_META' and OWNER_TYPE != 'ACTIVITY_INSTANCE') or\n" +
                "(\n" +
                " (OWNER_TYPE = 'VARIABLE_INSTANCE' and OWNER_ID in (select VARIABLE_INST_ID from VARIABLE_INSTANCE where PROCESS_INST_ID in " + processInstanceIds + ")) or\n" +
                " (OWNER_TYPE = 'ACTIVITY_INSTANCE' and OWNER_ID in (select ACTIVITY_INSTANCE_ID from ACTIVITY_INSTANCE where PROCESS_INSTANCE_ID in " + processInstanceIds + ") or\n" +
                " ((OWNER_TYPE = 'ADAPTER_REQUEST' or OWNER_TYPE = 'ADAPTER_RESPONSE') and OWNER_ID in (select ACTIVITY_INSTANCE_ID from ACTIVITY_INSTANCE where PROCESS_INSTANCE_ID in " + processInstanceIds + ")) or\n" +
                " (OWNER_TYPE = 'ADAPTER_REQUEST_META' and OWNER_ID in (select DOCUMENT_ID from DOCUMENT d where d.OWNER_TYPE = 'ADAPTER_REQUEST' and d.OWNER_ID in (select ACTIVITY_INSTANCE_ID from ACTIVITY_INSTANCE where PROCESS_INSTANCE_ID in " + processInstanceIds + "))) or\n" +
                " (OWNER_TYPE = 'ADAPTER_RESPONSE_META' and OWNER_ID in (select DOCUMENT_ID from DOCUMENT d where d.OWNER_TYPE = 'ADAPTER_RESPONSE' and d.OWNER_ID in (select ACTIVITY_INSTANCE_ID from ACTIVITY_INSTANCE where PROCESS_INSTANCE_ID in " + processInstanceIds + "))) or\n" +
                " (OWNER_TYPE = 'TASK_INSTANCE' and OWNER_ID in (select TASK_INSTANCE_ID from TASK_INSTANCE where TASK_INSTANCE_OWNER != 'PROCESS_INSTANCE' or TASK_INSTANCE_OWNER_ID in (select ACTIVITY_INSTANCE_ID from ACTIVITY_INSTANCE where PROCESS_INSTANCE_ID in " + processInstanceIds + "))) or\n" +
                " (OWNER_TYPE = 'LISTENER_REQUEST' and OWNER_ID in " + processInstanceIds + ") or\n" +
                " ((OWNER_TYPE = 'LISTENER_RESPONSE' or OWNER_TYPE = 'LISTENER_REQUEST_META') and OWNER_ID in (select DOCUMENT_ID from DOCUMENT d where d.OWNER_TYPE = 'LISTENER_REQUEST' and d.OWNER_ID in " + processInstanceIds + ")) or\n" +
                " (OWNER_TYPE = 'LISTENER_RESPONSE_META' and OWNER_ID in (select DOCUMENT_ID from DOCUMENT resp where resp.OWNER_TYPE = 'LISTENER_RESPONSE' and resp.OWNER_ID in (select DOCUMENT_ID from DOCUMENT req where req.OWNER_TYPE = 'LISTENER_REQUEST' and req.OWNER_ID in " + processInstanceIds + "))))\n" +
                ") or \n" +
                "DOCUMENT_ID in (select DOCUMENT_ID from EVENT_INSTANCE)";
    }
}
