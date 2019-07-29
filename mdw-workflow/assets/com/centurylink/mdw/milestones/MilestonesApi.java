package com.centurylink.mdw.milestones;

import com.centurylink.mdw.common.service.Query;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.config.PropertyGroup;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.workflow.Linked;
import com.centurylink.mdw.model.workflow.Milestone;
import com.centurylink.mdw.model.workflow.MilestoneFactory;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.service.data.process.HierarchyCache;
import com.centurylink.mdw.service.data.process.ProcessCache;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.rest.JsonRestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.json.JSONObject;

import javax.ws.rs.Path;
import java.util.Map;
import java.util.Properties;

@Path("/")
@Api("Workflow milestones")
public class MilestonesApi extends JsonRestService {

    @Path("/{masterRequestId}")
    @ApiOperation(value="Retrieve for a master request, or all milestones")
    public JSONObject get(String path, Map<String,String> headers) throws ServiceException {

        Query query = getQuery(path, headers);
        String seg4 = getSegment(path, 4);
        if (seg4 != null) {
            if (seg4.equals("definitions")) {
                String seg5 = getSegment(path, 5);
                if (seg5 != null) {
                    try {
                        // by process id
                        return getDefinition(Long.parseLong(seg5));
                    } catch (NumberFormatException ex) {
                        String proc = getSegment(path, 6);
                        if (proc != null) {
                            // process asset path
                            return getDefinition(seg5 + "/" + proc);
                        }
                    }
                }
            } else if (seg4.equals("groups")) {
                return getGroups();
            } else {
                // by masterRequestId
                boolean future = query.getBooleanFilter("future");
                return ServiceLocator.getWorkflowServices().getMilestones(seg4, future).getJson();
            }
        } else {
            return ServiceLocator.getWorkflowServices().getMilestones(query).getJson();
        }
        throw new ServiceException(ServiceException.BAD_REQUEST, "Bad path: " + path);
    }

    @Path("/definitions/{processId}")
    public JSONObject getDefinition(Long processId) throws ServiceException {
        Linked<Milestone> milestones = HierarchyCache.getMilestones(processId);
        if (milestones == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Milestones not found: " + processId);
        return milestones.getJson();
    }

    @Path("/definitions/{package}/{process}")
    public JSONObject getDefinition(String assetPath) throws ServiceException {
        Process process = ProcessCache.getProcess(assetPath);
        if (process == null)
            throw new ServiceException(ServiceException.NOT_FOUND, "Process not found: " + assetPath);
        return getDefinition(process.getId());
    }

    @Path("/groups")
    public JSONObject getGroups() {
        Properties props = PropertyManager.getInstance().getProperties(PropertyNames.MDW_MILESTONE_GROUPS);
        PropertyGroup milestonesGroup = new PropertyGroup("milestone.groups", PropertyNames.MDW_MILESTONE_GROUPS, props);
        for (PropertyGroup defaultGroup : MilestoneFactory.DEFAULT_GROUPS) {
            boolean overridden = false;
            for (PropertyGroup subgroup : milestonesGroup.getSubgroups()) {
                if (subgroup.getName().equals(defaultGroup.getName())) {
                    overridden = true;
                    break;
                }
            }
            if (!overridden) {
                milestonesGroup.getSubgroups().add(defaultGroup);
            }
        }
        if (props.isEmpty()) // remove Other
            milestonesGroup.getSubgroups().removeIf(subgroup -> MilestoneFactory.OTHER_GROUP.getName().equals(subgroup.getName()));

        return milestonesGroup.getJson();
    }
}
