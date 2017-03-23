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
