/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.http;

import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.form.FormActionParser;
import com.centurylink.mdw.common.utilities.form.ResourceLoader;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.common.utilities.property.PropertyManager;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.variable.VariableInstanceVO;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.FormatDom;
import com.qwest.mbeng.Logger;
import com.qwest.mbeng.MbengDocument;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;
import com.qwest.mbeng.MbengRuleSet;
import com.qwest.mbeng.MbengRuntime;

public class HtmlGenerator {

	protected static boolean useChromeMenu = false;

    private static final int CONTEXT_NORMAL = 0;
    private static final int CONTEXT_DIRECTIVE = 5;
    private static final int CONTEXT_FOREACH = 3;
    private static final int CONTEXT_IF = 1;
    private static final int CONTEXT_ELSE = 2;
    private static final int CONTEXT_END = 4;
    private static final int CONTEXT_FORINDEX = 6;
    private static final int CONTEXT_INCLUDE = 7;

	private static final int input_text_xhtml_adjust = -4;

	protected static StandardLogger logger = LoggerUtil.getStandardLogger();

	// common
	protected String assignStatus;
	protected FormDataDocument datadoc;
	private String servletUrl;
	// for general tasks
	protected String formName;	// form or dialog form name; null for classic tasks
	protected List<String> widgetInitialization;
	private Map<String,String> indexVars;
	protected List<MbengNode> menus;
	private boolean isDialog;
	protected Map<String,String> dialogs;	// dialog names. The value is either "NOTTABLE" or tableId (for dynamic table row editing dialog)
	private Map<String,MbengNode> tables;	// map from ID to table definition node, for use of generating table row edit dialog
	protected String summaryTaskManagerUrl;
	// for classic and autoform tasks
	protected List<VariableInstanceVO> variables;

	/*
	 * TODO potential conversion to native jQuery client side:
	 *   - set table data from formdata
	 *   - get DROPDOWN, LIST and LISTPICKER choices
	 * TODO misc
	 *   - remove usage of "name" attributes for LISTPICKER
	 *   - exclude "name" usage of RADIOBUTTONS from update_model_server()
	 */

    /**
     * Generate from html (rather than from pagelet forms)
     *   - isDialog is not used
     *   - isTask is not used
     */
    public String generate(String formname, String htmldoc, FormDataDocument datadoc,
    		ResourceLoader loader, String summaryTaskManagerUrl) {
    	this.formName = formname;
    	this.assignStatus = datadoc.getAssignStatus();
       	this.datadoc = datadoc;
       	this.isDialog = false;
       	this.summaryTaskManagerUrl = summaryTaskManagerUrl;
        return generateSub(htmldoc, null, loader);
    }

	/**
	 * Generate general task instance page from form (not from html)
	 *  - for both useJQuery or not
	 *  - isDialog only for non-JQuery
	 *  - isTask is not used here
	 */
    public String generate(String formName, MbengDocument doc, FormDataDocument datadoc,
    		ResourceLoader loader, String summaryTaskManagerUrl) {
    	this.formName = formName;
    	this.datadoc = datadoc;
    	this.menus = new ArrayList<MbengNode>();
    	this.isDialog = false;
    	this.assignStatus = datadoc.getAssignStatus();
       	this.summaryTaskManagerUrl = summaryTaskManagerUrl;
        this.tables = new HashMap<String,MbengNode>();
        String templateName = doc.getRootNode().getAttribute(FormConstants.FORMATTR_TEMPLATE);
        String template = this.getTemplate(loader, templateName);
        return generateSub(template, doc, loader);
    }

    /**
     * Generate classic and autoform process-variable based tasks
     * @param datadoc
     * @param task
     * @param templateName
     * @param loader
     * @return
     */
    public String generate(FormDataDocument datadoc, List<VariableInstanceVO> variables,
    		String templateName, ResourceLoader loader, String summaryTaskManagerUrl) {
    	this.formName = null;
    	this.variables = variables;
    	this.datadoc = datadoc;
    	this.assignStatus = datadoc.getAssignStatus();
       	this.summaryTaskManagerUrl = summaryTaskManagerUrl;
		String template = this.getTemplate(loader, templateName);
        return generateSub(template, null, loader);
    }

	/**
	 * Generate error page
	 * @param e
	 * @return
	 */
    public String generate(Exception e) {
		StringBuffer html = new StringBuffer();
		html.append("<html><head><title>Error</title></head><body><h1>Error</h1><pre>\n");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(out);
		e.printStackTrace(ps);
		ps.close();
		html.append(StringHelper.escapeXml(out.toString()));
		html.append("</pre></body></html>\n");
		return html.toString();
    }

    protected String generateSub(String template, MbengDocument formdoc, ResourceLoader loader) {
    	widgetInitialization = new ArrayList<String>();
    	indexVars = new HashMap<String,String>();
    	dialogs = new HashMap<String,String>();
        StringBuffer sb = new StringBuffer();
        List<Block> blocks = this.getBlocks(template);
        this.generateSub(template, blocks, 0, blocks.size(), sb,
        		isClassicTask()?null:datadoc.getRootNode(), formdoc, loader);
   		return sb.toString();
    }

    private void generateSub(String template, List<Block> blocks, int start_block, int end_block,
    		StringBuffer sb, MbengNode thisNode, MbengDocument formdoc, ResourceLoader loader) {
    	for (int j=start_block; j<end_block; j++) {
    		Block block = blocks.get(j);
    		if (block.context==CONTEXT_FOREACH){
    			int k;
    			for (k=j+1; k<end_block; k++) {
    				if (blocks.get(k).context==CONTEXT_END && blocks.get(k).level==block.level) break;
    			}
    			String thatPath = template.substring(block.start+8,block.end).trim();
    			if (thatPath.endsWith(".*")) {
    				int l = thatPath.length();
    				MbengNode thatNode;
	    			if (thatPath.startsWith("this.")) {
	    				if (l==6) thatNode = thisNode;
	    				else thatNode = datadoc.getNode(thisNode, thatPath.substring(5,l-2));
	    			} else thatNode = datadoc.getNode(thatPath.substring(0,l-2));
	    			if (thatNode!=null) {
	    				MbengNode child;
	    				for (child=thatNode.getFirstChild();child!=null;child=child.getNextSibling()) {
	    					generateSub(template, blocks, j+1, k, sb, child, formdoc, loader);
	    				}
	    			}
    			} else {
	    			MbengNode thatNode;
	    			if (thatPath.startsWith("this."))
	    				thatNode = datadoc.getNode(thisNode, thatPath.substring(5));
	    			else thatNode = datadoc.getNode(thatPath);
	    			while (thatNode!=null) {
	    				generateSub(template, blocks, j+1, k, sb, thatNode, formdoc, loader);
	    				MbengNode nextNode = thatNode.getNextSibling();
	    				while (nextNode!=null && !nextNode.getName().equals(thatNode.getName())) {
	    					nextNode = nextNode.getNextSibling();
	    				}
	    				thatNode = nextNode;
	    			}
    			}
    			j = k;
       		} else if (block.context==CONTEXT_FORINDEX) {
    			int k;
    			for (k=j+1; k<end_block; k++) {
    				if (blocks.get(k).context==CONTEXT_END && blocks.get(k).level==block.level) break;
    			}
    			String[] args = template.substring(block.start,block.end).split("[ \\t]+");
    			if (args.length>=3) {
    				String indexVar = args[1];
    				String thatPath = args[2].trim();
    				MbengNode thatNode;
	    			if (thatPath.startsWith("this."))
	    				thatNode = datadoc.getNode(thisNode, thatPath.substring(5));
	    			else thatNode = datadoc.getNode(thatPath);
	    			if (thatNode!=null) {
	    				MbengNode child;
	    				int index;
	    				for (child=thatNode.getFirstChild(),index=0;
	    					child!=null;
	    					child=child.getNextSibling(),index++) {
	    					indexVars.put(indexVar, Integer.toString(index));
	    					generateSub(template, blocks, j+1, k, sb, child, formdoc, loader);
	    				}
	    			}
    			}
    			j = k;
    		} else if (block.context==CONTEXT_IF) {
    			int k, e=-1;
    			for (k=j+1; k<end_block; k++) {
    				if (e<0 && blocks.get(k).context==CONTEXT_ELSE && blocks.get(k).level==block.level) e=k;
    				if (blocks.get(k).context==CONTEXT_END && blocks.get(k).level==block.level) break;
    			}
    			String condition = template.substring(block.start+3,block.end).trim();
    			boolean conditionIsTrue = evaluateCondition(condition, condition, thisNode);
    			if (conditionIsTrue) {
    				if (e>0) generateSub(template, blocks, j+1, e, sb, thisNode, formdoc, loader);
    				else generateSub(template, blocks, j+1, k, sb, thisNode, formdoc, loader);
    			} else {
    				if (e>0) generateSub(template, blocks, e+1, k, sb, thisNode, formdoc, loader);
    			}
    			j = k;
    		} else if (block.context==CONTEXT_INCLUDE) {
    			String subtemplateName = template.substring(block.start+8,block.end).trim();
    			String subtemplate = this.getTemplate(loader, subtemplateName);
    			List<Block> subblocks = this.getBlocks(subtemplate);
    	        this.generateSub(subtemplate, subblocks, 0, subblocks.size(), sb,
    	        		thisNode, formdoc, loader);
    		} else {
    			// generate code with place holders
    			for (int i=block.start; i<block.end; i++) {
    				char ch0 = template.charAt(i);
    				if (ch0=='#' && i<block.end-2 && template.charAt(i+1)=='{') {
		        		String placeHolder = null;
		        		char ch1;
		        		for (int k=i+2; k<block.end; k++) {
		        			ch1 = template.charAt(k);
		        			if (ch1=='}') {
		        				placeHolder = template.substring(i+2,k);
		        				i = k;
		        				break;
		        			} else if (ch1=='\n' || ch1=='\r') {
		        				break;
		        			}
		        		}
		        		if (placeHolder!=null) {
		        			fillPlaceHolder(sb, placeHolder, thisNode, formdoc, loader);
		        		} else sb.append(ch0);
    				} else sb.append(ch0);
    			}
        	}
        }
    }

    public void setServletUrl(String v) {
    	this.servletUrl = v;
    }

    class Block {
    	int context;
    	int level;
    	int start, end;
    }

    private List<Block> getBlocks(String template) {
    	boolean lastCharIsNewLine = true;
    	int n = template.length();
    	List<Block> blocks = new ArrayList<Block>();
    	Block block = new Block();
    	blocks.add(block);
    	block.start = 0;
    	block.context = CONTEXT_NORMAL;
        for (int i=0; i<n; i++) {
        	char ch0 = template.charAt(i);
        	if (ch0=='\r') continue;
        	if (lastCharIsNewLine) {
        		if (block.context!=CONTEXT_NORMAL) {
        			block.end = i;
        			block = new Block();
        			blocks.add(block);
        			block.start = i;
        			block.context = CONTEXT_NORMAL;
        		}
        		if (ch0=='#') {
        			if (block.start<i) {
        				block.end = i;
        				block = new Block();
        				blocks.add(block);
        				block.start = i;
        			}
    				block.context = CONTEXT_DIRECTIVE;
        		}
        	}
        	lastCharIsNewLine = ch0=='\n';
        }
        block.end = n;
        // determine level of blocks
        int level = 0;
        for (int i=0; i<blocks.size(); i++) {
        	block = blocks.get(i);
        	if (block.context==CONTEXT_DIRECTIVE) {
        		String lineFrom1 = template.substring(block.start+1,block.end);
        		if (lineFrom1.startsWith("end")) block.context = CONTEXT_END;
        		else if (lineFrom1.startsWith("foreach")) block.context = CONTEXT_FOREACH;
        		else if (lineFrom1.startsWith("if")) block.context = CONTEXT_IF;
        		else if (lineFrom1.startsWith("else")) block.context = CONTEXT_ELSE;
        		else if (lineFrom1.startsWith("forindex")) block.context = CONTEXT_FORINDEX;
        		else if (lineFrom1.startsWith("include")) block.context = CONTEXT_INCLUDE;
        	}
        	if (block.context==CONTEXT_END) {
        		block.level = level;
        		if (level>0) level--;
        	} else if (block.context==CONTEXT_IF) {
        		level++;
        		block.level = level;
        	} else if (block.context==CONTEXT_FOREACH) {
        		level++;
        		block.level = level;
        	} else if (block.context==CONTEXT_FORINDEX) {
        		level++;
        		block.level = level;
        	} else if (block.context==CONTEXT_INCLUDE) {
        		block.level = level+1;
        	} else {
        		block.level = level;	// NORMAL and ELSE
        	}
        }
        return blocks;
    }

    protected String getTemplate(ResourceLoader loader, String templateName) {
    	String template = null;
        if (templateName!=null&&templateName.length()>0) {
        	try {
        		RuleSetVO ruleset = loader.getResource(templateName, "HTML", 0);
				if (ruleset==null) throw new DataAccessException("template does not exist: " + templateName);
	        	template = ruleset.getRuleSet();
			} catch (DataAccessException e) {
				logger.exception("w.", "Failed to load template", e);
			}
        }
        if (template==null) {
    		template = "<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Transitional//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd'>\n"
        	    + "<html>\n<head>\n#{MDW.GENERATED_HEAD}\n</head>\n"
        		+ "<body bgcolor='#E0F0F0'>\n#{MDW.GENERATED_BODY}\n</body>\n</html>\n";
        }
        return template;
    }

