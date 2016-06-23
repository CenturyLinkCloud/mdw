/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services;

import java.util.List;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.model.value.process.OrderVO;

public interface OrderServices {

    /**
     */
    public List<OrderVO> getOrders() throws DataAccessException;

    /**
     */
    public OrderVO getOrder(String masterRequestId) throws DataAccessException;

}
