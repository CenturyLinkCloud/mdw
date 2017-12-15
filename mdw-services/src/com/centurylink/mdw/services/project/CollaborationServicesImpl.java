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
package com.centurylink.mdw.services.project;

import java.util.List;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Attachment;
import com.centurylink.mdw.model.Comment;
import com.centurylink.mdw.model.user.User;
import com.centurylink.mdw.service.data.CollaborationDataAccess;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.CollaborationServices;

public class CollaborationServicesImpl implements CollaborationServices {

    private CollaborationDataAccess getDataAccess() {
        return new CollaborationDataAccess();
    }

    @Override
    public List<Comment> getComments(String ownerType, Long ownerId) throws ServiceException {
        try {
          List<Comment> notes = getDataAccess().getNotes(ownerType, ownerId);
          for (Comment note : notes) {
              // Populate full user name if found.  We accept non-mdw users (slack, etc)
              String creator = note.getCreateUser();
              User mdwUser = UserGroupCache.getUser(creator);
              if (mdwUser != null)
                  note.setCreateUser(mdwUser.getName());
              String modifier = note.getModifyUser();
              if (modifier != null) {
                  mdwUser = UserGroupCache.getUser(modifier);
                  if (mdwUser != null)
                      note.setModifyUser(mdwUser.getName());
              }
          }
          return notes;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public Comment getComment(Long id) throws ServiceException {
        try {
            Comment comment = getDataAccess().getNote(id);
            if (comment == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Comment not found: " + id);
            return comment;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    @Override
    public Long createComment(Comment note) throws ServiceException {
        if (note.getCreateUser() == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing user");
        if (note.getOwnerId() == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing owner id");
        if (note.getOwnerType() == null)
            throw new ServiceException(ServiceException.BAD_REQUEST, "Missing owner type");
        try {
            Long id = getDataAccess().createNote(note.getOwnerType(), note.getOwnerId(), note.getName(), note.getContent(), note.getCreateUser());
            note.setId(id);
            return id;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void updateComment(Comment note) throws ServiceException {
        try {
            getDataAccess().updateNote(note.getId(), note.getName(), note.getContent(), note.getModifyUser());
        }
        catch (DataAccessException ex) {
          throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteComment(Long id) throws ServiceException {
        try {
            getDataAccess().deleteNote(id);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public List<Attachment> getAttachments(String ownerType, Long ownerId) throws ServiceException {
        try {
            return getDataAccess().getAttachments(ownerType, ownerId);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public Attachment getAttachment(Long id) throws ServiceException {
        try {
            Attachment attachment = getDataAccess().getAttachment(id);
            if (attachment == null)
                throw new ServiceException(ServiceException.NOT_FOUND, "Attachment not found: " + id);
            return attachment;
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    @Override
    public Long createAttachment(Attachment attachment) throws ServiceException {
        try {
            return getDataAccess().createAttachment(attachment.getOwnerType(),
                    attachment.getOwnerId(), attachment.getName(), attachment.getLocation(),
                    attachment.getContentType(), attachment.getCreateUser());
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void updateAttachment(Attachment attachment) throws ServiceException {
        try {
            getDataAccess().updateAttachment(attachment.getId(), attachment.getLocation(), attachment.getModifyUser());
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteAttachment(Long id) throws ServiceException {
        try {
            getDataAccess().deleteAttachment(id);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }
}
