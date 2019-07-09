package com.centurylink.mdw.model.workflow;

import com.centurylink.mdw.model.Jsonable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public class Linked<T extends Linkable> implements Jsonable, Iterable<Linked<T>> {

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
        if (!getChildren().isEmpty()) {
            JSONArray childrenArr = new JSONArray();
            for (Linked<T> child : getChildren()) {
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

    @Override
    public int hashCode() {
        return element.hashCode();
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
        for (Linked<T> child : caller.getChildren()) {
            if (!child.contains(element)) {
                toRemove.add(child);
            }
            child.prune(element);
        }
        caller.getChildren().removeAll(toRemove);
    }

    /**
     * Returns true if the call hierarchy contains the specified element.
     */
    public boolean contains(T element) {
        if (element.equals(this.element))
            return true;
        for (Linked<T> child : getChildren()) {
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
        if (getChildren().isEmpty()) {
            ends.add(this);
        }
        for (Linked<T> child : getChildren()) {
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
        return find(t -> t.equals(element));
    }

    public Linked<T> find(Predicate<T> predicate) {
        if (predicate.test(this.get()))
            return this;
        for (Linked<T> child : getChildren()) {
            Linked<T> found = child.find(predicate);
            if (found != null)
                return found;
        }
        return null;
    }

    @Override
    public Iterator<Linked<T>> iterator() {
        return new LinkedIterator(getTop());
    }

    public Linked<T> getTop() {
        Linked<T> next = this;
        Linked<T> parent = getParent();
        while (parent != null) {
            next = parent;
            parent = next.getParent();
        }
        return next;
    }

    private class LinkedIterator implements Iterator<Linked<T>> {
        List<Linked<T>> currents;
        int position;
        LinkedIterator(Linked<T> top) {
            currents = new ArrayList<>();
            currents.add(top);
        }
        public boolean hasNext() {
            if (position < currents.size()) {
                return true;
            }
            else {
                for (Linked<T> current : currents) {
                    if (!current.getChildren().isEmpty())
                        return true;
                }
                return false;
            }
        }
        public Linked<T> next() {
            Linked<T> next;
            if (position < currents.size()) {
                next = currents.get(position);
                position++;
            }
            else {
                List<Linked<T>> allChildren = new ArrayList<>();
                for (Linked<T> current : currents) {
                    allChildren.addAll(current.getChildren());
                }
                currents = allChildren;
                if (allChildren.isEmpty())
                    throw new NoSuchElementException(String.valueOf(position));
                next = allChildren.get(0);
                position = 1;
            }
            return next;
        }
    }

    public void dump(PrintStream out, boolean fromTop) {
        dump(out, fromTop, 0);
    }

    public void dump(PrintStream out, boolean fromTop, int maxDepth) {
        print(out, fromTop ? getTop() : this, 0, maxDepth);
    }

    private void print(PrintStream out, List<Linked<T>> parents, int depth, int max) {
        for (Linked<T> caller : parents) {
            print(out, caller, depth, max);
        }
    }

    protected void print(PrintStream out, Linked<T> parent, int depth, int max) {
        if (max != 0 && depth > max) {
            out.println(parent.toString(depth) + " (" + depth + " > " + max + ")");
            return;
        }
        out.println(parent.toString(depth));
        print(out, parent.getChildren(), depth + 1, max);
    }

    public int depth() {
        return getTop().depth(1);
    }

    private int depth(int depth) {
        if (!getChildren().isEmpty()) {
            depth++;
            for (Linked<T> child : getChildren()) {
                depth = child.depth(depth);
            }
        }
        return depth;
    }
}
