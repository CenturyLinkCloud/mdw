/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.views;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.internal.junit.ui.CounterPanel;
import org.eclipse.jdt.internal.junit.ui.JUnitProgressBar;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PageBook;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.designer.testing.LoadTestLogMonitor;
import com.centurylink.mdw.designer.testing.LogMessageMonitor;
import com.centurylink.mdw.designer.testing.MasterRequestListener;
import com.centurylink.mdw.designer.testing.StubServer;
import com.centurylink.mdw.designer.testing.StubServer.Stubber;
import com.centurylink.mdw.designer.testing.TestCase;
import com.centurylink.mdw.designer.testing.TestCaseRun;
import com.centurylink.mdw.designer.testing.ThreadPool;
import com.centurylink.mdw.model.value.activity.ActivityRuntimeContext;
import com.centurylink.mdw.model.value.activity.ActivityStubRequest;
import com.centurylink.mdw.model.value.activity.ActivityStubResponse;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.event.AdapterStubRequest;
import com.centurylink.mdw.model.value.event.AdapterStubResponse;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.MessageConsole;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.PluginUtil;
import com.centurylink.mdw.plugin.actions.MdwMenuManager;
import com.centurylink.mdw.plugin.ant.TestResultsFormatter;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestCase;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestResults;
import com.centurylink.mdw.plugin.designer.model.AutomatedTestSuite;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent;
import com.centurylink.mdw.plugin.designer.model.ElementChangeEvent.ChangeType;
import com.centurylink.mdw.plugin.designer.model.LegacyExpectedResults;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowAssetFactory;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.launch.AutomatedTestLaunchShortcut;

@SuppressWarnings("restriction")
public class AutomatedTestView extends TestRunnerViewPart implements IMenuListener, MasterRequestListener
{
  public static final String VIEW_ID = "mdw.views.launch.automatedTest";
  private boolean locked;
  public boolean isLocked() { return locked; }

  private AutomatedTestSuite testSuite;
  public AutomatedTestSuite getTestSuite() { return testSuite; }
  public void setTestSuite(AutomatedTestSuite testSuite)
  {
    this.testSuite = testSuite;
    File resultsFile = testSuite.getProject().getFunctionTestResultsFile();
    actionGroup.enableFormatFunctionTestResults(resultsFile != null && resultsFile.exists());
  }

  private AutomatedTestSuite loadTestSuite;
  public AutomatedTestSuite getLoadTestSuite() { return loadTestSuite; }
  public void setLoadTestSuite(AutomatedTestSuite loadTestSuite)
  {
    this.loadTestSuite = loadTestSuite;
    File resultsFile = loadTestSuite.getProject().getLoadTestResultsFile();
    actionGroup.enableFormatLoadTestResults(resultsFile != null && resultsFile.exists());
  }

  private Display display;
  private Map<AutomatedTestCase,String> testCaseStatuses;
  private CounterData counterData;
  private ThreadPool threadPool;
  private DesignerProxy designerProxy;

  private CompatibleCounterPanel counterPanel;
  private JUnitProgressBar progressBar;
  private Composite counterComposite;
  private SashForm sashForm;
  private TreeViewer treeViewer;
  private Text outputText;

  private AutomatedTestActionGroup actionGroup;
  private Menu contextMenu;
  private WorkflowElement selectedItem;
  public WorkflowElement getSelectedItem() { return selectedItem; }

  private Map<String,TestCaseRun> masterRequestRunMap;

