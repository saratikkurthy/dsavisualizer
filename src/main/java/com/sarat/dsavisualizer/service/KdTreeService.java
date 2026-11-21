package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * K-d tree nearest neighbour (Phase 23, how Maps finds the
 * single nearest place): the tree is built once by the lower
 * median, the split axis alternating x (depth 0), y (depth 1),
 * x, ... Sorting ties break by the other coordinate. The
 * search walks down to the nearest side, then unwinds: a far
 * side is searched only when the query's distance to the
 * splitting plane is smaller than the best distance so far,
 * otherwise the whole far subtree is pruned.
 *
 * <p>Steps: one per visit (line 2), one per prune (line 3),
 * plus the final Step (line 0). The trace is the search only
 * (the built tree is the visualizer's initial render). Every
 * step carries the full tree as a flat BFS list of
 * {@code NodeDto}s: {@code id} is the flat index,
 * {@code value} is the split axis (0 = x, 1 = y), {@code x} /
 * {@code y} are the point, {@code keys} are the node's region
 * [rx0, ry0, rx1, ry1] (the root region is the bounding box of
 * the points; a child region is the parent region cut at the
 * parent's point along the parent's axis). {@code data} is the
 * flat list [bestX, bestY, distMillis] where distMillis =
 * round(dist &times; 1000); {@code highlight} is the index of
 * the node visited or pruned. Demo (points (2,3), (5,4),
 * (9,6), (4,7), (8,1), (7,2)): root (5,4) axis x; left (2,3)
 * axis y with right child (4,7) axis x; right (7,2) axis y
 * with left (8,1) axis x and right (9,6) axis x. Query (9,2)
 * &rarr; nearest (8,1), dist 1.414: visit (5,4) 4.472 &rarr;
 * visit (7,2) 2.0 &rarr; visit (9,6) 4.0 &rarr; visit (8,1)
 * 1.414 (new best) &rarr; prune at (5,4) (|9-5| = 4 &gt; best
 * 1.414, so the whole left subtree is never visited). 4 visits
 * + 1 prune + final = 6 steps.</p>
 *
 * <p>The controller validates the input (word missing or not a
 * "x,y;x,y;..." point list &rarr; 400, a query point (values)
 * that is not 2 numbers &rarr; 400); this method guards
 * against bad input anyway with a single "empty" Step, and
 * defaults a missing query point to (9,2).</p>
 */
@Service
public class KdTreeService {

    private static final int LINE_VISIT = 2;
    private static final int LINE_PRUNE = 3;
    private static final int LINE_FINAL = 0;

    /** One k-d tree node: point, split axis, children, region. */
    private static class KDNode {
        final int x;
        final int y;
        final int axis;
        KDNode left;
        KDNode right;
        int rx0;
        int ry0;
        int rx1;
        int ry1;

        KDNode(int x, int y, int axis) {
            this.x = x;
            this.y = y;
            this.axis = axis;
        }
    }

    /** Mutable search state: the best point and distance so far. */
    private static class Search {
        KDNode best;
        double bestDist = Double.POSITIVE_INFINITY;
    }

    /**
     * Runs the nearest-neighbour search. {@code word} is
     * "x,y;x,y;..."; {@code values} is the query point [x, y]
     * (null = (9,2)).
     */
    public List<Step> kdtree(String word, List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        List<int[]> points = parsePoints(word);
        if (points == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Points are needed as "
                            + "\"x,y;x,y;...\" (e.g. "
                            + "\"2,3;5,4;9,6\").", LINE_FINAL));
            return steps;
        }
        int[] query = {9, 2};
        if (values != null && !values.isEmpty()) {
            if (values.size() != 2) {
                steps.add(new Step(List.of(), List.of(), List.of(),
                        List.of(), "The query point must be [x, y].",
                        LINE_FINAL));
                return steps;
            }
            query = new int[]{values.get(0), values.get(1)};
        }

        KDNode root = build(new ArrayList<>(points), 0);
        assignRegions(root, boundingBox(points));
        List<NodeDto> snapshot = snapshot(root);
        Map<KDNode, Integer> index = indexOf(root);
        Search search = new Search();
        search(root, query, steps, snapshot, index, search);
        steps.add(new Step(data(search), List.of(), List.of(), List.of(),
                "Nearest to (" + query[0] + "," + query[1] + ") is ("
                        + search.best.x + "," + search.best.y
                        + "), distance " + fmtDist(search.bestDist) + ".",
                LINE_FINAL, snapshot, null));
        return steps;
    }

    /**
     * The search: visit the node, then the near side, then the
     * far side only when the splitting plane is closer than
     * the best distance (otherwise one prune step).
     */
    private void search(KDNode node, int[] query, List<Step> steps,
                        List<NodeDto> snapshot,
                        Map<KDNode, Integer> index, Search state) {
        if (node == null) {
            return;
        }
        double d = dist(query, node);
        boolean improved = d < state.bestDist;
        if (improved) {
            state.best = node;
            state.bestDist = d;
        }
        steps.add(new Step(data(state), List.of(index.get(node)),
                List.of(), List.of(),
                "Visit (" + node.x + "," + node.y + "): distance "
                        + fmtDist(d)
                        + (improved ? " — new best." : "."),
                LINE_VISIT, snapshot, null));
        KDNode near;
        KDNode far;
        int planeCoord = node.axis == 0 ? node.x : node.y;
        if (query[node.axis] < planeCoord) {
            near = node.left;
            far = node.right;
        } else {
            near = node.right;
            far = node.left;
        }
        search(near, query, steps, snapshot, index, state);
        double plane = Math.abs(query[node.axis] - planeCoord);
        if (plane < state.bestDist) {
            search(far, query, steps, snapshot, index, state);
        } else {
            steps.add(new Step(data(state), List.of(index.get(node)),
                    List.of(), List.of(),
                    "Prune at (" + node.x + "," + node.y + "): |"
                            + query[node.axis] + "-" + planeCoord
                            + "| = " + fmtDist(plane)
                            + " >= best " + fmtDist(state.bestDist)
                            + " — the far side is skipped.",
                    LINE_PRUNE, snapshot, null));
        }
    }

    /** [bestX, bestY, distMillis] for a search state. */
    private List<Integer> data(Search state) {
        if (state.best == null) {
            return List.of();
        }
        return List.of(state.best.x, state.best.y,
                (int) Math.round(state.bestDist * 1000));
    }

    /**
     * Builds the tree by the lower median, axis alternating x,
     * y, x, ... Sorting ties break by the other coordinate.
     */
    private KDNode build(List<int[]> points, int depth) {
        if (points.isEmpty()) {
            return null;
        }
        int axis = depth % 2;
        points.sort((a, b) -> a[axis] != b[axis]
                ? Integer.compare(a[axis], b[axis])
                : Integer.compare(a[1 - axis], b[1 - axis]));
        int mid = (points.size() - 1) / 2;
        int[] median = points.get(mid);
        KDNode node = new KDNode(median[0], median[1], axis);
        node.left = build(new ArrayList<>(points.subList(0, mid)),
                depth + 1);
        node.right = build(
                new ArrayList<>(points.subList(mid + 1, points.size())),
                depth + 1);
        return node;
    }

    /** Assigns each node's region (root = the points' bounding box). */
    private void assignRegions(KDNode node, int[] region) {
        if (node == null) {
            return;
        }
        node.rx0 = region[0];
        node.ry0 = region[1];
        node.rx1 = region[2];
        node.ry1 = region[3];
        int[] leftRegion = region.clone();
        int[] rightRegion = region.clone();
        if (node.axis == 0) {
            leftRegion[2] = node.x;
            rightRegion[0] = node.x;
        } else {
            leftRegion[3] = node.y;
            rightRegion[1] = node.y;
        }
        assignRegions(node.left, leftRegion);
        assignRegions(node.right, rightRegion);
    }

    /** [minX, minY, maxX, maxY] over the points. */
    private int[] boundingBox(List<int[]> points) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int[] p : points) {
            minX = Math.min(minX, p[0]);
            minY = Math.min(minY, p[1]);
            maxX = Math.max(maxX, p[0]);
            maxY = Math.max(maxY, p[1]);
        }
        return new int[]{minX, minY, maxX, maxY};
    }

    /** Euclidean distance from the query point to a node. */
    private double dist(int[] query, KDNode node) {
        return Math.hypot(query[0] - node.x, query[1] - node.y);
    }

    /** BFS snapshot of the tree as NodeDto list. */
    private List<NodeDto> snapshot(KDNode root) {
        List<NodeDto> dtos = new ArrayList<>();
        List<KDNode> bfs = bfs(root);
        for (int i = 0; i < bfs.size(); i++) {
            KDNode n = bfs.get(i);
            NodeDto dto = new NodeDto(i, n.axis, n.x, n.y);
            dto.setKeys(List.of(n.rx0, n.ry0, n.rx1, n.ry1));
            dtos.add(dto);
        }
        return dtos;
    }

    /** BFS order of the tree's nodes. */
    private List<KDNode> bfs(KDNode root) {
        List<KDNode> out = new ArrayList<>();
        out.add(root);
        for (int i = 0; i < out.size(); i++) {
            KDNode n = out.get(i);
            if (n.left != null) {
                out.add(n.left);
            }
            if (n.right != null) {
                out.add(n.right);
            }
        }
        return out;
    }

    /** Node -> its BFS index in the snapshot. */
    private Map<KDNode, Integer> indexOf(KDNode root) {
        Map<KDNode, Integer> index = new HashMap<>();
        List<KDNode> bfs = bfs(root);
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

    /** A distance with up to 3 decimals, trailing zeros stripped. */
    private String fmtDist(double value) {
        String text = String.format(java.util.Locale.ROOT, "%.3f", value);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}
