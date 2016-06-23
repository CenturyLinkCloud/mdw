/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TestCompare {

    private PreFilter preFilter;

    public TestCompare(PreFilter preFilter) {
        this.preFilter = preFilter;
    }

    /**
     * Return value of zero means no diffs found.
     * Non-zero indicates the 1-based line number of the first difference.
     */
    public int doCompare(TestCaseAsset expected, File actual) throws IOException {
        BufferedReader expectedReader = null;
        BufferedReader actualReader = null;
        try {
            String expectedContent = expected.text();
            if (preFilter != null)
                expectedContent = preFilter.apply(expectedContent);
            expectedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(expectedContent.getBytes())));
            actualReader = new BufferedReader(new FileReader(actual));
            int i = 1;
            String expectedLine = null;
            while ((expectedLine = expectedReader.readLine()) != null) {
                String actualLine = actualReader.readLine();
                if (actualLine == null)
                    return i;
                if (!yamlEquals(expectedLine, actualLine))
                    return i;
                i++;
            }
            if (actualReader.readLine() != null)
                return i;
            return 0;
        }
        finally {
            if (expectedReader != null)
                expectedReader.close();
            if (actualReader != null)
                actualReader.close();
        }
    }

    private boolean yamlEquals(String line1, String line2) {
        // remove comments and trim trailing whitespace
        String l1 = stripComment(line1).replaceFirst("\\s+$", "");
        String l2 = stripComment(line2).replaceFirst("\\s+$", "");
        return l1.equals(l2) || matchRegex(l1, l2);
    }

    private String stripComment(String line) {
        int hash = line.indexOf('#');
        if (hash < 0)
            return line;
        else
            return line.substring(0, hash);
    }

    /**
     * Max one regex per line.  Returns false if no regex at all.
     */
    static boolean matchRegex(String regex, String actual) {
        String[] regexLines = regex.split("\\r?\\n");
        String[] actualLines = actual.split("\\r?\\n");

        if (regexLines.length != actualLines.length)
            return false;

        boolean hasRegex = false;
        for (int i = 0; i < regexLines.length; i++)  {
            String regexLine = regexLines[i];
            String actualLine = actualLines[i];
            int start = regexLine.indexOf("${~");
            if (start >= 0) {
                hasRegex = true;
                int end = regexLine.indexOf("}", start);
                if (end >= 0) {
                    String before = regexLine.substring(0, start);
                    String after = regexLine.substring(end + 1);
                    if (!before.equals(actualLine.substring(0, start)))
                        return false;
                    if (!after.equals(actualLine.substring(actualLine.length() - after.length())))
                        return false;
                    String lineRegex = regexLine.substring(start + 3, end);
                    String lineActual = actualLine.substring(start, actualLine.length() - after.length());
                    if (!lineActual.matches(lineRegex))
                        return false;
                }
            }
        }
        return hasRegex;
    }
}
