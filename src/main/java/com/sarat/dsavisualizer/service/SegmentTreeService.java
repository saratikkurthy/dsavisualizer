package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Segment tree (Phase 13): iterative, 1-indexed, size 2n. Leaves
 * sit at indices n..2n-1, internal node i = tree[2i] + tree[2i+1].
 * {@code data} is the whole tree array (index 0 unused, kept as 0
 * so array positions match tree indices). The tree drawing uses
 * {@code nodes}/{@code edges}: node id = tree index, value = the
 * node's sum, x/y from the heap layout (depth = floor(log2(i)),
 * position within the level = i &minus; 2^depth, levels centred so
 * a parent sits above the midpoint of its children — the same
 * 70px spacing the BST layout uses).
 *
 * Build: one Step per internal node computed, bottom-up (i = n-1
 * down to 1). Query: the standard half-open iterative walk
 * (l += n, r += n + 1); a Step per tree node whose sum is added to
 * the running total. Update: the leaf, then each ancestor up to
 * the root, one Step each. A computed / recomputed node is line 3
 * (a mutation), a visited node line 2, the summary line 0.
 */
@Service
public class SegmentTreeService {

    private static final int LINE_VISIT = 2;
    private static final int LINE_MUTATE = 3;
    private static final int LINE_FINAL = 0;

