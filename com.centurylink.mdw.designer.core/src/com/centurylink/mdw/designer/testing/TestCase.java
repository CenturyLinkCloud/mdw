/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class TestCase {

    public static final String LANGUAGE_GROOVY = "Groovy";
    public static final String LANGUAGE_GHERKIN = "Gherkin";
    public static final String LANGUAGE_MAGIC = "Magic";

    public static final Map<String,String> LEGACY_TEST_CASE_FILENAMES = new HashMap<String,String>();
    static {
        LEGACY_TEST_CASE_FILENAMES.put(LANGUAGE_GROOVY, "commands.groovy");
        LEGACY_TEST_CASE_FILENAMES.put(LANGUAGE_GHERKIN, "commands.feature");
        LEGACY_TEST_CASE_FILENAMES.put(LANGUAGE_MAGIC, "commands.txt");
    }

    public static final String START = "start";
    public static final String VERIFY_PROCESS = "verify_process";
    public static final String VERIFY_RESPONSE = "verify_response";
    public static final String TASK = "task";
    public static final String WAIT = "wait";
    public static final String STUB = "stub";
    public static final String MASTER_REQUEST_ID = "master_request_id";
    public static final String SLEEP = ControlCommandShell.SLEEP;
    public static final String MESSAGE = ControlCommandShell.MESSAGE;
    public static final String NOTIFY = "notify";
    public static final String SIGNAL = "signal";

    public static final String STATUS_NOT_RUN = "not run";
    public static final String STATUS_WAITING = "waiting";
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_FAIL = "failed";
    public static final String STATUS_PASS = "passed";
    public static final String STATUS_ERROR = "errored"; // unable to execute
    public static final String STATUS_STOP = "stopped"; // killed by user

    public static final String PLACEHOLDER_MAP_FILENAME = "placeHolderMap.csv";

    private String language = LANGUAGE_GROOVY;
    private TestFile commands;
    private String groovyScript;
    private File caseFile; // 5.5 asset style
    private RuleSetVO ruleSet; // 5.2 asset style
    private File caseDir;  // legacy style
    private File resultDir;
    private boolean selected;
    private String status;
    private Date startTime, endTime;
    private SimpleDateFormat sdf;
    private int numberPrepared;            // for load testing only
    private int numberStarted;            // for load testing only
    private int numberCompleted;        // for load testing only
    private String masterRequestId;
    private TestCaseRun firstRun;        // for function test only

    public TestCase() {
        clear();
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    public boolean isLegacy() {
        return caseDir != null;
    }

    private String prefix;
    public String getPrefix() { return prefix; }

    public String getPath() {
        if (isLegacy())
          return prefix + "/" + getName();
        else
            return prefix + "/" + caseFile.getName();
    }

    public TestCase(String prefix, File caseFile) {
        this();
        if (caseFile.isDirectory()) {
            setTestCaseDir(caseFile);
            if (new File(caseFile + "/" + LEGACY_TEST_CASE_FILENAMES.get(LANGUAGE_GHERKIN)).exists())
                setLanguage(LANGUAGE_GHERKIN);
            else if (new File(caseFile + "/" + LEGACY_TEST_CASE_FILENAMES.get(LANGUAGE_MAGIC)).exists())
                setLanguage(LANGUAGE_MAGIC);
        }
        else {
            this.caseFile = caseFile;
            if (caseFile.getName().endsWith(RuleSetVO.getFileExtension(RuleSetVO.FEATURE)))
                setLanguage(LANGUAGE_GHERKIN);
        }
        this.prefix = prefix;
    }

    public TestCase(String prefix, RuleSetVO ruleSet) {
        this();
        this.ruleSet = ruleSet;
        if (ruleSet.getName().endsWith(RuleSetVO.getFileExtension(RuleSetVO.FEATURE)))
            setLanguage(LANGUAGE_GHERKIN);
        this.prefix = prefix;
    }

    public TestCase(TestCase cloneFrom) {
        this();
        if (cloneFrom.getCaseDirectory() != null) {
            setTestCaseDir(cloneFrom.getCaseDirectory());
            if (new File(caseDir + "/" + LEGACY_TEST_CASE_FILENAMES.get(LANGUAGE_GHERKIN)).exists())
                setLanguage(LANGUAGE_GHERKIN);
            else if (new File(caseDir + "/" + LEGACY_TEST_CASE_FILENAMES.get(LANGUAGE_MAGIC)).exists())
                setLanguage(LANGUAGE_MAGIC);
        }
        else if (cloneFrom.ruleSet != null) {
            this.ruleSet = cloneFrom.ruleSet;
            if (ruleSet.getName().endsWith(RuleSetVO.getFileExtension(RuleSetVO.FEATURE)))
                setLanguage(LANGUAGE_GHERKIN);
        }
        else {
            this.caseFile = cloneFrom.getCaseFile();
            if (caseFile.getName().endsWith(RuleSetVO.getFileExtension(RuleSetVO.FEATURE)))
                setLanguage(LANGUAGE_GHERKIN);
        }
        this.prefix = cloneFrom.prefix;
    }

    public void setTestCaseDir(File testCaseDir) {
        this.caseDir = testCaseDir;
        this.resultDir = testCaseDir;
    }

    public void setTestCaseFile(File testCaseFile) {
        this.caseFile = testCaseFile;
    }

    public void setTestCaseRuleSet(RuleSetVO ruleSet) {
        this.ruleSet = ruleSet;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isGroovy() {
        return LANGUAGE_GROOVY.equals(language);
    }

    public boolean isGherkin() {
        return LANGUAGE_GHERKIN.equals(language);
    }

    public String getCommandsFileName() {
        return LEGACY_TEST_CASE_FILENAMES.get(language);
    }

    public void prepare()
    throws IOException, ParseException, DataAccessException {
        if (isLegacy()) {
            if (isGroovy()) {
                groovyScript = FileHelper.readFromFile(caseDir + "/" + getCommandsFileName());
                clear();
            }
            else {
                commands = new TestFile(caseDir, getCommandsFileName());
                commands.load();
                for (TestFileLine line : commands.getLines()) {
                    if (line.getCommand().equalsIgnoreCase(MASTER_REQUEST_ID)) {
                        masterRequestId = line.getWord(1);
                        commands.getLines().remove(line);
                        break;
                    }
                    else if (line.getWord(0).equalsIgnoreCase("Given") && line.getWord(1).equalsIgnoreCase("masterRequestId")) {
                        masterRequestId = line.getWord(2);
                        commands.getLines().remove(line);
                        break;
                    }
                }
            }
        }
        else {
            if (caseFile == null) {
                // non-VCS
                if (isGroovy()) {
                    groovyScript = ruleSet.getRuleSet();
                    clear();
                }
            }
            else {
                if (isGroovy()) {
                    groovyScript = FileHelper.readFromFile(caseFile.toString());
                    clear();
                }
            }
        }
        numberPrepared = numberStarted = numberCompleted = 0;
        status = STATUS_WAITING;
    }

    public void clear() {
        startTime = null;
        endTime = null;
        status = STATUS_NOT_RUN;
        numberPrepared = numberStarted = numberCompleted = 0;
        masterRequestId = null;
    }

    public TestFile getCommands() {
        return this.commands;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public File getCaseDirectory() {
        return caseDir;
    }

    public File getCaseFile() {
        return caseFile;
    }

    public RuleSetVO getRuleSet() {
        return ruleSet;
    }

    public String getGroovyScript() {
        return groovyScript;
    }

    public String getStartTime() {
        return startTime==null?null:sdf.format(startTime);
    }

    public Date getStartDate() {
        return startTime;
    }

    public void setStartDate(Date start) {
        this.startTime = start;
    }

    public String getEndTime() {
        return endTime==null?null:sdf.format(endTime);
    }

    public Date getEndDate() {
        return endTime;
    }

    public void setEndDate(Date end) {
        this.endTime = end;
    }

    public File[] getFiles() {
        return caseDir.listFiles();
    }

    public void setResultDirectory(File dir) {
        resultDir = dir == null ? caseDir : dir;
    }

    public File getResultDirectory() {
        return resultDir;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public String getName() {
        return getCaseName();
    }

    public String getCaseName() {
        if (isLegacy())
            return caseDir.getName();
        else {
            String assetName = caseFile == null ? ruleSet.getName() : caseFile.getName();

            int lastDot = assetName.lastIndexOf('.');
            if (lastDot > 0)
                return assetName.substring(0, lastDot);
            else
                return assetName;
        }
    }

    public void setFirstRun(TestCaseRun firstRun) {
        this.firstRun = firstRun;
    }
    public TestCaseRun getFirstRun() {
        return firstRun;
    }
    public String getFirstRunMasterRequestId() {
        return firstRun==null?null:firstRun.getMasterRequestId();
    }

    public String getMessage() {
        return firstRun==null?null:firstRun.getMessage();
    }

    public String getMasterRequestId() {
        return masterRequestId;
    }

    public void setMasterRequestId(String masterRequestId) {
        this.masterRequestId = masterRequestId;
    }

    public String readParameterValueFromFile(String filename) throws IOException {
        return commands.readFile(filename);
    }

    /**
     * Helpful in guessing percent complete in UI.
     */
    public int getTotalSteps() {
        if (commands == null || commands.getLines() == null)
            return 0;
        else
            return commands.getLines().size();
    }

    public int getNumberPrepared() {
        return numberPrepared;
    }
    public void setNumberPrepared(int numberPrepared) {
        this.numberPrepared = numberPrepared;
    }
    public int getNumberStarted() {
        return numberStarted;
    }
    public void setNumberStarted(int numberStarted) {
        this.numberStarted = numberStarted;
    }
    public int getNumberCompleted() {
        return numberCompleted;
    }
    public void setNumberCompleted(int numberCompleted) {
        this.numberCompleted = numberCompleted;
    }
    public String getElapseTime() {
        if (startTime==null || endTime==null) return "";
        long diff = endTime.getTime()-startTime.getTime();
        long min = diff/60000;
        diff = diff - min*60000;
        long sec = diff/1000;
        diff = diff - sec*1000;
        long tenth = diff/100;
        diff = diff - tenth*100;
        long hundredth = diff/10;
        diff = diff - hundredth*10;
        return Long.toString(min) + ":" + Long.toString(sec) + "."
            + Long.toString(tenth) + Long.toString(hundredth) + Long.toString(diff);
    }


}