    protected void fillPlaceHolder(StringBuffer sb, String placeHolder, MbengNode thisNode,
    		MbengDocument formdoc, ResourceLoader loader) {
    	if (placeHolder.startsWith("MDW.")) {
    		if (placeHolder.equals("MDW.GENERATED_HEAD")) {
    			if (isClassicTask()) generateHtmlHeadClassic(sb);
    			else generateHtmlHeadGeneral(sb, formdoc);
    		} else if (placeHolder.equals("MDW.GENERATED_BODY")) {
    			if (isClassicTask()) generateHtmlBodyClassic(sb);
    			else generateHtmlBodyGeneral(sb, formdoc, loader);
    		} else if (placeHolder.equals("MDW.VERSION")) {
    			sb.append(ApplicationContext.getMdwVersion());
    		} else if (placeHolder.equals("MDW.APPLICATION_VERSION")) {
    			sb.append(ApplicationContext.getApplicationVersion());
    		} else if (placeHolder.equals("MDW.SERVLET_URL")) {
    			sb.append(servletUrl);
    		} else if (placeHolder.equals("MDW.STANDARD_INCLUDES")) {
    			generateStandardIncludes(sb);	// for HTML page
    		} else if (placeHolder.equals("MDW.FORMDATA")) {
    			generateFormData(sb);		// for HTML page
    		} else if (placeHolder.equals("MDW.TASK_COMMON_DATA")) {
    			generateTaskCommonData(sb);
    		} else if (placeHolder.equals("MDW.TASK_COMMON_BUTTONS")) {
    			generateInflowTaskCommonButtons(sb);
    		} else {
    	    	String prop = loader.getProperty(getPackageName(),placeHolder.substring(4));
    	    	if (prop!=null) sb.append(prop);
    		}
    	} else if (placeHolder.startsWith("/")) {	// xpath
    		try {
				MbengNode node = datadoc.xpathFindNode(placeHolder);
				String value = node==null?null:node.getValue();
				if (value!=null) sb.append(StringHelper.escapeXml(value));
			} catch (MbengException e) {
				sb.append("#{").append(placeHolder).append("}");
			}
    	} else if (placeHolder.startsWith("^")) {	// no escape
    		String value = datadoc.getValue(placeHolder.substring(1));
			if (value!=null) sb.append(value);
    	} else if (placeHolder.startsWith("this.")) {
    		String value = datadoc.getValue(thisNode, placeHolder.substring(5));
			if (value!=null) sb.append(StringHelper.escapeXml(value));
    	} else if (placeHolder.equals("this")) {
    		String value = thisNode.getValue();
			if (value!=null) sb.append(StringHelper.escapeXml(value));
    	} else if (indexVars.get(placeHolder)!=null) {
    		sb.append(indexVars.get(placeHolder));
    	} else {
			String value = datadoc.getValue(placeHolder);
			if (value!=null) sb.append(StringHelper.escapeXml(value));
    	}
    }

    protected void generateStandardIncludes(StringBuffer sb) {
        if (useChromeMenu) {
        	sb.append("<link rel='stylesheet' type='text/css' href='resource/chromestyle.css'/>\n");
        }
        sb.append("<link rel='stylesheet' type='text/css' href='resource/jquery.ui.css'/>\n");
        sb.append("<link rel='stylesheet' type='text/css' href='resource/jquery.alerts.css'/>\n");
        sb.append("<link rel='stylesheet' type='text/css' href='resource/mdwform.css'/>\n");
        sb.append("<script language='javascript' src='resource/jquery.js'></script>\n");
        sb.append("<script language='javascript' src='resource/jquery.ui.js'></script>\n");
        sb.append("<script language='javascript' src='resource/jquery.json.js'></script>\n");
        sb.append("<script language='javascript' src='resource/jquery.alerts.js'></script>\n");
        if (useChromeMenu) {
        	// the menu widget was down loaded from
        	// http://www.dynamicdrive.com/dynamicindex1/chrome/index.htm
        	sb.append("<script language='javascript' src='resource/chrome.js'></script>\n");
        }
        sb.append("<script language='javascript' src='resource/mdwform.js'></script>\n");
    }

    protected void generateHtmlHeadGeneral(StringBuffer sb, MbengDocument formdoc) {
    	String title = datadoc.getMetaValue(FormDataDocument.META_TITLE);
    	if (title==null || title.length()==0) title = "MDW Form";
        sb.append("<title>" + title + "</title>\n");
        // base target must be after title but before others in head
//        if (isDialog) sb.append("<base target='_self'/>\n");
        generateStandardIncludes(sb);
        sb.append("<bgsound id='sound' volume='-800'/>\n");
        sb.append("<script><!--\n");
        generateFormData(sb);
        sb.append("$(document).ready(function (){\n");
        String av = datadoc.getMetaValue(FormDataDocument.META_ACTIVITY_INSTANCE_ID);
        if (av!=null) sb.append("  setReadonlyWhenNotAssigned();\n");
        if (formdoc!=null) sb.append("  widgetInitialization();\n");
        sb.append("  outputData('.dataOut');\n");
        sb.append("  if (formdata.ERROR!=undefined) {\n");
        sb.append("      jAlert(formdata.ERROR.join('<br/>'),'Error');\n");
        sb.append("  } else if (formdata.META.PROMPT!=undefined) {\n");
        sb.append("      jInfo(formdata.META.PROMPT,'Information');\n");
        if (av!=null) {
        	sb.append("  } else if (formdata.META.ENGINE_CALL_STATUS=='WAITING') {\n");
        	sb.append("      formdata.META.ENGINE_CALL_STATUS='';\n");
        	sb.append("      jInfo('Previous call to engine is still not done','Information');\n");
        	sb.append("  } else if (formdata.META.ENGINE_CALL_STATUS=='DONE') {\n");
        	sb.append("      formdata.META.ENGINE_CALL_STATUS='';\n");
        	sb.append("      jInfo('Previous call to engine has completed','Information');\n");
        }
        sb.append("  } else if (formdata.META.INITIALIZATION != undefined) {\n");
        sb.append("      eval(formdata.META.INITIALIZATION);\n");
        sb.append("  }\n");
        sb.append("});\n");
        sb.append("//--></script>\n");
    }

    protected void generateHtmlHeadClassic(StringBuffer sb) {
    	Long taskInstId = datadoc.getTaskInstanceId();
		sb.append("<title>Task " + taskInstId.toString() + "</title>\n");
		generateStandardIncludes(sb);
        sb.append("<script><!--\n");
        generateFormData(sb);
        sb.append("$(document).ready(function (){\n");
        sb.append("  setReadonlyWhenNotAssigned();\n");
        sb.append("  outputData('.dataOut');\n");
        sb.append("  if (formdata.ERROR!=undefined) {\n");
        sb.append("      jAlert(formdata.ERROR.join('<br/>'),'Error');\n");
//        sb.append("  } else if (formdata.META.PROMPT!=undefined) {\n");
//        sb.append("      jInfo(formdata.META.PROMPT,'Information');\n");
        sb.append("  }\n");
        sb.append("});\n");
        sb.append("//--></script>\n");
    }

    protected void generateHtmlBodyGeneral(StringBuffer sb, MbengDocument formdoc, ResourceLoader loader) {
        Rectangle vr = determine_vr(formdoc.getRootNode(), 0, 0, 800, 600);
        sb.append("   <div id='content' style='position:relative;width:" + vr.width + "px;height:"
                + vr.height + "px;' class='form-body'>\n");
        // id=content is needed only by dialog_box.js
        boolean hasForm = sb.indexOf("<form ")>0;
        if (!hasForm) {
        	sb.append("<form id='mdw_form' action='form' method='post'>\n");
        	sb.append("<input type='hidden' id='mdw_formdata' name='mdw_formdata'/>\n");
        }

        final int lx_default = 12;
        create_children(formdoc.getRootNode(), lx_default, sb, -2);
        if (useChromeMenu) {
	        while (menus.size()>0) {
	        	MbengNode menu = menus.remove(0);
	        	if (menu.getName().equals(FormConstants.WIDGET_MENUBAR)) {
	              indent(sb,0).append("<script type='text/javascript'>\n");
	              indent(sb,2).append("cssdropdown.startchrome('"
	            		  + menu.getAttribute(FormConstants.FORMATTR_ID) + "');\n");
	              indent(sb,0).append("</script>\n");
	        	} else create_MENU_common(menu, sb, 0, menus);
	        }
        } else {
        	if (menus.size()>0) {
        		sb.append("<script>\n");
        		for (MbengNode menu : menus) {
        			sb.append("  activateMenu('" + menu.getAttribute(FormConstants.FORMATTR_ID)
        				+ "');\n");
        		}
        		sb.append("</script>\n");
        	}
        }
        // handle loading dialog after call back
        String responseDialog = datadoc.getMetaValue(FormDataDocument.META_DIALOG);
        if (responseDialog!=null) {
        	datadoc.setMetaValue(FormDataDocument.META_DIALOG, null);
        	if (responseDialog.startsWith(FormConstants.TABLE_ROW_DIALOG_PREFIX)) {
        		String tableId = responseDialog.substring(FormConstants.TABLE_ROW_DIALOG_PREFIX.length());
        		dialogs.put(tableId+"_rowdialog", tableId);
        	} else dialogs.put(responseDialog, "NOTTABLE");
        }
        // generate dialog
        for (String dialogName : dialogs.keySet()) {
        	this.generateDialog(dialogName, dialogs.get(dialogName), loader, sb);
        }
        if (!hasForm) sb.append("</form>\n");
        // generate java script initialization
        sb.append("<script><!--\n");
        sb.append("function widgetInitialization() {\n");
        for (String one : widgetInitialization) {
        	sb.append("  ").append(one).append("\n");
        }
        sb.append("}\n");
        sb.append("//--></script>\n");
        sb.append("</div>");
    }

    protected void generateInflowTaskCommonButtons(StringBuffer sb) {
        sb.append("<fieldset>\n");
        sb.append("<legend>Actions</legend>\n");
    	if (assignStatus.equals(FormDataDocument.ASSIGN_STATUS_OPEN)) {
			sb.append("<input type='button' value='Claim' onclick='task_action(\"Claim\")'/>\n");
			sb.append("<input type='button' value='Assign' onclick='task_action(\"Assign\")'/>\n");
		} else if (assignStatus.equals(FormDataDocument.ASSIGN_STATUS_SELF)) {
			String v = datadoc.getMetaValue(FormDataDocument.META_TASK_CUSTOM_ACTIONS);
			String[] custom_actions = v == null ? null : v.split("#");
			if (custom_actions==null) {		// should not occur for newly created task instances
				sb.append("<input type='button' value='Complete' onclick='task_action(\"Complete\")'/>\n");
				sb.append("<input type='button' value='Cancel' onclick='task_action(\"Cancel\")'/>\n");
			} else {
				for (String action : custom_actions) {
					sb.append("<input type='button' value='").append(action)
						.append("' onclick='task_action(\"").append(action).append("\")'/>\n");
				}
			}
			sb.append("<input type='button' value='Save' onclick='task_action(\"Save\")'/>\n");
//			sb.append("<input type='button' value='Abort' onclick='task_action(\"Abort\")'/>\n");
			sb.append("<input type='button' value='Release' onclick='task_action(\"Release\")'/>\n");
			sb.append("<input type='button' value='Forward' onclick='task_action(\"Forward\")'/>\n");
		} else if (!assignStatus.equals(FormDataDocument.ASSIGN_STATUS_CLOSE)) {
			sb.append("<input type='button' value='Grab from " + assignStatus
					+ "' onclick='task_action(\"Claim\")'/>\n");
		} else {
			sb.append("No action can be performed as the task is already closed.\n");
		}
		sb.append("</fieldset>\n");
    }

    protected void generateHtmlBodyClassic(StringBuffer sb) {
    	Long taskInstId = datadoc.getTaskInstanceId();
    	sb.append("<h2>Task " + taskInstId.toString() + " - "
    			+ datadoc.getMetaValue(FormDataDocument.META_TASK_NAME) + "</h2>\n");
        sb.append("<form id='mdw_form' action='task' method='post'>\n");
        sb.append("<input type='hidden' id='mdw_formdata' name='mdw_formdata'/>\n");
        this.generateTaskCommonData(sb);
        sb.append("<fieldset>\n");
        sb.append("<legend>Task Specific Data</legend>\n");
        sb.append("<table border='1'>\n");
		for (VariableInstanceVO var : variables) {
			sb.append("<tr><td>");
			sb.append(var.getVariableReferredName());
			if (var.isEditable() && var.isRequired()) sb.append(" <font color='red'>*</font>");
			sb.append("</td><td>");
			if (var.isEditable()) {
				sb.append("<input type='text' class='dataOut dataIn' data='");
				sb.append(var.getName());
				sb.append("'/>\n");
			} else {
				sb.append("<span class='dataOut' data='");
				sb.append(var.getName());
				sb.append("'></span>\n");
			}
			sb.append("</td>\n");
		}
		sb.append("</table>\n");
		sb.append("</fieldset>\n");
		this.generateInflowTaskCommonButtons(sb);
		sb.append("</form>\n");
    }

