/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.draw;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Process;
import com.centurylink.mdw.model.workflow.Transition;

public class Subflow extends Shape implements Drawable {

    private Label label;
    private final List<Step> steps = new ArrayList<>();
    private final List<Link> links = new ArrayList<>();
    private final Process subprocess;
    private static final Color BOX_OUTLINE_COLOR = new Color(210859);

    /**
     * @return the label
     */
    public Label getLabel() {
        return label;
    }

    /**
     * @param label
     *            the label to set
     */
    public void setLabel(Label label) {
        this.label = label;
    }

    /**
     * @return the steps
     */
    public List<Step> getSteps() {
        return steps;
    }

    /**
     * @return the links
     */
    public List<Link> getLinks() {
        return links;
    }

    public Subflow(Graphics2D g2d, Process subprocess, Implementors implementors) {
        super(g2d, new Display(subprocess.getAttribute("WORK_DISPLAY_INFO")));
        this.g2d = g2d;
        this.subprocess = subprocess;
        this.g2d.setFont(Display.DEFAULT_FONT);
        int labelX = this.getDisplay().getX() + 10;
        int labelY = this.getDisplay().getY() - this.g2d.getFontMetrics().getAscent() + Label.PAD;
        this.label = new Label(g2d, new Display(labelX, labelY, 0, 0), this.subprocess.getName(),
                null);
        for (Activity activity : subprocess.getActivities()) {
            Implementor impl = implementors.get(activity.getImplementor());
            if (impl == null)
                impl = new Implementor(activity.getImplementor());
            Step step = new Step(g2d, activity, impl, true);
            steps.add(step);
        }

        // transitions
        for (Step step : steps) {
            for (Transition transition : subprocess.getAllTransitions(step.getActivity().getId())) {
                Link link = new Link(g2d, step, findStep(transition.getToId()), transition);
                links.add(link);
            }
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
        Display extents = new Display(0, 0, this.getDisplay().getX() + this.getDisplay().getW(), this.getDisplay().getY() + this.getDisplay().getH());
        drawRect(this.getDisplay().getX(), this.getDisplay().getY(), this.getDisplay().getW(),
                this.getDisplay().getH(), BOX_OUTLINE_COLOR, null, null);
        extents.setW(Math.max(extents.getW(), this.label.getDisplay().getX() + this.label.getDisplay().getW()));
        extents.setH(Math.max(extents.getH(), this.label.getDisplay().getY()));
        int labelW = this.g2d.getFontMetrics().stringWidth(this.label.getText()) + Label.PAD * 2;
        int labelH = this.g2d.getFontMetrics().getHeight() + Label.PAD * 2;
        clearRect(this.label.getDisplay().getX() - 1, this.label.getDisplay().getY(), labelW + 1, labelH);
        this.label.draw();

        for (Step step : steps) {
            Display d = step.draw();
            extents.setW(Math.max(extents.getW(), d.getX() + d.getW()));
        }

        for (Link link : links) {
            Display d = link.draw();
            extents.setH(Math.max(extents.getH(), d.getY()+ d.getH()));
        }

        if (subprocess.getId() > 0) {
            int metaX = display.getX() + 10;
            int metaY = display.getY() + display.getH() + g2d.getFontMetrics().getDescent();
            int metaW = g2d.getFontMetrics().stringWidth("P"+subprocess.getId());
            int metaH = g2d.getFontMetrics().getHeight();

            extents.setW(Math.max(extents.getW(), metaX + metaW));
            extents.setH(Math.max(extents.getH(), metaY));

            clearRect(metaX - 1, metaY - metaH + g2d.getFontMetrics().getDescent(), metaW + 2, metaH);
            drawText("[P" + subprocess.getId() + "]", metaX, metaY, null, Display.META_COLOR);
        }


        return extents;
     }

}
