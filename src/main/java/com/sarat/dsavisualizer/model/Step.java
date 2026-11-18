package com.sarat.dsavisualizer.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One snapshot in an algorithm's trace — the contract between the
 * backend and the frontend (see DESIGN.md).
 *
 * <ul>
 *   <li>{@code data} — the array after this step</li>
 *   <li>{@code highlight} — indexes being compared / probed</li>
 *   <li>{@code swapped} — indexes just swapped (empty if none)</li>
 *   <li>{@code sorted} — indexes already in their final place</li>
 *   <li>{@code message} — one-line explanation shown to the learner</li>
 *   <li>{@code line} — line number in the displayed code panel</li>
 * </ul>
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

    /** No-arg constructor for Jackson. */
    public Step() {
        this.data = new ArrayList<>();
        this.highlight = new ArrayList<>();
        this.swapped = new ArrayList<>();
        this.sorted = new ArrayList<>();
        this.message = "";
        this.line = 0;
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
}
