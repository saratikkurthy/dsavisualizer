package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Dinic's max flow (Phase 31, Graphs v3): level-graph BFS
 * phases, each followed by a blocking flow built from repeated
 * single-path DFS augmentations in the level graph.
 *
 * <p>Step shape follows MaxFlowService (Edmonds-Karp)
 * conventions: {@code data} is the flow per edge in
 * input-edge order on EVERY step, {@code aux} is the residual
 * capacity per edge in the same order, {@code nodes} /
 * {@code edges} are the full graph (circular layout, edges
 * with weight = capacity, parallel edges stay distinct).
 * One Step per BFS node visit (line 2, {@code sorted} = the
 * BFS-visited nodes in that phase) + one Step per
 * blocking-flow augmentation (line 4, {@code highlight} =
 * the path nodes) + one final Step (line 0).</p>
 *
 * <p>The level graph rides in {@code table} as a single row
 * (the level array, -1 = unreachable): visit steps carry the
 * levels discovered so far, augment steps carry the phase's
 * complete level array, and the final step carries the last
 * (failing) BFS's levels. {@code table} is the only spare
 * structured field — {@code data} stays the flow and
 * {@code aux} stays the residual on every step, exactly as
 * MaxFlowService does.</p>
 *
 * <p>BFS neighbours come from ONE pass over the input edge
 * list, in order, following forward residual edges only
 * (cap - flow &gt; 0) and stopping when {@code t} is first
 * discovered — the MaxFlowService rule, minus reverse edges.
 * Textbook Dinic builds the level graph on the full residual
 * graph (reverse edges with flow &gt; 0 included); this
 * implementation follows the design's sim instead, which is
 * forward-only. On the demo graph the difference is one
 * dead-end visit: a textbook BFS would also discover node 1
 * at level 3 in phase 2 (via the reverse of 1-&gt;4, whose
 * flow is 6), making 16 steps; the pinned trace is 15 steps
 * with phase-2 levels [0,-1,1,3,2,4], and the max flow is
 * 19 either way. DFS for augmenting paths tries neighbours
 * in input-edge order, only forward edges with residual
 * &gt; 0 and level[v] == level[u] + 1, first eligible edge
 * per node wins (parallel edges: the first eligible one in
 * input order), backtracking on dead ends — the sim's rule
 * ({@code ~/workspace/.tmp/p31-sim/sims.py}).</p>
 *
 * <p>Demo (directed), source 0, sink 5, edges: 0-&gt;1:10,
 * 0-&gt;2:10, 1-&gt;2:2, 1-&gt;3:4, 1-&gt;4:8, 2-&gt;4:9,
 * 3-&gt;5:10, 4-&gt;3:6, 4-&gt;5:10. Two phases, max flow 19.
 * Phase 1 (levels [0,1,1,2,2,3]): 0-&gt;1-&gt;3-&gt;5 by 4,
 * 0-&gt;1-&gt;4-&gt;5 by 6, 0-&gt;2-&gt;4-&gt;5 by 4. Phase 2
 * (levels [0,-1,1,3,2,4]): 0-&gt;2-&gt;4-&gt;3-&gt;5 by 5.
 * Final flow per edge (input order): [10, 9, 0, 4, 6, 9, 9,
 * 5, 10]. Trace: 4 visits + 3 augments + 4 visits + 1
 * augment + 2 visits (failing BFS) + final = 15 steps.</p>
 *
 * <p>The controller validates the input (2..12 edges,
 * capacity 1..99, node ids 0..9, source/sink present,
 * distinct and in the graph — the same bounds as
 * validateMaxFlow); this method guards against bad input
 * anyway with a single "empty" Step.</p>
 */
@Service
public class DinicService {

    private static final int LINE_VISIT = 2;
    private static final int LINE_AUGMENT = 4;
    private static final int LINE_FINAL = 0;

