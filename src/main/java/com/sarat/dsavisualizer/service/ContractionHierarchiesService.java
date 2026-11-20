package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Contraction Hierarchies (Phase 22, the Google Maps speedup):
 * preprocessing contracts the nodes one by one in a fixed
 * order; contracting x adds a shortcut (u, v) for every pair
 * of neighbours (u, v) of x whose best path WITHOUT x is
 * longer than w(u,x) + w(x,v) (the witness search is a
 * Dijkstra from u on the current graph with x and all
 * already-contracted nodes removed). The query is then a
 * bidirectional Dijkstra that only follows edges UP the
 * hierarchy (to higher-ranked nodes) — far fewer nodes are
 * settled than a plain Dijkstra. Shortcuts on the meeting
 * node's parent chains are unpacked into the real path.
 *
 * <p>Steps: one contract step per node (line 1) plus one
 * shortcut step per shortcut added (line 4), then the query
 * — one step per upward settle (line 6 forward / 8 backward),
 * one per upward relaxation that improves a neighbour
 * (line 7 / 9), one per mu improvement (line 10), plus the
 * final Step (line 0). A contract step carries {@code data} =
 * the ranks array (-1 = unranked), {@code highlight} = [node]
 * and {@code edges} = the original edges plus the shortcuts
 * added so far; its message names the node, its rank and how
 * many shortcuts were added. A shortcut step highlights
 * [u, v] and its edges include the new shortcut. Query steps
 * carry {@code data} = distF (upward) and {@code aux} = distB
 * (upward), -1 = unknown; a mu step carries {@code sorted} =
 * [meeting node]; the final Step carries {@code sorted} = the
 * unpacked path. The edge list is always the original edges
 * (input order) followed by the shortcuts (creation order),
 * each with its weight — so the frontend can tell shortcuts
 * apart from original edges. Demo (the standard Dijkstra
 * demo graph, order [2,1,4,0,3,5]): shortcuts [(0,1,3 via 2),
 * (0,3,8 via 1), (3,5,5 via 4)], ranks {2:0, 1:1, 4:2, 0:3,
 * 3:4, 5:5}, query dist 13, unpacked path [0,2,1,3,4,5],
 * meeting node 5, settles 3 (forward 2 + backward 1) vs plain
 * Dijkstra 6, 6 contracts + 3 shortcuts + 6 query steps +
 * final = 16.</p>
 *
 * <p>The controller validates the input (graphEdges/start
 * missing &rarr; 400, any weight &lt;= 0 &rarr; 400, target
 * missing, start/target not in the graph, start == target
 * &rarr; 400, and a given contraction order that is not a
 * permutation of the graph's nodes &rarr; 400); this method
 * guards against bad input anyway with a single "empty" Step,
 * and falls back to ascending node id when a given order is
 * not a permutation.</p>
 */
@Service
public class ContractionHierarchiesService {

    private static final int LINE_CONTRACT = 1;
    private static final int LINE_SHORTCUT = 4;
    private static final int LINE_SETTLE_FORWARD = 6;
    private static final int LINE_RELAX_FORWARD = 7;
    private static final int LINE_SETTLE_BACKWARD = 8;
    private static final int LINE_RELAX_BACKWARD = 9;
    private static final int LINE_MU = 10;
    private static final int LINE_FINAL = 0;

    /** Infinity for min-keys, mu and witness distances. */
    private static final long INF = Long.MAX_VALUE;
    private static final int INF_INT = Integer.MAX_VALUE;

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

    /** One weighted neighbour: the node id and the edge weight to it. */
    private static class Neighbor {
        final int id;
        final int weight;

        Neighbor(int id, int weight) {
            this.id = id;
            this.weight = weight;
        }
    }

    /** An unordered node pair (a shortcut key): lo &lt; hi. */
    private static class UnorderedPair {
        final int lo;
        final int hi;

        UnorderedPair(int a, int b) {
            this.lo = Math.min(a, b);
            this.hi = Math.max(a, b);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof UnorderedPair)) {
                return false;
            }
            UnorderedPair o = (UnorderedPair) other;
            return lo == o.lo && hi == o.hi;
        }

        @Override
        public int hashCode() {
            return 31 * lo + hi;
        }
    }

    /** A shortcut added while contracting a node (alt = INF: no witness). */
    private static class Shortcut {
        final int u;
        final int v;
        final int w;
        final int via;
        final int alt;

        Shortcut(int u, int v, int w, int via, int alt) {
            this.u = u;
            this.v = v;
            this.w = w;
            this.via = via;
            this.alt = alt;
        }
    }

    /** One upward edge: the target, the weight, and the via node (null = original edge). */
    private static class UpEdge {
        final int to;
        final int weight;
        final Integer via;

        UpEdge(int to, int weight, Integer via) {
            this.to = to;
            this.weight = weight;
            this.via = via;
        }
    }

    /** A parent pointer on a query chain: the parent, the edge weight, the via node (null = original edge). */
    private static class Parent {
        final int node;
        final int weight;
        final Integer via;

        Parent(int node, int weight, Integer via) {
            this.node = node;
            this.weight = weight;
            this.via = via;
        }
    }

    /**
     * Runs contraction hierarchies on the undirected weighted
     * edge list ({@code [u, v]} or {@code [u, v, w]} triples)
     * from {@code start} to {@code target}, contracting nodes
     * in {@code order} (null = ascending node id).
     */
    public List<Step> contractionHierarchies(
            List<List<Integer>> graphEdges, Integer start,
            Integer target, List<Integer> order) {
        List<Step> steps = new ArrayList<>();
        List<WeightedEdge> original = parseEdges(graphEdges);
        Set<Integer> nodeSet = new TreeSet<>();
        for (WeightedEdge e : original) {
            nodeSet.add(e.u);
            nodeSet.add(e.v);
        }
        List<Integer> nodes = new ArrayList<>(nodeSet);
        if (original.isEmpty() || nodes.isEmpty()) {
            steps.add(chStep(List.of(), List.of(), List.of(),
                    List.of(), "A graph is needed.", LINE_FINAL,
                    List.of(), List.of()));
            return steps;
        }
        if (start == null || target == null
                || !nodeSet.contains(start) || !nodeSet.contains(target)) {
            steps.add(chStep(List.of(), List.of(), List.of(),
                    List.of(), "Invalid start or target node.", LINE_FINAL,
                    snapshotNodes(nodes), snapshotEdges(original)));
            return steps;
        }
        if (start.equals(target)) {
            steps.add(chStep(List.of(), List.of(), List.of(),
                    List.of(), "Start and target must be different.",
                    LINE_FINAL, snapshotNodes(nodes),
                    snapshotEdges(original)));
            return steps;
        }
        List<NodeDto> nodeDtos = snapshotNodes(nodes);
        int maxId = nodes.get(nodes.size() - 1);

        // Contraction order: the given one when it is a
        // permutation of the nodes, else ascending node id.
        List<Integer> contractOrder = validOrder(order, nodes)
                ? new ArrayList<>(order) : new ArrayList<>(nodes);

        // ---- Preprocessing ----
        // cur: unordered pair -> current weight (last write wins,
        // first-insertion order — the sim's dict). Starts as the
        // original edges, gains one entry per shortcut.
        Map<UnorderedPair, Integer> cur = new LinkedHashMap<>();
        for (WeightedEdge e : original) {
            cur.put(new UnorderedPair(e.u, e.v), e.w);
        }
        Map<Integer, Integer> rank = new HashMap<>();
        List<Shortcut> shortcuts = new ArrayList<>();
        Set<Integer> contracted = new HashSet<>();
        for (int r = 0; r < contractOrder.size(); r++) {
            int x = contractOrder.get(r);
            rank.put(x, r);
            // The witness searches run on the graph as it stands
            // BEFORE x's shortcuts are added (the sim's snapshot).
            Map<Integer, List<Neighbor>> adj = buildAdj(cur);
            List<Integer> nbrs = new ArrayList<>();
            for (Neighbor nb : adj.getOrDefault(x, List.of())) {
                if (!contracted.contains(nb.id)) {
                    nbrs.add(nb.id);
                }
            }
            Collections.sort(nbrs);
            Map<Integer, Integer> wv = new HashMap<>();
            for (Neighbor nb : adj.getOrDefault(x, List.of())) {
                wv.put(nb.id, nb.weight);
            }
            List<Shortcut> added = new ArrayList<>();
            for (int i = 0; i < nbrs.size(); i++) {
                for (int j = i + 1; j < nbrs.size(); j++) {
                    int u = nbrs.get(i);
                    int v = nbrs.get(j);
                    int via = wv.get(u) + wv.get(v);
                    int alt = witness(u, v, x, contracted, adj);
                    if (via < alt) {
                        added.add(new Shortcut(u, v, via, x, alt));
                    }
                }
            }
            // Contract step first (its edges are the list before
            // x's own shortcuts), then one step per new shortcut.
            List<EdgeDto> edgesBefore = snapshotEdges(cur);
            steps.add(chStep(ranksList(rank, maxId), List.of(), List.of(x),
                    List.of(),
                    "Contract node " + x + " (rank " + r + "): "
                            + nbrs.size()
                            + (nbrs.size() == 1 ? " neighbour, " : " neighbours, ")
                            + added.size()
                            + (added.size() == 1 ? " shortcut added." : " shortcuts added."),
                    LINE_CONTRACT, nodeDtos, edgesBefore));
            for (Shortcut sc : added) {
                cur.put(new UnorderedPair(sc.u, sc.v), sc.w);
                shortcuts.add(sc);
                String message = "Shortcut " + sc.u + "\u2013" + sc.v
                        + " (weight " + sc.w + ") via node " + sc.via + "; "
                        + (sc.alt == INF_INT
                                ? "no alternative without " + sc.via + "."
                                : "best alternative without " + sc.via
                                        + " is " + sc.alt + ".");
                steps.add(chStep(ranksList(rank, maxId), List.of(),
                        List.of(sc.u, sc.v), List.of(), message,
                        LINE_SHORTCUT, nodeDtos, snapshotEdges(cur)));
            }
            contracted.add(x);
        }

        // ---- Query: alternating upward Dijkstra ----
        Map<Integer, List<UpEdge>> up = buildUp(original, shortcuts, rank);
        List<EdgeDto> finalEdges = snapshotEdges(cur);
        Map<Integer, Integer> distF = new LinkedHashMap<>();
        Map<Integer, Integer> distB = new LinkedHashMap<>();
        Map<Integer, Parent> parentF = new HashMap<>();
        Map<Integer, Parent> parentB = new HashMap<>();
        distF.put(start, 0);
        distB.put(target, 0);
        Set<Integer> setF = new HashSet<>();
        Set<Integer> setB = new HashSet<>();
        long mu = INF;
        Integer meetNode = null;

        // Alternating upward settles, forward first. A side whose
        // frontier is empty is SKIPPED (not a loop break): the
        // other side keeps going until its own min-key >= mu.
        // (The sim's literal `if n is None: break` ends the whole
        // search when one side empties, which reports "No path"
        // for connected graphs — e.g. the default ascending order
        // on the demo graph, where the target is the highest-ranked
        // node and the backward upward frontier holds only the
        // target. The design's stop rule is per-side — "stop a
        // side when its min-key >= mu" — and the sim's own
        // full-search reference finds the path, so the empty side
        // is skipped here. The pinned demo trace is unaffected:
        // it breaks on the mu condition before either side empties.)
        while (true) {
            // ---- Forward half (upward) ----
            int[] fwd = minUnsettled(distF, setF, nodes);
            if (fwd != null) {
                int u = fwd[0];
                int du = fwd[1];
                setF.add(u);
                steps.add(chStep(distFList(distF, maxId),
                        distBList(distB, maxId), List.of(u), List.of(),
                        "Forward (upward): settle node " + u + " (dist "
                                + du + ").",
                        LINE_SETTLE_FORWARD, nodeDtos, finalEdges));
                for (UpEdge e : up.getOrDefault(u, List.of())) {
                    int nd = du + e.weight;
                    Integer old = distF.get(e.to);
                    if (old == null || nd < old) {
                        distF.put(e.to, nd);
                        parentF.put(e.to, new Parent(u, e.weight, e.via));
                        steps.add(chStep(distFList(distF, maxId),
                                distBList(distB, maxId), List.of(u, e.to),
                                List.of(),
                                "Relax " + u + " -> " + e.to + ": " + nd
                                        + ".",
                                LINE_RELAX_FORWARD, nodeDtos, finalEdges));
                    }
                }
                MuResult muUpdate = updateMu(distF, distB, mu, steps,
                        nodeDtos, finalEdges, maxId);
                mu = muUpdate.mu;
                if (muUpdate.meetNode != null) {
                    meetNode = muUpdate.meetNode;
                }
                long mf = minUnsettledDist(distF, setF, nodes);
                long mb = minUnsettledDist(distB, setB, nodes);
                if (mf >= mu && mb >= mu) {
                    break;
                }
            }
            // ---- Backward half (upward) ----
            int[] bwd = minUnsettled(distB, setB, nodes);
            if (bwd != null) {
                int v = bwd[0];
                int dv = bwd[1];
                setB.add(v);
                steps.add(chStep(distFList(distF, maxId),
                        distBList(distB, maxId), List.of(v), List.of(),
                        "Backward (upward): settle node " + v + " (dist "
                                + dv + ").",
                        LINE_SETTLE_BACKWARD, nodeDtos, finalEdges));
                for (UpEdge e : up.getOrDefault(v, List.of())) {
                    int nd = dv + e.weight;
                    Integer old = distB.get(e.to);
                    if (old == null || nd < old) {
                        distB.put(e.to, nd);
                        parentB.put(e.to, new Parent(v, e.weight, e.via));
                        steps.add(chStep(distFList(distF, maxId),
                                distBList(distB, maxId), List.of(v, e.to),
                                List.of(),
                                "Relax " + v + " -> " + e.to + ": " + nd
                                        + ".",
                                LINE_RELAX_BACKWARD, nodeDtos, finalEdges));
                    }
                }
                MuResult muUpdate = updateMu(distF, distB, mu, steps,
                        nodeDtos, finalEdges, maxId);
                mu = muUpdate.mu;
                if (muUpdate.meetNode != null) {
                    meetNode = muUpdate.meetNode;
                }
                long mf = minUnsettledDist(distF, setF, nodes);
                long mb = minUnsettledDist(distB, setB, nodes);
                if (mf >= mu && mb >= mu) {
                    break;
                }
            }
            if (fwd == null && bwd == null) {
                break;
            }
        }

        if (meetNode == null) {
            steps.add(chStep(distFList(distF, maxId),
                    distBList(distB, maxId), List.of(), List.of(),
                    "No path from " + start + " to " + target + ".",
                    LINE_FINAL, nodeDtos, finalEdges));
            return steps;
        }
        List<Integer> path = unpackPath(parentF, parentB, start,
                target, meetNode, shortcuts);
        int settled = setF.size() + setB.size();
        steps.add(chStep(distFList(distF, maxId),
                distBList(distB, maxId), List.of(),
                new ArrayList<>(path),
                "Shortest path: " + pathText(path) + ", length " + mu
                        + " (unpacked from shortcuts). CH settled "
                        + settled + " nodes.",
                LINE_FINAL, nodeDtos, finalEdges));
        return steps;
    }

    /** The result of a query mu update: the best length and its meeting node (null when unchanged). */
    private static class MuResult {
        final long mu;
        final Integer meetNode;

        MuResult(long mu, Integer meetNode) {
            this.mu = mu;
            this.meetNode = meetNode;
        }
    }

    /**
     * Mu update for the query: the minimum over nodes with both
     * distances known of distF + distB (distF insertion order,
     * the sim's dict order). Each improvement records a mu step
     * (line 10) with the meeting node in sorted. Returns the
     * new mu and the meeting node (null when unchanged).
     */
    private MuResult updateMu(Map<Integer, Integer> distF,
                              Map<Integer, Integer> distB, long mu,
                              List<Step> steps, List<NodeDto> nodeDtos,
                              List<EdgeDto> edges, int maxId) {
        long best = mu;
        Integer bestNode = null;
        for (Map.Entry<Integer, Integer> entry : distF.entrySet()) {
            int v = entry.getKey();
            Integer db = distB.get(v);
            if (db == null) {
                continue;
            }
            long c = (long) entry.getValue() + db;
            if (c < best) {
                best = c;
                bestNode = v;
                steps.add(chStep(distFList(distF, maxId),
                        distBList(distB, maxId), List.of(v),
                        List.of(v),
                        "Meeting node " + v + ": " + entry.getValue()
                                + " + " + db + " = " + best + ".",
                        LINE_MU, nodeDtos, edges));
            }
        }
        return new MuResult(best, bestNode);
    }

    /**
     * Unpacks the shortcuts on the two parent chains into the
     * real path: the forward chain from the start to the meeting
     * node, then the backward chain from the target back to the
     * meeting node, reversed.
     */
    private List<Integer> unpackPath(Map<Integer, Parent> parentF,
                                     Map<Integer, Parent> parentB,
                                     int start, int target,
                                     int meetNode,
                                     List<Shortcut> shortcuts) {
        // Unordered pair -> via node (last shortcut wins, the sim's
        // dict comprehension).
        Map<UnorderedPair, Integer> expand = new HashMap<>();
        for (Shortcut sc : shortcuts) {
            expand.put(new UnorderedPair(sc.u, sc.v), sc.via);
        }
        List<Integer> fwd = unpackChain(parentF, start, meetNode, expand);
        List<Integer> bwd = unpackChain(parentB, target, meetNode, expand);
        Collections.reverse(bwd);
        List<Integer> path = new ArrayList<>(fwd);
        path.addAll(bwd.subList(1, bwd.size()));
        return path;
    }

    /** One parent chain, unpacked: [start, ..., end]. */
    private List<Integer> unpackChain(Map<Integer, Parent> parent,
                                      int start, int end,
                                      Map<UnorderedPair, Integer> expand) {
        List<Parent> chain = new ArrayList<>();
        List<Integer> nodes = new ArrayList<>();
        int x = end;
        while (x != start) {
            Parent p = parent.get(x);
            if (p == null) {
                break;
            }
            chain.add(p);
            nodes.add(x);
            x = p.node;
        }
        Collections.reverse(chain);
        Collections.reverse(nodes);
        List<Integer> out = new ArrayList<>();
        out.add(start);
        for (int i = 0; i < chain.size(); i++) {
            expandEdge(chain.get(i).node, nodes.get(i),
                    chain.get(i).via, expand, out);
        }
        return out;
    }

    /** Expands one (possibly shortcut) edge into real nodes. */
    private void expandEdge(int a, int b, Integer via,
                            Map<UnorderedPair, Integer> expand,
                            List<Integer> out) {
        if (via == null) {
            out.add(b);
            return;
        }
        expandEdge(a, via, expand.get(new UnorderedPair(a, via)), expand,
                out);
        expandEdge(via, b, expand.get(new UnorderedPair(via, b)), expand,
                out);
    }

    /** "0 → 2 → 1" rendering of a path. */
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
     * The witness search: the shortest u–v distance on the
     * snapshot graph with x and all already-contracted nodes
     * removed (INF when no such path exists). Ties break by
     * smallest (dist, node id).
     */
    private int witness(int from, int to, int excluded,
                        Set<Integer> contracted,
                        Map<Integer, List<Neighbor>> adj) {
        Map<Integer, Integer> dist = new HashMap<>();
        dist.put(from, 0);
        Set<Integer> done = new HashSet<>();
        while (true) {
            Integer bestNode = null;
            int bestDist = INF_INT;
            for (int node : new TreeSet<>(dist.keySet())) {
                if (done.contains(node)) {
                    continue;
                }
                int d = dist.get(node);
                if (d < bestDist) {
                    bestDist = d;
                    bestNode = node;
                }
            }
            if (bestNode == null) {
                break;
            }
            if (bestNode == to) {
                return bestDist;
            }
            done.add(bestNode);
            for (Neighbor nb : adj.getOrDefault(bestNode, List.of())) {
                if (nb.id == excluded || contracted.contains(nb.id)) {
                    continue;
                }
                int nd = bestDist + nb.weight;
                Integer old = dist.get(nb.id);
                if (old == null || nd < old) {
                    dist.put(nb.id, nd);
                }
            }
        }
        return dist.getOrDefault(to, INF_INT);
    }

    /**
     * The upward adjacency: for each original edge and each
     * shortcut, the edge from the lower-ranked end to the
     * higher-ranked end (the sim builds it from the original
     * edge list plus the shortcuts, each in order). Neighbours
     * are sorted by (target, weight).
     */
    private Map<Integer, List<UpEdge>> buildUp(
            List<WeightedEdge> original, List<Shortcut> shortcuts,
            Map<Integer, Integer> rank) {
        Map<Integer, List<UpEdge>> up = new HashMap<>();
        for (WeightedEdge e : original) {
            if (rank.get(e.u) < rank.get(e.v)) {
                up.computeIfAbsent(e.u, k -> new ArrayList<>())
                        .add(new UpEdge(e.v, e.w, null));
            }
            if (rank.get(e.v) < rank.get(e.u)) {
                up.computeIfAbsent(e.v, k -> new ArrayList<>())
                        .add(new UpEdge(e.u, e.w, null));
            }
        }
        for (Shortcut sc : shortcuts) {
            if (rank.get(sc.u) < rank.get(sc.v)) {
                up.computeIfAbsent(sc.u, k -> new ArrayList<>())
                        .add(new UpEdge(sc.v, sc.w, sc.via));
            } else {
                up.computeIfAbsent(sc.v, k -> new ArrayList<>())
                        .add(new UpEdge(sc.u, sc.w, sc.via));
            }
        }
        for (List<UpEdge> neighbours : up.values()) {
            neighbours.sort(Comparator.comparingInt((UpEdge e) -> e.to)
                    .thenComparingInt(e -> e.weight));
        }
        return up;
    }

    /** The snapshot graph: undirected adjacency from the current edge map. */
    private Map<Integer, List<Neighbor>> buildAdj(
            Map<UnorderedPair, Integer> cur) {
        Map<Integer, List<Neighbor>> adj = new HashMap<>();
        for (Map.Entry<UnorderedPair, Integer> entry : cur.entrySet()) {
            UnorderedPair pair = entry.getKey();
            int w = entry.getValue();
            adj.computeIfAbsent(pair.lo, k -> new ArrayList<>())
                    .add(new Neighbor(pair.hi, w));
            adj.computeIfAbsent(pair.hi, k -> new ArrayList<>())
                    .add(new Neighbor(pair.lo, w));
        }
        for (List<Neighbor> neighbours : adj.values()) {
            neighbours.sort(Comparator.comparingInt((Neighbor n) -> n.id)
                    .thenComparingInt(n -> n.weight));
        }
        return adj;
    }

    /** True when the given order is a permutation of the nodes. */
    private boolean validOrder(List<Integer> order, List<Integer> nodes) {
        if (order == null || order.size() != nodes.size()) {
            return false;
        }
        Set<Integer> seen = new HashSet<>();
        for (Integer node : order) {
            if (node == null || !seen.add(node)) {
                return false;
            }
        }
        return seen.equals(new HashSet<>(nodes));
    }

    /**
     * The unsettled node with the smallest known distance
     * (ties: smallest node id). Returns [node, dist], or null
     * when nothing is left unsettled.
     */
    private int[] minUnsettled(Map<Integer, Integer> dist,
                               Set<Integer> settled,
                               List<Integer> nodes) {
        Integer bestNode = null;
        int bestDist = Integer.MAX_VALUE;
        for (int node : nodes) {
            if (settled.contains(node)) {
                continue;
            }
            Integer d = dist.get(node);
            if (d != null && d < bestDist) {
                bestDist = d;
                bestNode = node;
            }
        }
        return bestNode == null ? null : new int[]{bestNode, bestDist};
    }

    /** The smallest unsettled distance, or INF when none is left. */
    private long minUnsettledDist(Map<Integer, Integer> dist,
                                  Set<Integer> settled,
                                  List<Integer> nodes) {
        int[] min = minUnsettled(dist, settled, nodes);
        return min == null ? INF : min[1];
    }

    /** The ranks array as a node-indexed list (-1 = unranked). */
    private List<Integer> ranksList(Map<Integer, Integer> rank,
                                    int maxId) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i <= maxId; i++) {
            out.add(rank.getOrDefault(i, -1));
        }
        return out;
    }

    /** distF as a node-indexed list (-1 = unknown). */
    private List<Integer> distFList(Map<Integer, Integer> distF,
                                    int maxId) {
        return distList(distF, maxId);
    }

    /** distB as a node-indexed list (-1 = unknown). */
    private List<Integer> distBList(Map<Integer, Integer> distB,
                                    int maxId) {
        return distList(distB, maxId);
    }

    /** A distance map as a node-indexed list (-1 = unknown). */
    private List<Integer> distList(Map<Integer, Integer> dist,
                                   int maxId) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i <= maxId; i++) {
            out.add(dist.getOrDefault(i, -1));
        }
        return out;
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

    /** The current edge map as EdgeDtos (insertion order, with weights). */
    private List<EdgeDto> snapshotEdges(Map<UnorderedPair, Integer> cur) {
        List<EdgeDto> dtos = new ArrayList<>();
        for (Map.Entry<UnorderedPair, Integer> entry : cur.entrySet()) {
            dtos.add(new EdgeDto(entry.getKey().lo, entry.getKey().hi,
                    entry.getValue()));
        }
        return dtos;
    }

    /** The original edge list as EdgeDtos (input order, with weights). */
    private List<EdgeDto> snapshotEdges(List<WeightedEdge> edges) {
        List<EdgeDto> dtos = new ArrayList<>();
        for (WeightedEdge e : edges) {
            dtos.add(new EdgeDto(e.u, e.v, e.w));
        }
        return dtos;
    }

    /** One CH Step: data in data, aux in aux. */
    private Step chStep(List<Integer> data, List<Integer> aux,
                        List<Integer> highlight, List<Integer> sorted,
                        String message, int line, List<NodeDto> nodes,
                        List<EdgeDto> edges) {
        Step step = new Step(new ArrayList<>(data), highlight,
                List.of(), new ArrayList<>(sorted), message, line,
                nodes, edges);
        step.setAux(new ArrayList<>(aux));
        return step;
    }
}
