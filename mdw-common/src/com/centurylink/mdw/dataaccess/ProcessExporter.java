/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess;

import java.util.List;

import org.apache.xmlbeans.XmlException;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;

public interface ProcessExporter {

    public String exportPackages(List<PackageVO> packages, boolean includeTaskTemplates)
    throws DataAccessException, XmlException;

    public String exportPackage(PackageVO packageVO, boolean includeTaskTemplates)
    throws DataAccessException, XmlException;

    public String exportProcess(ProcessVO processVO, int schemaVersion, List<ExternalEventVO> externalEvents)
    throws DataAccessException, XmlException;

    public String exportOverrideAttributes(String prefix, PackageVO packageVO)
    throws DataAccessException, XmlException;

    public String exportOverrideAttributes(String prefix, ProcessVO processVO, int schemaVersion)
    throws DataAccessException, XmlException;
}