    /** Build the segment tree over {@code values}. */
    public List<Step> build(List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            steps.add(treeStep(emptyTree(), List.of(),
                    "No values to build a segment tree from.",
                    LINE_FINAL));
            return steps;
        }
        int n = values.size();
        List<Integer> tree = emptyTree(2 * n);
        for (int i = 0; i < n; i++) {
            tree.set(n + i, values.get(i));
        }
        for (int i = n - 1; i >= 1; i--) {
            int left = tree.get(2 * i);
            int right = tree.get(2 * i + 1);
            tree.set(i, left + right);
            steps.add(treeStep(tree, List.of(i),
                    "tree[" + i + "] = tree[" + (2 * i) + "] + tree["
                            + (2 * i + 1) + "] = " + left + " + " + right
                            + " = " + (left + right) + ".",
                    LINE_MUTATE));
        }
        steps.add(treeStep(tree, List.of(1),
                "Segment tree built. Total sum " + tree.get(1) + ".",
                LINE_FINAL));
        return steps;
    }

    /**
     * Range sum over [left, right] (inclusive). The controller
     * validates the range; this method guards against bad input
     * anyway with a single "empty" Step.
     */
    public List<Step> query(List<Integer> values, int left, int right) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()
                || left < 0 || right < left || right >= values.size()) {
            steps.add(treeStep(emptyTree(), List.of(),
                    "A valid range inside the array is needed.",
                    LINE_FINAL));
            return steps;
        }
        int n = values.size();
        List<Integer> tree = buildTree(values);
        // Half-open walk over [left + n, right + n + 1).
        int l = left + n;
        int r = right + n + 1;
        int total = 0;
        while (l < r) {
            if (l % 2 == 1) {
                total += tree.get(l);
                steps.add(treeStep(tree, List.of(l),
                        "Visit tree[" + l + "] = " + tree.get(l)
                                + ". Running sum = " + total + ".",
                        LINE_VISIT));
                l++;
            }
            if (r % 2 == 1) {
                r--;
                total += tree.get(r);
                steps.add(treeStep(tree, List.of(r),
                        "Visit tree[" + r + "] = " + tree.get(r)
                                + ". Running sum = " + total + ".",
                        LINE_VISIT));
            }
            l /= 2;
            r /= 2;
        }
        steps.add(treeStep(tree, List.of(),
                "Range sum [" + left + ", " + right + "] = " + total
                        + ".",
                LINE_FINAL));
        return steps;
    }

    /**
     * Point update: set position {@code index} to {@code newValue},
     * then recompute each ancestor up to the root. The controller
     * validates the index; this method guards against bad input
     * anyway with a single "empty" Step.
     */
    public List<Step> update(List<Integer> values, int index,
                             int newValue) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()
                || index < 0 || index >= values.size()) {
            steps.add(treeStep(emptyTree(), List.of(),
                    "A valid position inside the array is needed.",
                    LINE_FINAL));
            return steps;
        }
        int n = values.size();
        List<Integer> tree = buildTree(values);
        int pos = n + index;
        int old = tree.get(pos);
        tree.set(pos, newValue);
        steps.add(treeStep(tree, List.of(pos),
                "Update leaf tree[" + pos + "] from " + old + " to "
                        + newValue + ".",
                LINE_MUTATE));
        pos /= 2;
        while (pos >= 1) {
            int left = tree.get(2 * pos);
            int right = tree.get(2 * pos + 1);
            tree.set(pos, left + right);
            steps.add(treeStep(tree, List.of(pos),
                    "Recompute tree[" + pos + "] = tree[" + (2 * pos)
                            + "] + tree[" + (2 * pos + 1) + "] = "
                            + left + " + " + right + " = "
                            + (left + right) + ".",
                    LINE_MUTATE));
            pos /= 2;
        }
        steps.add(treeStep(tree, List.of(1),
                "Updated position " + index + " to " + newValue
                        + ". New total " + tree.get(1) + ".",
                LINE_FINAL));
        return steps;
    }

    /** The tree array for {@code values}, built without any steps. */
    private List<Integer> buildTree(List<Integer> values) {
        int n = values.size();
        List<Integer> tree = emptyTree(2 * n);
        for (int i = 0; i < n; i++) {
            tree.set(n + i, values.get(i));
        }
        for (int i = n - 1; i >= 1; i--) {
            tree.set(i, tree.get(2 * i) + tree.get(2 * i + 1));
        }
        return tree;
    }

    /** An all-zero tree array of the given size. */
    private List<Integer> emptyTree() {
        return new ArrayList<>();
    }

    /** An all-zero tree array of the given size. */
    private List<Integer> emptyTree(int size) {
        return new ArrayList<>(Collections.nCopies(size, 0));
    }

    /**
     * One segment-tree Step: a copy of the tree array, highlight =
     * the tree index the step is about, and the nodes/edges for the
     * heap-layout drawing.
     */
    private Step treeStep(List<Integer> tree, List<Integer> highlight,
                          String message, int line) {
        return new Step(new ArrayList<>(tree), highlight, List.of(),
                List.of(), message, line,
                layoutNodes(tree), layoutEdges(tree.size()));
    }

    /**
     * NodeDtos for tree indices 1..size-1. Depth = floor(log2(i)),
     * position within the level = i &minus; 2^depth. Each level is
     * centred: the level's slot width is 70px * 2^(maxDepth-depth),
     * so a parent sits above the midpoint of its children.
     */
    private List<NodeDto> layoutNodes(List<Integer> tree) {
        int size = tree.size();
        if (size <= 1) {
            return new ArrayList<>();
        }
        int maxDepth = 31 - Integer.numberOfLeadingZeros(size - 1);
        List<NodeDto> nodes = new ArrayList<>();
        for (int i = 1; i < size; i++) {
            int depth = 31 - Integer.numberOfLeadingZeros(i);
            int posInLevel = i - (1 << depth);
            double slotWidth = 70.0 * (1 << (maxDepth - depth));
            double x = 40 + (posInLevel + 0.5) * slotWidth;
            double y = 40 + depth * 70.0;
            nodes.add(new NodeDto(i, tree.get(i), x, y));
        }
        return nodes;
    }

    /** Parent-to-child edges for tree indices 1..size-1. */
    private List<EdgeDto> layoutEdges(int size) {
        List<EdgeDto> edges = new ArrayList<>();
        for (int i = 1; i < size; i++) {
            if (2 * i < size) {
                edges.add(new EdgeDto(i, 2 * i));
            }
            if (2 * i + 1 < size) {
                edges.add(new EdgeDto(i, 2 * i + 1));
            }
        }
        return edges;
    }
}
