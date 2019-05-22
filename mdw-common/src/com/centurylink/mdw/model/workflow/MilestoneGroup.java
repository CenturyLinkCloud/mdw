package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.model.Jsonable;

public class MilestoneGroup implements Jsonable {

    public static final String ROOT_PROP = "mdw.milestone.groups";

    private String name;
    public String getName() { return name; }


    private String color;
    public String getColor() { return color; }


}
