/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.designer.testing;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

public class TestCompare {

    private PreFilter preFilter;

    public TestCompare(PreFilter preFilter) {
        this.preFilter = preFilter;
    }

    /**
     * Return value of zero means no diffs found.
     * Non-zero indicates the 1-based line number of the first difference.
     */
    public int doCompare(TestCaseAsset expected, TestCaseFile actual) throws IOException {
        BufferedReader expectedReader = null;
        BufferedReader actualReader = null;
        try {
            String expectedContent = expected.text();
            if (preFilter != null)
                expectedContent = preFilter.apply(expectedContent);
            expectedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(expectedContent.getBytes())));

            actualReader = new BufferedReader(new StringReader(actual.text()));
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
        return l1.equals(l2) || matchRegex(l1, l2) == 0;
    }

    private String stripComment(String line) {
        int hash = line.indexOf('#');
        if (hash < 0)
            return line;
        else
            return line.substring(0, hash);
    }

    /**
     * Greater than zero is mismatched line number (one-based).
     * Max one regex per line.
     */
    static int matchRegex(String regex, String actual) {
        String[] regexLines = regex.split("\\r?\\n");
        String[] actualLines = actual.split("\\r?\\n");

        for (int i = 0; i < regexLines.length; i++)  {
            String regexLine = regexLines[i];
            if (actualLines.length < i + 1)
                return i + 1;
            String actualLine = actualLines[i];
            int start = regexLine.indexOf("${~");
            if (start >= 0) {
                int end = regexLine.indexOf("}", start);
                if (end >= 0) {
                    String before = regexLine.substring(0, start);
                    String after = regexLine.substring(end + 1);
                    if (!before.equals(actualLine.substring(0, start)))
                        return i + 1;
                    if (!after.equals(actualLine.substring(actualLine.length() - after.length())))
                        return i + 1;
                    String lineRegex = regexLine.substring(start + 3, end);
                    String lineActual = actualLine.substring(start, actualLine.length() - after.length());
                    if (!lineActual.matches(lineRegex))
                        return i + 1;
                }
            }
            else if (!regexLine.equals(actualLine)) {
                return i + 1;
            }
        }

        if (regexLines.length < actualLines.length)
            return regexLines.length + 1;

        return 0;
    }
}
