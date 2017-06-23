package com.centurylink.tom.model;

import org.json.JSONObject;
import com.centurylink.mdw.model.Jsonable;

import io.swagger.annotations.ApiModelProperty;

public class CsrNote implements Jsonable
{

    private String text;
    private String date;
    @ApiModelProperty(required=true, value="CSR Note Author")
    private String author;

    /**
     * @param json
     * 
     */
    public CsrNote(JSONObject json) {
         bind(json);  
    }

    /**
     * 
     * @param author
     * @param text
     * @param date
     */
    public CsrNote(String text, String date, String author) {
        super();
        this.text = text;
        this.date = date;
        this.author = author;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

}
