/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

  public boolean equals(Object other)
  {
    if (!(other instanceof Employee))
      return false;
    else
      return toString().equals(other.toString());
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