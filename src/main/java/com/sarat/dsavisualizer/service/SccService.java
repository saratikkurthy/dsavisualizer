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
 * Kosaraju's strongly connected components (Phase 19), two passes
 * over a directed graph.
 *
 * <p>Pass 1: recursive DFS from the lowest unvisited node,
 * neighbours in ascending order; one Step per node finished
 * (line 2), {@code highlight} = [the node just finished],
 * {@code sorted} = nodes finished so far. Pass 2: DFS on the
 * reversed graph in reverse finish order; one Step per SCC found
 * (line 3), {@code highlight} = the whole SCC (node ids ascending),
 * {@code sorted} = nodes in SCCs found so far. Then the final Step
 * (line 0) with the component list.</p>
 *
 * <p>Steps carry the full graph ({@code nodes}/{@code edges},
 * circular layout, the GraphService positions); {@code data} is
 * empty. Unlike GraphService's undirected snapshot, edges here
 * keep their direction (from → to as given, exact duplicates
 * removed) — a reversed pair is a different edge. Self-loops are
 * skipped when building the graph, the topoSort precedent: they
 * never change which SCC a node belongs to, and a lone self-loop
 * node is dropped the same way topoSort drops it.</p>
 *
 * <p>The controller validates the input (2..12 edges, node ids
 * 0..9); this method guards against bad input anyway with a
 * single "empty" Step.</p>
 */
@Service
public class SccService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_SWAP = 3;
    private static final int LINE_FINAL = 0;

    /**
     * Runs Kosaraju on the directed edge list ({@code [u, v]}
     * pairs, a 3rd element — a weight — is ignored).
     */
    public List<Step> scc(List<List<Integer>> graphEdges) {
        List<Step> steps = new ArrayList<>();
        Map<Integer, List<Integer>> adj =
                buildDirectedAdjacency(graphEdges);
        Map<Integer, List<Integer>> radj =
                buildReversedAdjacency(graphEdges);
        List<Integer> nodes = new ArrayList<>(new TreeSet<>(adj.keySet()));
        if (nodes.isEmpty()) {
            steps.add(graphStep(List.of(), List.of(),
                    "Graph is empty.", LINE_FINAL, List.of(), List.of()));
            return steps;
        }
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        List<EdgeDto> edgeDtos = snapshotDirectedEdges(graphEdges);

        // Pass 1: DFS finish order (lowest unvisited node first).
        Set<Integer> visited = new HashSet<>();
        List<Integer> finishOrder = new ArrayList<>();
        for (int node : nodes) {
            if (!visited.contains(node)) {
                dfsFinish(node, adj, visited, finishOrder);
            }
        }
        List<Integer> finishedSoFar = new ArrayList<>();
        for (int i = 0; i < finishOrder.size(); i++) {
            int node = finishOrder.get(i);
            finishedSoFar.add(node);
            steps.add(graphStep(List.of(node),
                    new ArrayList<>(finishedSoFar),
                    "Finish " + node + " (order " + (i + 1) + ").",
                    LINE_COMPARE, nodeDtos, edgeDtos));
        }

        // Pass 2: DFS on the reversed graph, reverse finish order.
        Set<Integer> assigned = new HashSet<>();
        List<List<Integer>> components = new ArrayList<>();
        List<Integer> inComponentsSoFar = new ArrayList<>();
        for (int i = finishOrder.size() - 1; i >= 0; i--) {
            int node = finishOrder.get(i);
            if (assigned.contains(node)) {
                continue;
            }
            List<Integer> component = new ArrayList<>();
            dfsCollect(node, radj, assigned, component);
            Collections.sort(component);
            components.add(component);
            inComponentsSoFar.addAll(component);
            steps.add(graphStep(new ArrayList<>(component),
                    new ArrayList<>(inComponentsSoFar),
                    "SCC " + components.size() + ": " + component + ".",
                    LINE_SWAP, nodeDtos, edgeDtos));
        }

        StringBuilder finalMessage = new StringBuilder();
        finalMessage.append(components.size())
                .append(" strongly connected components: ");
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) {
                finalMessage.append(", ");
            }
            finalMessage.append(components.get(i));
        }
        finalMessage.append(".");
        steps.add(graphStep(List.of(), new ArrayList<>(nodes),
                finalMessage.toString(), LINE_FINAL, nodeDtos, edgeDtos));
        return steps;
    }

    /** Recursive DFS that appends each node when it finishes. */
    private void dfsFinish(int node, Map<Integer, List<Integer>> adj,
                           Set<Integer> visited,
                           List<Integer> finishOrder) {
        visited.add(node);
        for (int nb : adj.getOrDefault(node, List.of())) {
            if (!visited.contains(nb)) {
                dfsFinish(nb, adj, visited, finishOrder);
            }
        }
        finishOrder.add(node);
    }

    /** Recursive DFS on the reversed graph, collecting one SCC. */
    private void dfsCollect(int node, Map<Integer, List<Integer>> radj,
                            Set<Integer> assigned,
                            List<Integer> component) {
        assigned.add(node);
        component.add(node);
        for (int nb : radj.getOrDefault(node, List.of())) {
            if (!assigned.contains(nb)) {
                dfsCollect(nb, radj, assigned, component);
            }
        }
    }

    /**
     * Directed adjacency (neighbours ascending). Every endpoint
     * appears as a key, even a target-only node. 2-element edges
     * and 3-element edges (weight ignored) are accepted; bad pairs
     * and self-loops are skipped, the topoSort precedent.
     */
    private Map<Integer, List<Integer>> buildDirectedAdjacency(
            List<List<Integer>> graphEdges) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        if (graphEdges == null) {
            return adj;
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
            adj.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
            adj.computeIfAbsent(v, k -> new ArrayList<>());
        }
        for (List<Integer> neighbours : adj.values()) {
            Collections.sort(neighbours);
        }
        return adj;
    }

    /** Reversed directed adjacency (neighbours ascending). */
    private Map<Integer, List<Integer>> buildReversedAdjacency(
            List<List<Integer>> graphEdges) {
        Map<Integer, List<Integer>> radj = new HashMap<>();
        if (graphEdges == null) {
            return radj;
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
            radj.computeIfAbsent(v, k -> new ArrayList<>()).add(u);
            radj.computeIfAbsent(u, k -> new ArrayList<>());
        }
        for (List<Integer> neighbours : radj.values()) {
            Collections.sort(neighbours);
        }
        return radj;
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

    /**
     * Directed edges as given (from → to, exact duplicates
     * removed). Unlike GraphService's undirected snapshot, a
     * reversed pair is kept as its own edge.
     */
    private List<EdgeDto> snapshotDirectedEdges(
            List<List<Integer>> graphEdges) {
        List<EdgeDto> edges = new ArrayList<>();
        Set<String> seen = new HashSet<>();
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
            if (seen.add(u + "->" + v)) {
                edges.add(new EdgeDto(u, v));
            }
        }
        return edges;
    }

    /** One graph Step: empty data, highlight = node ids, sorted = visited. */
    private Step graphStep(List<Integer> highlight, List<Integer> sorted,
                           String message, int line,
                           List<NodeDto> nodes, List<EdgeDto> edges) {
        return new Step(List.of(), highlight, List.of(), sorted,
                message, line, nodes, edges);
    }
}
