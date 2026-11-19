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
 * {@code color} is "red" / "black" for red-black tree nodes
 * (Phase 11): the frontend colours the node by it. Null for every
 * other node (BST/AVL leave it null and the frontend renders the
 * default node colour).
 *
 * Jackson-friendly POJO: no-arg constructor plus getters/setters.
 */
public class NodeDto {

    private int id;
    private int value;
    private double x;
    private double y;
    private String label;
    private String color;

    /** No-arg constructor for Jackson. */
    public NodeDto() {
    }

    public NodeDto(int id, int value, double x, double y) {
        this.id = id;
        this.value = value;
        this.x = x;
        this.y = y;
        this.label = null;
        this.color = null;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
