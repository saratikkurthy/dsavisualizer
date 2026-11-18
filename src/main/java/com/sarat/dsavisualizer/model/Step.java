package com.sarat.dsavisualizer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One snapshot in an algorithm's trace — the contract between the
 * backend and the frontend (see DESIGN.md).
 *
 * <ul>
 *   <li>{@code data} — the array after this step</li>
 *   <li>{@code highlight} — indexes being compared / probed
 *       (for BST steps: node ids)</li>
 *   <li>{@code swapped} — indexes just swapped (empty if none)</li>
 *   <li>{@code sorted} — indexes already in their final place</li>
 *   <li>{@code message} — one-line explanation shown to the learner</li>
 *   <li>{@code line} — line number in the displayed code panel</li>
 *   <li>{@code nodes} — BST nodes (null for array-based steps)</li>
 *   <li>{@code edges} — BST parent-to-child edges
 *       (null for array-based steps)</li>
 * </ul>
 *
 * For BST steps {@code data} is empty and {@code nodes}/{@code edges}
 * carry the tree. For all array-based steps {@code nodes} and
 * {@code edges} stay null and the frontend ignores them.
 *
 * Jackson-friendly POJO: no-arg constructor plus getters/setters.
 */
public class Step {

    private List<Integer> data;
    private List<Integer> highlight;
    private List<Integer> swapped;
    private List<Integer> sorted;
    private String message;
    private int line;
    private List<NodeDto> nodes;
    private List<EdgeDto> edges;

    /** No-arg constructor for Jackson. Nodes/edges stay null. */
    public Step() {
        this.data = new ArrayList<>();
        this.highlight = new ArrayList<>();
        this.swapped = new ArrayList<>();
        this.sorted = new ArrayList<>();
        this.message = "";
        this.line = 0;
        this.nodes = null;
        this.edges = null;
    }

    public Step(List<Integer> data, List<Integer> highlight,
                List<Integer> swapped, List<Integer> sorted,
                String message, int line) {
        this.data = data;
        this.highlight = highlight;
        this.swapped = swapped;
        this.sorted = sorted;
        this.message = message;
        this.line = line;
        this.nodes = null;
        this.edges = null;
    }

    /** Full constructor, used by the BST service (nodes + edges). */
    public Step(List<Integer> data, List<Integer> highlight,
                List<Integer> swapped, List<Integer> sorted,
                String message, int line,
                List<NodeDto> nodes, List<EdgeDto> edges) {
        this.data = data;
        this.highlight = highlight;
        this.swapped = swapped;
        this.sorted = sorted;
        this.message = message;
        this.line = line;
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<Integer> getData() {
        return data;
    }

    public void setData(List<Integer> data) {
        this.data = data;
    }

    public List<Integer> getHighlight() {
        return highlight;
    }

    public void setHighlight(List<Integer> highlight) {
        this.highlight = highlight;
    }

    public List<Integer> getSwapped() {
        return swapped;
    }

    public void setSwapped(List<Integer> swapped) {
        this.swapped = swapped;
    }

    public List<Integer> getSorted() {
        return sorted;
    }

    public void setSorted(List<Integer> sorted) {
        this.sorted = sorted;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public List<NodeDto> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeDto> nodes) {
        this.nodes = nodes;
    }

    public List<EdgeDto> getEdges() {
        return edges;
    }

    public void setEdges(List<EdgeDto> edges) {
        this.edges = edges;
    }
}
