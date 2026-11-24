package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Lazy segment tree (Phase 27, range queries v3): a segment
 * tree with a lazy tag per node — a range add that fully
 * covers a node stops there (the tag records the pending
 * add, the sum is updated at once) and is pushed to the
 * children only when a later operation needs to go deeper.
 * Range add and range sum are both O(log n). The idea
 * behind lazy propagation in segment trees, Fenwick trees
 * with range updates, and sqrt decomposition with lazy
 * blocks (Phase 27).
 *
 * <p>Demo array [1, 2, 3, 4, 5, 6, 7, 8] (n=8), range add
 * +10 to [2, 5], then queries sum [0, 7] and sum [2, 5].
 * Heap layout, node ids 1..15: 1:[0,7] 2:[0,3] 3:[4,7]
 * 4:[0,1] 5:[2,3] 6:[4,5] 7:[6,7] 8..15 leaves. Build
 * sums: root 36, [0,3]=10, [4,7]=26. The update touches
 * FULL nodes 5 (sum 7 -&gt; 27, lazy 10) and 6 (sum 11
 * -&gt; 31, lazy 10); pulls: node 2 -&gt; 30, node 3
 * -&gt; 46, root -&gt; 76. Lazy tags are left only on
 * nodes 5 and 6. Queries: sum[0,7]=76 (the root covers
 * it), sum[2,5]=58 (nodes 5 + 6 = 27 + 31). The array is
 * now [1, 2, 13, 14, 15, 16, 7, 8]. Trace: 1 build step
 * (line 1) + 7 update steps (line 2) + 2 query steps
 * (line 3) + final (line 0) = 11 steps. The 7 update
 * steps are: enter 1, enter 2, FULL 5, PULL 2, FULL 6,
 * PULL 3, PULL 1. "Enter" steps narrate the first
 * (leftmost) descent only — the root and the partial
 * nodes reached by descending through left children
 * (nodes 1 and 2 here); entering a right subtree is
 * folded into that subtree's first FULL step, so there
 * is no "enter 3" step (the recursion does enter node 3,
 * but the step list shows it through FULL 6 and PULL 3).
 * Node {@code value} = segment sum, {@code label} = the
 * range string "[l,r]". Lazy tags go in {@code data}: a
 * flat list of [nodeId, tag] pairs for every node with a
 * nonzero lazy tag after that step (the FULL-5 step has
 * data [5, 10]; after the update the steps have data
 * [5, 10, 6, 10]). {@code highlight} = [node ids touched
 * this step]. Query steps: {@code data} = [answer].
 * {@code edges} = the 14 parent-to-child heap edges,
 * weight null, on every step.</p>
 *
 * <p>Bad input (an unparseable array, a range that is not
 * [l, r] with 0 &le; l &le; r &lt; n, or a missing add
 * amount) yields a single "empty" Step; the controller
 * 400s on it (see VisualizationController).</p>
 */
@Service
public class LazySegService {

    private static final int LINE_BUILD = 1;
    private static final int LINE_UPDATE = 2;
    private static final int LINE_QUERY = 3;
    private static final int LINE_FINAL = 0;

    /** Per-call mutable state (the service bean is a singleton, so no instance state). */
    private static final class Tree {
        final int n;
        final int[] sum;
        final int[] lazy;
        final List<Step> steps;

        Tree(int[] array, List<Step> steps) {
            this.n = array.length;
            this.sum = new int[4 * n + 4];
            this.lazy = new int[4 * n + 4];
            this.steps = steps;
            build(1, 0, n - 1, array);
        }

        private void build(int v, int tl, int tr, int[] array) {
            if (tl == tr) {
                sum[v] = array[tl];
            } else {
                int tm = (tl + tr) / 2;
                build(v * 2, tl, tm, array);
                build(v * 2 + 1, tm + 1, tr, array);
                sum[v] = sum[v * 2] + sum[v * 2 + 1];
            }
        }

        private static String rangeLabel(int tl, int tr) {
            return "[" + tl + "," + tr + "]";
        }

        /** One NodeDto per heap node (ids 1..2n-1), in id order. */
        List<NodeDto> snapshot() {
            NodeDto[] byId = new NodeDto[2 * n];
            snapshotInto(1, 0, n - 1, byId);
            List<NodeDto> dtos = new ArrayList<>();
            for (int v = 1; v <= 2 * n - 1; v++) {
                dtos.add(byId[v]);
            }
            return dtos;
        }

