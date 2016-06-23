/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.util.List;

import com.centurylink.mdw.bpm.MDWProcess;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;

public interface ProcessImporter {

    public List<PackageVO> importPackages(String packagesXml)
    throws DataAccessException;

    public PackageVO importPackage(String packageXml)
    throws DataAccessException;

    public ProcessVO importProcess(String xmlstring)
    throws DataAccessException;

    public ProcessVO importProcess(MDWProcess pProcess);

}
