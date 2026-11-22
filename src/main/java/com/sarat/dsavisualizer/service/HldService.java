package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Heavy-light decomposition (Phase 25, advanced trees):
 * the tree trick that turns a path query into a handful of
 * contiguous array segments — the idea behind fast queries
 * on trees (and behind link-cut trees in interview follow-ups).
 *
 * <p>Two passes. Pass 1 (line 2) sizes every subtree bottom-up
 * and picks each node's heavy child (the largest subtree;
 * ties go to the first child in edge order). Pass 2 (line 3)
 * walks heavy-first, assigning each node a position and a
 * chain head: a heavy edge stays in the parent's chain, a
 * light edge starts a new chain headed by the child. Then the
 * demo path query (line 4) walks the path chain by chain.
 * Demo tree (children in listed order): 0:[1,2], 1:[3,4],
 * 2:[5], 4:[6,7]. Subtree sizes: {0:8, 1:5, 2:2, 3:1, 4:3,
 * 5:1, 6:1, 7:1}. Heavy child: 0&rarr;1, 1&rarr;4, 2&rarr;5,
 * 4&rarr;6. Positions (heavy-first DFS): {0:0, 1:1, 4:2, 6:3,
 * 7:4, 3:5, 2:6, 5:7}. Chains: head 0 &rarr; [0,1,4,6],
 * head 2 &rarr; [2,5], head 3 &rarr; [3], head 7 &rarr; [7]
 * (4 chains). Demo path query 7&rarr;5 (LCA 0): segments in
 * path order [7], [0,1,4], [2,5] — 3 chain segments; full
 * path [7,4,1,0,2,5]. 8 size steps + 8 decompose steps +
 * 3 query steps + final = 20 steps.</p>
 *
 * <p>Size step: {@code highlight} = [node], {@code data} =
 * [size, heavyChild or -1], {@code nodes} value = subtree
 * size (0 until computed). Decompose step: {@code highlight}
 * = [node], {@code data} = [pos, head], {@code nodes} value
 * = head id (0 until assigned). Query step: {@code highlight}
 * = [segment nodes, chain order (head first)], {@code data}
 * = [segment index 0..2], {@code sorted} = the full path
 * [7,4,1,0,2,5] (from the first query step on). Final step
 * {@code data} = [4] (chain count), {@code sorted} = the
 * full path. {@code edges} = the 7 tree edges throughout.
 * {@code nodes} are in id order, labelled with the node id,
 * x/y = 0 (the frontend lays the tree out).</p>
 *
 * <p>Bad input (a missing/empty edge list, or a start/target
 * that is not a node in the tree) yields a single "empty"
 * Step; the controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class HldService {

    private static final int LINE_SIZE = 2;
    private static final int LINE_DECOMPOSE = 3;
    private static final int LINE_QUERY = 4;
    private static final int LINE_FINAL = 0;

    /**
     * Runs heavy-light decomposition on the tree given by
     * {@code graphEdges} ([parent, child] pairs, children in
     * listed order) and queries the path from {@code start}
     * to {@code target} (null &rarr; 7 and 5, the demo query).
     */
    public List<Step> hld(List<List<Integer>> graphEdges,
                          Integer start, Integer target) {
        List<Step> steps = new ArrayList<>();
        if (graphEdges == null || graphEdges.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Tree edges are needed as "
                            + "[parent, child] pairs.", LINE_FINAL));
            return steps;
        }
        Map<Integer, List<Integer>> children = new LinkedHashMap<>();
        Map<Integer, Integer> parent = new LinkedHashMap<>();
        List<Integer> nodeIds = new ArrayList<>();
        for (List<Integer> edge : graphEdges) {
            if (edge == null || edge.size() < 2
                    || edge.get(0) == null || edge.get(1) == null) {
                continue;
            }
            int p = edge.get(0);
            int c = edge.get(1);
            children.computeIfAbsent(p, k -> new ArrayList<>());
            children.computeIfAbsent(c, k -> new ArrayList<>());
            if (!nodeIds.contains(p)) {
                nodeIds.add(p);
            }
            if (!nodeIds.contains(c)) {
                nodeIds.add(c);
            }
            // A second parent means the input is not a tree.
            if (parent.containsKey(c)) {
                steps.add(new Step(List.of(), List.of(), List.of(),
                        List.of(), "Each node needs exactly one "
                                + "parent.", LINE_FINAL));
                return steps;
            }
            parent.put(c, p);
            children.get(p).add(c);
        }
        if (nodeIds.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Tree edges are needed as "
                            + "[parent, child] pairs.", LINE_FINAL));
            return steps;
        }
        nodeIds.sort(Integer::compareTo);
        int root = -1;
        for (int id : nodeIds) {
            if (!parent.containsKey(id)) {
                root = id;
                break;
            }
        }
        if (root < 0) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The tree needs a root.",
                    LINE_FINAL));
            return steps;
        }
        int from = start != null ? start : 7;
        int to = target != null ? target : 5;
        if (!nodeIds.contains(from) || !nodeIds.contains(to)) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Start and target must be nodes "
                            + "in the tree.", LINE_FINAL));
            return steps;
        }
        List<EdgeDto> edges = new ArrayList<>();
        for (List<Integer> edge : graphEdges) {
            if (edge == null || edge.size() < 2
                    || edge.get(0) == null || edge.get(1) == null) {
                continue;
            }
            edges.add(new EdgeDto(edge.get(0), edge.get(1), null));
        }

        // Pass 1: subtree sizes + heavy child, post-order.
        Map<Integer, Integer> size = new LinkedHashMap<>();
        Map<Integer, Integer> heavy = new LinkedHashMap<>();
        List<Integer> postOrder = new ArrayList<>();
        sizes(root, children, size, heavy, postOrder);
        for (int i = 0; i < postOrder.size(); i++) {
            int node = postOrder.get(i);
            // Only the nodes sized so far show a size; the rest read 0.
            Map<Integer, Integer> sizedSoFar = new LinkedHashMap<>();
            for (int j = 0; j <= i; j++) {
                sizedSoFar.put(postOrder.get(j),
                        size.get(postOrder.get(j)));
            }
            steps.add(new Step(
                    List.of(size.get(node), heavy.get(node)),
                    List.of(node), List.of(), List.of(),
                    "Node " + node + ": subtree size "
                            + size.get(node)
                            + (heavy.get(node) < 0
                            ? " (leaf, no heavy child)."
                            : ", heavy child " + heavy.get(node)
                                    + "."),
                    LINE_SIZE,
                    snapshot(nodeIds, sizedSoFar, 0), edges));
        }

        // Pass 2: heavy-first decompose — position + chain head.
        Map<Integer, Integer> pos = new LinkedHashMap<>();
        Map<Integer, Integer> head = new LinkedHashMap<>();
        List<Integer> decompOrder = new ArrayList<>();
        decompose(root, root, children, heavy, pos, head,
                decompOrder);
        for (int i = 0; i < decompOrder.size(); i++) {
            int node = decompOrder.get(i);
            // Only the nodes decomposed so far show a head; the rest read 0.
            Map<Integer, Integer> headedSoFar = new LinkedHashMap<>();
            for (int j = 0; j <= i; j++) {
                headedSoFar.put(decompOrder.get(j),
                        head.get(decompOrder.get(j)));
            }
            steps.add(new Step(
                    List.of(pos.get(node), head.get(node)),
                    List.of(node), List.of(), List.of(),
                    "Node " + node + ": position " + pos.get(node)
                            + ", chain head " + head.get(node) + ".",
                    LINE_DECOMPOSE,
                    snapshot(nodeIds, headedSoFar, 0), edges));
        }

        // Path query from -> to, grouped by chain.
        List<Integer> path = path(from, to, parent);
        List<List<Integer>> segments = segments(path, head, pos);
        for (int i = 0; i < segments.size(); i++) {
            steps.add(new Step(
                    List.of(i), new ArrayList<>(segments.get(i)),
                    List.of(), new ArrayList<>(path),
                    "Segment " + (i + 1) + " of " + segments.size()
                            + ": chain head "
                            + head.get(segments.get(i).get(0))
                            + " " + segments.get(i) + ".",
                    LINE_QUERY,
                    snapshot(nodeIds, head, 0), edges));
        }
        int chainCount = (int) head.values().stream().distinct()
                .count();
        steps.add(new Step(
                List.of(chainCount), List.of(), List.of(),
                new ArrayList<>(path),
                "Heavy-light decomposition done: " + chainCount
                        + " chains; path " + from + " to " + to
                        + " is " + path + ".",
                LINE_FINAL, snapshot(nodeIds, head, 0), edges));
        return steps;
    }

    /** Post-order sizes; heavy = largest child (first wins ties). */
    private int sizes(int node, Map<Integer, List<Integer>> children,
                      Map<Integer, Integer> size,
                      Map<Integer, Integer> heavy,
                      List<Integer> postOrder) {
        int total = 1;
        int best = -1;
        int bestSize = -1;
        for (int child : children.getOrDefault(node, List.of())) {
            int childSize = sizes(child, children, size, heavy,
                    postOrder);
            total += childSize;
            if (childSize > bestSize) {
                bestSize = childSize;
                best = child;
            }
        }
        size.put(node, total);
        heavy.put(node, best);
        postOrder.add(node);
        return total;
    }

    /** Heavy-first DFS: heavy child keeps the head, light children start new chains. */
    private void decompose(int node, int headId,
                           Map<Integer, List<Integer>> children,
                           Map<Integer, Integer> heavy,
                           Map<Integer, Integer> pos,
                           Map<Integer, Integer> head,
                           List<Integer> order) {
        pos.put(node, order.size());
        head.put(node, headId);
        order.add(node);
        int heavyChild = heavy.getOrDefault(node, -1);
        if (heavyChild >= 0) {
            decompose(heavyChild, headId, children, heavy, pos,
                    head, order);
        }
        for (int child : children.getOrDefault(node, List.of())) {
            if (child != heavyChild) {
                decompose(child, child, children, heavy, pos,
                        head, order);
            }
        }
    }

    /** The path from -> to via their lowest common ancestor. */
    private List<Integer> path(int from, int to,
                               Map<Integer, Integer> parent) {
        List<Integer> up = new ArrayList<>();
        int node = from;
        while (true) {
            up.add(node);
            Integer p = parent.get(node);
            if (p == null) {
                break;
            }
            node = p;
        }
        List<Integer> down = new ArrayList<>();
        node = to;
        while (true) {
            down.add(node);
            Integer p = parent.get(node);
            if (p == null) {
                break;
            }
            node = p;
        }
        int lca = -1;
        for (int candidate : up) {
            if (down.contains(candidate)) {
                lca = candidate;
                break;
            }
        }
        List<Integer> path = new ArrayList<>();
        for (int candidate : up) {
            path.add(candidate);
            if (candidate == lca) {
                break;
            }
        }
        List<Integer> tail = new ArrayList<>();
        for (int candidate : down) {
            if (candidate == lca) {
                break;
            }
            tail.add(candidate);
        }
        for (int i = tail.size() - 1; i >= 0; i--) {
            path.add(tail.get(i));
        }
        return path;
    }

    /** Groups the path by chain head; within a group, chain order (head first, by position). */
    private List<List<Integer>> segments(List<Integer> path,
                                         Map<Integer, Integer> head,
                                         Map<Integer, Integer> pos) {
        List<List<Integer>> segments = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        int currentHead = -1;
        for (int node : path) {
            int nodeHead = head.get(node);
            if (current.isEmpty()) {
                currentHead = nodeHead;
            } else if (nodeHead != currentHead) {
                segments.add(current);
                current = new ArrayList<>();
                currentHead = nodeHead;
            }
            current.add(node);
        }
        if (!current.isEmpty()) {
            segments.add(current);
        }
        for (List<Integer> segment : segments) {
            segment.sort((a, b) -> Integer.compare(
                    pos.get(a), pos.get(b)));
        }
        return segments;
    }

    /** One NodeDto per node in id order; value = the given flag map (0 until set). */
    private List<NodeDto> snapshot(List<Integer> nodeIds,
                                   Map<Integer, Integer> flags,
                                   int unset) {
        List<NodeDto> dtos = new ArrayList<>();
        for (int id : nodeIds) {
            NodeDto dto = new NodeDto(id,
                    flags.getOrDefault(id, unset), 0, 0);
            dto.setLabel(String.valueOf(id));
            dtos.add(dto);
        }
        return dtos;
    }
}
