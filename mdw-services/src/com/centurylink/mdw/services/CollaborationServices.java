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
