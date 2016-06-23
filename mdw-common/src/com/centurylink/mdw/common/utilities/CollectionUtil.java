/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.utilities;


import java.util.Collection;
import java.util.Map;

public final class CollectionUtil{

    /**    * Checks if the passed in collection is empty     * @param pCollection     */    public static boolean isEmpty(Collection<?> pCollection){       return ((pCollection == null) || (pCollection.isEmpty()));
    }
    /**     * Checks if the passed inc ollection is NOT empty     * @param pCollection     */     public static boolean isNotEmpty(Collection<?> pCollection){       return !isEmpty(pCollection);
    }
     
     public static boolean isEmpty(Map<?,?> map) {
       return (map == null || map.isEmpty());
     }
}
