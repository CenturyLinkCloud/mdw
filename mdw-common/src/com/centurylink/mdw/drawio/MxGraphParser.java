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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.centurylink.mdw.constant.WorkAttributeConstant;
import com.centurylink.mdw.drawio.MxEdgeGeometry.Point;
import com.centurylink.mdw.model.event.EventType;
import com.centurylink.mdw.model.workflow.Activity;
import com.centurylink.mdw.model.workflow.Display;
import com.centurylink.mdw.model.workflow.TextNote;
import com.centurylink.mdw.model.workflow.Transition;
import com.centurylink.mdw.model.workflow.TransitionDisplay;

/**
 * Reads activities, transitions and text notes from mxgraph format.
 */
public class MxGraphParser extends DefaultHandler {

    private List<Activity> activities = new ArrayList<>();
    public List<Activity> getActivities() { return activities; }
    public Activity findActivity(Long id) {
        for (Activity activity : activities) {
            if (id.equals(activity.getId()))
                return activity;
        }
        return null;
    }

    private List<Transition> transitions = new ArrayList<>();
    public List<Transition> getTransitions() { return transitions; }
    private Map<Long,MxEdgeGeometry> transitionGeometries = new HashMap<>();

    private List<TextNote> textNotes = new ArrayList<>();
    public List<TextNote> getTextNotes() { return textNotes; }

    public MxGraphParser read(InputStream input) throws IOException, SAXException, ParserConfigurationException {
        InputSource src = new InputSource(input);
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser parser = parserFactory.newSAXParser();
        parser.parse(src, this);

        // set begin/end points for transitions
        for (Transition transition : transitions) {
            Activity fromActivity = findActivity(transition.getFromId());
            Display fromDisplay = new Display(fromActivity.getAttribute(Display.NAME));
            Activity toActivity = findActivity(transition.getToId());
            Display toDisplay = new Display(toActivity.getAttribute(Display.NAME));

            MxEdgeGeometry geometry = transitionGeometries.get(transition.getId());
            TransitionDisplay display = new TransitionDisplay();
            int fromX = Math.round(fromDisplay.x + fromDisplay.w * geometry.exitX);
            int fromY = Math.round(fromDisplay.y + fromDisplay.h * geometry.exitY);
            display.add(fromX, fromY);
            if (geometry.intermediatePoints != null) {
                for (Point point : geometry.intermediatePoints) {
                    display.add(point.x, point.y);
                }
            }
            int toX = Math.round(toDisplay.x + toDisplay.w * geometry.entryX);
            int toY = Math.round(toDisplay.y + toDisplay.h * geometry.entryY);
            display.add(toX, toY);
            // TODO calc label
            transition.setAttribute(TransitionDisplay.NAME, display.toString());
        }

        return this;
    }

    private Object geometryOwner;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        Map<String,String> attrs = new HashMap<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            attrs.put(attributes.getQName(i), attributes.getValue(i));
        }

        if (qName.equals("mxCell") && geometryOwner == null) {
            // top level elements
            long id = Long.parseLong(attrs.get("id"));
            String style = attrs.get("style");

            if (attrs.get("edge") != null) {
                // line
                String source = attrs.get("source");
                String target = attrs.get("target");
                if (source != null && target != null) {
                    Transition transition = new Transition();
                    transition.setId(id);
                    transition.setAttribute(WorkAttributeConstant.LOGICAL_ID, "T" + transition.getId());
                    transition.setFromId(Long.parseLong(source));
                    transition.setToId(Long.parseLong(target));
                    transition.setCompletionCode(attrs.get("value"));
                    transition.setEventType(EventType.FINISH); // TODO
                    transitions.add(transition);
                    geometryOwner = transition;
                    MxEdgeGeometry edgeGeometry = new MxEdgeGeometry();
                    for (String styleElem : style.split(";")) {
                        if (styleElem.startsWith("entryX="))
                            edgeGeometry.entryX = Float.parseFloat(styleElem.substring(7));
                        if (styleElem.startsWith("entryY="))
                            edgeGeometry.entryY = Float.parseFloat(styleElem.substring(7));
                        if (styleElem.startsWith("exitX="))
                            edgeGeometry.exitX = Float.parseFloat(styleElem.substring(6));
                        if (styleElem.startsWith("exitY="))
                            edgeGeometry.exitY = Float.parseFloat(styleElem.substring(6));
                    }
                    transitionGeometries.put(transition.getId(), edgeGeometry);
                }
            }
            else if (style != null && style.startsWith("shape=callout")) {
                // note
                TextNote textNote = new TextNote();
                textNote.setLogicalId("N" + id);
                textNote.setContent(attrs.get("value"));
                textNotes.add(textNote);
                geometryOwner = textNote;
            }
            else if ("1".equals(attrs.get("parent"))) {
                // shape
                Activity activity = new Activity();
                activity.setId(id);
                activity.setAttribute(WorkAttributeConstant.LOGICAL_ID, "A" + id);
                activity.setName(attrs.get("value"));
                // TODO data property for implementor?
                activity.setImplementor("com.centurylink.mdw.workflow.activity.DefaultActivityImpl");
                activities.add(activity);
                geometryOwner = activity;
            }
        }
        else if (qName.equals("mxGeometry")) {
            int x = attrs.get("x") == null ? 0 : Integer.parseInt(attrs.get("x"));
            int y = attrs.get("y") == null ? 0 : Integer.parseInt(attrs.get("y"));
            int w = attrs.get("width") == null ? 0 : Integer.parseInt(attrs.get("width"));
            int h = attrs.get("height") == null ? 0 : Integer.parseInt(attrs.get("height"));
            if (geometryOwner instanceof TextNote) {
                TextNote textNote = (TextNote) geometryOwner;
                Display display = new Display(x, y, w, h);
                textNote.setAttribute(Display.NAME, display.toString());
            }
            else if (geometryOwner instanceof Activity) {
                Activity activity = (Activity) geometryOwner;
                Display display = new Display(x, y, w, h);
                activity.setAttribute(Display.NAME, display.toString());
            }
        }
        else if (qName.equals("mxPoint") && geometryOwner instanceof Transition && attrs.get("as") == null) {
            Transition transition = (Transition) geometryOwner;
            transitionGeometries.get(transition.getId()).addPoint(Integer.parseInt(attrs.get("x")),
                    Integer.parseInt(attrs.get("y")));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("mxCell")) {
            geometryOwner = null;
        }
    }
}
