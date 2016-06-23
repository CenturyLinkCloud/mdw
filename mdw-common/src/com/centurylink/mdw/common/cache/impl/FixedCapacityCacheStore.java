/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.cache.impl;

import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.common.cache.CacheStore;

public class FixedCapacityCacheStore<K,T> implements CacheStore<K,T> {
    
	private int capacity;
    private LinkNode queue;
    private Map<K,LinkNode> map;
    
    public FixedCapacityCacheStore(int capacity) {
    	this.capacity = capacity;
    	this.map = new HashMap<K,LinkNode>(capacity);
    	this.queue = new LinkNode(null, null);
		this.queue.prev = this.queue;
		this.queue.next = this.queue;
    }
    
    public final int getSize() { return map.size(); }

    public void clear() {
    	synchronized (map) {
    		this.map.clear();
    		this.queue.prev = this.queue;
    		this.queue.next = this.queue;
    	}
	}
    
    private void addToQueue(LinkNode node) {
		node.next = queue.next;
		node.prev = queue;
		queue.next = node;
		node.next.prev = node;
    }
    
    private void removeFromQueue(LinkNode node) {
    	node.prev.next = node.next;
    	node.next.prev = node.prev;
    }
    
    public final void add(K key, T obj) {
    	synchronized (map) {
	    	LinkNode node = map.get(key);
	    	if (node==null) {
	    		node = new LinkNode(key, obj);
				addToQueue(node);
				map.put(key, node);
				if (map.size()>capacity) {
					map.remove(queue.prev.key);
					removeFromQueue(queue.prev);
				}
	    	}
    	}
    }
    
    public final void update(K key, T obj) {
    	synchronized (map) {
	    	LinkNode node = map.get(key);
	    	if (node!=null) {
	    		removeFromQueue(node);
    			addToQueue(node);
	    		node.data = obj;
	    	} else {
	    	    node = new LinkNode(key, obj);
	    	    addToQueue(node);
                map.put(key, node);
                if (map.size()>capacity) {
                    map.remove(queue.prev.key);
                    removeFromQueue(queue.prev);
                }
	    	}
    	}
    }
    
    public T get(K key) {
    	T obj;
    	synchronized (map) {
    		LinkNode node = map.get(key);
    		if (node!=null) {
    			removeFromQueue(node);
    			addToQueue(node);
    			obj = node.data;
    		} else obj = null;
    	}
    	return obj;
    }
    
    public final void remove(K key) {
    	synchronized (map) {
    		LinkNode node = map.get(key);
    		if (node!=null) {
    			removeFromQueue(node);
    			map.remove(key);
    		}
    	}
    }
    
    private class LinkNode {
    	K key;
    	T data;
    	LinkNode prev, next;
    	LinkNode(K key, T data) { this.key = key; this.data = data; }
    }
    
    ///////////// test code
    
	public void print() {
		System.out.print(" [" + map.size() + "]");
		for (LinkNode one=queue.next; one!=queue; one=one.next) {
			System.out.print(" "+one.data.toString());
		}
		System.out.println("");
	}

}
