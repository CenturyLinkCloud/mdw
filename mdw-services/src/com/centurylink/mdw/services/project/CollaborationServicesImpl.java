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
import com.centurylink.mdw.model.Note;
import com.centurylink.mdw.service.data.CollaborationDataAccess;
import com.centurylink.mdw.services.CollaborationServices;

public class CollaborationServicesImpl implements CollaborationServices {

    private CollaborationDataAccess getDataAccess() {
        return new CollaborationDataAccess();
    }

    @Override
    public List<Note> getNotes(String ownerType, Long ownerId) throws ServiceException {
        try {
          return getDataAccess().getNotes(ownerType, ownerId);
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    @Override
    public Long createNote(Note note) throws ServiceException {
        try {
            return getDataAccess().createNote(note.getOwnerType(), note.getOwnerId(), note.getName(), note.getContent(), note.getCreateUser());
          }
          catch (DataAccessException ex) {
              throw new ServiceException(ex.getMessage(), ex);
          }
    }

    @Override
    public void updateNote(Note note) throws ServiceException {
        try {
            getDataAccess().updateNote(note.getId(), note.getName(), note.getContent(), note.getModifyUser());
          }
          catch (DataAccessException ex) {
              throw new ServiceException(ex.getMessage(), ex);
          }
    }

    @Override
    public void deleteNote(Long id, String user) throws ServiceException {
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
    public void deleteAttachment(Long id, String user) throws ServiceException {
        try {
            getDataAccess().deleteAttachment(id);
          }
          catch (DataAccessException ex) {
              throw new ServiceException(ex.getMessage(), ex);
          }
    }
}
