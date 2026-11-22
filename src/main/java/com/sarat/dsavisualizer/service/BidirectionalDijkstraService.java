package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Bidirectional Dijkstra (Phase 22, the Google Maps routing
 * algorithm): two Dijkstras run at once — one forward from the
 * source, one backward from the target — alternating settles,
 * forward first. After each settle, mu is the best path length
 * found so far: the minimum over every edge (u, v) of
 * distF[u] + w + distB[v] where BOTH endpoint distances are
 * known (frontier-inclusive, so a path through an unsettled
 * node still counts). The search stops when the smallest
 * unsettled forward key plus the smallest unsettled backward
 * key is &gt;= mu — no undiscovered path can beat mu then.
 * Min-extraction ties break by smallest (dist, node id);
 * neighbours are relaxed in ascending node-id order.
 *
 * <p>Steps: one per settle (line 2 forward / 6 backward), one
 * per relaxation that improves a neighbour (line 3 / 7), one
 * per mu improvement (line 4 / 8), plus the final Step
 * (line 0). Every step carries {@code data} = distF and
 * {@code aux} = distB (node-indexed, -1 = unknown); a settle
 * step highlights [node]; a relax step highlights [from, to];
 * a mu step highlights [a, b] (the meeting edge) and carries
 * {@code sorted} = the best path nodes so far (rebuilt from
 * the parent pointers at that moment). Every step carries the
 * full graph ({@code nodes}/{@code edges}, circular layout).
 * Demo (street network 0-1-2-3-4-5 plus side street 2-6-7):
 * shortest 7, path [0, 1, 2, 3, 4, 5], meeting edge (2, 3),
 * settles 5 (forward 3 + backward 2) vs plain Dijkstra 8,
 * mu improves 13 &rarr; 11 &rarr; 9 &rarr; 8 &rarr; 7,
 * 21 steps + final = 22.</p>
 *
 * <p>The controller validates the input (graphEdges/start
 * missing &rarr; 400, any weight &lt;= 0 &rarr; 400, target
 * missing, start/target not in the graph, or start == target
 * &rarr; 400); this method guards against bad input anyway
 * with a single "empty" Step.</p>
 */
@Service
public class BidirectionalDijkstraService {

    private static final int LINE_SETTLE_FORWARD = 2;
    private static final int LINE_RELAX_FORWARD = 3;
    private static final int LINE_MU_FORWARD = 4;
    private static final int LINE_SETTLE_BACKWARD = 6;
    private static final int LINE_RELAX_BACKWARD = 7;
    private static final int LINE_MU_BACKWARD = 8;
    private static final int LINE_FINAL = 0;

    /** Infinity for min-keys and mu (sums are longs, so no overflow). */
    private static final long INF = Long.MAX_VALUE;

    /** One weighted neighbour: the node id and the edge weight to it. */
    private static class Neighbor {
        final int id;
        final int weight;

        Neighbor(int id, int weight) {
            this.id = id;
            this.weight = weight;
        }
    }

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

