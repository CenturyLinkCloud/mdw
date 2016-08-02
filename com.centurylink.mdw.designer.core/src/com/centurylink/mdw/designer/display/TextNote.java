/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.display;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.List;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

import com.centurylink.mdw.model.value.activity.TextNoteVO;
import com.centurylink.mdw.model.value.attribute.AttributeVO;

/**
 *
 */
public class TextNote implements Serializable,Selectable,EditableCanvasText{

    public static final long serialVersionUID = 1L;

	public int x, y, w, h;
    public Graph graph;
    public TextNoteVO vo;
    public JTextArea textarea;

    public TextNote(TextNoteVO vo, Graph graph) {
        this.graph = graph;
        this.vo = vo;
        load_temp_vars(vo);
        textarea = new JTextArea();
        textarea.setBounds(x+1, y+1, w-2, h-2);
        textarea.setText(vo.getContent());
    }

    public TextNote(TextNoteVO vo, Graph graph, int x, int y, int w, int h) {
        this.graph = graph;
        this.vo = vo;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        textarea = new JTextArea();
        textarea.setBounds(x+1, y+1, w-2, h-2);
        textarea.setText(vo.getContent());
    }

	public boolean onPoint(int x1, int y1) {
		return (x1>=x && x1<x+w && y1>=y && y1<y+h);
	}

    private void load_temp_vars(TextNoteVO vo) {
        String guiinfo = vo.getAttribute(graph.geo_attribute);
        Rectangle rect = graph.parseGeoInfo(guiinfo);
        if (rect!=null) {
            this.x = rect.x;
            this.y = rect.y;
            this.w = rect.width;
            this.h = rect.height;
        } else {
            this.x = this.y = 10;
            this.w = this.h = 24;
        }
    }

    public void save_temp_vars() {
        setAttribute(graph.geo_attribute, graph.formatGeoInfo(x,y,w,h));
    }

    public String getAttribute(String attrname) {
        return vo.getAttribute(attrname);
    }

    public void setAttribute(String attrname, String v) {
        vo.setAttribute(attrname, v);
    }

    public List<AttributeVO> getAttributes() {
        return vo.getAttributes();
    }

    public Graph getGraph() {
    	return graph;
    }

	@Override
	public Long getId() {
		return null;	// not used
	}

	@Override
	public String getName() {
		return vo.getLogicalId();
	}

	@Override
	public void setName(String value) {
		vo.setLogicalId(value);
	}

	@Override
	public String getDescription() {
		return null;	// not used
	}

	@Override
	public void setDescription(String value) {
		// not used
	}

	@Override
	public int getSLA() {
		return 0;		// not used
	}

	@Override
	public void setSLA(int value) {
		// not used
	}

	@Override
	public void setText(String text) {
		vo.setContent(text);
		textarea.setText(text);
	}

	public void setSize(int w, int h) {
        this.w = w+2;
        this.h = h+2;
        textarea.setSize(this.w, this.h);
        textarea.setBounds(x, y, this.w, this.h);
        textarea.repaint();
	}

    public boolean adjustSize() {
        Dimension s1 = textarea.getSize();
        Dimension s2 = getTextSize();
        if (s1.width!= s2.width||s1.height!=s2.height) {
            s2.width += 3; // (LabelEditor.EDITOR_R_MARGIN)
            int width = s2.width;
            int height = s2.height;
            if (width<80) width = 80;
            if (height<20) height = 20;
            setSize(width, height);
            return true;
        } else return false;
    }

    private Dimension getTextSize() {
        Dimension d = new Dimension(0,0);
        try {
            int n = textarea.getLineCount();
            FontMetrics fm = textarea.getFontMetrics(textarea.getFont());
            d.height = fm.getHeight()*n;
            for (int i=0; i<n; i++) {
                int start = textarea.getLineStartOffset(i);
                int end = textarea.getLineEndOffset(i);
                String v = textarea.getText(start, end-start);
                int w = fm.stringWidth(v);
                if (w>d.width) d.width = w;
            }
        } catch (BadLocationException e) {
            textarea.getSize(d);
        }
        return d;
    }

}
