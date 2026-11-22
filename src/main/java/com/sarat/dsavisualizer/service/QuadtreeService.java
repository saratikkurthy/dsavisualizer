package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Quadtree (Phase 23, how Maps tiles the world and finds points
 * in a region): the map is a fixed 100&times;100 square, y up =
 * north. A leaf stores up to {@code capacity} points; when a
 * leaf overflows it splits into four quadrants — NW (x &lt; mx,
 * y &gt;= my), NE (x &gt;= mx, y &gt;= my), SW (x &lt; mx,
 * y &lt; my), SE (x &gt;= mx, y &lt; my) — so a point on a
 * boundary goes to the east or the north side. A region query
 * visits every node whose bounds overlap the query rect and
 * prunes the rest.
 *
 * <p>Steps: one per insert (line 2 store / line 3 split), one
 * per query node (line 4 visit / line 5 prune), plus the final
 * Step (line 0). Every step carries the full tree as a flat
 * BFS list of {@code NodeDto}s: {@code id} is the flat index,
 * {@code value} is the depth, {@code x}/{@code y} are the
 * bounds' top-left corner, {@code label} is the bounds string
 * "x0,y0,x1,y1", {@code keys} are the node's points flattened
 * as integers. {@code data} is the point just inserted [x, y]
 * (insert steps), the query rect [x0, y0, x1, y1] (query
 * steps), or the result points flattened (final step);
 * {@code highlight} is the index of the node the step is
 * about. Demo (capacity 2): A(10,10) store, B(20,80) store,
 * C(70,20) splits the root (NW=[B], NE=[], SW=[A], SE=[C]),
 * D(80,80) store, E(55,55) store, F(60,60) splits the NE node
 * (NW=[], NE=[D], SW=[E,F], SE=[]). Query (50,50,100,100)
 * visits 9 nodes and returns [(55,55),(60,60),(80,80)];
 * query (0,0,50,50) returns [(10,10)]. 6 inserts + 9 query
 * node steps + final = 16 steps for the NE query.</p>
 *
 * <p>The controller validates the input (word missing or not a
 * "x,y;x,y;..." point list &rarr; 400, a point outside the
 * 100&times;100 bounds &rarr; 400, capacity outside 1..16
 * &rarr; 400, a query rect (values) that is not 4 numbers
 * &rarr; 400); this method guards against bad input anyway
 * with a single "empty" Step, and defaults a missing capacity
 * to 2 and a missing query rect to the whole bounds.</p>
 */
@Service
public class QuadtreeService {

    private static final int LINE_STORE = 2;
    private static final int LINE_SPLIT = 3;
    private static final int LINE_VISIT = 4;
    private static final int LINE_PRUNE = 5;
    private static final int LINE_FINAL = 0;

    private static final int DEFAULT_CAPACITY = 2;
    private static final double BOUND = 100.0;

    /** One quadtree node: bounds, points, four children (null = leaf). */
    private static class QNode {
        final double x0;
        final double y0;
        final double x1;
        final double y1;
        final int depth;
        final List<int[]> points = new ArrayList<>();
        QNode[] kids;

        QNode(double x0, double y0, double x1, double y1, int depth) {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.depth = depth;
        }

        boolean isLeaf() {
            return kids == null;
        }

        /** Quadrant index for a point: 0 NW, 1 NE, 2 SW, 3 SE. */
        int quad(int[] p) {
            double mx = (x0 + x1) / 2;
            double my = (y0 + y1) / 2;
            if (p[0] < mx && p[1] >= my) {
                return 0;
            }
            if (p[0] >= mx && p[1] >= my) {
                return 1;
            }
            if (p[0] < mx && p[1] < my) {
                return 2;
            }
            return 3;
        }

        /** The four sub-bounds in NW, NE, SW, SE order. */
        double[][] subBounds() {
            double mx = (x0 + x1) / 2;
            double my = (y0 + y1) / 2;
            return new double[][]{
                    {x0, my, mx, y1},
                    {mx, my, x1, y1},
                    {x0, y0, mx, my},
                    {mx, y0, x1, my}};
        }

        String boundsText() {
            return fmt(x0) + "," + fmt(y0) + "," + fmt(x1) + "," + fmt(y1);
        }
    }

