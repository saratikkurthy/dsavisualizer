package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * R-tree with the Guttman linear split (Phase 23, how a real
 * map database such as PostGIS indexes bounding boxes): each
 * node holds up to M entries — rectangles in a leaf, (child,
 * MBR) pairs in an internal node. Insertion descends to the
 * leaf needing the least enlargement (ties: smaller area,
 * then fewer entries), stores the rectangle, refreshes every
 * ancestor's MBR (adjust-tree), and splits an overflowing
 * node: the seeds are the pair with the greatest normalized
 * separation, the rest go to the group needing the least
 * enlargement (ties: smaller area, then fewer entries, then
 * group 1), and a group that would drop below m = max(1,
 * M/2) is force-filled with the rest. A query visits every
 * node whose MBR overlaps the query rect and collects the
 * overlapping entries of every leaf it reaches.
 *
 * <p>Steps: one descend per level (line 2), one insert-store
 * (line 3), one split (line 4, pre-split snapshot) and — when
 * the root split — one new-root step (line 5, post-split
 * snapshot), one query-visit per overlapping node (line 6),
 * one query-match per visited leaf (line 7), plus the final
 * Step (line 0). Every step carries the MBR hierarchy as a
 * flat BFS list of {@code NodeDto}s: {@code id} is the flat
 * index, {@code value} is 1 for a leaf and 0 for an internal
 * node, {@code x}/{@code y} are the MBR's top-left corner,
 * {@code label} is the MBR string "x0,y0,x1,y1", {@code keys}
 * are entry indices — rect indices (0-based into the input
 * rect list) for a leaf, child node indices (into this flat
 * list) for an internal node. {@code data} is the rect just
 * inserted [x1, y1, x2, y2] (insert steps) or the query rect
 * (query steps); the final step's {@code data} is the matching
 * rect indices. {@code highlight} is the index of the node
 * the step is about. Demo (M=4, m=2): R1..R4 fill the root;
 * inserting R5 splits the root — seeds R4 (highest low-x 65)
 * and R1 (lowest high-x 20), normalized separation x 0.69
 * &gt; y 0.67; groups [R4,R3] / [R1,R2,R5]; new root MBR
 * (10,10,75,70). R6 descends into (60,10,75,25) and that
 * leaf's MBR refreshes to (60,10,75,70). Queries:
 * (12,12,30,30) &rarr; [R1,R2] (2 node visits),
 * (55,55,80,80) &rarr; [R6] (2 node visits),
 * (0,0,100,100) &rarr; all 6 (3 node visits). The default
 * query trace is 9 insert-phase steps + 3 query steps +
 * final = 13 steps.</p>
 *
 * <p>The controller validates the input (word missing or not a
 * "x1,y1,x2,y2;..." rect list &rarr; 400, a rect with x1
 * &gt;= x2 or y1 &gt;= y2 &rarr; 400, capacity outside 2..8
 * &rarr; 400, a query rect (values) that is not 4 numbers
 * &rarr; 400); this method guards against bad input anyway
 * with a single "empty" Step, and defaults a missing capacity
 * to 4 and a missing query rect to (12,12,30,30).</p>
 */
@Service
public class RTreeService {

    private static final int LINE_DESCEND = 2;
    private static final int LINE_STORE = 3;
    private static final int LINE_SPLIT = 4;
    private static final int LINE_NEW_ROOT = 5;
    private static final int LINE_QUERY_VISIT = 6;
    private static final int LINE_QUERY_MATCH = 7;
    private static final int LINE_FINAL = 0;

    private static final int DEFAULT_M = 4;
    private static final int[] DEFAULT_QUERY = {12, 12, 30, 30};

    /** One entry: a leaf rect (name + rect) or a child (node + stored MBR). */
    private static class Entry {
        final String name;
        final int[] rect;
        final RNode child;

        Entry(String name, int[] rect) {
            this.name = name;
            this.rect = rect;
            this.child = null;
        }

        Entry(RNode child, int[] mbr) {
            this.name = null;
            this.rect = mbr;
            this.child = child;
        }

        boolean isLeafEntry() {
            return child == null;
        }
    }

    /** One R-tree node: a leaf (rect entries) or internal (child entries). */
    private static class RNode {
        boolean leaf;
        final List<Entry> entries = new ArrayList<>();

        RNode(boolean leaf) {
            this.leaf = leaf;
        }

