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
package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.PreparedSelect;
import com.centurylink.mdw.dataaccess.PreparedWhere;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.report.Aggregate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

public abstract class AggregateDataAccess<T extends Aggregate> extends CommonDataAccess {

    int DAY_MS = 24 * 60 * 60 * 1000;

    /**
     * Return the top matches according to the query conditions.
     * This is used in the dashboard UI to select desired entities for getBreakdown().
     */
    abstract List<T> getTops(Query query) throws DataAccessException, ServiceException;

    /**
     * Return the data according to the user-selected values from getTops()
     * @return TreeMap where the Date keys are sorted according to natural ordering.
     */
    abstract TreeMap<Date,List<T>> getBreakdown(Query query) throws DataAccessException, ServiceException;

    protected Date getStartDate(Query query) throws ParseException, DataAccessException {
        Instant instant = query.getInstantFilter("Starting");
        Date start = instant == null ? query.getDateFilter("startDate") : Date.from(instant);
        if (start == null)
            throw new DataAccessException("Parameter Starting is required");
        // adjust to db time
        return new Date(start.getTime() + DatabaseAccess.getDbTimeDiff());
    }

    /**
     * This is not completion date.  It's ending start date.
     */
    protected Date getEndDate(Query query) {
        Instant instant = query.getInstantFilter("Ending");
        if (instant == null)
            return null;
        else {
            return new Date(Date.from(instant).getTime() + DatabaseAccess.getDbTimeDiff());
        }
    }

    protected PreparedWhere getInCondition(List<?> elements) {
        StringBuilder in = new StringBuilder();
        List<Object> params = new ArrayList<>();
        in.append("in (");
        if (elements.isEmpty()) {
            in.append("''");  // no match -- avoid malformed sql
        }
        else {
            for (int i = 0; i < elements.size(); i++) {
                in.append("?");
                if (i < elements.size() - 1)
                    in.append(",");
                params.add(elements.get(i));
            }
        }
        in.append(") ");
        return new PreparedWhere(in.toString(), params.toArray());
    }

    private static final int MAX_LIMIT = 100;

    protected List<T> getTopAggregates(PreparedSelect select, Query query, AggregateSupplier<T> supplier)
            throws SQLException, ServiceException {
        ResultSet rs = db.runSelect(select);
        List<T> list = new ArrayList<>();
        int idx = 0;
        int limit = query.getIntFilter("limit");
        if (limit > MAX_LIMIT)
            throw new ServiceException(ServiceException.BAD_REQUEST, "limit > max (" + MAX_LIMIT + ")");
        while (rs.next() && (limit == -1 || idx < limit)) {
            list.add(supplier.get(rs));
            idx++;
        }
        return list;
    }

    /**
     * Common logic for handling breakdown results and filling in gaps.
     * Relies on convention that resultSet start date column name is "st", and value
     * column is identified as "val".
     */
    protected TreeMap<Date,List<T>> handleBreakdownResult(PreparedSelect select, Query query, AggregateSupplier supplier)
            throws DataAccessException, SQLException, ParseException {
        try {
            db.openConnection();
            ResultSet resultSet = db.runSelect(select);

            TreeMap<Date,List<T>> map = new TreeMap<>();
            Date start = getStartDate(query);
            Date prevStartDate = start;
            while (resultSet.next()) {
                String startDateStr = resultSet.getString("st");
                Date startDate = getDateFormat().parse(startDateStr);
                // fill in gaps
                while (startDate.getTime() - prevStartDate.getTime() > DAY_MS) {
                    prevStartDate = new Date(prevStartDate.getTime() + DAY_MS);
                    map.put(getRoundDate(prevStartDate), new ArrayList<>());
                }
                List<T> aggregates = map.get(startDate);
                if (aggregates == null) {
                    aggregates = new ArrayList<>();
                    map.put(startDate, aggregates);
                }

                aggregates.add((T) supplier.get(resultSet));

                prevStartDate = startDate;
            }
            // missing start date
            Date roundStartDate = getRoundDate(start);
            if (map.get(roundStartDate) == null)
                map.put(roundStartDate, new ArrayList<>());
            // gaps at end
            Date end = getEndDate(query);
            while ((end != null) && ((end.getTime() - prevStartDate.getTime()) > DAY_MS)) {
                prevStartDate = new Date(prevStartDate.getTime() + DAY_MS);
                map.put(getRoundDate(prevStartDate), new ArrayList<>());
            }

            return map;
        }
        finally {
            db.closeConnection();
        }
    }
}
