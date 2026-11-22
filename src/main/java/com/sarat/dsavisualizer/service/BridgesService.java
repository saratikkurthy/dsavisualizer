package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Tarjan bridges and articulation points (Phase 21): one
 * recursive DFS (neighbours ascending) records discovery times
 * {@code disc} and low-link values {@code low}; an edge
 * (u, v) is a bridge when low[v] &gt; disc[u] on the return from
 * the child v, and u (non-root) is an articulation point when
 * some child v has low[v] &gt;= disc[u] (the root needs more
 * than one DFS-tree child).
 *
 * <p>Steps: one per node visited (line 1), one per back edge
 * (line 8 — each non-tree edge is counted once, at its deeper
 * end, where disc[v] &lt; disc[u]), one per return from a child
 * (line 6), plus the final Step (line 0). Visit/back-edge/return
 * steps carry {@code data} = disc array and {@code aux} = low
 * array (node-indexed, -1 = undiscovered/unknown); every step
 * carries {@code mstEdges} = bridges found so far (in discovery
 * order, as [parent, child] pairs — the Prim/Kruskal precedent,
 * so the bridges never flicker out of view on a visit step);
 * the final Step carries {@code sorted} = articulation points
 * ascending. Steps carry the full graph
 * ({@code nodes}/{@code edges}, circular layout, the GraphService
 * positions). Demo: 7 + 2 + 6 + 1 = 16 steps.</p>
 *
 * <p>The controller validates the input (2..12 edges, node ids
 * 0..20, a given start must be a node in the graph); this method
 * guards against bad input anyway with a single "empty" Step,
 * and defaults a missing (or absent) start to the smallest node.</p>
 */
@Service
public class BridgesService {

    private static final int LINE_VISIT = 1;
    private static final int LINE_RETURN = 6;
    private static final int LINE_BACK_EDGE = 8;
    private static final int LINE_FINAL = 0;

    /**
     * Runs the bridges/articulation DFS on the undirected edge
     * list ({@code [u, v]} pairs) from {@code start}.
     */
    public List<Step> bridges(List<List<Integer>> graphEdges,
                              Integer start) {
        List<Step> steps = new ArrayList<>();
        List<int[]> edges = parseEdges(graphEdges);
        if (edges.size() < 2) {
            steps.add(bridgeStep(List.of(), List.of(),
                    "A graph with at least 2 edges is needed.",
                    LINE_FINAL, List.of(), List.of(), List.of(),
                    List.of(), List.of()));
            return steps;
        }
        Set<Integer> nodeSet = new TreeSet<>();
        for (int[] e : edges) {
            nodeSet.add(e[0]);
            nodeSet.add(e[1]);
        }
        List<Integer> nodes = new ArrayList<>(nodeSet);
        int root = start != null && nodeSet.contains(start)
                ? start : nodes.get(0);
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        List<EdgeDto> edgeDtos = snapshotEdges(edges);

        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int node : nodes) {
            adj.put(node, new ArrayList<>());
        }
        for (int[] e : edges) {
            adj.get(e[0]).add(e[1]);
            adj.get(e[1]).add(e[0]);
        }
        for (List<Integer> neighbours : adj.values()) {
            Collections.sort(neighbours);
        }

