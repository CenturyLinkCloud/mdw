package com.centurylink.mdw.dataaccess.reports;

import com.centurylink.mdw.model.Aggregate;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface AggregateSupplier<T extends Aggregate> {

    T get(ResultSet resultSet) throws SQLException;
}
