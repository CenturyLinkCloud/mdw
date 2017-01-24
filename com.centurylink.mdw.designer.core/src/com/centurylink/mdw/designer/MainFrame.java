/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.WindowConstants;

import com.centurylink.mdw.common.cache.impl.VariableTypeCache;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.timer.ProgressMonitor;
import com.centurylink.mdw.designer.display.DesignerDataModel;
import com.centurylink.mdw.designer.icons.IconFactory;
import com.centurylink.mdw.designer.pages.DesignerPage;
import com.centurylink.mdw.designer.utils.CustomOptionPane;
import com.centurylink.mdw.designer.utils.Server;

public class MainFrame extends JFrame implements WindowListener {

	// start page names
	public static final String PROCESS_LIST = "PROCESS_LIST";
	public static final String PROCESS_INSTANCE = "PROCESS_INSTANCE";
	public static final String MILESTONE_INSTANCE = "MILESTONE_INSTANCE";
	public static final String FORM_MAIN = "FORM_MAIN";

	private DesignerPage current_page;

	private DesignerDataModel model;
	public DesignerDataAccess dao = null;
	private String title_base = "MDW Designer";
	private PrintStream log;
	private SimpleDateFormat df;
	public String errmsg;
	public Color color_canvas, color_readonly_canvas;
	public Font font_textfield;
	private String cuid;
	private CustomOptionPane optionPane;
	private String start_page;
	private HashMap<Class<?>,DesignerPage> pages;
	private boolean isAdditonalMainFrame;

	// the following are shared among all windows (MainFrame instances)
	private static List<Server> server_list;

	private IconFactory iconFactory;
	public IconFactory getIconFactory() { return iconFactory; }

    public MainFrame(String title) {
 		super(title);

 		this.iconFactory = new IconFactory();
		Color background = new Color(220,230,240);
		color_canvas = Color.white;
		color_readonly_canvas = new Color(235,240,245);
		setBackground(background);  // not really needed here, but other areas
		// need to access this color
		// hack..after the introduction of HTTP Servlet shall move this out
		System.setSecurityManager(null);
		//        SecurityManager securityMgr = System.getSecurityManager();
		//        System.out.println("SecurityManager: " + (securityMgr==null?"None":securityMgr.getClass().getName()));
		setSize(900, 720);
		addWindowListener(this);
		current_page = null;
		ImageIcon icon = new ImageIcon(this.getClass().getClassLoader().getResource("images/designer.gif"));
		setIconImage(icon.getImage());
		rootPane.setDoubleBuffered(false);
		font_textfield = new Font("courier", Font.PLAIN, 12);
		dao = null;
		log = System.out;
		df = new SimpleDateFormat("HH:mm:ss");
		setDefaultLookAndFeelDecorated(true);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		errmsg = null;
		start_page = null;
	}

	public void setPage(DesignerPage page) {
		Container content_pane = getContentPane();
		JMenuBar menubar = null;

		if (current_page!=null) {
			if (!current_page.canleave()) return;
			content_pane.remove(current_page);
		}
		Runtime.getRuntime().gc();
		content_pane.add(page);
		if (start_page==null) {
			menubar = page.getMenuBar(this.isAdditonalMainFrame);
			if (menubar!=null) setJMenuBar(menubar);
			else {
				menubar = this.getJMenuBar();
				if (menubar!=null) setJMenuBar(null);
				// this is for AK's change to remove menu bar for log in page
			}
		}
		page.setVisible(true);
		this.setTitle(page.getTitle());
		current_page = page;
		repaint();
		setVisible(true);
		if (errmsg!=null) {
			optionPane.showError(this, errmsg);
			errmsg = null;
		}
	}

	public void windowActivated(WindowEvent arg0) {
	}

	public void windowClosed(WindowEvent arg0) {
	}

	public DesignerDataModel getDataModel() {
		return model;
	}

	public void setOptionPane(CustomOptionPane pane) {
		optionPane = pane;
	}

	public CustomOptionPane getOptionPane() {
		return optionPane;
	}

	public String getCuid() {
		return cuid;
	}

	public void windowClosing(WindowEvent arg0) {
	}