    protected void generateTaskCommonData(StringBuffer sb) {
		sb.append("<fieldset>\n");
		sb.append("<legend>Common Task Information</legend>\n");
		sb.append("<table style='width:92%;'>\n");
		sb.append("<tr><td class='form-label'>Status</td><td><span class='dataOut form-input-readonly' data='META.TaskStatus'></span></td>\n");
		sb.append("<td class='form-label'>Process Instance ID</td><td><span class='dataOut form-input-readonly' data='META.PROCESS_INSTANCE_ID'></span></td></tr>\n");
		sb.append("<tr><td class='form-label'>Assignee</td><td><span class='dataOut form-input-readonly' data='META.TaskAssignee'></span></td>\n");
		if (this.isClassicTask()) {
			sb.append("<td class='form-label'>Activity Name</td><td><span class='dataOut form-input-readonly' data='META.ACTIVITY_NAME'></span></td></tr>\n");
		} else {
			sb.append("<td class='form-label'>Activity Instance ID</td><td><span class='dataOut form-input-readonly' data='META.ACTIVITY_INSTANCE_ID'></span></td></tr>\n");
		}
		sb.append("<tr><td class='form-label'>Start Date</td><td><span class='dataOut form-input-readonly' data='META.TaskStartDate'></span></td>\n");
		sb.append("<td class='form-label'>End Date</td><td><span class='dataOut form-input-readonly' data='META.TaskEndDate'></span></td></tr>\n");
		sb.append("<tr><td class='form-label'>Due Date</td><td><span class='dataOut form-input-readonly' data='META.TaskDueDate'></span></td>\n");
		sb.append("<td class='form-label'>Task Template ID</td><td><span class='dataOut form-input-readonly' data='META.NAME'></span></td></tr>\n");
		sb.append("<tr><td class='form-label'>Order ID</td><td><span class='dataOut form-input-readonly' data='META.MASTER_REQUEST_ID'></span></td>\n");
		sb.append("<td class='form-label'>Go to</td><td>");
		String ownerAppl = datadoc.getMetaValue(FormDataDocument.META_TASK_OWNER_APPL);
		if (ownerAppl!=null && ownerAppl.length()>0) {
			if (this.summaryTaskManagerUrl!=null) {
				sb.append("<a href='");
				sb.append(summaryTaskManagerUrl);
				sb.append("/MDWHTTPListener/form?name=html:TaskList'>Task List</a>");
			} else sb.append("<a href='form?name=html:TaskList'>Task List</a>");
		} else {
			if (this.summaryTaskManagerUrl!=null) {
				sb.append("<a href='").append(summaryTaskManagerUrl);
				sb.append("/facelets/tasks/myTasks.jsf'>My Tasks</a>");
				sb.append("&nbsp;&nbsp;<a href='").append(summaryTaskManagerUrl);
				sb.append("/facelets/tasks/workgroupTasks.jsf'>Work Group Tasks</a>");
			} else {
				sb.append("<a href='../facelets/tasks/myTasks.jsf'>My Tasks</a>");
				sb.append("&nbsp;&nbsp;<a href='../facelets/tasks/workgroupTasks.jsf'>Work Group Tasks</a>");
			}
		}
		String mainForm = datadoc.getMetaValue("MainForm");
		if (mainForm==null||mainForm.equals(datadoc.getMetaValue(FormDataDocument.META_FORM))) {
			sb.append("&nbsp;&nbsp;<a href='javascript:formdata.META.MainForm=formdata.META.FORM;perform_action(\"com.centurylink.mdw.listener.formaction.TaskActions?action=showNotes\",false)'>Task Notes</a>");
			if (datadoc.getNode("TaskNotes.ROW")!=null) sb.append("<font color='red'>!</font>");
		} else sb.append("&nbsp;&nbsp;<a href='javascript:perform_action(\"@PAGE?formName=").append(mainForm).append("\",false)'>Task Detail</a>");
		sb.append("</td></tr>\n");
		sb.append("</table>\n");
		sb.append("</fieldset>\n");
		// TODO a. due date editable b. hyperlink for process instance id c. link to show attachment
    }

    protected void generateFormData(StringBuffer sb) {
    	String jsondata;
		try {
			jsondata = datadoc.formatJson();
		} catch (Exception e) {
			logger.severeException("Failed to generate JSON data", e);
			if (formName!=null) {
				jsondata = "{\"ERROR\" : [\"Failed to generate JSON Data\"],\"META\":{\"FORM\":\""
					+ formName + "\"}}";
			} else {
				jsondata = "{\"ERROR\":[\"Failed to generate JSON data\"]}";
			}
		}
		sb.append("//FORMDATA_START\n");	// indicator for ajax call to parse data
    	sb.append("var formdata = ").append(jsondata).append(";\n");
		sb.append("//FORMDATA_END\n");
    }

    protected String getPackageName() {
    	return datadoc.getMetaValue(FormDataDocument.META_PACKAGE_NAME);
    }

    protected StringBuffer indent(StringBuffer sb, int indent) {
        for (int i=0; i<indent; i++) sb.append(' ');
        return sb;
    }

    protected void generateId(StringBuffer sb, int indent, String id, String data, boolean endline) {
    	indent(sb,indent).append("id='").append(id).append("'");
    	if (data!=null) sb.append(" data='").append(data).append("'");
    	if (endline) sb.append("\n");
    }

    protected void generateClass(StringBuffer sb, boolean readonly, String otherClass) {
    	sb.append("class='");
    	if (readonly) {
    		if (isDialog) sb.append("dialogDataOut");
    		else sb.append("dataOut");
    	} else {
    		if (isDialog) sb.append("dialogDataOut dialogDataIn");
    		else sb.append("dataOut dataIn");
    	}
    	if (otherClass!=null) sb.append(" ").append(otherClass);
    	sb.append("'");
    }

    protected Rectangle create_BUTTON(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
    	 Rectangle vr = determine_vr(node, vx, vy, 120, 24);
         String action = node.getAttribute(FormConstants.FORMATTR_ACTION);
         String imagename = node.getAttribute(FormConstants.FORMATTR_IMAGE);
         if (action==null) action = "";
         boolean isClaimButton = action.equals(FormConstants.ACTION_ASSIGN_TASK);
         if (isClaimButton) {
        	 if (assignStatus.equals(FormDataDocument.ASSIGN_STATUS_SELF)) label_text = "Release";
        	 else label_text = "Claim";
         }
         indent(sb,indent).append("<input type='button'\n");
         generateId(sb, indent+4, id, null, true);
         if (imagename!=null && imagename.length()>0) {
        	 indent(sb,indent+4).append("image='" + "/database/image/" + imagename + "'\n");
        	 indent(sb,indent+4).append("alt='" + label_text + "'\n");
         } else {
        	 indent(sb,indent+4).append("value='" + label_text + "'\n");
         }
         String tip = node.getAttribute(FormConstants.FORMATTR_TIP);
         if (tip!=null && tip.length()>0) {
         	indent(sb,indent+4).append("title='" + tip + "'\n");
         }
         if (isClaimButton) {
        	 if (!assignStatus.equals(FormDataDocument.ASSIGN_STATUS_OPEN)) generateReadonly(sb, indent+4, "disabled");
         } else {
        	 if (isReadonly(node,id)) generateReadonly(sb, indent+4, "disabled");
         }
         if (action.length()>0) {
        	 indent(sb,indent+4).append("onclick='");
             constructPerformActionCall(sb, node, action, false, false);
        	 sb.append("'\n");
         }
         indent(sb,indent+4).append("style='" + geo(vr) + "'");
         sb.append("/>").append('\n');
         return vr;
    }

    private void constructPerformActionCall(StringBuffer sb, MbengNode node, String action,
    		boolean secondary, boolean asHref) {
    	String showBusy;
    	String[] prompt = {FormConstants.FORMATTRVALUE_PROMPT_NONE};
    	boolean toValidate;
    	if (!secondary) {
    		showBusy = node.getAttribute(FormConstants.FORMATTR_SHOW_BUSY);
    		if (showBusy==null||showBusy.length()==0) showBusy= "false";
    		String promptString = node.getAttribute(FormConstants.FORMATTR_PROMPT);
    		if (promptString!=null) prompt = promptString.split("\\|");
    		toValidate = "true".equals(node.getAttribute(FormConstants.FORMATTR_VALIDATE));
    	} else {
    		showBusy = "false";
    		toValidate = false;
    	}
		FormActionParser command;
		try {
			command = new FormActionParser(action);
		} catch (Exception e) {
			logger.severeException(e.getMessage(), e);
			return;
		}
    	if (command.getFunctionName().equals("hyperlink")) {
    		if (asHref) sb.append(command.getArgumentEscaped(0));
    		else sb.append("window.location.href=\"").append(command.getArgumentEscaped(0)).append("\"");
    	} else {
    		if (asHref) sb.append("javascript:");
    		if (toValidate) sb.append("if (validate()) ");
    		String funcname = command.getFunctionName();
    		if (funcname.equals("perform_action")) {
    			if (prompt[0].equals(FormConstants.FORMATTRVALUE_PROMPT_CONFIRM)) {
    				sb.append("perform_action_confirm(\"");
    				sb.append(command.getArgumentEscaped(0)).append("\",\"");
    				sb.append(prompt[1]).append("\",").append(showBusy).append(")");
                } else if (prompt[0].equals(FormConstants.FORMATTRVALUE_PROMPT_INPUT)) {
                	sb.append("perform_action_input(\"");
                	sb.append(command.getArgumentEscaped(0)).append("\",\"");
        			sb.append(prompt[1]).append("\",\"<type in>\",").append(showBusy).append(")");
                } else if (prompt[0].equals(FormConstants.FORMATTRVALUE_PROMPT_SELECT)) {
                	sb.append("perform_action_select(\"");
                	sb.append(command.getArgumentEscaped(0)).append("\",\"");
                	sb.append(prompt[1]).append("\",\"");
                	if (prompt[2].startsWith("$$$.")) {
                		// use ajax call
                		sb.append(StringHelper.escapeXml(prompt[2]));
                	} else {
        	        	String[] choices = getChoices(prompt[2]);
        	            sb.append("<option></option>");
        	            if (choices!=null) {
        	                for (int i=0; i<choices.length; i++) {
        	                    sb.append("<option>").append(choices[i]).append("</option>");
        	                }
        	            }
                	}
        			sb.append("\",").append(showBusy).append(")");
                } else {	// assuming FormConstants.FORMATTRVALUE_PROMPT_NONE
                	sb.append("perform_action(\"");
                	sb.append(command.getArgumentEscaped(0)).append("\",").append(showBusy).append(")");
                }
    		} else {
    			if (funcname.equals("dialog_open")) {
    				dialogs.put(command.getArgument(0), "NOTTABLE");
    			} else if (funcname.equals("table_row_view")) {
    				if (command.getArgumentCount()==2) {
    					dialogs.put(command.getArgument(1), "NOTTABLE");
    				} else {
    					String tableId = command.getArgument(0);
    					dialogs.put(tableId+"_rowdialog", tableId);
    				}
    			}
    			sb.append(funcname).append("(");
    			int n = command.getArgumentCount();
    			for (int i=0; i<n; i++) {
    				if (i>0) sb.append(",");
    				sb.append(command.getArgumentQuoted(i));
    			}
    			sb.append(")");
    		}
    	}
    }

    protected Rectangle create_PANEL(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
    	 Rectangle vr = determine_vr(node, vx, vy, 600, 300);
    	 boolean useFieldSet = true;
    	 if (useFieldSet) {
	    	 indent(sb,indent).append("<fieldset class='form-panel'\n");
	    	 // not sure why the fieldset makes it wider than specified - subtract 16 to correct
	         indent(sb,indent+4).append("style='" + geo(vr.x,vr.y,vr.width-16,vr.height) + "'");
	    	 sb.append(">\n");
	    	 if (label_text!=null && label_text.length()>0) {
	    		 indent(sb,indent+2).append("<legend class='form-panel-header'>");
	    		 sb.append(label_text).append("</legend>\n");
	    	 }
	    	 create_children(node, lx_default, sb, indent);
	    	 indent(sb,indent).append("</fieldset>\n");
    	 } else {
	    	 indent(sb,indent).append("<div class='form-panel'\n");
	         indent(sb,indent+4).append("style='" + geo(vr) + "'");
	    	 sb.append(">\n");
	    	 if (label_text!=null && label_text.length()>0) {
	    		 indent(sb,indent+2).append("<div class='form-panel-header' ");
	    		 sb.append("style='position:absolute;left:0px;top:0px;width:100%;height:20px;'>");
	    		 sb.append(label_text).append("</div>\n");
	    	 }
	    	 indent(sb,indent+2).append("<div style='position:absolute;left:0px;top:0px;width:100%;height:"
	    			 + vr.height + "px;'>\n");
	    	 create_children(node, lx_default, sb, indent+2);
	    	 indent(sb,indent+2).append("</div>\n");
	    	 indent(sb,indent).append("</div>\n");
    	 }
         return vr;
    }

