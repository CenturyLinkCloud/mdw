package com.centurylink.mdw.model.workflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.centurylink.mdw.constant.WorkTransitionAttributeConstant;

public class TransitionDisplay {

    public static final String NAME = WorkTransitionAttributeConstant.TRANSITION_DISPLAY_INFO;

    public int lx;
    public int ly;
    public String type = "Elbow";
    public int[] xs = new int[0];
    public int[] ys = new int[0];

    public TransitionDisplay() {
    }

    public TransitionDisplay(String attribute) {
        for (String dim : attribute.split(",")) {
            if (dim.startsWith("type="))
                type = dim.substring(5);
            else if (dim.startsWith("lx="))
                lx = Integer.parseInt(dim.substring(3));
            else if (dim.startsWith("ly="))
                ly = Integer.parseInt(dim.substring(3));
            else if (dim.startsWith("xs=")) {
                for (String x : dim.substring(3).split("&")) {
                    addX(Integer.parseInt(x), true);
                }
            }
            else if (dim.startsWith("ys=")) {
                for (String y : dim.substring(3).split("&")) {
                    addY(Integer.parseInt(y), true);
                }
            }
        }
    }

    public void add(int x, int y) {
        add(x, y, true);
    }

    public void add(int x, int y, boolean end) {
        addX(x, end);
        addY(y, end);
    }


    private void addX(int x, boolean end) {
        List<Integer> xlist = new ArrayList<>();
        for (int xi : xs)
            xlist.add(xi);
        if (end || xlist.size() == 0) {
            xlist.add(x);
        }
        else {
            xlist.add(xlist.size() - 1, x);
        }
        xs = Arrays.stream(xlist.toArray(new Integer[0])).mapToInt(Integer::intValue).toArray();
    }

    private void addY(int y, boolean end) {
        List<Integer> ylist = new ArrayList<>();
        for (int yi : ys)
            ylist.add(yi);
        if (end || ylist.size() == 0) {
            ylist.add(y);
        }
        else {
            ylist.add(ylist.size() - 1, y);
        }
        ys = Arrays.stream(ylist.toArray(new Integer[0])).mapToInt(Integer::intValue).toArray();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("type=").append(type).append(",lx=").append(lx).append(",ly=").append(ly);
        sb.append(",xs=");
        for (int i = 0; i < xs.length; i++) {
            if (i > 0)
                sb.append("&");
            sb.append(xs[i]);
        }
        sb.append(",ys=");
        for (int i = 0; i < ys.length; i++) {
            if (i > 0)
                sb.append("&");
            sb.append(ys[i]);
        }
        return sb.toString();
    }
}
