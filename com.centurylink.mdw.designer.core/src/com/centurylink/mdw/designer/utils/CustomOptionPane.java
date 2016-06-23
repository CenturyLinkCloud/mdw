/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils; 

import java.awt.Component;

public interface CustomOptionPane
{
    void showMessage(Component parent, String message);

    void showError(Component parent, String message);
    
    boolean confirm(Component parent, String message, boolean yes_no);

    int choose(Component parent, String message, String[] choices);
    
    String getInput(Component parent, String message);
    
} 
