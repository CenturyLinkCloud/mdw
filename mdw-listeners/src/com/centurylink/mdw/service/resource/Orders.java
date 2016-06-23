/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.order.OrderList;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.value.process.OrderVO;
import com.centurylink.mdw.services.OrderServices;
import com.centurylink.mdw.services.ServiceLocator;

public class Orders implements JsonService {

    public String getJson(Map<String,Object> parameters, Map<String,String> metaInfo)
    throws ServiceException {

        String masterRequestId = (String)parameters.get("masterRequestId");
        if (masterRequestId == null)
            return getAllOrders();
        else
            return getOrder(masterRequestId);
    }

    /**
     * @param masterRequestId
     * @return
     */
    private String getOrder(String masterRequestId) throws ServiceException {
        try {
            OrderServices orderServices = ServiceLocator.getOrderServices();
            OrderVO order = orderServices.getOrder(masterRequestId);
            return order.getJson().toString(2);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * @throws ServiceException
     *
     */
    private String getAllOrders() throws ServiceException {
        try {
            OrderServices orderServices = ServiceLocator.getOrderServices();
            List<OrderVO> orders = orderServices.getOrders();
            // TODO: refactor sorting into OrderList
/*            Collections.sort(orders, new Comparator<OrderVO>() {
                public int compare(OrderVO ti1, OrderVO ti2) {
                    return ti2.getId().compareTo(ti1.getId());  // descending instanceId
                }
            });*/
            OrderList orderList = new OrderList("orders", orders.subList(0, 2)); //TODO --remove sublist
            orderList.setRetrieveDate(new Date());
            orderList.setCount(orders.subList(0, 2).size());  //TODO remove restriction
            return orderList.getJson().toString(2);
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }

    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo)
    throws ServiceException {
        return getJson(parameters, metaInfo);
    }
}
