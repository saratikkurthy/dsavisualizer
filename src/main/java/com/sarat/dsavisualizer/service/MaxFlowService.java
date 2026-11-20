package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Edmonds-Karp max flow (Phase 20): BFS finds the shortest
 * (fewest-edge) augmenting path in the residual graph, the
 * bottleneck is pushed along it, and the search repeats until
 * no augmenting path remains.
 *
 * <p>The BFS neighbour rule is pinned by the design (and the
 * sim): neighbours of {@code u} come from ONE pass over the
 * input edge list, in order. For each edge {@code (a, b, c)}:
 * if {@code a == u} and the forward residual {@code c - flow}
 * is positive, the neighbour is {@code b}; if {@code b == u}
 * and {@code flow > 0}, the neighbour is {@code a} (reverse
 * residual edge). BFS stops as soon as {@code t} is first
 * discovered, not when it is dequeued.</p>
 *
 * <p>One Step per BFS node visit (dequeue, line 2) + one Step
 * per augmentation (line 4) + one final Step (line 0).
 * {@code data} is the flow per edge in input-edge order;
 * {@code aux} is the residual capacity per edge in the same
 * order; {@code highlight} holds node ids (the node just
 * visited, or the path nodes on an augment step);
 * {@code sorted} holds the nodes visited so far in that BFS
 * round; {@code edges} are the input edges with
 * {@code weight} = capacity (all of them, in input order —
 * parallel edges stay distinct so the edge list stays aligned
 * with {@code data}/{@code aux}). Steps carry the full graph
 * ({@code nodes}/{@code edges}, circular layout, the
 * GraphService positions).</p>
 *
 * <p>The controller validates the input (2..12 edges, capacity
 * 1..99, node ids 0..9, source/sink present, distinct and in
 * the graph); this method guards against bad input anyway with
 * a single "empty" Step.</p>
 */
