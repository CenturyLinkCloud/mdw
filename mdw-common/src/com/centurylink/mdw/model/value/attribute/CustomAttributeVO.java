/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.attribute;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the DEFINITION of custom attributes which can be 
 * associated with an arbitrary owner type (eg: RULE_SET) and 
 * an optional categorizer for grouping the definitions (eg: LANGUAGE).
 */
public class CustomAttributeVO implements Serializable {
  
    private static final long serialVersionUID = 1L;
    
    public static final String DEFINITION = "DEFINITION";
    public static final String ROLES = "ROLES";
    
    private String ownerType;
    public String getOwnerType() { return ownerType; }
    
    private String categorizer;
    public String getCategorizer() { return categorizer; }
    
    public CustomAttributeVO(String ownerType) {
        this.ownerType = ownerType;
    }
    
    public CustomAttributeVO(String ownerType, String categorizer) {
        this.ownerType = ownerType;
        this.categorizer = categorizer;
    }
    
    public String getDefinitionAttrOwner() {
        if (categorizer == null)
            return ownerType;
        else
            return ownerType + ":" + categorizer;
    }
    
    public String getRolesAttrOwner() {
        if (categorizer == null)
            return ownerType;
        else
            return ownerType + ":" + categorizer;
    }
    
    // pagelet syntax
    private String definition;
    public String getDefinition() { return definition; }
    public void setDefinition(String def) { this.definition = def; }
    
    public List<String> roles;
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public String getRolesString() {
        if (roles == null)
            return null;
        String rolesString = "";
        for (int i = 0; i < roles.size(); i++) {
            rolesString += roles.get(i);
            if (i < roles.size() - 1)
                rolesString += ",";
        }
        return rolesString;
    }
    public void setRolesString(String rolesString) {
        if (rolesString != null) {
            roles = new ArrayList<String>();
            String[] array = rolesString.split(",");
            for (String role : array)
                roles.add(role);
        }
    }
    
    public Long getOwnerId() {
        return new Long(0);  // these are definition attributes
    }
    
    public boolean isEmpty() {
        return !hasDefinition() && !hasRoles();
    }
    
    public boolean hasDefinition() {
        return definition != null && definition.trim().length() > 0;
    }
    
    public boolean hasRoles() {
        return roles != null && roles.size() > 0;
    }

}
