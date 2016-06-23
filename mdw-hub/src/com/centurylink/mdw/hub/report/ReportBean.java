/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.report;

import java.io.Serializable;

import javax.faces.FacesException;

import com.centurylink.mdw.hub.report.birt.BirtReportFactory;

public class ReportBean  implements Serializable {
    private Report report;
    public Long id;
    public String name;
    public String version;
    public String type;
    public String comments;
    public String packageName;
    private double height;
    private double width=250;
    private double left;
    private double top;

    public ReportBean() {
    }

     /**
     * @param panelId
     * @param clientHeight
     * @param innerHeight
     * @param outerHeight
     * @param left
     * @param top
     */
    public ReportBean(String packageName,String reportName, double height, double width,
            double left, double top) {
        this.packageName=packageName;
        this.name=reportName;
        this.height = height;
        this.width= width;
        this.left=left;
        this.top=top;
     }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Report getReport() {
        try {
            report = BirtReportFactory.loadReport(getPackageName()+"/"+getName(), null);
            return report;
        }
        catch (Exception ex) {
            throw new FacesException(ex.getMessage(), ex);
       }


    }
    public void setReport(Report report) {
        this.report = report;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getLeft() {
        return left;
    }

    public void setLeft(double left) {
        this.left = left;
    }

    public double getTop() {
        return top;
    }

    public void setTop(double top) {
        this.top = top;
    }
}
