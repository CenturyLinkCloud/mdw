package com.centurylink.mdw.sendgrid;

import com.centurylink.mdw.model.Jsonable;

public class Message implements Jsonable {

  private String type;
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }

  private String value;
  public String getValue() { return value; }
  public void setValue(String value) { this.value = value; }

}
