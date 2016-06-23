/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.cache.impl;

import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.common.cache.CacheStore;

public class InfiniteCapacityCacheStore<K,T> implements CacheStore<K,T> {
    
    private Map<K,T> map;
    
    public InfiniteCapacityCacheStore() {
    	this.map = new HashMap<K,T>();
    }
    
    public final int getSize() { return map.size(); }
    
    public final void clear() { 
    	synchronized (map) {
    	    map.clear();
    	}
    }
    
    public final void remove(K key) {
    	synchronized (map) {
    		map.remove(key);
    	}
    }

    public final T get(K key) {
    	return map.get(key);
    }
    
    public final void add(K key, T obj) {
    	synchronized (map) {
	    	T obj0 = map.get(key);
	    	if (obj0==null) {
				map.put(key, obj);
	    	}
    	}
    }
    
    public final void update(K key, T obj) {
    	synchronized (map) {
	    	T obj0 = map.get(key);
	    	if (obj0!=null) map.put(key, obj);
    	}
    }
    
    ///////////// test code
    
	public void print() {
		System.out.print(" [" + map.size() + "]");
		for (K one : map.keySet()) {
			System.out.print(" "+ map.get(one).toString());
		}
		System.out.println("");
	}

}