        /** The MBR covering all entries. */
        int[] mbr() {
            int x0 = Integer.MAX_VALUE;
            int y0 = Integer.MAX_VALUE;
            int x1 = Integer.MIN_VALUE;
            int y1 = Integer.MIN_VALUE;
            for (Entry e : entries) {
                x0 = Math.min(x0, e.rect[0]);
                y0 = Math.min(y0, e.rect[1]);
                x1 = Math.max(x1, e.rect[2]);
                y1 = Math.max(y1, e.rect[3]);
            }
            return new int[]{x0, y0, x1, y1};
        }
    }

    /** The running trace + tree, so inserts can emit steps as they go. */
    private static class Run {
        final int m;
        final int maxEntries;
        RNode root;
        final List<Step> steps = new ArrayList<>();
        final List<int[]> rects;

        Run(int maxEntries, List<int[]> rects) {
            this.maxEntries = maxEntries;
            this.m = Math.max(1, maxEntries / 2);
            this.root = new RNode(true);
            this.rects = rects;
        }
    }

    /**
     * Runs the R-tree build + overlap query. {@code word} is
     * "x1,y1,x2,y2;..."; {@code values} is the query rect
     * [x0, y0, x1, y1] (null = (12,12,30,30)); {@code capacity}
     * is M (null = 4).
     */
    public List<Step> rtree(String word, List<Integer> values,
                            Integer capacity) {
        List<int[]> rects = parseRects(word);
        if (rects == null) {
            return List.of(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Rectangles are needed as "
                            + "\"x1,y1,x2,y2;...\" (e.g. "
                            + "\"10,10,20,20;15,15,25,25\").",
                    LINE_FINAL));
        }
        for (int[] r : rects) {
            if (r[0] >= r[2] || r[1] >= r[3]) {
                return List.of(new Step(List.of(), List.of(), List.of(),
                        List.of(), "Every rectangle needs x1 < x2 and "
                                + "y1 < y2.", LINE_FINAL));
            }
        }
        int maxEntries = capacity == null ? DEFAULT_M : capacity;
        if (maxEntries < 2 || maxEntries > 8) {
            return List.of(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Capacity M must be between 2 and 8.",
                    LINE_FINAL));
        }
        int[] query = DEFAULT_QUERY.clone();
        if (values != null && !values.isEmpty()) {
            if (values.size() != 4) {
                return List.of(new Step(List.of(), List.of(), List.of(),
                        List.of(), "The query rect must be "
                                + "[x0, y0, x1, y1].", LINE_FINAL));
            }
            query = new int[]{values.get(0), values.get(1),
                    values.get(2), values.get(3)};
        }

        Run run = new Run(maxEntries, rects);
        for (int i = 0; i < rects.size(); i++) {
            insert(run, "R" + (i + 1), rects.get(i));
        }
        List<String> matches = new ArrayList<>();
        List<Integer> matchIndices = new ArrayList<>();
        query(run, run.root, query, matches, matchIndices);
        List<String> sortedMatches = new ArrayList<>(matches);
        sortedMatches.sort(String::compareTo);
        List<Integer> sortedIndices = new ArrayList<>();
        for (String name : sortedMatches) {
            sortedIndices.add(Integer.parseInt(name.substring(1)) - 1);
        }
        run.steps.add(new Step(sortedIndices, List.of(), List.of(),
                List.of(),
                "Query (" + query[0] + "," + query[1] + "," + query[2]
                        + "," + query[3] + ") found " + matches.size()
                        + (matches.size() == 1 ? " rectangle: "
                        : " rectangles: ")
                        + String.join(", ", sortedMatches) + ".",
                LINE_FINAL, snapshot(run.root), null));
        return run.steps;
    }

    /** Inserts one rectangle, emitting descend/store/split steps. */
    private void insert(Run run, String name, int[] rect) {
        List<RNode> path = new ArrayList<>();
        RNode node = run.root;
        path.add(node);
        while (!node.leaf) {
            Entry best = chooseChild(node, rect);
            RNode child = best.child;
            List<NodeDto> snapshot = snapshot(run.root);
            Map<RNode, Integer> index = indexOf(run.root);
            run.steps.add(new Step(List.of(rect[0], rect[1], rect[2],
                    rect[3]), List.of(index.get(child)), List.of(),
                    List.of(),
                    "Descend into " + mbrText(best.rect)
                            + " — least enlargement for " + name + ".",
                    LINE_DESCEND, snapshot, null));
            node = child;
            path.add(node);
        }
        node.entries.add(new Entry(name, rect));
        refreshPath(path);
        List<NodeDto> storeSnapshot = snapshot(run.root);
        Map<RNode, Integer> storeIndex = indexOf(run.root);
        run.steps.add(new Step(List.of(rect[0], rect[1], rect[2],
                rect[3]), List.of(storeIndex.get(node)), List.of(),
                List.of(),
                "Insert " + name + rectText(rect) + ": stored in leaf "
                        + mbrText(node.mbr()) + ".",
                LINE_STORE, storeSnapshot, null));
        if (node.entries.size() > run.maxEntries) {
            split(run, node, path, rect);
        }
    }

    /**
     * Splits an overflowing node (linear split), emitting a
     * split step (pre-split snapshot) and, for a root split, a
     * new-root step (post-split snapshot). A non-root split
     * adds the sibling to the parent and propagates upward.
     */
    private void split(Run run, RNode node, List<RNode> path,
                       int[] rect) {
        List<List<Entry>> groups = linearSplit(node.entries, run.m);
        List<String> g1Names = entryNames(groups.get(0));
        List<String> g2Names = entryNames(groups.get(1));
        List<NodeDto> preSnapshot = snapshot(run.root);
        Map<RNode, Integer> preIndex = indexOf(run.root);
        run.steps.add(new Step(List.of(rect[0], rect[1], rect[2],
                rect[3]), List.of(preIndex.get(node)), List.of(),
                List.of(),
                "Split " + (node.leaf ? "leaf" : "node") + " "
                        + mbrText(node.mbr()) + ": ["
                        + String.join(", ", g1Names) + "] / ["
                        + String.join(", ", g2Names) + "].",
                LINE_SPLIT, preSnapshot, null));
        if (node == run.root) {
            RNode n1 = new RNode(node.leaf);
            RNode n2 = new RNode(node.leaf);
            n1.entries.addAll(groups.get(0));
            n2.entries.addAll(groups.get(1));
            node.leaf = false;
            node.entries.clear();
            node.entries.add(new Entry(n1, n1.mbr()));
            node.entries.add(new Entry(n2, n2.mbr()));
            List<NodeDto> postSnapshot = snapshot(run.root);
            Map<RNode, Integer> postIndex = indexOf(run.root);
            run.steps.add(new Step(List.of(rect[0], rect[1], rect[2],
                    rect[3]), List.of(postIndex.get(run.root)),
                    List.of(), List.of(),
                    "New root " + mbrText(run.root.mbr())
                            + " with two children.",
                    LINE_NEW_ROOT, postSnapshot, null));
            return;
        }
        RNode sibling = new RNode(node.leaf);
        sibling.entries.addAll(groups.get(1));
        node.entries.clear();
        node.entries.addAll(groups.get(0));
        RNode parent = path.get(path.size() - 2);
        parent.entries.add(new Entry(sibling, sibling.mbr()));
        // Refresh the parent's entry for the shrunk node and every
        // ancestor's entry above it (adjust-tree).
        refreshPath(path);
        if (parent.entries.size() > run.maxEntries) {
            split(run, parent, path.subList(0, path.size() - 1), rect);
        }
    }

    /** The child needing the least enlargement (ties: area, then entries). */
    private Entry chooseChild(RNode node, int[] rect) {
        Entry best = null;
        long bestEnlarge = Long.MAX_VALUE;
        long bestArea = Long.MAX_VALUE;
        int bestCount = Integer.MAX_VALUE;
        for (Entry e : node.entries) {
            long enlarge = enlarge(e.rect, rect);
            long area = area(e.rect);
            int count = e.child.entries.size();
            if (enlarge < bestEnlarge
                    || (enlarge == bestEnlarge && area < bestArea)
                    || (enlarge == bestEnlarge && area == bestArea
                    && count < bestCount)) {
                best = e;
                bestEnlarge = enlarge;
                bestArea = area;
                bestCount = count;
            }
        }
        return best;
    }

    /**
     * Guttman linear split: seeds are the pair with the
     * greatest normalized separation; the rest go to the group
     * needing the least enlargement (ties: smaller area, then
     * fewer entries, then group 1); a group that would drop
     * below m is force-filled with the rest.
     */
    private List<List<Entry>> linearSplit(List<Entry> entries, int m) {
        Entry[] seeds = linearSeeds(entries);
        List<Entry> g1 = new ArrayList<>(List.of(seeds[0]));
        List<Entry> g2 = new ArrayList<>(List.of(seeds[1]));
        List<Entry> rest = new ArrayList<>();
        for (Entry e : entries) {
            if (e != seeds[0] && e != seeds[1]) {
                rest.add(e);
            }
        }
        for (int i = 0; i < rest.size(); i++) {
            Entry e = rest.get(i);
            int remainingAfter = rest.size() - i - 1;
            if (g1.size() + remainingAfter < m && g1.size() < m) {
                g1.add(e);
                continue;
            }
            if (g2.size() + remainingAfter < m && g2.size() < m) {
                g2.add(e);
                continue;
            }
            long e1 = enlarge(mbrOf(g1), e.rect);
            long e2 = enlarge(mbrOf(g2), e.rect);
            if (e1 < e2) {
                g1.add(e);
            } else if (e2 < e1) {
                g2.add(e);
            } else if (area(mbrOf(g1)) < area(mbrOf(g2))) {
                g1.add(e);
            } else if (area(mbrOf(g2)) < area(mbrOf(g1))) {
                g2.add(e);
            } else if (g1.size() <= g2.size()) {
                g1.add(e);
            } else {
                g2.add(e);
            }
        }
        return List.of(g1, g2);
    }

    /** The seed pair: greatest normalized separation per dimension. */
    private Entry[] linearSeeds(List<Entry> entries) {
        Entry bestLo = null;
        Entry bestHi = null;
        double bestSep = -1;
        for (int dim = 0; dim < 2; dim++) {
            int maxLow = Integer.MIN_VALUE;
            int minHigh = Integer.MAX_VALUE;
            int minLow = Integer.MAX_VALUE;
            int maxHigh = Integer.MIN_VALUE;
            for (Entry e : entries) {
                maxLow = Math.max(maxLow, e.rect[dim]);
                minHigh = Math.min(minHigh, e.rect[dim + 2]);
                minLow = Math.min(minLow, e.rect[dim]);
                maxHigh = Math.max(maxHigh, e.rect[dim + 2]);
            }
            int width = maxHigh - minLow;
            double sep = width == 0 ? 0
                    : (double) (maxLow - minHigh) / width;
            if (sep > bestSep) {
                bestSep = sep;
                Entry lo = null;
                Entry hi = null;
                for (Entry e : entries) {
                    if (lo == null || e.rect[dim] > lo.rect[dim]) {
                        lo = e;
                    }
                    if (hi == null || e.rect[dim + 2] < hi.rect[dim + 2]) {
                        hi = e;
                    }
                }
                bestLo = lo;
                bestHi = hi;
            }
        }
        return new Entry[]{bestLo, bestHi};
    }

    /** Refreshes every ancestor's stored MBR for its child (adjust-tree). */
    private void refreshPath(List<RNode> path) {
        for (int i = 0; i + 1 < path.size(); i++) {
            RNode parent = path.get(i);
            RNode child = path.get(i + 1);
            for (int j = 0; j < parent.entries.size(); j++) {
                if (parent.entries.get(j).child == child) {
                    parent.entries.set(j,
                            new Entry(child, child.mbr()));
                }
            }
        }
    }

    /** The overlap query: visit steps + one match step per leaf. */
    private void query(Run run, RNode node, int[] rect,
                       List<String> matches, List<Integer> matchIndices) {
        List<NodeDto> snapshot = snapshot(run.root);
        Map<RNode, Integer> index = indexOf(run.root);
        run.steps.add(new Step(List.of(rect[0], rect[1], rect[2],
                rect[3]), List.of(index.get(node)), List.of(), List.of(),
                "Visit node " + mbrText(node.mbr())
                        + ": overlaps the query rect.",
                LINE_QUERY_VISIT, snapshot, null));
        if (node.leaf) {
            List<String> found = new ArrayList<>();
            for (Entry e : node.entries) {
                if (overlaps(e.rect, rect)) {
                    found.add(e.name);
                    matches.add(e.name);
                    matchIndices.add(Integer.parseInt(
                            e.name.substring(1)) - 1);
                }
            }
            run.steps.add(new Step(List.of(rect[0], rect[1], rect[2],
                    rect[3]), List.of(index.get(node)), List.of(),
                    List.of(),
                    found.isEmpty()
                            ? "Leaf " + mbrText(node.mbr())
                            + ": no overlapping rectangles."
                            : "Leaf " + mbrText(node.mbr())
                            + ": matched "
                            + String.join(", ", found) + ".",
                    LINE_QUERY_MATCH, snapshot, null));
            return;
        }
        for (Entry e : node.entries) {
            if (overlaps(e.rect, rect)) {
                query(run, e.child, rect, matches, matchIndices);
            }
        }
    }

    /** Names of the entries in a group (rect names, or child MBRs). */
    private List<String> entryNames(List<Entry> group) {
        List<String> names = new ArrayList<>();
        for (Entry e : group) {
            names.add(e.isLeafEntry() ? e.name : mbrText(e.rect));
        }
        return names;
    }

    /** The MBR covering a group's rects. */
    private int[] mbrOf(List<Entry> group) {
        List<int[]> rects = new ArrayList<>();
        for (Entry e : group) {
            rects.add(e.rect);
        }
        int x0 = Integer.MAX_VALUE;
        int y0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int y1 = Integer.MIN_VALUE;
        for (int[] r : rects) {
            x0 = Math.min(x0, r[0]);
            y0 = Math.min(y0, r[1]);
            x1 = Math.max(x1, r[2]);
            y1 = Math.max(y1, r[3]);
        }
        return new int[]{x0, y0, x1, y1};
    }

    private long area(int[] r) {
        return (long) (r[2] - r[0]) * (r[3] - r[1]);
    }

    private long enlarge(int[] mbr, int[] r) {
        int[] grown = {
                Math.min(mbr[0], r[0]), Math.min(mbr[1], r[1]),
                Math.max(mbr[2], r[2]), Math.max(mbr[3], r[3])};
        return area(grown) - area(mbr);
    }

    private boolean overlaps(int[] a, int[] b) {
        return !(a[2] <= b[0] || b[2] <= a[0]
                || a[3] <= b[1] || b[3] <= a[1]);
    }

    /** "(10,10,20,20)" rendering of a rect. */
    private String rectText(int[] r) {
        return "(" + r[0] + "," + r[1] + "," + r[2] + "," + r[3] + ")";
    }

    /** "(10,10,20,20)" rendering of an MBR. */
    private String mbrText(int[] r) {
        return rectText(r);
    }

    /** BFS snapshot of the tree as NodeDto list. */
    private List<NodeDto> snapshot(RNode root) {
        List<NodeDto> dtos = new ArrayList<>();
        List<RNode> bfs = bfs(root);
        for (int i = 0; i < bfs.size(); i++) {
            RNode n = bfs.get(i);
            int[] mbr = n.mbr();
            NodeDto dto = new NodeDto(i, n.leaf ? 1 : 0, mbr[0], mbr[1]);
            dto.setLabel(mbr[0] + "," + mbr[1] + "," + mbr[2] + ","
                    + mbr[3]);
            List<Integer> keys = new ArrayList<>();
            if (n.leaf) {
                for (Entry e : n.entries) {
                    keys.add(Integer.parseInt(e.name.substring(1)) - 1);
                }
            } else {
                Map<RNode, Integer> index = indexOf(root);
                for (Entry e : n.entries) {
                    keys.add(index.get(e.child));
                }
            }
            dto.setKeys(keys);
            dtos.add(dto);
        }
        return dtos;
    }

    /** BFS order of the tree's nodes. */
    private List<RNode> bfs(RNode root) {
        List<RNode> out = new ArrayList<>();
        out.add(root);
        for (int i = 0; i < out.size(); i++) {
            RNode n = out.get(i);
            if (!n.leaf) {
                for (Entry e : n.entries) {
                    out.add(e.child);
                }
            }
        }
        return out;
    }

    /** Node -> its BFS index in the snapshot. */
    private Map<RNode, Integer> indexOf(RNode root) {
        Map<RNode, Integer> index = new HashMap<>();
        List<RNode> bfs = bfs(root);
        for (int i = 0; i < bfs.size(); i++) {
            index.put(bfs.get(i), i);
        }
        return index;
    }

    /**
     * Parses "x1,y1,x2,y2;..." into integer rects, or null when
     * the word is missing or unparseable.
     */
    private List<int[]> parseRects(String word) {
        if (word == null || word.isBlank()) {
            return null;
        }
        List<int[]> rects = new ArrayList<>();
        for (String part : word.split(";", -1)) {
            String[] xy = part.trim().split(",", -1);
            if (xy.length != 4) {
                return null;
            }
            try {
                int[] r = new int[4];
                for (int i = 0; i < 4; i++) {
                    r[i] = Integer.parseInt(xy[i].trim());
                }
                rects.add(r);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return rects.isEmpty() ? null : rects;
    }
}
