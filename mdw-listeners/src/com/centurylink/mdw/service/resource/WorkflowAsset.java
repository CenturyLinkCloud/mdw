/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.service.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.json.JSONException;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.bpm.MDWActivity;
import com.centurylink.mdw.bpm.MDWActivityImplementor;
import com.centurylink.mdw.bpm.MDWAttribute;
import com.centurylink.mdw.bpm.MDWProcess;
import com.centurylink.mdw.bpm.MDWProcessDefinition;
import com.centurylink.mdw.bpm.ProcessDefinitionDocument;
import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.constant.OwnerType;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.service.JsonService;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.common.service.TextService;
import com.centurylink.mdw.common.service.XmlService;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.dataaccess.DatabaseAccess;
import com.centurylink.mdw.dataaccess.ProcessLoader;
import com.centurylink.mdw.dataaccess.file.AssetFile;
import com.centurylink.mdw.model.value.activity.ActivityImplementorVO;
import com.centurylink.mdw.model.value.asset.Asset;
import com.centurylink.mdw.model.value.attribute.AttributeVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.services.AssetServices;
import com.centurylink.mdw.services.BamManager;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.bam.BamManagerBean;

public class WorkflowAsset implements TextService, XmlService, JsonService {

    public static final String PARAM_NAME = "name";
    public static final String PARAM_PATH = "path";
    public static final String PARAM_VERSION = "version";

