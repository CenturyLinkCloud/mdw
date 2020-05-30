package com.centurylink.mdw.common.translator.impl;

import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.variable.VariableTranslator;

public abstract class BaseTranslator implements VariableTranslator {
    private Package pkg;
    public Package getPackage() { return pkg; }
    public void setPackage(Package pkg) { this.pkg = pkg; }
}
