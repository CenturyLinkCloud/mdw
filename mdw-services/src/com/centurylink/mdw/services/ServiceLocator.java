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
package com.centurylink.mdw.services;

import com.centurylink.mdw.services.asset.AssetServicesImpl;
import com.centurylink.mdw.services.event.EventServicesImpl;
import com.centurylink.mdw.services.project.CollaborationServicesImpl;
import com.centurylink.mdw.services.project.SolutionServicesImpl;
import com.centurylink.mdw.services.request.RequestServicesImpl;
import com.centurylink.mdw.services.rules.RulesServicesImpl;
import com.centurylink.mdw.services.system.SystemServicesImpl;
import com.centurylink.mdw.services.task.TaskServicesImpl;
import com.centurylink.mdw.services.test.TestingServicesImpl;
import com.centurylink.mdw.services.user.UserServicesImpl;
import com.centurylink.mdw.services.workflow.ProcessServicesImpl;
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

    public static ProcessServices getProcessServices() {
        return new ProcessServicesImpl(); // TODO use ServiceRegistry
    }

    public static UserServices getUserServices() {
        return new UserServicesImpl(); // TODO use ServiceRegistry
    }

    public static RequestServices getRequestServices() {
        return new RequestServicesImpl(); // TODO use ServiceRegistry
    }

    public static AssetServices getAssetServices() {
        return new AssetServicesImpl(); // TODO use ServiceRegistry
    }

    public static SolutionServices getSolutionServices() {
        return new SolutionServicesImpl(); // TODO use ServiceRegistry
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

    public static SystemServices getSystemServices() {
        return new SystemServicesImpl();
    }

    public static RulesServices getRulesServices() {
        return new RulesServicesImpl();
    }

}
