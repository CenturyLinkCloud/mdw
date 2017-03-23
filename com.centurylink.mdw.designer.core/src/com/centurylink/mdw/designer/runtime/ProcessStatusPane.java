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
package com.centurylink.mdw.designer.runtime; 


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Map;

import javax.swing.JPanel;

import com.centurylink.mdw.model.data.work.WorkStatuses;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;
/**
 * Static images to indicate the status of work
 */
public class ProcessStatusPane extends JPanel
{ 
    private static final long serialVersionUID = 1L;
    
    private static Color[] nodeColors = {
            Color.BLUE,
            Color.GREEN,
            Color.DARK_GRAY,
            Color.RED,
            Color.YELLOW,
            Color.CYAN,
            Color.LIGHT_GRAY };
    private static String[] nodeLegends = {
            "Pending Processing",
            "In Progress",
            "Completed",
            "Failed",
            "Waiting",
            "On Hold",
            "Cancelled" };
    
    private ProcessInstancePage page;
    private Map<Integer,String> workStatuses;
    
    public ProcessStatusPane(ProcessInstancePage page){
        super();
        this.page = page;
        workStatuses = WorkStatuses.getWorkStatuses();
    }

	protected void paintComponent(Graphics g) {
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }     
		super.paintComponent(g);
        
        int nodeLegendY = 5;
        g.setColor(new Color(0.8f,0.8f,0.5f));
        g.drawRect(5,nodeLegendY,175,nodeLegends.length*25+25);
        g.fillRect(5,nodeLegendY,175,20);
        g.setColor(Color.BLACK);
        g.drawString("Activity Status", 10, nodeLegendY+15);
        
        for (int i=0; i<nodeLegends.length; i++) {
            g.setColor(nodeColors[i]);
            g.fillRect(10,nodeLegendY+25+i*25,20,20);
            g.setColor(Color.BLACK);
            g.drawString(nodeLegends[i],40, nodeLegendY+40+25*i);
        }
        
        int infoY = nodeLegends.length*25+55;
        
        if (page != null) {
        	ProcessInstanceVO procInst = page.getProcessInstance();
            g.drawString("Instance ID: " + procInst.getId(), 10, infoY); infoY += 20;
            g.drawString("Process ID: " + procInst.getProcessId(), 10, infoY); infoY += 20;
            g.drawString("Master ID: " + procInst.getMasterRequestId(), 10, infoY); infoY += 20;
            g.drawString("Process Name: " + procInst.getProcessName(), 10, infoY); infoY += 20;
            g.drawString("Owner Type: " + procInst.getOwner(), 10, infoY); infoY += 20;
            g.drawString("Owner ID: " + procInst.getOwnerId(), 10, infoY); infoY += 20;
            g.drawString("Status: " + workStatuses.get(new Integer(procInst.getStatusCode())), 10, infoY); infoY += 20;
            g.drawString("Start: " + procInst.getStartDate(), 10, infoY); infoY += 20;
            g.drawString("Finish: " + procInst.getEndDate(), 10, infoY); infoY += 20;
        }
	}
} 
