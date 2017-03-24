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

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

public  class LabelEditor extends JTextArea {
    
    private static final long serialVersionUID = 1L;
    private static int EDITOR_R_MARGIN = 3;
    
    private int tx, ty;
    private JPanel canvas;
    private int zoom;
    
    public LabelEditor(JPanel canvas) {
        this.canvas = canvas;
        this.zoom = 100;
    }
    
    private void transCoord(int zoom, int x, int y) {
        int x0 = this.getX();
        int y0 = this.getY();
        int x1 = x0*zoom/100;
        int y1 = y0*zoom/100;
        tx = (x + x0 - x1)*100/zoom;
        ty = (y + y0 - y1)*100/zoom;
    }

    @Override
    public void paintImmediately(int x, int y, int w, int h) {
        if (zoom!=100) {
            canvas.repaint();
        } else super.paintImmediately(x, y, w, h);
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);
        if (zoom!=100) canvas.repaint();
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (zoom!=100) {
            transCoord(zoom, e.getX(), e.getY());
            e = new MouseEvent(e.getComponent(), e.getID(),
                    e.getWhen(), e.getModifiers(), tx, ty, 
                    e.getClickCount(), e.isPopupTrigger());
        }
        super.processMouseEvent(e);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (zoom!=100) {
            transCoord(zoom, e.getX(), e.getY());
            e = new MouseEvent(e.getComponent(), e.getID(),
                    e.getWhen(), e.getModifiers(), tx, ty, 
                    e.getClickCount(), e.isPopupTrigger());
        }
        super.processMouseMotionEvent(e);
    }

    @Override
    public boolean contains(int x, int y) {
        if (zoom!=100) {
            transCoord(zoom, x, y);
            return super.contains(tx, ty);
        } else return super.contains(x, y);
    } 
    
    private Dimension getTextSize() {
        Dimension d = new Dimension(0,0);
        try {
            int n = getLineCount();
            FontMetrics fm = getFontMetrics(getFont());
            d.height = fm.getHeight()*n;
            for (int i=0; i<n; i++) {
                int start = getLineStartOffset(i);
                int end = getLineEndOffset(i);
                String v = getText(start, end-start);
                int w = fm.stringWidth(v);
                if (w>d.width) d.width = w;
            }
        } catch (BadLocationException e) {
            getSize(d);
        }
        return d;
    }
    
    public void setText(String text, int x, int y, int zoom) {
        setText(text);
        Dimension d = getTextSize();
        setBounds(x, y, d.width+EDITOR_R_MARGIN, d.height);
        this.zoom = zoom;
    }
    
    public boolean adjustSize() {
        Dimension s1 = getSize();
        Dimension s2 = getTextSize();
        if (s1.width!= s2.width||s1.height!=s2.height) {
            s2.width += EDITOR_R_MARGIN;
            setSize(s2);
            return true;
        } else return false;
    }
}