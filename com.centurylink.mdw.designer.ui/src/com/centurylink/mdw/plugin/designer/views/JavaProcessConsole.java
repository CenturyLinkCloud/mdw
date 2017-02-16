/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import java.util.ResourceBundle;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.actions.ClearOutputAction;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.IUpdate;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.server.JavaSourceHyperlink;

/**
 * Console view for external java processes. Runtime output is displayed here.
 * Provides actions for copy/scroll-lock, etc. Also handles mouse events to
 * display source modules as links whose click action opens the source file to
 * the appropriate line number (in stack traces, etc).
 */
public class JavaProcessConsole extends ViewPart implements IAdaptable, IFindReplaceTarget,
        MouseListener, MouseTrackListener, MouseMoveListener, PaintListener {
    private IJavaProject javaProject;

    public IJavaProject getJavaProject() {
        return javaProject;
    }

    public void setJavaProject(IJavaProject jp) {
        javaProject = jp;
    }

    private static String consoleId = null;

    public static String getConsoleId() {
        return consoleId;
    }

    public static void setConsoleId(String s) {
        consoleId = s;
    }

    public static final int LINE_TYPE_NORMAL = 0;
    public static final int LINE_TYPE_STATUS = 1 << 1;
    public static final int LINE_TYPE_ERROR = 1 << 2;
    public static final int LINE_TYPE_HIGHLIGHT = 1 << 3;

    private int bufferSize;

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int i) {
        bufferSize = i;
    }

    // actions
    private Action copyAction;
    private Action selectAllAction;
    private Action scrollLockAction;
    private boolean scrollLocked = false;
    private ClearOutputAction clearOutputAction;
    private FindReplaceAction findReplaceAction;

    // resources
    @SuppressWarnings("unused")
    private static Color cyan, magenta, red, white;
    private Cursor handCursor;
    private Cursor textCursor;

    private TextViewer viewer;

    public TextViewer getViewer() {
        return viewer;
    }

    private JavaSourceHyperlink hyperLink;

    public StyledText getTextWidget() {
        return viewer.getTextWidget();
    }

    public IDocument getDocument() {
        return viewer.getDocument();
    }

    /**
     * Gets a handle to a runtime instance of the console. This method must be
     * run before show() or clear().
     *
     * @param id
     *            the console_id of the console to find
     * @return the console
     */
    public static JavaProcessConsole find(String id) {
        setConsoleId(id);
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        JavaProcessConsole console = (JavaProcessConsole) page.findView(id);

        if (console == null) {
            show();
            console = (JavaProcessConsole) page.findView(id);
            setConsoleId(id);
            return console;
        }
        else {
            setConsoleId(id);
            return console;
        }
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPart#createPartControl(Composite)
     */
    public void createPartControl(Composite parent) {
        viewer = new TextViewer(parent, SWT.V_SCROLL | SWT.H_SCROLL);
        GridData viewerData = new GridData(GridData.FILL_BOTH);
        getViewer().getControl().setLayoutData(viewerData);
        getViewer().setEditable(false);
        getViewer().enableOperation(TextViewer.COPY, true);
        getViewer().setDocument(new Document());

        // colors
        cyan = getViewer().getControl().getDisplay().getSystemColor(SWT.COLOR_CYAN);
        magenta = getViewer().getControl().getDisplay().getSystemColor(SWT.COLOR_MAGENTA);
        red = getViewer().getControl().getDisplay().getSystemColor(SWT.COLOR_RED);
        white = getViewer().getControl().getDisplay().getSystemColor(SWT.COLOR_WHITE);

        // set up the widget
        getTextWidget().addMouseTrackListener(this);
        getTextWidget().addPaintListener(this);

        // create actions
        createActions();
        // add the global action handlers
        addActionHandlers();

        createMenu();
        createToolbar();
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPart#setFocus()
     */
    public void setFocus() {
    }

    /**
     * show the console view based on the id set in find()
     */
    protected static void show() {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            page.showView(getConsoleId());
        }
        catch (NullPointerException ex) {
            // Eclipse Mars Bug:
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=473541
            // TODO Remove when fixed.
            PluginMessages.log(ex);
        }
        catch (PartInitException ex) {
            PluginMessages.log(ex);
        }
    }

    private StyleRange[] styleRanges;
    private StringBuffer buffer = new StringBuffer();
    private int buffed;

    /**
     * print output to the console
     *
     * @param text
     *            - the string to print
     * @param lineType
     *            - style attributes
     */
    public void print(String text, int lineType) {
        Color foreground = null;
        Color background = null;

        if (lineType != LINE_TYPE_NORMAL) {
            if ((LINE_TYPE_STATUS & lineType) == LINE_TYPE_STATUS
                    || (LINE_TYPE_HIGHLIGHT & lineType) == LINE_TYPE_HIGHLIGHT)
                show();

            if ((LINE_TYPE_ERROR & lineType) == LINE_TYPE_ERROR) {
                foreground = red;
            }
            if ((LINE_TYPE_STATUS & lineType) == LINE_TYPE_STATUS) {
                background = cyan;
            }
            else if ((LINE_TYPE_HIGHLIGHT & lineType) == LINE_TYPE_HIGHLIGHT) {
                background = magenta;
            }
        }

        StyledText st = getViewer().getTextWidget();

        synchronized (buffer) {
            if (lineType != LINE_TYPE_NORMAL) {
                int lineOffset = st.getOffsetAtLine(st.getLineCount() - 1) + buffed;

                if (st.getText().length() + buffed == 0)
                    styleRanges = null;

                StyleRange sr = new StyleRange(lineOffset, text.length(), foreground, background);
                if (styleRanges == null || styleRanges.length == 0)
                    styleRanges = new StyleRange[] { sr };
                else
                    styleRanges = (StyleRange[]) PluginUtil.addToArray(sr, styleRanges);
            }

            buffer.append(text);
            buffed += text.length();
        }

// TODO: honor buffer size preference
// int overflow = fullText.length() + text.length() - getBufferSize();
// if (overflow > 0)
// {
// int trimToIdx = fullText.indexOf("\n", overflow);
// if (trimToIdx > 0)
// {
// st.replaceTextRange(0, trimToIdx, "");
// }
// }

        printJob();
    }

    private UIJob queueJob;

    private UIJob getQueueJob() {
        if (queueJob == null) {
            queueJob = new UIJob("JavaProcessConsole") {
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    StyledText st = getViewer().getTextWidget();
                    synchronized (buffer) {
                        st.append(buffer.toString());
                        buffer.setLength(0);
                        buffed = 0;

                        st.setStyleRanges(styleRanges);

                        // scroll to the bottom
                        if (!scrollLockAction.isChecked()) {
                            int textLength = st.getText().length();
                            if (textLength > 0) {
                                st.setCaretOffset(textLength);
                                st.showSelection();
                            }
                        }

                        // update the find replace action since the text has
                        // changed
                        IUpdate findReplace = (IUpdate) findReplaceAction;
                        if (findReplace != null)
                            findReplace.update();
                    }

                    return Status.OK_STATUS;
                }
            };

            queueJob.setSystem(true);
            queueJob.setPriority(Job.INTERACTIVE);
            // queueJob.setRule(console.getSchedulingRule());
        }

        return queueJob;
    }

    public void printJob() {
        if (buffed > 500)
            getQueueJob().schedule();
        else
            getQueueJob().schedule(25);
    }

    /**
     * clear the console
     */
    public void clear() {
        show();
        StyledText st = getViewer().getTextWidget();
        synchronized (buffer) {
            st.setText("");
            styleRanges = new StyleRange[0];
            buffer.setLength(0);
            buffed = 0;
        }
    }

    /**
     * add action handlers for the view
     */
    private void createActions() {
        // copy
        copyAction = new Action("Copy") {
            public void run() {
                getViewer().getTextWidget().copy();
            }
        };
        copyAction.setImageDescriptor(MdwPlugin.getImageDescriptor("icons/copy.gif"));
        copyAction.setToolTipText("Copy");

        // select all
        selectAllAction = new Action("Select All") {
            public void run() {
                getViewer().getTextWidget().selectAll();
                getViewer().setSelection(getViewer().getSelection());
            }
        };
        selectAllAction.setImageDescriptor(MdwPlugin.getImageDescriptor("icons/select_all.gif"));
        selectAllAction.setToolTipText("Select All");

        // search
        ResourceBundle bundle = ResourceBundle
                .getBundle("org.eclipse.debug.internal.ui.views.DebugUIViewsMessages");
        findReplaceAction = new FindReplaceAction(bundle, null, this);
        findReplaceAction.setImageDescriptor(MdwPlugin.getImageDescriptor("icons/search.gif"));
        findReplaceAction.setToolTipText("Search");

        // clear output
        clearOutputAction = new ClearOutputAction(getViewer()) {
            public void run() {
                clear();
            }
        };

        // scroll lock
        scrollLockAction = new Action("Scroll Lock") {
            public void run() {
                scrollLocked = !scrollLocked;
                scrollLockAction.setChecked(scrollLocked);
            }
        };
        scrollLockAction.setImageDescriptor(MdwPlugin.getImageDescriptor("icons/lock.gif"));
        scrollLockAction.setToolTipText("Scroll Lock");
        scrollLockAction.setChecked(false);
    }

    /**
     * add action handlers for the view
     */
    private void addActionHandlers() {
        // add selection listener for enabling copying
        getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                updateActionEnablement();
            }
        });

        IActionBars bars = getViewSite().getActionBars();
        bars.setGlobalActionHandler("copy", copyAction);
        bars.setGlobalActionHandler("selectAll", selectAllAction);
        bars.setGlobalActionHandler("find", findReplaceAction);
        bars.setGlobalActionHandler("scroll_lock", scrollLockAction);

        updateActionEnablement();
    }

    /**
     * determine whether to enable actions
     */
    private void updateActionEnablement() {
        String selText = getViewer().getTextWidget().getSelectionText();
        copyAction.setEnabled(selText.length() > 0);
    }

    /**
     * create menu
     */
    private void createMenu() {
        IMenuManager mgr = getViewSite().getActionBars().getMenuManager();
        mgr.add(copyAction);
        mgr.add(selectAllAction);
        mgr.add(clearOutputAction);
        mgr.add(scrollLockAction);
        mgr.add(findReplaceAction);
    }

    /**
     * create toolbar
     */
    private void createToolbar() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
        mgr.add(copyAction);
        // mgr.add(selectAllAction);
        mgr.add(clearOutputAction);
        mgr.add(scrollLockAction);
        mgr.add(findReplaceAction);
    }

    /**
     * dispose of the text viewer and its resources
     */
    public void dispose() {
        Control control = getViewer().getTextWidget();
        if (control != null) {
            control.removeMouseTrackListener(this);
            control.removePaintListener(this);
        }
        if (handCursor != null) {
            handCursor.dispose();
        }
        if (textCursor != null) {
            textCursor.dispose();
        }
    }

    // methods for the IFindReplaceTarget interface
    public boolean canPerformFind() {
        return true;
    }

    public boolean isEditable() {
        return false;
    }

    public void replaceSelection(String text) {
    }

    public int findAndSelect(int widgetOffset, String findString, boolean searchForward,
            boolean caseSensitive, boolean wholeWord) {
        if (getViewer().getTextWidget() == null)
            return -1;

        try {
            int fromOffset = 0;
            if (widgetOffset == -1) {
                fromOffset = -1;
            }
            else {
                fromOffset = getViewer().getTextWidget().getCaretOffset();
                if (getViewer().getTextWidget().getSelectionCount() > 0)
                    fromOffset++;
                if (fromOffset >= getViewer().getDocument().getLength() && fromOffset > 0)
                    fromOffset--;
            }

            IRegion matchRegion = new FindReplaceDocumentAdapter(getViewer().getDocument())
                    .find(fromOffset, findString, searchForward, caseSensitive, wholeWord, false);
            if (matchRegion != null) {
                int widgetPos = matchRegion.getOffset();
                int length = matchRegion.getLength();
                getViewer().getTextWidget().setSelectionRange(widgetPos, length);
                getViewer().setSelection(getViewer().getSelection(), true);

                return getViewer().widgetOffset2ModelOffset(widgetPos);
            }
        }
        catch (BadLocationException ex) {
            PluginMessages.log(ex);
        }
        return -1;
    }

    public Point getSelection() {
        return getViewer().getTextWidget().getSelection();
    }

    public String getSelectionText() {
        return getViewer().getTextWidget().getSelectionText();
    }

    // methods for MouseListener interface
    public void mouseDoubleClick(MouseEvent e) {
    }

    public void mouseDown(MouseEvent e) {
    }

    public void mouseUp(MouseEvent e) {
        if (hyperLink != null) {
            String selection = getTextWidget().getSelectionText();
            if (selection.length() <= 0) {
                if (e.button == 1) {
                    hyperLink.linkActivated();
                }
            }
        }
    }

    // methods for MouseTrackListener interface
    public void mouseEnter(MouseEvent e) {
        getTextWidget().addMouseMoveListener(this);
    }

    public void mouseExit(MouseEvent e) {
        getTextWidget().removeMouseMoveListener(this);
        if (hyperLink != null) {
            linkExited(hyperLink);
        }
    }

    public void mouseHover(MouseEvent e) {
    }

    protected void linkEntered(JavaSourceHyperlink link) {
        Control control = getViewer().getTextWidget();
        control.setRedraw(false);
        if (hyperLink != null) {
            linkExited(hyperLink);
        }
        hyperLink = link;
        hyperLink.linkEntered();
        control.setCursor(getHandCursor());
        control.setRedraw(true);
        control.redraw();
        control.addMouseListener(this);
    }

    protected void linkExited(JavaSourceHyperlink link) {
        link.linkExited();
        hyperLink = null;
        Control control = getViewer().getTextWidget();
        control.setCursor(getTextCursor());
        control.redraw();
        control.removeMouseListener(this);
    }

    @SuppressWarnings("restriction")
    protected Cursor getHandCursor() {
        if (handCursor == null) {
            handCursor = new Cursor(
                    org.eclipse.debug.internal.ui.DebugUIPlugin.getStandardDisplay(),
                    SWT.CURSOR_HAND);
        }
        return handCursor;
    }

    @SuppressWarnings("restriction")
    protected Cursor getTextCursor() {
        if (textCursor == null) {
            textCursor = new Cursor(
                    org.eclipse.debug.internal.ui.DebugUIPlugin.getStandardDisplay(),
                    SWT.CURSOR_IBEAM);
        }
        return textCursor;
    }

    // method for the MouseMoveListener interface
    public void mouseMove(MouseEvent e) {
        mouseOffset = -1;
        try {
            Point p = new Point(e.x, e.y);
            mouseOffset = getTextWidget().getOffsetAtLocation(p);
        }
        catch (IllegalArgumentException ex) {
            // out of the document range
        }
        updateLinks(mouseOffset);
    }

    int mouseOffset;

    /**
     * the cursor has just moved to the given offset, the mouse has hovered over
     * the given offset, so update link rendering
     *
     * @param offset
     */
    protected void updateLinks(int offset) {
        if (offset >= 0) {
            JavaSourceHyperlink link = getHyperlink(offset);
            if (link != null) {
                if (link.equals(hyperLink)) {
                    return;
                }
                else {
                    linkEntered(link);
                    return;
                }
            }
        }
        if (hyperLink != null) {
            linkExited(hyperLink);
        }
    }

    /**
     * returns a JavaSourceHyperlink if the hover location is appropriate
     *
     * @param offset
     * @return the link or null if no link should appear
     */
    public JavaSourceHyperlink getHyperlink(int offset) {
        if (offset >= 0) {
            StyledText widget = getViewer().getTextWidget();
            int line = widget.getLineAtOffset(offset);
            String lineText = getLineText(line);
            JavaSourceHyperlink link = JavaSourceHyperlink.create(lineText, getJavaProject());
            return link;
        }
        return null;
    }

    /**
     * parse a String with delimiters to find the text for a given line
     *
     * @param line
     *            - line number
     * @return the string for the given line
     */
    private String getLineText(int line) {
        String text = getViewer().getTextWidget().getText();
        int count = 0;
        int start = 0;
        while (start != -1 && count < line) {
            start = text.indexOf('\n', start + 1);
            count++;
        }
        String lineText = text.substring(start + 1, text.indexOf('\n', start + 1));
        return lineText.endsWith("\r") ? lineText.substring(0, lineText.length() - 1) : lineText;
    }

    /**
     * paints the control
     *
     * @param e
     */
    public void paintControl(PaintEvent e) {
        if (hyperLink != null) {
            StyledText widget = getViewer().getTextWidget();
            int line = widget.getLineAtOffset(mouseOffset);
            try {
                IRegion lineRegion = getDocument().getLineInformation(line);
                int lineStart = lineRegion.getOffset();
                Color fgColor = e.gc.getForeground();
                e.gc.setForeground(red);
                int linkStart = lineStart + hyperLink.getLinkStart();
                int linkEnd = lineStart + hyperLink.getLinkEnd();
                Point p1 = getTextWidget().getLocationAtOffset(linkStart);
                Point p2 = getTextWidget().getLocationAtOffset(linkEnd);
                FontMetrics metrics = e.gc.getFontMetrics();
                int height = metrics.getHeight();
                e.gc.drawLine(p1.x, p1.y + height, p2.x, p2.y + height);
                e.gc.setForeground(fgColor);
            }
            catch (BadLocationException ex) {
            }
        }
    }
}