    protected Rectangle create_TEXTAREA(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
        String assoc_data = node.getAttribute(FormConstants.FORMATTR_DATA);
        String is_static = node.getAttribute(FormConstants.FORMATTR_IS_STATIC);
        if (assoc_data!=null && assoc_data.length()==0) assoc_data = null;
        String value = node.getAttribute(FormConstants.FORMATTR_AUTOVALUE);
        Rectangle vr = determine_vr(node, vx, vy, 600, 80);
        if (is_static!=null && is_static.equalsIgnoreCase("true")) {
        	indent(sb, indent).append("<span class='form-label' ");
        	generateClass(sb, true, null);
        	sb.append(" ");
            sb.append("style='" + geo(vr) + "'");
        	sb.append(">\n");
        	if (value!=null) sb.append(value);
        	sb.append("</span>\n");
        } else {
        	boolean required = isRequired(node, id);
        	create_label(node, lx_default, vy, label_text, sb, indent, required, id);
        	indent(sb, indent).append("<textarea");
        	boolean readonly = isReadonly(node,id);
        	sb.append(" ");
        	this.generateClass(sb, readonly, null);
    		sb.append("\n");
	        generateId(sb, indent+4, id, assoc_data, true);
	        if (readonly) {
	        	generateReadonly(sb, indent+4, "readonly");
		        indent(sb, indent+4).append("class='form-input-readonly'\n");
	        } else {
	        	indent(sb, indent+4).append("class='form-input'\n");
	        }
	        indent(sb,indent+4).append("style='" + geo(vr) + "'");
	        if (required) sb.append(" required='true'");
	        sb.append(">");
	        if (value!=null) sb.append(value);
	        sb.append("</textarea>\n");
        }
        return vr;
    }

    protected Rectangle create_DATE(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
    	boolean required = isRequired(node, id);
    	boolean readonly = isReadonly(node, id);
    	create_label(node, lx_default, vy, label_text, sb, indent, required, id);
        String assoc_data = node.getAttribute(FormConstants.FORMATTR_DATA);
        if (assoc_data!=null && assoc_data.length()==0) assoc_data = null;
        Rectangle vr = determine_vr(node, vx, vy, 120, 20);
        // need an extra div as rich:calendar does not take style parameter
        indent(sb, indent).append("<input type='text' ");
        generateClass(sb, readonly, readonly?"form-input-readonly":"form-input datepicker");
        sb.append("\n");
        indent(sb,indent+4).append("style='" + geo(vr) + "'");
        String fmt;
        fmt = node.getAttribute(FormConstants.FORMATTR_DATE_PATTERN);
        if (fmt==null || fmt.length()==0) fmt = "yyyy-mm-dd";
        boolean can_type_in;
        if (readonly) {
        	generateReadonly(sb, indent+6, "readonly");
        	can_type_in = false;
        } else {
        	String av = node.getAttribute(FormConstants.FORMATTR_CAN_TYPE_IN);
        	can_type_in = (av!=null && av.equalsIgnoreCase("true"));
        }
        if (can_type_in) {
        	String mask = generateDateMask(fmt);
        	indent(sb,indent+4).append("onkeypress='return masking(this,event,\""
        			+ mask + "\")'\n");

        }
        generateId(sb, indent+6, id, assoc_data, false);
        if (required) sb.append(" required='true'");
        sb.append(">\n");
        if (!readonly) this.widgetInitialization.add("$('#" + id + "').datepicker({'dateFormat':'" +
        		fmt.replaceAll("yyyy","yy") + "',autoSize:false});");
        return vr;
    }

    protected String generateDateMask(String datePattern) {
    	if (datePattern==null || datePattern.length()==0) return "9999-99-99";
    	StringBuffer sb = new StringBuffer(datePattern.length());
    	for (int i=0; i<datePattern.length(); i++) {
    		char ch = datePattern.charAt(i);
    		if (Character.isLetter(ch)) sb.append('9');
    		else sb.append(ch);
    	}
    	return sb.toString();
    }

    protected Rectangle create_DROPDOWN(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
    	boolean required = isRequired(node, id);
    	boolean readonly = isReadonly(node, id);
        create_label(node, lx_default, vy, label_text, sb, indent, required, id);
        Rectangle vr = determine_vr(node, vx, vy, 160, 20);
        String assoc_data = node.getAttribute(FormConstants.FORMATTR_DATA);
        if (assoc_data!=null && assoc_data.length()==0) assoc_data = null;
        String default_value = node.getAttribute(FormConstants.FORMATTR_AUTOVALUE);
        String can_type_value = node.getAttribute(FormConstants.FORMATTR_CAN_TYPE_IN);
        boolean editable = can_type_value!=null && can_type_value.equalsIgnoreCase("true");

        if (!readonly && editable) {
        	indent(sb,indent).append("<input type='text' style='").
        		append(geo(vr.x,vr.y,vr.width-22,vr.height)).append("'\n");
        	generateId(sb, indent+4, id, assoc_data, true);
        	indent(sb,indent+4);
        	generateClass(sb, false, null);
        	sb.append("\n");
        	indent(sb,indent).append("<input type='button' hidefocus='1' value='&#9660;'")
        		.append("style='").append(geo(vr.x+vr.width-22,vr.y,22,23))
        		.append("font-family:helvetica;' onclick='comboShowMenu(\"").append(id)
        		.append("\")'>\n");
        	indent(sb,indent).append("<div id='").append(id)
        		.append("_div' style='position:absolute; display:none; top:").append(vr.y+vr.height)
        		.append("px;left:").append(vr.x).append("px;width:").append(vr.width).append("px;z-index:10000'")
        		.append("  onmouseout='javascript:this.style.display=\"none\";'>\n");
        	indent(sb,indent+2).append("<select size='10' id='").append(id)
        		.append("_sel' style='width:220px; border-style: none'")
        		.append(" onclick='javascript:comboSetValue(\"").append(id)
        		.append("\",this.value);' onkeypress='javascript:comboKeyPress(\"").append(id)
        		.append("\", this.value);'");
            if (required) sb.append(" required='true'");
            sb.append(">\n");
        	create_select_items(node, sb, indent+4, false, null);
  	        indent(sb,indent+2).append("</select>\n");
  	        indent(sb,indent).append("</div>\n");
        } else {
        	indent(sb,indent).append("<select ");
	        generateClass(sb, readonly, null);
        	sb.append("\n");
	        generateId(sb, indent+4, id, assoc_data, true);
	        if (readonly) generateReadonly(sb, indent+4, "disabled");
	        indent(sb,indent+4).append("style='" + geo(vr) + "'");
	        String action = node.getAttribute(FormConstants.FORMATTR_ACTION);
	        if (action!=null && action.length()>0) {
	        	indent(sb,indent+4).append("onchange='");
	        	this.constructPerformActionCall(sb, node, action, true, false);
	        	sb.append("'");
	        }
	        sb.append(">\n");
	        create_select_items(node, sb, indent+2, default_value==null||default_value.length()==0, null);
	        indent(sb,indent).append("</select>\n");
        }
        return vr;
    }

    protected void create_TABLE_datarow(StringBuffer sb, int indent, MbengNode row, int rowIndex,
    				String tableId, List<TableColumn> columns) {
    	indent(sb,indent).append("<tr");
		if (rowIndex==0) sb.append(" id='"+tableId+"_row1'");
		sb.append(" onclick='table_row_select(\"" + tableId + "\","+rowIndex+")'>\n");
		int n = columns.size();
    	for (int j=0; j<n; j++) {
    		TableColumn column = columns.get(j);
    		MbengNode cell = row.findChild(column.name);
    		indent(sb,indent+2).append("<td class='form-table-td'>");
    		sb.append("<div style='white-space:nowrap;' data='").append(column.name).append("'>");
    		if (cell!=null) {
    			if (column.action!=null) {
        			String action = column.action;
    				sb.append("<a href='");
    				if (action.indexOf('?')>0) action = action + "&";
    				else action = action + "?";
    				action += "table=" + tableId + "&row=" + cell.getValue();
    				this.constructPerformActionCall(sb, null, action, true, true);
    				sb.append("'>").append(cell.getValue()).append("</a>");
    			} else {
    				sb.append(cell.getValue());
    			}
    		}
    		sb.append("</div>");
    		sb.append("</td>\n");
    	}
    	indent(sb,indent).append("</tr>\n");
    }

    protected static class TableColumn {
    	int width;
    	String name;
    	String action;
    	boolean hidden;
    }

    private List<TableColumn> create_TABLE_header(StringBuffer sb, int indent, MbengNode node,
    		String sortOn, boolean descending, String tablepath, String paginator, int pagesize,
    		String tableId, String table_style) {
    	indent(sb,indent).append("<thead>\n");
        indent(sb,indent+2).append("<tr>\n");
        int x = 0;
        String v;
        ArrayList<TableColumn> columns = new ArrayList<TableColumn>();
        boolean sortable;
        for (MbengNode child=node.getFirstChild(); child!=null; child=child.getNextSibling()) {
        	String columnId = child.getAttribute(FormConstants.FORMATTR_ID);
        	boolean hidden = isHidden(child, columnId);
        	if (hidden) continue;
        	TableColumn column = new TableColumn();
        	columns.add(column);
        	String collabel = child.getAttribute(FormConstants.FORMATTR_LABEL);
            column.name = child.getAttribute(FormConstants.FORMATTR_DATA);
        	v = child.getAttribute(FormConstants.FORMATTR_VW);
            column.width = (v!=null)?Integer.parseInt(v):100;
            sortable = "true".equals(child.getAttribute(FormConstants.FORMATTR_SORTABLE));
            indent(sb,indent+4).append("<th class='form-table-th'");
            if (sortable) {
            	sb.append(" onclick='ajax_action(\"");
	            if (paginator!=null) {
	            	sb.append(paginator);
	            } else {	// simple or scrolled style
	                sb.append("com.centurylink.mdw.listener.formaction.TablePaginator");
	            }
	            sb.append("?action=paging&amp;" + FormConstants.URLARG_TABLE + "=" + tablepath);
	            sb.append("&amp;" + FormConstants.URLARG_SORTON + "=");
	            if (sortOn!=null && sortOn.equals(column.name) && !descending) sb.append("-");
	            sb.append(column.name);
	            sb.append("&amp;" + FormConstants.URLARG_META + "=" + tablepath + "_META");
	            if (paginator!=null) {
	            	sb.append("&amp;" + FormConstants.URLARG_PAGESIZE + "=" + pagesize);
	            } else {
	            	sb.append("&amp;" + FormConstants.URLARG_TOPAGE + "=R");
	            }
	            sb.append("\",\"" + tableId + "\");");
	            if (table_style.equals(FormConstants.FORMATTRVALUE_TABLESTYLE_SCROLLED))
	            	sb.append("fix_table_header(\"" + tableId + "\");");
	            sb.append("'");
            }
            sb.append(">\n");
//            indent(sb,indent+8).append("<div style='width:"+w+"px;'>").append(collabel);
            indent(sb,indent+6).append("<div>").append(collabel);
            if (sortOn!=null && sortOn.equals(column.name)) {
           	 	indent(sb,indent+4).append("<img src='resource/");
           	 	sb.append(descending?"arrowdown2.gif":"arrowup2.gif").append("' alt='");
           	 	sb.append(descending?"down":"up").append("' style='border-width:0px;'/>");
            }
            sb.append("</div>\n");
            indent(sb,indent+4).append("</th>\n");
        	x += column.width;
        	column.action = child.getAttribute(FormConstants.FORMATTR_ACTION);
        	if (column.action!=null&&column.action.length()==0) column.action = null;
        }
        indent(sb,indent+2).append("</tr>\n");
        indent(sb,indent).append("</thead>\n");
        return columns;
    }

    private void create_TABLE_button(StringBuffer sb, int indent, String buttonName,
			boolean enabled, String datapath, String direction, int pagesize, String paginator, String tableId) {
    	indent(sb,indent).append("<td ");
    	if (!enabled) {
    		sb.append("class='form-table-footer-button-disabled'>\n");
    	} else {
    		sb.append("class='form-table-footer-button' onclick='ajax_action(\""
    				+ paginator + "?action=paging&amp;table="+datapath+"&amp;topage="
    				+ direction + "&amp;meta=" + datapath + "_META&amp;pagesize=" + pagesize
    				+ "\",\"" + tableId + "\");'>\n");
    	}
    	indent(sb,indent+2).append("<span>" + buttonName + "</span>\n");
    	indent(sb,indent).append("</td>\n");
    }

