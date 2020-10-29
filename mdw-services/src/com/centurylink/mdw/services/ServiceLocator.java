package com.centurylink.mdw.services;

import com.centurylink.mdw.services.asset.AssetServicesImpl;
import com.centurylink.mdw.services.asset.StagingServicesImpl;
import com.centurylink.mdw.services.event.EventServicesImpl;
import com.centurylink.mdw.services.project.CollaborationServicesImpl;
import com.centurylink.mdw.services.project.SolutionServicesImpl;
import com.centurylink.mdw.services.request.RequestServicesImpl;
import com.centurylink.mdw.services.rules.RulesServicesImpl;
import com.centurylink.mdw.services.system.SystemServicesImpl;
import com.centurylink.mdw.services.task.TaskServicesImpl;
import com.centurylink.mdw.services.test.TestingServicesImpl;
import com.centurylink.mdw.services.user.UserServicesImpl;
import com.centurylink.mdw.services.workflow.DesignServicesImpl;
import com.centurylink.mdw.services.workflow.WorkflowServicesImpl;

/**
 * TODO: Instances should be injected.
 */
public class ServiceLocator {

    public static EventServices getEventServices() {
        return new EventServicesImpl();
    }

    public static TaskServices getTaskServices() {
        return new TaskServicesImpl();
    }

    public static UserServices getUserServices() {
        return new UserServicesImpl();
    }

    public static RequestServices getRequestServices() {
        return new RequestServicesImpl();
    }

    public static AssetServices getAssetServices() {
        return new AssetServicesImpl();
    }
    public static StagingServices getStagingServices() {
        return new StagingServicesImpl();
    }

    public static SolutionServices getSolutionServices() {
        return new SolutionServicesImpl();
    }

    public static CollaborationServices getCollaborationServices() {
        return new CollaborationServicesImpl();
    }

    public static TestingServices getTestingServices() {
        return new TestingServicesImpl();
    }

    public static WorkflowServices getWorkflowServices() {
        return new WorkflowServicesImpl();
    }

    public static DesignServices getDesignServices() {
        return new DesignServicesImpl();
    }

    public static SystemServices getSystemServices() {
        return new SystemServicesImpl();
    }

    public static RulesServices getRulesServices() {
        return new RulesServicesImpl();
    }

}
