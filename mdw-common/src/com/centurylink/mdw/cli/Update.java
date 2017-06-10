/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription="Update an MDW project")
public class Update {

    private String project;

    public Update(String project) {
        this.project = project;
    }

    Update() {
        // cli use only
    }

}