        int maxNode = nodes.get(nodes.size() - 1);
        Dfs dfs = new Dfs(adj, maxNode, nodeDtos, edgeDtos, steps);
        dfs.run(root);
        for (int node : nodes) {
            if (dfs.disc[node] == -1) {
                dfs.run(node);
            }
        }
        steps.add(bridgeStep(List.of(), dfs.sortedArticulation(),
                dfs.finalMessage(), LINE_FINAL, nodeDtos, edgeDtos,
                dfs.discList(), dfs.lowList(), dfs.bridgesSoFar()));
        return steps;
    }

    /** The DFS state: disc/low arrays, the trace, bridges, articulation. */
    private static class Dfs {
        final Map<Integer, List<Integer>> adj;
        final int[] disc;
        final int[] low;
        final List<NodeDto> nodeDtos;
        final List<EdgeDto> edgeDtos;
        final List<Step> steps;
        final List<List<Integer>> bridges = new ArrayList<>();
        final Set<Integer> articulation = new HashSet<>();
        int time = 0;

        Dfs(Map<Integer, List<Integer>> adj, int maxNode,
            List<NodeDto> nodeDtos, List<EdgeDto> edgeDtos,
            List<Step> steps) {
            this.adj = adj;
            this.disc = new int[maxNode + 1];
            this.low = new int[maxNode + 1];
            for (int i = 0; i <= maxNode; i++) {
                disc[i] = -1;
                low[i] = -1;
            }
            this.nodeDtos = nodeDtos;
            this.edgeDtos = edgeDtos;
            this.steps = steps;
        }

        /** DFS from one root (parent -1), then the root rule. */
        void run(int root) {
            dfs(root, -1);
        }

        /** Recursive DFS; appends visit, back-edge and return steps. */
        private void dfs(int u, int parent) {
            disc[u] = low[u] = time++;
            steps.add(bridgeStep(List.of(u), List.of(),
                    "Visit node " + u + " (disc=" + disc[u] + ").",
                    LINE_VISIT, nodeDtos, edgeDtos,
                    discList(), lowList(), bridgesSoFar()));
            int children = 0;
            for (int v : adj.getOrDefault(u, List.of())) {
                if (disc[v] == -1) {
                    children++;
                    dfs(v, u);
                    int oldLow = low[u];
                    low[u] = Math.min(low[u], low[v]);
                    boolean isBridge = low[v] > disc[u];
                    if (isBridge) {
                        bridges.add(List.of(u, v));
                    }
                    boolean marksArticulation = parent != -1
                            && low[v] >= disc[u];
                    if (marksArticulation) {
                        articulation.add(u);
                    }
                    StringBuilder message = new StringBuilder();
                    message.append("Return from ").append(v)
                            .append(" to ").append(u)
                            .append(": low[").append(u).append("] = min(")
                            .append(oldLow).append(", ").append(low[v])
                            .append(") = ").append(low[u])
                            .append("; edge ").append(u).append("\u2013")
                            .append(v)
                            .append(isBridge ? " is a bridge"
                                    : " is not a bridge");
                    if (marksArticulation) {
                        message.append("; ").append(u)
                                .append(" is an articulation point");
                    }
                    message.append(".");
                    steps.add(bridgeStep(List.of(v, u), List.of(),
                            message.toString(), LINE_RETURN, nodeDtos,
                            edgeDtos, discList(), lowList(),
                            bridgesSoFar()));
                } else if (v != parent && disc[v] < disc[u]) {
                    // Back edge to an ancestor, counted once at
                    // the deeper end.
                    int oldLow = low[u];
                    low[u] = Math.min(low[u], disc[v]);
                    steps.add(bridgeStep(List.of(u, v), List.of(),
                            "Back edge " + u + "\u2013" + v + ": low[" + u
                                    + "] = min(" + oldLow + ", " + disc[v]
                                    + ") = " + low[u] + ".",
                            LINE_BACK_EDGE, nodeDtos, edgeDtos,
                            discList(), lowList(), bridgesSoFar()));
                }
            }
            if (parent == -1 && children > 1) {
                articulation.add(u);
            }
        }

        /** Disc array as a list (node-indexed, -1 = undiscovered). */
        List<Integer> discList() {
            List<Integer> out = new ArrayList<>();
            for (int d : disc) {
                out.add(d);
            }
            return out;
        }

        /** Low array as a list (node-indexed, -1 = unknown). */
        List<Integer> lowList() {
            List<Integer> out = new ArrayList<>();
            for (int l : low) {
                out.add(l);
            }
            return out;
        }

        /** Bridges found so far, in discovery order. */
        List<List<Integer>> bridgesSoFar() {
            List<List<Integer>> out = new ArrayList<>();
            for (List<Integer> b : bridges) {
                out.add(new ArrayList<>(b));
            }
            return out;
        }

        /** Articulation points, ascending. */
        List<Integer> sortedArticulation() {
            List<Integer> out = new ArrayList<>(articulation);
            Collections.sort(out);
            return out;
        }

        /** Final message: bridges sorted, articulation ascending. */
        String finalMessage() {
            List<List<Integer>> sortedBridges = bridgesSoFar();
            sortedBridges.sort((a, b) -> {
                if (!a.get(0).equals(b.get(0))) {
                    return Integer.compare(a.get(0), b.get(0));
                }
                return Integer.compare(a.get(1), b.get(1));
            });
            StringBuilder message = new StringBuilder();
            if (sortedBridges.isEmpty()) {
                message.append("No bridges. ");
            } else {
                message.append("Bridges: ");
                for (int i = 0; i < sortedBridges.size(); i++) {
                    if (i > 0) {
                        message.append(", ");
                    }
                    message.append("(").append(sortedBridges.get(i).get(0))
                            .append(", ").append(sortedBridges.get(i).get(1))
                            .append(")");
                }
                message.append(". ");
            }
            List<Integer> art = sortedArticulation();
            if (art.isEmpty()) {
                message.append("No articulation points.");
            } else {
                message.append("Articulation points: ");
                for (int i = 0; i < art.size(); i++) {
                    if (i > 0) {
                        message.append(", ");
                    }
                    message.append(art.get(i));
                }
                message.append(".");
            }
            return message.toString();
        }
    }

    /**
     * Valid pairs only: [u, v], no self-loops, each unordered pair
     * once (first appearance order).
     */
    private List<int[]> parseEdges(List<List<Integer>> graphEdges) {
        List<int[]> edges = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (graphEdges == null) {
            return edges;
        }
        for (List<Integer> pair : graphEdges) {
            if (pair == null || pair.size() != 2) {
                continue;
            }
            Integer u = pair.get(0);
            Integer v = pair.get(1);
            if (u == null || v == null || u.equals(v)) {
                continue;
            }
            String key = Math.min(u, v) + "-" + Math.max(u, v);
            if (seen.add(key)) {
                edges.add(new int[]{u, v});
            }
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

    /** Undirected edges as given (deduped), in first-appearance order. */
    private List<EdgeDto> snapshotEdges(List<int[]> edges) {
        List<EdgeDto> dtos = new ArrayList<>();
        for (int[] e : edges) {
            dtos.add(new EdgeDto(e[0], e[1]));
        }
        return dtos;
    }

    /** One bridges Step: disc in data, low in aux, bridges in mstEdges. */
    private static Step bridgeStep(List<Integer> highlight,
                                   List<Integer> sorted, String message,
                                   int line, List<NodeDto> nodes,
                                   List<EdgeDto> edges, List<Integer> disc,
                                   List<Integer> low,
                                   List<List<Integer>> bridges) {
        Step step = new Step(new ArrayList<>(disc), highlight,
                List.of(), sorted, message, line, nodes, edges);
        step.setAux(new ArrayList<>(low));
        step.setMstEdges(new ArrayList<>(bridges));
        return step;
    }
}
