package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Hash table with linear probing that records each probe as a Step.
 *
 * Fixed hash function h(v) = Math.floorMod(v, capacity); collisions
 * resolve by linear probing. Default capacity 11. {@code data} is
 * the table array; empty buckets are null (Jackson serialises them
 * as JSON null) and the frontend renders them as empty buckets.
 * {@code highlight} holds bucket indexes.
 * Line numbers: compare/probe = 2, place = 3, final = 0.
 */
@Service
public class HashTableService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_SWAP = 3;
    private static final int LINE_FINAL = 0;
    private static final int DEFAULT_CAPACITY = 11;

    private static int hash(int value, int capacity) {
        return Math.floorMod(value, capacity);
    }

    /**
     * Rebuilds the table by hashing {@code values} in order.
     * A value already present is not inserted twice.
     */
    private List<Integer> buildTable(List<Integer> values, int capacity) {
        List<Integer> table = new ArrayList<>();
        for (int i = 0; i < capacity; i++) {
            table.add(null);
        }
        if (values == null) {
            return table;
        }
        for (int v : values) {
            int h = hash(v, capacity);
            for (int probe = 0; probe < capacity; probe++) {
                int b = (h + probe) % capacity;
                Integer current = table.get(b);
                if (current == null) {
                    table.set(b, v);
                    break;
                }
                if (current == v) {
                    break; // duplicate — do not insert twice
                }
            }
        }
        return table;
    }

    /**
     * Insert a value with linear probing. One Step per bucket
     * checked, one Step when the value is placed, then a final Step.
     * If the value is already present the trace ends with
     * "X is already in the table."; if the table is full it ends
     * with "Table is full.".
     */
    public List<Step> insert(List<Integer> values, int value,
                             Integer capacity) {
        List<Step> steps = new ArrayList<>();
        int cap = capacity != null ? capacity : DEFAULT_CAPACITY;
        List<Integer> table = buildTable(values, cap);
        if (!table.contains(null)) {
            steps.add(tableStep(table, List.of(), List.of(),
                    "Table is full.", LINE_FINAL));
            return steps;
        }
        int h = hash(value, cap);
        for (int probe = 0; probe < cap; probe++) {
            int b = (h + probe) % cap;
            Integer current = table.get(b);
            if (current != null && current == value) {
                steps.add(tableStep(table, List.of(b), List.of(),
                        value + " is already in the table.", LINE_FINAL));
                return steps;
            }
            if (current != null) {
                steps.add(tableStep(table, List.of(b), List.of(),
                        "Hash of " + value + " is " + h + ". Bucket " + b
                                + " holds " + current + " — probe next.",
                        LINE_COMPARE));
                continue;
            }
            steps.add(tableStep(table, List.of(b), List.of(),
                    "Hash of " + value + " is " + h + ". Bucket " + b
                            + " is empty.",
                    LINE_COMPARE));
            table.set(b, value);
            steps.add(tableStep(table, List.of(b), List.of(b),
                    "Inserted " + value + " at bucket " + b + ".", LINE_SWAP));
            steps.add(tableStep(table, List.of(), List.of(),
                    "Table: " + table + ".", LINE_FINAL));
            return steps;
        }
        steps.add(tableStep(table, List.of(), List.of(),
                "Table is full.", LINE_FINAL));
        return steps;
    }

    /**
     * Search for a target with linear probing. One Step per bucket
     * checked; probing stops at the first empty bucket. Ends with
     * "Found X at bucket b!" or "X is not in the table.".
     */
    public List<Step> search(List<Integer> values, int target,
                             Integer capacity) {
        List<Step> steps = new ArrayList<>();
        int cap = capacity != null ? capacity : DEFAULT_CAPACITY;
        List<Integer> table = buildTable(values, cap);
        if (values == null || values.isEmpty()) {
            steps.add(tableStep(table, List.of(), List.of(),
                    "Table is empty.", LINE_FINAL));
            return steps;
        }
        int h = hash(target, cap);
        for (int probe = 0; probe < cap; probe++) {
            int b = (h + probe) % cap;
            Integer current = table.get(b);
            if (current == null) {
                steps.add(tableStep(table, List.of(b), List.of(),
                        "Check bucket " + b + ": empty — not found.",
                        LINE_COMPARE));
                steps.add(tableStep(table, List.of(), List.of(),
                        target + " is not in the table.", LINE_FINAL));
                return steps;
            }
            if (current == target) {
                steps.add(tableStep(table, List.of(b), List.of(),
                        "Check bucket " + b + ": holds " + current + ".",
                        LINE_COMPARE));
                steps.add(tableStep(table, List.of(b), List.of(),
                        "Found " + target + " at bucket " + b + "!",
                        LINE_FINAL));
                return steps;
            }
            steps.add(tableStep(table, List.of(b), List.of(),
                    "Check bucket " + b + ": holds " + current + ".",
                    LINE_COMPARE));
        }
        steps.add(tableStep(table, List.of(), List.of(),
                target + " is not in the table.", LINE_FINAL));
        return steps;
    }

    /** One hash-table Step: table copy in data, bucket indexes in highlight. */
    private Step tableStep(List<Integer> table, List<Integer> highlight,
                           List<Integer> swapped, String message, int line) {
        return new Step(new ArrayList<>(table), highlight, swapped, List.of(),
                message, line);
    }
}
