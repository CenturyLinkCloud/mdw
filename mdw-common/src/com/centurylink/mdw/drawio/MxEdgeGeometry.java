package com.centurylink.mdw.drawio;

import java.util.ArrayList;
import java.util.List;

public class MxEdgeGeometry {

    // fractional connect points along associated activities
    Float exitX = 1f;
    Float exitY = 0.5f;
    Float entryX;
    Float entryY;

    List<Point> intermediatePoints;

    void addPoint(int x, int y) {
        if (intermediatePoints == null)
            intermediatePoints = new ArrayList<>();
        intermediatePoints.add(new Point(x, y));
    }

    class Point {
        int x;
        int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

}