@Service
public class MaxFlowService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_AUGMENT = 4;
    private static final int LINE_FINAL = 0;

    /**
     * Runs Edmonds-Karp on the directed edge list
     * ({@code [u, v, capacity]} triples) from {@code source} to
     * {@code sink}.
     */
    public List<Step> maxflow(List<List<Integer>> graphEdges,
                              Integer start, Integer target) {
        List<Step> steps = new ArrayList<>();
        List<int[]> edges = parseEdges(graphEdges);
        if (edges.isEmpty() || start == null || target == null) {
            steps.add(flowStep(List.of(), List.of(),
                    "A graph with a source and a sink is needed.",
                    LINE_FINAL, List.of(), List.of(),
                    List.of(), List.of()));
            return steps;
        }
        Set<Integer> nodeSet = new TreeSet<>();
        for (int[] e : edges) {
            nodeSet.add(e[0]);
            nodeSet.add(e[1]);
        }
        List<Integer> nodes = new ArrayList<>(nodeSet);
        if (!nodes.contains(start) || !nodes.contains(target)) {
            steps.add(flowStep(List.of(), List.of(),
                    "Source and sink must be nodes in the graph.",
                    LINE_FINAL, snapshotNodes(nodes),
                    snapshotEdges(edges), zeroFlow(edges),
                    residual(edges, zeroFlow(edges))));
            return steps;
        }
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        List<EdgeDto> edgeDtos = snapshotEdges(edges);

        int[] flow = new int[edges.size()];
        int total = 0;
        while (true) {
            // One BFS round, with the pinned neighbour rule.
            Map<Integer, Integer> parent = new HashMap<>();
            Map<Integer, Integer> parentEdge = new HashMap<>();
            Map<Integer, Boolean> parentForward = new HashMap<>();
            parent.put(start, null);
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(start);
            List<Integer> visitedOrder = new ArrayList<>();
            boolean found = false;
            while (!queue.isEmpty() && !found) {
                int u = queue.poll();
                visitedOrder.add(u);
                steps.add(flowStep(List.of(u),
                        new ArrayList<>(visitedOrder),
                        "BFS visits " + u + ".",
                        LINE_COMPARE, nodeDtos, edgeDtos,
                        flowList(flow), residual(edges, flow)));
                for (int i = 0; i < edges.size(); i++) {
                    int[] e = edges.get(i);
                    int a = e[0];
                    int b = e[1];
                    int c = e[2];
                    Integer v = null;
                    boolean fwd = true;
                    if (a == u && c - flow[i] > 0) {
                        v = b;
                    } else if (b == u && flow[i] > 0) {
                        v = a;
                        fwd = false;
                    }
                    if (v != null && !parent.containsKey(v)) {
                        parent.put(v, u);
                        parentEdge.put(v, i);
                        parentForward.put(v, fwd);
                        if (v.equals(target)) {
                            found = true;
                            break;
                        }
                        queue.add(v);
                    }
                }
            }
            if (!found) {
                break;
            }
            // Rebuild the path t -> s, then push the bottleneck.
            List<Integer> pathNodes = new ArrayList<>();
            pathNodes.add(target);
            int node = target;
            int bottleneck = Integer.MAX_VALUE;
            while (node != start) {
                int i = parentEdge.get(node);
                boolean fwd = parentForward.get(node);
                int res = fwd ? edges.get(i)[2] - flow[i] : flow[i];
                bottleneck = Math.min(bottleneck, res);
                node = parent.get(node);
                pathNodes.add(0, node);
            }
            node = target;
            while (node != start) {
                int i = parentEdge.get(node);
                boolean fwd = parentForward.get(node);
                flow[i] += fwd ? bottleneck : -bottleneck;
                node = parent.get(node);
            }
            total += bottleneck;
            StringBuilder path = new StringBuilder();
            for (int i = 0; i < pathNodes.size(); i++) {
                if (i > 0) {
                    path.append("\u2192");
                }
                path.append(pathNodes.get(i));
            }
            steps.add(flowStep(new ArrayList<>(pathNodes),
                    new ArrayList<>(visitedOrder),
                    "Augment along " + path + " by " + bottleneck
                            + ": flow " + total + ".",
                    LINE_AUGMENT, nodeDtos, edgeDtos,
                    flowList(flow), residual(edges, flow)));
        }
        steps.add(flowStep(List.of(),
                new ArrayList<>(lastVisited(steps)),
                "Max flow = " + total
                        + " (no more augmenting paths).",
                LINE_FINAL, nodeDtos, edgeDtos,
                flowList(flow), residual(edges, flow)));
        return steps;
    }

    /**
     * The visited set of the last BFS round — the nodes the final
     * (failing) BFS reached. The last step added before the final
     * step is a visit step of that round (or the final step is the
     * only step, when the guard above fired — then the set is
     * empty). Recovered from the trace instead of threading the
     * round's list through the loop.
     */
    private List<Integer> lastVisited(List<Step> steps) {
        // The final step is appended by the caller after this
        // returns, so the last step here is the last visit step
        // of the final BFS round — its sorted list IS that round's
        // visited set. (When the loop never ran, steps is empty.)
        if (steps.isEmpty()) {
            return List.of();
        }
        return steps.get(steps.size() - 1).getSorted();
    }

    /** Valid triples only: [u, v, capacity], no self-loops. */
    private List<int[]> parseEdges(List<List<Integer>> graphEdges) {
        List<int[]> edges = new ArrayList<>();
        if (graphEdges == null) {
            return edges;
        }
        for (List<Integer> pair : graphEdges) {
            if (pair == null || pair.size() != 3) {
                continue;
            }
            Integer u = pair.get(0);
            Integer v = pair.get(1);
            Integer c = pair.get(2);
            if (u == null || v == null || c == null || u.equals(v)) {
                continue;
            }
            edges.add(new int[]{u, v, c});
        }
        return edges;
    }

    /** Flow per edge as a list (input order). */
    private List<Integer> flowList(int[] flow) {
        List<Integer> out = new ArrayList<>();
        for (int f : flow) {
            out.add(f);
        }
        return out;
    }

    /** Zero flow per edge (input order). */
    private List<Integer> zeroFlow(List<int[]> edges) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            out.add(0);
        }
        return out;
    }

    /** Residual capacity per edge: capacity - flow (input order). */
    private List<Integer> residual(List<int[]> edges, int[] flow) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            out.add(edges.get(i)[2] - flow[i]);
        }
        return out;
    }

    /** Residual capacity per edge from a flow list (input order). */
    private List<Integer> residual(List<int[]> edges,
                                   List<Integer> flow) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            out.add(edges.get(i)[2] - flow.get(i));
        }
        return out;
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

    /** Directed edges with weight = capacity, in input order. */
    private List<EdgeDto> snapshotEdges(List<int[]> edges) {
        List<EdgeDto> dtos = new ArrayList<>();
        for (int[] e : edges) {
            dtos.add(new EdgeDto(e[0], e[1], e[2]));
        }
        return dtos;
    }

    /** One max-flow Step: flow in data, residual in aux. */
    private Step flowStep(List<Integer> highlight,
                          List<Integer> sorted, String message,
                          int line, List<NodeDto> nodes,
                          List<EdgeDto> edges, List<Integer> flow,
                          List<Integer> residual) {
        Step step = new Step(new ArrayList<>(flow), highlight,
                List.of(), sorted, message, line, nodes, edges);
        step.setAux(new ArrayList<>(residual));
        return step;
    }
}