    /**
     * Runs bidirectional Dijkstra on the undirected weighted edge
     * list ({@code [u, v]} or {@code [u, v, w]} triples) from
     * {@code start} to {@code target}.
     */
    public List<Step> bidijkstra(List<List<Integer>> graphEdges,
                                 Integer start, Integer target) {
        List<Step> steps = new ArrayList<>();
        List<WeightedEdge> edges = parseEdges(graphEdges);
        Set<Integer> nodeSet = new TreeSet<>();
        for (WeightedEdge e : edges) {
            nodeSet.add(e.u);
            nodeSet.add(e.v);
        }
        List<Integer> nodes = new ArrayList<>(nodeSet);
        if (edges.isEmpty() || nodes.isEmpty()) {
            steps.add(bidijkstraStep(List.of(), List.of(), List.of(),
                    List.of(), "A graph is needed.", LINE_FINAL,
                    List.of(), List.of()));
            return steps;
        }
        if (start == null || target == null
                || !nodeSet.contains(start) || !nodeSet.contains(target)) {
            steps.add(bidijkstraStep(List.of(), List.of(), List.of(),
                    List.of(), "Invalid start or target node.", LINE_FINAL,
                    snapshotNodes(nodes), snapshotEdges(edges)));
            return steps;
        }
        if (start.equals(target)) {
            steps.add(bidijkstraStep(List.of(), List.of(), List.of(),
                    List.of(), "Start and target must be different.",
                    LINE_FINAL, snapshotNodes(nodes),
                    snapshotEdges(edges)));
            return steps;
        }
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        List<EdgeDto> edgeDtos = snapshotEdges(edges);
        int maxId = nodes.get(nodes.size() - 1);

        // Weighted adjacency (undirected, neighbours by (id, weight)
        // — the sim's sorted(adj[n]) order).
        Map<Integer, List<Neighbor>> adj = new HashMap<>();
        for (int node : nodes) {
            adj.put(node, new ArrayList<>());
        }
        for (WeightedEdge e : edges) {
            adj.get(e.u).add(new Neighbor(e.v, e.w));
            adj.get(e.v).add(new Neighbor(e.u, e.w));
        }
        for (List<Neighbor> neighbours : adj.values()) {
            neighbours.sort(Comparator.comparingInt((Neighbor n) -> n.id)
                    .thenComparingInt(n -> n.weight));
        }

        // Search state: insertion-ordered distances (the sim's dicts),
        // parent pointers, settled sets, best length and meeting edge.
        Map<Integer, Integer> distF = new LinkedHashMap<>();
        Map<Integer, Integer> distB = new LinkedHashMap<>();
        Map<Integer, int[]> parentF = new HashMap<>();
        Map<Integer, int[]> parentB = new HashMap<>();
        distF.put(start, 0);
        distB.put(target, 0);
        Set<Integer> setF = new HashSet<>();
        Set<Integer> setB = new HashSet<>();
        long mu = INF;
        int[] meeting = null;
        MuUpdate update;

        while (true) {
            // ---- Forward half ----
            int[] fwd = minUnsettled(distF, setF, nodes);
            if (fwd == null) {
                break;
            }
            int u = fwd[0];
            int du = fwd[1];
            setF.add(u);
            steps.add(bidijkstraStep(distFList(distF, maxId),
                    distBList(distB, maxId), List.of(u), List.of(),
                    "Forward: settle node " + u + " (dist " + du + ").",
                    LINE_SETTLE_FORWARD, nodeDtos, edgeDtos));
            for (Neighbor nb : adj.getOrDefault(u, List.of())) {
                int nd = du + nb.weight;
                Integer old = distF.get(nb.id);
                if (old == null || nd < old) {
                    distF.put(nb.id, nd);
                    parentF.put(nb.id, new int[]{u, nb.weight});
                    steps.add(bidijkstraStep(distFList(distF, maxId),
                            distBList(distB, maxId), List.of(u, nb.id),
                            List.of(),
                            "Relax " + u + " -> " + nb.id + ": " + nd + ".",
                            LINE_RELAX_FORWARD, nodeDtos, edgeDtos));
                }
            }
            update = updateMu(edges, distF, distB, parentF,
                    parentB, start, target, mu, meeting, steps, nodeDtos,
                    edgeDtos, maxId, LINE_MU_FORWARD);
            mu = update.mu;
            meeting = update.meeting;
            long mf = minUnsettledDist(distF, setF, nodes);
            long mb = minUnsettledDist(distB, setB, nodes);
            if (mf == INF || mb == INF || mf + mb >= mu) {
                break;
            }
            // ---- Backward half ----
            int[] bwd = minUnsettled(distB, setB, nodes);
            if (bwd == null) {
                break;
            }
            int v = bwd[0];
            int dv = bwd[1];
            setB.add(v);
            steps.add(bidijkstraStep(distFList(distF, maxId),
                    distBList(distB, maxId), List.of(v), List.of(),
                    "Backward: settle node " + v + " (dist " + dv + ").",
                    LINE_SETTLE_BACKWARD, nodeDtos, edgeDtos));
            for (Neighbor nb : adj.getOrDefault(v, List.of())) {
                int nd = dv + nb.weight;
                Integer old = distB.get(nb.id);
                if (old == null || nd < old) {
                    distB.put(nb.id, nd);
                    parentB.put(nb.id, new int[]{v, nb.weight});
                    steps.add(bidijkstraStep(distFList(distF, maxId),
                            distBList(distB, maxId), List.of(v, nb.id),
                            List.of(),
                            "Relax " + v + " -> " + nb.id + ": " + nd + ".",
                            LINE_RELAX_BACKWARD, nodeDtos, edgeDtos));
                }
            }
            update = updateMu(edges, distF, distB, parentF,
                    parentB, start, target, mu, meeting, steps, nodeDtos,
                    edgeDtos, maxId, LINE_MU_BACKWARD);
            mu = update.mu;
            meeting = update.meeting;
            mf = minUnsettledDist(distF, setF, nodes);
            mb = minUnsettledDist(distB, setB, nodes);
            if (mf == INF || mb == INF || mf + mb >= mu) {
                break;
            }
        }

        if (meeting == null) {
            steps.add(bidijkstraStep(distFList(distF, maxId),
                    distBList(distB, maxId), List.of(), List.of(),
                    "No path from " + start + " to " + target + ".",
                    LINE_FINAL, nodeDtos, edgeDtos));
            return steps;
        }
        List<Integer> path = pathThrough(meeting[0], meeting[1],
                parentF, parentB, start, target);
        int settled = setF.size() + setB.size();
        steps.add(bidijkstraStep(distFList(distF, maxId),
                distBList(distB, maxId), List.of(),
                new ArrayList<>(path),
                "Shortest path: " + pathText(path) + ", length " + mu
                        + ". Bidirectional settled " + settled + " nodes.",
                LINE_FINAL, nodeDtos, edgeDtos));
        return steps;
    }

