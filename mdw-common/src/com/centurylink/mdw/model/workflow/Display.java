package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.constant.WorkAttributeConstant;

public class Display {

    public static final String NAME = WorkAttributeConstant.WORK_DISPLAY_INFO;

    public int x;
    public int y;
    public int w;
    public int h;

    public Display(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h= h;
    }

    public Display(String attribute) {
        for (String dim : attribute.split(",")) {
            if (dim.startsWith("x="))
                x = Integer.parseInt(dim.substring(2));
            else if (dim.startsWith("y="))
                y = Integer.parseInt(dim.substring(2));
            else if (dim.startsWith("w="))
                w = Integer.parseInt(dim.substring(2));
            else if (dim.startsWith("h="))
                h = Integer.parseInt(dim.substring(2));
        }
    }

    public String toString() {
        return "x=" + x + ",y=" + y + ",w=" + w + ",h=" + h;
    }
}
