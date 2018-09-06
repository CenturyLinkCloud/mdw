/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.draw;

public class LinkDisplay {
    private String type;
    private int lx;
    private int ly;
    private int[] xs;

    private int[] ys;

    public final String getType() {
        return this.type;
    }

    public final void setType(String type) {
        this.type = type;
    }

    public final int getLx() {
        return this.lx;
    }

    public final void setLx(int var1) {
        this.lx = var1;
    }

    public final int getLy() {
        return this.ly;
    }

    public final void setLy(int var1) {
        this.ly = var1;
    }

    public final int[] getXs() {
        return this.xs;
    }

    public final void setXs(int[] xs) {
        this.xs = xs;
    }

    public final int[] getYs() {
        return this.ys;
    }

    public final void setYs(int[] ys) {
        this.ys = ys;
    }

    public LinkDisplay(String attr) {
        if (attr != null) {
            for (String v : attr.split(",")) {
                if (v.startsWith("lx=")) {
                    lx = Integer.parseInt(v.substring(3));
                }
                else if (v.startsWith("ly=")) {
                    ly = Integer.parseInt(v.substring(3));
                }
                else if (v.startsWith("xs=")) {
                    String[] xsAttr = v.substring(3).split("&");
                    xs = new int[ xsAttr.length];
                    for (int i = 0; i < xsAttr.length; i++) {
                        xs[i] = Integer.parseInt(xsAttr[i]);
                    }
                }
                else if (v.startsWith("ys=")) {
                    String[] ysAttr = v.substring(3).split("&");
                    ys = new int[ ysAttr.length];
                    for (int i = 0; i < ysAttr.length; i++) {
                        ys[i] = Integer.parseInt(ysAttr[i]);
                    }
                }
                else if (v.startsWith("type=")) {
                    String t = v.substring(5);
                    if (t == "Curve")
                        t = "Elbow";
                    type = Link.ELBOW;
                }
            }
        }
    }

    public LinkDisplay(String type, int lx, int ly, int[] xs, int[] ys) {
        this.type = type;
        this.lx = lx;
        this.ly = ly;
        this.xs = xs;
        this.ys = ys;
    }
}
