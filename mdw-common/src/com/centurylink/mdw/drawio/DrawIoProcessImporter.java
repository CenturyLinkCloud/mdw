package com.centurylink.mdw.drawio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

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
            Process process = new Process();
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
