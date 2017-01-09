/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.runtime;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.TreePath;

import com.centurylink.mdw.designer.MainFrame;
import com.centurylink.mdw.designer.display.Graph;
import com.centurylink.mdw.designer.pages.DesignerPage;
import com.centurylink.mdw.designer.utils.Constants;
import com.centurylink.mdw.model.value.process.ProcessInstanceVO;

public class ProcessInstancePage extends DesignerPage
implements ActionListener, MouseListener
{
	private static final String ACTION_ZOOM = "ZOOM";

	private Graph process;
	private ProcessInstanceVO processInstance;
	public RunTimeDesignerCanvas canvas;
	private JButton backButton;
	private JTree treeTable;
	private JComboBox zoomWidget;
	private DesignerPage pageFrom;

	/**
	 * Public instantiation mechanism for Eclipse designer.
	 */
	public static ProcessInstancePage newPage(MainFrame frame) {
		return new ProcessInstancePage(frame);
	}

	public ProcessInstancePage(MainFrame frame){
		super(frame);
		pageFrom = null;
		canvas = new RunTimeDesignerCanvas(this);
		JScrollPane canvasScrollpane = new JScrollPane(canvas);

		//make the tree
		ProcessInstanceTreeModel treeTableModel = new ProcessInstanceTreeModel();
		treeTable = new JTree(treeTableModel);
		//        treeTable.setBackground(this.getBackground());
		// for unknown reason, above is not working - must use frame background
		treeTable.setBackground(frame.getBackground());
		treeTable.addMouseListener(this);
		JScrollPane treeScrollPane = new JScrollPane(treeTable);
		treeScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		treeScrollPane.setPreferredSize(new Dimension(200,190));

		JScrollPane nodeScrollpane = new JScrollPane(new ProcessStatusPane(this));
		nodeScrollpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		JSplitPane eastPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,treeScrollPane,nodeScrollpane);
		eastPane.setSize(getSize());
		eastPane.setOpaque(false);
		eastPane.setOneTouchExpandable(true);
		eastPane.setResizeWeight(0.01);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				eastPane,
				canvasScrollpane);
		splitPane.setSize(getSize());
		splitPane.setOpaque(false);
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(0.2);
		add(splitPane,BorderLayout.CENTER);
	}

	public void createMenuBar() {
		menubar = new JMenuBar();
		JMenu menu1 = new JMenu("File");
		menubar.add(menu1);
		if (!MainFrame.PROCESS_INSTANCE.equals(frame.getStartPage())) {
			createMenuItem(menu1, "Process List", Constants.ACTION_PROCLIST, this);
		}
		createMenuItem(menu1, "Close", Constants.ACTION_LOGOUT, this);
		createMenuItem(menu1, "Print", Constants.ACTION_PRINT, this);
	}

	public void createToolBar() {
		toolbar = new JToolBar();
		add(toolbar, BorderLayout.NORTH);
		if (frame.getStartPage()!=null) {
			backButton = createToolButton("back","back.gif", "Back to last page", Constants.BACK, this);
			createToolButton("Save", "save.gif", "Save ProcessInstance as JPEG", Constants.ACTION_SAVE, this);
			createToolButton("Variables", "variable.jpg", "variables", Constants.ACTION_DOCVIEW, this);
		} else {
			backButton = createToolButton("back","back.gif", "Back to last page", Constants.BACK, this);
			createToolButton("Process Definition", "process.jpg", "process definition", Constants.ACTION_FLOWVIEW, this);
			if (!frame.dao.noDatabase())
	            createToolButton("ProcessInstance","table24.gif",
	            		"All Process Instance for this process",Constants.VIEW_ALL_PROCESS_INSTANCE,this);
			createToolButton("Save", "save.gif", "Save ProcessInstance as JPEG", Constants.ACTION_SAVE, this);
			createToolButton("Variables", "variable.jpg", "variables", Constants.ACTION_DOCVIEW, this);
			//	    createToolButton("Task Instances", "task.jpg", "Task Instance", Constants.VIEW_ALL_TASK_INSTANCE, this);
			createToolButton("Send message", "start.gif", "Send message", Constants.ACTION_START, this);
			createToolButton("Remove", "delete.gif", "remove instance from menu", Constants.ACTION_DELETE, this);

			//AK..added 02/25/2011
			createToolButton("Logout", "logout.gif", "Log out", Constants.ACTION_LOGOUT, this);

			String[] actions = {
					Constants.ACTION_PROCLIST,
					Constants.ACTION_PACKAGE,
					ACTION_SCRIPT,
					ACTION_TESTING
			};
			addCommonToolButtons(actions);
		}
		zoomWidget = createToolDropdown(Graph.zoomLevelNames, ACTION_ZOOM, this, 3, 60);

	}


	public void actionPerformed(ActionEvent event)
	{
		Object source = event.getSource();
		String cmd;
		if (source instanceof AbstractButton)
			cmd = ((AbstractButton)source).getActionCommand();
		else if (source instanceof JComboBox)
			cmd = ((JComboBox)source).getActionCommand();
		else return;
		if(cmd.equals(Constants.ACTION_SAVE)){
			try {
				FileDialog fd = new FileDialog(frame, "Save ProcessInstance as JPEG", FileDialog.SAVE);
				fd.setFile(processInstance.getProcessName() + processInstance.getId()+".jpeg");
				fd.setVisible(true);
				String name = fd.getDirectory()+fd.getFile();
				if(!name.equalsIgnoreCase("nullnull")){
					BufferedImage image = new BufferedImage(canvas.getWidth(), canvas.getHeight(),
							BufferedImage.TYPE_INT_RGB);
					Graphics2D g2 = image.createGraphics();
					canvas.paint(g2);
					g2.dispose();
					ImageIO.write(image, "jpeg", new File(name));
					image = null;
					Runtime r = Runtime.getRuntime();
					r.gc();
				}
			} catch (IOException e) {
				System.err.println(e);
			}
		} else if (cmd.equals(Constants.BACK)) {
			if (pageFrom!=null) frame.setPage(pageFrom);
		} else if (cmd.equals(ACTION_ZOOM)) {
			int zoomLevel = Graph.zoomLevels[zoomWidget.getSelectedIndex()];
			canvas.zoom(process, zoomLevel);
		} else {
			super.actionPerformed(event);
		}
	}

	/**
	 * @return Returns the process.
	 */
	public Graph getProcess() {
		return process;
	}

	public void setProcess(Graph process) {
		this.process = process;
	}

	public void setData(ProcessInstanceTreeModel model, DesignerPage pageFrom) {
		this.process = model.getCurrentProcess().getGraph();
		this.processInstance = process.getInstance();
		if (pageFrom == null) {
			this.pageFrom = null;
			backButton.setEnabled(false);
		} else if (!(pageFrom instanceof ProcessInstancePage)) {
			this.pageFrom = pageFrom;
			backButton.setEnabled(true);
		}
		treeTable.setModel(model);
		treeTable.updateUI();
		TreePath path = model.getCurrentProcessPath();
		if (path!=null) treeTable.setSelectionPath(path);
		Rectangle aRect = new Rectangle(0,0,64,64);
		canvas.scrollRectToVisible(aRect);

		Dimension size = process.getGraphSize();
		size.width = (size.width+40)*process.zoom/100;
		size.height = (size.height+40)*process.zoom/100;
		canvas.setPreferredSize(size);
		int k = 6;
		for (int i=0; i<Graph.zoomLevels.length; i++) {
			if (process.zoom == Graph.zoomLevels[i]) {
				k = i;
				break;
			}
		}
		zoomWidget.setSelectedIndex(k);
	}

	 /**
	  * @return Returns the process.
	  */
	 public ProcessInstanceVO getProcessInstance() {
		 return processInstance;
	 }

	 public String getTitle() {
		 return frame.getDesignerTitle() +" - " +
		 processInstance.getProcessName() + " instance " + processInstance.getId()
		 + " [" + processInstance.getMasterRequestId() + "]";
	 }

	 public void mouseEntered(MouseEvent e) {
	 }

	 public void mouseExited(MouseEvent e) {
	 }

	 public void mousePressed(MouseEvent e) {
	 }

	 public void mouseReleased(MouseEvent e) {
	 }

     public void mouseClicked(MouseEvent e) {
     }

	 public void setZoomLevel(int zoomLevel) {
		 canvas.zoom(process, zoomLevel);
	 }

	 public JTree getInstanceTree() {
		 return treeTable;
	 }

}
