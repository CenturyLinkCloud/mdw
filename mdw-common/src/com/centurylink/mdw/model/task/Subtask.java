package com.centurylink.mdw.model.task;

import java.math.BigInteger;

/**
 * Execution plan subprocess.
 */
public class Subtask {
    /**
     * Template asset path.
     */
    private String templatePath;
    public String getTemplatePath() { return templatePath; }
    public void setTemplatePath(String templatePath) { this.templatePath = templatePath; }

    private BigInteger count;
    public BigInteger getCount() { return count; }
    public void setCount(BigInteger count) { this.count = count; }
}
