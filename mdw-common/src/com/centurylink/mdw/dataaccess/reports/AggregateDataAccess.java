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
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.Aggregate;
import com.centurylink.mdw.model.workflow.ProcessAggregate;

import java.text.ParseException;
import java.time.Instant;
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

}
