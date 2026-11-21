package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

/**
 * Yen's K-shortest paths (Phase 23, the alternative routes
 * Google Maps shows you): the first path is the plain shortest
 * path; each next path is the best spur deviation off one of
 * the paths found so far. For a path's spur node, the root
 * path (everything before the spur node) is fixed, the root
 * nodes are banned, and the edge the already-found paths take
 * out of the spur node is banned; a Dijkstra from the spur
 * node then gives a candidate (root + spur path). Candidates
 * carry across iterations — a loser in one round can win the
 * next. The spur Dijkstra breaks equal distances by the
 * lexicographically smallest node sequence (the same
 * tie-break as the plain Dijkstra service).
 *
 * <p>Steps: one chosen step per path found (line 2), and per
 * spur attempt one spur step (line 3) plus one candidate step
 * (line 4) or one no-spur step (line 5), plus the final Step
 * (line 0). {@code step.nodes} are the graph nodes (circular
 * layout); {@code step.edges} are the current edge list —
 * banned edges are EXCLUDED during a spur search. {@code data}
 * is [k, candidate length] for candidate steps, [k] for chosen
 * and spur/no-spur steps, and the K path lengths in order for
 * the final step. Highlight is node ids: the path nodes for a
 * chosen/candidate step, [spur node] for a spur/no-spur step.
 * Demo (the CH demo graph, source 0, target 5, K=3): k1 = 13
 * via [0,2,1,3,4,5]; k2 = 14 via [0,1,3,4,5] (it beats the
 * tied 14 via [0,2,1,3,5] on lexicographic order); k3 = 14
 * via [0,2,1,3,5]. k2 has 5 spur attempts (spur 0 &rarr;
 * candidate 14 [0,1,3,4,5]; spur 2 &rarr; 15 [0,2,3,4,5];
 * spur 1 &rarr; no spur path; spur 3 &rarr; 14 [0,2,1,3,5];
 * spur 4 &rarr; no spur path); k3 has 4 spur attempts (spur 0
 * &rarr; no spur path; spur 1 &rarr; 18 [0,1,2,3,4,5]; spur 3
 * &rarr; 15 [0,1,3,5]; spur 4 &rarr; no spur path). 1 + 10 + 1
 * + 8 + 1 + 1 = 22 steps.</p>
 *
 * <p>The controller validates the input (graphEdges/start
 * missing or any weight &lt;= 0, target missing, start ==
 * target, or start/target not in the graph &rarr; 400, K
 * (index) outside 1..5 &rarr; 400); this method guards against
 * bad input anyway with a single "empty" Step, and defaults a
 * missing K to 3.</p>
 */
@Service
public class YenService {

    private static final int LINE_CHOSEN = 2;
    private static final int LINE_SPUR = 3;
    private static final int LINE_CANDIDATE = 4;
    private static final int LINE_NO_SPUR = 5;
    private static final int LINE_FINAL = 0;

    private static final int DEFAULT_K = 3;

    /** One weighted edge, in input order. */
    private static class WeightedEdge {
        final int u;
        final int v;
        final int w;

        WeightedEdge(int u, int v, int w) {
            this.u = u;
            this.v = v;
            this.w = w;
        }
    }

    /** A (distance, path) pair — the spur Dijkstra's result. */
    private static class PathResult {
        final int dist;
        final List<Integer> path;

        PathResult(int dist, List<Integer> path) {
            this.dist = dist;
            this.path = path;
        }
    }

