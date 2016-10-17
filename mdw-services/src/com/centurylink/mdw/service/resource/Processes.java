/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.util.Arrays;
import java.util.Map;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessExporter;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;

public class Processes implements XmlService {

    public String getXml(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        try {
            String id = (String)parameters.get("id");
            String name = (String)parameters.get("name");
            if (id != null || name != null) {
                // return full instance
                ProcessLoader loader = DataAccess.getProcessLoader();
                ProcessVO process = null;
                if (id != null) {
                    process = loader.loadProcess(Long.parseLong(id), false);
                    if (process == null)
                        throw new ServiceException(404, "Process not found for parameters: " + id);
                }
                else if (name != null) {
                    String version = (String)parameters.get("version");
                    int ver = version == null ? 0 : RuleSetVO.parseVersion(version);
                    process = loader.getProcessBase(name, ver);
                    if (process == null)
                        throw new ServiceException(404, "Process not found: " + name + (version == null ? "" : " v" + version));
                    process = loader.loadProcess(process.getId(), false);
                }
                ProcessExporter exporter = DataAccess.getProcessExporter(DataAccess.currentSchemaVersion);
                PackageVO pkg = new PackageVO();
                pkg.setName(process.getPackageName());
                pkg.setVersion(PackageVO.parseVersion(process.getPackageVersion()));
                pkg.setProcesses(Arrays.asList(new ProcessVO[]{process}));
                return exporter.exportPackage(pkg, false);
            }
            else {
                // client probably meant to use new process REST services
                return "{ \"status\": { \"code\": 415, \"message\": \"HTTP 'Accept' header should be 'application/json'\" } }";
            }
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        catch (XmlException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

    public String getText(Map<String,Object> parameters, Map<String,String> metaInfo) throws ServiceException {
        return getXml(parameters, metaInfo);
    }

}
