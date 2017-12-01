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
package com.centurylink.mdw.service.data;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.db.CommonDataAccess;
import com.centurylink.mdw.model.Attachment;
import com.centurylink.mdw.model.Note;

public class CollaborationDataAccess extends CommonDataAccess {

    public List<Note> getNotes(String owner, Long ownerId) throws DataAccessException {
        try {
            db.openConnection();
            List<Note> notes = new ArrayList<Note>();
            String query = "select INSTANCE_NOTE_ID,INSTANCE_NOTE_NAME,INSTANCE_NOTE_DETAILS,"
                    + " CREATE_DT,CREATE_USR,MOD_DT,MOD_USR " + "from INSTANCE_NOTE "
                    + "where INSTANCE_NOTE_OWNER='" + owner + "' and INSTANCE_NOTE_OWNER_ID=? "
                    + " order by CREATE_DT desc";
            ResultSet rs = db.runSelect(query, ownerId);
            while (rs.next()) {
                Note note = new Note();
                note.setId(rs.getLong(1));
                note.setOwnerType(owner);
                note.setOwnerId(ownerId);
                note.setName(rs.getString(2));
                note.setContent(rs.getString(3));
                note.setCreated(rs.getTimestamp(4));
                note.setCreateUser(rs.getString(5));
                note.setModified(rs.getTimestamp(6));
                note.setModifyUser(rs.getString(7));
                notes.add(note);
            }
            return notes;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to get notes", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Long createNote(String owner, Long ownerId, String name, String contents, String user)
            throws DataAccessException {
        try {
            db.openConnection();
            Long id = db.isMySQL() ? null : this.getNextId("INSTANCE_NOTE_ID_SEQ");
            String query = "insert into INSTANCE_NOTE "
                    + "(INSTANCE_NOTE_ID,INSTANCE_NOTE_OWNER,INSTANCE_NOTE_OWNER_ID,"
                    + " INSTANCE_NOTE_NAME,INSTANCE_NOTE_DETAILS," + " CREATE_DT,CREATE_USR) "
                    + "values (?,?,?,?,?," + now() + ",?)";
            Object[] args = new Object[6];
            args[0] = id;
            args[1] = owner;
            args[2] = ownerId;
            args[3] = name;
            args[4] = contents;
            args[5] = user;
            if (db.isMySQL())
                id = db.runInsertReturnId(query, args);
            else
                db.runUpdate(query, args);
            db.commit();
            return id;
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException("Failed to create " + owner + " note for: " + ownerId, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void deleteNote(Long id) throws DataAccessException {
        try {
            db.openConnection();
            String query = "delete INSTANCE_NOTE where INSTANCE_NOTE_ID=?";
            db.runUpdate(query, id);
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException("Failed to delete note: " + id, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void updateNote(Long id, String name, String contents, String user) throws DataAccessException {
        try {
            db.openConnection();
            String query = "update INSTANCE_NOTE "
                    + "set INSTANCE_NOTE_NAME=?,INSTANCE_NOTE_DETAILS=?,MOD_DT=" + now()
                    + ",MOD_USR=? " + "where INSTANCE_NOTE_ID=?";
            Object[] args = new Object[4];
            args[0] = name;
            args[1] = contents;
            args[2] = user;
            args[3] = id;
            db.runUpdate(query, args);
            db.commit();
        }
        catch (Exception e) {
            db.rollback();
            throw new DataAccessException(0, "failed to update instance note", e);
        }
        finally {
            db.closeConnection();
        }
    }

    public List<Attachment> getAttachments(String ownerType, Long ownerId) throws DataAccessException {
        try {
            db.openConnection();
            List<Attachment> attachments = new ArrayList<Attachment>();
            String query = "select * from attachment where attachment_owner=? and attachment_owner_id=?";
            Object[] args = new Object[2];
            args[0] = ownerType;
            args[1] = ownerId;
            ResultSet rs = db.runSelect(query, args);
            while (rs.next()) {
                Attachment attachment = new Attachment();
                attachment.setId(rs.getLong("attachment_id"));
                attachment.setOwnerType(rs.getString("attachment_owner_type"));
                attachment.setOwnerId(rs.getLong("attachment_owner_id"));
                attachment.setName(rs.getString("attachment_name"));
                attachment.setLocation(rs.getString("attachment_location"));
                attachment.setContentType(rs.getString("attachment_content_type"));
                attachment.setCreated(rs.getTimestamp("create_dt"));
                attachment.setCreateUser(rs.getString("create_usr"));
                attachment.setModified(rs.getTimestamp("mod_dt"));
                attachment.setModifyUser(rs.getString("mod_usr"));
                attachments.add(attachment);
            }
            return attachments;
        }
        catch (Exception ex) {
            throw new DataAccessException("Failed to get attachments", ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public Long createAttachment(String ownerType, Long ownerId,
            String name, String location, String contentType, String user)
            throws DataAccessException {
        try {
            db.openConnection();
            Long id = db.isMySQL() ? null : this.getNextId("ATTACHMENT_ID_SEQ");
            String query = "insert into ATTACHMENT "
                    + "(ATTACHMENT_ID,ATTACHMENT_OWNER,ATTACHMENT_OWNER_ID,"
                    + " ATTACHMENT_NAME,ATTACHMENT_LOCATION,ATTACHMENT_CONTENT_TYPE,"
                    + " CREATE_DT,CREATE_USR) " + "values (?,?,?,?,?,?,?," + now() + ",?)";
            Object[] args = new Object[7];
            args[0] = id;
            args[1] = ownerType;
            args[2] = ownerId;
            args[3] = name;
            args[4] = location;
            args[5] = contentType;
            args[6] = user;
            if (db.isMySQL())
                id = db.runInsertReturnId(query, args);
            else
                db.runUpdate(query, args);
            db.commit();
            return id;
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException("Failed to create " + ownerType +  " attachment for: " + ownerId, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void deleteAttachment(Long id) throws DataAccessException {
        try {
            db.openConnection();
            String query = "delete from ATTACHMENT where ATTACHMENT_ID=?";
            Object[] args = new Object[1];
            args[1] = id;
            db.runUpdate(query, args);
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException("Failed to delete attachment: " + id, ex);
        }
        finally {
            db.closeConnection();
        }
    }

    public void updateAttachment(Long id, String location, String user)
            throws DataAccessException {
        try {
            db.openConnection();
            String query = "update ATTACHMENT " + "set ATTACHMENT_LOCATION=?,MOD_DT=" + now()
                    + ",MOD_USR=? " + "where ATTACHMENT_ID=?";
            Object[] args = new Object[3];
            args[0] = location;
            args[1] = user;
            args[2] = id;
            db.runUpdate(query, args);
            db.commit();
        }
        catch (Exception ex) {
            db.rollback();
            throw new DataAccessException("Failed to update attachment: " + id, ex);
        }
        finally {
            db.closeConnection();
        }
    }

}
