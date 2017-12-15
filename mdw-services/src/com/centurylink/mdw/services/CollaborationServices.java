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
package com.centurylink.mdw.services;

import java.util.List;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.Attachment;
import com.centurylink.mdw.model.Note;

public interface CollaborationServices {

    public List<Note> getNotes(String ownerType, Long ownerId) throws ServiceException;
    public Note getNote(Long id) throws ServiceException;
    public Long createNote(Note note) throws ServiceException;
    public void updateNote(Note note) throws ServiceException;
    public void deleteNote(Long id) throws ServiceException;

    public List<Attachment> getAttachments(String ownerType, Long ownerId) throws ServiceException;
    public Attachment getAttachment(Long id) throws ServiceException;
    public Long createAttachment(Attachment attachment) throws ServiceException;
    public void updateAttachment(Attachment attachment) throws ServiceException;
    public void deleteAttachment(Long id) throws ServiceException;

}
