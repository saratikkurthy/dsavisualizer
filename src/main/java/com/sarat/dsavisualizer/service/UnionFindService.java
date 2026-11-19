package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Union-Find (Phase 13): union by rank + path compression
 * (recursive find with compression). {@code values} is the element
 * set, {@code graphEdges} the union pairs in order.
 *
 * One Step per union: the find steps for each side first (the
 * message names the root found, and notes when path compression
 * rewires a parent), then the union step. {@code data} is the
 * parent array after that union (indexed by position in
 * {@code values}); {@code highlight} is the pair being unioned,
 * as positions in {@code values}. The final Step lists the sets.
 *
 * The parent array in the final Step is the state right after the
 * unions: the sets for the final message are computed by a
 * read-only root walk, so no further find compresses anything
 * after the snapshot (a later find(5) would compress parent[5]
 * to 0 — the asserted array is the state before any such find).
 * A computed node is line 3 (a mutation), a find step line 2,
 * the summary line 0.
 */
@Service
public class UnionFindService {

    private static final int LINE_VISIT = 2;
    private static final int LINE_MUTATE = 3;
    private static final int LINE_FINAL = 0;

    /**
     * Run the union pairs in order over the element set. The
     * controller validates the input (non-empty elements, at most
     * 8, pairs of two distinct elements from the set); this method
     * guards against bad input anyway with a single "empty" Step.
     */
    public List<Step> union(List<Integer> values,
                            List<List<Integer>> pairs) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()
                || pairs == null || pairs.isEmpty()) {
            steps.add(ufStep(List.of(), List.of(),
                    "An element set and union pairs are needed.",
                    LINE_FINAL));
            return steps;
        }
        int n = values.size();
        Map<Integer, Integer> pos = new HashMap<>();
        for (int i = 0; i < n; i++) {
            pos.put(values.get(i), i);
        }
        int[] parent = new int[n];
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        for (List<Integer> pair : pairs) {
            if (pair == null || pair.size() != 2
                    || pair.get(0) == null || pair.get(1) == null
                    || !pos.containsKey(pair.get(0))
                    || !pos.containsKey(pair.get(1))) {
                continue; // the controller rejects these pairs
            }
            int a = pair.get(0);
            int b = pair.get(1);
            int rootA = find(a, parent, pos, values, steps);
            int rootB = find(b, parent, pos, values, steps);
            List<Integer> highlight = List.of(pos.get(a), pos.get(b));
            if (rootA == rootB) {
                steps.add(ufStep(parent, highlight,
                        "Union " + a + " and " + b + ": already in the "
                                + "same set (root " + rootA + ").",
                        LINE_VISIT));
                continue;
            }
            int ia = pos.get(rootA);
            int ib = pos.get(rootB);
            String attached;
            if (rank[ia] < rank[ib]) {
                parent[ia] = ib;
                attached = "attach root " + rootA + " under root "
                        + rootB;
            } else if (rank[ia] > rank[ib]) {
                parent[ib] = ia;
                attached = "attach root " + rootB + " under root "
                        + rootA;
            } else {
                parent[ib] = ia;
                rank[ia]++;
                attached = "attach root " + rootB + " under root "
                        + rootA + " (equal ranks — rank of " + rootA
                        + " becomes " + rank[ia] + ")";
            }
            steps.add(ufStep(parent, highlight,
                    "Union " + a + " and " + b + ": " + attached + ".",
                    LINE_MUTATE));
        }
        // Snapshot the parent array first, then compute the sets
        // with a read-only root walk (no compression after the
        // snapshot).
        List<Integer> parentSnapshot = copyParent(parent);
        List<List<Integer>> sets = setsOf(values, parent, pos);
        StringBuilder setsText = new StringBuilder();
        for (int i = 0; i < sets.size(); i++) {
            if (i > 0) {
                setsText.append(", ");
            }
            setsText.append("{").append(joinInts(sets.get(i)))
                    .append("}");
        }
        String noun = sets.size() == 1 ? " set: " : " sets: ";
        steps.add(ufStep(parentSnapshot, List.of(),
                sets.size() + noun + setsText + ".", LINE_FINAL));
        return steps;
    }

    /**
     * Find the root of element {@code x}, with path compression.
     * Emits one Step naming the chain walked, the root found, and
     * any parent rewired by compression. Returns the root element.
     */
    private int find(int x, int[] parent, Map<Integer, Integer> pos,
                     List<Integer> values, List<Step> steps) {
        // Walk the chain from x up to its root.
        List<Integer> chain = new ArrayList<>();
        int cur = pos.get(x);
        while (parent[cur] != cur) {
            chain.add(values.get(cur));
            cur = parent[cur];
        }
        chain.add(values.get(cur)); // the root
        int root = values.get(cur);
        // Rewire every node on the chain straight to the root.
        List<String> rewired = new ArrayList<>();
        for (int j = 0; j < chain.size() - 1; j++) {
            int nodePos = pos.get(chain.get(j));
            if (parent[nodePos] != cur) {
                rewired.add("parent[" + chain.get(j) + "] = " + root);
                parent[nodePos] = cur;
            }
        }
        String message = "Find(" + x + "): " + joinArrows(chain)
                + ". Root is " + root + ".";
        if (!rewired.isEmpty()) {
            message += " Path compression: "
                    + String.join(", ", rewired) + ".";
        }
        steps.add(ufStep(parent, List.of(pos.get(x)), message,
                LINE_VISIT));
        return root;
    }

    /**
     * The sets, grouped by root (read-only walk — no path
     * compression), in order of first appearance in
     * {@code values}.
     */
    private List<List<Integer>> setsOf(List<Integer> values,
                                       int[] parent,
                                       Map<Integer, Integer> pos) {
        Map<Integer, List<Integer>> groups = new HashMap<>();
        List<Integer> order = new ArrayList<>();
        for (int element : values) {
            int i = pos.get(element);
            int root = i;
            while (parent[root] != root) {
                root = parent[root];
            }
            int rootElement = values.get(root);
            if (!groups.containsKey(rootElement)) {
                groups.put(rootElement, new ArrayList<>());
                order.add(rootElement);
            }
            groups.get(rootElement).add(element);
        }
        List<List<Integer>> sets = new ArrayList<>();
        for (int rootElement : order) {
            sets.add(groups.get(rootElement));
        }
        return sets;
    }

    /** One union-find Step: a copy of the parent array. */
    private Step ufStep(int[] parent, List<Integer> highlight,
                        String message, int line) {
        return new Step(copyParent(parent), highlight, List.of(),
                List.of(), message, line);
    }

    /** One union-find Step from an already-copied parent array. */
    private Step ufStep(List<Integer> parent, List<Integer> highlight,
                        String message, int line) {
        return new Step(new ArrayList<>(parent), highlight, List.of(),
                List.of(), message, line);
    }

    /** A copy of the parent array as a List. */
    private List<Integer> copyParent(int[] parent) {
        List<Integer> copy = new ArrayList<>();
        for (int p : parent) {
            copy.add(p);
        }
        return copy;
    }

    /** "0, 1, 2" from [0, 1, 2]. */
    private String joinInts(List<Integer> ints) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ints.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(ints.get(i));
        }
        return sb.toString();
    }

    /** "3 → 2 → 0" from [3, 2, 0]. */
    private String joinArrows(List<Integer> ints) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ints.size(); i++) {
            if (i > 0) {
                sb.append(" → ");
            }
            sb.append(ints.get(i));
        }
        return sb.toString();
    }
}
