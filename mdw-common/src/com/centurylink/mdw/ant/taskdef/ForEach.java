/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.ant.taskdef;

import java.io.File;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.CallTarget;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileNameMapper;

/***
 * Ant task to iterates over a list, a list of filesets, or both.
 *
 * <pre>
 *
 * Usage:
 *
 *   Task declaration in the project:
 *   <code>
 *     &lt;taskdef name="foreach" classname="com.centurylink.mdw.ant.taskdef" /&gt;
 *   </code>
 *
 *   Call Syntax:
 *   <code>
 *     &lt;foreach list="values" target="targ" param="name"
 *                 [parallel="true|false"]
 *                 [delimiter="delim"] /&gt;
 *   </code>
 *
 *   Attributes:
 *         list      --> The list of values to process, with the delimiter character,
 *                       indicated by the "delim" attribute, separating each value
 *         target    --> The target to call for each token, passing the token as the
 *                       parameter with the name indicated by the "param" attribute
 *         param     --> The name of the parameter to pass the tokens in as to the
 *                       target
 *         delimiter --> The delimiter string that separates the values in the "list"
 *                       parameter.  The default is ","
 *         parallel  --> Should all targets execute in parallel.  The default is false.
 *         trim      --> Should we trim the list item before calling the target?
 *
 * </pre>
 */
public class ForEach extends Task
{
    private String list;
    private String param;
    private String delimiter;
    private String target;
    private boolean inheritAll;
    private boolean inheritRefs;
    private Vector<Property> params;
    private Vector<Ant.Reference> references;
    private Path currPath;
    private boolean trim;
    private Mapper mapper;

    /***
     * Default Constructor
     */
    public ForEach()
    {
        super();
        this.list = null;
        this.param = null;
        this.delimiter = ",";
        this.target = null;
        this.inheritAll = false;
        this.inheritRefs = false;
        this.params = new Vector<Property>();
        this.references = new Vector<Ant.Reference>();
    }

    private void executeSequential(Vector<Task> tasks)
    {
        TaskContainer tc = (TaskContainer) getProject().createTask("sequential");
        Enumeration<Task> e = tasks.elements();
        Task t = null;
        while (e.hasMoreElements())
        {
            t = e.nextElement();
            tc.addTask(t);
        }

        ((Task)tc).execute();
    }

    public void execute()
        throws BuildException
    {
        if (list == null && currPath == null) {
            throw new BuildException("You must have a list or path to iterate through");
        }
        if (param == null)
            throw new BuildException("You must supply a property name to set on each iteration in param");
        if (target == null)
            throw new BuildException("You must supply a target to perform");

        Vector<Object> values = new Vector<Object>();

        // Take Care of the list attribute
        if (list != null)
        {
            StringTokenizer st = new StringTokenizer(list, delimiter);

            while (st.hasMoreTokens())
            {
                String tok = st.nextToken();
                if (trim) tok = tok.trim();
                values.addElement(tok);
            }
        }

        String[] pathElements = new String[0];
        if (currPath != null) {
            pathElements = currPath.list();
        }

        for (int i=0;i<pathElements.length;i++)
        {
            if (mapper != null)
            {
                FileNameMapper m = mapper.getImplementation();
                String mapped[] = m.mapFileName(pathElements[i]);
                for (int j=0;j<mapped.length;j++)
                    values.addElement(mapped[j]);
            }
            else
            {
                values.addElement(new File(pathElements[i]));
            }
        }

        Vector<Task> tasks = new Vector<Task>();

        int sz = values.size();
        CallTarget ct = null;
        Object val = null;
        Property p = null;

        for (int i = 0; i < sz; i++) {
            val = values.elementAt(i);
            ct = createCallTarget();
            p = ct.createParam();
            p.setName(param);

            if (val instanceof File)
                p.setLocation((File)val);
            else
                p.setValue((String)val);

            tasks.addElement(ct);
        }

        executeSequential(tasks);
    }

    public void setTrim(boolean trim)
    {
        this.trim = trim;
    }

    public void setList(String list)
    {
        this.list = list;
    }

    public void setDelimiter(String delimiter)
    {
        this.delimiter = delimiter;
    }

    public void setParam(String param)
    {
        this.param = param;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }

    /**
     * Corresponds to <code>&lt;antcall&gt;</code>'s <code>inheritall</code>
     * attribute.
     */
    public void setInheritall(boolean b) {
        this.inheritAll = b;
    }

    /**
     * Corresponds to <code>&lt;antcall&gt;</code>'s <code>inheritrefs</code>
     * attribute.
     */
    public void setInheritrefs(boolean b) {
        this.inheritRefs = b;
    }

    /**
     * Corresponds to <code>&lt;antcall&gt;</code>'s nested
     * <code>&lt;param&gt;</code> element.
     */
    public void addParam(Property p) {
        params.addElement(p);
    }

    /**
     * Corresponds to <code>&lt;antcall&gt;</code>'s nested
     * <code>&lt;reference&gt;</code> element.
     */
    public void addReference(Ant.Reference r) {
        references.addElement(r);
    }

    /**
     * @deprecated Use createPath instead.
     */
    public void addFileset(FileSet set)
    {
        log("The nested fileset element is deprectated, use a nested path "
            + "instead",
            Project.MSG_WARN);
        createPath().addFileset(set);
    }

    public Path createPath() {
        if (currPath == null) {
            currPath = new Path(getProject());
        }
        return currPath;
    }

    public Mapper createMapper()
    {
        mapper = new Mapper(getProject());
        return mapper;
    }

    private CallTarget createCallTarget() {
        CallTarget ct = (CallTarget) getProject().createTask("antcall");
        ct.setOwningTarget(getOwningTarget());
        ct.init();
        ct.setTarget(target);
        ct.setInheritAll(inheritAll);
        ct.setInheritRefs(inheritRefs);
        Enumeration<Property> e = params.elements();
        while (e.hasMoreElements()) {
            Property param = e.nextElement();
            Property toSet = ct.createParam();
            toSet.setName(param.getName());
            if (param.getValue() != null) {
                toSet.setValue(param.getValue());
            }
            if (param.getFile() != null) {
                toSet.setFile(param.getFile());
            }
            if (param.getResource() != null) {
                toSet.setResource(param.getResource());
            }
            if (param.getPrefix() != null) {
                toSet.setPrefix(param.getPrefix());
            }
            if (param.getRefid() != null) {
                toSet.setRefid(param.getRefid());
            }
            if (param.getEnvironment() != null) {
                toSet.setEnvironment(param.getEnvironment());
            }
            if (param.getClasspath() != null) {
                toSet.setClasspath(param.getClasspath());
            }
        }

        Enumeration<Ant.Reference> e2 = references.elements();
        while (e2.hasMoreElements()) {
            ct.addReference(e2.nextElement());
        }

        return ct;
    }

    protected void handleOutput(String line)
    {
        try {
                super.handleOutput(line);
        }
        // This is needed so we can run with 1.5 and 1.5.1
        catch (IllegalAccessError e) {
            super.handleOutput(line);
        }
    }

    protected void handleErrorOutput(String line)
    {
        try {
                super.handleErrorOutput(line);
        }
        // This is needed so we can run with 1.5 and 1.5.1
        catch (IllegalAccessError e) {
            super.handleErrorOutput(line);
        }
    }

}


