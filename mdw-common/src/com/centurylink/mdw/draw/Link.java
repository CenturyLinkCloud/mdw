/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.draw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.workflow.Transition;

public class Link implements Drawable {
    public static final String ELBOW = "Elbow";
    public static final String STRAIGHT = "Straight";
    public static final String CURVE = "Curve";
    public static final String ELBOWH = "ElbowH";
    public static final String ELBOWV = "ElbowV";

    public static final int AUTOLINK_H = 1;
    public static final int AUTOLINK_V = 2;
    public static final int AUTOLINK_HV = 3;
    public static final int AUTOLINK_VH = 4;
    public static final int AUTOLINK_HVH = 5;
    public static final int AUTOLINK_VHV = 6;

    public static final Color COLOR_NORMAL = Color.GRAY;
    public static final Color COLOR_OTHER = Color.ORANGE;

    private Step from;
    private Step to;
    public int lx;
    public int ly;

    public static final int CORR = 3;
    public static final int CR = 8;
    public static final int GAP = 4;
    public static final int LABEL_CORR = -2;
    public static final double ELBOW_THRESHOLD = 0.8D;
    public static final int ELBOW_VH_THRESHOLD = 60;

    private static final BasicStroke LINK_STROKE = Display.LINK_STROKE;
    public static final double HIT_PAD = 4.0D;
    private LinkDisplay display;
    private final Color color;
    public Transition conn;
    private final Graphics2D g2d;
    private final String event;
    private Label label;
    private Calcs calcs;

    Link(Graphics2D g2d, Step from, Step to, Transition conn) {
        this.g2d = g2d;
        this.from = from;
        this.to = to;
        this.conn = conn;
        event = EventType.getEventTypeName(conn.getEventType());
        color = getColorForType();
        display = new LinkDisplay(conn.getAttribute("TRANSITION_DISPLAY_INFO"));
        calcs = new Calcs(display);
        if (this.getLabelText().length() > 0)
            label = new Label(g2d, new Display(display.getLx(), display.getLy() + LABEL_CORR, 0, 0),
                    this.getLabelText(), Display.DEFAULT_FONT);
    }

    public final String getLabelText() {
        String text = (event == EventType.EVENTNAME_FINISH) ? "" : event + ":";
        text += (conn.getCompletionCode() == null) ? "" : conn.getCompletionCode();
        return text;
    }

    public Display draw() {
        this.g2d.setColor(this.color);
        this.g2d.setPaint((Paint) this.color);
        this.drawConnector(null, null);
        if (this.label != null) {
            label.draw();
        }
        return new Display(0, 0, 0, 0);
    }

     private int getAutoElbowLinkType() {
        String type = this.display.getType();
        int[] xs = this.display.getXs();
        int[] ys = this.display.getYs();
        if (type.equals(ELBOWH)) {
            if (xs[0] == xs[1]) {
                return AUTOLINK_V;
            }
            else if (ys[0] == ys[1]) {
                return AUTOLINK_H;
            }
            else if (Math
                    .abs(to.getDisplay().getX() - from.getDisplay().getX()) > ELBOW_VH_THRESHOLD) {
                return AUTOLINK_HVH;
            }
            else {
                return AUTOLINK_HV;
            }
        }
        else if (type.equals(ELBOWV)) {
            if (xs[0] == xs[1]) {
                return AUTOLINK_V;
            }
            else if (ys[0] == ys[1]) {
                return AUTOLINK_H;
            }
            else if (Math
                    .abs(to.getDisplay().getY() - from.getDisplay().getY()) > ELBOW_VH_THRESHOLD) {
                return AUTOLINK_VHV;
            }
            else {
                return AUTOLINK_VH;
            }
        }
        else {
            if (xs[0] == xs[1]) {
                return AUTOLINK_V;
            }
            else if (ys[0] == ys[1]) {
                return AUTOLINK_H;
            }
            else if (Math.abs(to.getDisplay().getX() - from.getDisplay().getX()) < Math
                    .abs(to.getDisplay().getY() - from.getDisplay().getY()) * ELBOW_THRESHOLD) {
                return AUTOLINK_VHV;
            }
            else if (Math.abs(to.getDisplay().getY() - from.getDisplay().getY()) < Math
                    .abs(to.getDisplay().getX() - from.getDisplay().getX()) * ELBOW_THRESHOLD) {
                return AUTOLINK_HVH;
            }
            else {
                return AUTOLINK_HV;
            }
        }
    }

