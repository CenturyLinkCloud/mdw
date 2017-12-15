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

import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.types.StatusMessage;
import com.centurylink.mdw.model.Attachment;
import com.centurylink.mdw.model.JsonArray;
import com.centurylink.mdw.model.Note;
import com.centurylink.mdw.model.listener.Listener;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.services.CollaborationServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Path("/Comments")
@Api("Discussion comments")
public class Comments extends JsonRestService {

    @Override
    @Path("/{id}")
    @ApiOperation(value="Retrieve comment(s)",
        notes="If id not present, includes all comments for the given owner type and ownerId.",
        response=Note.class, responseContainer="List")
    @ApiImplicitParams({
        @ApiImplicitParam(name="ownerType", paramType="query", required=true, dataType="string"),
        @ApiImplicitParam(name="ownerId", paramType="query", required=true, dataType="string")})
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException, JSONException {
        String id = getSegment(path, 2);
        if (id != null) {
            try {
                return ServiceLocator.getCollaborationServices().getNote(Long.parseLong(id)).getJson();
            }
            catch (NumberFormatException ex) {
                throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid comment id: " + id);
            }
        }
        else {
            Query query = new Query(path, headers);
            String ownerType = query.getFilter("ownerType");
            if (ownerType == null)
                throw new ServiceException("Missing parameter: ownerType");
            Long ownerId = query.getLongFilter("ownerId");
            if (ownerId == -1)
                throw new ServiceException("Missing parameter: ownerId");
            CollaborationServices collabServices = ServiceLocator.getCollaborationServices();
            List<Note> comments = collabServices.getNotes(ownerType.toUpperCase(), ownerId);
            JSONArray commentsJson = new JSONArray();
            for (Note comment : comments)
                commentsJson.put(comment);
            return new JsonArray(commentsJson).getJson();
        }
    }

    @Override
    @ApiOperation(value="Create a comment", response=Note.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="comment", paramType="body", required=true, dataType="com.centurylink.mdw.model.Note")})
    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
    throws ServiceException, JSONException {
        Note comment = new Note(content);
        comment.setId(ServiceLocator.getCollaborationServices().createNote(comment));
        headers.put(Listener.METAINFO_HTTP_STATUS_CODE, "201");
        return comment.getJson();
    }


    @Override
    @Path("/{id}")
    @ApiOperation(value="Update a comment", response=StatusMessage.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name="comment", paramType="body", required=true, dataType="com.centurylink.mdw.model.Note"),
        @ApiImplicitParam(name="id", paramType="path", required=true)})
    public JSONObject put(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        ServiceLocator.getCollaborationServices().updateNote(new Note(content));
        return null;
    }

    @Override
    @Path("/{id}")
    @ApiOperation(value="Delete a comment", response=StatusMessage.class)
    public JSONObject delete(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        String id = getSegment(path, 1);
        if (id == null)
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Missing comment id: " + path);
        try {
            ServiceLocator.getCollaborationServices().deleteNote(Long.parseLong(id));
            return null;
        }
        catch (NumberFormatException ex) {
            throw new ServiceException(HTTP_400_BAD_REQUEST, "Invalid comment id: " + id);
        }
    }

    @Override
    public List<String> getRoles(String path) {
        List<String> roles = super.getRoles(path);
        roles.add(Role.PROCESS_EXECUTION);
        return roles;
    }

    @Override
    protected Entity getEntity(String path, Object content, Map<String,String> headers) {
        return Entity.Note;
    }
}