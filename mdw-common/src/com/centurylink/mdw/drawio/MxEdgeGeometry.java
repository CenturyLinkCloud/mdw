/*
 * Copyright (C) 2018 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
