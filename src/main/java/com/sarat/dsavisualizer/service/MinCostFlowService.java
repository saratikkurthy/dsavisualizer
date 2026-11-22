package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Min-cost max-flow (Phase 25, matching &amp; flow):
 * successive shortest paths — every augmentation pushes
 * flow along the cheapest s&rarr;t path left in the residual
 * graph (SPFA, because reverse edges carry negative costs),
 * and the flow stays cheapest because each augmentation
 * only ever uses a shortest path.
 *
 * <p>Demo network s=0, a=1, b=2, t=3. Edges (in order):
 * 0&rarr;1 cap 2 cost 1; 0&rarr;2 cap 2 cost 3; 1&rarr;3 cap
 * 2 cost 1; 2&rarr;3 cap 2 cost 1; 1&rarr;2 cap 1 cost 1.
 * Augment 1 — 0&rarr;1&rarr;3, bottleneck 2, path cost 2,
 * cost so far 4; augment 2 — 0&rarr;2&rarr;3, bottleneck 2,
 * path cost 4, cost so far 12. Max flow <b>4</b>, min cost
 * <b>12</b>. (The 1&rarr;2 shortcut costs 3 a unit — dearer
 * than 0&rarr;1&rarr;3 at 2 — so the optimum never uses it.)
 * Trace: 2 augment steps (line 2) + final (line 0) = 3
 * steps. Augment step: {@code highlight} = [path nodes in
 * order], {@code data} = [flow so far, cost so far],
 * {@code sorted} = [flow on each edge, in input order],
 * {@code edges} weight = flow on that edge. Final step
 * {@code data} = [4, 12], {@code sorted} = [2, 2, 2, 2, 0].
 * {@code nodes} are s/a/b/t (value 0, x/y = 0).</p>
 *
 * <p>Bad input (a missing/empty edge list, or a cost list
 * that does not match the edges) yields a single "empty"
 * Step; the controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class MinCostFlowService {

    private static final int LINE_AUGMENT = 2;
    private static final int LINE_FINAL = 0;
    private static final int INF = Integer.MAX_VALUE / 2;

    /** One residual edge (forward or reverse). */
    private static final class Residual {
        final int to;
        final int cost;
        int capacity;
        final int reverse;

        Residual(int to, int capacity, int cost, int reverse) {
            this.to = to;
            this.capacity = capacity;
            this.cost = cost;
            this.reverse = reverse;
        }
    }

    /**
     * Runs min-cost max-flow on the network given by
     * {@code graphEdges} ([u, v, cap] triples) with the
     * parallel {@code costs} (null &rarr; all zero), from
     * {@code start} (null &rarr; the smallest node id) to
     * {@code target} (null &rarr; the largest node id).
     */
    public List<Step> minCostFlow(List<List<Integer>> graphEdges,
                                  List<Integer> costs,
                                  Integer start, Integer target) {
        List<Step> steps = new ArrayList<>();
        if (graphEdges == null || graphEdges.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Network edges are needed as "
                            + "[u, v, cap] triples.", LINE_FINAL));
            return steps;
        }
        int edgeCount = graphEdges.size();
        if (costs != null && costs.size() != edgeCount) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "One cost is needed per edge.",
                    LINE_FINAL));
            return steps;
        }
        List<int[]> triples = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int maxNode = -1;
        for (List<Integer> edge : graphEdges) {
            if (edge == null || edge.size() < 3
                    || edge.get(0) == null || edge.get(1) == null
                    || edge.get(2) == null) {
                steps.add(new Step(List.of(), List.of(), List.of(),
                        List.of(), "Each edge must be "
                                + "[u, v, cap].", LINE_FINAL));
                return steps;
            }
            int cost = costs != null ? costs.get(triples.size()) : 0;
            triples.add(new int[]{edge.get(0), edge.get(1),
                    edge.get(2), cost});
            maxNode = Math.max(maxNode,
                    Math.max(edge.get(0), edge.get(1)));
        }
        int nodeCount = maxNode + 1;
        int source = start != null ? start : 0;
        int sink = target != null ? target : maxNode;
        if (source < 0 || source >= nodeCount
                || sink < 0 || sink >= nodeCount
                || source == sink) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Source and sink must be "
                            + "different nodes in the network.",
                    LINE_FINAL));
            return steps;
        }

        // Residual graph: each edge gets a reverse edge (negative cost).
        List<List<Residual>> residual = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            residual.add(new ArrayList<>());
        }
        // flow[i] = the flow on original edge i; forwardIndex[i] =
        // the residual index of original edge i's forward edge.
        int[] flow = new int[edgeCount];
        int[] forwardIndex = new int[edgeCount];
        for (int i = 0; i < edgeCount; i++) {
            int[] triple = triples.get(i);
            int u = triple[0];
            int v = triple[1];
            int cap = triple[2];
            int cost = triple[3];
            forwardIndex[i] = residual.get(u).size();
            int reverseIndex = residual.get(v).size();
            residual.get(u).add(new Residual(v, cap, cost,
                    reverseIndex));
            residual.get(v).add(new Residual(u, 0, -cost,
                    forwardIndex[i]));
        }

        int totalFlow = 0;
        int totalCost = 0;
        int augmentNumber = 0;
        while (true) {
            // SPFA: shortest s->t path in the residual graph.
            int[] dist = new int[nodeCount];
            int[] prevNode = new int[nodeCount];
            int[] prevEdge = new int[nodeCount];
            boolean[] inQueue = new boolean[nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                dist[i] = INF;
                prevNode[i] = -1;
            }
            dist[source] = 0;
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(source);
            inQueue[source] = true;
            while (!queue.isEmpty()) {
                int u = queue.remove();
                inQueue[u] = false;
                for (int i = 0; i < residual.get(u).size(); i++) {
                    Residual edge = residual.get(u).get(i);
                    if (edge.capacity > 0
                            && dist[u] + edge.cost < dist[edge.to]) {
                        dist[edge.to] = dist[u] + edge.cost;
                        prevNode[edge.to] = u;
                        prevEdge[edge.to] = i;
                        if (!inQueue[edge.to]) {
                            queue.add(edge.to);
                            inQueue[edge.to] = true;
                        }
                    }
                }
            }
            if (dist[sink] == INF) {
                break;
            }
            // Bottleneck along the path, then augment.
            List<Integer> path = new ArrayList<>();
            int bottleneck = INF;
            for (int node = sink; node != source;
                    node = prevNode[node]) {
                path.add(0, node);
                bottleneck = Math.min(bottleneck,
                        residual.get(prevNode[node]).get(prevEdge[node])
                                .capacity);
            }
            path.add(0, source);
            for (int node = sink; node != source;
                    node = prevNode[node]) {
                int u = prevNode[node];
                int edgeIndex = prevEdge[node];
                Residual edge = residual.get(u).get(edgeIndex);
                Residual reverse = residual.get(node)
                        .get(edge.reverse);
                edge.capacity -= bottleneck;
                reverse.capacity += bottleneck;
            }
            totalFlow += bottleneck;
            totalCost += bottleneck * dist[sink];
            augmentNumber++;
            for (int i = 0; i < edgeCount; i++) {
                flow[i] = triples.get(i)[2]
                        - residual.get(triples.get(i)[0])
                                .get(forwardIndex[i]).capacity;
            }
            steps.add(new Step(
                    List.of(totalFlow, totalCost),
                    new ArrayList<>(path), List.of(),
                    flows(flow),
                    "Augment " + augmentNumber + ": push " + bottleneck
                            + " along " + path + " (path cost "
                            + dist[sink] + "); flow " + totalFlow
                            + ", cost " + totalCost + ".",
                    LINE_AUGMENT, snapshot(nodeCount),
                    edgeDtos(triples, flow)));
        }
        steps.add(new Step(
                List.of(totalFlow, totalCost), List.of(), List.of(),
                flows(flow),
                "Min-cost max-flow done: flow " + totalFlow
                        + ", cost " + totalCost + ".",
                LINE_FINAL, snapshot(nodeCount),
                edgeDtos(triples, flow)));
        return steps;
    }

    private List<Integer> flows(int[] flow) {
        List<Integer> result = new ArrayList<>();
        for (int value : flow) {
            result.add(value);
        }
        return result;
    }

    /** One NodeDto per node (labelled s/a/b/t for the demo network, else the id). */
    private List<NodeDto> snapshot(int nodeCount) {
        List<NodeDto> dtos = new ArrayList<>();
        String[] names = {"s", "a", "b", "t"};
        for (int i = 0; i < nodeCount; i++) {
            NodeDto dto = new NodeDto(i, 0, 0, 0);
            dto.setLabel(i < names.length ? names[i]
                    : String.valueOf(i));
            dtos.add(dto);
        }
        return dtos;
    }

    /** One EdgeDto per network edge, weight = the flow on it. */
    private List<EdgeDto> edgeDtos(List<int[]> triples, int[] flow) {
        List<EdgeDto> dtos = new ArrayList<>();
        for (int i = 0; i < triples.size(); i++) {
            dtos.add(new EdgeDto(triples.get(i)[0],
                    triples.get(i)[1], flow[i]));
        }
        return dtos;
    }
}
