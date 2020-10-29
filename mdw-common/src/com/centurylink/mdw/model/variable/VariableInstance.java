package com.centurylink.mdw.model.variable;

import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.translator.DocumentReferenceTranslator;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Represent a variable instance with its runtime value.
 */
public class VariableInstance implements Jsonable, Serializable, Comparable<VariableInstance>
{
    private String name;
    public void setName(String name){
        this.name = name;
    }
    public String getName(){
        return this.name;
    }

    private String type;
    public String getType(){
        return this.type;
    }
    public void setType(String type) {
        this.type = type;
    }

    private Long id;
    public Long getId(){
        return this.id;
    }
    public void setId(Long id){
        this.id = id;
    }
    @Deprecated
    public Long getInstanceId() { return id; }

    private Long variableId;
    public Long getVariableId(){
        return this.variableId;
    }
    public void setVariableId(Long variableId){
        this.variableId = variableId;
    }

    private Long processInstanceId;
    public void setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
    }
    public Long getProcessInstanceId() {
        return this.processInstanceId;
    }


    public VariableInstance() {
    }

    public VariableInstance(String name, Object objValue) {
        this.name = name;
        this.data = objValue;
    }

    public VariableInstance(JSONObject json) throws JSONException {
        if (json.has("id"))
            id = json.getLong("id");
        if (json.has("variableId"))
            variableId = json.getLong("variableId");
        if (json.has("name"))
            name = json.getString("name");
        if (json.has("value"))
            value = json.getString("value");
        if (json.has("type"))
            type = json.getString("type");
    }

    private String value;
    public String getStringValue(Package pkg) {
        if (value != null)
            return value;
        if (data == null)
            return null;
        if (pkg == null)
            pkg = PackageCache.getMdwBasePackage();
        value = pkg.getStringValue(type, data);
        return value;
    }
    public void setStringValue(String value){
        this.value = value;
        this.data = null;
    }

    transient private Object data;
    public Object getData(Package pkg) {
        if (data != null)
            return this.data;
        if (value == null)
            return null;
        if (pkg == null)
            pkg = PackageCache.getMdwBasePackage();
        data = pkg.getObjectValue(type, value);
        return data;
    }
    public void setData(Object data) {
        this.data = data;
        this.value = null;
    }

    public boolean isDocument(Package pkg) {
        if (data != null)
            return data instanceof DocumentReference;
        if (value == null)
            return false;
        if (!value.startsWith("DOCUMENT:"))
            return false;
        if (pkg == null)
            pkg = PackageCache.getMdwBasePackage();
        return pkg.getTranslator(type) instanceof DocumentReferenceTranslator;
    }

    public Long getDocumentId() {
        if (data instanceof DocumentReference) {
            DocumentReference docRef = (DocumentReference) data;
            return docRef.getDocumentId();
        }
        if (value.startsWith("DOCUMENT:")) {
            return Long.parseLong(value.split(":")[1]);
        }
        return null;
    }

    public int compareTo(VariableInstance other) {
      return getName().compareTo(other.getName());
    }

    public String getJsonName() {
        return "VariableInstance";
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = create();
        if (id != null)
            json.put("id", id);
        if (variableId != null)
            json.put("variableId", variableId);
        if (name != null)
            json.put("name", name);
        if (value != null)
            json.put("value", value);
        if (type != null)
            json.put("type", type);
        return json;
    }
}
