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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.Note;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.CollaborationServices;
import com.centurylink.mdw.services.ServiceLocator;
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
        response=Note.class, responseContainer="List")
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
            CollaborationServices collabServices = ServiceLocator.getCollaborationServices();
            List<Note> notes = collabServices.getNotes(ownerType.toUpperCase(), ownerId);
            Collections.sort(notes);
            String name = ownerType.toLowerCase() + "Notes";
            return new JSONObject();  // TODO
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
            Note note = new Note(content);
            String user = note.getCreateUser();
            User userVO = ServiceLocator.getUserManager().getUser(user);
            if (userVO == null)
                throw new ServiceException("User not found: " + user);
            CollaborationServices collabServices = ServiceLocator.getCollaborationServices();
            Long id = collabServices.createNote(note);

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
    @Path("/{noteId}")
    @ApiOperation(value="Update an instance note",
        notes="Note must contain a valid user, and content ownerId must match path.", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="Note", paramType="body", dataType="com.centurylink.mdw.model.note.InstanceNote")})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        String ownerId = getSegment(path, 1);
        try {
            validateOwnerId(path, ownerId, content.getLong("ownerId"));

            Note note = new Note(content);
            String user = note.getModifyUser();  // createdBy populated by ctor (not modBy)
            User userVO = ServiceLocator.getUserManager().getUser(user);
            if (userVO == null)
                throw new ServiceException("User not found: " + user);

            CollaborationServices collabServices = ServiceLocator.getCollaborationServices();
            collabServices.updateNote(note);

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
    @Path("/{noteId}")
    @ApiOperation(value="Delete an instance note",
        notes="Path must include ownerId and valid user.", response=StatusMessage.class)
    public JSONObject delete(String path, JSONObject content, Map<String, String> headers)
            throws ServiceException, JSONException {

        String noteId = getSegment(path, 1);

        try {
            CollaborationServices collabServices = ServiceLocator.getCollaborationServices();
            collabServices.deleteNote(Long.valueOf(noteId), getAuthUser(headers));

            if (logger.isDebugEnabled())
                logger.debug("Deleted note id: " + noteId);

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