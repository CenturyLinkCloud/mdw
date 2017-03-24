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
package com.centurylink.mdw.plugin;

import java.util.HashMap;

import org.eclipse.compare.ICompareFilter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class HashCommentFilter implements ICompareFilter {
    public void setInput(Object input, Object ancestor, Object left, Object right) {
    }

    @SuppressWarnings("rawtypes")
    public IRegion[] getFilteredRegions(HashMap lineComparison) {
        String line = (String) lineComparison.get(THIS_LINE);
        if (line != null) {
            int hash = line.indexOf('#');
            if (hash >= 0) {
                IRegion region = new Region(hash, line.length() - hash);
                return new IRegion[] { region };
            }
        }
        return new IRegion[0];
    }

    public boolean isEnabledInitially() {
        return true;
    }

    public boolean canCacheFilteredRegions() {
        return true;
    }
}
