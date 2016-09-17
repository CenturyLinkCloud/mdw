/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.version4;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.bpm.MDWActivity;
import com.centurylink.mdw.bpm.MDWActivityImplementor;
import com.centurylink.mdw.bpm.MDWAttribute;
import com.centurylink.mdw.bpm.MDWExternalEvent;
import com.centurylink.mdw.bpm.MDWPackage;
import com.centurylink.mdw.bpm.MDWProcess;
import com.centurylink.mdw.bpm.MDWProcessDefinition;
import com.centurylink.mdw.bpm.MDWRuleSet;
import com.centurylink.mdw.bpm.MDWTextNote;
import com.centurylink.mdw.bpm.MDWTransition;
import com.centurylink.mdw.bpm.MDWTransitionEvent;
import com.centurylink.mdw.bpm.MDWVariable;
import com.centurylink.mdw.bpm.PackageDocument;
import com.centurylink.mdw.bpm.ProcessDefinitionDocument;
import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.SchemaTypeTranslator;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.ProcessExporter;
import com.centurylink.mdw.dataaccess.ProcessImporter;
import com.centurylink.mdw.model.data.event.EventType;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.activity.TextNoteVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.attribute.CustomAttributeVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.ExternalEventVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.variable.VariableVO;
import com.centurylink.mdw.model.value.work.WorkTransitionVO;

public class ProcessImporterExporterV4 implements ProcessImporter,ProcessExporter {

    private Map<String,Long> idmap;
    private int nextId;
    private int activityIdCounter = 1;
    private int processIdCounter = 1;
    private int transitionIdCounter = 1;
    private Map<Long,String> workNameRef = new HashMap<Long,String>();
    private Map<String,String> activityImplRef = new HashMap<String,String>();
    private Map<Long,String> transIdRef = new HashMap<Long,String>();

    protected SchemaTypeTranslator schemaTypeTranslator;  // used by exporter

    public ProcessImporterExporterV4() {
    }

    public ProcessImporterExporterV4(SchemaTypeTranslator schemaTypeTranslator) {
        this.schemaTypeTranslator = schemaTypeTranslator;
    }


    /////////////////////// importer portion /////////////////////////////////////////

