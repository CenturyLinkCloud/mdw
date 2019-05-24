package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Linked<T extends Linkable> implements Jsonable {

    protected static final int INDENT = 2;

    private T element;
    public T get() { return element; }

    private Linked<T> parent;
    public Linked<T> getParent() { return parent; }
    public void setParent(Linked<T> parent) { this.parent = parent; }

    private List<Linked<T>> children = new ArrayList<>();
    public List<Linked<T>> getChildren() { return children; }
    public void setChildren(List<Linked<T>> children) { this.children = children; }

    private boolean circular;
    public boolean isCircular() { return circular; }
    public void setCircular(boolean snipped) { this.circular = snipped; }

    public Linked(T element) {
        if (element == null)
            throw new NullPointerException("null element");
        this.element = element;
    }

    public JSONObject getJson() throws JSONException {
        return getJson(0);
    }

    /**
     * Includes children (not parent).
     * @param detail level of detail to include in element summary
     */
    public JSONObject getJson(int detail) throws JSONException {
        JSONObject json = create();
        json.put(element.getObjectName(), element.getSummaryJson(detail));
        if (!children.isEmpty()) {
            JSONArray childrenArr = new JSONArray();
            for (Linked<T> child : children) {
                childrenArr.put(child.getJson(detail));
            }
            json.put("children", childrenArr);
        }
        if (isCircular())
            json.put("circular", true);
        return json;
    }

    @Override
    public String toString() {
        return element.getQualifiedLabel() + (isCircular() ? " (+)" : "");
    }

    /**
     * Indented per specified depth
     */
    public String toString(int depth) {
        if (depth == 0) {
            return toString();
        }
        else {
            return String.format("%1$" + (depth * INDENT) + "s", "") + " - " + toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o.getClass() == getClass() && ((Linked)o).element.equals(element);
    }

    public String getJsonName() {
        return getClass().getSimpleName();
    }

    /**
     * Remove all branches that don't lead to specified element.
     */
    public void prune(T element) {
        if (element.equals(this.element))
            return;
        Linked<T> caller = this;
        List<Linked<T>> toRemove = new ArrayList<>();
        for (Linked<T> child : caller.children) {
            if (!child.contains(element)) {
                toRemove.add(child);
            }
            child.prune(element);
        }
        caller.children.removeAll(toRemove);
    }

    /**
     * Returns true if the call hierarchy contains the specified element.
     */
    public boolean contains(T element) {
        if (element.equals(this.element))
            return true;
        for (Linked<T> child : children) {
            if (child.contains(element))
                return true;
        }
        return false;
    }

    private Linked<T> callChain;  // cached
    /**
     * @return direct line call chain to me
     */
    public Linked<T> getCallChain() {
        if (callChain == null) {
            Linked<T> parent = getParent();
            if (parent == null) {
                callChain = new Linked<>(get());
            }
            else {
                Linked<T> chainedParent = new Linked<>(get());
                while (parent != null) {
                    Linked<T> newParent = new Linked<>(parent.get());
                    newParent.getChildren().add(chainedParent);
                    chainedParent.setParent(newParent);
                    chainedParent = newParent;
                    parent = parent.getParent();
                }
                callChain = chainedParent;
            }
        }
        return callChain;
    }

    public boolean checkCircular() {
        Linked<T> p = getCallChain();
        List<T> called = new ArrayList<>();
        List<Linked<T>> c;
        while (!(c = p.getChildren()).isEmpty()) {
            Linked<T> child = c.get(0);
            if (called.contains(child.get())) {
                setCircular(true);
                return true;
            }
            called.add(child.get());
            p = child;
        }
        return false;
    }

    /**
     * Returns all end-of-the-line elements (no children)
     */
    public List<Linked<T>> getEnds() {
        List<Linked<T>> ends = new ArrayList<>();
        for (Linked<T> child : children) {
            if (child.getChildren().isEmpty() && !ends.contains(child)) {
                ends.add(child);
            }
            else {
                for (Linked<T> end : child.getEnds()) {
                    if (!ends.contains(end))
                        ends.add(end);
                }
            }
        }
        return ends;
    }

    /**
     * Navigates this hierarchy to find first matching element.
     */
    public Linked<T> find(T element) {
        if (this.get().equals(element))
            return this;
        for (Linked<T> child : getChildren()) {
            Linked<T> found = child.find(element);
            if (found != null)
                return found;
        }
        return null;
    }
}
