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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.cli.Checkpoint;
import com.centurylink.mdw.common.service.AuthorizationException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.SystemMessages;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.AssetRef;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.DbAccess;
import com.centurylink.mdw.dataaccess.ProcessPersister.PersistType;
import com.centurylink.mdw.dataaccess.file.ImporterExporterJson;
import com.centurylink.mdw.dataaccess.file.LoaderPersisterVcs;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.dataaccess.file.VcsArchiver;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.StatusResponse;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.system.Bulletin;
import com.centurylink.mdw.model.system.SystemMessage.Level;
import com.centurylink.mdw.model.user.AuthenticatedUser;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.user.Workgroup;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.service.data.task.UserGroupCache;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.asset.Renderer;
import com.centurylink.mdw.services.asset.RenderingException;
import com.centurylink.mdw.services.cache.CacheRegistration;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.file.ZipHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.LoggerProgressMonitor;
import com.centurylink.mdw.util.timer.ProgressMonitor;

/**
 * Provides read/update access for raw asset content.
 */
@WebServlet(urlPatterns = { "/asset/*" }, loadOnStartup = 1)
public class AssetContentServlet extends HttpServlet {

    private static final StandardLogger logger = LoggerUtil.getStandardLogger();

    private File assetRoot;

    public void init() throws ServletException {
        assetRoot = ApplicationContext.getAssetRoot();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!assetRoot.isDirectory())
            throw new ServletException(assetRoot + " is not a directory");

        String path = request.getPathInfo().substring(1);
        try {
            authorizeForView(request.getSession(), path);
        }
        catch (AuthorizationException ex) {
            logger.severeException(ex.getMessage(), ex);
            StatusResponse sr = new StatusResponse(ex.getCode(), ex.getMessage());
            response.setStatus(sr.getStatus().getCode());
            response.getWriter().println(sr.getJson().toString(2));
            return;
        }

