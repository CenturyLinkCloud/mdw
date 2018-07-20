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
import com.centurylink.mdw.model.Comment;

public interface CollaborationServices {

    /**
     * Get comments related to an entity instance.
     * @param ownerType = entity type (eg: TASK_INSTANCE)
     * @param ownerId = entity instance id
     * @return list of comments
     */
    public List<Comment> getComments(String ownerType, Long ownerId) throws ServiceException;
    public Comment getComment(Long id) throws ServiceException;
    public Long createComment(Comment comment) throws ServiceException;
    public void updateComment(Comment comment) throws ServiceException;
    public void deleteComment(Long id) throws ServiceException;

    /**
     * Get attachments related to an entity instance.
     * @param ownerType = entity type (eg: COMMENT)
     * @param ownerId = entity instance id
     * @return list of attachments
     */
    public List<Attachment> getAttachments(String ownerType, Long ownerId) throws ServiceException;
    public Attachment getAttachment(Long id) throws ServiceException;
    public Long createAttachment(Attachment attachment) throws ServiceException;
    public void updateAttachment(Attachment attachment) throws ServiceException;
    public void deleteAttachment(Long id) throws ServiceException;

}
