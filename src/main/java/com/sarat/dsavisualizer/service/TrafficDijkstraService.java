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
 * Traffic-aware Dijkstra (Phase 23, ETA with live traffic):
 * the same trip at rush hour takes a different road. The
 * model is a snapshot — each edge's weight is fixed for the
 * whole trip, looked up once from the departure hour: if
 * 7 &lt;= hour &lt;= 9 (rush hour) the edge's weight is base
 * &times; its rush factor, otherwise base &times; 1. That
 * keeps Dijkstra exact (static weights per run) and is how
 * short trips are approximated in practice.
 *
 * <p>Wire encoding (the Java model cannot carry decimals):
 * the rush factors arrive in {@code values} as integers
 * &times; 10 (tenths — 15 means 1.5&times;); effective
 * weights and arrival times in {@code step.data} and
 * {@code step.edges} are likewise &times; 10 (the demo 8:00
 * effective weights are 30, 60, 10, 150, 50, 40, 10, 20, 60,
 * 10, 10 and the arrival times are 0, 30, 40, 50, 60, 80, 90).
 * The frontend divides by 10 when rendering. The messages
 * carry the exact decimals as text, e.g. "Relax edge 2 -&gt;
 * 3: effective 5 (base 1 &times; 5, rush hour)."</p>
 *
 * <p>Steps: the plain Dijkstra shape (one relax step per
 * improvement, line 3, then one settle step per node, line 2)
 * plus the final Step (line 0). {@code step.edges} carry the
 * EFFECTIVE weights (&times; 10); {@code data} is [arrival
 * time &times; 10] for a settle step or [effective weight
 * &times; 10] for a relax step, and the first step's data
 * also carries the departure hour ([hour, ...]) so the
 * frontend can label the run. {@code sorted} holds the settled
 * node ids; a relax step highlights [to], a settle step [u].
 * Demo (the Phase 22 street graph, rush factors [15, 15, 10,
 * 30, 50, 10, 10, 10, 10, 10, 10]): depart 8:00 &rarr; route
 * [0,1,2,4,5], length 9 (the jammed 2-3 is avoided; effective
 * weights 0-1:3, 0-2:6, 1-3:15, 2-3:5), 18 steps; depart
 * 10:00 &rarr; route [0,1,2,3,4,5], length 7 (the plain
 * shortest path), 20 steps. Equal distances break by the
 * lexicographically smallest node sequence (the sim's
 * (dist, path) queue order).</p>
 *
 * <p>The controller validates the input (graphEdges/start
 * missing or any weight &lt;= 0, target missing, start ==
 * target, or start/target not in the graph &rarr; 400,
 * departure hour (index) outside 0..23 &rarr; 400, rush
 * factors (values) with a non-positive entry or a length
 * different from the edge count &rarr; 400); this method
 * guards against bad input anyway with a single "empty" Step,
 * and defaults a missing hour to 8 and missing factors to
 * all 1&times;.</p>
 */
@Service
public class TrafficDijkstraService {

    private static final int LINE_SETTLE = 2;
    private static final int LINE_RELAX = 3;
    private static final int LINE_FINAL = 0;

    private static final int DEFAULT_HOUR = 8;

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

    /** A (distance, path) pair in the Dijkstra queue. */
    private static class QueueEntry {
        final double dist;
        final List<Integer> path;

        QueueEntry(double dist, List<Integer> path) {
            this.dist = dist;
            this.path = path;
        }
    }

