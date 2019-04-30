package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.Math.floor;

/**
 * TODO: handle DocumentDb
 */
@Parameters(commandNames = "dbimport", commandDescription = "Import database tables from json", separators = "=")
public class DbImport extends DbOperation {

    @Parameter(names = "--input", description = "JSON file to import (MUST be created by dbexport)", required=true)
    private File input;
    public File getInput() {
        return input;
    }
    public void setInput(File file) { this.input = file; }

    @Parameter(names="--no-prompt", description="Suppress prompt for confirmation")
    private boolean noPrompt;
    public boolean isNoPrompt() { return noPrompt; }
    public void setWarn(boolean noPrompt) { this.noPrompt = noPrompt; }

    @Override
    public Operation run(ProgressMonitor... monitors) throws IOException {
        String propDbUrl = new Props(this).get(Props.Db.URL);
        if (!getDatabaseUrl().equals(propDbUrl))
            throw new IOException("Parameter --database-url (" + propDbUrl + ") must match value in mdw.yaml for import");

        if (!noPrompt) {
            getOut().println("Import data into " + getDatabaseUrl() + "? All existing data will be OBLITERATED! Type 'yes'<Enter> to proceed.");
            String entered = new Scanner(System.in).nextLine();
            if (!"yes".equalsIgnoreCase(entered))
                return null;
        }

        super.run(monitors);

        for (ProgressMonitor monitor : monitors)
            monitor.progress(0);

        // count lines (5% progress)
        int totalLines = 0;
        try (FileReader fr = new FileReader(input);
             BufferedReader br = new BufferedReader(fr)) {
            while (br.readLine() != null)
                totalLines++;
        }
        for (ProgressMonitor monitor : monitors)
            monitor.progress(5);

        List<String> tables = new ArrayList<>(getTables());
        tables.addAll(getExcludedTables());

        // read/insert data (90% progress)
        Connection conn = null;
        try {
            conn = getDbConnection();

            // truncate tables (5% progress)
            try (Statement st = conn.createStatement()) {
                if (!isOracle())
                    st.executeUpdate("set FOREIGN_KEY_CHECKS = 0");
                for (int i = tables.size() - 1; i >= 0; i--) {
                    String table = tables.get(i);
                    String truncate = "truncate table " + table;
                    if (isOracle())
                        truncate += " cascade";
                    st.executeUpdate(truncate);
                    int prog = 5 + (int) floor((tables.size() - i) * 5) / tables.size();
                    for (ProgressMonitor monitor : monitors)
                        monitor.progress(prog);
                }
            }
            finally {
                if (!isOracle()) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate("set FOREIGN_KEY_CHECKS = 1");
                    }
                }
            }

            int lineNum = 0;
            try (FileReader fr = new FileReader(input);
                 BufferedReader br = new BufferedReader(fr);
                 Statement stmt = conn.createStatement()) {
                String line;
                String table = null;
                List<String> columns = null;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.equals("{")) {
                        if (line.startsWith("\"")) {
                            // start of table
                            table = line.substring(1, line.length() - 4);
                            columns = new ArrayList<>();
                            String select = "select * from " + table;
                            if (isOracle())
                                select += " where rownum = 1";
                            else
                                select += " limit 1";
                            try (Statement st = conn.createStatement();
                                 ResultSet rs = st.executeQuery(select)) {
                                ResultSetMetaData rsmd = rs.getMetaData();
                                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                                    columns.add(rsmd.getColumnName(i));
                                }
                            }
                        }
                        else if (line.startsWith("{") && table != null) {
                            if (line.endsWith(","))
                                line = line.substring(0, line.length() - 1);
                            JSONObject rowJson = new JSONObject(line);
                            boolean hasOne = false;
                            String insert = "insert into " + table + "\n  (";
                            String values = "values (";
                            for (int i = 0; i < columns.size(); i++) {
                                String column = columns.get(i);
                                if (rowJson.has(column)) {
                                    Object value = rowJson.get(column);
                                    if (hasOne) {
                                        insert += ", ";
                                        values += ", ";
                                    }
                                    insert += column;
                                    if (value instanceof String)
                                        values += "'" + ((String)value).replaceAll("'", "''") + "'";
                                    else
                                        values += "" + value;
                                    hasOne = true;
                                }
                            }
                            insert += ")\n";
                            values += ")\n";
                            stmt.executeUpdate(insert + values);

                        }
                        else if (line.equals("]") || line.equals("],")) {
                            // end of table
                            table = null;
                        }
                    }
                    int prog = 10 + (int) floor((lineNum * 90) / totalLines);
                    for (ProgressMonitor monitor : monitors)
                        monitor.progress(prog);
                    lineNum++;
                }
            }
            for (ProgressMonitor monitor : monitors)
                monitor.progress(100);
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
