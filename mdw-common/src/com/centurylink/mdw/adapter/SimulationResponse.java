package com.centurylink.mdw.adapter;


public class SimulationResponse {

    private Boolean selected;
    private String returnCode;
    private Integer chance;
    private String response;

    public SimulationResponse(String value) {
        int firstDelim = value.indexOf(',');
        int secondDelim = (firstDelim>=0)?value.indexOf(',', firstDelim+1):-1;
        if (secondDelim>0) {
            returnCode = value.substring(0,firstDelim);
            chance = new Integer(value.substring(firstDelim+1,secondDelim));
            response = value.substring(secondDelim+1);
        } else if (firstDelim>=0) {
            returnCode = value.substring(0,firstDelim);
            chance = new Integer(value.substring(firstDelim+1));
            response = "";
        } else {
            returnCode = value;
            chance = 0;
            response = "";
        }
        selected = false;
    }

    public SimulationResponse(String returnCode, int chance, String response) {
        this.returnCode = returnCode;
        this.chance = chance;
        this.response = response;
        selected = false;
    }

    private String getAttributeValue() {
        return (returnCode!=null?returnCode:"")
                + ','
                + (chance!=null?Integer.toString(chance):"0")
                + ','
                + (response!=null?response:"");
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public String getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(String returnCode) {
        this.returnCode = returnCode;
    }

    public Integer getChance() {
        return chance;
    }

    public void setChance(Integer chance) {
        this.chance = chance;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