        if ("packages".equals(path)) {
            String packages = request.getParameter("packages");
            if (packages == null) {
                StatusResponse sr = new StatusResponse(Status.BAD_REQUEST, "Missing parameter: 'packages'");
                response.setStatus(sr.getStatus().getCode());
                response.getWriter().println(sr.getJson().toString(2));
            }
            else {
                String recursive = request.getParameter("recursive");
                boolean includeSubPkgs = recursive == null ? false
                        : recursive.equalsIgnoreCase("true") ? true : false;
                response.setHeader("Content-Disposition", "attachment;filename=\"packages.zip\"");
                response.setContentType("application/octet-stream");
                try {
                    List<File> includes = new ArrayList<File>();
                    for (String pkgName : getPackageNames(packages))
                        includes.add(new File(assetRoot + "/" + pkgName.replace('.', '/')));
                    ZipHelper.writeZipWith(assetRoot, response.getOutputStream(), includes,
                            includeSubPkgs);
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            }
        }
        else {
            if (path.indexOf('/') == -1) {
                // must be qualified
                StatusResponse sr = new StatusResponse(Status.NOT_FOUND);
                response.setStatus(sr.getStatus().getCode());
                response.getWriter().println(sr.getJson().toString(2));
                return;
            }

            boolean gitRemote = false;
            String render = request.getParameter("render");
            File assetFile;
            if (path.startsWith("Archive")) {
                assetFile = new File(assetRoot + "/" + path);
            }
            else {
                AssetInfo asset = new AssetInfo(assetRoot, path);
                gitRemote = "true".equalsIgnoreCase(request.getParameter("gitRemote"));

                assetFile = asset.getFile();
                if ("true".equalsIgnoreCase(request.getParameter("download"))) {
                    response.setHeader("Content-Disposition",
                            "attachment;filename=\"" + asset.getFile().getName() + "\"");
                    response.setContentType("application/octet-stream");
                }
                else {
                    if (render == null) {
                        response.setContentType(asset.getContentType());
                    }
                    else {
                        try {
                            Renderer renderer = ServiceLocator.getAssetServices().getRenderer(path, render.toUpperCase());
                            if (renderer == null)
                                throw new RenderingException(ServiceException.NOT_FOUND, "Renderer not found: " + render);
                            String contentType = Asset.getContentType(render.toUpperCase());
                            if (contentType != null)
                                response.setContentType(contentType);
                            Map<String,String> options = new HashMap<>();
                            Enumeration<String> paramNames = request.getParameterNames();
                            while (paramNames.hasMoreElements()) {
                                String paramName = paramNames.nextElement();
                                options.put(paramName, request.getParameter(paramName));
                            }
                            response.getOutputStream().write(renderer.render(options));
                        }
                        catch (ServiceException ex) {
                            logger.severeException(ex.getMessage(), ex);
                            StatusResponse sr = new StatusResponse(ex.getCode(), ex.getMessage());
                            response.setStatus(sr.getStatus().getCode());
                            response.getWriter().println(sr.getJson().toString(2));
                        }
                        return;
                    }
                }
            }

            InputStream in = null;
            OutputStream out = response.getOutputStream();
            try {
                if (gitRemote) {
                    String branch = PropertyManager.getProperty(PropertyNames.MDW_GIT_BRANCH);
                    if (branch == null)
                        throw new PropertyException("Missing required property: " + PropertyNames.MDW_GIT_BRANCH);
                    AssetServices assetServices = ServiceLocator.getAssetServices();
                    VersionControlGit vcGit = (VersionControlGit) assetServices.getVersionControl();
                    String gitPath = vcGit.getRelativePath(assetFile);
                    in = vcGit.getRemoteContentStream(branch, gitPath);
                    if (in == null)
                        throw new IOException("Git remote not found: " + gitPath);
                }
                else {
                    if (!assetFile.isFile()) {
                        StatusResponse sr = new StatusResponse(Status.NOT_FOUND, "Asset file '" + assetFile + "' not found");
                        response.setStatus(sr.getStatus().getCode());
                        out.write(sr.getJson().toString(2).getBytes());
                        return;
                    }
                    in = new FileInputStream(assetFile);
                }

                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = in.read(bytes)) != -1)
                    out.write(bytes, 0, read);
            }
            catch (Exception ex) {
                logger.severeException(ex.getMessage(), ex);
            }
            finally {
                if (in != null)
                    in.close();
            }
        }
    }

    /**
     * Distributed operations support does not include package import.
     * authorization for distributed requests handled by issue #222.
     */
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!assetRoot.isDirectory())
            throw new ServletException(assetRoot + " is not a directory");

        String path = request.getPathInfo().substring(1);
        Bulletin bulletin = null;
        try {
            LoaderPersisterVcs persisterVcs = (LoaderPersisterVcs) DataAccess.getProcessPersister();
            AssetServices assetServices = ServiceLocator.getAssetServices();
            try {
                if ("packages".equals(path)) {
                    authorizeForUpdate(request.getSession(), Action.Import, Entity.Package, "Package zip");
                    String contentType = request.getContentType();
                    boolean isZip = "application/zip".equals(contentType);
                    if (!isZip && !"application/json".equals(contentType))
                        throw new ServiceException(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                                "Unsupported content: " + contentType);
                    File tempDir = new File(ApplicationContext.getTempDirectory());
                    String fileExt = isZip ? ".zip" : ".json";
                    File tempFile = new File(tempDir + "/packageImport_"
                            + StringHelper.filenameDateToString(new Date()) + fileExt);
                    logger.info("Saving package import temporary file: " + tempFile);
                    FileHelper.writeToFile(request.getInputStream(), tempFile);
                    ProgressMonitor progressMonitor = new LoggerProgressMonitor(logger);
                    VersionControlGit vcs = (VersionControlGit) assetServices.getVersionControl();
                    progressMonitor.start("Archive existing assets");
                    if (VcsArchiver.setInProgress()) {
                        bulletin = SystemMessages.bulletinOn("Asset import in progress...");
                        VcsArchiver archiver = new VcsArchiver(assetRoot, tempDir, vcs,
                                progressMonitor);
                        archiver.backup();
                        if (isZip) {
                            logger.info("Unzipping " + tempFile + " into: " + assetRoot);
                            ZipHelper.unzip(tempFile, assetRoot, null, null, true);
                        }
                        else {
                            logger.info("Importing " + tempFile + " into: " + assetRoot);
                            ImporterExporterJson importer = new ImporterExporterJson();
                            String packageJson = new String(FileHelper.read(tempFile));
                            List<Package> packages = importer.importPackages(packageJson);
                            for (Package pkg : packages) {
                                PackageDir pkgDir = persisterVcs
                                        .getTopLevelPackageDir(pkg.getName());
                                if (pkgDir == null) {
                                    // new pkg
                                    pkgDir = new PackageDir(persisterVcs.getStorageDir(), pkg,
                                            persisterVcs.getVersionControl());
                                    pkgDir.setYaml(true);
                                }
                                persisterVcs.save(pkg, pkgDir, true);
                                pkgDir.parse(); // sync
                            }
                        }
                        archiver.archive();
                        SystemMessages.bulletinOff(bulletin, "Asset import completed");
                        bulletin = null;
                        Thread thread = new Thread() {
                            @Override
                            public void run() {
                                this.setName("AssetPackagesCacheRefresh-thread");
                                CacheRegistration.getInstance().refreshCaches(null);
                            }
                        };
                        thread.start();
                    }
                    else {
                        throw new ServiceException(ServiceException.CONFLICT,
                                "Asset import was NOT performed since an import was already in progress...");
                    }
                    progressMonitor.done();
                }
                else {
                    authorizeForUpdate(request.getSession(), Action.Change, Entity.Asset, path);

                    int lastSlash = path.lastIndexOf('/');
                    if (lastSlash == -1 || lastSlash > path.length() - 2)
                        throw new ServiceException(ServiceException.BAD_REQUEST,
                                "Bad path: " + path);
                    String pkgName = path.substring(0, lastSlash);
                    Package pkg = persisterVcs.getPackage(pkgName);
                    if (pkg == null)
                        throw new ServiceException(ServiceException.NOT_FOUND,
                                "Package not found: " + pkgName);

                    String assetName = path.substring(lastSlash + 1);
                    String version = request.getParameter("version");
                    boolean verChange = false;
                    Asset asset = null;
                    PackageDir pkgDir = persisterVcs.getTopLevelPackageDir(pkgName);

                    // TODO event handler and implementors
                    if (assetName.endsWith(".proc")) {
                        asset = persisterVcs.getProcessBase(
                                pkgName + "/" + assetName.substring(0, assetName.length() - 5), 0);
                    }
                    else if (assetName.endsWith(".task")) {
                        asset = persisterVcs.loadTaskTemplate(pkgDir, pkgDir.getAssetFile(new File(
                                assetRoot + "/" + pkgName.replace('.', '/') + "/" + assetName)));
                    }
                    else {
                        asset = persisterVcs.getAsset(pkg.getId(), assetName);
                    }

                    if (asset == null)
                        throw new ServiceException(ServiceException.NOT_FOUND,
                                "Asset not found: " + pkgName + "/" + assetName);

                    if (version == null)
                        version = asset.getVersionString();

                    logger.info("Saving asset: " + pkgName + "/" + assetName + " v" + version);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    InputStream is = request.getInputStream();
                    int read = 0;
                    byte[] bytes = new byte[1024];
                    while ((read = is.read(bytes)) != -1)
                        baos.write(bytes, 0, read);
                    byte[] content = baos.toByteArray();

                    int ver = asset.getVersion();

                    if (asset instanceof Process) {
                        asset = new Process(new JsonObject(new String(content)));
                        asset.setName(assetName.substring(0, assetName.length() - 5));
                        asset.setPackageName(pkgName);
                    }
                    else {
                        asset.setRawContent(content);
                    }

                    int newVer = Asset.parseVersion(version);
                    if (newVer < ver)
                        throw new ServiceException(ServiceException.BAD_REQUEST,
                                "Invalid asset version: v" + version);

                    // update ASSET_REF with current info before saving
                    verChange = newVer != ver;
                    if (verChange) {
                        VersionControlGit vc = (VersionControlGit) assetServices
                                .getVersionControl();
                        if (vc != null && vc.getCommit() != null) {
                            String curPath = pkgName + "/" + assetName + " v"
                                    + Asset.formatVersion(ver);
                            AssetRef curRef = new AssetRef(curPath, vc.getId(new File(curPath)),
                                    vc.getCommit());
                            try (DbAccess dbAccess = new DbAccess()) {
                                Checkpoint cp = new Checkpoint(assetServices.getAssetRoot(), vc,
                                        curRef.getRef(), dbAccess.getConnection());
                                cp.updateRef(curRef);
                            }
                        }
                    }

                    asset.setVersion(newVer);
                    if (asset instanceof Process) {
                        persisterVcs.save((Process) asset, pkgDir);
                        persisterVcs.updateProcess((Process) asset);
                    }
                    else {
                        persisterVcs.save(asset, pkgDir);
                        persisterVcs.updateAsset(asset);
                    }

                    if (verChange) {
                        persisterVcs.persistPackage(pkg, PersistType.NEW_VERSION);
                    }
                    logger.info("Asset saved: " + path + " v" + version);

                    if (ApplicationContext.isDevelopment()) { // Only Dev mode
                                                              // allows for
                                                              // saving without
                                                              // also committing
                                                              // and pushing to
                                                              // Git
                        Thread thread = new Thread() {
                            @Override
                            public void run() {
                                this.setName("AssetSaveCacheRefresh-thread");
                                CacheRegistration.getInstance().refreshCaches(null);
                            }
                        };
                        thread.start();
                    }

                    response.getWriter().write(new StatusResponse(200, "OK").getJson().toString(2));
                }
            }
            catch (ServiceException ex) {
                logger.severeException(ex.getMessage(), ex);
                SystemMessages.bulletinOff(bulletin, Level.Error,
                        "Asset import failed: " + ex.getMessage());
                response.getWriter().write(ex.getStatusResponse().getJson().toString(2));
                StatusResponse sr = new StatusResponse(ex.getCode(), ex.getMessage());
                response.setStatus(sr.getStatus().getCode());
                response.getWriter().println(sr.getJson().toString(2));
            }
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            SystemMessages.bulletinOff(bulletin, Level.Error,
                    "Asset import failed: " + ex.getMessage());
            StatusResponse sr = new StatusResponse(Status.INTERNAL_SERVER_ERROR, ex.getMessage());
            response.setStatus(sr.getStatus().getCode());
            response.getWriter().println(sr.getJson().toString(2));
        }
    }

    private String[] getPackageNames(String packagesParam) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("packages", packagesParam);
        Query query = new Query("", params);
        return query.getArrayFilter("packages");
    }

    /**
     * Also audit logs (if not distributed propagation).
     */
    private void authorizeForUpdate(HttpSession session, Action action, Entity entity,
            String includes) throws AuthorizationException, DataAccessException {
        AuthenticatedUser user = (AuthenticatedUser) session.getAttribute("authenticatedUser");
        if (user == null && ApplicationContext.getServiceUser() != null) {
            String cuid = ApplicationContext.getServiceUser();
            user = new AuthenticatedUser(UserGroupCache.getUser(cuid));
        }

        if (user == null)
            throw new AuthorizationException(AuthorizationException.NOT_AUTHORIZED, "Authentication failure");
        if (!user.hasRole(Role.ASSET_DESIGN) && !user.hasRole(Workgroup.SITE_ADMIN_GROUP)) {
            throw new AuthorizationException(AuthorizationException.FORBIDDEN,
                    "User " + user.getCuid() + " not authorized for this action");
        }

        logger.info("Asset mod request received from user: " + user.getCuid() + " for: " + includes);
        UserAction userAction = new UserAction(user.getCuid(), action, entity, 0L, includes);
        userAction.setSource(getClass().getSimpleName());
        ServiceLocator.getUserServices().auditLog(userAction);
    }

    /**
     * Only if "Asset View" role exists.  Web resource assets are excluded.
     */
    private void authorizeForView(HttpSession session, String path) throws AuthorizationException {
        if (UserGroupCache.getRole(Role.ASSET_VIEW) != null) {
            if (!path.endsWith(".css") && !path.endsWith(".js") && !path.endsWith(".jpg") && !path.endsWith(".png")
                    && !path.endsWith(".gif") && !path.endsWith("woff") && !path.endsWith("woff2") && !path.endsWith("ttf")) {
                AuthenticatedUser user = (AuthenticatedUser) session.getAttribute("authenticatedUser");
                if (user == null && ApplicationContext.getServiceUser() != null) {
                    String cuid = ApplicationContext.getServiceUser();
                    user = new AuthenticatedUser(UserGroupCache.getUser(cuid));
                }
                if (user == null)
                    throw new AuthorizationException(AuthorizationException.NOT_AUTHORIZED, "Authentication failure");
                if (!user.hasRole(Role.ASSET_VIEW) && !user.hasRole(Role.ASSET_DESIGN) && !user.hasRole(Workgroup.SITE_ADMIN_GROUP)) {
                    throw new AuthorizationException(AuthorizationException.FORBIDDEN,
                            "User " + user.getCuid() + " not authorized for " + path);
                }
            }
        }
    }
}