    /**
     * Runs Dinic's algorithm on the directed edge list
     * ({@code [u, v, capacity]} triples) from {@code source}
     * to {@code sink}.
     */
    public List<Step> dinic(List<List<Integer>> graphEdges,
                            Integer start, Integer target) {
        List<Step> steps = new ArrayList<>();
        List<int[]> edges = parseEdges(graphEdges);
        if (edges.isEmpty() || start == null || target == null) {
            steps.add(flowStep(List.of(), List.of(),
                    "A graph with a source and a sink is needed.",
                    LINE_FINAL, List.of(), List.of(),
                    List.of(), List.of(), List.of()));
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
                    residual(edges, zeroFlow(edges)), List.of()));
            return steps;
        }
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        List<EdgeDto> edgeDtos = snapshotEdges(edges);
        int n = nodes.size();
        Map<Integer, Integer> pos = new HashMap<>();
        for (int i = 0; i < n; i++) {
            pos.put(nodes.get(i), i);
        }

        int[] flow = new int[edges.size()];
        int total = 0;
        int phase = 0;
        List<Integer> lastVisited = List.of();
        int[] lastLevel = new int[n];
        for (int i = 0; i < n; i++) {
            lastLevel[i] = -1;
        }
        while (true) {
            // One BFS round: the level graph for this phase.
            int[] level = new int[n];
            for (int i = 0; i < n; i++) {
                level[i] = -1;
            }
            level[pos.get(start)] = 0;
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(start);
            List<Integer> visitedOrder = new ArrayList<>();
            boolean found = false;
            while (!queue.isEmpty() && !found) {
                int u = queue.poll();
                visitedOrder.add(u);
                steps.add(flowStep(List.of(u),
                        new ArrayList<>(visitedOrder),
                        "BFS visits " + u + " (level " + level[pos.get(u)]
                                + ").",
                        LINE_VISIT, nodeDtos, edgeDtos,
                        flowList(flow), residual(edges, flow),
                        levelList(level)));
                for (int i = 0; i < edges.size(); i++) {
                    int[] e = edges.get(i);
                    int a = e[0];
                    int b = e[1];
                    int c = e[2];
                    // Forward residual edges only (cap - flow > 0):
                    // the design's level graph is built on forward
                    // edges, not the full residual graph. Textbook
                    // Dinic also follows reverse edges (flow > 0),
                    // which on the demo graph only discovers node 1
                    // at level 3 in phase 2 — a dead end that leaves
                    // the max flow at 19 but adds a 16th step. The
                    // pinned trace (15 steps, phase-2 levels
                    // [0,-1,1,3,2,4]) is forward-only, so this is
                    // forward-only.
                    Integer v = null;
                    if (a == u && c - flow[i] > 0
                            && level[pos.get(b)] < 0) {
                        v = b;
                    }
                    if (v != null) {
                        level[pos.get(v)] = level[pos.get(u)] + 1;
                        if (v.equals(target)) {
                            found = true;
                            break;
                        }
                        queue.add(v);
                    }
                }
            }
            lastVisited = new ArrayList<>(visitedOrder);
            lastLevel = level.clone();
            if (level[pos.get(target)] < 0) {
                break;
            }
            phase++;
            // Blocking flow: repeated single-path DFS in the
            // level graph until no augmenting path remains.
            while (true) {
                List<Integer> pathNodes = new ArrayList<>();
                Map<Integer, Integer> parent = new HashMap<>();
                Map<Integer, Integer> parentEdge = new HashMap<>();
                parent.put(start, null);
                Deque<Integer> stack = new ArrayDeque<>();
                stack.push(start);
                boolean pathFound = false;
                while (!stack.isEmpty()) {
                    int u = stack.peek();
                    if (u == target) {
                        pathFound = true;
                        break;
                    }
                    Integer next = null;
                    int nextEdge = -1;
                    for (int i = 0; i < edges.size(); i++) {
                        int[] e = edges.get(i);
                        int a = e[0];
                        int b = e[1];
                        int c = e[2];
                        // Forward residual edges only, like the
                        // BFS above (see its comment): the next
                        // node must be exactly one level deeper.
                        Integer v = null;
                        if (a == u && c - flow[i] > 0
                                && level[pos.get(b)]
                                        == level[pos.get(u)] + 1
                                && !parent.containsKey(b)) {
                            v = b;
                        }
                        if (v != null) {
                            next = v;
                            nextEdge = i;
                            break;
                        }
                    }
                    if (next == null) {
                        stack.pop();
                    } else {
                        parent.put(next, u);
                        parentEdge.put(next, nextEdge);
                        stack.push(next);
                    }
                }
                if (!pathFound) {
                    break;
                }
                int node = target;
                int bottleneck = Integer.MAX_VALUE;
                while (node != start) {
                    int i = parentEdge.get(node);
                    int res = edges.get(i)[2] - flow[i];
                    bottleneck = Math.min(bottleneck, res);
                    node = parent.get(node);
                }
                node = target;
                while (node != start) {
                    int i = parentEdge.get(node);
                    flow[i] += bottleneck;
                    node = parent.get(node);
                }
                total += bottleneck;
                pathNodes.add(target);
                node = target;
                while (node != start) {
                    node = parent.get(node);
                    pathNodes.add(0, node);
                }
                StringBuilder path = new StringBuilder();
                for (int i = 0; i < pathNodes.size(); i++) {
                    if (i > 0) {
                        path.append("\u2192");
                    }
                    path.append(pathNodes.get(i));
                }
                steps.add(flowStep(new ArrayList<>(pathNodes),
                        new ArrayList<>(visitedOrder),
                        "Phase " + phase + ": augment along " + path
                                + " by " + bottleneck + ": flow " + total
                                + ".",
                        LINE_AUGMENT, nodeDtos, edgeDtos,
                        flowList(flow), residual(edges, flow),
                        levelList(level)));
            }
        }
        steps.add(flowStep(List.of(), new ArrayList<>(lastVisited),
                "Dinic's max flow = " + total
                        + " (no more augmenting paths).",
                LINE_FINAL, nodeDtos, edgeDtos,
                flowList(flow), residual(edges, flow),
                levelList(lastLevel)));
        return steps;
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

    /** Level array as a list (sorted-node order, -1 = unreachable). */
    private List<Integer> levelList(int[] level) {
        List<Integer> out = new ArrayList<>();
        for (int l : level) {
            out.add(l);
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

    /**
     * One Dinic Step: flow in data, residual in aux, the level
     * array as a single-row table.
     */
    private Step flowStep(List<Integer> highlight,
                          List<Integer> sorted, String message,
                          int line, List<NodeDto> nodes,
                          List<EdgeDto> edges, List<Integer> flow,
                          List<Integer> residual, List<Integer> level) {
        Step step = new Step(new ArrayList<>(flow), highlight,
                List.of(), sorted, message, line, nodes, edges);
        step.setAux(new ArrayList<>(residual));
        step.setTable(List.of(new ArrayList<>(level)));
        return step;
    }
}
