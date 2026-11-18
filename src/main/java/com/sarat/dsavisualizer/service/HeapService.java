package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Min-heap operations that record each action as a Step.
 *
 * The heap is stored as an array (level order): index i has
 * children 2i+1 and 2i+2. Steps carry BOTH the array ({@code data})
 * and the tree ({@code nodes}/{@code edges}) so the frontend can
 * draw it as a tree; node ids are array indexes.
 *
 * Layout (computed on every snapshot): depth d = floor(log2(i+1)),
 * position in level k = i - (2^d - 1),
 * x = 40 + (k + 0.5) * (480 / 2^d), y = 40 + d * 70.
 * Line numbers: compare = 2, swap = 3, final = 0.
 */
@Service
public class HeapService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_SWAP = 3;
    private static final int LINE_FINAL = 0;

    private static int parent(int i) {
        return (i - 1) / 2;
    }

    private static int left(int i) {
        return 2 * i + 1;
    }

    private static int right(int i) {
        return 2 * i + 2;
    }

    /**
     * Insert a value: append it at the end, then bubble it up
     * while it is smaller than its parent. One Step per comparison
     * and per swap, then a final Step.
     */
    public List<Step> insert(List<Integer> values, int value) {
        List<Step> steps = new ArrayList<>();
        List<Integer> a = values == null
                ? new ArrayList<>() : new ArrayList<>(values);
        a.add(value);
        int i = a.size() - 1;
        while (i > 0) {
            int p = parent(i);
            steps.add(heapStep(a, List.of(i, p), List.of(),
                    "Compare " + a.get(i) + " with parent " + a.get(p) + ".",
                    LINE_COMPARE));
            if (a.get(i) < a.get(p)) {
                int childVal = a.get(i);
                int parentVal = a.get(p);
                a.set(i, parentVal);
                a.set(p, childVal);
                steps.add(heapStep(a, List.of(i, p), List.of(i, p),
                        "Swap " + childVal + " and " + parentVal
                                + " — bubble up.",
                        LINE_SWAP));
                i = p;
            } else {
                break;
            }
        }
        steps.add(heapStep(a, List.of(), List.of(),
                "Inserted " + value + ". Heap: " + a + ".", LINE_FINAL));
        return steps;
    }

    /**
     * Remove the minimum (the root): move the last element to the
     * root, then bubble it down (heapify). One Step per comparison
     * and per swap, then a final Step.
     */
    public List<Step> extractMin(List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            steps.add(heapStep(List.of(), List.of(), List.of(),
                    "Heap is empty — nothing to extract.", LINE_FINAL));
            return steps;
        }
        List<Integer> a = new ArrayList<>(values);
        int min = a.get(0);
        steps.add(heapStep(a, List.of(0), List.of(),
                "Extract min " + min + " (root).", LINE_COMPARE));
        int last = a.remove(a.size() - 1);
        if (a.isEmpty()) {
            steps.add(heapStep(List.of(), List.of(), List.of(),
                    "Extracted " + min + ". Heap is empty.", LINE_FINAL));
            return steps;
        }
        a.set(0, last);
        steps.add(heapStep(a, List.of(0), List.of(),
                "Move " + last + " to the root.", LINE_SWAP));
        int i = 0;
        while (true) {
            int l = left(i);
            int r = right(i);
            int smallest = i;
            if (l < a.size()) {
                steps.add(heapStep(a, List.of(i, l), List.of(),
                        "Compare " + a.get(i) + " with smaller child "
                                + a.get(l) + ".",
                        LINE_COMPARE));
                if (a.get(l) < a.get(smallest)) {
                    smallest = l;
                }
            }
            if (r < a.size()) {
                steps.add(heapStep(a, List.of(i, r), List.of(),
                        "Compare " + a.get(i) + " with smaller child "
                                + a.get(r) + ".",
                        LINE_COMPARE));
                if (a.get(r) < a.get(smallest)) {
                    smallest = r;
                }
            }
            if (smallest == i) {
                break;
            }
            int tmp = a.get(i);
            a.set(i, a.get(smallest));
            a.set(smallest, tmp);
            steps.add(heapStep(a, List.of(i, smallest),
                    List.of(i, smallest),
                    "Swap — bubble down.", LINE_SWAP));
            i = smallest;
        }
        steps.add(heapStep(a, List.of(), List.of(),
                "Extracted " + min + ". Heap: " + a + ".", LINE_FINAL));
        return steps;
    }

    /** One heap Step: array copy in data, tree snapshot in nodes/edges. */
    private Step heapStep(List<Integer> a, List<Integer> highlight,
                          List<Integer> swapped, String message, int line) {
        return new Step(new ArrayList<>(a), highlight, swapped, List.of(),
                message, line, snapshotNodes(a), snapshotEdges(a));
    }

    /** Node snapshot for the array-as-tree layout. */
    private List<NodeDto> snapshotNodes(List<Integer> a) {
        List<NodeDto> nodes = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            int d = 0;
            while ((1 << (d + 1)) <= i + 1) {
                d++;
            }
            int k = i - ((1 << d) - 1);
            double x = 40 + (k + 0.5) * (480.0 / (1 << d));
            double y = 40 + d * 70;
            nodes.add(new NodeDto(i, a.get(i), x, y));
        }
        return nodes;
    }

    /** Edge snapshot: one parent-to-child pair per child in range. */
    private List<EdgeDto> snapshotEdges(List<Integer> a) {
        List<EdgeDto> edges = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            if (left(i) < a.size()) {
                edges.add(new EdgeDto(i, left(i)));
            }
            if (right(i) < a.size()) {
                edges.add(new EdgeDto(i, right(i)));
            }
        }
        return edges;
    }
}