    private Color getColorForType() {
        Integer eventType = conn.getEventType();
        if (EventType.FINISH.equals(eventType))
            return COLOR_NORMAL;
        else
            return COLOR_OTHER;
    }

    private final boolean drawConnector(Integer hitX, Integer hitY) {
        String type = this.display.getType();
        int[] xs = this.display.getXs();
        int[] ys = this.display.getYs();
        this.g2d.setStroke(LINK_STROKE);
        boolean hit = false;
        if (type.startsWith("Elbow")) {
            if (xs.length == 2) {
                hit = this.drawAutoElbowConnector(hitX, hitY);
            }
            else {
                GeneralPath path = new GeneralPath();
                boolean horizontal = ys[0] == ys[1] && (xs[0] != xs[1] || xs[1] == xs[2]);
                path.moveTo((float) xs[0], (float) ys[0]);
                for (int i = 0; i < xs.length; i++) {
                    if (i != 0) {
                        if (hitX != null && hitY != null) {
                            GeneralPath hitPath = new GeneralPath();
                            hitPath.moveTo((float) xs[i - 1], (float) ys[i - 1]);
                            hitPath.lineTo((float) xs[i], (float) ys[i]);
                            if (hitPath.intersects((double) hitX - HIT_PAD, (double) hitY - HIT_PAD,
                                    HIT_PAD * 2, HIT_PAD * 2)) {
                                return true;
                            }
                        }
                        else if (horizontal) {
                            path.lineTo((xs[i] > xs[i - 1]) ? (float) (xs[i] - CR)
                                    : (float) (xs[i] + CR), (float) ys[i]);
                            if (i < xs.length - 1) {
                                path.quadTo((float) xs[i], (float) ys[i], (float) xs[i],
                                        (ys[i + 1] > ys[i]) ? (float) (ys[i] + CR)
                                                : (float) (ys[i] - CR));
                            }

                            this.g2d.draw(path);
                        }
                        else {
                            path.lineTo((float) xs[i], (ys[i] > ys[i - 1]) ? (float) (ys[i] - CR)
                                    : (float) (ys[i] + CR));
                            if (i < xs.length - 1) {
                                path.quadTo((float) xs[i], (float) ys[i],
                                        (xs[i + 1] > xs[i]) ? (float) (xs[i] + CR)
                                                : (float) (xs[i] - CR),
                                        (float) ys[i]);
                            }

                            this.g2d.draw(path);
                        }

                        horizontal = !horizontal;
                    }
                }
            }
        }
        else if (type == Link.STRAIGHT) {
            List<Seg> segs = new ArrayList<>();
            for (int i = 0; i < xs.length; i++) {
                if (i < xs.length - 1) {
                    segs.add(new Seg(new Pt(xs[i], ys[i]), new Pt(xs[i + 1], ys[i + 1])));
                }
            }

            if (hitX != null && hitY != null) {
                for (Seg seg : segs) {
                    GeneralPath hitPath = new GeneralPath();
                    hitPath.moveTo((float) seg.getFrom().getX(), (float) seg.getFrom().getY());
                    hitPath.lineTo((float) seg.getTo().getX(), (float) seg.getTo().getY());
                    hit = hitPath.intersects(hitX - HIT_PAD, hitY - HIT_PAD, HIT_PAD * 2,
                            HIT_PAD * 2);
                }
            }
            else {
                this.drawLine(segs);
            }
        }

        if (!hit) {
            hit = this.drawConnectorArrow(hitX, hitY);
        }

        this.g2d.setStroke((Stroke) Display.DEFAULT_STROKE);
        this.g2d.setColor(Display.DEFAULT_COLOR);
        this.g2d.setPaint((Paint) Display.DEFAULT_COLOR);
        return hit;
    }

    private final void drawLine(List<Seg> segs) {
        this.g2d.draw(this.getPath(segs));
    }