        private void snapshotInto(int v, int tl, int tr,
                                  NodeDto[] byId) {
            NodeDto dto = new NodeDto(v, sum[v], 0, 0);
            dto.setLabel(rangeLabel(tl, tr));
            byId[v] = dto;
            if (tl != tr) {
                int tm = (tl + tr) / 2;
                snapshotInto(v * 2, tl, tm, byId);
                snapshotInto(v * 2 + 1, tm + 1, tr, byId);
            }
        }

        /** The 14 (2n-2) parent-to-child heap edges, in id order. */
        List<EdgeDto> edgeSnapshot() {
            List<EdgeDto> edges = new ArrayList<>();
            edgeSnapshotInto(1, 0, n - 1, edges);
            edges.sort((a, b) -> a.getFrom() != b.getFrom()
                    ? Integer.compare(a.getFrom(), b.getFrom())
                    : Integer.compare(a.getTo(), b.getTo()));
            return edges;
        }

        private void edgeSnapshotInto(int v, int tl, int tr,
                                      List<EdgeDto> edges) {
            if (tl != tr) {
                int tm = (tl + tr) / 2;
                edges.add(new EdgeDto(v, v * 2, null));
                edges.add(new EdgeDto(v, v * 2 + 1, null));
                edgeSnapshotInto(v * 2, tl, tm, edges);
                edgeSnapshotInto(v * 2 + 1, tm + 1, tr, edges);
            }
        }

        /** Flat [nodeId, tag] pairs for every node with a nonzero lazy tag, id order. */
        List<Integer> lazyPairs() {
            List<Integer> pairs = new ArrayList<>();
            for (int v = 1; v < lazy.length; v++) {
                if (lazy[v] != 0) {
                    pairs.add(v);
                    pairs.add(lazy[v]);
                }
            }
            return pairs;
        }

        /** Pushes v's tag to its children (no-op when the tag is 0 or v is a leaf). */
        private void push(int v, int tl, int tr) {
            if (lazy[v] != 0 && tl != tr) {
                int tm = (tl + tr) / 2;
                apply(v * 2, tl, tm, lazy[v]);
                apply(v * 2 + 1, tm + 1, tr, lazy[v]);
                lazy[v] = 0;
            }
        }

        /** Applies an add to node v covering [tl, tr] (sum and tag). */
        private void apply(int v, int tl, int tr, int add) {
            sum[v] += add * (tr - tl + 1);
            lazy[v] += add;
        }

        /**
         * Range add with trace steps. {@code narrate} is true
         * on the left spine of the recursion (the root and
         * partial nodes reached through left children only) —
         * those nodes get an "enter" step; a right subtree's
         * entry is folded into its first FULL step (see the
         * class Javadoc — this is what makes the demo's
         * update exactly 7 steps with no "enter 3").
         */
        void update(int v, int tl, int tr, int l, int r, int add,
                    boolean narrate, int origL, int origR) {
            if (l > r) {
                return;
            }
            if (l == tl && r == tr) {
                int before = sum[v];
                apply(v, tl, tr, add);
                steps.add(new Step(lazyPairs(), List.of(v),
                        List.of(), List.of(),
                        "Node " + v + " " + rangeLabel(tl, tr)
                                + " is fully inside the update range ["
                                + origL + ", " + origR + "] — add "
                                + add + ": sum " + before
                                + " -> " + sum[v] + ", lazy tag "
                                + lazy[v] + ".",
                        LINE_UPDATE, snapshot(), edgeSnapshot()));
                return;
            }
            push(v, tl, tr);
            if (narrate) {
                steps.add(new Step(lazyPairs(), List.of(v),
                        List.of(), List.of(),
                        "Enter node " + v + " " + rangeLabel(tl, tr)
                                + " — partially covered by [" + origL
                                + ", " + origR + "].",
                        LINE_UPDATE, snapshot(), edgeSnapshot()));
            }
            int tm = (tl + tr) / 2;
            update(v * 2, tl, tm, l, Math.min(r, tm), add, narrate,
                    origL, origR);
            update(v * 2 + 1, tm + 1, tr, Math.max(l, tm + 1), r,
                    add, false, origL, origR);
            sum[v] = sum[v * 2] + sum[v * 2 + 1];
            steps.add(new Step(lazyPairs(), List.of(v),
                    List.of(), List.of(),
                    "Pull node " + v + " " + rangeLabel(tl, tr)
                            + ": " + sum[v * 2] + " + "
                            + sum[v * 2 + 1] + " = " + sum[v] + ".",
                    LINE_UPDATE, snapshot(), edgeSnapshot()));
        }

