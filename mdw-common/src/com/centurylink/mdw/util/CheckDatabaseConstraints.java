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
package com.centurylink.mdw.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import com.centurylink.mdw.dataaccess.DatabaseAccess;

public class CheckDatabaseConstraints {

    private void check(String dburl, String owner) throws SQLException {
        
        DatabaseAccess db = new DatabaseAccess(dburl);
        try {
            db.openConnection();
            StringBuffer sb;
            ResultSet rs;
            HashMap<String,String> fk_index = new HashMap<String,String>();
            
            // indexes
            sb = new StringBuffer();
            sb.append("select i.table_name,ic.column_name,i.index_name,i.uniqueness ");
            sb.append("from all_indexes i, all_ind_columns ic ");
            sb.append("where i.table_owner='" + owner + "' ");
            sb.append("and ic.table_owner='" + owner + "' ");
            sb.append("and i.index_name=ic.index_name ");
            sb.append("order by i.table_name,ic.column_name");
            rs = db.runSelect(sb.toString(), null);
            System.out.println("\nall indexes\n===============");
            System.out.format("%-24s %-28s %-28s %-12s\n",
                    "TABLE_NAME", "COLUMN_NAME", "INDEX_NAME", "UNIQUENESS");
            while (rs.next()) {
                String table_name = rs.getString(1);
                String column_name = rs.getString(2);
                String index_name =  rs.getString(3);
                System.out.format("%-24s %-28s %-28s %-12s\n",
                        table_name, column_name,index_name, rs.getString(4));
                fk_index.put(table_name+":"+column_name, index_name);
            }
            
            // primary key constraints
            sb = new StringBuffer();
            sb.append("select table_name,constraint_name,index_name from all_constraints ");
            sb.append("where owner='" + owner + "' ");
            sb.append("and constraint_type='P' and status='ENABLED' ");
            sb.append("order by table_name");
            rs = db.runSelect(sb.toString(), null);
            System.out.println("\nprimary key constraints\n===============");
            System.out.format("%-24s %-28s %-28s\n",
                    "TABLE_NAME", "CONSTRAINT_NAME", "INDEX_NAME");
            while (rs.next()) {
                System.out.format("%-24s %-28s %-28s\n",
                        rs.getString(1), rs.getString(2), rs.getString(3));
            }
            
            // foreign key constraints
            sb = new StringBuffer();
            sb.append("select c.table_name,cc.column_name,c.constraint_name,c.r_constraint_name ");
            sb.append("from all_constraints c, all_cons_columns cc ");
            sb.append("where c.owner='" + owner + "' and cc.owner='" + owner +"' ");
            sb.append("and c.constraint_type='R' and c.status='ENABLED' ");
            sb.append("and cc.table_name=c.table_name and cc.constraint_name=c.constraint_name ");
            sb.append("order by c.table_name, cc.column_name ");
            rs = db.runSelect(sb.toString(), null);
            System.out.println("\nforeign key constraints\n===============");
            System.out.format("%-24s %-28s %-28s %-5s\n",
                    "TABLE_NAME", "COLUMN_NAME", "CONSTRAINT_NAME", "INDEX");
            while (rs.next()) {
                String table_name = rs.getString(1);
                String column_name = rs.getString(2);
                String index = fk_index.get(table_name+":"+column_name)==null?"no":"yes";
                System.out.format("%-24s %-28s %-28s %-5s\n",
                        table_name, column_name, rs.getString(3), index);
            }
            
            // unique constraints
            sb = new StringBuffer();
            sb.append("select table_name,constraint_name,index_name from all_constraints ");
            sb.append("where owner='" + owner + "' ");
            sb.append("and constraint_type='U' and status='ENABLED' ");
            sb.append("order by table_name");
            rs = db.runSelect(sb.toString(), null);
            System.out.println("\nUNIQUE constraints\n===============");
            System.out.format("%-24s %-16s %-40s\n",
                    "TABLE_NAME", "CONSTRAINT_NAME", "INDEX_NAME");
            while (rs.next()) {
                System.out.format("%-24s %-16s %-40s\n",
                        rs.getString(1), rs.getString(2), rs.getString(3));
            }
            
            // other constraints
            sb = new StringBuffer();
            sb.append("select table_name,constraint_name,search_condition from all_constraints ");
            sb.append("where owner='" + owner + "' ");
            sb.append("and constraint_type='C' and status='ENABLED' ");
            sb.append("order by table_name");
            rs = db.runSelect(sb.toString(), null);
            System.out.println("\nNOT NULL and other constraints\n===============");
            System.out.format("%-24s %-16s %-40s\n",
                    "TABLE_NAME", "CONSTRAINT_NAME", "INDEX_NAME");
            while (rs.next()) {
                System.out.format("%-24s %-16s %-40s\n",
                        rs.getString(1), rs.getString(2), rs.getString(3));
            }
            
        } finally {
            db.closeConnection();
        }
        
    }
    
    
    
    public static void main(String[] args) throws Exception {
        CheckDatabaseConstraints me = new CheckDatabaseConstraints();
        String dburl, owner;
//        dburl = "jdbc:oracle:thin:mdwdemo/mdwdemo@mdwdevdb.dev.qintra.com:1594:mdwdev";
//        owner = "MDWDEMO";
        dburl = "jdbc:oracle:thin:mdw/mdw@mdwdevdb.dev.qintra.com:1594:mdwdev";
        owner = "MDW";
//        dburl = "jdbc:oracle:thin:epwf_app/epwf_app_epwfst1@epwfst1db.dev.qintra.com:1602:epwfst1";
//        owner = "EPWF_MDW";
//        dburl = "jdbc:oracle:thin:iom_app/iom_app123@iomprddb.qintra.com:1525:iomprd";
//        owner = "MDW";
//        dburl = "jdbc:oracle:thin:iom_app/iomst10g_suomt100@iomst10gdb.dev.qintra.com:1677:iomst10g";
//        owner = "MDW";
        me.check(dburl, owner);
    }
}