    protected Rectangle create_TABLE(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
    	Rectangle vr = determine_vr(node, vx, vy, 600, 120);
        String datapath = node.getAttribute(FormConstants.FORMATTR_DATA);
        tables.put(id, node);
        String style = node.getAttribute(FormConstants.FORMATTR_TABLE_STYLE);
        if (style==null) style = FormConstants.FORMATTRVALUE_TABLESTYLE_SCROLLED;
//        style = FormConstants.FORMATTRVALUE_TABLESTYLE_PAIENATED;
        MbengNode tablenode = datapath==null?null:datadoc.getNode(datapath);
        MbengNode metanode = datapath==null?null:datadoc.getNode(datapath+"_META");
        String sortOn = metanode==null?null:datadoc.getValue(metanode,"sort_on");
        boolean descending = false;
        if (sortOn!=null && sortOn.startsWith("-")) {
        	descending = true;
        	sortOn = sortOn.substring(1);
        }
        String paginator = node.getAttribute(FormConstants.FORMATTR_ACTION);
        if (style.equals(FormConstants.FORMATTRVALUE_TABLESTYLE_PAGINATED)) {
        	indent(sb,indent).append("<div ");
        	sb.append("style='" + geo(vr) + "'");
            sb.append(">\n");
	        indent(sb,indent).append("<table class='form-table' id='").append(id).append("'");
	        if (datapath!=null) {
	        	sb.append(" data='" + datapath + "'");
	        	sb.append(" meta='" + datapath + "_META'");
	        }
	        if (paginator!=null) sb.append(" paginator='").append(paginator).append("'");
	        sb.append(" table_style='").append(style).append("'");
	        sb.append(">\n");
	        // create table header
        	int rowsPerPage = (vr.height-45)/24;
	        List<TableColumn> columns = create_TABLE_header(sb, indent+2, node, sortOn, descending, datapath,
	        		paginator, rowsPerPage, id, style);
	        // count pages
	        int startrow, totalrows;
	        if (metanode!=null) {
	            String v = datadoc.getValue(metanode, "start_row");
	            startrow = (v==null||v.length()==0)?1:Integer.parseInt(v);
	            v = datadoc.getValue(metanode, "total_rows");
	            if (v==null||v.length()==0) {
	            	totalrows = 0;
	            	for (MbengNode row=tablenode.getFirstChild(); row!=null; row=row.getNextSibling()) {
	            		totalrows++;
	            	}
	            } else totalrows = Integer.parseInt(v);
	        } else {
	        	startrow = 1;
	        	totalrows = 0;
	        }
	        // create table footer
	        indent(sb,indent+2).append("<tfoot>\n");
	        indent(sb,indent+4).append("<tr class='form-table-footer'>\n");
	        indent(sb,indent+6).append("<td colspan='3' scope='colgroup'>\n");
	        indent(sb,indent+8).append("<div align='center'>\n");
	        indent(sb,indent+10).append("<table border='0' cellpadding='0' cellspacing='1' style='text-align:center'>\n");
	        indent(sb,indent+12).append("<tbody>\n");
	        indent(sb,indent+14).append("<tr>\n");
	        create_TABLE_button(sb, indent+16, "First", startrow>1, datapath, "F", rowsPerPage, paginator, id);
	        create_TABLE_button(sb, indent+16, "Prev", startrow>1, datapath, "P", rowsPerPage, paginator, id);
	        indent(sb,indent+16).append("<td>&nbsp;" + startrow + " - " +
	        		(startrow+rowsPerPage-1>totalrows?totalrows:startrow+rowsPerPage-1)+ " of " + totalrows + "&nbsp;</td>\n");
	        create_TABLE_button(sb, indent+16, "Next", startrow+rowsPerPage<=totalrows, datapath, "N", rowsPerPage, paginator, id);
	        create_TABLE_button(sb, indent+16, "Last", startrow+rowsPerPage<=totalrows, datapath, "L", rowsPerPage, paginator, id);
	        indent(sb,indent+14).append("</tr>\n");
	        indent(sb,indent+12).append("</tbody>\n");
	        indent(sb,indent+10).append("</table>\n");
	        indent(sb,indent+8).append("</div>\n");
	        indent(sb,indent+6).append("</td>\n");
	        indent(sb,indent+4).append("</tr>\n");
	        indent(sb,indent+2).append("</tfoot>\n");

	        // create table body
	        indent(sb,indent+2).append("<tbody id='" + id + "_bs'>\n");
	        if (tablenode!=null) {
	        	int k = 0;
	        	for (MbengNode row=tablenode.getFirstChild(); row!=null&&k<rowsPerPage; row=row.getNextSibling()) {
	        		create_TABLE_datarow(sb, indent+4, row, k++, id, columns);
	        	}
	        }
	        indent(sb,indent+2).append("</tbody>\n");
	        indent(sb,indent).append("</table>\n");
	        indent(sb,indent).append("</div>\n");
        } else if (style.equals(FormConstants.FORMATTRVALUE_TABLESTYLE_SIMPLE)) {
           	indent(sb,indent).append("<div class='form-table'");
	        if (datapath!=null) {
	        	sb.append(" data='" + datapath + "'");
	        	sb.append(" meta='" + datapath + "_META'");
	        }
	        if (paginator!=null) sb.append(" paginator='").append(paginator).append("'");
	        sb.append(" table_style='").append(style).append("'");
	        sb.append(" style='" + geo(vr) + "overflow:auto;'");
	        sb.append(" id='" + id  + "'>\n");
	        indent(sb,indent+2).append("<table cellpadding='1' cellspacing='1'");
	        sb.append(" style='position:absolute;left:0px;top:0px;width:100%;border-collapse:collapse;'>\n");
	        List<TableColumn> columns = create_TABLE_header(sb, indent+4, node, sortOn, descending, datapath, null, 0, id, style);
	        indent(sb,indent+4).append("<tbody id='" + id + "_bs'>\n");
	        if (tablenode!=null) {
	        	int k = 0;
	        	for (MbengNode row=tablenode.getFirstChild(); row!=null; row=row.getNextSibling()) {
	        		create_TABLE_datarow(sb, indent+6, row, k++, id, columns);
	        	}
	        }
	        indent(sb,indent+4).append("</tbody>\n");
	        indent(sb,indent+2).append("</table>\n");
	        indent(sb,indent).append("</div>\n");
        } else {	// scrolled
        	style = FormConstants.FORMATTRVALUE_TABLESTYLE_SCROLLED;
	        indent(sb,indent).append("<div");
	        if (datapath!=null) {
	        	sb.append(" data='" + datapath + "'");
	        	sb.append(" meta='" + datapath + "_META'");
	        }
	        if (paginator!=null) sb.append(" paginator='").append(paginator).append("'");
	        sb.append(" table_style='").append(style).append("'");
	        sb.append(" style='" + geo(vr) + "' id='" + id + "'>\n");
	        // create table header
	        indent(sb,indent+2).append("<div style='position:absolute;left:0px;top:0px;width:100%;height:24px;overflow:hidden;'>\n");
	        indent(sb,indent+4).append("<div style='position:relative;' id='" + id + "_hs'>\n");
	        indent(sb,indent+6).append("<table cellpadding='1' cellspacing='1' style='border-collapse:collapse;'>\n");
	        List<TableColumn> columns = create_TABLE_header(sb, indent+8, node, sortOn, descending, datapath, null, 0, id, style);
	        indent(sb,indent+6).append("</table>\n");
	        indent(sb,indent+4).append("</div>\n");
	        indent(sb,indent+2).append("</div>\n");
	        // create table body
	        indent(sb,indent+2).append("<div style='position:absolute;left:0px;top:24px;width:"+vr.width
	        		+ "px;height:" + (vr.height-24) + "px;border=1;overflow:auto;' onscroll='scroll_table_header(\""
	        		+ id + "\");' id='" + id + "_bs'>\n");
	        indent(sb,indent+4).append("<table cellpadding='1' cellspacing='1' style='border-collapse:collapse;width:100%;'>\n");
	        if (tablenode!=null) {
	        	int k = 0;
	        	for (MbengNode row=tablenode.getFirstChild(); row!=null; row=row.getNextSibling()) {
	        		create_TABLE_datarow(sb, indent+6, row, k++, id, columns);
	        	}
	        }
	        indent(sb,indent+4).append("</table>\n");
	        indent(sb,indent+2).append("</div>\n");
	        // fix table header widths
	        this.widgetInitialization.add("fix_table_header('"+id+"');");
	        indent(sb,indent).append("</div>\n");
        }
        return vr;
    }

    protected Rectangle create_TEXT(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
    	boolean required = isRequired(node, id);
    	boolean readonly = isReadonly(node, id);
        create_label(node, lx_default, vy, label_text, sb, indent, required, id);
        String assoc_data = node.getAttribute(FormConstants.FORMATTR_DATA);
        if (assoc_data!=null && assoc_data.length()==0) assoc_data = null;
        Rectangle vr = determine_vr(node, vx, vy, 300, 20);
        indent(sb, indent);
        if ("w_pass".equals(assoc_data)) sb.append("<input type='password'\n");
        else {
        	sb.append("<input type='text' ");
        	generateClass(sb, readonly, readonly?"form-input-readonly":"form-input");
        	sb.append("\n");
        }
        // TODO: use attribute to implement secret
        generateId(sb, indent+4, id, assoc_data, true);
        if (readonly) generateReadonly(sb, indent+4, "readonly");
        String validators = node.getAttribute(FormConstants.FORMATTR_VALIDATORS);
        String mask = getMaskValidator(validators);
        if (mask!=null) {
        	indent(sb,indent+4).append("onkeypress='return masking(this,event,"
        			+ mask.replaceAll("'","\"") + ")'\n");
        } else {
        	String max = getLengthValidator(validators);
        	if (max!=null) {
        		indent(sb,indent+4).append("onkeypress='return lengthing(this,event,\""
        				+ max + "\")'\n");
        	}
        }
        indent(sb,indent+4).append("style='" +
        		geo(vr.x,vr.y,vr.width+input_text_xhtml_adjust,vr.height+input_text_xhtml_adjust) + "'\n");
        if (validators!=null && validators.length()>0)
        	indent(sb,indent+4).append("validator='").append(validators.replaceAll("'","\"")).append("'");
        if (required) sb.append(" required='true'");
        sb.append("/>\n");
    	return vr;
    }

    protected Rectangle create_TABBEDPANE(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
        Rectangle vr = determine_vr(node, vx, vy, 600, 300);
    	boolean readonly = isReadonlyRegardlessAsssignment(node, id);
    	int activeIndex = 0;
        String assoc_data = node.getAttribute(FormConstants.FORMATTR_DATA);
        if (assoc_data==null || assoc_data.length()==0)
        	assoc_data = "__mdwtabindex__" + id;
        String v = datadoc.getValue(assoc_data);
        if (v!=null && v.length()>0) activeIndex = Integer.parseInt(v);
        String tabbing_style = node.getAttribute(FormConstants.FORMATTR_TABBING_STYLE);
        if (readonly) {
	        int k = 0;
	    	for (MbengNode child=node.getFirstChild(); child!=null; child=child.getNextSibling()) {
	    		if (activeIndex==k) {
	    			String tablabel = child.getAttribute(FormConstants.FORMATTR_LABEL);
	    			indent(sb,indent).append("<fieldset id='" + id + "_tb"+k + "' class='form-panel'\n");
	    			// not sure why the fieldset makes it wider than specified - subtract 16 to correct
	    			indent(sb,indent+4).append("style='" + geo(vr.x,vr.y,vr.width-16,vr.height) + "'");
	    			sb.append(">\n");
	    			if (tablabel!=null && tablabel.length()>0) {
	    				indent(sb,indent+2).append("<legend class='form-panel-header'>");
	    				sb.append(tablabel).append("</legend>\n");
	    			}
	    			indent(sb,indent+2).append("<div style='position:absolute;left:0px;top:20px;width:100%;height:"
	    	    			 + (vr.height-20) + "px;'>\n");
	    			create_children(child, lx_default, sb, indent+2);
	    			indent(sb,indent+2).append("</div>\n");
	    			indent(sb,indent).append("</fieldset>\n");
	    		}
	    		k++;
	        }
        } else if (FormConstants.FORMATTRVALUE_TABBINGSTYLE_JQUERY.equalsIgnoreCase(tabbing_style)) {
        	indent(sb,indent).append("<div id='").append(id).append("'\n");
     		indent(sb,indent+4).append("style='" + geo(vr) + "'");
     		sb.append(" data='").append(assoc_data).append("'");
     		sb.append(">\n");
     		// create tab header
     		indent(sb,indent+2).append("<ul>\n");
        	int k = 0;
        	for (MbengNode child=node.getFirstChild(); child!=null; child=child.getNextSibling()) {
        		k++;
	        	String tablabel = child.getAttribute(FormConstants.FORMATTR_LABEL);
        		indent(sb,indent+4).append("<li><a href='#").append(id).append("-").append(k).append("'>")
        			.append(tablabel).append("</a></li>\n");
        	}
     		indent(sb,indent+2).append("</ul>\n");
	        k = 0;
	    	for (MbengNode child=node.getFirstChild(); child!=null; child=child.getNextSibling()) {
	    		k++;
	    		indent(sb,indent+2).append("<div id='").append(id).append("-").append(k).append("'\n");
	    		indent(sb,indent+6).append("style='").append(geo(0,25,vr.width,vr.height-25)).append("'>\n");
//	    		indent(sb,indent+6).append("style='position:absolute;top:25px;width:100%;height:").append(vr.height-25).append("px;'>\n");
	    		create_children(child, lx_default, sb, indent+4);
	    		indent(sb,indent+2).append("</div>\n");
	        }
	        indent(sb,indent).append("</div>\n");
	        this.widgetInitialization.add("$('#" + id + "').tabs({'selected':" + activeIndex + "});");
        } else {	// MDW implemented - client side, server side, or ajax
     		indent(sb,indent).append("<div id='").append(id).append("'\n");
     		indent(sb,indent+4).append("style='" + geo(vr) + "'");
     		sb.append(" data='").append(assoc_data).append("'");
	        String paginator = node.getAttribute(FormConstants.FORMATTR_ACTION);
     		if (FormConstants.FORMATTRVALUE_TABBINGSTYLE_AJAX.equalsIgnoreCase(tabbing_style)) {
    	        if (paginator!=null && paginator.length()>0) {
    	        	sb.append(" paginator='").append(paginator).append("'");
    	        }
     		}
     		sb.append(">\n");
     		// create tab header
     		indent(sb,indent+2).append("<div class='form-tabheader'>\n");
        	int n = 0;
        	for (MbengNode child=node.getFirstChild(); child!=null; child=child.getNextSibling()) {
        		n++;
        	}
        	int k = 0;
	        for (MbengNode child=node.getFirstChild(); child!=null; child=child.getNextSibling()) {
	        	String tablabel = child.getAttribute(FormConstants.FORMATTR_LABEL);
	        	indent(sb,indent+4).append("<span class='")
	        		.append(activeIndex==k?"form-tabheader-active":"form-tabheader-inactive")
	        		.append("' id='").append(id+"_th"+k);
	        	if (FormConstants.FORMATTRVALUE_TABBINGSTYLE_SERVER.equalsIgnoreCase(tabbing_style)
	        			&& paginator!=null && paginator.length()>0) {
	        		sb.append("' onclick='perform_action(\"").append(paginator)
        				.append("?action=tabbing&amp;tabs=")
        				.append(id).append("&amp;tab=").append(k).append("&amp;data=")
        				.append(assoc_data).append("\")'>");
	        	} else {	// FormConstants.FORMATTRVALUE_TABBINGSTYLE_CLIENT
	        		sb.append("' onclick='show_tab(\""+id+"\","+k+")'>");
	        	}
	        	sb.append(tablabel).append("</span>\n");
	        	k++;
	        }
	    	indent(sb,indent+2).append("</div>\n");
	    	// create tab body
	    	indent(sb,indent+2).append("<div class='form-tabbody'\n");
	        indent(sb,indent+6).append("style='" + geo(0,22,vr.width,vr.height) + "'");
	        sb.append(">\n");
	        k = 0;
	    	for (MbengNode child=node.getFirstChild(); child!=null; child=child.getNextSibling()) {
	    		indent(sb,indent+4).append("<div id='" + id + "_tb"+k + "'\n");
	    		indent(sb,indent+8).append("style='").append(geo(0,0,vr.width,vr.height));
	    		if (activeIndex==k) {
	    			sb.append("'>\n");
	    		} else {
	    			sb.append("display:none;'>\n");
	    		}
	    		if (activeIndex==k || tabbing_style==null || tabbing_style.equalsIgnoreCase("client")) {
	    			create_children(child, lx_default, sb, indent+4);
	    		}
	    		indent(sb,indent+4).append("</div>\n");
	    		k++;
	        }
	    	indent(sb,indent+2).append("</div>\n");
	        indent(sb,indent).append("</div>\n");
        }
        return vr;
    }

