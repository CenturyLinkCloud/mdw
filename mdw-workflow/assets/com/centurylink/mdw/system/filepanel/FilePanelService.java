/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.system.filepanel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.system.Dir;
import com.centurylink.mdw.model.system.FileInfo;
import com.centurylink.mdw.services.rest.JsonRestService;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

@Path("/")
public class FilePanelService extends JsonRestService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    @Override
    @Path("/{filePath}")
    public JSONObject get(String path, Map<String,String> headers)
            throws ServiceException, JSONException {

        String[] segments = getSegments(path);
        if (segments.length == 5) {
            // root dirs
            List<Dir> dirs = getFilePanelDirs();
            JSONObject json = new JSONObject();
            JSONArray dirsArr = new JSONArray();
            for (Dir dir : dirs)
                dirsArr.put(dir.getJson());
            json.put("dirs", dirsArr);
            return json;
        }
        else if (segments.length == 6) {
            // file view
            File file = new File(segments[5]);
            if (!file.isFile())
                throw new ServiceException(ServiceException.NOT_FOUND, "Not found: " + segments[5]);
            try {
                Query query = new Query(path, headers);
                if (query.getBooleanFilter("download")) {
                    // TODO file download
                }
                else {
                    FileInfo fileInfo = new FileInfo(file);
                    FileView fileView = new FileView(fileInfo, query);

                }
                return new JSONObject();
            }
            catch (IOException ex) {
                throw new ServiceException(ServiceException.INTERNAL_ERROR, ex.getMessage());
            }
        }
        else {
            throw new ServiceException(ServiceException.BAD_REQUEST, "Invalid path: " + path);
        }
    }

    private List<Dir> getFilePanelDirs() throws ServiceException {
        String rootDirs = PropertyManager.getProperty(PropertyNames.FILEPANEL_ROOT_DIRS);
        if (rootDirs == null)
            throw new ServiceException(ServiceException.INTERNAL_ERROR, "Missing property: " + PropertyNames.FILEPANEL_ROOT_DIRS);
        String[] excludes = null;
        String excludePatterns = PropertyManager.getProperty(PropertyNames.FILEPANEL_EXCLUDE_PATTERNS);
        if (excludePatterns != null) {
            excludes = excludePatterns.trim().split("\\s*,\\s*");
        }

        List<Dir> dirs = new ArrayList<>();
        for (String rootDir : rootDirs.trim().split("\\s*,\\s*")) {
            File file = new File(rootDir);
            if (file.isDirectory()) {
                try {
                    dirs.add(new Dir(file, excludes));
                }
                catch (IOException ex) {
                    logger.severeException(ex.getMessage(), ex);
                }
            }
        }
        return dirs;
    }

}