	public void windowDeactivated(WindowEvent arg0) {
	}

	public void windowDeiconified(WindowEvent arg0) {
	}

	public void windowIconified(WindowEvent arg0) {
	}

	public void windowOpened(WindowEvent arg0) {
	}

	public void setNewServer() {
		if (dao==null) return;
		dao.renewServer();
	}

	public void log(String msg) {
		String line = "[" + df.format(new Date()) + "][designer]" + msg;
		log.println(line);
	}

	public List<Server> getServerList() {
		return server_list;
	}

	public String getDesignerTitle(){
		return title_base;
	}

    /**
     * Uses 50% of progressMonitor.
     */
    public void startSession(String cuid, Server server, ProgressMonitor progressMonitor,
            Map<String, String> connectParams, boolean oldNamespaces, boolean remoteRetrieve, int schemaVersion)
    throws NamingException, RemoteException, DataAccessException {
	    this.cuid = cuid;
		dao = new DesignerDataAccess(server, server_list, this.cuid, connectParams, oldNamespaces, remoteRetrieve, schemaVersion);
		model = new DesignerDataModel();
		pages = new HashMap<Class<?>,DesignerPage>();
		iconFactory.setDesignerDataAccess(dao);
		model.setDatabaseSchemaVersion(dao.getDatabaseSchemaVersion());
		if (progressMonitor != null) {
		    progressMonitor.progress(5);
		    progressMonitor.subTask("Loading reference information");
		}
		model.reloadVariableTypes(dao);
		model.reloadRoleNames(dao);
		model.reloadTaskCategories(dao);
		// privileges must be loaded before loading groups/process/resource
		model.reloadPriviledges(dao, cuid);
		model.reloadGroups(dao);
		if (progressMonitor != null)
		    progressMonitor.progress(5);
		// resources must be loaded before processes
		if (progressMonitor != null)
		    progressMonitor.subTask("Loading workflow assets");
		model.reloadRuleSets(dao);
		if (progressMonitor != null)
		    progressMonitor.progress(15);
		// activity implementors must be loaded before processes
		if (progressMonitor != null)
		    progressMonitor.subTask("Loading activity implementors");
		model.reloadActivityImplementors(dao);
		if (progressMonitor != null)
		    progressMonitor.progress(5);
		if (progressMonitor != null)
		    progressMonitor.subTask("Loading event handlers");
		model.reloadExternalEvents(dao);
		if (progressMonitor != null)
		    progressMonitor.progress(5);
        if (progressMonitor != null)
            progressMonitor.subTask("Loading task templates");
        model.reloadTaskTemplates(dao);
        if (progressMonitor != null)
            progressMonitor.progress(5);
		if (start_page==null || PROCESS_LIST.equals(start_page)) {
			if (progressMonitor != null)
			    progressMonitor.subTask("Loading process list");
			model.reloadProcesses(dao);	// must be after loading rule sets
			if (progressMonitor != null)
			    progressMonitor.progress(15);
		}
		VariableTypeCache.loadCache(model.getVariableTypes());
		title_base = "MDW Designer (" + dao.getSessionIdentity() + ")";
	}

	public boolean isInEclipse() {
		return !this.isVisible() && start_page==null && !isAdditonalMainFrame;
	}

	public DesignerPage getPage(Class<?> cls) {
		return getPage(cls, false);
	}

	public DesignerPage getPage(Class<?> cls, boolean forceNew) {
		try {
			DesignerPage page = pages.get(cls);
			if (page==null || forceNew) {
				page = (DesignerPage)cls.getConstructor(MainFrame.class).newInstance(this);
				pages.put(cls, page);
			}
			return page;
		} catch (Exception e) {
			e.printStackTrace();
			getOptionPane().showError(this, "Failed to create page " + cls.getName());
			return current_page;
		}
	}

	public String getStartPage() {
		return start_page;
	}

	public void setPage(Class<?> cls) {
		DesignerPage page = getPage(cls);
		this.setPage(page);
	}

	public void removePage(Class<?> cls) {
		pages.remove(cls);
	}

	public DesignerPage getCurrentPage() {
		return current_page;
	}
}