    public String getText(Map<String, Object> parameters, Map<String, String> metaInfo) throws ServiceException {
        String name = (String) parameters.get(PARAM_NAME);
        String path = (String) parameters.get(PARAM_PATH);
        String version = (String) parameters.get(PARAM_VERSION);
        String masterRequestId = (String) parameters.get(ProcessInstanceData.PARAM_MASTER_REQUEST_ID);
        try {
            if (name != null) {
                return getAssetByName(name, version);
            }
            else if (path != null) {
                return getAssetByPath(path);
            }
            else if (masterRequestId != null) {
                return getAssetByMasterRequestId(masterRequestId);
            }
            else {
                throw new ServiceException("Missing parameter: one of 'path' or 'name' or 'MasterRequestId' is required. path = "
                        + path + " masterRequestId =" + masterRequestId + " and name =" + name + "version=" + version);
            }
        }
        catch (DataAccessException ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
        catch (XmlException e) {
            throw new ServiceException(e.getMessage(), e);
        }
        catch (IOException e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    /**
     * Bypasses cache.
     * @throws ServiceException
     * @throws IOException
     */
    private String getAssetByName(String procname, String version) throws DataAccessException, XmlException, ServiceException, IOException {
        //Use asset services to try VCS
        AssetServices assetServices = ServiceLocator.getAssetServices();
        Asset asset = assetServices.getAsset(procname);
        ProcessLoader loader = DataAccess.getProcessLoader();
        //RuleSetVO ruleSetVO = getRuleSetForPackage(loader, new AssetVersionSpec(procname, version));
        if (asset == null || !asset.getFile().isFile()) {
            return null;
        }

        else {
            ProcessDefinitionDocument processDef = ProcessDefinitionDocument.Factory.parse(new String(FileHelper.readFromFile(asset.getFile().getPath())));
            List<MDWActivityImplementor> actArrList = mapActivityImplementors(loader.getActivityImplementors());
            MDWProcessDefinition procDefn  = processDef.getProcessDefinition();
            List<AttributeVO> bamAttrs = new ArrayList<AttributeVO>();
            Map<String,String> attributes = DataAccess.getDbProcessLoader().getAttributes(OwnerType.PROCESS, ((AssetFile)(asset.getFile())).getId());
            if (attributes != null) {
                for (String name : attributes.keySet()) {
                    if (name.contains("BAM@"))
                        bamAttrs.add(new AttributeVO(name, attributes.get(name)));
                }
            }

            for (AttributeVO bamAttr: bamAttrs) {
                MDWAttribute newBamAttr = procDefn.getProcessArray(0).addNewAttribute();
                newBamAttr.setName(bamAttr.getAttributeName());
                newBamAttr.setValue(bamAttr.getAttributeValue());
            }
            procDefn.setImplementorArray(actArrList.toArray(new MDWActivityImplementor[] {}));
            addAllAttributes(procDefn.getProcessList(), bamAttrs);
            return processDef.xmlText();
        }
//        else {
//            return ruleSetVO.getRuleSet();
//        }
    }

    private String getAssetByPath(String path) {
        return RuleSetCache.getRuleSet(path).getRuleSet();
    }

    private String getAssetByMasterRequestId(String masterRequestId) throws DataAccessException, XmlException {
        BamManager bamMgr = new BamManagerBean(new DatabaseAccess(null));
        Long processId = bamMgr.getMainProcessId(masterRequestId);
        ProcessLoader loader = DataAccess.getProcessLoader();
        PackageVO processPkg = PackageVOCache.getProcessPackage(processId);
        ProcessDefinitionDocument processDef = ProcessDefinitionDocument.Factory.parse(loader.getRuleSet(processId).getRuleSet());
        List<MDWActivityImplementor> actArrList = mapActivityImplementors(loader.getActivityImplementors());
        MDWProcessDefinition procDefn  = processDef.getProcessDefinition();
        if (processPkg != null) {
            procDefn.setPackageVersion(processPkg.getVersionString());
            procDefn.setPackageName(processPkg.getPackageName());
        }
        // Add a special attribute for owner (owner isn't passed explicitly)
        MDWAttribute ownerAttr = procDefn.getProcessArray(0).addNewAttribute();
        ownerAttr.setName("ownerId");
        ownerAttr.setValue(String.valueOf(processId));
        ownerAttr.setOwner(String.valueOf(processId));

        List<AttributeVO> attrs = new ArrayList<AttributeVO>();
        Map<String,String> attributes = DataAccess.getDbProcessLoader().getAttributes(OwnerType.PROCESS, processId);
        if (attributes != null) {
            for (String name : attributes.keySet()) {
                if (name.contains("BAM@") || name.contains("ARTIS@"))
                    attrs.add(new AttributeVO(processId, name, attributes.get(name)));

            }
        }

        for (AttributeVO attr:  attrs) {
            MDWAttribute newAttr = procDefn.getProcessArray(0).addNewAttribute();
            newAttr.setName(attr.getAttributeName());
            newAttr.setValue(attr.getAttributeValue());
            newAttr.setOwner(String.valueOf(attr.getAttributeId()));
        }

        procDefn.setImplementorArray(actArrList.toArray(new MDWActivityImplementor[] {}));
        addAllAttributes(procDefn.getProcessList(), attrs);
        return processDef.xmlText();
    }


    /**
     * @param processList
     * @param bamAttrs
     */
    private void addAllAttributes(List<MDWProcess> processList, List<AttributeVO> attrs) {
        for (MDWProcess process:  processList){
            List<MDWActivity> acts = process.getActivityList();
            for (MDWActivity act :acts){
                List<AttributeVO>  actAttrs = getOverrideAttributes(attrs,  act);
                for (AttributeVO attr:  actAttrs ){
                    MDWAttribute newAttr = act.addNewAttribute();
                    newAttr.setName(attr.getAttributeName());
                    newAttr.setValue(attr.getAttributeValue());
                    newAttr.setOwner(String.valueOf(attr.getAttributeId()));
                }
            }
        }
    }

    /**
     * @param bamAttrs
     * @param act
     * @return
     */
    private List<AttributeVO> getOverrideAttributes(List<AttributeVO> attrs, MDWActivity act) {
        List<AttributeVO>  actAttrs  = new ArrayList<AttributeVO>();
        for (AttributeVO attr : attrs) {
            if (attr.getAttributeName().startsWith(WorkAttributeConstant.OVERRIDE_ACTIVITY)) {
                int k = attr.getAttributeName().indexOf(':');
                String attrname = attr.getAttributeName().substring(k+1);
                Long actId = new Long(attr.getAttributeName().substring(WorkAttributeConstant.OVERRIDE_ACTIVITY.length(), k));


                if (act.getId().equalsIgnoreCase("A" + actId))
                {
                    attr.setAttributeName(attrname);
                    actAttrs.add(attr);
                }

            }
        }
        return actAttrs;

    }

   @Override
    public String getJson(Map<String, Object> parameters, Map<String, String> metaInfo) throws ServiceException {
       try {
           return org.json.XML.toJSONObject(getText(parameters, metaInfo)).toString();
       }
       catch (JSONException e) {
           throw new ServiceException("Unable to parse XML to Json "+e.getMessage(), e);
       }
    }

    @Override
    public String getXml(Map<String, Object> parameters, Map<String, String> metaInfo) throws ServiceException {
        return getText(parameters, metaInfo);
    }
    /**
     * Method that maps the implementors
     * @param pImplementors
     * @return List of MDWActivityImplementor
     * @throws DataAccessException
     */
    private List<MDWActivityImplementor> mapActivityImplementors(List<ActivityImplementorVO> pImplementors)
            throws DataAccessException {
        List<MDWActivityImplementor> retList = new ArrayList<MDWActivityImplementor>();
        if (pImplementors == null) {
            return retList;
        }
        for (ActivityImplementorVO vo : pImplementors) {
            MDWActivityImplementor retImpl = MDWActivityImplementor.Factory.newInstance();
            retImpl.setImplementation(vo.getImplementorClassName());
            if (vo.getBaseClassName()!=null) retImpl.setType(vo.getBaseClassName());
            else retImpl.setType(GeneralActivity.class.getName());
            if (vo.getIconName()!=null) retImpl.setIconFile(vo.getIconName());
            if (vo.getLabel()!=null) retImpl.setLabel(vo.getLabel());
            if (vo.getAttributeDescription()!=null) retImpl.setAttributeDescription(vo.getAttributeDescription());
            retList.add(retImpl);
        }
        return retList;
    }
}
