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

import static java.lang.Math.floor;

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

    @Override
    public Operation run(ProgressMonitor... monitors) throws IOException {

        super.run(monitors);

        for (ProgressMonitor monitor : monitors)
            monitor.progress(0);

        Connection conn = null;
        try {
            conn = getDbConnection();

            List<String> tables = getTables();

            // count rows (10% progress)
            int allRows = 0;
            for (int i = 0; i < tables.size(); i++) {
                String table = tables.get(i);
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("select count(*) from " + table)) {
                    rs.next();
                    int rows = rs.getInt(1);
                    if (rows > rowLimit)
                        throw new IOException("Table " + table + " exceeds --row-limit of " + rowLimit);
                    int prog = (int) floor((i * 10)/tables.size());
                    for (ProgressMonitor monitor : monitors)
                        monitor.progress(prog);
                    allRows += rows;
                }
            }

            // retrieve/write rows (90% progress)
            try (FileWriter fw = new FileWriter(output);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write("{");
                int overallRow = 0;
                for (int i = 0; i < tables.size(); i++) {
                    String table = tables.get(i);
                    bw.write("\n  \"" + table + "\": [");
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("select * from " + table)) {
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
                            int prog = 10 + (int) floor((overallRow * 90) / allRows);
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
}
