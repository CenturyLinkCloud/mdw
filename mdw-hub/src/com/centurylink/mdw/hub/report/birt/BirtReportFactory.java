/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.report.birt;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.faces.model.SelectItem;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.IGetParameterDefinitionTask;
import org.eclipse.birt.report.engine.api.IParameterDefn;
import org.eclipse.birt.report.engine.api.IParameterSelectionChoice;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportEngineFactory;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IScalarParameterDefn;

import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.web.ui.UIException;

public class BirtReportFactory {

    public static BirtReport loadReport(String reportName, Map<String,String> paramValues) throws UIException {
      return loadReport(reportName, paramValues, 0);
    }

    public static BirtReport loadReport(String reportName, Map<String,String> paramValues, int version) throws UIException {
        RuleSetVO ruleSet = getRuleSet(reportName, version);
        if (ruleSet == null)
            throw new UIException("Unable to load Report : " + reportName);

        InputStream inStream = new ByteArrayInputStream(ruleSet.getRuleSet().getBytes());
        try {
            IReportEngine reportEngine = getReportEngine();
            IReportRunnable reportDesign = reportEngine.openReportDesign(inStream);

            BirtReport birtReport = new BirtReport(reportName);
            birtReport.setReportDesign(reportDesign);

            birtReport.setTitle((String) reportDesign.getProperty(IReportRunnable.TITLE));

            // create the filter
            IGetParameterDefinitionTask task = reportEngine.createGetParameterDefinitionTask(reportDesign);
            Collection<?> params = task.getParameterDefns(false);
            BirtFilter birtFilter = null;
            if (params.size() != 0) {
                List<IParameterDefn> parameterDefs = new ArrayList<IParameterDefn>();
                Map<String, List<SelectItem>> selectItemsMap = null;
                for (Iterator<?> iter = params.iterator(); iter.hasNext();) {
                    IParameterDefn parameterDef = (IParameterDefn) iter.next();
                    if (parameterDef instanceof IScalarParameterDefn) {
                        IScalarParameterDefn scalar = (IScalarParameterDefn) parameterDef;
                        if (scalar.getControlType() == IScalarParameterDefn.LIST_BOX) {
                            // populate the selection list
                            Collection<?> selectionList = task.getSelectionList(parameterDef.getName());
                            // Selection contains data
                            if (selectionList != null) {
                                if (selectItemsMap == null)
                                    selectItemsMap = new HashMap<String, List<SelectItem>>();
                                List<SelectItem> selectItems = new ArrayList<SelectItem>();
                                for (Iterator<?> slIter = selectionList.iterator(); slIter.hasNext();) {
                                    IParameterSelectionChoice selectionItem = (IParameterSelectionChoice) slIter.next();
                                    selectItems.add(new SelectItem(selectionItem.getValue(), selectionItem.getLabel()));
                                }
                                selectItemsMap.put(parameterDef.getName(), selectItems);
                            }
                        }
                        parameterDefs.add(parameterDef);
                    }
                }

                String filterId = FileHelper.stripDisallowedFilenameChars(reportName) + "_Filter";
                birtFilter = new BirtFilter(filterId, parameterDefs, selectItemsMap, paramValues);
            }
            birtReport.setFilter(birtFilter);

            return birtReport;
        }
        catch (BirtException ex) {
            throw new UIException(ex.getMessage(), ex);
        }
    }

    private static IReportEngine _reportEngine;
    public static synchronized IReportEngine getReportEngine() throws BirtException {
        if (_reportEngine == null) {
            EngineConfig config = new EngineConfig();
            // config.getAppContext().put(EngineConstants.APPCONTEXT_CLASSLOADER_KEY, Thread.currentThread().getContextClassLoader());
            config.setLogConfig(null, Level.FINE);
            Platform.startup(config);
            IReportEngineFactory factory = (IReportEngineFactory) Platform.createFactoryObject(IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY);
            _reportEngine = factory.createReportEngine(config);
            // _reportEngine.changeLogLevel(Level.WARNING);
        }
        return _reportEngine;
    }

    public static RuleSetVO getRuleSet(String reportName, int version) {
        return RuleSetCache.getRuleSet(reportName, RuleSetVO.BIRT, version);
    }

  }
