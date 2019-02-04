package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.model.report.Aggregate;

import java.sql.ResultSet;

@FunctionalInterface
public interface Aggregator<T extends Aggregate> {

    T translate(ResultSet resultSet);
}
