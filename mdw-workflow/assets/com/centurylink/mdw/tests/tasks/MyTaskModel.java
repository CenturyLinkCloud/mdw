/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.tests.tasks;

public class MyTaskModel implements java.io.Serializable {
    
    public static final long serialVersionUID = 2L;
    
    private String taskmaster;
    public String getTaskmaster() { return taskmaster; }
    public void setTaskmaster(String tm) { this.taskmaster = tm; }
    
    @Override
    public boolean equals(Object other) {
        return other instanceof MyTaskModel && 
                ((MyTaskModel)other).taskmaster.equals(taskmaster);
    }

    @Override
    public String toString() {
        return "taskmaster: " + taskmaster;
    }
}
