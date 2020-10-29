package com.centurylink.mdw.cli;

public class Prop {

    /**
     * Name in templates and --param.
     */
    private String name;
    public String getName() { return name; }

    /**
     * File path relative to project dir.
     */
    private String file;
    public String getFile() { return file; }

    private String property;
    public String getProperty() { return property; }

    public Prop(String name, String file, String property) {
        this(name, file, property, false);
    }

    public Prop(String name, String file, String property, boolean inProjectDir) {
        this.name = name;
        this.file = file;
        this.property = property;
        this.inProjectDir = inProjectDir;
    }

    /**
     * Specified via command-line.
     */
    boolean specified;

    public String toString() {
        String s = name + " --> (" + file + ") " + property;
        if (specified)
            s += "*";
        return s;
    }

    /**
     * Whether the prop file exists in projectDir vs configRoot
     */
    boolean inProjectDir;


}
