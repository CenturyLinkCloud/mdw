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
package com.centurylink.mdw.hub.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.AuthorizationException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Attachment;
import com.centurylink.mdw.model.user.AuthenticatedUser;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

/**
 * Provides read/update access for raw asset content.
 */
@WebServlet(urlPatterns={"/attach/*"}, loadOnStartup=1)
public class AttachmentServlet extends HttpServlet {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static final int FILE_BUFFER_KB = 16;

    private File attachmentsRoot;

    public void init() throws ServletException {
        String attachDir = PropertyManager.getProperty(PropertyNames.MDW_ATTACHMENTS_DIR);
        if (attachDir != null)
            attachmentsRoot = new File(attachDir);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        String path = request.getPathInfo().substring(1);
        try {
            authorize(request.getSession(), Action.Read, Entity.Attachment, path);
            Attachment attachment = ServiceLocator.getCollaborationServices().getAttachment(Long.parseLong(path));
            File attachmentFile = new File(attachmentsRoot + "/" + attachment.getLocation());
            if ("true".equalsIgnoreCase(request.getParameter("download"))) {
                response.setHeader("Content-Disposition", "attachment;filename=\"" + attachmentFile.getName() + "\"");
                response.setContentType("application/octet-stream");
            }
            else {
                response.setContentType(attachment.getContentType());
            }
            try (InputStream in = new FileInputStream(attachmentFile)) {
                OutputStream out = response.getOutputStream();
                int read = 0;
                byte[] bytes = new byte[FILE_BUFFER_KB * 1024];
                while ((read = in.read(bytes)) != -1)
                    out.write(bytes, 0, read);
            }
        }
        catch (NumberFormatException ex) {
            logger.severeException("Bad attachment request: " + path, ex);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        catch (Exception ex) {
            logger.severeException("Attachment request error: " + path, ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        String path = request.getPathInfo().substring(1);
        try {
            authorize(request.getSession(), Action.Create, Entity.Attachment, path);
            Attachment attachment = ServiceLocator.getCollaborationServices().getAttachment(Long.parseLong(path));
            File attachmentFile = new File(attachmentsRoot + "/" + attachment.getLocation());
            try (OutputStream out = new FileOutputStream(attachmentFile)) {
                InputStream in = request.getInputStream();
                int read = 0;
                byte[] bytes = new byte[FILE_BUFFER_KB * 1024];
                while ((read = in.read(bytes)) != -1)
                    out.write(bytes, 0, read);
            }
        }
        catch (NumberFormatException ex) {
            logger.severeException("Bad attachment create request: " + path, ex);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        catch (Exception ex) {
            logger.severeException("Attachment create request error: " + path, ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        String path = request.getPathInfo().substring(1);
        try {
            authorize(request.getSession(), Action.Delete, Entity.Attachment, path);
            Attachment attachment = ServiceLocator.getCollaborationServices().getAttachment(Long.parseLong(path));
            File attachmentFile = new File(attachmentsRoot + "/" + attachment.getLocation());
            attachmentFile.delete();
        }
        catch (NumberFormatException ex) {
            logger.severeException("Bad attachment delete request: " + path, ex);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
        }
        catch (Exception ex) {
            logger.severeException("Attachment delete request error: " + path, ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Also audit logs create and delete.
     */
    private void authorize(HttpSession session, Action action, Entity entity, String location)
            throws AuthorizationException, DataAccessException {
        AuthenticatedUser user;
        if (ApplicationContext.isServiceApiOpen()) {
            String cuid = ApplicationContext.getServiceUser();
            user = new AuthenticatedUser(UserGroupCache.getUser(cuid));
        }
        else {
            user = (AuthenticatedUser)session.getAttribute("authenticatedUser");
        }

        if (user == null)
            throw new AuthorizationException(AuthorizationException.NOT_AUTHORIZED, "Authentication failure");
        if (action == Action.Create || action == Action.Delete) {
            if (!user.hasRole(Role.PROCESS_EXECUTION)) {
                throw new AuthorizationException(AuthorizationException.FORBIDDEN, "User "
                        + user.getCuid() + " not authorized for " + action + " on " + location);
            }
            logger.info("Asset mod request received from user: " + user.getCuid() + " for: " + location);
            UserAction userAction = new UserAction(user.getCuid(), action, entity, 0L, location);
            userAction.setSource(getClass().getSimpleName());
            ServiceLocator.getUserServices().auditLog(userAction);
        }
    }
}
