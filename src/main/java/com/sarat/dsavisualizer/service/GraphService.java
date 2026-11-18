package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Undirected graph BFS/DFS that records each visit/discovery as a Step.
 *
 * Steps carry the full graph ({@code nodes}/{@code edges}, circular
 * layout), {@code data} is empty, {@code highlight} holds node ids
 * being visited/discovered, {@code sorted} holds visited node ids.
 * Line numbers: visit/discover = 2, final = 0.
 */
@Service
public class GraphService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_FINAL = 0;

    /**
     * Breadth-first search from {@code start}. Neighbours are
     * visited in ascending order. One Step per node visited and
     * per neighbour discovered, then a final Step with the order.
     */
    public List<Step> bfs(List<List<Integer>> graphEdges, Integer start) {
        List<Step> steps = new ArrayList<>();
        Map<Integer, List<Integer>> adj = buildAdjacency(graphEdges);
        List<Integer> nodes = nodeIds(adj);
        if (graphEdges == null || graphEdges.isEmpty() || nodes.isEmpty()) {
            steps.add(graphStep(List.of(), List.of(), "Graph is empty.",
                    LINE_FINAL, List.of(), List.of()));
            return steps;
        }
        if (start == null || !nodes.contains(start)) {
            steps.add(graphStep(List.of(), List.of(), "Invalid start node.",
                    LINE_FINAL, snapshotNodes(nodes),
                    snapshotEdges(graphEdges)));
            return steps;
        }
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        List<EdgeDto> edgeDtos = snapshotEdges(graphEdges);
        Deque<Integer> queue = new ArrayDeque<>();
        Set<Integer> visited = new LinkedHashSet<>();
        List<Integer> visitOrder = new ArrayList<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            int node = queue.poll();
            visitOrder.add(node);
            steps.add(graphStep(List.of(node), new ArrayList<>(visitOrder),
                    "Visit node " + node + ". Queue: " + queue + ".",
                    LINE_COMPARE, nodeDtos, edgeDtos));
            for (int nb : adj.getOrDefault(node, List.of())) {
                if (!visited.contains(nb)) {
                    visited.add(nb);
                    queue.add(nb);
                    steps.add(graphStep(List.of(nb),
                            new ArrayList<>(visitOrder),
                            "Discover node " + nb + " from " + node + ".",
                            LINE_COMPARE, nodeDtos, edgeDtos));
                }
            }
        }
        steps.add(graphStep(List.of(), new ArrayList<>(visitOrder),
                "BFS order: " + visitOrder + ".", LINE_FINAL,
                nodeDtos, edgeDtos));
        return steps;
    }

    /**
     * Iterative depth-first search from {@code start}. Neighbours
     * are pushed in reverse ascending order so the smallest is
     * popped first. One Step per node visited and per neighbour
     * discovered, then a final Step with the order.
     */
    public List<Step> dfs(List<List<Integer>> graphEdges, Integer start) {
        List<Step> steps = new ArrayList<>();
        Map<Integer, List<Integer>> adj = buildAdjacency(graphEdges);
        List<Integer> nodes = nodeIds(adj);
        if (graphEdges == null || graphEdges.isEmpty() || nodes.isEmpty()) {
            steps.add(graphStep(List.of(), List.of(), "Graph is empty.",
                    LINE_FINAL, List.of(), List.of()));
            return steps;
        }
        if (start == null || !nodes.contains(start)) {
            steps.add(graphStep(List.of(), List.of(), "Invalid start node.",
                    LINE_FINAL, snapshotNodes(nodes),
                    snapshotEdges(graphEdges)));
            return steps;
        }
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        List<EdgeDto> edgeDtos = snapshotEdges(graphEdges);
        Deque<Integer> stack = new ArrayDeque<>();
        Set<Integer> visited = new LinkedHashSet<>();
        List<Integer> visitOrder = new ArrayList<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            int node = stack.pop();
            if (visited.contains(node)) {
                continue;
            }
            visited.add(node);
            visitOrder.add(node);
            steps.add(graphStep(List.of(node), new ArrayList<>(visitOrder),
                    "Visit node " + node + ". Stack: " + stack + ".",
                    LINE_COMPARE, nodeDtos, edgeDtos));
            List<Integer> neighbours = adj.getOrDefault(node, List.of());
            for (int k = neighbours.size() - 1; k >= 0; k--) {
                int nb = neighbours.get(k);
                if (!visited.contains(nb)) {
                    stack.push(nb);
                    steps.add(graphStep(List.of(nb),
                            new ArrayList<>(visitOrder),
                            "Discover node " + nb + " from " + node + ".",
                            LINE_COMPARE, nodeDtos, edgeDtos));
                }
            }
        }
        steps.add(graphStep(List.of(), new ArrayList<>(visitOrder),
                "DFS order: " + visitOrder + ".", LINE_FINAL,
                nodeDtos, edgeDtos));
        return steps;
    }

    /** Adjacency map from the edge list (undirected, neighbours ascending). */
    private Map<Integer, List<Integer>> buildAdjacency(
            List<List<Integer>> graphEdges) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        if (graphEdges == null) {
            return adj;
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
            adj.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
            adj.computeIfAbsent(v, k -> new ArrayList<>()).add(u);
        }
        for (List<Integer> neighbours : adj.values()) {
            Collections.sort(neighbours);
        }
        return adj;
    }

    /** Sorted node ids (every endpoint of at least one edge). */
    private List<Integer> nodeIds(Map<Integer, List<Integer>> adj) {
        return new ArrayList<>(new TreeSet<>(adj.keySet()));
    }

    /** Circular layout: node i at angle 2*pi*i/n - pi/2. */
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

    /** Edges as {from, to} with from < to, deduplicated. */
    private List<EdgeDto> snapshotEdges(List<List<Integer>> graphEdges) {
        List<EdgeDto> edges = new ArrayList<>();
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
            int from = Math.min(u, v);
            int to = Math.max(u, v);
            String key = from + "-" + to;
            if (seen.add(key)) {
                edges.add(new EdgeDto(from, to));
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
