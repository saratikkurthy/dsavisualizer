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
 * {@code words} is the words already in the trie (nullable).
 * {@code word} is the word to insert into / search in the trie
 * (nullable). NOTE: the trie search reads {@code word}, not
 * {@code target} — {@code target} is an Integer and cannot hold a
 * word (Phase 8).
 * {@code weights}/{@code values} are the knapsack item weights and
 * values (nullable); {@code capacity} doubles as the knapsack
 * capacity for the DP endpoint (it already existed as the hash
 * table size — Phase 10 reuses the same field).
 */
public class TraceRequest {

    private List<Integer> values;
    private Integer target;
    private Integer value;
    private Integer index;
    private Integer capacity;
    private List<List<Integer>> graphEdges;
    private Integer start;
    private List<String> words;
    private String word;
    private List<Integer> weights;
    private List<Integer> values;

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

    public List<String> getWords() {
        return words;
    }

    public void setWords(List<String> words) {
        this.words = words;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public List<Integer> getWeights() {
        return weights;
    }

    public void setWeights(List<Integer> weights) {
        this.weights = weights;
    }

    public List<Integer> getValues() {
        return values;
    }

    public void setValues(List<Integer> values) {
        this.values = values;
    }
}
