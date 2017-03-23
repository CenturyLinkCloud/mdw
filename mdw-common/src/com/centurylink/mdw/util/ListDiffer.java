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
package com.centurylink.mdw.util;

import java.util.List;

public class ListDiffer<T> {

    public void diff(List<T> existing, List<T> newlist,
            List<T> insertlist, List<T> updatelist, List<T> deletelist) {
        for (T n : newlist) {
            boolean found = false;
            for (T e : existing) {
                if (e.equals(n)) {
                    found = true;
                    break;
                }
            }
            if (found) updatelist.add(n);
            else insertlist.add(n);
        }
        if (deletelist!=null) {
            for (T e : existing) {
                boolean found = false;
                for (T n : newlist) {
                    if (n.equals(e)) {
                        found = true;
                        break;
                    }
                }
                if (!found) deletelist.add(e);
            }
        }
    }
}