    /**
     * Runs the quadtree build + region query. {@code word} is
     * "x,y;x,y;..."; {@code values} is the query rect
     * [x0, y0, x1, y1] (null = the whole bounds); {@code capacity}
     * is the node capacity (null = 2).
     */
    public List<Step> quadtree(String word, List<Integer> values,
                               Integer capacity) {
        List<Step> steps = new ArrayList<>();
        List<int[]> points = parsePoints(word);
        if (points == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Points are needed as "
                            + "\"x,y;x,y;...\" (e.g. "
                            + "\"10,10;20,80;70,20\").", LINE_FINAL));
            return steps;
        }
        for (int[] p : points) {
            if (p[0] < 0 || p[0] > BOUND || p[1] < 0 || p[1] > BOUND) {
                steps.add(new Step(List.of(), List.of(), List.of(),
                        List.of(), "Every point must be inside the "
                                + "100x100 map.", LINE_FINAL));
                return steps;
            }
        }
        int cap = capacity == null ? DEFAULT_CAPACITY : capacity;
        if (cap < 1 || cap > 16) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Capacity must be between 1 and 16.",
                    LINE_FINAL));
            return steps;
        }
        int[] rect = {0, 0, 100, 100};
        if (values != null && !values.isEmpty()) {
            if (values.size() != 4) {
                steps.add(new Step(List.of(), List.of(), List.of(),
                        List.of(), "The query rect must be "
                                + "[x0, y0, x1, y1].", LINE_FINAL));
                return steps;
            }
            rect = new int[]{values.get(0), values.get(1),
                    values.get(2), values.get(3)};
        }

        QNode root = new QNode(0, 0, BOUND, BOUND, 0);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            names.add(String.valueOf((char) ('A' + i)));
        }
        for (int i = 0; i < points.size(); i++) {
            int[] p = points.get(i);
            List<QNode> splitNodes = new ArrayList<>();
            QNode target = insert(root, p, cap, splitNodes);
            List<NodeDto> snapshot = snapshot(root);
            Map<QNode, Integer> index = indexOf(root);
            if (splitNodes.isEmpty()) {
                steps.add(new Step(List.of(p[0], p[1]),
                        List.of(index.get(target)), List.of(), List.of(),
                        "Insert " + names.get(i) + pointText(p)
                                + ": stored in leaf ("
                                + target.boundsText() + ").",
                        LINE_STORE, snapshot, null));
            } else {
                QNode split = splitNodes.get(0);
                steps.add(new Step(List.of(p[0], p[1]),
                        List.of(index.get(split)), List.of(), List.of(),
                        "Insert " + names.get(i) + pointText(p) + ": "
                                + (split == root ? "root" : "node ("
                                + split.boundsText() + ")")
                                + " full — split into "
                                + splitText(split, names, points),
                        LINE_SPLIT, snapshot, null));
            }
        }

        List<int[]> found = new ArrayList<>();
        query(root, root, rect, found, steps, names, points);
        found.sort((a, b) -> a[0] != b[0] ? a[0] - b[0] : a[1] - b[1]);
        List<Integer> resultData = new ArrayList<>();
        for (int[] p : found) {
            resultData.add(p[0]);
            resultData.add(p[1]);
        }
        steps.add(new Step(resultData, List.of(), List.of(), List.of(),
                "Query (" + rect[0] + "," + rect[1] + "," + rect[2]
                        + "," + rect[3] + ") found " + found.size()
                        + (found.size() == 1 ? " point: " : " points: ")
                        + pointsText(found) + ".",
                LINE_FINAL, snapshot(root), null));
        return steps;
    }

    /**
     * Inserts a point, returning the leaf it landed in and
     * recording every node that split along the way (usually
     * none, or one). A split redistributes the node's points
     * into its four children (recursively — a child can split
     * too, and that split is recorded as well).
     */
    private QNode insert(QNode node, int[] p, int capacity,
                         List<QNode> splitNodes) {
        if (node.isLeaf()) {
            node.points.add(p);
            if (node.points.size() > capacity) {
                double[][] sub = node.subBounds();
                node.kids = new QNode[4];
                for (int i = 0; i < 4; i++) {
                    node.kids[i] = new QNode(sub[i][0], sub[i][1],
                            sub[i][2], sub[i][3], node.depth + 1);
                }
                List<int[]> old = new ArrayList<>(node.points);
                node.points.clear();
                splitNodes.add(node);
                for (int[] q : old) {
                    insert(node.kids[node.quad(q)], q, capacity,
                            splitNodes);
                }
            }
            return node;
        }
        return insert(node.kids[node.quad(p)], p, capacity, splitNodes);
    }

    /**
     * The region query: one step per node visited (line 4) or
     * pruned (line 5), in NW, NE, SW, SE order.
     */
    private void query(QNode root, QNode node, int[] rect,
                       List<int[]> found, List<Step> steps,
                       List<String> names, List<int[]> points) {
        List<NodeDto> snapshot = snapshot(root);
        Map<QNode, Integer> index = indexOf(root);
        if (rect[2] <= node.x0 || rect[0] >= node.x1
                || rect[3] <= node.y0 || rect[1] >= node.y1) {
            steps.add(new Step(List.of(rect[0], rect[1], rect[2], rect[3]),
                    List.of(index.get(node)), List.of(), List.of(),
                    "Prune node (" + node.boundsText()
                            + "): misses the query rect.",
                    LINE_PRUNE, snapshot, null));
            return;
        }
        List<int[]> collected = new ArrayList<>();
        for (int[] p : node.points) {
            if (rect[0] <= p[0] && p[0] < rect[2]
                    && rect[1] <= p[1] && p[1] < rect[3]) {
                collected.add(p);
                found.add(p);
            }
        }
        String message = "Visit node (" + node.boundsText()
                + "): overlaps the query rect";
        if (!collected.isEmpty()) {
            message += " — collected " + pointsText(collected);
        }
        message += ".";
        steps.add(new Step(List.of(rect[0], rect[1], rect[2], rect[3]),
                List.of(index.get(node)), List.of(), List.of(),
                message, LINE_VISIT, snapshot, null));
        if (node.kids != null) {
            for (QNode kid : node.kids) {
                query(root, kid, rect, found, steps, names, points);
            }
        }
    }

    /** "NW=[B], NE=[], SW=[A], SE=[C]" for a split node's children. */
    private String splitText(QNode split, List<String> names,
                             List<int[]> points) {
        String[] quadNames = {"NW", "NE", "SW", "SE"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(quadNames[i]).append("=[");
            List<String> kids = new ArrayList<>();
            collectNames(split.kids[i], names, points, kids);
            sb.append(String.join(", ", kids)).append("]");
        }
        return sb.toString();
    }

    /** Point names in a subtree, in NW, NE, SW, SE order. */
    private void collectNames(QNode node, List<String> names,
                              List<int[]> points, List<String> out) {
        for (int[] p : node.points) {
            out.add(nameOf(p, names, points));
        }
        if (node.kids != null) {
            for (QNode kid : node.kids) {
                collectNames(kid, names, points, out);
            }
        }
    }

    /** The name of a point (identity match — points are shared objects). */
    private String nameOf(int[] p, List<String> names,
                          List<int[]> points) {
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i) == p) {
                return names.get(i);
            }
        }
        return "?";
    }

    /** "(10,10)" rendering of a point. */
    private String pointText(int[] p) {
        return "(" + p[0] + "," + p[1] + ")";
    }

    /** "(10,10), (20,80)" rendering of a point list. */
    private String pointsText(List<int[]> pts) {
        List<String> texts = new ArrayList<>();
        for (int[] p : pts) {
            texts.add(pointText(p));
        }
        return String.join(", ", texts);
    }

    /** BFS snapshot of the tree as NodeDto list. */
    private List<NodeDto> snapshot(QNode root) {
        List<NodeDto> dtos = new ArrayList<>();
        List<QNode> bfs = bfs(root);
        for (int i = 0; i < bfs.size(); i++) {
            QNode n = bfs.get(i);
            NodeDto dto = new NodeDto(i, n.depth, n.x0, n.y0);
            dto.setLabel(n.boundsText());
            List<Integer> keys = new ArrayList<>();
            for (int[] p : n.points) {
                keys.add(p[0]);
                keys.add(p[1]);
            }
            dto.setKeys(keys);
            dtos.add(dto);
        }
        return dtos;
    }

    /** BFS order of the tree's nodes. */
    private List<QNode> bfs(QNode root) {
        List<QNode> out = new ArrayList<>();
        Queue<QNode> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            QNode n = queue.remove();
            out.add(n);
            if (n.kids != null) {
                for (QNode kid : n.kids) {
                    queue.add(kid);
                }
            }
        }
        return out;
    }

    /** Node -> its BFS index in the current snapshot. */
    private Map<QNode, Integer> indexOf(QNode root) {
        Map<QNode, Integer> index = new HashMap<>();
        List<QNode> bfs = bfs(root);
        for (int i = 0; i < bfs.size(); i++) {
            index.put(bfs.get(i), i);
        }
        return index;
    }

    /**
     * Parses "x,y;x,y;..." into integer point pairs, or null
     * when the word is missing or unparseable.
     */
    private List<int[]> parsePoints(String word) {
        if (word == null || word.isBlank()) {
            return null;
        }
        List<int[]> points = new ArrayList<>();
        for (String part : word.split(";", -1)) {
            String[] xy = part.trim().split(",", -1);
            if (xy.length != 2) {
                return null;
            }
            try {
                int x = Integer.parseInt(xy[0].trim());
                int y = Integer.parseInt(xy[1].trim());
                points.add(new int[]{x, y});
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return points.isEmpty() ? null : points;
    }

    /**
     * Formats a bound: integers without a decimal point, other
     * values with up to 4 decimals, trailing zeros stripped.
     */
    private static String fmt(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        String text = String.format(java.util.Locale.ROOT, "%.4f", value);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}
