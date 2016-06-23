/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.services.dao.bam;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.data.bam.Attribute;
import com.centurylink.mdw.model.data.bam.Component;
import com.centurylink.mdw.model.data.bam.ComponentRelation;
import com.centurylink.mdw.model.data.bam.Event;
import com.centurylink.mdw.model.data.bam.MasterRequest;

public interface BamDataAccess {

    /**
     * Load all master requests between the given dates.
     * Shallow load - does not load components/events/attributes
     * @param db
     * @param from
     * @param to
     * @return
     * @throws SQLException
     */
    public List<MasterRequest> loadMasterRequests(DatabaseAccess db, Date from, Date to) throws SQLException;

    public List<MasterRequest> loadMasterRequests(DatabaseAccess db, String queryRest) throws SQLException;

    /**
     * Load a single master request.
     * Events, components, and attributes are loaded when loadAll is true.
     * @param db
     * @param masterRequestId
     * @param realm
     * @param loadLevel: 0 - master request proper, 1 - add components, 2 - add attributes, 3 - add events
     * @return
     * @throws SQLException
     */
    public MasterRequest loadMasterRequest(DatabaseAccess db, String masterRequestId, String realm, int loadLevel)
            throws SQLException;

    /**
     * Load a single master request.
     * Events, components, and attributes are loaded when loadAll is true.
     * @param db
     * @param masterRequestId
     * @param realm
     * @param lock on bam_master_request
     * @param loadLevel: 0 - master request proper, 1 - add components, 2 - add attributes, 3 - add events
     * @return
     * @throws SQLException
     */
    public MasterRequest loadMasterRequest(DatabaseAccess db, String masterRequestId, String realm, int loadLevel, boolean lock)
            throws SQLException;
    /**
     * Load component - but not events/attributes of the component
     *
     * @param db
     * @param masterRequest
     * @param componentId
     * @return
     * @throws SQLException
     */
    public Component loadComponent(DatabaseAccess db, MasterRequest masterRequest, String componentId)
            throws SQLException;

    public List<Attribute> loadAttributes(DatabaseAccess db, Long masterRequestRowId, Long componentRowId)
            throws SQLException;

    public void persistComponents(DatabaseAccess db, Long masterRequestRowId, Long eventRowId, List<Component> components)
            throws SQLException;

    public void persistComponentRelations(DatabaseAccess db, Long masterRequestRowId,
            List<ComponentRelation> componentRelations) throws SQLException;

    public Long persistEvent(DatabaseAccess db, Long masterRequestRowId, Long componentRowId, Event event)
            throws SQLException;

    public void persistMasterRequest(DatabaseAccess db, MasterRequest masterRequest) throws SQLException;

    public void persistAttributes(DatabaseAccess db, Long masterRequestRowId, Long componentRowId,
            Long eventRowId, List<Attribute> attributes) throws SQLException;

    public void deleteComponentRelations(DatabaseAccess db, List<Component> componentList) throws SQLException;

    /**
     * Update the attribute list of the specified component or master request proper when
     * component is not specified.
     * The method assumes all attributes are existing
     * @param db
     * @param masterRequestRowId
     * @param componentRowId
     * @param attributes
     * @throws SQLException
     */
    public void updateAttributes(DatabaseAccess db, Long masterRequestRowId, Long componentRowId,
            List<Attribute> attributes) throws SQLException;

}