    private final GeneralPath getPath(List<Seg> segs) {
        GeneralPath path = new GeneralPath();

        for (Seg seg : segs) {
            path.moveTo((float) seg.getFrom().getX(), (float) seg.getFrom().getY());
            path.lineTo((float) seg.getTo().getX(), (float) seg.getTo().getY());
        }
        return path;
    }

    private final boolean drawAutoElbowConnector(Integer hitX, Integer hitY) {
        int[] xs = this.display.getXs();
        int[] ys = this.display.getYs();
        int t = 0;
        int xcorr = (xs[0] < xs[1]) ? 3 : -3;
        int ycorr = (ys[0] < ys[1]) ? 3 : -3;
        GeneralPath path = new GeneralPath();
        switch (this.getAutoElbowLinkType()) {
        case 1:
            path.moveTo((float) (xs[0] - xcorr), (float) ys[0]);
            path.lineTo((float) xs[1], (float) ys[1]);
            break;
        case 2:
            path.moveTo((float) xs[0], (float) (ys[0] - ycorr));
            path.lineTo((float) xs[1], (float) ys[1]);
            break;
        case 3:
            t = (xs[0] + xs[1]) / 2;
            path.moveTo((float) (xs[0] - xcorr), (float) ys[0]);
            path.lineTo((t > xs[0]) ? (float) (t - 8) : (float) (t + 8), (float) ys[0]);
            path.quadTo((float) t, (float) ys[0], (float) t,
                    (ys[1] > ys[0]) ? (float) (ys[0] + 8) : (float) (ys[0] - 8));
            path.lineTo((float) t, (ys[1] > ys[0]) ? (float) (ys[1] - 8) : (float) (ys[1] + 8));
            path.quadTo((float) t, (float) ys[1], (xs[1] > t) ? (float) (t + 8) : (float) (t - 8),
                    (float) ys[1]);
            path.lineTo((float) xs[1], (float) ys[1]);
            break;
        case 4:
            t = (ys[0] + ys[1]) / 2;
            path.moveTo((float) xs[0], (float) (ys[0] - ycorr));
            path.lineTo((float) xs[0], (t > ys[0]) ? (float) (t - 8) : (float) (t + 8));
            path.quadTo((float) xs[0], (float) t,
                    (xs[1] > xs[0]) ? (float) (xs[0] + 8) : (float) (xs[0] - 8), (float) t);
            path.lineTo((xs[1] > xs[0]) ? (float) (xs[1] - 8) : (float) (xs[1] + 8), (float) t);
            path.quadTo((float) xs[1], (float) t, (float) xs[1],
                    (ys[1] > t) ? (float) (t + 8) : (float) (t - 8));
            path.lineTo((float) xs[1], (float) ys[1]);
            break;
        case 5:
            path.moveTo((float) (xs[0] - xcorr), (float) ys[0]);
            path.lineTo((xs[1] > xs[0]) ? (float) (xs[1] - 8) : (float) (xs[1] + 8), (float) ys[0]);
            path.quadTo((float) xs[1], (float) ys[0], (float) xs[1],
                    (ys[1] > ys[0]) ? (float) (ys[0] + 8) : (float) (ys[0] - 8));
            path.lineTo((float) xs[1], (float) ys[1]);
            break;
        case 6:
            path.moveTo((float) xs[0], (float) (ys[0] - ycorr));
            path.lineTo((float) xs[0], (ys[1] > ys[0]) ? (float) (ys[1] - 8) : (float) (ys[1] + 8));
            path.quadTo((float) xs[0], (float) ys[1],
                    (xs[1] > xs[0]) ? (float) (xs[0] + 8) : (float) (xs[0] - 8), (float) ys[1]);
            path.lineTo((float) xs[1], (float) ys[1]);
            break;
        default:
        }

        if (hitX != null && hitY != null) {
            return path.intersects((double) hitX - HIT_PAD, (double) hitY - HIT_PAD, HIT_PAD * 2,
                    HIT_PAD * 2);
        }
        else {
            this.g2d.draw(path);
            return false;
        }
    }