    protected void create_MENU_common(MbengNode menu, StringBuffer sb, int indent, List<MbengNode> submenus) {
    	String id = menu.getAttribute(FormConstants.FORMATTR_ID);
    	String label = menu.getAttribute(FormConstants.FORMATTR_LABEL);
    	if (menu.getParent().getName().equals(FormConstants.WIDGET_MENUBAR)) {
    		indent(sb,indent).append("<div class='dropmenudiv' id='" + id + "'>\n");
    	} else if (menu.getParent().getName().equals(FormConstants.WIDGET_MENU)) {
    		indent(sb,indent).append("<div class='dropmenudiv' id='" + id
    				+ "' onmouseout='hide_popup_menu(this,event);'>\n");
    	} else {	// pop up
    		indent(sb,indent).append("<div class='dropmenudiv' id='" + id
    				+ "' onmouseout='hide_popup_menu(this,event);'>\n");
    	}
    	for (MbengNode item=menu.getFirstChild(); item!=null; item=item.getNextSibling()) {
			if (item.getName().equals(FormConstants.WIDGET_MENUITEM)) {
				String itemLabel = item.getAttribute(FormConstants.FORMATTR_LABEL);
				String itemAction = item.getAttribute(FormConstants.FORMATTR_ACTION);
				if (itemAction==null) itemAction = "alert('Action not specified')";
				indent(sb,indent+2).append("<a href='");
				constructPerformActionCall(sb, item, itemAction, false, true);
				sb.append("'>").append(itemLabel).append("</a>\n");
			} else if (item.getName().equals(FormConstants.WIDGET_MENU)) {
				String itemId = item.getAttribute(FormConstants.FORMATTR_ID);
				indent(sb,indent+2).append("<span onmouseover='show_popup_menu(this,event,\""
	    				+ itemId + "\",\"submenu\");'>").append(label).append(" >>>").append("</span>\n");
				submenus.add(item);
			}
		}
    	indent(sb,indent).append("</div>\n");
    	// TODO fix problems with submenu (positioning, hide, etc)
    }

    protected Rectangle create_MENUBAR(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
        Rectangle vr = determine_vr(node, 0, 0, 1600, 20);
        if (useChromeMenu) {
	        // create menu bar, including menu labels in the bar
	        indent(sb,indent).append("<div class='chromestyle' id='" + id
	        		+ "' style='position:absolute;left:0px;top:0px;'>\n");
	        indent(sb,indent+2).append("<ul>\n");
	        for (MbengNode child=node.getFirstChild(); child!=null; child=child.getNextSibling()) {
	            String childId = child.getAttribute(FormConstants.FORMATTR_ID);
	            String label = child.getAttribute(FormConstants.FORMATTR_LABEL);
	        	indent(sb,indent+4).append("<li><a href='#' rel='" + childId + "'>"
	        			+ label + "</a></li>\n");
	        	menus.add(child);
	        }
	        indent(sb,indent+2).append("</ul>\n");
	        indent(sb,indent).append("</div>\n");
	        // what is the following doing? need to call these after menus are created
	        menus.add(node);
	//        indent(sb,indent).append("<script type='text/javascript'>\n");
	//        indent(sb,indent+2).append("cssdropdown.startchrome('" + id +"');\n");
	//        indent(sb,indent).append("</script>\n");
        } else {
        	indent(sb,indent).append("<ul class='simpleMenu' id='").append(id)
        		.append("' style='position:absolute;left:-40px;top:0px;width:99%;height:25px;'>\n");
            create_children(node, lx_default, sb, indent);
        	indent(sb,indent).append("</ul>\n");
        	menus.add(node);
        }
    	return vr;
    }

    protected Rectangle create_MENU(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
    	Rectangle vr = determine_vr(node, vx, 0, 120, 20);
    	if (node.getParent().getName().equals(FormConstants.WIDGET_MENUBAR)) {
    		if (useChromeMenu) {
    			// should never be here
    		} else {
    			indent(sb,indent).append("<li><a href='#'>").append(label_text).append("</a>\n");
    			indent(sb,indent+2).append("<ul>\n");
    			create_children(node, lx_default, sb, indent+2);
    			indent(sb,indent+2).append("</ul>\n");
    			indent(sb,indent).append("</li>\n");
    		}
    	} else if (node.getParent().getName().equals(FormConstants.WIDGET_MENU)) {
    		if (useChromeMenu) {
    			// should never be here
    		} else {
    			indent(sb,indent).append("<li><a href='#'>").append(label_text).append("</a>\n");
    			indent(sb,indent+2).append("<ul>\n");
                create_children(node, lx_default, sb, indent+2);
        		indent(sb,indent+2).append("</ul>\n");
    			indent(sb,indent).append("</li>\n");
    		}
    	} else {	// popup menu
    		if (useChromeMenu) {
	//    		indent(sb,indent).append("<span id='" + id + "' class='chromestyle ul li a'\n");
	//    		indent(sb,indent+4).append("style='").append(geo(vr)).append("'\n");
	    		indent(sb,indent).append("<span id='" + id + "_button'\n");
	    		indent(sb,indent+4).append("style='").append(geo(vr)).append("border-width:1px;border-style:solid;border-color:#252525;'\n");
	    		indent(sb,indent+4).append("onclick='show_popup_menu(this,event,\""
	    				+ id + "\",\"popup\");'>\n");
	    		indent(sb,indent+2).append(label_text).append("\n");
	    		indent(sb,indent).append("</span>\n");
	            menus.add(node);
    		} else {
    			indent(sb,indent).append("<ul class='simpleMenu' id='").append(id)
    			.append("' style='").append(geo(vr.x-40,vr.y,vr.width,vr.height)).append("padding:4px;'>\n");
//				indent(sb,indent+2).append("<li><a href='#'>").append(label_text).append("</a>\n");
    			indent(sb,indent+2).append("<li style='width:" + vr.width + "px;padding:2px;border-width:1px;border-style:solid;border-color:#404040;'><span >").append(label_text).append("</span>\n");
    			indent(sb,indent+4).append("<ul>\n");
    			create_children(node, lx_default, sb, indent+4);
    			indent(sb,indent+4).append("</ul>\n");
    			indent(sb,indent+2).append("</li>\n");
        		indent(sb,indent).append("</ul>\n");
        		menus.add(node);
    		}
    	}
        return vr;
    }

    protected Rectangle create_MENUITEM(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
		if (useChromeMenu) {
			// should never be called
		} else {
			String itemAction = node.getAttribute(FormConstants.FORMATTR_ACTION);
	    	Rectangle vr = determine_vr(node.getParent(), vx, 0, 120, 20);	// parent VR
			if (itemAction==null) itemAction = "alert('Action not specified')";
			indent(sb,indent).append("<li style='width:" + (vr.width-3) + "px;padding:2px;'><a href='");
			constructPerformActionCall(sb, node, itemAction, false, true);
			sb.append("'>").append(label_text).append("</a></li>\n");
		}
		return null;
    }

    protected Rectangle create_RADIOBUTTONS(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
    	boolean required = isRequired(node, id);		// TODO how to check required on client side
    	boolean readonly = isReadonly(node, id);
    	create_label(node, lx_default, vy, label_text, sb, indent, required, id);
        Rectangle vr = determine_vr(node, vx, vy, 160, 20);
        String assoc_data = node.getAttribute(FormConstants.FORMATTR_DATA);
        if (assoc_data!=null && assoc_data.length()==0) assoc_data = null;
        indent(sb,indent).append("<div\n");
        generateId(sb, indent+4, id, null, true);
        if (readonly) generateReadonly(sb, indent+4, "disabled");
        String av = node.getAttribute(FormConstants.FORMATTR_DIRECTION);
        boolean vertical = (av!=null && av.startsWith("V"));
        indent(sb,indent+4).append("style='" + geo(vr) + "'");
        sb.append(">\n");
        String[] choices = getChoices(node.getAttribute(FormConstants.FORMATTR_CHOICES));
        String action = node.getAttribute(FormConstants.FORMATTR_ACTION);
        if (action!=null && action.length()==0) action = null;
        if (choices!=null) {
        	String v, l;
        	for (int i=0; i<choices.length; i++) {
        		int k = choices[i].indexOf(':');
                if (k>=0) {
                	v = choices[i].substring(0,k);
                	l = choices[i].substring(k+1);
                } else v = l = choices[i];
                indent(sb,indent+2).append("<input type='radio' name='").append(id).append("' value='");
                sb.append(v).append("' ");
                if (assoc_data!=null) {
                	sb.append("data='").append(assoc_data).append("' ");
                	generateClass(sb, readonly, null);
                }
            	if (action!=null) {
            		sb.append(" onclick='");
            		this.constructPerformActionCall(sb, node, action, true, false);
            		sb.append("'");
            	}
            	sb.append(">" + l + (vertical?"<br>\n":"&nbsp;\n"));
        	}
        }
        indent(sb,indent).append("</div>\n");

    	return vr;
    }

    protected Rectangle create_CHECKBOX(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
    	boolean required = isRequired(node, id);
    	boolean readonly = isReadonly(node, id);
    	create_label(node, lx_default, vy, label_text, sb, indent, required, id);
        Rectangle vr = determine_vr(node, vx, vy, 24, 24);
        String assoc_data = node.getAttribute(FormConstants.FORMATTR_DATA);
        if (assoc_data!=null && assoc_data.length()==0) assoc_data = null;
        indent(sb,indent).append("<input type='checkbox'\n");
        generateId(sb, indent+4, id, assoc_data, true);
    	if (readonly) generateReadonly(sb, indent+4, "disabled");
    	indent(sb,indent+4);
    	generateClass(sb, readonly, null);
        indent(sb,indent+4).append("style='" + geo(vr) + "'");
        String action = node.getAttribute(FormConstants.FORMATTR_ACTION);
        if (action!=null && action.length()>0) {
        	sb.append(" onclick='");
        	this.constructPerformActionCall(sb, node, action, true, false);
        	sb.append("'");
        }
        if (required) sb.append(" required='true'");
        sb.append("/>\n");
    	return vr;
    }

    protected String[] getChoices(String av) {
        String[] choices;
        if (av!=null && av.length()>0) {
            if (av.startsWith("$$.")) {
            	MbengNode choicesNode = datadoc.getNode(av.substring(3));
            	if (choicesNode!=null) {
            		int n=0;
            		for (MbengNode op=choicesNode.getFirstChild(); op!=null; op=op.getNextSibling()) {
            			n++;
            		}
            		choices = new String[n];
            		n = 0;
            		for (MbengNode op=choicesNode.getFirstChild(); op!=null; op=op.getNextSibling()) {
            			choices[n++] = op.getValue();
            		}
            	} else choices = null;
            } else choices = av.split(", *");
        } else choices = null;
        return choices;
    }

