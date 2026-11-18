package com.sarat.dsavisualizer.model;

import java.util.List;

/**
 * Request body for a trace endpoint.
 * {@code target} is only used by the search endpoints (nullable).
 * {@code value} is the element to push/enqueue/insert (nullable).
 * {@code index} is the position for list insert/delete (nullable).
 * {@code capacity} is the hash table size (nullable, default 11).
 * {@code graphEdges} is the graph edge list (nullable).
 * {@code start} is the graph BFS/DFS start node (nullable).
 */
public class TraceRequest {

    private List<Integer> values;
    private Integer target;
    private Integer value;
    private Integer index;
    private Integer capacity;
    private List<List<Integer>> graphEdges;
    private Integer start;

    public TraceRequest() {
    }

    public TraceRequest(List<Integer> values, Integer target) {
        this.values = values;
        this.target = target;
    }

    public List<Integer> getValues() {
        return values;
    }

    public void setValues(List<Integer> values) {
        this.values = values;
    }

    public Integer getTarget() {
        return target;
    }

    public void setTarget(Integer target) {
        this.target = target;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public List<List<Integer>> getGraphEdges() {
        return graphEdges;
    }

    public void setGraphEdges(List<List<Integer>> graphEdges) {
        this.graphEdges = graphEdges;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }
}