    public List<PackageVO> importPackages(String packagesXml) throws DataAccessException {
        try {
            PackageDocument pkgDoc = PackageDocument.Factory.parse(packagesXml, Compatibility.namespaceOptions());
            MDWPackage pkgsDef = pkgDoc.getPackage();
            List<PackageVO> imported = new ArrayList<PackageVO>();
            for (MDWProcessDefinition pkgDef : pkgsDef.getProcessDefinitionList()) {
              PackageVO pkg = importPackage0(pkgDef);
              imported.add(pkg);
            }
            return imported;
        } catch (XmlException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    /**
         * Gets the work instances for a particular work id This method shall be used by the BPM
         * Designer
         * @param pWorkId
         * @return Collection of Work Instances
         * @throws DataAccessException
         *
         */
    public PackageVO importPackage(String xmlstring) throws DataAccessException {
        try {
            ProcessDefinitionDocument defDoc = ProcessDefinitionDocument.Factory.parse(xmlstring, Compatibility.namespaceOptions());
            MDWProcessDefinition def = defDoc.getProcessDefinition();
            // pkg configuration
            PackageVO pkg = importPackage0(def);
            return pkg;
        } catch (XmlException e) {
            throw new DataAccessException(0, "Failed to import process", e);
        }
    }

    protected PackageVO importPackage0(MDWProcessDefinition def) {
        idmap = new HashMap<String,Long>();
        nextId = 1;
        PackageVO packageVO = new PackageVO();
        if (def.getPackageName()!=null)
            packageVO.setPackageName(def.getPackageName());
        else packageVO.setPackageName("package");
        packageVO.setPackageId(this.mapId("package", packageVO.getPackageName()));
        packageVO.setSchemaVersion(RuleSetVO.parseVersion(def.getSchemaVersion()));
        packageVO.setVersion(PackageVO.parseVersion(def.getPackageVersion()));
        packageVO.setGroup(def.getPackageWorkgroup());
        // right now, package ID is only used to determine if
        // importing is successful by the designer

        // create the activity impls
        packageVO.setImplementors(new ArrayList<ActivityImplementorVO>());
        List<MDWActivityImplementor> actImplArr = def.getImplementorList();
        if (actImplArr!=null) {
            for (MDWActivityImplementor ai : actImplArr) {
                ActivityImplementorVO vo = new ActivityImplementorVO();
                packageVO.getImplementors().add(vo);
                vo.setImplementorClassName(ai.getImplementation());
                vo.setImplementorType(new Integer(1));
                vo.setBaseClassName(ai.getType());
                vo.setIconName(ai.getIconFile());
                vo.setLabel(ai.getLabel());
                if (ai.getHidden())
                    vo.setHidden(true);
                vo.setAttributeDescription(ai.getAttributeDescription());
                vo.setVariables(importVariables(ai.getVariablesList()));
            }
        }
        // create external event handlers
        packageVO.setExternalEvents(new ArrayList<ExternalEventVO>());
        List<MDWExternalEvent> extEventList = def.getExternalEventHandlerList();
        if (extEventList!=null) {
            for (MDWExternalEvent ee: extEventList) {
                ExternalEventVO eevo = new ExternalEventVO();
                eevo.setEventName(ee.getEventName());
                eevo.setEventHandler(ee.getEventHandler());
                packageVO.getExternalEvents().add(eevo);
            }
        }
        // create rule sets
        if (def.getRuleSetList() != null)
            importRuleSets(packageVO, def);
        // create package attributes
        if (def.getAttributeList() != null)
            importPackageAttributes(packageVO, def);
        // create custom attributes
        if (def.getCustomAttributeList() != null)
            importCustomAttributes(packageVO, def);
        // create task templates
        if (def.getTaskTemplateList() != null)
            importTaskTemplates(packageVO, def);
        // create processes
        packageVO.setProcesses(new ArrayList<ProcessVO>());
        importConfiguration(packageVO, def);
        for (MDWProcess proc : def.getProcessList()) {
            ProcessVO processVO = importProcessBase(proc);
            if (processVO.isInRuleSet())
                importProcessUsingLogicalId(proc, processVO, false);
            else
                importProcessUsingMap(proc, processVO, proc.getName());
            packageVO.getProcesses().add(processVO);
        }

        return packageVO;

    }

    /**
     * Used to load process from file/ruleset. Set IDs using logical IDs
     */
    public ProcessVO importProcess(String xmlstring) throws DataAccessException {
    	try {
            ProcessDefinitionDocument procdef = ProcessDefinitionDocument.Factory.parse(xmlstring, Compatibility.namespaceOptions());
            MDWProcessDefinition processDefn = procdef.getProcessDefinition();
            MDWProcess pProcess = processDefn.getProcessArray(0);
            ProcessVO processVO = importProcessBase(pProcess);
            importProcessUsingLogicalId(pProcess, processVO, false);
            return processVO;
        } catch (XmlException e) {
            throw new DataAccessException(0, "Failed to import process", e);
        }
    }

    private Long getIdFromLogicalId(String lid) {
        if(StringHelper.isEmpty(lid)) return null;
    	if (lid.startsWith("MainProcess")) return 0L;
    	else if (lid.startsWith("SubProcess")) return new Long(lid.substring(10));
    	else if (lid.startsWith("Activity")) return new Long(lid.substring(8));
    	else if (lid.startsWith("Transition")) return new Long(lid.substring(10));
    	else return new Long(lid.substring(1));
    }

    protected ProcessVO importProcessBase(MDWProcess pProcess) {
    	ProcessVO processVO = new ProcessVO();
	    processVO.setProcessName(pProcess.getName());
	    processVO.setVersion(ProcessVO.parseVersion(pProcess.getVersion()));
	    processVO.setProcessDescription(pProcess.getDescription());
	    processVO.setAttributes(importAttributes(pProcess.getAttributeList()));
        processVO.setVariables(importVariables(pProcess.getVariablesList()));
        String av = processVO.getAttribute("IsInRuleSet");
        processVO.setAttribute("IsInRuleSet", null);
        processVO.setInRuleSet("true".equalsIgnoreCase(av)
        		|| DataAccess.supportedSchemaVersion>=DataAccess.schemaVersion52);
	    return processVO;
    }

	protected void importProcessUsingLogicalId(MDWProcess pProcess, ProcessVO processVO, boolean isEmbeddedProcess) {
	    if (isEmbeddedProcess) processVO.setProcessId(getIdFromLogicalId(pProcess.getId()));
	    else {
	    	for (VariableVO var : processVO.getVariables()) {
	    		var.setVariableId(0L);
	    	}
	    	processVO.setProcessId(0L);	// will be overridden by persister/loaders
	    }

	    // create subprocesses VO
	    processVO.setSubProcesses(new ArrayList<ProcessVO>());
	    if (pProcess.getSubProcessList()!=null) {
	        for (MDWProcess p : pProcess.getSubProcessList()) {
	            ProcessVO pvo = importProcessBase(p);
	            importProcessUsingLogicalId(p, pvo, true);
	            pvo.setAttribute(WorkAttributeConstant.LOGICAL_ID, p.getId());
	            processVO.getSubProcesses().add(pvo);
	        }
	    }

	    // create activities
	    processVO.setActivities(new ArrayList<ActivityVO>());
	    if (pProcess.getActivityList()!=null) {
	        for (MDWActivity a : pProcess.getActivityList()) {
	            ActivityVO vo = new ActivityVO();
	            processVO.getActivities().add(vo);
	            vo.setActivityId(getIdFromLogicalId(a.getId()));
	            vo.setActivityName(a.getName());
	            vo.setImplementorClassName(a.getImplementation());
	//            vo.setActivityType(???);  this is the only time activityType is not set correctly
	            vo.setAttributes(importAttributes(a.getAttributeList()));
	            vo.setAttribute(WorkAttributeConstant.LOGICAL_ID, a.getId());
	        }
	    }

	    // create the transitions
	    processVO.setTransitions(new ArrayList<WorkTransitionVO>());
	    if (pProcess.getTransitionList()!=null) {
	        for (MDWTransition t : pProcess.getTransitionList()) {
	            WorkTransitionVO vo = new WorkTransitionVO();
	            processVO.getTransitions().add(vo);
	            if(getIdFromLogicalId(t.getFrom())!= null)
	                vo.setFromWorkId(getIdFromLogicalId(t.getFrom()));
	            if(getIdFromLogicalId(t.getTo()) != null)
	                vo.setToWorkId(getIdFromLogicalId(t.getTo()));
	            if(t.getEvent() != null)
	                vo.setEventType(EventType.getEventTypeFromName(t.getEvent().toString()));
	            vo.setCompletionCode(t.getCompletionCode());
	            if ("".equals(vo.getCompletionCode())) vo.setCompletionCode(null);
	            vo.setWorkTransitionId(getIdFromLogicalId(t.getId()));
	            vo.setAttributes(importAttributes(t.getAttributeList()));
	            vo.setAttribute(WorkAttributeConstant.LOGICAL_ID, t.getId());
	        }
	    }

	    // create the text notes
	    if (pProcess.getTextNoteList()!=null) {
	    	List<TextNoteVO> volist = new ArrayList<TextNoteVO>();
	    	processVO.setTextNotes(volist);
	    	for (MDWTextNote textnote : pProcess.getTextNoteList()) {
	    		TextNoteVO vo = new TextNoteVO();
	    		vo.setContent(textnote.getContent());
	    		vo.setReference(textnote.getReference());
	    		vo.setAttributes(importAttributes(textnote.getAttributeList()));
	    		volist.add(vo);
	    	}
	    }

	}


    /**
     * import process as part of a package. Uses a map to set activity/transition IDs
     * with generated values, not using logical IDs
     */
    private void importProcessUsingMap(MDWProcess pProcess, ProcessVO processVO, String mainProcName) {
        processVO.setProcessId(mapId(mainProcName,pProcess.getId()));

        // create subprocesses VO
        processVO.setSubProcesses(new ArrayList<ProcessVO>());
        if (pProcess.getSubProcessList()!=null) {
            for (MDWProcess p : pProcess.getSubProcessList()) {
                ProcessVO pvo = importProcessBase(p);
                importProcessUsingMap(p, pvo, mainProcName);
                pvo.setAttribute(WorkAttributeConstant.LOGICAL_ID, p.getId());
                processVO.getSubProcesses().add(pvo);
            }
        }

        // create activities and synchronizations
        processVO.setActivities(new ArrayList<ActivityVO>());
        if (pProcess.getActivityList()!=null) {
            for (MDWActivity a : pProcess.getActivityList()) {
                ActivityVO vo = new ActivityVO();
                processVO.getActivities().add(vo);
                vo.setActivityId(mapId(mainProcName,a.getId()));
                vo.setActivityName(a.getName());
                vo.setImplementorClassName(a.getImplementation());
//                vo.setActivityType(???);  this is the only time activityType is not set correctly
//                if (a.isSetSla()) vo.setSla(a.getSla());
                vo.setAttributes(importAttributes(a.getAttributeList()));
                vo.setAttribute(WorkAttributeConstant.LOGICAL_ID, a.getId());
                List<String> syncs = a.getSynchronizedOnWorkList();
                if (syncs!=null) {
                    Long[] s = new Long[syncs.size()];
                    vo.setSynchronzingIds(s);
                    for (int i=0; i<s.length; i++) {
                        s[i] = mapId(mainProcName,syncs.get(i));
                    }
                }
            }
        }

        // create the transitions and transaction dependencies
        processVO.setTransitions(new ArrayList<WorkTransitionVO>());
        if (pProcess.getTransitionList()!=null) {
            for (MDWTransition t : pProcess.getTransitionList()) {
                WorkTransitionVO vo = new WorkTransitionVO();
                processVO.getTransitions().add(vo);
                vo.setFromWorkId(mapId(mainProcName,t.getFrom()));
                vo.setToWorkId(mapId(mainProcName,t.getTo()));
                vo.setEventType(EventType.getEventTypeFromName(t.getEvent().toString()));
                vo.setCompletionCode(t.getCompletionCode());
                vo.setWorkTransitionId(mapId(mainProcName,t.getId()));
                vo.setAttributes(importAttributes(t.getAttributeList()));
                vo.setAttribute(WorkAttributeConstant.LOGICAL_ID, t.getId());
                if (t.getValidation()!=null)
                    vo.setValidatorClassName(t.getValidation().getValidationClass());
                // now transition dependencies
                List<String> depends = t.getDependsOnList();
                if (depends!=null) {
                    vo.setDependentTransitionIds(new ArrayList<Long>());
                    for (String d : depends) {
                        vo.getDependentTransitionIds().add(mapId(mainProcName,d));
                    }
                }
            }
        }

        // check the external event map and update that one as well
        if (pProcess.getExternalEventList()!=null) {
            processVO.setExternalEvents(new ArrayList<ExternalEventVO>());
            for (MDWExternalEvent e : pProcess.getExternalEventList()) {
                processVO.getExternalEvents().add(
                        new ExternalEventVO(null, e.getEventName(),e.getEventHandler()));
            }
        } else processVO.setExternalEvents(null);

    }

    private Long mapId(String procname, String nameId) {
    	String qname = procname + ":" + nameId;
        Long id = idmap.get(qname);
        if (id==null) {
            id = new Long(nextId++);
            idmap.put(qname, id);
        }
        return id;
    }

    protected List<AttributeVO> importAttributes(List<MDWAttribute> pattrs) {
        List<AttributeVO> attrs = new ArrayList<AttributeVO>();
        if (pattrs!=null) {
            for (MDWAttribute pattr: pattrs) {
                String attrname = pattr.getName();
                attrs.add(new AttributeVO(null, attrname, pattr.getValue()));
            }
        }
        return attrs;
    }

    private List<VariableVO> importVariables(List<MDWVariable> mdwvars) {
        List<VariableVO> vars = new ArrayList<VariableVO>();
        if (mdwvars==null) return vars;
        for (MDWVariable v : mdwvars) {
            VariableVO vo = new VariableVO();
            vars.add(vo);
            vo.setVariableName(v.getName());
            vo.setVariableType(v.getType().getName());
            MDWVariable.Category.Enum cat = v.getCategory();
            if (cat!=null) {
            	if (cat == MDWVariable.Category.LOCAL)
            		vo.setVariableCategory(VariableVO.CAT_LOCAL);
            	else if (cat == MDWVariable.Category.INPUT)
            		vo.setVariableCategory(VariableVO.CAT_INPUT);
            	else if (cat == MDWVariable.Category.OUTPUT)
            		vo.setVariableCategory(VariableVO.CAT_OUTPUT);
            	else if (cat == MDWVariable.Category.INOUT)
            		vo.setVariableCategory(VariableVO.CAT_INOUT);
            	else if (cat == MDWVariable.Category.STATIC)
            		vo.setVariableCategory(VariableVO.CAT_STATIC);
            }
            else vo.setVariableCategory(VariableVO.CAT_LOCAL);
            vo.setDescription(v.getDescription());
            vo.setVariableReferredAs(v.getReferredAs());
            vo.setDisplaySequence(v.getDisplaySequence() == null ? 0 : v.getDisplaySequence().intValue());
        }
        return vars;
    }

    ///////////////////////// Exporter portion ///////////////////////////

    public String exportPackages(List<PackageVO> packages, boolean includeTaskTemplates) throws DataAccessException, XmlException {
        throw new UnsupportedOperationException();
    }

    public String exportPackage(PackageVO packageVO, boolean includeTaskTemplates) throws DataAccessException, XmlException {
        preparePackageForExport(packageVO);
        ProcessDefinitionDocument defnDoc = ProcessDefinitionDocument.Factory.newInstance();
        MDWProcessDefinition procDefn = defnDoc.addNewProcessDefinition();
        mapPackage(procDefn, packageVO, includeTaskTemplates);
        return processDefDocToString(defnDoc);
    }

    protected void preparePackageForExport(PackageVO packageVO) throws DataAccessException {
        // in the exporter processVO order is significant
        // -- subprocesses must come after their containing parent processes
        Collections.sort(packageVO.getProcesses(), new Comparator<ProcessVO>() {
            public int compare(ProcessVO pVO1, ProcessVO pVO2) {
                boolean pVO1_hasSubProcs = pVO1.getSubProcesses() != null && pVO1.getSubProcesses().size() > 0;
                boolean pVO2_hasSubProcs = pVO2.getSubProcesses() != null && pVO2.getSubProcesses().size() > 0;
                if (pVO1_hasSubProcs == pVO2_hasSubProcs) {
                    // sort by label
                    return pVO1.getLabel().compareToIgnoreCase(pVO2.getLabel());
                }
                else if (pVO1_hasSubProcs)
                    return -1;
                else
                    return 1;
            }
        });

        // sort implementors
        if (packageVO.getImplementors() != null) {
            Collections.sort(packageVO.getImplementors(), new Comparator<ActivityImplementorVO>() {
                public int compare(ActivityImplementorVO a1, ActivityImplementorVO a2) {
                    return a1.getImplementorClassNameWithoutPath().compareTo(a2.getImplementorClassNameWithoutPath());
                }
            });
        }

        // sort externalEvents and ruleSets
        if (packageVO.getExternalEvents() != null)
            Collections.sort(packageVO.getExternalEvents());
        if (packageVO.getRuleSets() != null)
            Collections.sort(packageVO.getRuleSets());

        if (packageVO.getRuleSets() != null) {
        for (RuleSetVO ruleSet : packageVO.getRuleSets())
            if (ruleSet.isRaw() && ruleSet.isBinary()) {
                if (RuleSetVO.excludedFromMemoryCache(ruleSet.getName())) {
                    try {
                        ruleSet.setRawContent(FileHelper.read(ruleSet.getRawFile()));
                    }
                    catch (IOException ex) {
                        throw new DataAccessException(ex.getMessage(), ex);
                    }
                }
                ruleSet.setRuleSet(RuleSetVO.encode(ruleSet.getContent()));
            }
        }
    }

    protected void mapPackage(MDWProcessDefinition procDefn, PackageVO packageVO, boolean includeTaskTemplates) throws DataAccessException {

        List<MDWActivityImplementor> actArrList = mapActivityImplementors(packageVO.getImplementors());
        if (packageVO.getExternalEvents() != null)
            mapAllExternalEvents(procDefn, packageVO.getExternalEvents());
        if (packageVO.getRuleSets() != null)
            exportRuleSets(procDefn, packageVO.getRuleSets());
        if (includeTaskTemplates && packageVO.getTaskTemplates() != null)
            exportTaskTemplates(procDefn, packageVO.getTaskTemplates());
        if (packageVO.getAttributes() != null)
            exportPackageAttributes(procDefn, packageVO.getAttributes());
        if (packageVO.getCustomAttributes() != null)
            exportCustomAttributes(procDefn, packageVO.getCustomAttributes());
        exportConfiguration(procDefn, packageVO);
        int mainProcessCounter = 1;
        for (ProcessVO processVO : packageVO.getProcesses()) {
            MDWProcess process = procDefn.addNewProcess();
            String exportedId = "MainProcess"+(mainProcessCounter++);
            if (!processVO.isInRuleSet())
                workNameRef.put(processVO.getProcessId(), exportedId);
            mapProcess(process, processVO, exportedId);
            MDWAttribute attr = process.addNewAttribute();
            attr.setName("IsInRuleSet");
            attr.setValue(processVO.isInRuleSet()?"true":"false");
            // this.mapActivityImplementors(processVO, actArrList);
        }
        procDefn.setImplementorArray(actArrList.toArray(new MDWActivityImplementor[] {}));
        int schemaVersion = packageVO.getSchemaVersion();
        procDefn.setSchemaVersion(createVersionString(schemaVersion));
        procDefn.setPackageVersion(createPackageVersionString(packageVO.getVersion()));
        procDefn.setPackageName(packageVO.getPackageName());
        if (packageVO.getGroup() != null)
          procDefn.setPackageWorkgroup(packageVO.getGroup());
    }

    public String exportOverrideAttributes(String prefix, PackageVO packageVO) throws DataAccessException, XmlException {
        return null; // not applicable for R4
    }

    /**
     * Export a single process by generating the XML for the process.
     * When externalEvents is not null (can be empty), the generated XML
     * contains a package wrapper (no package name, package version is 0.0,
     * the list of external events passed in, and automatically includes implementors)
     * When externalEvents is null, generates only the process itself without a
     * package wrapper.
     *
     * @param pProcessVO the process to be exported.
     * @param externalEvents a list of external events to be exported along with the process.
     * 		null when package wrapper is not to be generated.
     * @return the XML string
     * @throws DataAccessException
     *
     */
    public String exportProcess(ProcessVO pProcessVO, int schemaVersion, List<ExternalEventVO> externalEvents)
            throws DataAccessException, XmlException {
        ProcessDefinitionDocument defnDoc = ProcessDefinitionDocument.Factory.newInstance();
        MDWProcessDefinition procDefn = defnDoc.addNewProcessDefinition();
        if (externalEvents!=null) {
        	List<MDWActivityImplementor> actArrList = new ArrayList<MDWActivityImplementor>();
        	this.mapActivityImplementors(pProcessVO, actArrList);
        	procDefn.setImplementorArray((MDWActivityImplementor[]) actArrList
        			.toArray(new MDWActivityImplementor[] {}));
        	this.mapAllExternalEvents(procDefn, externalEvents);
        }
        MDWProcess process = procDefn.addNewProcess();
        String exportedId = "MainProcess";
        if (!pProcessVO.isInRuleSet())
        	workNameRef.put(pProcessVO.getProcessId(), exportedId);
        mapProcess(process, pProcessVO, exportedId);
        procDefn.setSchemaVersion(createVersionString(schemaVersion));
        // procDefn.setPackageVersion("0.0");
        return processDefDocToString(defnDoc);
    }

    /**
         * Gets the work instances for a particular work id This method shall be used by the BPM
         * Designer
         * @param pWorkId
         * @return Collection of Work Instances
         * @throws DataAccessException
         *
         */
    private void mapProcess(MDWProcess process, ProcessVO pProcessVO, String pProcExpId)
        throws DataAccessException {
        process.setId(pProcExpId);
        process.setName(pProcessVO.getProcessName());
        process.setVersion(createVersionString(pProcessVO.getVersion()));
        process.setDescription(pProcessVO.getProcessDescription());
        MDWActivity[] acts = this.mapActivities(pProcessVO.getActivities());
        if (acts != null)
            process.setActivityArray(acts);

        MDWVariable[] vars = this.mapVariables(pProcessVO.getVariables());
        if (vars != null)
            process.setVariablesArray(vars);
        List<MDWAttribute> attribs = this.mapAttributes(pProcessVO.getAttributes());
        process.setAttributeArray(attribs.toArray(new MDWAttribute[attribs.size()]));

        List<ProcessVO> subProcs = pProcessVO.getSubProcesses();
        if (subProcs != null) {
            for (ProcessVO pv : subProcs) {
                String exportedId = pv.getAttribute(WorkAttributeConstant.LOGICAL_ID);
                if (exportedId==null) exportedId = "SubProcess" + this.processIdCounter++;
                MDWProcess subProc = process.addNewSubProcess();
                if (!pProcessVO.isInRuleSet())
                	workNameRef.put(pv.getProcessId(), exportedId);
                this.mapProcess(subProc, pv, exportedId);
            }
        }
        acts = this.mapActivitySynchronizations(pProcessVO.getActivities(), acts);
        if (acts != null)
            process.setActivityArray(acts);
        MDWTransition[] trans = this.mapTransitions(pProcessVO.getTransitions(), pProcessVO
            .getProcessId(), pProcExpId);
        if (trans != null)
            process.setTransitionArray(trans);
        trans = this.mapTransitionDependents(pProcessVO.getTransitions(), trans);
        if (trans != null)
            process.setTransitionArray(trans);
        mapExternalEvents(pProcessVO, process);
        if (pProcessVO.getTextNotes()!=null)
        	process.setTextNoteArray(mapTextNotes(pProcessVO.getTextNotes(), process));
    }

    private void mapAllExternalEvents(MDWProcessDefinition procDefn, List<ExternalEventVO> list) {
    	for (ExternalEventVO one : list) {
            MDWExternalEvent event = procDefn.addNewExternalEventHandler();
            event.setEventHandler(one.getEventHandler());
            event.setEventName(one.getEventName());
        }
    }

    protected void exportRuleSets(MDWProcessDefinition procDefn, List<RuleSetVO> rulesets) {
    	// this for MDW 5 only - will be overriden
    }

    protected void exportTaskTemplates(MDWProcessDefinition procDefn, List<TaskVO> taskTemplates) {
        // this for MDW 5 only - will be overriden
    }

    protected void exportPackageAttributes(MDWProcessDefinition procDefn, List<AttributeVO> packageAttrs) {
        // only for MDW 5
    }

    protected void exportCustomAttributes(MDWProcessDefinition procDefn, List<CustomAttributeVO> customAttrs) {
        // only for MDW 5
    }

    protected void exportConfiguration(MDWProcessDefinition procDefn, PackageVO pkg) throws DataAccessException {
        // only for MDW 5
    }

    /**
         * Method that maps the passed in params to external event
         * @param pExtEventName
         * @param pExtEventHandlerName
         *
         * TODO: need to have external event directly loaded to ProcessVO
         */
    private void mapExternalEvents(ProcessVO processVO, MDWProcess process)
            throws DataAccessException {
        if (processVO.getExternalEvents()==null) return;
        for (ExternalEventVO vo : processVO.getExternalEvents()) {
            MDWExternalEvent event = process.addNewExternalEvent();
            event.setEventName(vo.getEventName());
            event.setEventHandler(vo.getEventHandler());
        }
    }

    /**
         * Method that maps the attributes
         * @param pAttribVOs
         */
    private MDWActivity[] mapActivities(List<ActivityVO> pArrVO) {

    if (pArrVO == null) {
        return null;
    }
    MDWActivity[] mdwActivity = new MDWActivity[pArrVO.size()];
    for (int i = 0; i < mdwActivity.length; i++) {
        ActivityVO vo = pArrVO.get(i);
        mdwActivity[i] = MDWActivity.Factory.newInstance();
        String exportedId = vo.getLogicalId();
        if (exportedId==null) exportedId = "Activity" + this.activityIdCounter++;
        mdwActivity[i].setId(exportedId);
        mdwActivity[i].setName(vo.getActivityName());
        mdwActivity[i].setImplementation(vo.getImplementorClassName());
        List<MDWAttribute> attribs = this.mapAttributes(vo.getAttributes());
        mdwActivity[i].setAttributeArray(attribs.toArray(new MDWAttribute[attribs.size()]));
//        if (vo.getSla()!=0) mdwActivity[i].setSla(vo.getSla());
        this.workNameRef.put(vo.getActivityId(), exportedId);
    }
    return mdwActivity;
    }

    /**
         * Method that maps the attributes
         * @param pAttribVOs
         */
    private MDWActivity[] mapActivitySynchronizations(List<ActivityVO> pArrVO, MDWActivity[] pMDWAct) {
        if (pArrVO == null || pArrVO.size() == 0 || pMDWAct == null || pMDWAct.length == 0) {
            return pMDWAct;
        }
        for (ActivityVO vo : pArrVO) {
            Long[] synchIds = vo.getSynchronzingIds();
            if (synchIds == null || synchIds.length == 0) {
                continue;
            }
            String exportedActId = (String) this.workNameRef.get(vo.getActivityId());
            for (int j = 0; j < synchIds.length; j++) {
                String exportedId = (String) this.workNameRef.get(synchIds[j]);
                if (exportedId == null) {
//                    logger.warn("Failed to locate the Exported Id for SynchWorkId:" + synchIds[j]);
                    continue;
                }
                for (int k = 0; k < pMDWAct.length; k++) {
                    if (exportedActId.equalsIgnoreCase(pMDWAct[k].getId())) {
                    pMDWAct[k].addSynchronizedOnWork(exportedId);

                    }
                }
            }
        }
        return pMDWAct;
    }

    /**
         * Method that maps the transitions
         * @param pTransVos
         * @return MDWTransition[]
         */
    private MDWTransition[] mapTransitions(List<WorkTransitionVO> pTransVos,
    		Long pProcessId, String pProcExpId)
        throws DataAccessException {
        if (pTransVos == null || pTransVos.size() == 0) {
            return null;
        }
        MDWTransition[] trans = new MDWTransition[pTransVos.size()];
        for (int i = 0; i < trans.length; i++) {
            WorkTransitionVO vo = pTransVos.get(i);
            trans[i] = MDWTransition.Factory.newInstance();
            if (vo.getAttributes()!=null) {
            	List<MDWAttribute> attribs = this.mapAttributes(vo.getAttributes());
            	trans[i].setAttributeArray(attribs.toArray(new MDWAttribute[attribs.size()]));
            }
            trans[i].setCompletionCode(vo.getCompletionCode());
            String fromExpId;
            if (vo.getFromWorkId().longValue()==0L) fromExpId = pProcExpId;
            else fromExpId = workNameRef.get(vo.getFromWorkId());
            String toExpId = workNameRef.get(vo.getToWorkId());
            trans[i].setFrom(fromExpId);
            trans[i].setTo(toExpId);
            String eventName = EventType.getEventTypeName(vo.getEventType());
            if (fromExpId == null || toExpId == null || eventName == null) {
                String msg = "One of the required params for Transitions is missing. Process:"
                    + pProcessId + " From:" + vo.getFromWorkId() + " To:"
                    + vo.getToWorkId() + " Event:" + eventName;
//                logger.warn(msg);
                throw new DataAccessException(msg);
            }
            trans[i].setEvent(MDWTransitionEvent.Enum.forString(eventName));
            if (vo.getValidatorClassName() != null) {
            	trans[i].addNewValidation();
            	trans[i].getValidation().setValidationClass(vo.getValidatorClassName());
            	trans[i].getValidation().setName(vo.getValidatorClassName());
            }
            String exportedId = vo.getLogicalId();
            if (exportedId==null) exportedId = "Transition" + this.transitionIdCounter++;
            trans[i].setId(exportedId);
            this.transIdRef.put(vo.getWorkTransitionId(), exportedId);

        }

        return trans;

    }

    /**
         * Method that maps the transitions
         * @param pTransVos
         * @return MDWTransition[]
         */
    private MDWTransition[] mapTransitionDependents(List<WorkTransitionVO> pTransVos,
        MDWTransition[] pMDWTrans) {

        if (pTransVos == null || pTransVos.size() == 0 || pMDWTrans == null
                || pMDWTrans.length == 0) {
            return null;
        }
        for (WorkTransitionVO vo : pTransVos) {
            List<Long> depndIds = vo.getDependentTransitionIds();
            if (depndIds == null) continue;
            String exportedActId = (String) this.transIdRef.get(vo.getWorkTransitionId());
            for (Long l : depndIds) {
                String exportedId = this.transIdRef.get(l);
                if (exportedId == null) {
//                    logger.warn("Failed to locate the Exported Id for Dep Trans Id:" + l);
                    continue;
                }
                for (int k = 0; k < pMDWTrans.length; k++) {
                    if (exportedActId.equalsIgnoreCase(pMDWTrans[k].getId())) {
                        pMDWTrans[k].addDependsOn(exportedId);
                    }
                }
            }
        }
        return pMDWTrans;
    }

    /**
         * Method that maps the implementors
         * @param implementors
         * @return List of MDWActivityImplementor
         * @throws DataAccessException
         */
    private List<MDWActivityImplementor> mapActivityImplementors(List<ActivityImplementorVO> implementors)
        throws DataAccessException {
        List<MDWActivityImplementor> retList = new ArrayList<MDWActivityImplementor>();
        if (implementors == null) {
            return retList;
        }
        for (ActivityImplementorVO vo : implementors) {
            String implClass = vo.getImplementorClassName();
            if (this.activityImplRef.containsKey(implClass)) {
                continue;
            }
            MDWActivityImplementor retImpl = MDWActivityImplementor.Factory.newInstance();
            retImpl.setImplementation(vo.getImplementorClassName());
            if (vo.getBaseClassName() != null)
                retImpl.setType(vo.getBaseClassName());
            else
                retImpl.setType(GeneralActivity.class.getName());
            if (vo.getIconName() != null)
                retImpl.setIconFile(vo.getIconName());
            if (vo.getLabel() != null)
                retImpl.setLabel(vo.getLabel());
            if (vo.getAttributeDescription() != null)
                retImpl.setAttributeDescription(vo.getAttributeDescription());
            if (vo.isHidden())
                retImpl.setHidden(true);
            MDWVariable[] vars = this.mapVariables(vo.getVariables());
            if (vars != null)
                retImpl.setVariablesArray(vars);
            retList.add(retImpl);
            this.activityImplRef.put(implClass, implClass);
        }
        return retList;

    }

    /**
         * Method that maps the implementors
         * @param pProcessVO
         * @return MDWActivityImplementor[]
         * @throws DataAccessException
         */
    private void mapActivityImplementors(ProcessVO pProcessVO, List<MDWActivityImplementor> pRetList)
        throws DataAccessException {
        pRetList.addAll(this.mapActivityImplementors(pProcessVO.getImplementors()));
        List<ProcessVO> subProcs = pProcessVO.getSubProcesses();
        if (subProcs == null) return;
        for (ProcessVO pv : subProcs) {
            this.mapActivityImplementors(pv, pRetList);
        }
    }

    private MDWTextNote[] mapTextNotes(List<TextNoteVO> textNoteVOs, MDWProcess process) {
    	if (textNoteVOs == null || textNoteVOs.size() == 0) return null;
    	MDWTextNote[] retArray = new MDWTextNote[textNoteVOs.size()];
    	for (int i=0; i<textNoteVOs.size(); i++) {
    		TextNoteVO vo = textNoteVOs.get(i);
    		retArray[i] = MDWTextNote.Factory.newInstance();
    		retArray[i].setContent(vo.getContent());
    		retArray[i].setReference(vo.getReference());
    		if (vo.getAttributes()!=null) {
    			for (AttributeVO attr : vo.getAttributes()) {
    				MDWAttribute mdwattr = retArray[i].addNewAttribute();
    				mdwattr.setName(attr.getAttributeName());
    				mdwattr.setValue(attr.getAttributeValue());
    			}
    		}
    	}
    	return retArray;
    }

    protected String createPackageVersionString(int version) {
        if (DataAccess.supportedSchemaVersion < DataAccess.schemaVersion55)
            return PackageVO.formatVersionOld(version);
        else
            return PackageVO.formatVersion(version);
    }

    protected String createVersionString(int version) {
        return RuleSetVO.formatVersion(version);
    }

    /**
     * Method that maps the variables
     */
    private MDWVariable[] mapVariables(List<VariableVO> pVars) {
        if (pVars == null) {
            return null;
        }
        List<MDWVariable> mdwVarList = new ArrayList<MDWVariable>();
        for (VariableVO vo : pVars) {
            MDWVariable mdwVar = MDWVariable.Factory.newInstance();
            mdwVar.setName(vo.getVariableName());
//            mdwVar.setValueLocator(vo.getValueLocator());
            mdwVar.setReferredAs(vo.getVariableReferredAs());
//            mdwVar.setIsOptional(!vo.isRequired());
            if (vo.getVariableCategory() != null) {
                switch (vo.getVariableCategory().intValue()) {
                  case VariableVO.CAT_LOCAL:
                    mdwVar.setCategory(MDWVariable.Category.LOCAL);
                    break;
                  case VariableVO.CAT_INPUT:
                    mdwVar.setCategory(MDWVariable.Category.INPUT);
                    break;
                  case VariableVO.CAT_OUTPUT:
                    mdwVar.setCategory(MDWVariable.Category.OUTPUT);
                    break;
                  case VariableVO.CAT_INOUT:
                    mdwVar.setCategory(MDWVariable.Category.INOUT);
                    break;
                  case VariableVO.CAT_STATIC:
                    mdwVar.setCategory(MDWVariable.Category.STATIC);
                    break;
                  default:
                    mdwVar.setCategory(MDWVariable.Category.LOCAL);
                }
            }
            if (vo.getDescription()!=null && vo.getDescription().length()>0) {
            	mdwVar.setDescription(vo.getDescription());
            }
            mdwVar.setDisplaySequence(new BigInteger("0"));
            if (vo.getDisplaySequence() != null && vo.getDisplaySequence().intValue() != 0) {
                mdwVar.setDisplaySequence(new BigInteger(vo.getDisplaySequence().toString()));
            }
//            mdwVar.setDataSource(VariableVO.DATA_SOURCE_WORKFLOW);
//            if (vo.getDataSource() != null && !vo.getDataSource().equals(VariableVO.DATA_SOURCE_WORKFLOW)) {
//                mdwVar.setDataSource(vo.getDataSource());
//            }
            mdwVar.addNewType();
            mdwVar.getType().setName(vo.getVariableType());
//            mdwVar.getType().setTranslatorClass(vo.getVariableType().getTranslatorClass());

            mdwVarList.add(mdwVar);

        }
        return (MDWVariable[]) mdwVarList.toArray(new MDWVariable[mdwVarList.size()]);

    }

    /**
         * Method that maps the attributes
         * @param pAttribVOs
         */
    private List<MDWAttribute> mapAttributes(List<AttributeVO> pAttribs) {
        List<MDWAttribute> mdwAttrList = new ArrayList<MDWAttribute>(pAttribs.size());
        for (int i = 0; i < pAttribs.size(); i++) {
        	AttributeVO attr = pAttribs.get(i);
        	// ignore override attributes, logical id, and defunct designer attributes
        	if (WorkAttributeConstant.isOverrideAttribute(attr.getAttributeName()))
        		continue;
        	if (attr.getAttributeName().equals(WorkAttributeConstant.LOGICAL_ID))
        	    continue;
            if (attr.getAttributeName().equals("REFERENCED_ACTIVITIES"))
                continue;
            if (attr.getAttributeName().equals("REFERENCED_PROCESSES"))
                continue;
            if (attr.getAttributeName().equals("processid"))
                continue;
            MDWAttribute mdwAttrib = MDWAttribute.Factory.newInstance();
            mdwAttrib.setName(attr.getAttributeName());
            mdwAttrib.setValue(attr.getAttributeValue());
            mdwAttrList.add(mdwAttrib);
        }
        return mdwAttrList;

    }

    protected void importRuleSets(PackageVO packageVO, MDWProcessDefinition processDefn) {
    	// this is for MDW 5 only - to be overriden
    }

    protected void importTaskTemplates(PackageVO packageVO, MDWProcessDefinition processDefn) {
        // for MDW 5 only
    }

    protected void importPackageAttributes(PackageVO packageVO, MDWProcessDefinition processDefn) {
        // for MDW 5 only
    }

    protected void importCustomAttributes(PackageVO packageVO, MDWProcessDefinition processDefn) {
        // for MDW 5 only
    }

    protected void importConfiguration(PackageVO packageVO, MDWProcessDefinition procdef) {
        // for MDW 5 only
    }

    public String exportResources(List<RuleSetVO> resources)
		    throws DataAccessException, XmlException {
		ProcessDefinitionDocument defnDoc = ProcessDefinitionDocument.Factory.newInstance();
		MDWProcessDefinition procDefn = defnDoc.addNewProcessDefinition();
		for (RuleSetVO resource : resources) {
			MDWRuleSet mdwRuleSet = procDefn.addNewRuleSet();
			mdwRuleSet.setRuleSetName(resource.getName());
			mdwRuleSet.setRuleSetLanguage(resource.getLanguage());
			mdwRuleSet.setRuleSetContent(resource.getRuleSet());
		}
		procDefn.setSchemaVersion(createVersionString(DataAccess.currentSchemaVersion));
		procDefn.setPackageVersion("0.0");
		return processDefDocToString(defnDoc);
    }

    public List<RuleSetVO> importResources(String xml)
		    throws DataAccessException, XmlException {
		ProcessDefinitionDocument defnDoc = ProcessDefinitionDocument.Factory.parse(xml, Compatibility.namespaceOptions());
		List<RuleSetVO> resources = new ArrayList<RuleSetVO>();
		for (MDWRuleSet mdwRuleSet : defnDoc.getProcessDefinition().getRuleSetList()) {
			RuleSetVO resource = new RuleSetVO();
			resource.setId(-1L);
			resource.setName(mdwRuleSet.getRuleSetName());
			resource.setLanguage(mdwRuleSet.getRuleSetLanguage());
			resource.setRuleSet(mdwRuleSet.getRuleSetContent());
			resources.add(resource);
		}
		return resources;
    }

    protected String processDefDocToString(ProcessDefinitionDocument processDefDoc) throws XmlException {
        if (schemaTypeTranslator != null)
            return schemaTypeTranslator.getOldProcessDefinition(processDefDoc);
        else
            return processDefDoc.xmlText(getXmlOptions());
    }

    protected XmlOptions getXmlOptions() {
        return new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(2);
    }

    /* (non-Javadoc)
     * @see com.centurylink.mdw.dataaccess.ProcessExporter#exportOverrideAttributes(java.lang.String, com.centurylink.mdw.model.value.process.ProcessVO)
     */
    @Override
    public String exportOverrideAttributes(String prefix, ProcessVO processVO, int schemaVersion)
            throws DataAccessException, XmlException {
        // TODO Auto-generated method stub
        return null;
    }
}
