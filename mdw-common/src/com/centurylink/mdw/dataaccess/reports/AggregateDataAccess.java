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
import com.centurylink.mdw.model.workflow.CompletionTimeUnit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public abstract class AggregateDataAccess<T extends Aggregate> extends CommonDataAccess {

    /**
     * Return the top matches according to the query conditions.
     * This is used in the dashboard UI to select desired entities for getBreakdown().
     */
    public abstract List<T> getTops(Query query) throws DataAccessException, ServiceException;

    /**
     * Return the data according to the user-selected values from getTops()
     * @return TreeMap where the Date keys are sorted according to natural ordering.
     */
    public abstract TreeMap<Instant,List<T>> getBreakdown(Query query) throws DataAccessException, ServiceException;

    protected Instant getStart(Query query) throws DataAccessException {
        Instant start = query.getInstantFilter("Starting");
        if (start == null)
            throw new DataAccessException("Parameter Starting is required");
        return start;
    }

    /**
     * This is not completion date.  It's ending start date.
     */
    protected Instant getEnd(Query query) {
        return query.getInstantFilter("Ending");
    }

    protected TimeIncrement getIncrement(Query query) {
        return TimeIncrement.valueOf(query.getFilter("Increment", "day"));
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
    protected TreeMap<Instant,List<T>> handleBreakdownResult(PreparedSelect select, Query query, AggregateSupplier supplier)
            throws DataAccessException, SQLException, ParseException {
        try {
            db.openConnection();
            ResultSet resultSet = db.runSelect(select);

            TreeMap<Instant,List<T>> map = new TreeMap<>();
            Instant start = getStart(query);
            TimeIncrement increment = getIncrement(query);

            Instant prevStartDate = start;
            while (resultSet.next()) {
                String startDateStr = resultSet.getString("st");
                Instant startDate = parseDbSt(startDateStr, increment);
                if (increment == TimeIncrement.day) {
                    // adjust from server time to same hour offset as request
                    int plusHours = LocalDateTime.ofInstant(start, ZoneId.systemDefault()).get(ChronoField.HOUR_OF_DAY) -
                            LocalDateTime.ofInstant(startDate, ZoneId.systemDefault()).get(ChronoField.HOUR_OF_DAY);
                    startDate = startDate.plus(plusHours, ChronoUnit.HOURS);
                }

                // fill in gaps
                while (startDate.toEpochMilli() - prevStartDate.toEpochMilli() > increment.ms) {
                    prevStartDate = Instant.ofEpochMilli(prevStartDate.toEpochMilli() + increment.ms);
                    map.put(prevStartDate, new ArrayList<>());
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
            if (map.get(start) == null)
                map.put(start, new ArrayList<>());
            // gaps at end
            Instant end = getEnd(query);
            while ((end != null) && ((end.toEpochMilli() - prevStartDate.toEpochMilli()) > increment.ms)) {
                prevStartDate = Instant.ofEpochMilli(prevStartDate.toEpochMilli() + increment.ms);
                map.put(prevStartDate, new ArrayList<>());
            }

            return map;
        }
        finally {
            db.closeConnection();
        }
    }

    /**
     * Parse db-selected datetime to an Instant.
     */
    protected Instant parseDbSt(String st, TimeIncrement increment) throws ParseException {
        if (increment == TimeIncrement.minute || increment == TimeIncrement.hour) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(st).toInstant();
        }
        else {
            return new SimpleDateFormat("yyyy-MM-dd").parse(st).toInstant();
        }
    }

    /**
     * Start datetime to select, adjusted for discrepancy between db time and server time.
     */
    protected String getSt(String col, Query query) throws DataAccessException {
        TimeIncrement increment = getIncrement(query);
        long hoursDiff = DatabaseAccess.getDbTimeDiff() / 3600000;
        if (db.isOracle()) {
            if (increment == TimeIncrement.minute) {
                return "to_char(" + col + " - interval '" + hoursDiff + "' hour,'YYYY-MM-DD HH24:MI') as st";
            }
            else if (increment == TimeIncrement.hour) {
                return "to_char(" + col + " - interval '" + hoursDiff + "' hour,'YYYY-MM-DD HH24:\"00\"') as st";

            }
            else {
                // adjust from server time to same hour offset as request
                LocalDateTime serverMidnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
                hoursDiff += LocalDateTime.ofInstant(getStart(query), ZoneId.systemDefault()).get(ChronoField.HOUR_OF_DAY) -
                        serverMidnight.get(ChronoField.HOUR_OF_DAY);
                return "to_char(" + col + " - interval '" + hoursDiff + "' hour,'YYYY-MM-DD') as st";
            }
        }
        else {
            if (increment == TimeIncrement.minute) {
                return "time_format(date_sub(" + col + ", interval " + hoursDiff + " hour),'%Y-%m-%d %H:%i') as st";
            }
            else if (increment == TimeIncrement.hour) {
                return "time_format(date_sub(" + col + ", interval " + hoursDiff + " hour),'%Y-%m-%d %H:00') as st";
            }
            else {
                // adjust from server time to same hour offset as request
                LocalDateTime serverMidnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
                hoursDiff += LocalDateTime.ofInstant(getStart(query), ZoneId.systemDefault()).get(ChronoField.HOUR_OF_DAY) -
                        serverMidnight.get(ChronoField.HOUR_OF_DAY);
                return "time_format(date_sub(" + col + ", interval " + hoursDiff + " hour), '%Y-%m-%d') as st";
            }
        }
    }

    protected CompletionTimeUnit getTimeUnit(Query query) {
        return getTimeUnit(query, null);
    }

    protected CompletionTimeUnit getTimeUnit(Query query, CompletionTimeUnit defaultUnit) {
        String completionTimesIn = query.getFilter("Completion Times In");
        if (completionTimesIn == null)
            return defaultUnit == null ? CompletionTimeUnit.Seconds : defaultUnit;
        return CompletionTimeUnit.valueOf(completionTimesIn);
    }
}
