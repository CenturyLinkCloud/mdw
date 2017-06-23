/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.tom.model;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class Address implements Jsonable {
    
    public Address(JSONObject json) {
      bind(json);    
    }
    
    private String unit;
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    private String geoCode;
    public String getGeoCode() { return geoCode; }
    public void setGeoCode(String geoCode) { this.geoCode = geoCode; }
    
    private boolean verified;
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    
    private String building;
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    
    private String floor;
    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }
    
    private String stateOrProvince;
    public String getStateOrProvince() { return stateOrProvince; }
    public void setStateOrProvince(String stateOrProvince) { this.stateOrProvince = stateOrProvince; }
    
    private String addressType;
    public String getAddressType() { return addressType; }
    public void setAddressType(String type) { this.addressType = type; }
    
    private String streetNumberFirst;
    public String getStreetNumberFirst() { return streetNumberFirst; }
    public void setStreetNumberFirst(String streetNumberFirst) { this.streetNumberFirst = streetNumberFirst; }
    
    private String streetNumberFirstSuffix;
    public String getStreetNumberFirstSuffix() { return streetNumberFirstSuffix; }
    public void setStreetNumberFirstSuffix(String streetNumberFirstSuffix) { this.streetNumberFirstSuffix = streetNumberFirstSuffix; }
    
    private String streetNumberLastSuffix;
    public String getStreetNumberLastSuffix() { return streetNumberLastSuffix; }
    public void setStreetNumberLastSuffix(String streetNumberLastSuffix) { this.streetNumberLastSuffix = streetNumberLastSuffix; }
    
    private String streetName;
    public String getStreetName() { return streetName; }
    public void setStreetName(String streetName) { this.streetName = streetName; }
    
    private String streetType;
    public String getStreetType() { return streetType; }
    public void setStreetType(String streetType) { this.streetType = streetType; }
    
    private String streetSuffix;
    public String getStreetSuffix() { return streetSuffix; }
    public void setStreetSuffix(String streetSuffix) { this.streetSuffix = streetSuffix; }
    
    private String locality;
    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }
    
    private String postCode;
    public String getPostCode() { return postCode; }
    public void setPostCode(String postCode) { this.postCode = postCode; }
    
    private String postCodeExt;
    public String getPostCodeExt() { return postCodeExt; }
    public void setPostCodeExt(String postCodeExt) { this.postCodeExt = postCodeExt; }
    
    private String addressLine1;
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    
}
