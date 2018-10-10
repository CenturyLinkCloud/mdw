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
package com.centurylink.mdw.util;

import java.io.File;
import java.util.Date;

import com.centurylink.mdw.dataaccess.AssetRef;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.file.LoaderPersisterVcs;
import com.centurylink.mdw.dataaccess.file.VersionControlGit;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

public class AssetRefConverter {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    public static Process getProcess(AssetRef assetRef) throws Exception {
        Process proc = null;
        byte[] contentBytes = readAsset(assetRef);
        if (contentBytes != null && contentBytes.length > 0) {
            proc = new Process(new JsonObject(new String(contentBytes)));
            proc.setId(assetRef.getDefinitionId());
            int startIdx = assetRef.getName().lastIndexOf('/') + 1;
            int endIdx = assetRef.getName().lastIndexOf(".proc v");
            proc.setName(assetRef.getName().substring(startIdx, endIdx));
            proc.setLanguage(Asset.PROCESS);
            proc.setVersion(Asset.parseVersion(assetRef.getName().substring(endIdx+6)));
            proc.setPackageName(assetRef.getName().substring(0, startIdx-1));
        }
        return proc;
    }

    public static Asset getAsset(AssetRef assetRef) throws Exception {
        Asset asset = null;
        byte[] contentBytes = readAsset(assetRef);

        if (contentBytes != null && contentBytes.length > 0) {
            asset = new Asset();
            asset.setId(assetRef.getDefinitionId());
            int startIdx = assetRef.getName().lastIndexOf('/') + 1;
            int endIdx = assetRef.getName().lastIndexOf(" v");
            asset.setName(assetRef.getName().substring(startIdx, endIdx));
            asset.setLanguage(Asset.getFormat(asset.getName()));
            asset.setVersion(Asset.parseVersion(assetRef.getName().substring(endIdx+1)));
            asset.setPackageName(assetRef.getName().substring(0, startIdx-1));
            asset.setLoadDate(new Date());
            asset.setRaw(true);
            // do not load jar assets into memory
            if (!Asset.excludedFromMemoryCache(asset.getName()))
                asset.setRawContent(contentBytes);
        }
        return asset;
    }

    public static TaskTemplate getTaskTemplate(AssetRef assetRef) throws Exception {
        TaskTemplate taskVO = null;
        byte[] contentBytes = readAsset(assetRef);

        if (contentBytes != null && contentBytes.length > 0) {
            taskVO = new TaskTemplate(new JsonObject(new String(contentBytes)));
            taskVO.setTaskId(assetRef.getDefinitionId());
            int startIdx = assetRef.getName().lastIndexOf('/') + 1;
            int endIdx = assetRef.getName().lastIndexOf(" v");
            taskVO.setName(assetRef.getName().substring(startIdx, endIdx));
            taskVO.setVersion(Asset.parseVersion(assetRef.getName().substring(endIdx+1)));
            taskVO.setPackageName(assetRef.getName().substring(0, startIdx-1));
        }
        return taskVO;
    }

    private static byte[] readAsset(AssetRef assetRef) {
        byte[] contentBytes = null;
        if (assetRef != null) {
            try {
            LoaderPersisterVcs lp = (LoaderPersisterVcs)DataAccess.getProcessLoader();
            VersionControlGit vc = (VersionControlGit)lp.getVersionControl();
            String tempName = assetRef.getName().substring(0, assetRef.getName().lastIndexOf(" v"));
            int fileExtIdx = tempName.lastIndexOf(".");
            tempName = tempName.substring(0, fileExtIdx).replace('.', '/');
            String path = getMissingPath(lp.getStorageDir(), "") + tempName + assetRef.getName().substring(0, assetRef.getName().lastIndexOf(" v")).substring(fileExtIdx);
            contentBytes = vc.readFromCommit(assetRef.getRef(), path);
            }
            catch (Throwable ex) {
                logger.severeException("Exception trying to read asset from Git: " + assetRef.getName(), ex);
            }
        }
        return contentBytes;
    }

    private static String getMissingPath(File assetLoc, String path) {
        File test = new File(assetLoc.getPath() + "/.git");
        if (test.isDirectory())
            return path;

        return getMissingPath(new File(assetLoc.getParent()), assetLoc.getName() + "/" + path);

    }
}
