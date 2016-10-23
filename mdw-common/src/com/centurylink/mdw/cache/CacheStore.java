/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.cache;

public interface CacheStore<K,T> {
    
    int getSize();

    void clear();
    
    void add(K key, T obj);
    
    T get(K key);
        
    void remove(K key);
    
    void update(K key, T obj);
    
    ///////////// test code
    
    void print();

}
