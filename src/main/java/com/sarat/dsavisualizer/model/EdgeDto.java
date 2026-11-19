package com.sarat.dsavisualizer.model;

/**
 * One edge of a binary search tree, as sent to the frontend:
 * a parent-to-child link, using node ids (see NodeDto).
 *
 * Also used for graph edges: {@code weight} is the edge weight
 * (Phase 7, Dijkstra), null when the edge is unweighted.
 *
 * Jackson-friendly POJO: no-arg constructor plus getters/setters.
 */
public class EdgeDto {

    private int from;
    private int to;
    private Integer weight;

    /** No-arg constructor for Jackson. */
    public EdgeDto() {
    }

    public EdgeDto(int from, int to) {
        this.from = from;
        this.to = to;
        this.weight = null;
    }

    public EdgeDto(int from, int to, Integer weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
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

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }
}
