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
import com.centurylink.mdw.model.Aggregate;
import com.centurylink.mdw.model.workflow.ProcessAggregate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Supplier;

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
    @SuppressWarnings("deprecation")
    protected Date getEndDate(Query query) {
        Instant instant = query.getInstantFilter("Ending");
        if (instant == null)
            return null;
        else {
            Date end = new Date(Date.from(instant).getTime() + DatabaseAccess.getDbTimeDiff());
            if (end.getHours() == 0) {
                end = new Date(end.getTime() + DAY_MS);  // end of day
            }
            return end;
        }
    }

    @SuppressWarnings("deprecation")
    protected static Date getRoundDate(Date date) {
        Date roundDate = new Date(date.getTime());
        roundDate.setHours(0);
        roundDate.setMinutes(0);
        roundDate.setSeconds(0);
        return roundDate;
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

    protected List<T> getTopAggregates(Query query, PreparedSelect select, AggregateSupplier<T> supplier)
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
}