  @Override
  public void createPartControl(Composite parent)
  {
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    parent.setLayout(gridLayout);

    counterComposite = createProgressCountPanel(parent);
    counterComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
    SashForm sashForm = createSashForm(parent);
    sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

    // action group
    actionGroup = new AutomatedTestActionGroup(this);
    IActionBars actionBars = getViewSite().getActionBars();
    actionGroup.fillActionBars(actionBars);

    // context menu
    MenuManager menuMgr = new MdwMenuManager("Automated Test");
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(this);
    contextMenu = menuMgr.createContextMenu(treeViewer.getTree());
    treeViewer.getTree().setMenu(contextMenu);
    getSite().registerContextMenu(menuMgr, treeViewer);

    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, MdwPlugin.getPluginId() + ".toolbox_help");
  }

  protected Composite createProgressCountPanel(Composite parent)
  {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    composite.setLayout(layout);
    layout.numColumns = 2;

    counterPanel = new CompatibleCounterPanel(composite);
    counterPanel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
    progressBar = new JUnitProgressBar(composite);
    progressBar.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
    return composite;
  }

  private AutomatedTestContentProvider contentProvider = new AutomatedTestContentProvider();
  private SashForm createSashForm(Composite parent)
  {
    sashForm = new SashForm(parent, SWT.HORIZONTAL);

    ViewForm results = new ViewForm(sashForm, SWT.NONE);

    PageBook viewerBook = new PageBook(results, SWT.BORDER);

    treeViewer = new TreeViewer(viewerBook, SWT.V_SCROLL | SWT.SINGLE);
    treeViewer.setUseHashlookup(true);
    treeViewer.setContentProvider(contentProvider);
    treeViewer.setLabelProvider(new AutomatedTestLabelProvider());
    treeViewer.addOpenListener(new IOpenListener()
    {
      public void open(OpenEvent event)
      {
        actionGroup.getActionHandler().open(selectedItem);
      }
    });
    treeViewer.addSelectionChangedListener(new ISelectionChangedListener()
    {
      public void selectionChanged(SelectionChangedEvent event)
      {
        handleSelectionChanged((IStructuredSelection)event.getSelection());
      }
    });
    viewerBook.showPage(treeViewer.getTree());
    results.setContent(viewerBook);

    ViewForm output = new ViewForm(sashForm, SWT.NONE);

    outputText = new Text(output, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
    outputText.setFont(JFaceResources.getTextFont());
    output.setContent(outputText);

    sashForm.setWeights(new int[] {33, 67});
    return sashForm;
  }

  private LogMessageMonitor monitor;

  private LoadTestLogMonitor monitorObj;

  public void runTests()
  {
    // TODO: currently mixed execution of Gherkin/Groovy scripts
    // fails to start LogMessageMonitor in Designer VM (otherwise will conflict with Cucumber-JVM)
    boolean hasGherkin = false;
    for (AutomatedTestCase rtc : testSuite.getTestCases())
    {
      if (rtc.isGherkin())
        hasGherkin = true;
    }

    try
    {
      prepForRun();
      counterData = new CounterData(testSuite.getTestCases().size());
      updateCounterPanel(0, 0, 0, true);
      monitor = null;
      if (!hasGherkin)
        monitor = new LogMessageMonitor(testSuite.getProject().getDesignerProxy().getDesignerDataAccess(), testSuite.getProject().isOldNamespaces());

      String msg = designerProxy.checkForServerDbMismatch();
      if (msg != null)
      {
        MessageDialog.openError(getSite().getShell(), "Server DB Mismatch", msg);
        handleStop();
        return;
      }

      if (StubServer.isRunning())
        StubServer.stop();
      if (testSuite.isStubbing() && !hasGherkin)
      {
        StubServer.Stubber stubber = new TestStubber();
        int port = testSuite.getProject().getServerSettings().getStubServerPort();
        StubServer.start(testSuite.getProject().getDesignerProxy().getRestfulServer(), port, stubber,testSuite.getProject().isOldNamespaces());
      }
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Automated Test", testSuite.getProject());
      handleStop();
      return;
    }

    Thread background = new Thread(new Runnable()
    {
      public void run()
      {
        testCaseStatuses = new HashMap<AutomatedTestCase,String>();
        updateProgressBar(5, false, false);
        threadPool = new ThreadPool(testSuite.getThreadCount());
        PrintStream printStream = null;

        updateProgressBar(10, false, false);

        if (monitor != null)
          monitor.start(true);
        for (AutomatedTestCase testCase : testSuite.getTestCases())
        {
          if (!testSuite.isRunning())
            continue;

          File resultDir = testCase.getResultsDir();
          try
          {
            File executeLog = testCase.getOutputFile();
            deleteFile(executeLog);
            if (!executeLog.getParentFile().exists() && !executeLog.getParentFile().mkdirs())
                throw new IOException("Unable to create test run directory: " + executeLog.getParentFile());
            PrintStream log = new PrintStream(executeLog);

            ImageDescriptor icon = MdwPlugin.getImageDescriptor("icons/auto_test.gif");
            MessageConsole console = MessageConsole.findConsole("Tests", icon, display);
            printStream = new PrintStream(console.newFileConsoleOutputStream(executeLog));

            TestCaseRun testCaseRun = designerProxy.prepareTestCase(testCase, 0, resultDir, testSuite.isCreateReplaceResults(), testSuite.isVerbose(), log, monitor, testSuite.isSingleServer(), testSuite.isStubbing());

            masterRequestRunMap.put(testCaseRun.getMasterRequestId(), testCaseRun);
            testCaseRun.setMasterRequestListener(AutomatedTestView.this);

            threadPool.execute(testCaseRun);

            updateTreeAndProgressBar(false);
            try
            {
              Thread.sleep(testSuite.getThreadInterval() * 1000);
            }
            catch (InterruptedException ex) { }
          }
          catch (Exception ex)
          {
            PluginMessages.log(ex);
            testCase.setErrored();
            updateTreeAndProgressBar(true);
            testSuite.setRunning(false);
            if (printStream != null)
              ex.printStackTrace(printStream);
            handleStop();
            printStream.close();
            return;
          }
        }

        while (!threadPool.isTerminated())
        {
          // update the tree and the progress bar
          updateTreeAndProgressBar(false);
          if (testSuite.isFinished())
            threadPool.shutdown();

          try
          {
            Thread.sleep(2500);
          }
          catch (InterruptedException e) { }
        }

        updateTreeAndProgressBar(true);
        testSuite.setRunning(false);
        actionGroup.enableStopAction(false);
        actionGroup.enableRerunAction(true);
        File resultsFile = testSuite.getProject().getFunctionTestResultsFile();
        actionGroup.enableFormatFunctionTestResults(resultsFile != null && resultsFile.exists());
        locked = false;
        if (monitor != null && monitor.isBound())
          monitor.shutdown();
        if (printStream != null)
          printStream.close();
      }
    });

    background.start();
  }

  public void runLoadTests()
  {
    int totalRunCount = 0;
    for (AutomatedTestCase testCase : testSuite.getTestCases())
      totalRunCount += testCase.getRunCount();
    testSuite.setRunCount(totalRunCount);
    try
    {
      prepForRun();
      counterData = new CounterData(testSuite.getRunCount());
      updateCounterPanel(0, 0, 0, true);
      monitorObj = new LoadTestLogMonitor(testSuite.getProject().getDesignerProxy().getDesignerDataAccess(), masterRequestRunMap, null);

      String msg = designerProxy.checkForServerDbMismatch();
      if (msg != null)
      {
        MessageDialog.openError(getSite().getShell(), "Server DB Mismatch", msg);
        handleStop();
        return;
      }

      if (testSuite.isStubbing())
      {
        StubServer.Stubber stubber = new TestStubber();
        if (StubServer.isRunning())
          StubServer.stop();
        int port = testSuite.getProject().getServerSettings().getStubServerPort();
        StubServer.start(testSuite.getProject().getDesignerProxy().getRestfulServer(), port, stubber, testSuite.getProject().isOldNamespaces());
      }
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Load Test", testSuite.getProject());
      handleStop();
      return;
    }

    Thread background = new Thread(new Runnable()
    {
      public void run()
      {
        testCaseStatuses = new HashMap<AutomatedTestCase,String>();
        updateProgressBar(5, false, false);

        threadPool = new ThreadPool(testSuite.getThreadCount());

        long startTime = System.currentTimeMillis();
        int totalPrepared = 0;
        int totalCompleted = 0;
        try
        {
          startTime = System.currentTimeMillis();
          monitorObj.start(true);

          for (AutomatedTestCase testCase : testSuite.getTestCases())
          {
            File resultDir = testCase.getResultsDir();
            File executeLog = new File(resultDir.getPath() + "/execute.log");
            int ct = testCase.getRunCount();
            for (int k = 0; k < ct; k++)
            {
              deleteFile(executeLog);
              if (!executeLog.getParentFile().exists() && !executeLog.getParentFile().mkdirs())
                  throw new IOException("Unable to create test run directory: " + executeLog.getParentFile());
              PrintStream log = new PrintStream(executeLog);

              TestCaseRun testCaseRun = designerProxy.prepareTestCase(testCase, k, resultDir, false, testSuite.isVerbose(), log, monitorObj, testSuite.isSingleServer(), testSuite.isStubbing());
              testCase.getTestCase().setNumberPrepared(testCase.getTestCase().getNumberPrepared() + 1);
              masterRequestRunMap.put(testCaseRun.getMasterRequestId(), testCaseRun);
              threadPool.execute(testCaseRun);
              totalPrepared++;
              try
              {
                Thread.sleep(testSuite.getThreadInterval() * 1000);
              }
              catch (InterruptedException ex) { }
            }
          }

          updateProgressBar(10, false, false);
          updateLoadTestProgress(totalCompleted, totalPrepared, false);
     //     startTime = System.currentTimeMillis(); - Incorrect - This is after all test cases REST messages have been "submitted" to designer's thread pool, no relation to server's commonThreadPool
          while (!threadPool.isTerminated())
          {
            // update the tree and the progress bar
            updateTreeAndProgressBar(false);
            if (testSuite.isFinished())
              threadPool.shutdown();

            try
            {
              Thread.sleep(1000);
            }
            catch (InterruptedException e) { }
          }

          testSuite.setRunning(false);
          actionGroup.enableStopAction(false);
          actionGroup.enableRerunAction(true);
          File resultsFile = testSuite.getProject().getLoadTestResultsFile();
          actionGroup.enableFormatLoadTestResults(resultsFile != null && resultsFile.exists());
        }
        catch (Exception ex)
        {
          ex.printStackTrace();
        }
        finally
        {
          if (monitorObj != null)
            monitorObj.shutdown();

          locked = false;
        }
        try
        {
          generateLoadTestReport(totalPrepared, totalCompleted, monitorObj, startTime, testSuite.getResultsDir().toString());
        }
        catch (FileNotFoundException e)
        {
          e.printStackTrace();
        }
      }
    });
    background.start();
  }

  private void generateLoadTestReport(int totalPrepared, int totalCompleted, LoadTestLogMonitor monitor,
      long startTime, String resultDirectory) throws FileNotFoundException
  {
    System.out.println("Generating LoadTestReport in Result directory::"+resultDirectory);
    StringBuffer sb = new StringBuffer();
    long endTime = System.currentTimeMillis();
    SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
    int totalActivityStarted = monitor.getNumberOfActivityStarted() ;
    int totalActivityCompleted = monitor.getNumberOfActivityCompleted() ;
    int totalProcessStarted = monitor.getNumberOfProcesses();
    String finalStartTime = df.format(new Date(startTime));
    String finalEndTime = df.format(new Date(endTime));
    sb.append("Total number of cases: ").append(totalPrepared).append('\n');
    sb.append("Total number of activities started: ").append(totalActivityStarted).append('\n');
    sb.append("Total number of activities completed: ").append(totalActivityCompleted).append('\n');
    sb.append("Start Time: ").append(finalStartTime).append('\n');
    sb.append("End Time: ").append(finalEndTime).append('\n');
    sb.append("Number of processes started: ").append(totalProcessStarted).append('\n');
    double speed = monitor.getNumberOfActivityStarted() * 3600000.0 / (endTime - startTime);
    sb.append("Activities per hour: ").append(speed).append('\n');
    String report = sb.toString();
    String reportFileName = resultDirectory + "/load_test_rpt.txt";
    PrintWriter writer = null;
    try
    {
      writer = new PrintWriter(reportFileName);
      writer.print(report);
    }
    finally
    {
      if (writer != null)
        writer.close();
    }
    testSuite.writeLoadTestResults(totalPrepared, totalCompleted, totalActivityStarted, totalActivityCompleted, finalStartTime, finalEndTime, totalProcessStarted, speed, resultDirectory);
  }

  public void prepForRun()
  {
    locked = true;
    display = getSite().getShell().getDisplay();

    actionGroup.enableRerunAction(false);
    actionGroup.enableStopAction(true);

    masterRequestRunMap = new ConcurrentHashMap<String,TestCaseRun>();

    for (AutomatedTestCase testCase : testSuite.getTestCases()) {
      testCase.setStatus(TestCase.STATUS_NOT_RUN);
      testSuite.getProject().fireTestCaseStatusChange(testCase, testCase.getStatus());
    }

    testSuite.setRunning(true);
    List<AutomatedTestSuite> rootElements = new ArrayList<AutomatedTestSuite>();
    rootElements.add(testSuite);
    treeViewer.setInput(rootElements);
    treeViewer.expandToLevel(3);

    BusyIndicator.showWhile(display, new Runnable()
    {
      public void run()
      {
        // can take a long time if the project has not been loaded
        designerProxy = testSuite.getProject().getDesignerProxy();
      }
    });
  }

  private synchronized void updateTreeAndProgressBar(boolean done)
  {
    int completedCases = 0;
    int erroredCases = 0;
    int failedCases = 0;
    int casePercents = 0;
    for (AutomatedTestCase testCase : testSuite.getTestCases())
    {
      String oldStatus = testCaseStatuses.get(testCase);
      boolean statusChanged = done || oldStatus == null || !oldStatus.equals(testCase.getStatus());
      if (statusChanged)
      {
        testSuite.getProject().fireTestCaseStatusChange(testCase, testCase.getStatus());
        testCaseStatuses.put(testCase, testCase.getStatus());
      }

      int casePercent = 0;

      if (testCase.isFinished())
      {
        if (statusChanged && !done && testSuite.isCreateReplaceResults() && testCase.isSuccess()) // only applies for VCS assets
        {
          if (testCase.isLegacy())
          {
            testCase.setTestCase(testCase.getTestCase()); // trigger reload expected results
            if (testCase.getLegacyExpectedResults() != null)
            {
              for (LegacyExpectedResults legacyResults : testCase.getLegacyExpectedResults())
              {
                legacyResults.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, legacyResults);
                testSuite.getProject().fireElementChangeEvent(ChangeType.ELEMENT_CREATE, legacyResults);
              }
            }
          }
          else
          {
            try
            {
              // create asset from newly-created results and refresh process explorer (only for VCS assets)
              String resAssetName = testCase.getTestCase().getCaseName() + RuleSetVO.getFileExtension(RuleSetVO.YAML);
              File resFile = new File(testSuite.getProject().getProjectDir() + "/" + testCase.getPackage().getVcsAssetPath() + "/" + resAssetName);
              if (resFile.exists())  // may not exist if test case does not launch any processes
              {
                byte[] bytes = PluginUtil.readFile(resFile);
                WorkflowAsset existing = testSuite.getProject().getAsset(testCase.getPackage().getName() + "/" + resAssetName);
                if (existing == null)
                {
                  // create (waiting a second or two for the results file to have been created)
                  RuleSetVO newRuleSet = new RuleSetVO();
                  newRuleSet.setName(resAssetName);
                  newRuleSet.setLanguage(RuleSetVO.YAML);
                  newRuleSet.setRaw(true);
                  newRuleSet.setRawFile(resFile);
                  newRuleSet.setRawContent(bytes);
                  WorkflowAsset newAsset = WorkflowAssetFactory.createAsset(newRuleSet, testCase.getPackage());
                  newAsset.setPackage(testCase.getPackage());
                  newAsset.setId(new Long(-1));
                  newAsset.setCreateUser(testSuite.getProject().getUser().getUsername());
                  newAsset.setLockingUser(testSuite.getProject().getUser().getUsername());
                  designerProxy.saveWorkflowAsset(newAsset, true);
                  testSuite.getProject().getDataAccess().getDesignerDataModel().addRuleSet(newAsset.getRuleSetVO());
                  newAsset.getPackage().addAsset(newAsset);
                  designerProxy.savePackage(newAsset.getPackage());
                  newAsset.fireElementChangeEvent(ChangeType.ELEMENT_CREATE, newAsset);
                }
                else
                {
                  // increment version
                  existing.getRuleSetVO().setRawContent(bytes);
                  existing.setVersion(existing.getNextMinorVersion());
                  existing.setRevisionComment("Auto-created");
                  testSuite.getProject().getDesignerProxy().saveWorkflowAsset(existing, true);
                  existing.fireElementChangeEvent(ChangeType.VERSION_CHANGE, existing.getVersion());
                }
              }
            }
            catch (Exception ex)
            {
              PluginMessages.uiError(ex, "Create/Replace Test Results", testSuite.getProject());
            }
          }
        }
        updateExpectedResults(testCase);
        completedCases++;
        casePercent = 100;
      }
      else
      {
        casePercent = testCase.getTotalSteps() == 0 ? 0 : (testCase.getStepsCompleted() * 100) / testCase.getTotalSteps();
      }
      casePercents += casePercent;

      if (testCase.isErrored())
        erroredCases++;
      else if (testCase.isFailed())
        failedCases++;

      if (statusChanged)
        try{
          testSuite.writeTestCaseResults(testCase);
        }catch(Exception ex){
          PluginMessages.uiError(ex, "Create/Replace Test Results", testSuite.getProject());
        }
    }

    if (done)
    {
      testSuite.setRunning(false);
      updateCounterPanel(testSuite.getTestCases().size(), erroredCases, failedCases, true);
      updateProgressBar(100, testSuite.hasErrors() || testSuite.hasFailures(), testSuite.isStopped());
    }
    else
    {
      int totalPercents = testSuite.getTestCases().size() * 100;
      updateCounterPanel(completedCases, erroredCases, failedCases, false);
      float percentComplete = totalPercents == 0 ? 0 : (casePercents * 100) / totalPercents;
      updateProgressBar(Math.round(percentComplete * 90/100) + 10, erroredCases + failedCases > 0, testSuite.isStopped());
    }

    refreshOutputFromSelection();
  }

  private void updateExpectedResults(final AutomatedTestCase testCase)
  {
    display.asyncExec(new Runnable()
    {
      public void run()
      {
        treeViewer.refresh(testCase, true);
      }
    });
  }

  private void updateLoadTestProgress(int done, int total, boolean stopped)
  {
    // 10 percent is already completed during prepare
    int percent = 10 + (done*90/total);
    updateProgressBar(percent, false, stopped);
    updateCounterPanel(done, 0, 0, stopped);

    System.out.println("Load Test Update");
    for (AutomatedTestCase testCase : testSuite.getTestCases())
    {
      String oldStatus = testCaseStatuses.get(testCase);
      boolean statusChanged = stopped || oldStatus == null || !oldStatus.equals(testCase.getStatus());
      if (statusChanged)
      {
        if (testCase.getStatus().equals(TestCase.STATUS_PASS))
          testCase.setStatus(TestCase.STATUS_STOP);
        System.out.println(testCase.getPath() + ": " + testCase.getStatus());
        // update test view but not process explorer
        contentProvider.elementChanged(new ElementChangeEvent(ChangeType.STATUS_CHANGE, testCase));
      }
    }
  }

  private void updateProgressBar(final int percent, final boolean hasErrors, final boolean stopped)
  {
    display.asyncExec(new Runnable()
    {
      public void run()
      {
        progressBar.reset(hasErrors, stopped, percent, 100);
      }
    });
  }

  private void updateCounterPanel(final int completed, final int errored, final int failed, final boolean force)
  {
    display.asyncExec(new Runnable()
    {
      public void run()
      {
        counterPanel.setTotal(counterData.total);
        if (force || counterData.completed != completed)
        {
          counterPanel.setRunValue(completed);
          counterData.completed = completed;
        }
        if (force || counterData.errored != errored)
        {
          counterPanel.setErrorValue(errored);
          counterData.errored = errored;
        }
        if (force || counterData.failed != failed)
        {
          counterPanel.setFailureValue(failed);
          counterData.failed = failed;
        }
      }
    });
  }

  private void refreshOutputFromSelection()
  {
    display.asyncExec(new Runnable()
    {
      public void run()
      {
        String output = "";
        if (selectedItem instanceof AutomatedTestCase)
        {
          AutomatedTestCase testCase = (AutomatedTestCase) selectedItem;
          output = readFile(testCase.getOutputFile()).replaceAll("\r\n", "\n").replaceAll("\n", "\r\n");
        }
        else if (selectedItem instanceof AutomatedTestResults)
        {
          AutomatedTestResults expectedResults = (AutomatedTestResults) selectedItem;
          output = "Results:\n--------\n" + readFile(expectedResults.getActualResults());
        }
        else if (selectedItem instanceof LegacyExpectedResults)
        {
          LegacyExpectedResults expectedResult = (LegacyExpectedResults) selectedItem;
          output = "Results:\n--------\n" + readFile(expectedResult.getActualResultFile());
        }
        if (!outputText.getText().equals(output))
        {
          outputText.setText(output);
          outputText.setSelection(outputText.getText().length());
        }
      }
    });
  }

  @Override
  public void setFocus()
  {
  }

  public void handleSelectionChanged(IStructuredSelection selection)
  {
    String output = "";
    if (selection.size() == 1 && selection.getFirstElement() instanceof WorkflowElement)
    {
      selectedItem = (WorkflowElement) selection.getFirstElement();
      if (selectedItem instanceof AutomatedTestCase)
      {
        AutomatedTestCase testCase = (AutomatedTestCase) selectedItem;
        output = readFile(testCase.getOutputFile());
      }
      else if (selectedItem instanceof AutomatedTestResults)
      {
        AutomatedTestResults expectedResults = (AutomatedTestResults) selectedItem;
        output = "Results:\n-----------\n" + readFile(expectedResults.getActualResults());
      }
      else if (selectedItem instanceof LegacyExpectedResults)
      {
        LegacyExpectedResults expectedResult = (LegacyExpectedResults) selectedItem;
        output = "Results:\n-----------\n" + readFile(expectedResult.getActualResultFile());
      }
    }
    else
    {
      selectedItem = null;
    }

    outputText.setText(output);
  }

  public void handleRerun()
  {
    testSuite.clearCases();
    if (testSuite.isLoadTest())
      runLoadTests();
    else
      runTests();
  }

  public void handleRerunSelection()
  {
    if (selectedItem != null)
    {
      if (selectedItem instanceof AutomatedTestSuite)
      {
        handleRerun();
      }
      else if (selectedItem instanceof WorkflowPackage)
      {
        WorkflowPackage pkg = (WorkflowPackage) selectedItem;
        List<AutomatedTestCase> cases = new ArrayList<AutomatedTestCase>();
        List<AutomatedTestCase> pkgCases = pkg.getTestCases();
        for (AutomatedTestCase testCase : testSuite.getTestCases())
        {
          if (pkgCases.contains(testCase))
            cases.add(testCase);
        }
        runTestCases(cases);
      }
      else if (selectedItem instanceof AutomatedTestCase)
      {
        AutomatedTestCase testCase = (AutomatedTestCase) selectedItem;
        runTestCase(testCase);
      }
    }
  }

  private void runTestCases(List<AutomatedTestCase> testCases)
  {
    if (testCases.size() > 0)
    {
      String msg = testCases.get(0).getProject().getDesignerProxy().checkForServerDbMismatch();
      if (msg != null)
      {
        MessageDialog.openError(getSite().getShell(), "Server DB Mismatch", msg);
        return;
      }

      AutomatedTestLaunchShortcut launchShortcut = new AutomatedTestLaunchShortcut();
      launchShortcut.launch(new StructuredSelection(testCases), ILaunchManager.RUN_MODE);
    }
  }

  private void runTestCase(AutomatedTestCase testCase)
  {
    String msg = testCase.getProject().getDesignerProxy().checkForServerDbMismatch();
    if (msg != null)
    {
      MessageDialog.openError(getSite().getShell(), "Server DB Mismatch", msg);
      return;
    }

    AutomatedTestLaunchShortcut launchShortcut = new AutomatedTestLaunchShortcut();
    launchShortcut.launch(new StructuredSelection(testCase), ILaunchManager.RUN_MODE);
  }

  public void handleStop()
  {
    actionGroup.enableStopAction(false);
    actionGroup.enableRerunAction(true);
    testSuite.setRunning(false);
    if (threadPool != null)
      threadPool.stop();
    if (monitor != null)
      monitor.shutdown();
    StubServer.stop();
    locked = false;
    updateProgressBar(100, testSuite.hasErrors() || testSuite.hasFailures(), true);
  }

  private String readFile(File file)
  {
    String output = "";
    if (file == null)
      return output;
    if (file.exists())
    {
      try
      {
        return new String(PluginUtil.readFile(file));
      }
      catch (IOException ex)
      {
        PluginMessages.log(ex);
        output = ex.toString();
      }
    }
    else
    {
      output = ""; // show nothing
    }
    return output;
  }

  private void deleteFile(File file)
  {
    if (file != null && file.exists())
    {
      file.delete();
    }
  }

  public void menuAboutToShow(IMenuManager menuManager)
  {
    actionGroup.fillContextMenu(menuManager);
  }

  class CounterData
  {
    int total;
    int completed;
    int errored;
    int failed;

    public CounterData(int total)
    {
      this.total = total;
    }
  }

  public void formatFunctionTestResults()
  {
    try
    {
      BusyIndicator.showWhile(display == null ? MdwPlugin.getDisplay() : display, new Runnable()
      {
        public void run()
        {
          try
          {
            TestResultsFormatter formatter = new TestResultsFormatter(testSuite.getProject());
            formatter.formatFunctionTestResults();
          }
          catch (Exception ex)
          {
            throw new RuntimeException(ex.getMessage(), ex);
          }
        }
      });
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Format Results", testSuite.getProject());
    }
  }

  /**
   * formats the load test report and generates html reports.
   */
  public void formatLoadTestResults()
  {
    try
    {
      BusyIndicator.showWhile(display, new Runnable()
      {
        public void run()
        {
          try
          {
            TestResultsFormatter formatter = new TestResultsFormatter(testSuite.getProject());
            formatter.formatLoadTestResults();
          }
          catch (Exception ex)
          {
            throw new RuntimeException(ex.getMessage(), ex);
          }
        }
      });
    }
    catch (Exception ex)
    {
      PluginMessages.uiError(ex, "Format Results", testSuite.getProject());
    }
  }

  private class CompatibleCounterPanel extends CounterPanel
  {
    CompatibleCounterPanel(Composite composite)
    {
      super(composite);
    }

    public void setRunValue(int value)
    {
      try
      {
        // use reflection for compatibility with both Juno and Kepler
        try
        {
          Method setRunValue = CounterPanel.class.getMethod("setRunValue", new Class[]{int.class,int.class,int.class});
          setRunValue.invoke(this, new Object[] {value, 0, 0} );
        }
        catch (NoSuchMethodException ex)
        {
          Method setRunValue = CounterPanel.class.getMethod("setRunValue", new Class[]{int.class,int.class});
          setRunValue.invoke(this, new Object[] {value, 0} );
        }
      }
      catch (Exception ex)
      {
        PluginMessages.log(ex);
      }
    }
  }

  private class TestStubber implements Stubber
  {
    public String processMessage(String masterRequestId, String request)
    {
      TestCaseRun run = masterRequestRunMap.get(masterRequestId);

      try
      {
        if (run == null)
        {
          JSONObject requestJson = null;
          ActivityStubRequest activityStubRequest = null; // mdw6
          ActivityRuntimeContext activityRuntimeContext = null;
          AdapterStubRequest adapterStubRequest = null; // mdw6
          if (request != null && request.trim().startsWith("{")) {
              try {
                  requestJson = new JSONObject(request);
              }
              catch (JSONException ex) {
                  // unparseable -- handle old way for adapter stubbing
              }
              if (requestJson.has(ActivityStubRequest.JSON_NAME)) {
                  activityStubRequest = new ActivityStubRequest(requestJson);
                  activityRuntimeContext = activityStubRequest.getRuntimeContext();
              }
              else if (requestJson.has("ActivityRuntimeContext")) {
                  activityRuntimeContext = new ActivityRuntimeContext(requestJson);
              }
              else if (requestJson.has(AdapterStubRequest.JSON_NAME)) {
                  adapterStubRequest = new AdapterStubRequest(requestJson);
              }
          }

          if (activityRuntimeContext != null)
          {
            if (activityStubRequest != null)
            {
              // mdw6+
              ActivityStubResponse activityStubResponse = new ActivityStubResponse();
              activityStubResponse.setPassthrough(true);
              return activityStubResponse.getJson().toString(2);
            }
            else
            {
              return "(EXECUTE_ACTIVITY)";
            }
          }
          else
          {
            if (adapterStubRequest != null)
            {
              // mdw6+
              AdapterStubResponse stubResponse = new AdapterStubResponse(AdapterActivity.MAKE_ACTUAL_CALL);
              stubResponse.setPassthrough(true);
              return stubResponse.getJson().toString(2);
            }
            else
            {
              return AdapterActivity.MAKE_ACTUAL_CALL;
            }
          }
        }
        return run.getStubResponse(masterRequestId, request, run.getRunNumber());
      }
      catch (JSONException ex)
      {
        PluginMessages.uiError(ex, "Test Stubber", testSuite.getProject());
        return null;
      }
    }
  }

  @Override
  public void syncMasterRequestId(String oldId, String newId)
  {
    TestCaseRun run = masterRequestRunMap.remove(oldId);
    if (run != null)
    {
      masterRequestRunMap.put(newId, run);
    }
  }
}