        /** Range sum; the contributing (fully covered) node ids are collected. */
        int query(int v, int tl, int tr, int l, int r,
                  List<Integer> contributors) {
            if (l > r) {
                return 0;
            }
            if (l == tl && r == tr) {
                contributors.add(v);
                return sum[v];
            }
            push(v, tl, tr);
            int tm = (tl + tr) / 2;
            return query(v * 2, tl, tm, l, Math.min(r, tm),
                    contributors)
                    + query(v * 2 + 1, tm + 1, tr,
                            Math.max(l, tm + 1), r, contributors);
        }
    }

    /**
     * Builds the lazy segment tree of the array in
     * {@code word}, adds {@code index} to [values[0],
     * values[1]], then answers sum [values[2], values[3]]
     * and sum [values[4], values[5]].
     */
    public List<Step> lazySeg(String word, List<Integer> values,
                              Integer index) {
        List<Step> steps = new ArrayList<>();
        int[] parsed = parseArray(word);
        if (parsed == null || parsed.length == 0) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The array is needed as "
                            + "comma-separated numbers.", LINE_FINAL));
            return steps;
        }
        if (values == null || values.size() != 6) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The update range and the two "
                            + "query ranges are needed as "
                            + "[ul, ur, q1l, q1r, q2l, q2r].",
                    LINE_FINAL));
            return steps;
        }
        int ul = values.get(0);
        int ur = values.get(1);
        int q1l = values.get(2);
        int q1r = values.get(3);
        int q2l = values.get(4);
        int q2r = values.get(5);
        if (!inRange(ul, ur, parsed.length)
                || !inRange(q1l, q1r, parsed.length)
                || !inRange(q2l, q2r, parsed.length)) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A range is out of bounds.",
                    LINE_FINAL));
            return steps;
        }
        if (index == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The add amount is needed.",
                    LINE_FINAL));
            return steps;
        }
        Tree tree = new Tree(parsed, steps);
        int buildTm = (parsed.length - 1) / 2;
        steps.add(new Step(tree.lazyPairs(), List.of(), List.of(),
                List.of(),
                "Built the segment tree: root sum " + tree.sum[1]
                        + " ([0," + buildTm + "]="
                        + tree.sum[2] + ", [" + (buildTm + 1)
                        + "," + (parsed.length - 1) + "]="
                        + tree.sum[3] + ").",
                LINE_BUILD, tree.snapshot(), tree.edgeSnapshot()));
        tree.update(1, 0, parsed.length - 1, ul, ur, index, true,
                ul, ur);
        List<Integer> contributors1 = new ArrayList<>();
        int answer1 = tree.query(1, 0, parsed.length - 1, q1l, q1r,
                contributors1);
        steps.add(new Step(List.of(answer1),
                new ArrayList<>(contributors1), List.of(), List.of(),
                "Query sum[" + q1l + "," + q1r + "]: node"
                        + (contributors1.size() == 1 ? "" : "s")
                        + " " + contributors1 + " cover it — "
                        + answer1 + ".",
                LINE_QUERY, tree.snapshot(), tree.edgeSnapshot()));
        List<Integer> contributors2 = new ArrayList<>();
        int answer2 = tree.query(1, 0, parsed.length - 1, q2l, q2r,
                contributors2);
        steps.add(new Step(List.of(answer2),
                new ArrayList<>(contributors2), List.of(), List.of(),
                "Query sum[" + q2l + "," + q2r + "]: node"
                        + (contributors2.size() == 1 ? "" : "s")
                        + " " + contributors2 + " cover it — "
                        + answer2 + ".",
                LINE_QUERY, tree.snapshot(), tree.edgeSnapshot()));
        steps.add(new Step(tree.lazyPairs(), List.of(), List.of(),
                List.of(),
                "Lazy segment tree done: root sum " + tree.sum[1]
                        + "; lazy tags left only on nodes "
                        + lazyNodeList(tree) + "; queries [" + q1l
                        + "," + q1r + "]=" + answer1 + ", [" + q2l
                        + "," + q2r + "]=" + answer2 + ".",
                LINE_FINAL, tree.snapshot(), tree.edgeSnapshot()));
        return steps;
    }

    /** Node ids with a nonzero lazy tag, for the final message. */
    private List<Integer> lazyNodeList(Tree tree) {
        List<Integer> ids = new ArrayList<>();
        List<Integer> pairs = tree.lazyPairs();
        for (int i = 0; i < pairs.size(); i += 2) {
            ids.add(pairs.get(i));
        }
        return ids;
    }

    private boolean inRange(int l, int r, int n) {
        return l >= 0 && r < n && l <= r;
    }

    /** Parses "3,1,4,1,5,9,2,6" into an int array, or null. */
    private int[] parseArray(String word) {
        if (word == null || word.isBlank()) {
            return null;
        }
        String[] parts = word.trim().split(",", -1);
        int[] result = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                result[i] = Integer.parseInt(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return result;
    }
}
