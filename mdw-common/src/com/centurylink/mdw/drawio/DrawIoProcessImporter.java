/*
 * Copyright (C) 2018 CenturyLink, Inc.
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
package com.centurylink.mdw.drawio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.centurylink.mdw.app.Templates;
import com.centurylink.mdw.model.workflow.Display;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.procimport.ProcessImporter;

/**
 * Imports activities and transitions in draw.io format.
 */
public class DrawIoProcessImporter implements ProcessImporter {

    /**
     * TODO: handle compressed files
     */
    @Override
    public Process importProcess(File from) throws IOException {
        try {
            Process process = new Process(new JSONObject(Templates.get("assets/new.proc")));
            String name = from.getName();
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0 && lastDot < name.length() - 1)
                name = name.substring(0, lastDot);
            process.setName(name);
            process.setAttribute(Display.NAME, "x=25,y=25,w=0,h=0");
            MxGraphParser graph = importGraph(new FileInputStream(from));
            process.setActivities(graph.getActivities());
            process.setTransitions(graph.getTransitions());
            if (!graph.getTextNotes().isEmpty()) {
                process.setTextNotes(graph.getTextNotes());
            }
            return process;
        }
        catch (SAXException | ParserConfigurationException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    public MxGraphParser importGraph(InputStream input) throws IOException, SAXException, ParserConfigurationException {
        return new MxGraphParser().read(input);
    }
}
