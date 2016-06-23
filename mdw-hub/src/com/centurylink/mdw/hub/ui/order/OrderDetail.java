/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.ui.order;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.data.bam.Attribute;
import com.centurylink.mdw.model.data.bam.Event;
import com.centurylink.mdw.model.data.bam.MasterRequest;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.services.dao.bam.BamDataAccess;
import com.centurylink.mdw.services.dao.bam.BamDataAccessDao;
import com.centurylink.mdw.taskmgr.ui.layout.DetailUI;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.xml.DomHelper;
import com.centurylink.orderRepository.OrderRepositoryServiceRequestDocument;
import com.centurylink.orderRepository.OrderRepositoryServiceRequestT;
import com.centurylink.orderRepository.OrderRepositoryServiceRequestT.OperationName;

/**
 * Default order detail has no functionality out-of-the-box except the ability
 * to display a workflow snapshot and a task list for an order.
 */
public class OrderDetail extends com.centurylink.mdw.taskmgr.ui.orders.detail.OrderDetail {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public OrderDetail(DetailUI detailUI) {
        super(detailUI);
    }

    public OrderDetail() {
        super(null);
        orderServiceUrl = PropertyManager.getProperty("mdw.order.service.url");
        realm = PropertyManager.getProperty("mdw.bam.realm");
    }

    private String detailData;

    /**
     * @return the detailData
     */
    public String getDetailData() {
        HttpHelper httpHelper = null;
        try {
            URL url = new URL(orderServiceUrl + "/Services");
            httpHelper = new HttpHelper(url);
            OrderRepositoryServiceRequestDocument orderreqDoc = OrderRepositoryServiceRequestDocument.Factory
                    .newInstance();
            OrderRepositoryServiceRequestT orderReq = orderreqDoc
                    .addNewOrderRepositoryServiceRequest();
            orderReq.setOrderNumber(this.getMasterRequestId());
            orderReq.setOperationName(OperationName.Enum.forString("RetrieveOrder"));
            orderReq.setRealm(realm);
            AuthenticatedUser authUser = FacesVariableUtil.getCurrentUser();
            orderReq.setRequester(authUser.getCuid());
            orderReq.setRequestTime(Calendar.getInstance());
            logger.mdwDebug("<-------------orderreqDoc is ---->" + orderreqDoc.xmlText());
            detailData = httpHelper.post(orderreqDoc.xmlText());
            XmlObject orderXMLBean = XmlObject.Factory.parse(detailData);
            XmlOptions options = new XmlOptions();
            options.setSavePrettyPrint();
            options.setSaveAggressiveNamespaces();
            options.setSavePrettyPrintIndent(2);
            detailData = orderXMLBean.xmlText(options);
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        finally{

        }
        return detailData;
    }

    /**
     * @return the transientData
     */
    public String getTransientData() {
        HttpHelper httpHelper = null;
        try {
            URL url = new URL(orderServiceUrl + "/Services");
            httpHelper = new HttpHelper(url);

            OrderRepositoryServiceRequestDocument orderreqDoc = OrderRepositoryServiceRequestDocument.Factory
                    .newInstance();
            OrderRepositoryServiceRequestT orderReq = orderreqDoc
                    .addNewOrderRepositoryServiceRequest();
            orderReq.setOrderNumber(this.getMasterRequestId());
            orderReq.setOperationName(OperationName.Enum.forString("GetAllTransientData"));
            orderReq.setRealm(realm);
            AuthenticatedUser authUser = FacesVariableUtil.getCurrentUser();
            orderReq.setRequester(authUser.getCuid());
            orderReq.setRequestTime(Calendar.getInstance());
            orderReq.setOrderFormat("Equipment Build");
            logger.mdwDebug("<-------------orderreqDoc is ---->" + orderreqDoc.xmlText());
            transientData = httpHelper.post(orderreqDoc.xmlText());
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        finally{

        }
        return transientData;
    }

    /**
     * @param transientData
     *            the transientData to set
     */
    public void setTransientData(String transientData) {
        this.transientData = transientData;
    }

    private String transientData;

    /**
     * @return the eventData
     */
    public List<Event> getEventData() {
        if (bamMasterRequest == null)
            bamMasterRequest = this.getBamMasterRequest();
        if (bamMasterRequest != null)
            this.eventData = bamMasterRequest.getEvents();
        else
            return null;
        return this.eventData;
    }

    /**
     * @param eventData
     *            the eventData to set
     */
    public void setEventData(List<Event> eventData) {
        this.eventData = eventData;
    }

    private List<Event> eventData;

    /**
     * @return the bamAttrData
     */
    public List<Attribute> getBamAttrData() {
        if (bamMasterRequest == null)
            bamMasterRequest = this.getBamMasterRequest();
        if (bamMasterRequest != null)
            this.bamAttrData = bamMasterRequest.getAttributes();
        else
            return null;
        return this.bamAttrData;
    }

    /**
     * @param bamAttrData
     *            the bamAttrData to set
     */
    public void setBamAttrData(List<Attribute> bamAttrData) {
        this.bamAttrData = bamAttrData;
    }

    private List<Attribute> bamAttrData;
    private MasterRequest bamMasterRequest;

    /**
     * @return the bamMasterRequest
     */
    public MasterRequest getBamMasterRequest() {
        DatabaseAccess db = null;
        db = new DatabaseAccess(null);
        try {
            db.openConnection();
            BamDataAccess dbhelper = new BamDataAccessDao();
            if (this.getMasterRequestId() != null)
                bamMasterRequest = dbhelper.loadMasterRequest(db, this.getMasterRequestId(), realm,
                        3);
            db.commit();
        }
        catch (SQLException e) {
            logger.severeException(
                    "Unable to get BAM data for masterRequestId =" + this.getMasterRequestId()
                            + " and realm =" + realm + e.getMessage(), e);
            db.rollback();
            return null;
        }
        finally {
            if (db != null)
                db.closeConnection();
        }
        return bamMasterRequest;
    }

    /**
     * @param bamMasterRequest
     *            the bamMasterRequest to set
     */
    public void setBamMasterRequest(MasterRequest masterRequest) {
        this.bamMasterRequest = masterRequest;
    }

    private String orderServiceUrl;
    private String realm;

    public static void main(String[] args) {
        String request = "<ord:OrderRepositoryServiceResponse xmlns:ord=\"http://www.centurylink.com/OrderRepository\">"
                + "    <ord:Status>0</ord:Status>"
                + "    <ord:OrderContent>Hello</ord:OrderContent>"
                + "</ord:OrderRepositoryServiceResponse>";
        try {
            Document doc = DomHelper.toDomDocument(request);
            String ns = DomHelper.toDomDocument(request).getFirstChild().getNamespaceURI();
            System.out.println(ns);

            NodeList nodeList = doc.getChildNodes();
            if (nodeList != null) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    System.out.println(" 1 " + nodeList.item(i).getTextContent());
                    System.out.println(" 2 " + nodeList.item(i).getFirstChild().getTextContent());

                }
            }
            nodeList = nodeList.item(0).getChildNodes();
            if (nodeList != null) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    System.out.println("  i= " + i + " "
                            + nodeList.item(i).getFirstChild().getNodeValue());
                }
            }
        }

        catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        catch (SAXException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}
