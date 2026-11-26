package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Johnson's all-pairs shortest paths (Phase 31, Graphs v3):
 * Bellman-Ford from an added source q (0-edges to every node)
 * gives potentials h; edges are reweighted to w' = w + h[u] -
 * h[v] (all &gt;= 0 when there is no negative cycle); then
 * Dijkstra runs once per source on the reweighted graph, and
 * the true distances are d'[v] - h[s] + h[v].
 *
 * <p>The graph is DIRECTED (unlike Floyd-Warshall in this
 * codebase, which treats edges as two-way): a negative edge
 * is only a negative cycle if a directed cycle sums
 * negative. Two-element edges default to weight 1, the
 * Floyd-Warshall parse rule.</p>
 *
 * <p>Arrays are indexed by position in the sorted node list
 * (for the demo the node ids are 0..3, so positions equal
 * ids). Unreachable distances are null in {@code data} — the
 * Floyd-Warshall / Dijkstra convention (null = infinity).
 * Step shape: build (line 2, {@code data} = h initial, all
 * zeros) + one Step per Bellman-Ford relaxation round (line
 * 3, {@code data} = h after that round, {@code highlight} =
 * the nodes whose h changed; rounds stop after the first
 * round that changes nothing — the sim's rule) + one
 * reweight step (line 2, {@code data} = h, {@code aux} =
 * the reweighted edge weights in input order; the
 * {@code edges} snapshot keeps the ORIGINAL weights
 * throughout — the visualizer's "w &rarr; w'" line reads
 * the originals from the step's edges and the reweighted
 * weights from {@code aux}, so mutating the edges would
 * make it read "6 &rarr; 6") + one Step per Dijkstra node visit
 * per source (line 3, {@code data} = the reweighted
 * distances from that source after relaxing from the
 * visited node, {@code sorted} = the settled nodes so far)
 * + final (line 0, {@code data} = the flattened all-pairs
 * TRUE distance matrix, row-major, {@code sorted} = all
 * node ids). A negative cycle ends with a final step
 * reading "Negative cycle detected — no shortest path
 * exists." — the Floyd-Warshall wording.</p>
 *
 * <p>Demo (directed, negative edges, no negative cycle), 4
 * nodes, edges in input order: 0-&gt;1:1, 0-&gt;2:4,
 * 1-&gt;2:-2, 1-&gt;3:2, 2-&gt;3:3, 3-&gt;0:1.
 * Bellman-Ford from q: h = [0, 0, -2, 0]. Reweighted
 * edges: 0-&gt;1:1, 0-&gt;2:6, 1-&gt;2:0, 1-&gt;3:2,
 * 2-&gt;3:1, 3-&gt;0:1. True all-pairs matrix (rows =
 * source): from 0: [0, 1, -1, 2]; from 1: [2, 0, -2, 1];
 * from 2: [4, 5, 0, 3]; from 3: [1, 2, 0, 0]. Trace: 1
 * build + 2 Bellman-Ford rounds + 1 reweight + 16 Dijkstra
 * visits + 1 final = 21 steps.</p>
 *
 * <p>The controller validates the input (the
 * Floyd-Warshall validator: non-empty, each edge [from, to]
 * or [from, to, weight], node ids 0..9); this method guards
 * against bad input anyway with a single "empty" Step.</p>
 */
@Service
public class JohnsonService {

    private static final int LINE_BUILD = 2;
    private static final int LINE_STEP = 3;
    private static final int LINE_FINAL = 0;

    /** Johnson's all-pairs shortest paths on the directed edge list. */
    public List<Step> johnson(List<List<Integer>> graphEdges) {
        List<Step> steps = new ArrayList<>();
        List<int[]> edges = parseEdges(graphEdges);
        if (edges.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Graph is empty.", LINE_FINAL));
            return steps;
        }
        Set<Integer> nodeSet = new TreeSet<>();
        for (int[] e : edges) {
            nodeSet.add(e[0]);
            nodeSet.add(e[1]);
        }
        List<Integer> nodes = new ArrayList<>(nodeSet);
        int n = nodes.size();
        Map<Integer, Integer> pos = new HashMap<>();
        for (int i = 0; i < n; i++) {
            pos.put(nodes.get(i), i);
        }
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        List<EdgeDto> edgeDtos = snapshotEdges(edges);
        List<Integer> h = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            h.add(0);
        }
        steps.add(new Step(new ArrayList<>(h), List.of(), List.of(),
                List.of(),
                "Johnson's algorithm: add a source node q with "
                        + "0-edges to all nodes; h = " + h + ".",
                LINE_BUILD, nodeDtos, edgeDtos));

        // Bellman-Ford from q: h starts at 0 for every node
        // (q's 0-edges), so n-1 rounds over the real edges
        // suffice; stop after the first round that changes
        // nothing (the sim's rule).
        boolean changed = true;
        for (int round = 0; round < n - 1 && changed; round++) {
            changed = false;
            List<Integer> changedNodes = new ArrayList<>();
            for (int[] e : edges) {
                int u = pos.get(e[0]);
                int v = pos.get(e[1]);
                int w = e[2];
                if (h.get(u) + w < h.get(v)) {
                    h.set(v, h.get(u) + w);
                    changed = true;
                    if (!changedNodes.contains(nodes.get(v))) {
                        changedNodes.add(nodes.get(v));
                    }
                }
            }
            steps.add(new Step(new ArrayList<>(h),
                    new ArrayList<>(changedNodes), List.of(), List.of(),
                    "Bellman-Ford round " + (round + 1) + ": h = " + h
                            + (changedNodes.isEmpty()
                                    ? " (no change — distances are stable)."
                                    : "."),
                    LINE_STEP, nodeDtos, edgeDtos));
        }
        // Negative-cycle detection pass (the sim's rule).
        for (int[] e : edges) {
            int u = pos.get(e[0]);
            int v = pos.get(e[1]);
            if (h.get(u) + e[2] < h.get(v)) {
                steps.add(new Step(new ArrayList<>(h), List.of(),
                        List.of(), List.of(),
                        "Negative cycle detected — no shortest path exists.",
                        LINE_FINAL, nodeDtos, edgeDtos));
                return steps;
            }
        }

        // Reweight: w' = w + h[u] - h[v] (all >= 0 now).
        List<Integer> reweighted = new ArrayList<>();
        List<int[]> rwEdges = new ArrayList<>();
        for (int[] e : edges) {
            int wPrime = e[2] + h.get(pos.get(e[0])) - h.get(pos.get(e[1]));
            reweighted.add(wPrime);
            rwEdges.add(new int[]{e[0], e[1], wPrime});
        }
        // The reweighted edges drive Dijkstra, but the step
        // snapshots keep the ORIGINAL edge weights (see the
        // class javadoc): the visualizer reads w from the
        // step's edges and w' from aux.
        Step reweight = new Step(new ArrayList<>(h), List.of(),
                List.of(), List.of(),
                "Reweight edges with h = " + h
                        + ": all weights are now non-negative.",
                LINE_BUILD, nodeDtos, edgeDtos);
        reweight.setAux(new ArrayList<>(reweighted));
        steps.add(reweight);

        // Dijkstra from each source on the reweighted graph
        // (neighbours in input-edge order; ties in the min
        // pick go to the smallest node index — the sim's rule).
        // data = reweighted distances (null = unreachable);
        // the true all-pairs matrix is built for the final step.
        List<List<Integer>> allPairs = new ArrayList<>();
        for (int s = 0; s < n; s++) {
            List<Integer> dist = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                dist.add(null);
            }
            dist.set(s, 0);
            List<Boolean> settled = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                settled.add(false);
            }
            List<Integer> settledOrder = new ArrayList<>();
            for (int iter = 0; iter < n; iter++) {
                int u = -1;
                int best = Integer.MAX_VALUE;
                for (int i = 0; i < n; i++) {
                    if (!settled.get(i) && dist.get(i) != null
                            && dist.get(i) < best) {
                        best = dist.get(i);
                        u = i;
                    }
                }
                if (u < 0) {
                    // Nothing else reachable — the existing
                    // Dijkstra service stops here too (it does
                    // not settle unreachable nodes).
                    break;
                }
                // Relax u's neighbours in input-edge order.
                for (int[] e : rwEdges) {
                    if (pos.get(e[0]) != u) {
                        continue;
                    }
                    int v = pos.get(e[1]);
                    int nd = dist.get(u) + e[2];
                    if (dist.get(v) == null || nd < dist.get(v)) {
                        dist.set(v, nd);
                    }
                }
                settled.set(u, true);
                settledOrder.add(nodes.get(u));
                steps.add(new Step(new ArrayList<>(dist),
                        List.of(nodes.get(u)), List.of(),
                        new ArrayList<>(settledOrder),
                        "Dijkstra from " + nodes.get(s) + ": settle node "
                                + nodes.get(u) + " with distance "
                                + dist.get(u) + ".",
                        LINE_STEP, nodeDtos, edgeDtos));
            }
            // True distances: d[v] - h[s] + h[v].
            List<Integer> trueRow = new ArrayList<>();
            for (int v = 0; v < n; v++) {
                if (dist.get(v) == null) {
                    trueRow.add(null);
                } else {
                    trueRow.add(dist.get(v) - h.get(s) + h.get(v));
                }
            }
            allPairs.add(trueRow);
        }
        List<Integer> flat = new ArrayList<>();
        for (List<Integer> row : allPairs) {
            flat.addAll(row);
        }
        steps.add(new Step(flat, List.of(), List.of(),
                new ArrayList<>(nodes),
                "All-pairs shortest paths: " + allPairs + ".",
                LINE_FINAL, nodeDtos, edgeDtos));
        return steps;
    }

    /**
     * Valid edges in input order: [from, to, weight], weight
     * defaulting to 1 for 2-element edges (the Floyd-Warshall
     * parse rule). Self-loops are kept here — a self-loop with
     * negative weight IS a negative cycle, and Bellman-Ford
     * must see it.
     */
    private List<int[]> parseEdges(List<List<Integer>> graphEdges) {
        List<int[]> edges = new ArrayList<>();
        if (graphEdges == null) {
            return edges;
        }
        for (List<Integer> pair : graphEdges) {
            if (pair == null || (pair.size() != 2 && pair.size() != 3)) {
                continue;
            }
            Integer u = pair.get(0);
            Integer v = pair.get(1);
            if (u == null || v == null) {
                continue;
            }
            int w = pair.size() == 3 && pair.get(2) != null
                    ? pair.get(2) : 1;
            edges.add(new int[]{u, v, w});
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

    /** Directed edges with weight, in input order. */
    private List<EdgeDto> snapshotEdges(List<int[]> edges) {
        List<EdgeDto> dtos = new ArrayList<>();
        for (int[] e : edges) {
            dtos.add(new EdgeDto(e[0], e[1], e[2]));
        }
        return dtos;
    }
}
