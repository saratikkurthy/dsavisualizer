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
 * Stoer-Wagner global minimum cut (Phase 31, Graphs v3):
 * repeatedly grow a set A from the start vertex, always
 * adding the outside vertex most strongly connected to A
 * (ties go to the smallest supernode position); the last
 * vertex added defines a cut, then the last two vertices
 * added are merged into one supernode and the phase
 * repeats. The smallest cut over all phases is the global
 * minimum cut.
 *
 * <p>Supernodes are kept as a list of member sets (the
 * sim's structure, {@code ~/workspace/.tmp/p31-sim/sims.py}):
 * the merged supernode goes to the END of the list, edge
 * weights between supernodes are summed, and tie-breaking
 * uses the supernode POSITION (not the node id). Node ids
 * in the output are the smallest member id of each
 * supernode; a merged supernode's {@code label} is the
 * comma-joined member ids (e.g. "2,3") and its
 * {@code value} is the smallest member id. Singleton
 * supernodes carry no label (the Phase 29 lesson — graph
 * services never call setLabel for plain nodes).</p>
 *
 * <p>Step shape: build (line 2, {@code data} = [number of
 * vertices]) + one Step per vertex added to A, INCLUDING
 * the seed vertex (line 3, {@code highlight} = [added
 * vertex id], {@code data} = [weight of the added vertex
 * to A] — 0 for the seed, {@code sorted} = A after the
 * addition) + one Step per phase cut (line 4, {@code data}
 * = [phase cut weight], {@code sorted} = A at phase end,
 * {@code highlight} = [last vertex added]) + final (line
 * 0, {@code data} = [minCut], {@code sorted} = the best
 * partition's smaller side — ties go to the side
 * containing the smallest node id). {@code nodes} /
 * {@code edges} show the current supernode graph
 * (circular layout by supernode position, edges with
 * summed weights).</p>
 *
 * <p>Demo (undirected), 4 nodes, edges: 0-1:3, 0-2:2,
 * 1-2:4, 1-3:2, 2-3:3. Phase 1: A grows 0, 1, 2, 3; cut
 * = 5 (last vertex 3, rest {0,1,2}). Phase 2 (merge
 * {2,3}): A grows 0, 1, {2,3}; cut = 8. Phase 3 (merge
 * {1,2,3}): A grows 0, {1,2,3}; cut = 5. Min cut = 5,
 * best partition {3} | {0,1,2} (the first phase achieving
 * it). Trace: 1 build + (4 + 1) + (3 + 1) + (2 + 1) + 1
 * final = 14 steps.</p>
 *
 * <p>The controller validates the input (undirected edges,
 * weights 1..99, 2..12 edges, node ids 0..9, graph
 * connected); this method guards against bad input anyway
 * with a single "empty" Step.</p>
 */
@Service
public class StoerWagnerService {

    private static final int LINE_BUILD = 2;
    private static final int LINE_ADD = 3;
    private static final int LINE_CUT = 4;
    private static final int LINE_FINAL = 0;

    /** Stoer-Wagner global min cut on the undirected edge list. */
    public List<Step> stoerWagner(List<List<Integer>> graphEdges) {
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
        steps.add(new Step(List.of(nodes.size()), List.of(), List.of(),
                List.of(),
                "Stoer-Wagner: " + nodes.size() + " vertices, each "
                        + "alone in its own set.",
                LINE_BUILD, snapshot(nodes, edges), snapshotEdges(edges)));

        // Supernodes as a list of member sets (positions matter
        // for tie-breaking); edges as (position, position,
        // weight) with parallel edges summed.
        List<Set<Integer>> superNodes = new ArrayList<>();
        for (int v : nodes) {
            superNodes.add(new TreeSet<>(Set.of(v)));
        }
        List<int[]> superEdges = aggregate(nodes, superNodes, edges);

        int bestCut = Integer.MAX_VALUE;
        List<Integer> bestSide = List.of();
        int phase = 0;
        while (superNodes.size() > 1) {
            phase++;
            int m = superNodes.size();
            // Grow A from position 0 (the supernode holding
            // the smallest node id — it is never merged until
            // the last phase, so it stays at position 0).
            List<Integer> inA = new ArrayList<>();
            boolean[] inASet = new boolean[m];
            int[] wsum = new int[m];
            inA.add(0);
            inASet[0] = true;
            for (int[] e : superEdges) {
                if (e[0] == 0) {
                    wsum[e[1]] += e[2];
                }
                if (e[1] == 0) {
                    wsum[e[0]] += e[2];
                }
            }
            steps.add(new Step(List.of(0), List.of(smallest(superNodes, 0)),
                    List.of(), List.of(smallest(superNodes, 0)),
                    "Phase " + phase + ": start with vertex "
                            + smallest(superNodes, 0) + " in A.",
                    LINE_ADD, snapshotSuper(superNodes, superEdges),
                    snapshotEdges(superEdges, superNodes)));
            while (inA.size() < m) {
                int next = -1;
                int nextWeight = -1;
                for (int i = 0; i < m; i++) {
                    if (inASet[i]) {
                        continue;
                    }
                    if (wsum[i] > nextWeight) {
                        nextWeight = wsum[i];
                        next = i;
                    }
                }
                inA.add(next);
                inASet[next] = true;
                for (int[] e : superEdges) {
                    if (e[0] == next && !inASet[e[1]]) {
                        wsum[e[1]] += e[2];
                    }
                    if (e[1] == next && !inASet[e[0]]) {
                        wsum[e[0]] += e[2];
                    }
                }
                steps.add(new Step(List.of(nextWeight),
                        List.of(smallest(superNodes, next)), List.of(),
                        aIds(superNodes, inA),
                        "Add vertex " + smallest(superNodes, next)
                                + " to A (weight " + nextWeight + ").",
                        LINE_ADD, snapshotSuper(superNodes, superEdges),
                        snapshotEdges(superEdges, superNodes)));
            }
            int lastPos = inA.get(inA.size() - 1);
            int secondLastPos = inA.get(inA.size() - 2);
            int cut = wsum[lastPos];
            List<Integer> rest = new ArrayList<>();
            for (int i = 0; i < inA.size() - 1; i++) {
                rest.addAll(superNodes.get(inA.get(i)));
            }
            List<Integer> lastMembers = new ArrayList<>(
                    superNodes.get(lastPos));
            steps.add(new Step(List.of(cut),
                    List.of(smallest(superNodes, lastPos)), List.of(),
                    aIds(superNodes, inA),
                    "Phase " + phase + " cut = " + cut + " (last vertex "
                            + membersLabel(superNodes.get(lastPos))
                            + ", other side " + rest + ").",
                    LINE_CUT, snapshotSuper(superNodes, superEdges),
                    snapshotEdges(superEdges, superNodes)));
            if (cut < bestCut) {
                bestCut = cut;
                bestSide = lastMembers;
            }
            // Merge the last two vertices added (s, t) into one
            // supernode, appended at the end of the list.
            Set<Integer> merged = new TreeSet<>(
                    superNodes.get(secondLastPos));
            merged.addAll(superNodes.get(lastPos));
            List<Set<Integer>> nextNodes = new ArrayList<>();
            Map<Integer, Integer> remap = new HashMap<>();
            for (int i = 0; i < m; i++) {
                if (i == secondLastPos || i == lastPos) {
                    continue;
                }
                remap.put(i, nextNodes.size());
                nextNodes.add(superNodes.get(i));
            }
            remap.put(secondLastPos, nextNodes.size());
            remap.put(lastPos, nextNodes.size());
            nextNodes.add(merged);
            superNodes = nextNodes;
            superEdges = aggregatePositions(superEdges, remap,
                    nextNodes.size());
        }
        // Best partition: the smaller side (ties go to the side
        // holding the smallest node id).
        List<Integer> allMembers = new ArrayList<>();
        for (Set<Integer> s : superNodes) {
            allMembers.addAll(s);
        }
        List<Integer> otherSide = new ArrayList<>(allMembers);
        otherSide.removeAll(bestSide);
        List<Integer> smaller = bestSide.size() <= otherSide.size()
                ? bestSide : otherSide;
        if (bestSide.size() == otherSide.size()
                && !otherSide.isEmpty()
                && otherSide.get(0) < bestSide.get(0)) {
            smaller = otherSide;
        }
        steps.add(new Step(List.of(bestCut), List.of(), List.of(),
                new ArrayList<>(smaller),
                "Minimum cut = " + bestCut + ": " + bestSide + " | "
                        + otherSide + ".",
                LINE_FINAL, snapshotSuper(superNodes, superEdges),
                snapshotEdges(superEdges, superNodes)));
        return steps;
    }

    /**
     * Valid undirected edges in input order: [u, v, weight],
     * weight defaulting to 1 for 2-element edges (the
     * Kruskal parse rule). Self-loops are dropped — they
     * carry no cut.
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
            if (u == null || v == null || u.equals(v)) {
                continue;
            }
            int w = pair.size() == 3 && pair.get(2) != null
                    ? pair.get(2) : 1;
            edges.add(new int[]{u, v, w});
        }
        return edges;
    }

    /**
     * Aggregates the input edges between the current
     * supernodes: (position, position, summed weight),
     * sorted by (min position, max position).
     */
    private List<int[]> aggregate(List<Integer> nodes,
                                  List<Set<Integer>> superNodes,
                                  List<int[]> edges) {
        Map<Integer, Integer> pos = new HashMap<>();
        for (int i = 0; i < superNodes.size(); i++) {
            for (int v : superNodes.get(i)) {
                pos.put(v, i);
            }
        }
        Map<String, Integer> sums = new HashMap<>();
        for (int[] e : edges) {
            int a = pos.get(e[0]);
            int b = pos.get(e[1]);
            if (a == b) {
                continue;
            }
            String key = Math.min(a, b) + ":" + Math.max(a, b);
            sums.merge(key, e[2], Integer::sum);
        }
        List<int[]> out = new ArrayList<>();
        List<String> keys = new ArrayList<>(sums.keySet());
        keys.sort((x, y) -> {
            String[] px = x.split(":");
            String[] py = y.split(":");
            int c = Integer.compare(Integer.parseInt(px[0]),
                    Integer.parseInt(py[0]));
            return c != 0 ? c : Integer.compare(
                    Integer.parseInt(px[1]), Integer.parseInt(py[1]));
        });
        for (String key : keys) {
            String[] p = key.split(":");
            out.add(new int[]{Integer.parseInt(p[0]),
                    Integer.parseInt(p[1]), sums.get(key)});
        }
        return out;
    }

    /** Re-aggregates position edges after a merge (remap: old -> new). */
    private List<int[]> aggregatePositions(List<int[]> edges,
                                           Map<Integer, Integer> remap,
                                           int size) {
        Map<String, Integer> sums = new HashMap<>();
        for (int[] e : edges) {
            int a = remap.get(e[0]);
            int b = remap.get(e[1]);
            if (a == b) {
                continue;
            }
            String key = Math.min(a, b) + ":" + Math.max(a, b);
            sums.merge(key, e[2], Integer::sum);
        }
        List<int[]> out = new ArrayList<>();
        List<String> keys = new ArrayList<>(sums.keySet());
        keys.sort((x, y) -> {
            String[] px = x.split(":");
            String[] py = y.split(":");
            int c = Integer.compare(Integer.parseInt(px[0]),
                    Integer.parseInt(py[0]));
            return c != 0 ? c : Integer.compare(
                    Integer.parseInt(px[1]), Integer.parseInt(py[1]));
        });
        for (String key : keys) {
            String[] p = key.split(":");
            out.add(new int[]{Integer.parseInt(p[0]),
                    Integer.parseInt(p[1]), sums.get(key)});
        }
        return out;
    }

    /** Smallest member id of the supernode at position {@code i}. */
    private int smallest(List<Set<Integer>> superNodes, int i) {
        return superNodes.get(i).iterator().next();
    }

    /** Member ids of a set, comma-joined (e.g. "2,3"). */
    private String membersLabel(Set<Integer> members) {
        StringBuilder sb = new StringBuilder();
        for (int v : members) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(v);
        }
        return sb.toString();
    }

    /** A-set positions as node ids (smallest member id each). */
    private List<Integer> aIds(List<Set<Integer>> superNodes,
                               List<Integer> positions) {
        List<Integer> out = new ArrayList<>();
        for (int p : positions) {
            out.add(smallest(superNodes, p));
        }
        return out;
    }

    /** Snapshot of the initial graph (singleton supernodes). */
    private List<NodeDto> snapshot(List<Integer> nodes,
                                   List<int[]> edges) {
        List<Set<Integer>> singles = new ArrayList<>();
        for (int v : nodes) {
            singles.add(new TreeSet<>(Set.of(v)));
        }
        return snapshotSuper(singles, edgesToPositions(nodes, edges));
    }

    /** Converts input edges to position edges over the node list. */
    private List<int[]> edgesToPositions(List<Integer> nodes,
                                         List<int[]> edges) {
        Map<Integer, Integer> pos = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            pos.put(nodes.get(i), i);
        }
        List<int[]> out = new ArrayList<>();
        for (int[] e : edges) {
            out.add(new int[]{pos.get(e[0]), pos.get(e[1]), e[2]});
        }
        return out;
    }

    /**
     * Snapshot of the current supernode graph: one node per
     * supernode (circular layout by position, id = value =
     * smallest member id, label = comma-joined members when
     * merged), edges with summed weights.
     */
    private List<NodeDto> snapshotSuper(List<Set<Integer>> superNodes,
                                   List<int[]> superEdges) {
        List<NodeDto> dtos = new ArrayList<>();
        int n = superNodes.size();
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            double x = 280 + 200 * Math.cos(angle);
            double y = 160 + 120 * Math.sin(angle);
            int id = smallest(superNodes, i);
            NodeDto dto = new NodeDto(id, id, x, y);
            if (superNodes.get(i).size() > 1) {
                dto.setLabel(membersLabel(superNodes.get(i)));
            }
            dtos.add(dto);
        }
        return dtos;
    }

    /** Supernode edges as EdgeDtos (from/to = smallest member ids). */
    private List<EdgeDto> snapshotEdges(List<int[]> superEdges,
                                        List<Set<Integer>> superNodes) {
        List<EdgeDto> dtos = new ArrayList<>();
        for (int[] e : superEdges) {
            dtos.add(new EdgeDto(smallest(superNodes, e[0]),
                    smallest(superNodes, e[1]), e[2]));
        }
        return dtos;
    }

    /** Initial-graph edges as EdgeDtos (input order, weight kept). */
    private List<EdgeDto> snapshotEdges(List<int[]> edges) {
        List<EdgeDto> dtos = new ArrayList<>();
        for (int[] e : edges) {
            dtos.add(new EdgeDto(e[0], e[1], e[2]));
        }
        return dtos;
    }
}
