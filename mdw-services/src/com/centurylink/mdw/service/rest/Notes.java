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
package com.centurylink.mdw.service.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.model.note.InstanceNote;
import com.centurylink.mdw.model.note.NotesList;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.TaskManager;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Notes")
@Api("Instance notes")
public class Notes extends JsonRestService {
    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_EXECUTION);
        return roles;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Role;
    }

    @Override
    @ApiOperation(value="Retrieve instance notes",
        notes="Includes all notes for the given owner type and ownerId.",
        response=InstanceNote.class, responseContainer="List")
    @ApiImplicitParams({
        @ApiImplicitParam(name="user", paramType="query", required=true, dataType="string"),
        @ApiImplicitParam(name="ownerType", paramType="query", required=true, dataType="string"),
        @ApiImplicitParam(name="ownerId", paramType="query", required=true, dataType="string")})
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        Map<String,String> parameters = getParameters(headers);
        String user = parameters.get("user");
        if (user == null)
            throw new ServiceException("Missing parameter: user");
        String ownerType = parameters.get("ownerType");
        if (ownerType == null)
            throw new ServiceException("Missing parameter: ownerType");
        String ownerIdStr = parameters.get("ownerId");
        if (ownerIdStr == null)
            throw new ServiceException("Missing parameter: ownerId");
        Long ownerId = new Long(ownerIdStr);

        try {
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            Collection<InstanceNote> notesColl = taskMgr.getNotes(ownerType.toUpperCase(), ownerId);
            List<InstanceNote> notes = new ArrayList<InstanceNote>(notesColl);
            Collections.sort(notes);
            String name = ownerType.toLowerCase() + "Notes";
            NotesList notesList = new NotesList(name, ownerType, ownerId, notes);
            notesList.setRetrieveDate(DatabaseAccess.getDbDate());
            notesList.setCount(notes.size());
            return notesList.getJson();
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * For creating a new note
     */
    @Override
    @ApiOperation(value="Create an instance note",
        notes="Note must contain a valid user.", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Note", paramType="body", dataType="com.centurylink.mdw.model.note.InstanceNote")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        try {
            InstanceNote note = new InstanceNote(content);
            String user = note.getCreatedBy();
            User userVO = ServiceLocator.getUserManager().getUser(user);
            if (userVO == null)
                throw new ServiceException("User not found: " + user);
            TaskManager taskMgr = ServiceLocator.getTaskManager();
            Long id = taskMgr.addNote(note.getOwnerType(), note.getOwnerId(), note.getNoteName(), note.getNoteDetails(), user);

            if (logger.isDebugEnabled())
                logger.debug("Added instance note id: " + id);

            return null;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }
    /**
     * For update of note.
     */
    @Override
    @Path("/{ownerId}")
    @ApiOperation(value="Update an instance note",
        notes="Note must contain a valid user, and content ownerId must match path.", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Note", paramType="body", dataType="com.centurylink.mdw.model.note.InstanceNote")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        String ownerId = getSegment(path, 1);
        try {
            validateOwnerId(path, ownerId, content.getLong("ownerId"));

            InstanceNote note = new InstanceNote(content);
            String user = note.getCreatedBy();  // createdBy populated by ctor (not modBy)
            User userVO = ServiceLocator.getUserManager().getUser(user);
            if (userVO == null)
                throw new ServiceException("User not found: " + user);

            TaskManager taskMgr = ServiceLocator.getTaskManager();
            taskMgr.updateNote(note.getOwnerType(), note.getOwnerId(), note.getNoteName(), note.getNoteDetails(), user);

            if (logger.isDebugEnabled())
                logger.debug("Updated instance for owner " + note.getOwnerType() + ", id: " + note.getOwnerId());

            return null;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }
    /**
     * Delete a note
     */
    @Override
    @Path("/{ownerId}/{user}")
    @ApiOperation(value="Delete an instance note",
        notes="Path must include ownerId and valid user.", response=StatusMessage.class)
    public JSONObject delete(String path, JSONObject content, Map<String, String> headers)
            throws ServiceException, JSONException {

        String ownerId = getSegment(path, 1);
        String user = getSegment(path, 2);
        try {
            if (user == null) {
                throw new ServiceException(HTTP_400_BAD_REQUEST, "URL for removal of Notes should be e.g /Notes/123/user : ");
            }
            User userVO = ServiceLocator.getUserManager().getUser(user);
            if (userVO == null)
                throw new ServiceException("User not found: " + user);

            TaskManager taskMgr = ServiceLocator.getTaskManager();
            taskMgr.deleteNote(Long.valueOf(ownerId), userVO.getId());

            if (logger.isDebugEnabled())
                logger.debug("Deleted note id: " + ownerId + ", user: " + user);

            return null;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * @param path
     * @param ownerId
     * @param contentownerId
     * @throws ServiceException
     */
    protected void validateOwnerId(String path, String ownerId,
            long contentownerId) throws ServiceException {
        if (!(Long.valueOf(ownerId).longValue() == contentownerId)) {
            throw new ServiceException(
                    "Url " + path + " contains a different ownerId from the content "
                            + contentownerId);
        }

    }

}