/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
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
