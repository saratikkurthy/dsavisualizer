package com.sarat.dsavisualizer.model;

/**
 * One node of a binary search tree, as sent to the frontend.
 * {@code x} and {@code y} are the screen coordinates the backend
 * computes (see BstService): in-order rank for x, depth for y.
 *
 * {@code label} is the character for Trie nodes (Phase 8): the
 * frontend shows {@code label} when present, else {@code value}.
 * Null for every other node.
 *
 * Jackson-friendly POJO: no-arg constructor plus getters/setters.
 */
public class NodeDto {

    private int id;
    private int value;
    private double x;
    private double y;
    private String label;

    /** No-arg constructor for Jackson. */
    public NodeDto() {
    }

    public NodeDto(int id, int value, double x, double y) {
        this.id = id;
        this.value = value;
        this.x = x;
        this.y = y;
        this.label = null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