    private final boolean drawConnectorArrow(Integer hitX, Integer hitY) {
        int[] xs = this.display.getXs();
        int[] ys = this.display.getYs();
        int p = 12;
        float slope = 0.0F;
        int x = 0;
        int y = 0;
        int k;
        if (display.getType().equals(Link.STRAIGHT)) {
            k = xs.length - 1;
            int p1 = k - 1;
            x = xs[k];
            y = ys[k];
            slope = calcs.calcSlope(xs[p1], ys[p1], xs[k], ys[k]);
        }
        else if (xs.length == 2) {
            switch (getAutoElbowLinkType()) {
            case 1:
            case 2:
            case 3:
                x = xs[1];
                y = ys[1] > ys[0] ? ys[1] + GAP : ys[1] - GAP;
                slope = ys[1] > ys[0] ? (float) (Math.PI / 2) : (float) (Math.PI * 1.5);
                break;
            case 4:
            case 5:
            case 6:
                x = xs[1] > xs[0] ? xs[1] + GAP : (xs[1]) - GAP;
                y = (ys[1]);
                slope = xs[1] > xs[0] ? 0.0F : (float) Math.PI;
                break;
            default:
            }
        }
        else {
            k = xs.length - 1;
            if (xs[k] == xs[k - 1] && (ys[k] != ys[k - 1] || ys[k - 1] == ys[k - 2])) {
                x = xs[k];
                y = ys[k] > ys[k - 1] ? ys[k] + GAP : ys[k] - GAP;
                slope = ys[k] > ys[k - 1] ? (float) (Math.PI / 2) : (float) (Math.PI * 1.5);
            }
            else {
                x = xs[k] > xs[k - 1] ? xs[k] + GAP : xs[k] - GAP;
                y = ys[k];
                slope = (xs[k]) > (xs[k - 1]) ? 0.0F : (float) Math.PI;
            }
        }

        double dl = (double) slope - 2.7052D;
        double dr = (double) slope + 2.7052D;
        GeneralPath path = new GeneralPath();
        path.moveTo((float) x, (float) y);
        path.lineTo(Math.cos(dl) * (double) p + (double) x, Math.sin(dl) * (double) p + (double) y);
        path.lineTo(Math.cos(dr) * (double) p + (double) x, Math.sin(dr) * (double) p + (double) y);
        path.lineTo((float) x, (float) y);
        if (hitX != null && hitY != null) {
            return path.contains((double) hitX, (double) hitY);
        }
        else {
            this.g2d.fill(path);
            return false;
        }
    }

    class Pt {
        int x;

        /**
         * @return the x
         */
        public int getX() {
            return x;
        }

        /**
         * @param x
         *            the x to set
         */
        public void setX(int x) {
            this.x = x;
        }

        int y;

        /**
         * @return the y
         */
        public int getY() {
            return y;
        }

        /**
         * @param y
         *            the y to set
         */
        public void setY(int y) {
            this.y = y;
        }

        public Pt(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    class Seg {
        Pt from;

        /**
         * @return the from
         */
        public Pt getFrom() {
            return from;
        }

        /**
         * @param from
         *            the from to set
         */
        public void setFrom(Pt from) {
            this.from = from;
        }

        Pt to;

        /**
         * @return the to
         */
        public Pt getTo() {
            return to;
        }

        /**
         * @param to
         *            the to to set
         */
        public void setTo(Pt to) {
            this.to = to;
        }

        public Seg(Pt from, Pt to) {
            this.from = from;
            this.to = to;
        }
    }

    class Calcs {
        private LinkDisplay display = null;

        public final float calcSlope(int x1, int y1, int x2, int y2) {
            float slope;
            if (x1 == x2) {
                slope = y1 < y2 ? (float) (Math.PI / 2) : (float) (-Math.PI / 2);
            }
            else {
                slope = (float) Math.atan((double) (y2 - y1) / (double) (x2 - x1));
                if (x1 > x2) {
                    if (slope > 0) {
                        slope -= (float) Math.PI;
                    }
                    else {
                        slope += (float) Math.PI;
                    }
                }
            }

            return slope;
        }

        public final LinkDisplay getDisplay() {
            return this.display;
        }

        public Calcs(LinkDisplay display) {
            super();
            this.display = display;
        }
    }
}
