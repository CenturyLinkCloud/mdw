package MyServices;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.Jsonable;
import com.centurylink.mdw.model.user.User;

import io.swagger.annotations.ApiModel;

/**
 * Model object for Employees REST service.
 */
@ApiModel(value="Employee", description="employee")
public class Employee extends User implements Jsonable {
    
    /**
     * Jsonables should have a constructor that takes a JSONObject.
     */
    public Employee(JSONObject json) throws JSONException {
        super(json);
        if (json.has("title"))
            this.title = json.getString("title");
        if (json.has("department"))
            this.department = json.getString("department");
    }
    
    public String getWorkstationId() {
        return getCuid();
    }
    
    private String title;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    private String department;
    public String getDepartment() { return department; }
    public void setDepartment(String dept) { this.department = dept; }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject json = super.getJson();
        if (title != null)
            json.put("title", title);
        if (department != null)
            json.put("department", department);
        return json;
    }    
}