    /**
     * Runs Yen's algorithm. {@code graphEdges} are [u, v] or
     * [u, v, w] triples (undirected); {@code k} is the number
     * of paths (null = 3).
     */
    public List<Step> yen(List<List<Integer>> graphEdges, Integer start,
                          Integer target, Integer k) {
        List<Step> steps = new ArrayList<>();
        List<WeightedEdge> edges = parseEdges(graphEdges);
        Set<Integer> nodeSet = new TreeSet<>();
        for (WeightedEdge e : edges) {
            nodeSet.add(e.u);
            nodeSet.add(e.v);
        }
        List<Integer> nodes = new ArrayList<>(nodeSet);
        if (edges.isEmpty() || nodes.isEmpty()) {
            steps.add(yenStep(List.of(), List.of(), List.of(),
                    "A graph is needed.", LINE_FINAL,
                    List.of(), List.of()));
            return steps;
        }
        if (start == null || target == null
                || !nodeSet.contains(start) || !nodeSet.contains(target)) {
            steps.add(yenStep(List.of(), List.of(), List.of(),
                    "Invalid start or target node.", LINE_FINAL,
                    snapshotNodes(nodes), snapshotEdges(edges, Set.of())));
            return steps;
        }
        if (start.equals(target)) {
            steps.add(yenStep(List.of(), List.of(), List.of(),
                    "Start and target must be different.", LINE_FINAL,
                    snapshotNodes(nodes), snapshotEdges(edges, Set.of())));
            return steps;
        }
        int paths = k == null ? DEFAULT_K : k;
        if (paths < 1 || paths > 5) {
            steps.add(yenStep(List.of(), List.of(), List.of(),
                    "K must be between 1 and 5.", LINE_FINAL,
                    snapshotNodes(nodes), snapshotEdges(edges, Set.of())));
            return steps;
        }
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        List<EdgeDto> fullEdges = snapshotEdges(edges, Set.of());
        Map<String, Integer> weightByPair = weightByPair(edges);

        PathResult first = dijkstraPath(edges, start, target,
                Set.of(), Set.of());
        if (first == null) {
            steps.add(yenStep(List.of(), List.of(), List.of(),
                    "No path from " + start + " to " + target + ".",
                    LINE_FINAL, nodeDtos, fullEdges));
            return steps;
        }
        List<PathResult> found = new ArrayList<>();
        found.add(first);
        List<PathResult> pool = new ArrayList<>();
        steps.add(yenStep(List.of(1), new ArrayList<>(first.path),
                new ArrayList<>(first.path),
                "Path 1 (shortest): " + pathText(first.path)
                        + ", length " + first.dist + ".",
                LINE_CHOSEN, nodeDtos, fullEdges));
        for (int kth = 2; kth <= paths; kth++) {
            PathResult prev = found.get(found.size() - 1);
            List<Integer> prevPath = prev.path;
            for (int i = 0; i < prevPath.size() - 1; i++) {
                int spur = prevPath.get(i);
                List<Integer> root = new ArrayList<>(
                        prevPath.subList(0, i + 1));
                Set<Integer> bannedNodes = new HashSet<>(root);
                bannedNodes.remove(spur);
                Set<String> bannedEdges = new HashSet<>();
                for (PathResult p : found) {
                    if (p.path.size() > i + 1
                            && p.path.subList(0, i + 1).equals(root)) {
                        bannedEdges.add(pairKey(p.path.get(i),
                                p.path.get(i + 1)));
                    }
                }
                List<EdgeDto> spurEdges = snapshotEdges(edges, bannedEdges);
                steps.add(yenStep(List.of(kth), List.of(spur),
                        new ArrayList<>(root),
                        "Spur " + spur + " (path " + kth + "): root "
                                + pathText(root) + ".",
                        LINE_SPUR, nodeDtos, spurEdges));
                PathResult spurResult = dijkstraPath(edges, spur, target,
                        bannedNodes, bannedEdges);
                if (spurResult == null) {
                    steps.add(yenStep(List.of(kth), List.of(spur),
                            new ArrayList<>(root),
                            "Spur " + spur + ": no spur path — every "
                                    + "neighbour is banned.",
                            LINE_NO_SPUR, nodeDtos, spurEdges));
                    continue;
                }
                List<Integer> candidate = new ArrayList<>(root);
                candidate.addAll(spurResult.path.subList(1,
                        spurResult.path.size()));
                int rootCost = rootCost(root, weightByPair);
                int candidateLength = rootCost + spurResult.dist;
                PathResult cand = new PathResult(candidateLength, candidate);
                steps.add(yenStep(List.of(kth, candidateLength),
                        new ArrayList<>(candidate),
                        new ArrayList<>(candidate),
                        "Candidate " + pathText(candidate) + ", length "
                                + candidateLength + ".",
                        LINE_CANDIDATE, nodeDtos, spurEdges));
                if (!contains(pool, cand) && !contains(found, cand)) {
                    pool.add(cand);
                }
            }
            if (pool.isEmpty()) {
                break;
            }
            // Best candidate first: shortest length, then the
            // lexicographically smallest node sequence.
            pool.sort((a, b) -> a.dist != b.dist
                    ? Integer.compare(a.dist, b.dist)
                    : comparePaths(a.path, b.path));
            PathResult next = pool.remove(0);
            found.add(next);
            steps.add(yenStep(List.of(found.size()),
                    new ArrayList<>(next.path),
                    new ArrayList<>(next.path),
                    "Path " + found.size() + ": " + pathText(next.path)
                            + ", length " + next.dist + ".",
                    LINE_CHOSEN, nodeDtos, fullEdges));
        }
        List<Integer> lengths = new ArrayList<>();
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < found.size(); i++) {
            lengths.add(found.get(i).dist);
            if (i > 0) {
                message.append(" ");
            }
            message.append("Path ").append(i + 1).append(": ")
                    .append(pathText(found.get(i).path))
                    .append(" (").append(found.get(i).dist).append(").");
        }
        steps.add(yenStep(lengths, List.of(),
                new ArrayList<>(found.get(found.size() - 1).path),
                found.size() + " shortest path"
                        + (found.size() == 1 ? ": " : "s: ")
                        + message,
                LINE_FINAL, nodeDtos, fullEdges));
        return steps;
    }

    /** True when the (dist, path) pair is already in the list. */
    private boolean contains(List<PathResult> list, PathResult cand) {
        for (PathResult p : list) {
            if (p.dist == cand.dist && p.path.equals(cand.path)) {
                return true;
            }
        }
        return false;
    }

    /** The cost of a root path, from the edge list (first match wins). */
    private int rootCost(List<Integer> root,
                         Map<String, Integer> weightByPair) {
        int cost = 0;
        for (int i = 0; i + 1 < root.size(); i++) {
            cost += weightByPair.getOrDefault(
                    pairKey(root.get(i), root.get(i + 1)), 0);
        }
        return cost;
    }

    /** Edge weights by unordered pair (first edge wins, like the sim). */
    private Map<String, Integer> weightByPair(List<WeightedEdge> edges) {
        Map<String, Integer> map = new HashMap<>();
        for (WeightedEdge e : edges) {
            map.putIfAbsent(pairKey(e.u, e.v), e.w);
        }
        return map;
    }

    /** "min-max" key for an unordered node pair. */
    private String pairKey(int a, int b) {
        return Math.min(a, b) + "-" + Math.max(a, b);
    }

    /** Lexicographic path comparison (shorter wins on a prefix). */
    private static int comparePaths(List<Integer> a, List<Integer> b) {
        int n = Math.min(a.size(), b.size());
        for (int i = 0; i < n; i++) {
            int c = Integer.compare(a.get(i), b.get(i));
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(a.size(), b.size());
    }

    /**
     * Dijkstra returning (dist, path), breaking equal distances
     * by the lexicographically smallest node sequence: the
     * priority queue orders (dist, path) pairs, neighbours are
     * pushed in ascending (node, weight) order, and a node is
     * settled the first time it is popped (the sim's algorithm).
     * Banned nodes/edges are excluded from the adjacency.
     */
    private PathResult dijkstraPath(List<WeightedEdge> edges, int start,
                                    int target, Set<Integer> bannedNodes,
                                    Set<String> bannedEdges) {
        Map<Integer, List<int[]>> adj = new HashMap<>();
        for (WeightedEdge e : edges) {
            if (bannedEdges.contains(pairKey(e.u, e.v))
                    || bannedNodes.contains(e.u)
                    || bannedNodes.contains(e.v)) {
                continue;
            }
            adj.computeIfAbsent(e.u, x -> new ArrayList<>())
                    .add(new int[]{e.v, e.w});
            adj.computeIfAbsent(e.v, x -> new ArrayList<>())
                    .add(new int[]{e.u, e.w});
        }
        for (List<int[]> neighbours : adj.values()) {
            neighbours.sort((a, b) -> a[0] != b[0]
                    ? Integer.compare(a[0], b[0])
                    : Integer.compare(a[1], b[1]));
        }
        PriorityQueue<PathResult> pq = new PriorityQueue<>((a, b) ->
                a.dist != b.dist ? Integer.compare(a.dist, b.dist)
                        : comparePaths(a.path, b.path));
        Map<Integer, Integer> seen = new HashMap<>();
        pq.add(new PathResult(0, List.of(start)));
        while (!pq.isEmpty()) {
            PathResult cur = pq.remove();
            int u = cur.path.get(cur.path.size() - 1);
            Integer settled = seen.get(u);
            if (settled != null && settled <= cur.dist) {
                continue;
            }
            seen.put(u, cur.dist);
            if (u == target) {
                return cur;
            }
            for (int[] nb : adj.getOrDefault(u, List.of())) {
                List<Integer> next = new ArrayList<>(cur.path);
                next.add(nb[0]);
                pq.add(new PathResult(cur.dist + nb[1], next));
            }
        }
        return null;
    }

    /** "0 → 1 → 2" rendering of a path. */
    private String pathText(List<Integer> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                sb.append(" \u2192 ");
            }
            sb.append(path.get(i));
        }
        return sb.toString();
    }

    /**
     * Valid weighted edges in input order: [u, v] (weight 1) or
     * [u, v, w] (a null weight also defaults to 1). Malformed
     * pairs and self-loops are skipped.
     */
    private List<WeightedEdge> parseEdges(List<List<Integer>> graphEdges) {
        List<WeightedEdge> edges = new ArrayList<>();
        if (graphEdges == null) {
            return edges;
        }
        for (List<Integer> pair : graphEdges) {
            if (pair == null || (pair.size() != 2 && pair.size() != 3)) {
                continue;
            }
            Integer u = pair.get(0);
            Integer v = pair.get(1);
            if (u == null || v == null || u.equals(v)) {
                continue;
            }
            int w = pair.size() == 3 && pair.get(2) != null
                    ? pair.get(2) : 1;
            edges.add(new WeightedEdge(u, v, w));
        }
        return edges;
    }

    /** Circular layout: node i at angle 2*pi*i/n - pi/2 (GraphService). */
    private List<NodeDto> snapshotNodes(List<Integer> nodes) {
        List<NodeDto> dtos = new ArrayList<>();
        int n = nodes.size();
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            double x = 280 + 200 * Math.cos(angle);
            double y = 160 + 120 * Math.sin(angle);
            dtos.add(new NodeDto(nodes.get(i), nodes.get(i), x, y));
        }
        return dtos;
    }

    /** Edges as given (deduplicated by unordered pair), minus banned pairs. */
    private List<EdgeDto> snapshotEdges(List<WeightedEdge> edges,
                                        Set<String> banned) {
        List<EdgeDto> dtos = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (WeightedEdge e : edges) {
            String key = pairKey(e.u, e.v);
            if (banned.contains(key) || !seen.add(key)) {
                continue;
            }
            dtos.add(new EdgeDto(e.u, e.v, e.w));
        }
        return dtos;
    }

    /** One Yen Step: data, highlight (node ids), message, line, snapshot. */
    private Step yenStep(List<Integer> data, List<Integer> highlight,
                         List<Integer> sorted, String message, int line,
                         List<NodeDto> nodes, List<EdgeDto> edges) {
        return new Step(new ArrayList<>(data), new ArrayList<>(highlight),
                List.of(), new ArrayList<>(sorted), message, line,
                nodes, edges);
    }
}
