package com.sarat.dsavisualizer.model;

import java.util.List;

/**
 * Request body for a trace endpoint.
 * {@code target} is only used by the search endpoints (nullable).
 */
public class TraceRequest {

    private List<Integer> values;
    private Integer target;

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
}
