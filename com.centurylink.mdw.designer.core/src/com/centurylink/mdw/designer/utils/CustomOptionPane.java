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