    protected void create_select_items(MbengNode node, StringBuffer sb,
    		int indent, boolean withEmptyChoice, String value) {
        String[] choices = getChoices(node.getAttribute(FormConstants.FORMATTR_CHOICES));
        if (choices==null) {
            indent(sb,indent).append("<option value=''></option>\n");
        } else {
            if (withEmptyChoice)
            	indent(sb,indent).append("<option value=''></option>\n");
            String v, l;
            for (int i=0; i<choices.length; i++) {
                int k = choices[i].indexOf(':');
                if (k>=0) {
                	v = choices[i].substring(0,k);
                	l = choices[i].substring(k+1);
                } else v = l = choices[i];
                indent(sb,indent).append("<option value='");
                sb.append(v);
                if (value!=null && value.equals(v)) sb.append("' selected='selected'>");
                else sb.append("'>");
                sb.append(l).append("</option>\n");
            }
        }
    }

    protected Rectangle create_LIST(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
    	boolean required = isRequired(node, id);
    	create_label(node, lx_default, vy, label_text, sb, indent, required, id);
        Rectangle vr = determine_vr(node, vx, vy, 160, 100);
        int size = vr.height/24;
        String assoc_data = node.getAttribute(FormConstants.FORMATTR_DATA);
        if (assoc_data!=null && assoc_data.length()==0) assoc_data = null;
        // TODO get data from javascript data
        MbengNode valuenode = (assoc_data==null)?null:datadoc.getNode(assoc_data);
        indent(sb, indent).append("<select size='" + size + "' multiple='multiple'").append('\n');
        generateId(sb, indent+4, id, assoc_data, true);
        indent(sb,indent+4).append("style='" + geo(vr) + "'");
        if (required) sb.append(" required='true'");
        sb.append(">\n");
        String[] choices = getChoices(node.getAttribute(FormConstants.FORMATTR_CHOICES));
        if (choices!=null) {
            String v, l;
            for (int i=0; i<choices.length; i++) {
                int k = choices[i].indexOf(':');
                if (k>=0) {
                	v = choices[i].substring(0,k);
                	l = choices[i].substring(k+1);
                } else v = l = choices[i];
                indent(sb,indent+2).append("<option value='").append(v).append("'");
                if (valuenode!=null) {
                	for (MbengNode vn=valuenode.getFirstChild(); vn!=null; vn=vn.getNextSibling()) {
                		String v1 = vn.getAttribute(FormDataDocument.ATTR_NAME);
                		if (v.equals(v1)) {
                			sb.append(" selected='selected'");
                			break;
                		}
                	}
                }
                sb.append(">");
                sb.append(l).append("</option>\n");
            }
        }
        indent(sb,indent).append("</select>\n");
        return vr;
    }

    protected void create_LISTPICKER_button(StringBuffer sb, int indent, String id, String label, String dir,
    		int x, int y, int w, int h) {
    	indent(sb,indent).append("<input type='button' style='").append(geo(x,y,w,h));
    	sb.append("' value='").append(label).append("' onclick='perform_action(\"");
    	sb.append(FormConstants.ACTION_LIST_PICK).append("?list=").append(id);
    	sb.append("&amp;dir=").append(dir).append("\");'/>\n");
    }

    protected Rectangle create_LISTPICKER(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
    	boolean required = isRequired(node, id);
        create_label(node, lx_default, vy, label_text, sb, indent, required, id);
        Rectangle vr = determine_vr(node, vx, vy, 320, 100);
        int can_x = 2;
        int sel_x = vr.width/2 + 13;
        int sw = vr.width/2-15;
        int sy = 23;
        int ly = 5;
        int sh = vr.height - 25;
        String assoc_data = node.getAttribute(FormConstants.FORMATTR_DATA);
        // TODO get data from javascript data
        MbengNode valuenode = (assoc_data==null)?null:datadoc.getNode(assoc_data);
        indent(sb,indent).append("<div style='").append(geo(vr)).append("'>\n");
        indent(sb,indent+2).append("<span style='"+geo(can_x,ly,100,20)+"'>Candidates</span>\n");
        indent(sb,indent+2).append("<span style='"+geo(sel_x,ly,100,20)+"'>Selected</span>\n");
        // candidate list
        indent(sb,indent+2).append("<div style='overflow:auto;border-width:1px;border-style: solid;border-color: #cfcfcf;").append(geo(can_x,sy,sw,sh)).append("'>\n");
        indent(sb, indent+4).append("<select style='width:100%;height:100%;' multiple='multiple' name='" + id + "_can'");
        if (required) sb.append(" required='true'");
        sb.append(">\n");
        String[] choices = getChoices(node.getAttribute(FormConstants.FORMATTR_CHOICES));
        if (choices!=null) {
            String v, l;
            MbengNode selected;
            for (int i=0; i<choices.length; i++) {
                int k = choices[i].indexOf(':');
                if (k>=0) {
                	v = choices[i].substring(0,k);
                	l = choices[i].substring(k+1);
                } else v = l = choices[i];
                selected = (valuenode==null)?null:valuenode.findChild(v);
                if (selected==null) {
                	indent(sb,indent+6).append("<option value='").append(v).append("'");
                	sb.append(">").append(l).append("</option>\n");
                }
            }
        }
        indent(sb,indent+4).append("</select>\n");
        indent(sb,indent+2).append("</div>\n");
        // selected list
        indent(sb,indent+2).append("<div style='overflow:auto;border-width:1px;border-style: solid;border-color: #cfcfcf;").append(geo(sel_x,sy,sw,sh)).append("'>\n");
        indent(sb, indent+4).append("<select style='width:100%;height:100%;' multiple='multiple' name='" + id + "_sel'>\n");
        if (valuenode!=null) {
        	for (MbengNode vn=valuenode.getFirstChild(); vn!=null; vn=vn.getNextSibling()) {
        		String v = vn.getName();
        		String l = vn.getValue();
        		if (v==null || v.length()==0) v = l;
        		indent(sb,indent+6).append("<option value='").append(v).append("'");
                sb.append(">");
                sb.append(l).append("</option>\n");
            }
        }
        indent(sb,indent+4).append("</select>\n");
        indent(sb,indent+2).append("</div>\n");
        // buttons
        int bw = 24;
        int bx = vr.width/2-bw/2;
        int bym = vr.height/2+10;
        int bh = 20;
        create_LISTPICKER_button(sb, indent+2, id, "&gt;&gt;", "selectall", bx, bym-46, bw, bh);
        create_LISTPICKER_button(sb, indent+2, id, "&gt;", "select", bx, bym-22, bw, bh);
        create_LISTPICKER_button(sb, indent+2, id, "&lt;", "exclude", bx, bym+2, bw, bh);
        create_LISTPICKER_button(sb, indent+2, id, "&lt;&lt;", "excludeall", bx, bym+26, bw, bh);

        indent(sb,indent).append("</div>\n");
        return vr;
    }

    protected Rectangle create_HYPERLINK(MbengNode node, String id, String label_text,
    		int lx_default, int vx, int vy, StringBuffer sb, int indent) {
    	 Rectangle vr = determine_vr(node, vx, vy, 480, 20);
         String url = node.getAttribute(FormConstants.FORMATTR_ACTION);
         String imagename = node.getAttribute(FormConstants.FORMATTR_IMAGE);
         if (url==null) url = "";
         if (url.startsWith("prop:")) {
        	 url = PropertyManager.getProperty(url.substring(5));
         } else if (url.startsWith("$$.")) {
             // TODO get data from javascript data
        	 MbengNode datanode = datadoc.getNode(url.substring(3));
        	 if (datanode!=null) url = datanode.getValue();
         } else if (!url.contains("/")) {
        	 url = "resource?name=" + url;
         }
         indent(sb,indent).append("<span\n");
         indent(sb,indent+4).append("id='" + id + "'\n");
         indent(sb,indent+4).append("value='" + url + "'\n");
         indent(sb,indent+4).append("style='" + geo(vr) + "'>\n");
         indent(sb,indent+2).append("<a href='").append(StringHelper.escapeXml(url)).append("' target='_blank'>\n");
         if (imagename!=null && imagename.length()>0) {
        	 url = "resource?name=" + imagename;
        	 indent(sb,indent+4).append("<img src='").append(url).append("' ");
        	 sb.append("alt='" + label_text + "' style='border-width:0px;'/>\n");
         } else {
        	 sb.append(label_text);
         }
         indent(sb,indent+2).append("</a>\n");
         indent(sb,indent).append("</span>\n");
         return vr;
    }

    protected String getLabel(MbengNode node) {
    	String label = node.getAttribute(FormConstants.FORMATTR_LABEL);
    	if (label!=null && label.startsWith("$$.")) {
            // TODO get data from javascript data
        	MbengNode datanode = datadoc.getNode(label.substring(3));
        	if (datanode!=null) label = datanode.getValue();
    	}
    	return label;
    }

