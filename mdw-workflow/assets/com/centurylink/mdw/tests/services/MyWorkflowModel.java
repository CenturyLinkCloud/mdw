package com.centurylink.mdw.tests.services;

import com.centurylink.mdw.tests.tasks.MyTaskModel;

public class MyWorkflowModel implements java.io.Serializable {
    
    public static final long serialVersionUID = 2L;
    
    private String flowmaster;
    public String getFlowmaster() { return flowmaster; }
    public void setFlowmaster(String fm) { this.flowmaster = fm; }
    
    @Override
    public boolean equals(Object other) {
        return other instanceof MyWorkflowModel && 
                ((MyWorkflowModel)other).flowmaster.equals(flowmaster);
    }

    @Override
    public String toString() {
        return "flowmaster: " + flowmaster;
    }
}
