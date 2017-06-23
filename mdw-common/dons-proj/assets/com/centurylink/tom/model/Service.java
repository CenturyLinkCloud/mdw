/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.tom.model;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class Service implements Jsonable {
    
    public Service(JSONObject json) {
        bind(json);    
    }
    
    private String commonName;
    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }
    
    private Map<String,String> attributes;
    public Map<String,String> getAttributes() { return attributes; }
    public void setAttributes(Map<String,String> attributes) { this.attributes = attributes; }
    
    private String serviceType;
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    
    private String action;
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    private List<ServiceOrderService> serviceOrderServices;
    public List<ServiceOrderService> getServiceOrderServices() { return serviceOrderServices; }
    public void setServiceOrderServices(List<ServiceOrderService> soServices) { this.serviceOrderServices = soServices; }
    
    private Address serviceAddress;
    public Address getServiceAddress() { return serviceAddress; }
    public void setServiceAddress(Address serviceAddress) { this.serviceAddress = serviceAddress; }
    
    
    public class ServiceOrderService implements Jsonable {
        
        public ServiceOrderService(JSONObject json) {
            bind(json);    
        }
        
        private String commonName;
        public String getCommonName() { return commonName; }
        public void setCommonName(String commonName) { this.commonName = commonName; }

        private String action;
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        private Map<String,String> attributes;
        public Map<String,String> getAttributes() { return attributes; }
        public void setAttributes(Map<String,String> attributes) { this.attributes = attributes; }
        
    }
}