    /**
     * Runs traffic-aware Dijkstra. {@code graphEdges} are
     * [u, v] or [u, v, w] triples (undirected); {@code hour}
     * is the departure hour (null = 8); {@code values} are the
     * per-edge rush factors &times; 10, parallel to
     * {@code graphEdges} (null = all 1&times;).
     */
    public List<Step> traffic(List<List<Integer>> graphEdges,
                              Integer start, Integer target,
                              Integer hour, List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        List<WeightedEdge> edges = parseEdges(graphEdges);
        Set<Integer> nodeSet = new TreeSet<>();
        for (WeightedEdge e : edges) {
            nodeSet.add(e.u);
            nodeSet.add(e.v);
        }
        List<Integer> nodes = new ArrayList<>(nodeSet);
        if (edges.isEmpty() || nodes.isEmpty()) {
            steps.add(trafficStep(List.of(), List.of(), List.of(),
                    "A graph is needed.", LINE_FINAL,
                    List.of(), List.of()));
            return steps;
        }
        if (start == null || target == null
                || !nodeSet.contains(start) || !nodeSet.contains(target)) {
            steps.add(trafficStep(List.of(), List.of(), List.of(),
                    "Invalid start or target node.", LINE_FINAL,
                    snapshotNodes(nodes),
                    snapshotEdges(edges, List.of())));
            return steps;
        }
        if (start.equals(target)) {
            steps.add(trafficStep(List.of(), List.of(), List.of(),
                    "Start and target must be different.", LINE_FINAL,
                    snapshotNodes(nodes),
                    snapshotEdges(edges, List.of())));
            return steps;
        }
        int depHour = hour == null ? DEFAULT_HOUR : hour;
        if (depHour < 0 || depHour > 23) {
            steps.add(trafficStep(List.of(), List.of(), List.of(),
                    "Departure hour must be between 0 and 23.",
                    LINE_FINAL, snapshotNodes(nodes),
                    snapshotEdges(edges, List.of())));
            return steps;
        }
        List<Double> factors = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            for (int i = 0; i < edges.size(); i++) {
                factors.add(1.0);
            }
        } else {
            if (values.size() != edges.size()) {
                steps.add(trafficStep(List.of(), List.of(), List.of(),
                        "Rush factors must be parallel to the edges.",
                        LINE_FINAL, snapshotNodes(nodes),
                        snapshotEdges(edges, List.of())));
                return steps;
            }
            for (Integer v : values) {
                if (v == null || v <= 0) {
                    steps.add(trafficStep(List.of(), List.of(), List.of(),
                            "Rush factors must be positive.",
                            LINE_FINAL, snapshotNodes(nodes),
                            snapshotEdges(edges, List.of())));
                    return steps;
                }
                factors.add(v / 10.0);
            }
        }
        boolean rush = depHour >= 7 && depHour <= 9;
        List<Double> effective = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            effective.add(edges.get(i).w * (rush ? factors.get(i) : 1.0));
        }
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        List<EdgeDto> edgeDtos = snapshotEdges(edges, effective);

        Map<Integer, List<int[]>> adj = buildAdjacency(edges, effective);
        Map<Integer, Double> dist = new HashMap<>();
        dist.put(start, 0.0);
        Set<Integer> settled = new HashSet<>();
        List<Integer> settledOrder = new ArrayList<>();
        PriorityQueue<QueueEntry> pq = new PriorityQueue<>(
                (a, b) -> a.dist != b.dist
                        ? Double.compare(a.dist, b.dist)
                        : comparePaths(a.path, b.path));
        pq.add(new QueueEntry(0.0, List.of(start)));
        boolean firstStep = true;
        List<Integer> finalPath = null;
        double finalLength = 0;
        while (!pq.isEmpty()) {
            QueueEntry cur = pq.remove();
            int u = cur.path.get(cur.path.size() - 1);
            if (settled.contains(u)) {
                continue;
            }
            double du = cur.dist;
            // Relax u's neighbours (ascending), one Step per improvement.
            for (int[] nb : adj.getOrDefault(u, List.of())) {
                int v = nb[0];
                double w = effective.get(nb[1]);
                double nd = du + w;
                Double old = dist.get(v);
                if (old == null || nd < old) {
                    dist.put(v, nd);
                    List<Integer> data = new ArrayList<>();
                    if (firstStep) {
                        data.add(depHour);
                        firstStep = false;
                    }
                    data.add((int) Math.round(w * 10));
                    // Highlight both endpoints: the visualizer
                    // draws the relaxed edge active only when
                    // highlight has exactly its two endpoints.
                    steps.add(trafficStep(data, List.of(u, v),
                            new ArrayList<>(settledOrder),
                            "Relax edge " + u + " -> " + v
                                    + ": effective " + fmt(w) + " (base "
                                    + edges.get(nb[1]).w
                                    + (rush ? " × "
                                    + fmt(factors.get(nb[1]))
                                    + ", rush hour)."
                                    : ", off-peak)."),
                            LINE_RELAX, nodeDtos, edgeDtos));
                }
                List<Integer> nextPath = new ArrayList<>(cur.path);
                nextPath.add(v);
                pq.add(new QueueEntry(nd, nextPath));
            }
            settled.add(u);
            settledOrder.add(u);
            if (u == target && finalPath == null) {
                // The target's shortest path is fixed the first
                // time it is settled; the trace keeps settling
                // the remaining reachable nodes (plain Dijkstra
                // settles everything, it does not stop at the
                // target).
                finalPath = cur.path;
                finalLength = du;
            }
            List<Integer> data = new ArrayList<>();
            if (firstStep) {
                data.add(depHour);
                firstStep = false;
            }
            data.add((int) Math.round(du * 10));
            steps.add(trafficStep(data, List.of(u),
                    new ArrayList<>(settledOrder),
                    "Settle node " + u + " with arrival time "
                            + fmt(du) + ".",
                    LINE_SETTLE, nodeDtos, edgeDtos));
        }
        if (finalPath == null) {
            steps.add(trafficStep(List.of(), List.of(), List.of(),
                    "No path from " + start + " to " + target + ".",
                    LINE_FINAL, nodeDtos, edgeDtos));
            return steps;
        }
        steps.add(trafficStep(
                List.of((int) Math.round(finalLength * 10)), List.of(),
                new ArrayList<>(finalPath),
                "Shortest path: " + pathText(finalPath) + ", length "
                        + fmt(finalLength) + ". Departed " + depHour
                        + ":00 (" + (rush ? "rush hour" : "off-peak")
                        + ").",
                LINE_FINAL, nodeDtos, edgeDtos));
        return steps;
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
     * Weighted adjacency (undirected): each neighbour is
     * [node id, edge index]; neighbours sorted by (id, weight).
     */
    private Map<Integer, List<int[]>> buildAdjacency(
            List<WeightedEdge> edges, List<Double> effective) {
        Map<Integer, List<int[]>> adj = new HashMap<>();
        for (int i = 0; i < edges.size(); i++) {
            WeightedEdge e = edges.get(i);
            adj.computeIfAbsent(e.u, x -> new ArrayList<>())
                    .add(new int[]{e.v, i});
            adj.computeIfAbsent(e.v, x -> new ArrayList<>())
                    .add(new int[]{e.u, i});
        }
        for (List<int[]> neighbours : adj.values()) {
            neighbours.sort((a, b) -> a[0] != b[0]
                    ? Integer.compare(a[0], b[0])
                    : Double.compare(effective.get(a[1]),
                            effective.get(b[1])));
        }
        return adj;
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

    /** Edges as given (deduplicated by unordered pair), effective weights ×10. */
    private List<EdgeDto> snapshotEdges(List<WeightedEdge> edges,
                                        List<Double> effective) {
        List<EdgeDto> dtos = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < edges.size(); i++) {
            WeightedEdge e = edges.get(i);
            String key = Math.min(e.u, e.v) + "-" + Math.max(e.u, e.v);
            if (!seen.add(key)) {
                continue;
            }
            int weight = effective.isEmpty() ? e.w
                    : (int) Math.round(effective.get(i) * 10);
            dtos.add(new EdgeDto(e.u, e.v, weight));
        }
        return dtos;
    }

    /** One traffic Step: data, highlight, settled order, message, line. */
    private Step trafficStep(List<Integer> data, List<Integer> highlight,
                             List<Integer> sorted, String message, int line,
                             List<NodeDto> nodes, List<EdgeDto> edges) {
        return new Step(new ArrayList<>(data), new ArrayList<>(highlight),
                List.of(), new ArrayList<>(sorted), message, line,
                nodes, edges);
    }

    /**
     * Formats a weight/time: integers without a decimal point,
     * other values with up to 4 decimals, trailing zeros stripped.
     */
    private String fmt(double value) {
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
