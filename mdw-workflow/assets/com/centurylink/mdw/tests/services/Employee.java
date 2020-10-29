package com.centurylink.mdw.tests.services;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

@XmlType(name = "", propOrder = { "workstationId", "sapId", "firstName", "lastName" })
@XmlRootElement(name="GetEmployeeResponse", namespace="http://mdw.centurylink.com/serviceTypes")
public class Employee implements Serializable, Jsonable
{
  private String sapId;
  @XmlElement(name="sapId", namespace="http://mdw.centurylink.com/serviceTypes")
  public String getSapId() { return sapId; }
  public void setSapId(String sapId) { this.sapId = sapId; }

  private String workstationId;
  @XmlElement(name="workstationId", namespace="http://mdw.centurylink.com/serviceTypes")
  public String getWorkstationId() { return workstationId; }
  public void setWorkstationId(String workstationId) { this.workstationId = workstationId; }

  private String firstName;
  @XmlElement(name="firstName", namespace="http://mdw.centurylink.com/serviceTypes")
  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }

  private String lastName;
  @XmlElement(name="lastName", namespace="http://mdw.centurylink.com/serviceTypes")
  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }

  public String toString()
  {
    return sapId + ": " + lastName + ", " + firstName;
  }

  @Override
  public boolean equals(Object other)
  {
    if (!(other instanceof Employee))
      return false;
    else
      return toString().equals(other.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  public Employee() {

  }

  public Employee(JSONObject json) throws JSONException {
      bind(json);
  }

  @Override
  public String getJsonName() {
      return "employee";
  }
}