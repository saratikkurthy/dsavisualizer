package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Hopcroft-Karp (Phase 25, matching &amp; flow): maximum
 * bipartite matching in O(E&radic;V) — the phase trick that
 * makes it faster than one augmenting path at a time. Each
 * phase BFS-layers the free left vertices, then DFS finds a
 * maximal set of vertex-disjoint <i>shortest</i> augmenting
 * paths; the phases end when no free left vertex can reach
 * a free right one.
 *
 * <p>Demo: left L1..L3 (ids 0,1,2), right R1..R3 (ids 3,4,5),
 * edges (in order) 0-3, 0-4, 1-4, 1-5, 2-3. Matching:
 * {0:4, 1:5, 2:3} — size <b>3</b>. Phases: <b>2</b>. Trace:
 * phase 1 BFS (line 2) + 2 DFS steps (line 3) + phase 2 BFS
 * (line 2) + 1 DFS step (line 3) + final BFS (line 2, no
 * free left vertex reaches a free right one) + final
 * (line 0) = 7 steps. BFS step: {@code highlight} = [free
 * left nodes], {@code data} = [matching size so far],
 * {@code sorted} = the matching pairs flattened ([u,v,&hellip;]).
 * DFS step: {@code highlight} = [the augmenting path nodes,
 * in path order], {@code data} = [matching size after this
 * path], {@code sorted} = the matching pairs flattened.
 * Final step {@code data} = [3], {@code sorted} =
 * [0,4,1,5,2,3]. {@code nodes} value = matched partner id
 * or -1. {@code edges} = the 5 bipartite edges throughout.</p>
 *
 * <p><b>Design deviation (recorded, not hidden):</b> the
 * design pins phase 1's paths as 0&rarr;4 and 1&rarr;5, but
 * textbook Hopcroft-Karp on this graph — BFS layers from
 * the free left vertices, DFS in edge order — finds 0&rarr;3
 * and 1&rarr;4 in phase 1 (vertex 2's only neighbour, 3, is
 * taken by 0, and the layered DFS cannot reroute through it:
 * dist[0] = 0 &ne; dist[2] + 1), and phase 2 finds the
 * length-5 path 2&rarr;3&rarr;0&rarr;4&rarr;1&rarr;5, which
 * reassigns 0 and 1 to their final partners. The final
 * matching {0:4, 1:5, 2:3}, the 2 phases, the 7 steps and
 * every {@code data} / {@code sorted} value match the design
 * exactly; only the three DFS-step highlights differ from
 * the design's hand-sim (0&rarr;3, 1&rarr;4,
 * 2&rarr;3&rarr;0&rarr;4&rarr;1&rarr;5 here). Note the
 * design's own split cannot come from textbook HK: for
 * 2&rarr;3 to land in phase 2, vertex 3 must already be
 * matched during phase 1, which requires 0&rarr;3 in phase
 * 1 — contradicting the pinned 0&rarr;4. The frontend mock
 * should be updated to the backend's paths (the Phase 24
 * precedent: the backend governs where the sim and the
 * algorithm disagree). This service is the real algorithm,
 * not a scripted demo: any bipartite graph gets a correct
 * maximum matching.</p>
 *
 * <p>Bad input (a missing/empty edge list, or a node that
 * appears on both sides) yields a single "empty" Step; the
 * controller 400s on it (see VisualizationController).</p>
 */
@Service
public class HopcroftKarpService {

    private static final int LINE_BFS = 2;
    private static final int LINE_DFS = 3;
    private static final int LINE_FINAL = 0;
    private static final int INF = Integer.MAX_VALUE;

    /** Per-call matching state (the service bean is a singleton, so no instance state). */
    private static final class Matching {
        final List<Integer> left;
        final List<Integer> right;
        final Map<Integer, List<Integer>> adj = new LinkedHashMap<>();
        final Map<Integer, Integer> pairU = new HashMap<>();
        final Map<Integer, Integer> pairV = new HashMap<>();
        final Map<Integer, Integer> dist = new HashMap<>();
        int distNil = INF;

        Matching(List<Integer> left, List<Integer> right) {
            this.left = left;
            this.right = right;
            for (int u : left) {
                adj.put(u, new ArrayList<>());
                pairU.put(u, -1);
                dist.put(u, INF);
            }
            for (int v : right) {
                pairV.put(v, -1);
            }
        }

        /** The standard HK BFS: layers the free left vertices; true when a shortest augmenting path exists. */
        boolean bfs() {
            Deque<Integer> queue = new ArrayDeque<>();
            for (int u : left) {
                if (pairU.get(u) == -1) {
                    dist.put(u, 0);
                    queue.add(u);
                } else {
                    dist.put(u, INF);
                }
            }
            distNil = INF;
            while (!queue.isEmpty()) {
                int u = queue.remove();
                if (dist.get(u) < distNil) {
                    for (int v : adj.get(u)) {
                        int pu = pairV.get(v);
                        if (pu == -1) {
                            distNil = dist.get(u) + 1;
                        } else if (dist.get(pu) == INF) {
                            dist.put(pu, dist.get(u) + 1);
                            queue.add(pu);
                        }
                    }
                }
            }
            return distNil != INF;
        }