    /** The result of a mu update: the best length and its meeting edge. */
    private static class MuUpdate {
        final long mu;
        final int[] meeting;

        MuUpdate(long mu, int[] meeting) {
            this.mu = mu;
            this.meeting = meeting;
        }
    }

    /**
     * Mu update over every edge in input order, both directions:
     * when distF[a] + w + distB[b] beats mu, record a mu step
     * (line 4 forward / 8 backward) with the meeting edge
     * highlighted and the best path so far in sorted. Returns
     * the new mu and meeting edge.
     */
    private MuUpdate updateMu(List<WeightedEdge> edges,
                              Map<Integer, Integer> distF,
                              Map<Integer, Integer> distB,
                              Map<Integer, int[]> parentF,
                              Map<Integer, int[]> parentB,
                              int start, int target,
                              long mu, int[] meeting, List<Step> steps,
                              List<NodeDto> nodeDtos, List<EdgeDto> edgeDtos,
                              int maxId, int line) {
        long best = mu;
        int[] bestMeeting = meeting;
        for (WeightedEdge e : edges) {
            for (int[] ab : new int[][]{{e.u, e.v}, {e.v, e.u}}) {
                int a = ab[0];
                int b = ab[1];
                Integer da = distF.get(a);
                Integer db = distB.get(b);
                if (da == null || db == null) {
                    continue;
                }
                long c = (long) da + e.w + db;
                if (c < best) {
                    best = c;
                    bestMeeting = new int[]{a, b};
                    List<Integer> path = pathThrough(a, b, parentF,
                            parentB, start, target);
                    steps.add(bidijkstraStep(distFList(distF, maxId),
                            distBList(distB, maxId), List.of(a, b),
                            path,
                            "New best path via edge " + a + "-" + b
                                    + ": length " + best + ".",
                            line, nodeDtos, edgeDtos));
                }
            }
        }
        return new MuUpdate(best, bestMeeting);
    }

