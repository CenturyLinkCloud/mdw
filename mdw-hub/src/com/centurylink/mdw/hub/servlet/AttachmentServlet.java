package com.centurylink.mdw.hub.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.AuthorizationException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.Attachment;
import com.centurylink.mdw.model.user.AuthenticatedUser;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.service.data.user.UserGroupCache;
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

    private static File attachmentsRoot;

    public void init() throws ServletException {
        attachmentsRoot = new File(ApplicationContext.getAttachmentsDirectory());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        String path = request.getPathInfo().substring(1);
        try {
            authorize(request.getSession(), Action.Read, Entity.Attachment, path);
            File attachmentFile = new File(attachmentsRoot + "/" + path);
            if (!attachmentFile.isFile())
                throw new ServiceException(ServiceException.NOT_FOUND, "Not found: " + path);
            String contentType = Files.probeContentType(attachmentFile.toPath());
            if (contentType == null) {
                response.setContentType("application/octet-stream");
            }
            else {
                response.setContentType(contentType);
            }
            response.setHeader("Content-Disposition", "attachment;filename=\"" + attachmentFile.getName() + "\"");

            try (InputStream in = new FileInputStream(attachmentFile)) {
                OutputStream out = response.getOutputStream();
                int read = 0;
                byte[] bytes = new byte[FILE_BUFFER_KB * 1024];
                while ((read = in.read(bytes)) != -1)
                    out.write(bytes, 0, read);
            }
        }
        catch (NumberFormatException ex) {
            logger.error("Bad attachment request: " + path, ex);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        catch (ServiceException ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(ex.getCode());
        }
        catch (Exception ex) {
            logger.error("Attachment request error: " + path, ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        String path = request.getPathInfo().substring(1);
        try {
            authorize(request.getSession(), Action.Create, Entity.Attachment, path);
            File attachmentFile = new File(attachmentsRoot + "/" + path);
            if (!attachmentFile.getParentFile().exists() && !attachmentFile.getParentFile().mkdirs())
                throw new IOException("Unable to create directory: " + attachmentFile.getParentFile().getAbsolutePath());
            try (OutputStream out = new FileOutputStream(attachmentFile)) {
                InputStream in = request.getInputStream();
                int read = 0;
                byte[] bytes = new byte[FILE_BUFFER_KB * 1024];
                while ((read = in.read(bytes)) != -1)
                    out.write(bytes, 0, read);
            }
        }
        catch (NumberFormatException ex) {
            logger.error("Bad attachment create request: " + path, ex);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        catch (ServiceException ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(ex.getCode());
        }
        catch (Exception ex) {
            logger.error("Attachment create request error: " + path, ex);
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
            logger.error("Bad attachment delete request: " + path, ex);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        catch (ServiceException ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(ex.getCode());
        }
        catch (Exception ex) {
            logger.error("Attachment delete request error: " + path, ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Also audit logs create and delete.
     */
    private void authorize(HttpSession session, Action action, Entity entity, String location)
            throws AuthorizationException, DataAccessException {
        AuthenticatedUser user  = (AuthenticatedUser)session.getAttribute("authenticatedUser");
        if (user == null && ApplicationContext.getServiceUser() != null) {
            String cuid = ApplicationContext.getServiceUser();
            user = new AuthenticatedUser(UserGroupCache.getUser(cuid));
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
