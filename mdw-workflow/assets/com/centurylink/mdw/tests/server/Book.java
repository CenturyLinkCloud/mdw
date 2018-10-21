package com.centurylink.mdw.tests.server;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.tests.workflow.Person;
import org.json.JSONObject;

public class Book implements Jsonable {

    public Book(JSONObject json) {
        bind(json);
    }

    public Book(String title, Person author, int pages, boolean fiction) {
        this.title = title;
        this.author = author;
        this.pages = pages;
        this.fiction = fiction;
    }

    private String title;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    private Person author;
    public Person getAuthor() { return author; }
    public void setAuthor(Person author) { this.author = author; }

    private int pages;
    public int getPages() { return pages; }
    public void setPages(int pages) { this.pages = pages; }

    private boolean fiction;
    public boolean isFiction() { return fiction; }
    public void setFiction(boolean fiction) { this.fiction = fiction; }

}
