package com.centurylink.mdw.model.asset;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommitInfo implements Jsonable {

    private String commit;
    public String getCommit() { return commit; }

    private String committer;
    public String getCommitter() { return committer; }
    public void setCommitter(String committer) { this.committer = committer; }

    private String email;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    private Date date;
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    private String message;
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    private String url;
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public CommitInfo(String commit) {
        this.commit = commit;
    }

    public CommitInfo(JSONObject json) throws JSONException {
        if (json.has("commit"))
            this.commit = json.getString("commit");
        if (json.has("committer"))
            this.committer = json.getString("committer");
        if (json.has("email"))
            this.email = json.getString("email");
        if (json.has("date"))
            this.date = stringToDate(json.getString("date"));
        if (json.has("message"))
            this.message = json.getString("message");
        if (json.has("url"))
            this.url = json.getString("url");
    }

    public JSONObject getJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (commit != null)
            json.put("commit", commit);
        if (committer != null)
            json.put("committer", committer);
        if (email != null)
            json.put("email", email);
        if (date != null)
            json.put("date", dateToString(date));
        if (message != null)
            json.put("message", message);
        if (url != null)
            json.put("url", url);
        return json;
    }

    public String getJsonName() {
        return "commitInfo";
    }

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static String dateToString(Date d) {
        return d == null ? null : new SimpleDateFormat(DATE_FORMAT).format(d);  // TODO: use an Instant
    }

    public static Date stringToDate(String s) {
        if (s == null)
            return null;
        try {
            return new SimpleDateFormat(DATE_FORMAT).parse(s);   // TODO: use an Instant
        }
        catch (ParseException e) {
            return null;
        }
    }
}