    /**
     * The path through the meeting edge (a, b): the forward
     * parent chain from a back to the start, then the backward
     * parent chain from b forward to the target.
     */
    private List<Integer> pathThrough(int a, int b,
                                      Map<Integer, int[]> parentF,
                                      Map<Integer, int[]> parentB,
                                      int start, int target) {
        List<Integer> fwd = new ArrayList<>();
        int x = a;
        while (true) {
            fwd.add(x);
            if (x == start) {
                break;
            }
            int[] parent = parentF.get(x);
            if (parent == null) {
                break;
            }
            x = parent[0];
        }
        Collections.reverse(fwd);
        List<Integer> path = new ArrayList<>(fwd);
        int y = b;
        while (y != target) {
            path.add(y);
            int[] parent = parentB.get(y);
            if (parent == null) {
                break;
            }
            y = parent[0];
        }
        path.add(target);
        return path;
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
     * The unsettled node with the smallest known distance
     * (ties: smallest node id — nodes are scanned ascending
     * with a strict &lt;). Returns [node, dist], or null when
     * nothing is left unsettled.
     */
    private int[] minUnsettled(Map<Integer, Integer> dist,
                               Set<Integer> settled,
                               List<Integer> nodes) {
        Integer bestNode = null;
        int bestDist = Integer.MAX_VALUE;
        for (int node : nodes) {
            if (settled.contains(node)) {
                continue;
            }
            Integer d = dist.get(node);
            if (d != null && d < bestDist) {
                bestDist = d;
                bestNode = node;
            }
        }
        return bestNode == null ? null : new int[]{bestNode, bestDist};
    }

    /** The smallest unsettled distance, or INF when none is left. */
    private long minUnsettledDist(Map<Integer, Integer> dist,
                                  Set<Integer> settled,
                                  List<Integer> nodes) {
        int[] min = minUnsettled(dist, settled, nodes);
        return min == null ? INF : min[1];
    }

    /** distF as a node-indexed list (-1 = unknown). */
    private List<Integer> distFList(Map<Integer, Integer> distF,
                                    int maxId) {
        return distList(distF, maxId);
    }

    /** distB as a node-indexed list (-1 = unknown). */
    private List<Integer> distBList(Map<Integer, Integer> distB,
                                    int maxId) {
        return distList(distB, maxId);
    }

    /** A distance map as a node-indexed list (-1 = unknown). */
    private List<Integer> distList(Map<Integer, Integer> dist,
                                   int maxId) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i <= maxId; i++) {
            out.add(dist.getOrDefault(i, -1));
        }
        return out;
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

    /** Edges as given (deduplicated by unordered pair), with weights. */
    private List<EdgeDto> snapshotEdges(List<WeightedEdge> edges) {
        List<EdgeDto> dtos = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (WeightedEdge e : edges) {
            String key = Math.min(e.u, e.v) + "-" + Math.max(e.u, e.v);
            if (seen.add(key)) {
                dtos.add(new EdgeDto(e.u, e.v, e.w));
            }
        }
        return dtos;
    }

    /** One bidirectional-Dijkstra Step: distF in data, distB in aux. */
    private Step bidijkstraStep(List<Integer> distF, List<Integer> distB,
                                List<Integer> highlight,
                                List<Integer> sorted, String message,
                                int line, List<NodeDto> nodes,
                                List<EdgeDto> edges) {
        Step step = new Step(new ArrayList<>(distF), highlight,
                List.of(), new ArrayList<>(sorted), message, line,
                nodes, edges);
        step.setAux(new ArrayList<>(distB));
        return step;
    }
}
