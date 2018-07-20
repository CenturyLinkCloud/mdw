package com.centurylink.mdw.tests.microservice;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import javax.validation.constraints.Email;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="NewUser", description="User with optional groups")
public class User implements Jsonable {

    public User(JSONObject json) {
        bind(json);
    }

    @ApiModelProperty(value="Unique ID", required=true)
    private String id;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @ApiModelProperty(required=true)
    private String firstName;
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    @ApiModelProperty(required=true)
    private String lastName;
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    @Email
    private String emailAddress;
    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    private String group;
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

}
