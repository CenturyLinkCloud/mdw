package com.centurylink.mdw.test;

/**
 * Filter applied to expected results before parsing according to old expressions syntax.
 */
public interface PreFilter {
    public String apply(String before);
}
