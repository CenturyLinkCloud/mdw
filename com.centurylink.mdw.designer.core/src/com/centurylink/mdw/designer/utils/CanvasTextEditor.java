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

import java.awt.Point;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.centurylink.mdw.designer.display.EditableCanvasText;

/**
 */
public class CanvasTextEditor extends JDialog
{
    private static final long serialVersionUID = 1L;

	private JTextArea textarea;
	private EditableCanvasText editingObject;

	public CanvasTextEditor(JFrame owner, EditableCanvasText editingObject,
			int x, int y, int w, int h, String content, JPanel canvas) {
		super(owner);
		this.setUndecorated(true);
		this.editingObject = editingObject;
		JPanel panel = new JPanel(null);
		getContentPane().add(panel);
		int margin = 4;
		Point point = canvas.getLocationOnScreen();
		setLocation(point.x + x - margin, point.y + y - margin);
		setSize(w + margin*2, h + margin*2);
		textarea = new JTextArea();
		textarea.setBounds(margin, margin, w, h);
        textarea.setWrapStyleWord(true);
        textarea.setLineWrap(true);
        textarea.setEditable(true);
		textarea.setText(content);
		panel.add(textarea);
	}

	@Override
	public void setVisible(boolean b) {
		if (!b) editingObject.setText(textarea.getText());
		super.setVisible(b);
		if (!b) super.dispose();
	}

}
