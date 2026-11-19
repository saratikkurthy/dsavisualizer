package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

/**
 * Undirected graph BFS/DFS/Dijkstra/Prim's/Kruskal's, Bellman-Ford
 * (negative weights allowed) and topological sort (directed,
 * Kahn's algorithm) that record each visit/discovery as a Step.
 *
 * Steps carry the full graph ({@code nodes}/{@code edges}, circular
 * layout), {@code data} is empty for BFS/DFS and Kruskal's, the
 * distances array for Dijkstra and Bellman-Ford, the key array for
 * Prim's, and the in-degree array for topological sort,
 * {@code highlight} holds node ids being visited/discovered/
 * settled, {@code sorted} holds visited/settled node ids. Line
 * numbers: visit/discover/settle = 2, relax/swap = 3, final = 0.
 *
 * Edges may be 2-element {@code [from, to]} (weight defaults to 1)
 * or 3-element {@code [from, to, weight]} (Phase 7, Dijkstra).
 * BFS/DFS/topological sort ignore weights.
 */
@Service
public class GraphService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_SWAP = 3;
    private static final int LINE_FINAL = 0;

    /** One weighted neighbour: the node id and the edge weight to it. */
    private static class Neighbor {
        final int id;
        final int weight;

        Neighbor(int id, int weight) {
            this.id = id;
            this.weight = weight;
        }
    }

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

    /**
     * Dijkstra's shortest path from {@code start}. Edge weights
     * default to 1 for 2-element edges. One Step per relaxation
     * that improves a neighbour (emitted first, line 3) and one
     * Step per node settled (line 2), then a final Step with the
     * distances. {@code data} is the distances array (index = node
     * id, JSON null = infinity / not yet reached); {@code sorted}
     * holds settled node ids.
     */
    public List<Step> dijkstra(List<List<Integer>> graphEdges,
                               Integer start) {
        List<Step> steps = new ArrayList<>();
        Map<Integer, List<Neighbor>> wAdj =
                buildWeightedAdjacency(graphEdges);
        List<Integer> nodes = nodeIds(wAdj);
        if (graphEdges == null || graphEdges.isEmpty() || nodes.isEmpty()) {
            steps.add(dijkstraStep(List.of(), List.of(), List.of(),
                    "Graph is empty.", LINE_FINAL, List.of(), List.of()));
            return steps;
        }
        if (start == null || !nodes.contains(start)) {
            steps.add(dijkstraStep(List.of(), List.of(), List.of(),
                    "Invalid start node.", LINE_FINAL,
                    snapshotNodes(nodes), snapshotEdges(graphEdges)));
            return steps;
        }
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        List<EdgeDto> edgeDtos = snapshotEdges(graphEdges);
        int maxId = nodes.get(nodes.size() - 1);
        List<Integer> dist = new ArrayList<>();
        for (int i = 0; i <= maxId; i++) {
            dist.add(null);
        }
        dist.set(start, 0);
        Set<Integer> settled = new HashSet<>();
        List<Integer> settledOrder = new ArrayList<>();
        while (true) {
            // Pick the unsettled node with the smallest known distance.
            Integer u = null;
            int best = Integer.MAX_VALUE;
            for (int node : nodes) {
                if (!settled.contains(node) && dist.get(node) != null
                        && dist.get(node) < best) {
                    best = dist.get(node);
                    u = node;
                }
            }
            if (u == null) {
                break; // nothing else reachable
            }
            // Relax u's neighbours (ascending), one Step per improvement.
            for (Neighbor nb : wAdj.getOrDefault(u, List.of())) {
                int nd = dist.get(u) + nb.weight;
                Integer old = dist.get(nb.id);
                if (old == null || nd < old) {
                    dist.set(nb.id, nd);
                    steps.add(dijkstraStep(dist, List.of(nb.id),
                            new ArrayList<>(settledOrder),
                            "Relax edge " + u + " -> " + nb.id
                                    + ": distance "
                                    + (old == null ? "∞" : old)
                                    + " -> " + nd + ".",
                            LINE_SWAP, nodeDtos, edgeDtos));
                }
            }
            // Settle u.
            settled.add(u);
            settledOrder.add(u);
            steps.add(dijkstraStep(dist, List.of(u),
                    new ArrayList<>(settledOrder),
                    "Settle node " + u + " with distance " + dist.get(u)
                            + ".",
                    LINE_COMPARE, nodeDtos, edgeDtos));
        }
        steps.add(dijkstraStep(dist, List.of(),
                new ArrayList<>(settledOrder),
                "Shortest distances from " + start + ": " + dist + ".",
                LINE_FINAL, nodeDtos, edgeDtos));
        return steps;
    }

    /**
     * Prim's minimum spanning tree from {@code start}. Edge weights
     * default to 1 for 2-element edges.
     *
     * CRITICAL: Prim's keys are EDGE weights — key[v] is the minimum
     * weight of an edge connecting v to the MST — NOT path distances
     * from the start (that would be Dijkstra). One Step per
     * relaxation that improves a neighbour's key (emitted first,
     * line 3) and one Step per node added to the MST (line 2), then
     * a final Step with the total weight. {@code data} is the key
     * array (index = node id, JSON null = infinity);
     * {@code sorted} holds the nodes in the MST; {@code mstEdges}
     * holds the MST edges so far as [parent, child] pairs.
     */
    public List<Step> prim(List<List<Integer>> graphEdges,
                           Integer start) {
        List<Step> steps = new ArrayList<>();
        Map<Integer, List<Neighbor>> wAdj =
                buildWeightedAdjacency(graphEdges);
        List<Integer> nodes = nodeIds(wAdj);
        if (graphEdges == null || graphEdges.isEmpty() || nodes.isEmpty()) {
            steps.add(mstStep(List.of(), List.of(), List.of(),
                    List.of(), "Graph is empty.", LINE_FINAL,
                    List.of(), List.of()));
            return steps;
        }
        if (start == null || !nodes.contains(start)) {
            steps.add(mstStep(List.of(), List.of(), List.of(),
                    List.of(), "Invalid start node.", LINE_FINAL,
                    snapshotNodes(nodes), snapshotEdges(graphEdges)));
            return steps;
        }
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        List<EdgeDto> edgeDtos = snapshotEdges(graphEdges);
        int maxId = nodes.get(nodes.size() - 1);
        List<Integer> key = new ArrayList<>();
        for (int i = 0; i <= maxId; i++) {
            key.add(null);
        }
        key.set(start, 0);
        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> inMst = new HashSet<>();
        List<Integer> mstOrder = new ArrayList<>();
        List<List<Integer>> mstEdges = new ArrayList<>();
        int totalWeight = 0;
        while (true) {
            // Pick the node outside the MST with the smallest key.
            Integer u = null;
            int best = Integer.MAX_VALUE;
            for (int node : nodes) {
                if (!inMst.contains(node) && key.get(node) != null
                        && key.get(node) < best) {
                    best = key.get(node);
                    u = node;
                }
            }
            if (u == null) {
                break; // MST complete, or the rest is unreachable
            }
            // Relax u's neighbours (ascending), one Step per improvement.
            for (Neighbor nb : wAdj.getOrDefault(u, List.of())) {
                if (inMst.contains(nb.id)) {
                    continue;
                }
                Integer old = key.get(nb.id);
                if (old == null || nb.weight < old) {
                    key.set(nb.id, nb.weight);
                    parent.put(nb.id, u);
                    steps.add(mstStep(key, List.of(nb.id),
                            new ArrayList<>(mstOrder),
                            copyMstEdges(mstEdges),
                            "Relax edge " + u + "-" + nb.id + ": key "
                                    + (old == null ? "∞" : old)
                                    + " -> " + nb.weight + ".",
                            LINE_SWAP, nodeDtos, edgeDtos));
                }
            }
            // Add u to the MST.
            inMst.add(u);
            mstOrder.add(u);
            String message;
            if (u.equals(start)) {
                message = "Add node " + u + " to the MST. Total weight: 0.";
            } else {
                int w = key.get(u);
                mstEdges.add(List.of(parent.get(u), u));
                totalWeight += w;
                message = "Add node " + u + " to the MST via edge "
                        + parent.get(u) + "-" + u + " (weight " + w
                        + "). Total weight: " + totalWeight + ".";
            }
            steps.add(mstStep(key, List.of(u),
                    new ArrayList<>(mstOrder), copyMstEdges(mstEdges),
                    message, LINE_COMPARE, nodeDtos, edgeDtos));
        }
        steps.add(mstStep(key, List.of(), new ArrayList<>(mstOrder),
                copyMstEdges(mstEdges),
                "MST total weight: " + totalWeight + ".",
                LINE_FINAL, nodeDtos, edgeDtos));
        return steps;
    }

    /**
     * Kruskal's minimum spanning tree. No start node — edges are
     * sorted by weight ascending (stable: equal weights keep input
     * order) and added unless they form a cycle (union-find). One
     * consider Step per edge (line 2), one add or skip Step per
     * edge (line 3), then a final Step with the total weight.
     * {@code data} is empty; {@code sorted} holds the nodes with at
     * least one MST edge; {@code mstEdges} holds the MST edges so
     * far as [from, to] pairs in input order.
     */
    public List<Step> kruskal(List<List<Integer>> graphEdges) {
        List<Step> steps = new ArrayList<>();
        if (graphEdges == null || graphEdges.isEmpty()) {
            steps.add(mstStep(List.of(), List.of(), List.of(),
                    List.of(), "Graph is empty.", LINE_FINAL,
                    List.of(), List.of()));
            return steps;
        }
        // Collect the valid edges with their weights.
        List<int[]> edges = new ArrayList<>(); // [from, to, weight]
        Set<Integer> nodeSet = new TreeSet<>();
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
            edges.add(new int[]{u, v, w});
            nodeSet.add(u);
            nodeSet.add(v);
        }
        List<Integer> nodes = new ArrayList<>(nodeSet);
        if (nodes.isEmpty()) {
            steps.add(mstStep(List.of(), List.of(), List.of(),
                    List.of(), "Graph is empty.", LINE_FINAL,
                    List.of(), List.of()));
            return steps;
        }
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        List<EdgeDto> edgeDtos = snapshotEdges(graphEdges);
        // Stable sort by weight ascending: equal weights keep input order.
        edges.sort(Comparator.comparingInt(e -> e[2]));
        // Union-find over node ids.
        Map<Integer, Integer> ufParent = new HashMap<>();
        for (int node : nodes) {
            ufParent.put(node, node);
        }
        List<List<Integer>> mstEdges = new ArrayList<>();
        Set<Integer> inMstNodes = new TreeSet<>();
        int totalWeight = 0;
        int n = nodes.size();
        for (int[] edge : edges) {
            if (mstEdges.size() == n - 1) {
                break;
            }
            int u = edge[0];
            int v = edge[1];
            int w = edge[2];
            steps.add(mstStep(List.of(), List.of(u, v),
                    new ArrayList<>(inMstNodes), copyMstEdges(mstEdges),
                    "Consider edge " + u + "-" + v + " (weight " + w + ").",
                    LINE_COMPARE, nodeDtos, edgeDtos));
            if (findRoot(ufParent, u) != findRoot(ufParent, v)) {
                union(ufParent, u, v);
                mstEdges.add(List.of(u, v));
                inMstNodes.add(u);
                inMstNodes.add(v);
                totalWeight += w;
                steps.add(mstStep(List.of(), List.of(u, v),
                        new ArrayList<>(inMstNodes),
                        copyMstEdges(mstEdges),
                        "Add edge " + u + "-" + v + " (weight " + w
                                + ") to the MST. Total weight: "
                                + totalWeight + ".",
                        LINE_SWAP, nodeDtos, edgeDtos));
            } else {
                steps.add(mstStep(List.of(), List.of(u, v),
                        new ArrayList<>(inMstNodes),
                        copyMstEdges(mstEdges),
                        "Skip edge " + u + "-" + v + " (weight " + w
                                + ") — it would form a cycle.",
                        LINE_SWAP, nodeDtos, edgeDtos));
            }
        }
        steps.add(mstStep(List.of(), List.of(),
                new ArrayList<>(inMstNodes), copyMstEdges(mstEdges),
                "MST total weight: " + totalWeight + ".",
                LINE_FINAL, nodeDtos, edgeDtos));
        return steps;
    }

    /**
     * Bellman-Ford shortest path from {@code start}. Weights MAY be
     * negative — that is the point: unlike Dijkstra it does not
     * finalise nodes early, so it finds the shorter path 0→1→2 = 1
     * through the negative edge, where Dijkstra would settle node 2
     * at distance 2. V-1 iterations over all edges in input order:
     * one Step per relaxation (line 2) and one summary Step per
     * iteration that relaxed something (line 2). Early exit: an
     * iteration that relaxes nothing ends the trace with the final
     * Step. A detection pass after the iterations reports a
     * negative cycle. {@code data} is the distances array (index =
     * node id, JSON null = infinity); {@code sorted} stays empty.
     */
    public List<Step> bellmanFord(List<List<Integer>> graphEdges,
                                  Integer start) {
        List<Step> steps = new ArrayList<>();
        // Valid edges in input order: [from, to, weight].
        List<int[]> edges = new ArrayList<>();
        Set<Integer> nodeSet = new TreeSet<>();
        if (graphEdges != null) {
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
                edges.add(new int[]{u, v, w});
                nodeSet.add(u);
                nodeSet.add(v);
            }
        }
        List<Integer> nodes = new ArrayList<>(nodeSet);
        if (graphEdges == null || graphEdges.isEmpty() || nodes.isEmpty()) {
            steps.add(bellmanFordStep(List.of(), List.of(),
                    "Graph is empty.", LINE_FINAL, List.of(), List.of()));
            return steps;
        }
        if (start == null || !nodes.contains(start)) {
            steps.add(bellmanFordStep(List.of(), List.of(),
                    "Invalid start node.", LINE_FINAL,
                    snapshotNodes(nodes), snapshotEdges(graphEdges)));
            return steps;
        }
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        List<EdgeDto> edgeDtos = snapshotEdges(graphEdges);
        int maxId = nodes.get(nodes.size() - 1);
        List<Integer> dist = new ArrayList<>();
        for (int i = 0; i <= maxId; i++) {
            dist.add(null);
        }
        dist.set(start, 0);
        int vCount = nodes.size();
        for (int iter = 1; iter <= vCount - 1; iter++) {
            boolean relaxed = false;
            for (int[] edge : edges) {
                int u = edge[0];
                int v = edge[1];
                int w = edge[2];
                Integer du = dist.get(u);
                if (du == null) {
                    continue;
                }
                int nd = du + w;
                Integer old = dist.get(v);
                if (old == null || nd < old) {
                    dist.set(v, nd);
                    relaxed = true;
                    steps.add(bellmanFordStep(dist, List.of(u, v),
                            "Relax edge " + u + "-" + v + " (weight " + w
                                    + "): dist[" + v + "] "
                                    + (old == null ? "∞" : old)
                                    + " -> " + nd + ".",
                            LINE_COMPARE, nodeDtos, edgeDtos));
                }
            }
            if (!relaxed) {
                // Early exit: distances are stable, so no negative
                // cycle is reachable and the detection pass would
                // find nothing.
                steps.add(bellmanFordStep(dist, List.of(),
                        "Shortest distances from " + start + ": " + dist
                                + ".",
                        LINE_FINAL, nodeDtos, edgeDtos));
                return steps;
            }
            steps.add(bellmanFordStep(dist, List.of(),
                    "Iteration " + iter + " complete. Distances: " + dist
                            + ".",
                    LINE_COMPARE, nodeDtos, edgeDtos));
        }
        // Detection pass: any edge still relaxable means a negative
        // cycle is reachable from the start.
        for (int[] edge : edges) {
            int u = edge[0];
            int v = edge[1];
            int w = edge[2];
            Integer du = dist.get(u);
            if (du == null) {
                continue;
            }
            Integer old = dist.get(v);
            if (old == null || du + w < old) {
                steps.add(bellmanFordStep(dist, List.of(u, v),
                        "Negative cycle detected — no shortest path exists.",
                        LINE_FINAL, nodeDtos, edgeDtos));
                return steps;
            }
        }
        steps.add(bellmanFordStep(dist, List.of(),
                "Shortest distances from " + start + ": " + dist + ".",
                LINE_FINAL, nodeDtos, edgeDtos));
        return steps;
    }

    /**
     * Topological sort (Kahn's algorithm, in-degree based) over the
     * directed edges; weights are ignored. One Step per node output
     * (line 2) and one Step when a neighbour's in-degree drops to 0
     * (line 3), then a final Step with the order — or a cycle
     * message when fewer nodes than V were output. {@code data} is
     * the in-degree array (index = node id); {@code sorted} holds
     * the nodes in the output order so far. Tie-breaking: the ready
     * queue is FIFO, and neighbours are processed in ascending
     * node-id order.
     */
    public List<Step> topoSort(List<List<Integer>> graphEdges) {
        List<Step> steps = new ArrayList<>();
        if (graphEdges == null || graphEdges.isEmpty()) {
            steps.add(topoStep(List.of(), List.of(), List.of(),
                    "Graph is empty.", LINE_FINAL, List.of(), List.of()));
            return steps;
        }
        // Directed adjacency (neighbours ascending) and in-degrees.
        Map<Integer, List<Integer>> adj = new HashMap<>();
        Map<Integer, Integer> inDegree = new HashMap<>();
        Set<Integer> nodeSet = new TreeSet<>();
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
            inDegree.put(v, inDegree.getOrDefault(v, 0) + 1);
            inDegree.putIfAbsent(u, 0);
            nodeSet.add(u);
            nodeSet.add(v);
        }
        List<Integer> nodes = new ArrayList<>(nodeSet);
        if (nodes.isEmpty()) {
            steps.add(topoStep(List.of(), List.of(), List.of(),
                    "Graph is empty.", LINE_FINAL, List.of(), List.of()));
            return steps;
        }
        for (List<Integer> neighbours : adj.values()) {
            Collections.sort(neighbours);
        }
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        List<EdgeDto> edgeDtos = snapshotEdges(graphEdges);
        int maxId = nodes.get(nodes.size() - 1);
        List<Integer> inDeg = new ArrayList<>();
        for (int i = 0; i <= maxId; i++) {
            inDeg.add(inDegree.getOrDefault(i, 0));
        }
        // Ready queue (FIFO), seeded with in-degree-0 nodes ascending.
        Deque<Integer> ready = new ArrayDeque<>();
        for (int node : nodes) {
            if (inDeg.get(node) == 0) {
                ready.add(node);
            }
        }
        List<Integer> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            int node = ready.poll();
            order.add(node);
            steps.add(topoStep(inDeg, List.of(node),
                    new ArrayList<>(order),
                    "Output node " + node + " (in-degree 0).",
                    LINE_COMPARE, nodeDtos, edgeDtos));
            for (int nb : adj.getOrDefault(node, List.of())) {
                int old = inDeg.get(nb);
                inDeg.set(nb, old - 1);
                if (old - 1 == 0) {
                    ready.add(nb);
                    steps.add(topoStep(inDeg, List.of(nb),
                            new ArrayList<>(order),
                            "Node " + nb + " in-degree " + old
                                    + " -> 0 — ready.",
                            LINE_SWAP, nodeDtos, edgeDtos));
                }
            }
        }
        if (order.size() < nodes.size()) {
            steps.add(topoStep(inDeg, List.of(), new ArrayList<>(order),
                    "Cycle detected — no topological order exists.",
                    LINE_FINAL, nodeDtos, edgeDtos));
        } else {
            steps.add(topoStep(inDeg, List.of(), new ArrayList<>(order),
                    "Topological order: " + order + ".",
                    LINE_FINAL, nodeDtos, edgeDtos));
        }
        return steps;
    }

    /**
     * A* pathfinding on a grid. 4-directional movement (up/right/
     * down/left), cost 1 per step, Manhattan distance heuristic
     * (admissible, so the path found is a shortest one). One Step
     * per cell popped from the open set (line 2), then a final Step
     * with the path (line 0) — or "No path found." when the open
     * set empties. {@code table} is the grid snapshot, one state
     * code per cell: 0 = free/unvisited, 1 = wall, 2 = in the open
     * set, 3 = closed/visited, 4 = current cell, 5 = path.
     * {@code highlight} is [row, col] (the cell being processed).
     * Tie-breaking in the open set is deterministic: lowest f,
     * then lowest h, then lowest row, then lowest column.
     */
    public List<Step> aStar(List<List<Integer>> grid,
                            List<Integer> startCell,
                            List<Integer> goalCell) {
        List<Step> steps = new ArrayList<>();
        if (grid == null || grid.isEmpty() || startCell == null
                || startCell.size() != 2 || goalCell == null
                || goalCell.size() != 2) {
            steps.add(aStarStep(List.of(), List.of(),
                    "Invalid grid.", LINE_FINAL));
            return steps;
        }
        int rows = grid.size();
        int cols = grid.get(0).size();
        // State table: 0 = free, 1 = wall (any non-zero cell).
        List<List<Integer>> state = new ArrayList<>();
        for (List<Integer> row : grid) {
            List<Integer> stateRow = new ArrayList<>();
            for (Integer cell : row) {
                stateRow.add(cell != null && cell != 0 ? 1 : 0);
            }
            state.add(stateRow);
        }
        int sr = startCell.get(0);
        int sc = startCell.get(1);
        int gr = goalCell.get(0);
        int gc = goalCell.get(1);
        if (sr < 0 || sr >= rows || sc < 0 || sc >= cols
                || gr < 0 || gr >= rows || gc < 0 || gc >= cols
                || state.get(sr).get(sc) == 1
                || state.get(gr).get(gc) == 1) {
            steps.add(aStarStep(state, List.of(),
                    "Invalid start or goal.", LINE_FINAL));
            return steps;
        }
        int[][] gScore = new int[rows][cols];
        int[][] cameFromR = new int[rows][cols];
        int[][] cameFromC = new int[rows][cols];
        boolean[][] closed = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                gScore[r][c] = Integer.MAX_VALUE;
                cameFromR[r][c] = -1;
                cameFromC[r][c] = -1;
            }
        }
        gScore[sr][sc] = 0;
        PriorityQueue<AStarCell> open = new PriorityQueue<>(
                (a, b) -> a.f() != b.f() ? Integer.compare(a.f(), b.f())
                        : a.h != b.h ? Integer.compare(a.h, b.h)
                        : a.r != b.r ? Integer.compare(a.r, b.r)
                        : Integer.compare(a.c, b.c));
        open.add(new AStarCell(sr, sc, 0, manhattan(sr, sc, gr, gc)));
        state.get(sr).set(sc, 2);
        int[] current = null; // [row, col] of the last popped cell
        // 4-directional neighbours: up, right, down, left.
        int[][] dirs = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};
        while (!open.isEmpty()) {
            AStarCell cell = open.poll();
            if (closed[cell.r][cell.c]) {
                continue; // stale entry (a better path was found)
            }
            // The previously current cell is now closed.
            if (current != null) {
                state.get(current[0]).set(current[1], 3);
            }
            closed[cell.r][cell.c] = true;
            current = new int[]{cell.r, cell.c};
            state.get(cell.r).set(cell.c, 4);
            if (cell.r == gr && cell.c == gc) {
                // Reconstruct the path from the goal back to the start.
                List<int[]> path = new ArrayList<>();
                int pr = gr;
                int pc = gc;
                while (pr != -1) {
                    path.add(new int[]{pr, pc});
                    int nr = cameFromR[pr][pc];
                    int nc = cameFromC[pr][pc];
                    pr = nr;
                    pc = nc;
                }
                Collections.reverse(path);
                for (int[] p : path) {
                    state.get(p[0]).set(p[1], 5);
                }
                StringBuilder pathText = new StringBuilder();
                for (int k = 0; k < path.size(); k++) {
                    if (k > 0) {
                        pathText.append(" -> ");
                    }
                    pathText.append("(").append(path.get(k)[0])
                            .append(",").append(path.get(k)[1]).append(")");
                }
                steps.add(aStarStep(state, List.of(gr, gc),
                        "Path found! Length " + (path.size() - 1)
                                + ". Path: " + pathText + ".",
                        LINE_FINAL));
                return steps;
            }
            steps.add(aStarStep(state, List.of(cell.r, cell.c),
                    "Visit (" + cell.r + "," + cell.c + "). f = g + h = "
                            + cell.g + " + " + cell.h + " = " + cell.f()
                            + ".",
                    LINE_COMPARE));
            for (int[] dir : dirs) {
                int nr = cell.r + dir[0];
                int nc = cell.c + dir[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols
                        || state.get(nr).get(nc) == 1 || closed[nr][nc]) {
                    continue;
                }
                int tentative = gScore[cell.r][cell.c] + 1;
                if (tentative < gScore[nr][nc]) {
                    gScore[nr][nc] = tentative;
                    cameFromR[nr][nc] = cell.r;
                    cameFromC[nr][nc] = cell.c;
                    open.add(new AStarCell(nr, nc, tentative,
                            manhattan(nr, nc, gr, gc)));
                    if (state.get(nr).get(nc) == 0) {
                        state.get(nr).set(nc, 2);
                    }
                }
            }
        }
        // The open set emptied without reaching the goal.
        if (current != null) {
            state.get(current[0]).set(current[1], 3);
        }
        steps.add(aStarStep(state, List.of(), "No path found.",
                LINE_FINAL));
        return steps;
    }

    /** Manhattan distance between two grid cells. */
    private int manhattan(int r1, int c1, int r2, int c2) {
        return Math.abs(r1 - r2) + Math.abs(c1 - c2);
    }

    /** One cell in the A* open set: position, g and h. f = g + h. */
    private static class AStarCell {
        final int r;
        final int c;
        final int g;
        final int h;

        AStarCell(int r, int c, int g, int h) {
            this.r = r;
            this.c = c;
            this.g = g;
            this.h = h;
        }

        int f() {
            return g + h;
        }
    }

    /** Adjacency map from the edge list (undirected, neighbours ascending). */
    private Map<Integer, List<Integer>> buildAdjacency(
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
            adj.computeIfAbsent(v, k -> new ArrayList<>()).add(u);
        }
        for (List<Integer> neighbours : adj.values()) {
            Collections.sort(neighbours);
        }
        return adj;
    }

    /**
     * Weighted adjacency map from the edge list (undirected,
     * neighbours ascending by id). 2-element edges default to
     * weight 1; a null 3rd element also defaults to 1.
     */
    private Map<Integer, List<Neighbor>> buildWeightedAdjacency(
            List<List<Integer>> graphEdges) {
        Map<Integer, List<Neighbor>> adj = new HashMap<>();
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
            int w = pair.size() == 3 && pair.get(2) != null
                    ? pair.get(2) : 1;
            adj.computeIfAbsent(u, k -> new ArrayList<>())
                    .add(new Neighbor(v, w));
            adj.computeIfAbsent(v, k -> new ArrayList<>())
                    .add(new Neighbor(u, w));
        }
        for (List<Neighbor> neighbours : adj.values()) {
            neighbours.sort(Comparator.comparingInt(n -> n.id));
        }
        return adj;
    }

    /** Sorted node ids (every endpoint of at least one edge). */
    private List<Integer> nodeIds(Map<Integer, ?> adj) {
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

    /** Edges as {from, to, weight?} with from < to, deduplicated. */
    private List<EdgeDto> snapshotEdges(List<List<Integer>> graphEdges) {
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
            int from = Math.min(u, v);
            int to = Math.max(u, v);
            String key = from + "-" + to;
            if (seen.add(key)) {
                Integer w = pair.size() == 3 ? pair.get(2) : null;
                edges.add(new EdgeDto(from, to, w));
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

    /**
     * One Dijkstra Step: data = distances (copied, may contain nulls
     * for infinity), highlight = node ids, sorted = settled ids.
     */
    private Step dijkstraStep(List<Integer> distances,
                              List<Integer> highlight,
                              List<Integer> sorted,
                              String message, int line,
                              List<NodeDto> nodes, List<EdgeDto> edges) {
        return new Step(new ArrayList<>(distances), highlight, List.of(),
                sorted, message, line, nodes, edges);
    }

    /**
     * One Bellman-Ford Step: data = distances (copied, may contain
     * nulls for infinity), highlight = the edge being relaxed,
     * sorted stays empty.
     */
    private Step bellmanFordStep(List<Integer> distances,
                                 List<Integer> highlight,
                                 String message, int line,
                                 List<NodeDto> nodes, List<EdgeDto> edges) {
        return new Step(new ArrayList<>(distances), highlight, List.of(),
                List.of(), message, line, nodes, edges);
    }

    /**
     * One topological-sort Step: data = in-degree array (copied),
     * highlight = the node being output (or the neighbour that just
     * became ready), sorted = the output order so far.
     */
    private Step topoStep(List<Integer> inDegree, List<Integer> highlight,
                          List<Integer> sorted,
                          String message, int line,
                          List<NodeDto> nodes, List<EdgeDto> edges) {
        return new Step(new ArrayList<>(inDegree), highlight, List.of(),
                sorted, message, line, nodes, edges);
    }

    /**
     * One A* Step: table = the grid state snapshot (deep copy),
     * highlight = [row, col] of the cell being processed.
     */
    private Step aStarStep(List<List<Integer>> table,
                           List<Integer> highlight,
                           String message, int line) {
        Step step = new Step(List.of(), highlight, List.of(), List.of(),
                message, line);
        step.setTable(copyTable(table));
        return step;
    }

    /**
     * Deep copy of a grid/table (null cells included), so later
     * steps never mutate earlier snapshots.
     */
    private List<List<Integer>> copyTable(List<List<Integer>> table) {
        List<List<Integer>> copy = new ArrayList<>();
        for (List<Integer> row : table) {
            copy.add(new ArrayList<>(row));
        }
        return copy;
    }

    /**
     * One Prim's/Kruskal's Step: data = key array (Prim's) or empty
     * (Kruskal's), highlight = node ids, sorted = MST node ids,
     * mstEdges = the MST edges so far.
     */
    private Step mstStep(List<Integer> data, List<Integer> highlight,
                         List<Integer> sorted,
                         List<List<Integer>> mstEdges,
                         String message, int line,
                         List<NodeDto> nodes, List<EdgeDto> edges) {
        Step step = new Step(new ArrayList<>(data), highlight, List.of(),
                sorted, message, line, nodes, edges);
        step.setMstEdges(mstEdges);
        return step;
    }

    /**
     * Deep copy of the MST edge list, so later steps never mutate
     * earlier snapshots.
     */
    private List<List<Integer>> copyMstEdges(
            List<List<Integer>> mstEdges) {
        List<List<Integer>> copy = new ArrayList<>();
        for (List<Integer> edge : mstEdges) {
            copy.add(new ArrayList<>(edge));
        }
        return copy;
    }

    /** Union-find root with path compression. */
    private int findRoot(Map<Integer, Integer> parent, int node) {
        int root = node;
        while (parent.get(root) != root) {
            root = parent.get(root);
        }
        while (parent.get(node) != root) {
            int next = parent.get(node);
            parent.put(node, root);
            node = next;
        }
        return root;
    }

    /** Union by attaching one root under the other. */
    private void union(Map<Integer, Integer> parent, int a, int b) {
        int rootA = findRoot(parent, a);
        int rootB = findRoot(parent, b);
        if (rootA != rootB) {
            parent.put(rootA, rootB);
        }
    }
}
