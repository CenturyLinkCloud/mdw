/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.draw;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.TextNote;
import com.centurylink.mdw.model.workflow.Transition;

public class Diagram implements Drawable {
    private final Label label;
    private final List<Step> steps = new ArrayList<>();
    private final List<Link> links = new ArrayList<>();
    private final List<Note> notes = new ArrayList<>();
    private final List<Subflow> subflows = new ArrayList<>();
    private final Graphics2D g2d;
    private final Display display;
    /**
     * @return the display
     */
    public Display getDisplay() {
        return display;
    }

    private boolean showGrid;
    public static final int BOUNDARY_DIM = 25;
    private Color background;
    public static final int MARQUEE_ROUNDING = 3;
    public static final int PASTE_OFFSET = 20;

    public Diagram(Graphics2D g2d, Display display, Process process, Implementors implementors) {
        super();
        this.g2d = g2d;
        this.display = display;
        this.background = Color.WHITE;
        g2d.setBackground(background);
        this.showGrid = true;
        label = new Label(g2d,
                new Display(process.getAttribute(WorkAttributeConstant.WORK_DISPLAY_INFO)),
                process.getName(), Display.TITLE_FONT);

        // activities
        for (Activity activity : process.getActivities()) {
            Implementor impl = implementors.get(activity.getImplementor());
            if (impl == null)
                impl = new Implementor(activity.getImplementor());
            Step step = new Step(g2d, activity, impl, true);
            steps.add(step);
        }

        // transitions
        for (Step step : steps) {
            for (Transition conn : process.getAllTransitions(step.getActivity().getId())) {
                Link link = new Link(g2d, step, findStep(conn.getToId()), conn);
                links.add(link);
            }
        }

        // subflows
        for (Process subprocess : process.getSubprocesses()) {
            Subflow subflow = new Subflow(g2d, subprocess, implementors);
            subflows.add(subflow);
        }

        // notes
        for (TextNote textNote : process.getTextNotes()) {
            Note note = new Note(g2d, textNote);
            notes.add(note);
        }
    }

    public Step findStep(Long id) {
        for (Step step : steps) {
            if (step.getActivity().getLogicalId().equals("A" + id))
                return step;
        }
        return null;
    }

    public Display draw() {
        if (this.showGrid) {
            this.g2d.setColor(Display.GRID_COLOR);
            this.g2d.setStroke(Display.GRID_STROKE);

            for (int x = 10; x < this.display.getW() + 25; x += 10) {
                this.g2d.drawLine(x, 0, x, this.display.getH() + 25);
            }

            for (int y = 10; y < this.display.getH() + 25; y += 10) {
                this.g2d.drawLine(0, y, this.display.getW() + 25, y);
            }

            this.g2d.setColor(Display.DEFAULT_COLOR);
            this.g2d.setStroke(Display.DEFAULT_STROKE);
        }

        makeRoom(label.draw());

        for (Step step : steps) {
            makeRoom(step.draw());
        }

        for (Link link : links) {
            makeRoom(link.draw());
        }

        for (Subflow subflow : subflows) {
            makeRoom(subflow.draw());
        }

        for (Note note : notes) {
            makeRoom(note.draw());
        }

        return this.display;
    }

    private final void makeRoom(Display extents) {
        int reqWidth = extents.getX() + extents.getW();
        if (reqWidth > this.display.getW()) {
            this.display.setW(reqWidth);
        }
        int reqHeight = extents.getY() + extents.getH();
        if (reqHeight > this.display.getH()) {
            this.display.setH(reqHeight);
        }
    }
}
