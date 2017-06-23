/**
 * Copyright (c) 2017 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.tom.model;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class Name implements Jsonable {
    
    public Name(JSONObject json) {
        bind(json);    
    }
    
    private String formOfAddress;
    public String getFormOfAddress() { return formOfAddress; }
    public void setFormOfAddress(String formOfAddress) { this.formOfAddress = formOfAddress; }
    
    private String givenNames;
    public String getGivenNames() { return givenNames; }
    public void setGivenNames(String givenNames) { this.givenNames = givenNames; }
    
    private String familyNames;
    public String getFamilyNames() { return familyNames; }
    public void setFamilyNames(String familyNames) { this.familyNames = familyNames; }
    
    private String familyGeneration;
    public String getFamilyGeneration() { return familyGeneration; }
    public void setFamilyGeneration(String familyGeneration) { this.familyGeneration = familyGeneration; }
    
    private String qualifications;
    public String getQualifications() { return qualifications; }
    public void setQualifications(String qualifications) { this.qualifications = qualifications; }
}
