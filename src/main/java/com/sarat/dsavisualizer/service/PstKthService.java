package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Persistent segment tree (Chairman tree) k-th smallest
 * (Phase 27, range queries v3): one segment tree version
 * per array prefix — version i counts the values of
 * a[0..i-1] over the compressed value range, and each
 * version shares all but O(log m) nodes with the previous
 * one. The k-th smallest in a[l..r] is a walk down the
 * two versions (roots[r+1] minus roots[l]): at each
 * level the left child's count difference says whether
 * the answer is left (k unchanged) or right (k minus
 * that difference). The idea behind persistent data
 * structures and order statistics on ranges.
 *
 * <p>Demo array [3, 1, 4, 1, 5, 9, 2, 6]. Compressed
 * values [1, 2, 3, 4, 5, 6, 9] (indices 0..6). 32
 * persistent nodes (id 0 is the null node); version roots
 * [0, 1, 5, 9, 13, 17, 21, 24, 28]. Query 1 ([1,5],
 * k=3): the values are [1, 4, 1, 5, 9], sorted [1, 1,
 * 4, 5, 9] — the 3rd smallest is 4. Walk:
 * (0,6,left=3) -&gt; (0,3,left=2) -&gt; (2,3,left=0),
 * then right to [3,3] = 4. Query 2 ([0,7], k=6): the
 * whole array, sorted [1, 1, 2, 3, 4, 5, 6, 9] — the
 * 6th smallest is 5. Walk: (0,6,left=5) -&gt;
 * (4,6,left=2) -&gt; (4,5,left=1), then left to [4,4] =
 * 5. Trace: 1 build step (line 1) + 3 query steps per
 * query (line 2, one per non-leaf walk level) + final
 * (line 0) = 8 steps. Build step: {@code nodes} = the
 * version-8 tree (13 nodes: the full tree over the 7
 * compressed values, heap layout over value range
 * [0,6]), {@code data} = the version roots [0, 1, 5, 9,
 * 13, 17, 21, 24, 28], {@code sorted} = the same roots.
 * Query step: {@code nodes} = the walk path so far in
 * version r (accumulating: root, then root+child, then
 * +grandchild), {@code edges} = parent-to-child along
 * the path, {@code data} = [k remaining, leftCount],
 * {@code highlight} = [current node id]. The LEFT
 * version (l-1) is NOT drawn; its role is carried by
 * leftCount in {@code data}. Final step {@code data} =
 * [4, 5] (the two answers), {@code nodes} = the
 * version-8 tree again. Node {@code value} = count of
 * elements in that value-range, {@code label} = the
 * compressed-value range as "lo-hi" using the ACTUAL
 * values (e.g. "[1,2]", "[9,9]" for a leaf — never the
 * compressed indices). Node ids are heap ids over the
 * value range (1:[0,6] 2:[0,3] 3:[4,6] 4:[0,1] 5:[2,3]
 * 6:[4,5] 7:[6,6] 8:[0,0] 9:[1,1] 10:[2,2] 11:[3,3]
 * 12:[4,4] 13:[5,5]). Query-step node values are the
 * counts in version r (roots[r+1]); the range counts
 * are leftCount in {@code data}.</p>
 *
 * <p>Bad input (an unparseable array, or a query that is
 * not [l, r, k] with 0 &le; l &le; r &lt; n and 1 &le; k
 * &le; r-l+1) yields a single "empty" Step; the
 * controller 400s on it (see VisualizationController).</p>
 */
@Service
public class PstKthService {

    private static final int LINE_BUILD = 1;
    private static final int LINE_QUERY = 2;
    private static final int LINE_FINAL = 0;

    /** One persistent node: [left child id, right child id, count]. */
    private static final class PNode {
        int left;
        int right;
        int count;

        PNode(int left, int right, int count) {
            this.left = left;
            this.right = right;
            this.count = count;
        }
    }

    /** Per-call mutable state (the service bean is a singleton, so no instance state). */
    private static final class Tree {
        final int[] values;
        final int m;
        final List<PNode> nodes = new ArrayList<>();
        final List<Integer> roots = new ArrayList<>();