    protected Rectangle create_component(MbengNode node,
            int lx_default, int vx, int vy, StringBuffer sb, int indent) {
        String id = node.getAttribute(FormConstants.FORMATTR_ID);
        String label_text = this.getLabel(node);
        Rectangle vr = null;
    	boolean hidden = isHidden(node, id);
    	if (hidden) return null;

        if (node.getName().equals(FormConstants.WIDGET_TEXT)) {
        	vr = create_TEXT(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else if (node.getName().equals(FormConstants.WIDGET_TEXTAREA)) {
        	vr = create_TEXTAREA(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else if (node.getName().equals(FormConstants.WIDGET_DROPDOWN)) {
        	vr = create_DROPDOWN(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else if (node.getName().equals(FormConstants.WIDGET_TABLE)) {
        	vr = create_TABLE(node, id, label_text, lx_default, vx, vy, sb, indent);
//        } else if (node.getName().equals(FormConstants.WIDGET_COLUMN)) {
        	// called directly in table, so not used
//        	vr = create_COLUMN(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else if (node.getName().equals(FormConstants.WIDGET_PANEL)) {
        	vr = create_PANEL(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else if (node.getName().equals(FormConstants.WIDGET_TABBEDPANE)) {
        	vr = create_TABBEDPANE(node, id, label_text, lx_default, vx, vy, sb, indent);
//        } else if (node.getName().equals(FormConstants.WIDGET_TAB)) {
//        	// called directly in tabbed pane, so not used
//        	vr = create_TAB(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else if (node.getName().equals(FormConstants.WIDGET_BUTTON)) {
        	vr = create_BUTTON(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else if (node.getName().equals(FormConstants.WIDGET_MENUBAR)) {
        	vr = create_MENUBAR(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else if (node.getName().equals(FormConstants.WIDGET_MENU)) {
        	vr = create_MENU(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else if (node.getName().equals(FormConstants.WIDGET_MENUITEM)) {
        	vr = create_MENUITEM(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else if (node.getName().equals(FormConstants.WIDGET_RADIOBUTTONS)) {
        	vr = create_RADIOBUTTONS(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else if (node.getName().equals(FormConstants.WIDGET_DATE)) {
        	vr = create_DATE(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else if (node.getName().equals(FormConstants.WIDGET_CHECKBOX)) {
        	vr = create_CHECKBOX(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else if (node.getName().equals(FormConstants.WIDGET_LIST)) {
        	vr = create_LIST(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else if (node.getName().equals(FormConstants.WIDGET_LISTPICKER)) {
        	vr = create_LISTPICKER(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else if (node.getName().equals(FormConstants.WIDGET_HYPERLINK)) {
        	vr = create_HYPERLINK(node, id, label_text, lx_default, vx, vy, sb, indent);
        } else {
            // VALIDATION_RULES, unimplemented
//            create_label(node, lx_default, vy, label_text, container);
            vr = null;
        }
        return vr;
    }

    protected String geo(int x, int y, int w, int h) {
        return "position:absolute;left:" + x + "px;top:" + y + "px;width:"
        	+ w + "px;height:" + h + "px;";
    }

    protected String geo(Rectangle r) {
        return "position:absolute;left:" + r.x + "px;top:" + r.y
        	+ "px;width:" + r.width + "px;height:" + r.height + "px;";
    }

    protected String formatText(String text) {
    	if (text==null) return text;
    	if (text.startsWith("<HTML>")) {
    		text = text.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    	} else if (text.contains("\\n")) {
			text = text.replaceAll("\\\\n", "&lt;br/&gt;");
		}
    	return text;
    }

    protected void create_label(MbengNode node, int lx, int ly, String text,
    		StringBuffer sb, int indent, boolean required, String id) {
        String v = node.getAttribute(FormConstants.FORMATTR_LX);
        if (v!=null) lx = Integer.parseInt(v);
        v = node.getAttribute(FormConstants.FORMATTR_LY);
        if (v!=null) ly = Integer.parseInt(v);
        v = node.getAttribute(FormConstants.FORMATTR_LW);
        int lw = (v!=null)?Integer.parseInt(v):160;
        indent(sb, indent).append("<label class='form-label' ");
        sb.append("style='" + geo(lx,ly,lw,20) + "'");
        String tip = node.getAttribute(FormConstants.FORMATTR_TIP);
        if (tip!=null && tip.length()>0) sb.append(" title='" + tip + "'");
        if (id!=null) sb.append(" for='").append(id).append("'");
        sb.append(">\n");
        indent(sb, indent+2).append(formatText(text));
        if (required) sb.append("<font color='red'>*</font>");
        indent(sb,indent+4).append("</label>\n");
    }

    protected void create_children(MbengNode node, int lx_default, StringBuffer sb, int indent) {
        int vx_default = 120;
        int max_pw = 0, max_ph = 5;
        for (MbengNode child=node.getFirstChild(); child!=null; child=child.getNextSibling()) {
        	if (child.getName().contains(FormConstants.WIDGET_MENUBAR)) continue;
        	if (child.getName().contains(FormConstants.WIDGET_MENU)) continue;
            Rectangle r = create_component(child, lx_default, vx_default, max_ph, sb, indent+2);
            if (r!=null) {
                if (r.x+r.width+5>max_pw) max_pw = r.x+r.width+5;
                if (r.y+r.height+5>max_ph) max_ph = r.y+r.height+5;
            }
        }
        // create menu and menu bars at the end to ensure they staty on top
        // this is needed only for simple menu
        for (MbengNode child=node.getFirstChild(); child!=null; child=child.getNextSibling()) {
        	if (child.getName().contains(FormConstants.WIDGET_MENUBAR)||
        			child.getName().contains(FormConstants.WIDGET_MENU)) {
        		create_component(child, lx_default, vx_default, max_ph, sb, indent+2);
        	}
        }
    }

    protected Rectangle determine_vr(MbengNode node, int vx, int vy, int vw, int vh) {
        Rectangle vr = new Rectangle();
        String v = node.getAttribute(FormConstants.FORMATTR_VX);
        vr.x = (v!=null)?Integer.parseInt(v):vx;
        v = node.getAttribute(FormConstants.FORMATTR_VY);
        vr.y = (v!=null)?Integer.parseInt(v):vy;
        v = node.getAttribute(FormConstants.FORMATTR_VW);
        vr.width = (v!=null)?Integer.parseInt(v):vw;
        v = node.getAttribute(FormConstants.FORMATTR_VH);
        vr.height = (v!=null)?Integer.parseInt(v):vh;
        return vr;
    }

    protected void generateReadonly(StringBuffer sb, int indent, String jsfAttr) {
    	if (jsfAttr.equals("readonly")) indent(sb,indent).append("readonly='readonly'\n");
    	else indent(sb,indent).append(jsfAttr).append("='true'\n");
    }

    protected boolean isHidden(MbengNode node, String id) {
    	String cond = node.getAttribute(FormConstants.FORMATTR_VISIBLE);
    	if (cond==null || cond.length()==0) return false;
    	if (cond.equalsIgnoreCase("false")) return true;
    	if (cond.equalsIgnoreCase("true")) return false;
    	return !evaluateCondition("Visible_"+id, cond, null);
    }


    protected boolean isReadonly(MbengNode node, String id) {
    	if (!assignStatus.equals(FormDataDocument.ASSIGN_STATUS_SELF)) return true;
    	String cond = node.getAttribute(FormConstants.FORMATTR_EDITABLE);
    	if (cond==null || cond.length()==0) return false;
    	if (cond.equalsIgnoreCase("false")) return true;
    	if (cond.equalsIgnoreCase("true")) return false;
    	return !evaluateCondition("Editable_"+id, cond, null);
    }

    protected boolean isReadonlyRegardlessAsssignment(MbengNode node, String id) {
    	String cond = node.getAttribute(FormConstants.FORMATTR_EDITABLE);
    	if (cond==null || cond.length()==0) return false;
    	if (cond.equalsIgnoreCase("false")) return true;
    	if (cond.equalsIgnoreCase("true")) return false;
    	return !evaluateCondition("Editable_"+id, cond, null);
    }

    protected boolean isRequired(MbengNode node, String id) {
    	if (!assignStatus.equals(FormDataDocument.ASSIGN_STATUS_SELF)) return false;
    	String cond = node.getAttribute(FormConstants.FORMATTR_REQUIRED);
    	if (cond==null || cond.length()==0) return false;
    	if (cond.equalsIgnoreCase("true")) return true;
    	if (cond.equalsIgnoreCase("false")) return false;
    	return evaluateCondition("Required_"+id, cond, null);
    }

    protected class MyRuleSet extends MbengRuleSet {
        MyRuleSet(String name, char type) throws MbengException {
            super(name, type, true, false);
//          this.defineVariable("this", false);
            this.defineTopLevelName("this");
        }

        @Override
        protected boolean isSectionName(String name) {
            return true;
        }
    }

    // TODO cache compiled version
    protected MbengRuleSet compileMagicRule(String name, String rule, char ruletype) throws MbengException {
        MbengRuleSet ruleset = new MyRuleSet(name, ruletype);
//        ruleset.defineDocument("datadoc", DomDocument.class.getName());
        ruleset.parse(rule);
        return ruleset;
    }

    protected static class MbengMDWLogger implements Logger {
    	StandardLogger _logger;
    	public MbengMDWLogger(StandardLogger logger) {
    		_logger = logger;
    	}

    	public PrintStream getPrintStream() {
    		return null;
    	}

    	public void logline(String message) {
    		_logger.info(message);
    	}
    }

    protected boolean evaluateCondition(String name, String cond, MbengNode thisNode) {
    	try {
			MbengRuleSet ruleset = compileMagicRule(name, cond, MbengRuleSet.RULESET_COND);
			MbengRuntime runtime = new MbengRuntime(ruleset, new MbengMDWLogger(logger));
//			runtime.bind("datadoc", datadoc);
			runtime.bind("$", datadoc);
			if (thisNode!=null) runtime.bind_if_exists("this", thisNode);
			return runtime.verify();
		} catch (MbengException e) {
			logger.exception("[form "+this.formName + " cond " + name + "]", "failed to compile condition", e);
			return false;
		}
    }

//    protected String evaluateExpression(String name, String expression) {
//    	try {
//    		MbengRuleSet ruleset = compileMagicRule(name, expression, MbengRuleSet.RULESET_EXPR);
//    		MbengRuntime runtime = new MbengRuntime(ruleset, new MbengMDWLogger(logger));
////    		runtime.bind("datadoc", datadoc);
//			runtime.bind("$", datadoc);
//    		return runtime.evaluate();
//    	} catch (MbengException e) {
//			logger.exception("[form "+this.formName + " expr " + name + "]", "failed to compile expression", e);
//			return null;
//		}
//    }

    protected String getMaskValidator(String validators) {
    	List<String> vs = stringToList(validators, ';');
    	for (String v : vs) {
    		if (v.startsWith("mask")) {
    			int openPar = v.indexOf('(');
    	    	int closePar = v.lastIndexOf(')');
    	    	if (openPar>0 && closePar>0)
    	    		return v.substring(openPar+1,closePar);
    	    	else return null;
    		}
    	}
    	return null;
    }

    protected String getLengthValidator(String validators) {
    	List<String> vs = stringToList(validators, ';');
    	for (String v : vs) {
    		if (v.startsWith("length")) {
    			int openPar = v.indexOf('(');
    	    	int closePar = v.lastIndexOf(')');
    	    	if (openPar<0 || closePar<0) return null;
    	    	v = v.substring(openPar+1,closePar);
        		int k = v.indexOf(',');
        		String max;
        		if (k>0) {
        			max = v.substring(k+1).trim();
        		} else {
        			max = v.trim();
        		}
        		return max;
    		}
    	}
    	return null;
    }

    protected List<String> stringToList(String inputStr, char delimiter) {
        List<String> list = new ArrayList<String>();
        if (inputStr==null || inputStr.length()==0) return list;
        StringBuffer sb = new StringBuffer();
        int i=0, n = inputStr.length();
        boolean escaped = false;
        char ch;
        while (i<n) {
            ch = inputStr.charAt(i);
            if (escaped) {
                sb.append(ch);
                escaped = false;
            } else if (ch=='\\') {
                escaped = true;
            } else if (ch==delimiter) {
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
            i++;
        }
        list.add(sb.toString());
        return list;
    }

    /**
     * Return true for both classic and autoform tasks
     * @return
     */
    private boolean isClassicTask() {
    	return variables!=null;
    }

    protected void generateDialog(String formName, String tableId, ResourceLoader loader, StringBuffer sb) {
    	String dialogId = formName;
    	try {
    		if (!tableId.equals("NOTTABLE")) {
    			MbengNode tableNode = tables.get(tableId);
    			if (tableNode==null) {
    				sb.append("<div id='").append(dialogId)
    					.append("' style='display:none;width:540px;height:120px;' class='form-dialog'>\n");
    				sb.append("Error: there is no table with ID " + tableId);
    				sb.append("</div>\n");
    				widgetInitialization.add("  $('#" + dialogId + "').dialog({autoOpen: false, modal: true});\n");
    			} else {
    				int width = 600;
    				int height = 100;
    				sb.append("<div id='").append(dialogId)
						.append("' style='display:none;' class='form-dialog'>\n");
    				sb.append("<table>\n");
    				for (MbengNode child=tableNode.getFirstChild(); child!=null; child=child.getNextSibling()) {
    					String collabel = child.getAttribute(FormConstants.FORMATTR_LABEL);
    					sb.append("<tr><td>").append(collabel).append("</td><td>");
    		        	String style_str = child.getAttribute(FormConstants.FORMATTR_COLUMN_STYLE);
    		        	Map<String,String> styles = StringHelper.parseMap(style_str);
    					String style = styles.get("style");
    					String editable = child.getAttribute(FormConstants.FORMATTR_EDITABLE);
    		        	if ("textarea".equalsIgnoreCase(style)) {
    		        		String v = styles.get("height");
    		        		int h = v==null?60:Integer.parseInt(v);
    		        		if ("false".equalsIgnoreCase(editable)) {
	    						sb.append("<span class='dialogDataOut' style='width:400px;height:"
	    								+ h + "px;' data='")
									.append(child.getAttribute(FormConstants.FORMATTR_DATA)).append("'></span>");
	    					} else if ("when new".equalsIgnoreCase(editable)) {
	    						sb.append("<textarea class='dialogDataOut dialogDataIn dialogDataNew' style='width:400px;height:"
	    								+ h + "px;' data='")
									.append(child.getAttribute(FormConstants.FORMATTR_DATA)).append("'></textarea>");
	    					} else {
	    						sb.append("<textarea class='dialogDataOut dialogDataIn' style='width:400px;height:"
	    								+ h + "px;' data='")
									.append(child.getAttribute(FormConstants.FORMATTR_DATA)).append("'></textarea>");
	    					}
    		        		height += h + 5;
    		        	} else {
	    					if ("false".equalsIgnoreCase(editable)) {
	    						sb.append("<span class='dialogDataOut' data='")
									.append(child.getAttribute(FormConstants.FORMATTR_DATA)).append("'></span>");
	    					} else if ("when new".equalsIgnoreCase(editable)) {
	    						sb.append("<input class='dialogDataOut dialogDataIn dialogDataNew' style='width:400px;' data='")
									.append(child.getAttribute(FormConstants.FORMATTR_DATA)).append("'/>");
	    					} else {
	    						sb.append("<input class='dialogDataOut dialogDataIn' style='width:400px;' data='")
									.append(child.getAttribute(FormConstants.FORMATTR_DATA)).append("'/>");
	    					}
	    					height += 25;
    		        	}
    					sb.append("</td></tr>\n");
    				}
    				sb.append("<tr><td colspan='2' align='center'>\n");
    				sb.append("<input type='button' value='Save' onclick='table_dialog_ok(\"" + dialogId + "\")'/>\n");
    				sb.append("<input type='button' value='Cancel' onclick='dialog_cancel(\"" + dialogId + "\")'/>\n");
    				sb.append("</td></tr>\n");
    				sb.append("</table>\n");
    				sb.append("</div>\n");
    				widgetInitialization.add("  $('#" + dialogId + "').dialog({autoOpen: false, modal: true, width: " +
    						width + ", height: " + height + "});\n");
    			}
    		} else if (formName.startsWith("html:")) {
				formName = formName.substring(5);
				RuleSetVO ruleset = loader.getResource(formName, RuleSetVO.HTML, 0);
				sb.append("<div id='").append(dialogId)
						.append("' style='display:none;' class='form-dialog'>\n");
				this.generateSub(ruleset.getRuleSet(), null, loader);
				sb.append("</div>\n");
				widgetInitialization.add("  $('#" + dialogId + "').dialog({autoOpen: false, modal: true});\n");
			} else {
				RuleSetVO ruleset = loader.getResource(formName, RuleSetVO.FORM, 0);
				MbengDocument dialogFormDef = (MbengDocument)ruleset.getCompiledObject();
				if (dialogFormDef==null) {
					FormatDom fmter = new FormatDom();
					dialogFormDef = new DomDocument();
					fmter.load(dialogFormDef, ruleset.getRuleSet());
					ruleset.setCompiledObject(dialogFormDef);
				}
				Rectangle vr = determine_vr(dialogFormDef.getRootNode(), 0, 0, 800, 600);
				sb.append("<div id='").append(dialogId)
					.append("' style='display:none;width:").append(vr.width).append("px;height:")
					.append(vr.height).append("px;' class='form-dialog'>\n");
				final int lx_default = 12;
				String saveFormName = this.formName;
				this.formName = formName;
				this.isDialog = true;
				create_children(dialogFormDef.getRootNode(), lx_default, sb, -2);
				this.formName = saveFormName;
				this.isDialog = false;
				sb.append("</div>\n");
				widgetInitialization.add("  $('#" + dialogId + "').dialog({autoOpen: false, modal: true, width: " +
						vr.width + ", height: " + vr.height + "});\n");
			}
		} catch (Exception e) {
			logger.exception("w.", "Failed to load dialog definition", e);
		}
    }

}
