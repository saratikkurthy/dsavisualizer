package com.sarat.dsavisualizer.model;

/**
 * One edge of a binary search tree, as sent to the frontend:
 * a parent-to-child link, using node ids (see NodeDto).
 *
 * Jackson-friendly POJO: no-arg constructor plus getters/setters.
 */
public class EdgeDto {

    private int from;
    private int to;

    /** No-arg constructor for Jackson. */
    public EdgeDto() {
    }

    public EdgeDto(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }
}