        /**
         * The standard HK DFS: the augmenting path from u, in
         * path order (free left ... free right), or null. The
         * matching is updated as the recursion unwinds.
         */
        List<Integer> dfs(int u) {
            for (int v : adj.get(u)) {
                int pu = pairV.get(v);
                if (pu == -1) {
                    if (dist.get(u) + 1 == distNil) {
                        pairU.put(u, v);
                        pairV.put(v, u);
                        return List.of(u, v);
                    }
                } else if (dist.get(pu) == dist.get(u) + 1) {
                    List<Integer> sub = dfs(pu);
                    if (sub != null) {
                        pairU.put(u, v);
                        pairV.put(v, u);
                        List<Integer> path = new ArrayList<>();
                        path.add(u);
                        path.add(v);
                        path.addAll(sub);
                        return path;
                    }
                }
            }
            dist.put(u, INF);
            return null;
        }

        int size() {
            int size = 0;
            for (int u : left) {
                if (pairU.get(u) != -1) {
                    size++;
                }
            }
            return size;
        }

        /** The matching pairs flattened, in left-id order. */
        List<Integer> pairs() {
            List<Integer> pairs = new ArrayList<>();
            for (int u : left) {
                int v = pairU.get(u);
                if (v != -1) {
                    pairs.add(u);
                    pairs.add(v);
                }
            }
            return pairs;
        }

        List<Integer> freeLeft() {
            List<Integer> free = new ArrayList<>();
            for (int u : left) {
                if (pairU.get(u) == -1) {
                    free.add(u);
                }
            }
            return free;
        }
    }

    /** Runs Hopcroft-Karp on the bipartite graph given by {@code graphEdges} ([left, right] pairs). */
    public List<Step> hopcroftKarp(List<List<Integer>> graphEdges) {
        List<Step> steps = new ArrayList<>();
        if (graphEdges == null || graphEdges.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Bipartite edges are needed as "
                            + "[left, right] pairs.", LINE_FINAL));
            return steps;
        }
        TreeSet<Integer> leftSet = new TreeSet<>();
        TreeSet<Integer> rightSet = new TreeSet<>();
        List<int[]> edges = new ArrayList<>();
        for (List<Integer> edge : graphEdges) {
            if (edge == null || edge.size() < 2
                    || edge.get(0) == null || edge.get(1) == null) {
                continue;
            }
            leftSet.add(edge.get(0));
            rightSet.add(edge.get(1));
            edges.add(new int[]{edge.get(0), edge.get(1)});
        }
        if (edges.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Bipartite edges are needed as "
                            + "[left, right] pairs.", LINE_FINAL));
            return steps;
        }
        // A node on both sides is not a bipartite graph as given.
        for (int node : leftSet) {
            if (rightSet.contains(node)) {
                steps.add(new Step(List.of(), List.of(), List.of(),
                        List.of(), "Node " + node + " appears on "
                                + "both sides.", LINE_FINAL));
                return steps;
            }
        }
        List<Integer> left = new ArrayList<>(leftSet);
        List<Integer> right = new ArrayList<>(rightSet);
        Matching matching = new Matching(left, right);
        for (int[] edge : edges) {
            if (!matching.adj.get(edge[0]).contains(edge[1])) {
                matching.adj.get(edge[0]).add(edge[1]);
            }
        }
        List<EdgeDto> edgeDtos = new ArrayList<>();
        for (int[] edge : edges) {
            edgeDtos.add(new EdgeDto(edge[0], edge[1], null));
        }

        while (true) {
            boolean found = matching.bfs();
            steps.add(new Step(
                    List.of(matching.size()),
                    matching.freeLeft(), List.of(),
                    matching.pairs(),
                    found
                            ? "BFS: shortest augmenting path has "
                                    + "length " + (2 * matching.distNil - 1)
                                    + "; free left vertices "
                                    + matching.freeLeft() + "."
                            : "BFS: no free left vertex reaches a "
                                    + "free right vertex — no more "
                                    + "augmenting paths.",
                    LINE_BFS, snapshot(left, right, matching),
                    new ArrayList<>(edgeDtos)));
            if (!found) {
                break;
            }
            for (int u : new ArrayList<>(left)) {
                if (matching.pairU.get(u) != -1) {
                    continue;
                }
                List<Integer> path = matching.dfs(u);
                if (path != null) {
                    steps.add(new Step(
                            List.of(matching.size()),
                            new ArrayList<>(path), List.of(),
                            matching.pairs(),
                            "Augmenting path " + path
                                    + ": matching size "
                                    + matching.size() + ".",
                            LINE_DFS, snapshot(left, right, matching),
                            new ArrayList<>(edgeDtos)));
                }
            }
        }
        steps.add(new Step(
                List.of(matching.size()), List.of(), List.of(),
                matching.pairs(),
                "Maximum matching done: size " + matching.size()
                        + ", pairs " + matching.pairs() + ".",
                LINE_FINAL, snapshot(left, right, matching),
                new ArrayList<>(edgeDtos)));
        return steps;
    }

    /** One NodeDto per vertex: left ids then right ids, labelled L1.. / R1.., value = partner or -1. */
    private List<NodeDto> snapshot(List<Integer> left,
                                   List<Integer> right,
                                   Matching matching) {
        List<NodeDto> dtos = new ArrayList<>();
        for (int i = 0; i < left.size(); i++) {
            NodeDto dto = new NodeDto(left.get(i),
                    matching.pairU.get(left.get(i)), 0, 0);
            dto.setLabel("L" + (i + 1));
            dtos.add(dto);
        }
        for (int i = 0; i < right.size(); i++) {
            NodeDto dto = new NodeDto(right.get(i),
                    matching.pairV.get(right.get(i)), 0, 0);
            dto.setLabel("R" + (i + 1));
            dtos.add(dto);
        }
        return dtos;
    }
}
