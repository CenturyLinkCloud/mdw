package com.centurylink.mdw.sendgrid;

import com.centurylink.mdw.model.Jsonable;

public class Attachment implements Jsonable {

    private String filename;
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    private String type;
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    private String content_id;
    public String getContent_id() { return content_id; }
    public void setContent_id(String content_id) { this.content_id = content_id; }

    private String content;
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

}
