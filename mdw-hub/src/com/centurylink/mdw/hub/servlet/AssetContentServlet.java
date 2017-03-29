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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.AuthorizationException;
import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.dataaccess.ProcessPersister.PersistType;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.ImporterExporterJson;
import com.centurylink.mdw.dataaccess.file.LoaderPersisterVcs;
import com.centurylink.mdw.dataaccess.file.PackageDir;
import com.centurylink.mdw.dataaccess.file.VcsArchiver;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.user.AuthenticatedUser;
import com.centurylink.mdw.model.user.Role;
import com.centurylink.mdw.model.user.UserAction;
import com.centurylink.mdw.model.user.UserAction.Action;
import com.centurylink.mdw.model.user.UserAction.Entity;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.util.StringHelper;
import com.centurylink.mdw.util.file.FileHelper;
import com.centurylink.mdw.util.file.ZipHelper;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;
import com.centurylink.mdw.util.timer.LoggerProgressMonitor;
import com.centurylink.mdw.util.timer.ProgressMonitor;

/**
 * Only works for VCS assets.
 */
public class AssetContentServlet extends HttpServlet {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private File assetRoot;

    public void init() throws ServletException {
        assetRoot = ApplicationContext.getAssetRoot();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        if (!assetRoot.isDirectory())
            throw new ServletException(assetRoot + " is not a directory");

        String path = request.getPathInfo().substring(1);
        if ("packages".equals(path)) {
            String packages = request.getParameter("packages");
            if (packages == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: 'packages'");
            }
            else {
                response.setHeader("Content-Disposition", "attachment;filename=\"packages.zip\"");
                response.setContentType("application/octet-stream");
                try {
                    List<File> includes = new ArrayList<File>();
                    for (String pkgName : getPackageNames(packages))
                        includes.add(new File(assetRoot + "/" + pkgName.replace('.', '/')));
                    ZipHelper.writeZipWith(assetRoot, response.getOutputStream(), includes);
                }
                catch (Exception ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            }
        }
        else {
            AssetInfo asset = new AssetInfo(assetRoot, path);
            boolean gitRemote = "true".equalsIgnoreCase(request.getParameter("gitRemote"));

            if (!asset.getFile().isFile() && !gitRemote) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Asset file '" + asset.getFile() + "' not found");
            }
            else {
                if ("true".equalsIgnoreCase(request.getParameter("download"))) {
                    response.setHeader("Content-Disposition", "attachment;filename=\"" + asset.getFile().getName() + "\"");
                    response.setContentType("application/octet-stream");
                }
                else {
                    response.setContentType(asset.getContentType());
                }

                InputStream in = null;
                OutputStream out = response.getOutputStream();
                try {
                    if (gitRemote) {
                        String branch = PropertyManager.getProperty(PropertyNames.MDW_GIT_BRANCH);
                        if (branch == null)
                            throw new PropertyException("Missing required property: " + PropertyNames.MDW_GIT_BRANCH);
                        VersionControlGit vcGit = VersionControlGit.getFrameworkGit();
                        String gitPath = vcGit.getRelativePath(asset.getFile());
                        in = vcGit.getRemoteContentStream(branch, gitPath);
                        if (in == null)
                            throw new IOException("Git remote not found: " + gitPath);
                    }
                    else {
                        if (!asset.getFile().isFile())
                            throw new IOException("Asset file not found: " + asset.getFile());
                        in = new FileInputStream(asset.getFile());
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
    }

    /**
     * Note: Processes are updated in Workflow REST service.
     */
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!assetRoot.isDirectory())
            throw new ServletException(assetRoot + " is not a directory");

        String path = request.getPathInfo().substring(1);
        try {
            LoaderPersisterVcs persisterVcs = (LoaderPersisterVcs) DataAccess.getProcessPersister();

            if ("packages".equals(path)) {
                authorizeForUpdate(request.getSession(), Entity.Package, "Package zip");
                String contentType = request.getContentType();
                boolean isZip = "application/zip".equals(contentType);
                if (!isZip && !"application/json".equals(contentType))
                    throw new ServiceException(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported content: " + contentType);
                File tempDir = new File(ApplicationContext.getTempDirectory());
                String fileExt = isZip ? ".zip" : ".json";
                File tempFile = new File(tempDir + "/packageImport_" + StringHelper.filenameDateToString(new Date()) + fileExt);
                logger.info("Saving package import temporary file: " + tempFile);
                FileHelper.writeToFile(request.getInputStream(), tempFile);
                ProgressMonitor progressMonitor = new LoggerProgressMonitor(logger);
                VersionControl vcs = new VersionControlGit();
                vcs.connect(null, null, null, assetRoot);
                progressMonitor.start("Archive existing assets");
                VcsArchiver archiver = new VcsArchiver(assetRoot, tempDir, vcs, progressMonitor);
                archiver.backup();
                if (isZip) {
                    logger.info("Unzipping " + tempFile + " into: " + assetRoot);
                    FileHelper.unzipFile(tempFile, assetRoot, null, null, true);
                }
                else {
                    logger.info("Importing " + tempFile + " into: " + assetRoot);
                    ImporterExporterJson importer = new ImporterExporterJson();
                    String packageJson = new String(FileHelper.read(tempFile));
                    List<Package> packages = importer.importPackages(packageJson);
                    for (Package pkg : packages) {
                        PackageDir pkgDir = persisterVcs.getTopLevelPackageDir(pkg.getName());
                        if (pkgDir == null) {
                            // new pkg
                            pkgDir = new PackageDir(persisterVcs.getStorageDir(), pkg, persisterVcs.getVersionControl());
                        }
                        persisterVcs.save(pkg, pkgDir, true);
                        pkgDir.parse(); // sync
                    }
                }
                archiver.archive();
                progressMonitor.done();
            }
            else {
                authorizeForUpdate(request.getSession(), Entity.Asset, path);

                int lastSlash = path.lastIndexOf('/');
                if (lastSlash == -1 || lastSlash > path.length() - 2)
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Bad path: " + path);
                String pkgName = path.substring(0, lastSlash);
                Package pkg = persisterVcs.getPackage(pkgName);
                if (pkg == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Package not found: " + pkgName);

                // TODO processes
                String assetName = path.substring(lastSlash + 1);
                Asset asset = persisterVcs.getAsset(pkg.getId(), assetName);
                if (asset == null)
                    throw new ServiceException(ServiceException.NOT_FOUND, "Asset not found: " + pkgName + "/" + assetName);

                String version = request.getParameter("version");
                if (version == null)
                    version = asset.getVersionString();
                logger.info("Saving asset: " + path + " v" + version);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream is = request.getInputStream();
                int read = 0;
                byte[] bytes = new byte[1024];
                while((read = is.read(bytes)) != -1)
                    baos.write(bytes, 0, read);

                asset.setRawContent(baos.toByteArray());
                int ver = asset.getVersion();
                int newVer = Asset.parseVersion(version);
                if (newVer < ver)
                    throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid asset version: v" + version);
                boolean verChange = newVer != ver;
                asset.setVersion(newVer);
                PackageDir pkgDir = persisterVcs.getTopLevelPackageDir(pkgName);
                persisterVcs.save(asset, pkgDir);
                persisterVcs.updateAsset(asset);
                if (verChange) {
                    persisterVcs.persistPackage(pkg, PersistType.NEW_VERSION);
                }
                logger.info("Asset saved: " + path + " v" + version);

                // TODO: distributedSave
                boolean distributed = "true".equalsIgnoreCase(request.getParameter("distributedSave"));
            }
        }
        catch (ServiceException ex) {
            logger.severeException(ex.getMessage(), ex);
            response.sendError(ex.getCode(), ex.getMessage());
        }
        catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private String[] getPackageNames(String packagesParam) {
        Map<String,String> params = new HashMap<String,String>();
        params.put("packages", packagesParam);
        Query query = new Query("", params);
        return query.getArrayFilter("packages");
    }

    /**
     * Also audit logs
     */
    private void authorizeForUpdate(HttpSession session, Entity entity, String includes) throws AuthorizationException, DataAccessException {
        AuthenticatedUser user = (AuthenticatedUser)session.getAttribute("authenticatedUser");
        if (user == null)
            throw new AuthorizationException(AuthorizationException.NOT_AUTHORIZED, "Authentication failure");
        if (!user.hasRole(Role.PROCESS_DESIGN))
            throw new AuthorizationException(AuthorizationException.FORBIDDEN, "User " + user.getCuid() + " not authorized for this action");

        logger.info("Import request received from user: " + user.getCuid() + " for items: " + includes);
        UserAction userAction = new UserAction(user.getCuid(), Action.Import, entity, 0L, includes);
        userAction.setSource("Asset Import Service");
        ServiceLocator.getUserServices().auditLog(userAction);
    }
}