        Tree(int[] array) {
            int[] sorted = array.clone();
            Arrays.sort(sorted);
            List<Integer> distinct = new ArrayList<>();
            for (int v : sorted) {
                if (distinct.isEmpty()
                        || distinct.get(distinct.size() - 1) != v) {
                    distinct.add(v);
                }
            }
            this.values = new int[distinct.size()];
            for (int i = 0; i < distinct.size(); i++) {
                this.values[i] = distinct.get(i);
            }
            this.m = values.length;
            nodes.add(new PNode(0, 0, 0));
            roots.add(0);
            for (int v : array) {
                roots.add(update(roots.get(roots.size() - 1), 0, m - 1,
                        indexOf(v)));
            }
        }

        int indexOf(int value) {
            int lo = 0;
            int hi = m - 1;
            while (lo <= hi) {
                int mid = (lo + hi) / 2;
                if (values[mid] == value) {
                    return mid;
                } else if (values[mid] < value) {
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            return -1;
        }

        /** Persistent point update: returns the new root id. */
        int update(int prev, int tl, int tr, int pos) {
            int cur = nodes.size();
            PNode prevNode = nodes.get(prev);
            nodes.add(new PNode(prevNode.left, prevNode.right,
                    prevNode.count + 1));
            if (tl != tr) {
                int tm = (tl + tr) / 2;
                if (pos <= tm) {
                    nodes.get(cur).left = update(prevNode.left, tl, tm,
                            pos);
                } else {
                    nodes.get(cur).right = update(prevNode.right,
                            tm + 1, tr, pos);
                }
            }
            return cur;
        }

        /** Count of the persistent node id (0 for the null node). */
        int count(int nodeId) {
            return nodes.get(nodeId).count;
        }

        /**
         * The persistent node id covering compressed range
         * [tl, tr] in the tree rooted at {@code root}.
         */
        int nodeForRange(int root, int tl, int tr, int lo, int hi) {
            if (lo == tl && hi == tr) {
                return root;
            }
            int tm = (lo + hi) / 2;
            if (tr <= tm) {
                return nodeForRange(nodes.get(root).left, tl, tr, lo, tm);
            }
            return nodeForRange(nodes.get(root).right, tl, tr, tm + 1, hi);
        }

        /** Count of values in [tl, tr] in the prefix version {@code root}. */
        int rangeCount(int root, int tl, int tr) {
            return count(nodeForRange(root, tl, tr, 0, m - 1));
        }

        /** Label "lo-hi" with the ACTUAL values (never compressed indices). */
        String rangeLabel(int tl, int tr) {
            return "[" + values[tl] + "," + values[tr] + "]";
        }

        /**
         * The version-8 (full-array) tree as NodeDtos, heap
         * layout over the value range [0, m-1], ids 1..2m-1.
         */
        List<NodeDto> versionTreeSnapshot(int versionRoot) {
            NodeDto[] byId = new NodeDto[2 * m];
            versionTreeInto(1, 0, m - 1, versionRoot, byId);
            List<NodeDto> dtos = new ArrayList<>();
            for (int heapId = 1; heapId <= 2 * m - 1; heapId++) {
                dtos.add(byId[heapId]);
            }
            return dtos;
        }

        private void versionTreeInto(int heapId, int tl, int tr,
                                     int versionRoot,
                                     NodeDto[] byId) {
            NodeDto dto = new NodeDto(heapId,
                    rangeCount(versionRoot, tl, tr), 0, 0);
            dto.setLabel(rangeLabel(tl, tr));
            byId[heapId] = dto;
            if (tl != tr) {
                int tm = (tl + tr) / 2;
                versionTreeInto(heapId * 2, tl, tm, versionRoot, byId);
                versionTreeInto(heapId * 2 + 1, tm + 1, tr, versionRoot,
                        byId);
            }
        }

        /** Heap parent-to-child edges for the full value-range tree. */
        List<EdgeDto> versionTreeEdges() {
            List<EdgeDto> edges = new ArrayList<>();
            versionTreeEdgesInto(1, 0, m - 1, edges);
            edges.sort((a, b) -> a.getFrom() != b.getFrom()
                    ? Integer.compare(a.getFrom(), b.getFrom())
                    : Integer.compare(a.getTo(), b.getTo()));
            return edges;
        }

        private void versionTreeEdgesInto(int heapId, int tl, int tr,
                                          List<EdgeDto> edges) {
            if (tl != tr) {
                int tm = (tl + tr) / 2;
                edges.add(new EdgeDto(heapId, heapId * 2, null));
                edges.add(new EdgeDto(heapId, heapId * 2 + 1, null));
                versionTreeEdgesInto(heapId * 2, tl, tm, edges);
                versionTreeEdgesInto(heapId * 2 + 1, tm + 1, tr, edges);
            }
        }

        /**
         * The k-th smallest walk for range [l, r]: returns
         * the compressed index of the answer, appending one
         * [tl, tr, leftCount] entry per non-leaf level to
         * {@code path} and one [heapId, tl, tr, k, leftCount]
         * entry per level to {@code stepRows}.
         */
        int kth(int l, int r, int k, List<int[]> stepRows) {
            int u = roots.get(l);
            int v = roots.get(r + 1);
            return kth(u, v, 0, m - 1, k, 1, stepRows);
        }

        private int kth(int u, int v, int tl, int tr, int k,
                        int heapId, List<int[]> stepRows) {
            if (tl == tr) {
                return tl;
            }
            int tm = (tl + tr) / 2;
            int leftCount = count(nodes.get(v).left)
                    - count(nodes.get(u).left);
            stepRows.add(new int[]{heapId, tl, tr, k, leftCount});
            if (k <= leftCount) {
                return kth(nodes.get(u).left, nodes.get(v).left,
                        tl, tm, k, heapId * 2, stepRows);
            }
            return kth(nodes.get(u).right, nodes.get(v).right,
                    tm + 1, tr, k - leftCount, heapId * 2 + 1,
                    stepRows);
        }
    }

    /**
     * Answers k-th smallest for [values[0], values[1]],
     * k=values[2] and [values[3], values[4]],
     * k=values[5] over the array in {@code word}.
     */
    public List<Step> pstKth(String word, List<Integer> values) {
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
                    List.of(), "Two queries are needed as "
                            + "[l1, r1, k1, l2, r2, k2].",
                    LINE_FINAL));
            return steps;
        }
        int l1 = values.get(0);
        int r1 = values.get(1);
        int k1 = values.get(2);
        int l2 = values.get(3);
        int r2 = values.get(4);
        int k2 = values.get(5);
        if (!validQuery(l1, r1, k1, parsed.length)
                || !validQuery(l2, r2, k2, parsed.length)) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A query is out of bounds.",
                    LINE_FINAL));
            return steps;
        }
        Tree tree = new Tree(parsed);
        int lastRoot = tree.roots.get(tree.roots.size() - 1);
        steps.add(new Step(new ArrayList<>(tree.roots), List.of(),
                List.of(), new ArrayList<>(tree.roots),
                "Built " + tree.roots.size() + " versions over "
                        + tree.m + " compressed values "
                        + Arrays.toString(tree.values) + ": "
                        + tree.nodes.size() + " persistent nodes; "
                        + "version roots " + tree.roots + ".",
                LINE_BUILD, tree.versionTreeSnapshot(lastRoot),
                tree.versionTreeEdges()));
        List<Integer> answers = new ArrayList<>();
        int[][] queries = {{l1, r1, k1}, {l2, r2, k2}};
        for (int qi = 0; qi < queries.length; qi++) {
            int l = queries[qi][0];
            int r = queries[qi][1];
            int k = queries[qi][2];
            List<int[]> rows = new ArrayList<>();
            int answerIdx = tree.kth(l, r, k, rows);
            int answer = tree.values[answerIdx];
            answers.add(answer);
            if (rows.isEmpty()) {
                // Degenerate case: a single distinct value — the
                // root is already the leaf, so there is no walk.
                List<NodeDto> single = new ArrayList<>();
                NodeDto dto = new NodeDto(1,
                        tree.rangeCount(tree.roots.get(r + 1), 0,
                                tree.m - 1), 0, 0);
                dto.setLabel(tree.rangeLabel(0, tree.m - 1));
                single.add(dto);
                steps.add(new Step(List.of(k, 0), List.of(1),
                        List.of(), List.of(),
                        "Query " + (qi + 1) + " [" + l + "," + r
                                + "] k=" + k + ": only one distinct "
                                + "value — answer " + answer + ".",
                        LINE_QUERY, single, new ArrayList<>()));
                continue;
            }
            List<NodeDto> pathNodes = new ArrayList<>();
            List<EdgeDto> pathEdges = new ArrayList<>();
            int versionRoot = tree.roots.get(r + 1);
            for (int si = 0; si < rows.size(); si++) {
                int[] row = rows.get(si);
                int heapId = row[0];
                int tl = row[1];
                int tr = row[2];
                int kHere = row[3];
                int leftCount = row[4];
                NodeDto dto = new NodeDto(heapId,
                        tree.rangeCount(versionRoot, tl, tr), 0, 0);
                dto.setLabel(tree.rangeLabel(tl, tr));
                pathNodes.add(dto);
                if (si > 0) {
                    pathEdges.add(new EdgeDto(
                            rows.get(si - 1)[0], heapId, null));
                }
                String dir;
                int nextK = kHere;
                if (kHere <= leftCount) {
                    dir = "go left";
                } else {
                    dir = "go right";
                    nextK = kHere - leftCount;
                }
                String msg;
                if (si == rows.size() - 1) {
                    int tm = (tl + tr) / 2;
                    int leafTl;
                    int leafTr;
                    if (kHere <= leftCount) {
                        leafTl = tl;
                        leafTr = tm;
                    } else {
                        leafTl = tm + 1;
                        leafTr = tr;
                    }
                    msg = "Query " + (qi + 1) + " [" + l + "," + r
                            + "] k=" + k + ": at "
                            + tree.rangeLabel(tl, tr) + " (k=" + kHere
                            + ", leftCount=" + leftCount + ") — "
                            + dir + " to "
                            + tree.rangeLabel(leafTl, leafTr)
                            + " = " + answer + ". Query " + (qi + 1)
                            + " answer: " + answer + ".";
                } else {
                    msg = "Query " + (qi + 1) + " [" + l + "," + r
                            + "] k=" + k + ": at "
                            + tree.rangeLabel(tl, tr) + " (k=" + kHere
                            + ", leftCount=" + leftCount + ") — "
                            + dir + " (k becomes " + nextK + ").";
                }
                steps.add(new Step(List.of(kHere, leftCount),
                        List.of(heapId), List.of(), List.of(),
                        msg, LINE_QUERY, new ArrayList<>(pathNodes),
                        new ArrayList<>(pathEdges)));
            }
        }
        steps.add(new Step(new ArrayList<>(answers), List.of(),
                List.of(), List.of(),
                "Persistent segment tree done: query answers "
                        + answers + " — the " + k1
                        + ordinalSuffix(k1) + " smallest in [" + l1
                        + "," + r1 + "] is " + answers.get(0)
                        + ", the " + k2 + ordinalSuffix(k2)
                        + " smallest in [" + l2 + "," + r2 + "] is "
                        + answers.get(1) + ".",
                LINE_FINAL, tree.versionTreeSnapshot(lastRoot),
                tree.versionTreeEdges()));
        return steps;
    }

    private String ordinalSuffix(int k) {
        if (k % 100 >= 11 && k % 100 <= 13) {
            return "th";
        }
        switch (k % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }

    private boolean validQuery(int l, int r, int k, int n) {
        return l >= 0 && r < n && l <= r && k >= 1 && k <= r - l + 1;
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
