package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Centroid decomposition (Phase 25, advanced trees): the
 * tree trick that turns "is there a pair at distance k?"
 * into a divide-and-conquer over centroids — the idea
 * behind counting paths in a tree in O(n log n).
 *
 * <p>Repeatedly pick the centroid of the current component
 * (the node whose removal leaves no component bigger than
 * half the nodes; ties go to the lowest node id), record it
 * as the centroid-tree parent of the centroids of the pieces
 * it splits off, and recurse on each piece in the adjacency
 * order of the centroid's neighbours. Demo tree (same as
 * the HLD demo): 0:[1,2], 1:[0,3,4], 2:[0,5], 3:[1],
 * 4:[1,6,7], 5:[2], 6:[4], 7:[4]. Centroid picks, in
 * decomposition order: <b>1</b> (level 0; removing it leaves
 * max component 3), <b>2</b> (level 1; of {0,2,5}), <b>0</b>
 * (level 2), <b>5</b> (level 2), <b>3</b> (level 1),
 * <b>4</b> (level 1; of {4,6,7}), <b>6</b> (level 2),
 * <b>7</b> (level 2). Levels: {1:0, 2:1, 3:1, 4:1, 0:2,
 * 5:2, 6:2, 7:2}. Centroid parents: {2:1, 3:1, 4:1, 0:2,
 * 5:2, 6:4, 7:4}. 8 pick steps + final = 9 steps.</p>
 *
 * <p>Pick step: {@code highlight} = [centroid], {@code data}
 * = [level, parent or -1], {@code sorted} = [pick order so
 * far], {@code nodes} value = level once picked else -1,
 * {@code edges} = the centroid-tree edges built so far
 * (parent&rarr;child). Final step {@code data} = [2] (max
 * level), {@code sorted} = the full pick order
 * [1,2,0,5,3,4,6,7]. {@code nodes} are in id order,
 * labelled with the node id, x/y = 0 (the frontend lays
 * the tree out). A disconnected input is decomposed
 * component by component, lowest-id component first.</p>
 *
 * <p>Bad input (a missing/empty edge list) yields a single
 * "empty" Step; the controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class CentroidService {

    private static final int LINE_PICK = 2;
    private static final int LINE_FINAL = 0;

    /** Runs centroid decomposition on the tree given by {@code graphEdges}. */
    public List<Step> centroid(List<List<Integer>> graphEdges) {
        List<Step> steps = new ArrayList<>();
        if (graphEdges == null || graphEdges.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Tree edges are needed as "
                            + "[u, v] pairs.", LINE_FINAL));
            return steps;
        }
        Map<Integer, List<Integer>> adj = new LinkedHashMap<>();
        Set<Integer> nodeSet = new TreeSet<>();
        for (List<Integer> edge : graphEdges) {
            if (edge == null || edge.size() < 2
                    || edge.get(0) == null || edge.get(1) == null) {
                continue;
            }
            int u = edge.get(0);
            int v = edge.get(1);
            adj.computeIfAbsent(u, k -> new ArrayList<>());
            adj.computeIfAbsent(v, k -> new ArrayList<>());
            if (!adj.get(u).contains(v)) {
                adj.get(u).add(v);
            }
            if (!adj.get(v).contains(u)) {
                adj.get(v).add(u);
            }
            nodeSet.add(u);
            nodeSet.add(v);
        }
        if (nodeSet.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Tree edges are needed as "
                            + "[u, v] pairs.", LINE_FINAL));
            return steps;
        }
        List<Integer> nodeIds = new ArrayList<>(nodeSet);
        Map<Integer, Integer> level = new HashMap<>();
        Map<Integer, Integer> centroidParent = new HashMap<>();
        List<Integer> pickOrder = new ArrayList<>();
        List<EdgeDto> centroidEdges = new ArrayList<>();

        // Decompose every component (a tree has one), lowest-id first.
        Set<Integer> remaining = new HashSet<>(nodeSet);
        while (!remaining.isEmpty()) {
            int seed = remaining.stream().min(Integer::compareTo)
                    .orElseThrow();
            Set<Integer> component = component(seed, remaining, adj);
            decompose(component, 0, -1, adj, level,
                    centroidParent, pickOrder, centroidEdges,
                    steps, nodeIds);
            remaining.removeAll(component);
        }
        int maxLevel = level.values().stream()
                .mapToInt(Integer::intValue).max().orElse(0);
        steps.add(new Step(
                List.of(maxLevel), List.of(), List.of(),
                new ArrayList<>(pickOrder),
                "Centroid decomposition done: " + pickOrder.size()
                        + " centroids, max level " + maxLevel
                        + ", pick order " + pickOrder + ".",
                LINE_FINAL, snapshot(nodeIds, level),
                new ArrayList<>(centroidEdges)));
        return steps;
    }

    /** Picks the centroid of the component, emits its step, and recurses on the pieces. */
    private void decompose(Set<Integer> component, int levelValue,
                           int parentCentroid,
                           Map<Integer, List<Integer>> adj,
                           Map<Integer, Integer> level,
                           Map<Integer, Integer> centroidParent,
                           List<Integer> pickOrder,
                           List<EdgeDto> centroidEdges,
                           List<Step> steps,
                           List<Integer> nodeIds) {
        if (component.isEmpty()) {
            return;
        }
        int centroid = findCentroid(component, adj);
        level.put(centroid, levelValue);
        centroidParent.put(centroid, parentCentroid);
        pickOrder.add(centroid);
        if (parentCentroid >= 0) {
            centroidEdges.add(new EdgeDto(parentCentroid, centroid,
                    null));
        }
        steps.add(new Step(
                List.of(levelValue, parentCentroid),
                List.of(centroid), List.of(),
                new ArrayList<>(pickOrder),
                "Centroid " + centroid + " at level " + levelValue
                        + (parentCentroid < 0
                        ? " (root of the centroid tree)."
                        : ", centroid parent " + parentCentroid
                                + "."),
                LINE_PICK, snapshot(nodeIds, level),
                new ArrayList<>(centroidEdges)));
        Set<Integer> rest = new HashSet<>(component);
        rest.remove(centroid);
        // Pieces in the adjacency order of the centroid's neighbours.
        Set<Integer> seen = new HashSet<>();
        for (int neighbour : adj.getOrDefault(centroid,
                List.of())) {
            if (!rest.contains(neighbour)
                    || seen.contains(neighbour)) {
                continue;
            }
            Set<Integer> piece = component(neighbour, rest, adj);
            seen.addAll(piece);
            decompose(piece, levelValue + 1, centroid, adj, level,
                    centroidParent, pickOrder, centroidEdges,
                    steps, nodeIds);
        }
    }

    /** The connected component of seed within the given node set. */
    private Set<Integer> component(int seed, Set<Integer> allowed,
                                   Map<Integer, List<Integer>> adj) {
        Set<Integer> component = new HashSet<>();
        List<Integer> stack = new ArrayList<>();
        stack.add(seed);
        while (!stack.isEmpty()) {
            int node = stack.remove(stack.size() - 1);
            if (!component.add(node)) {
                continue;
            }
            for (int neighbour : adj.getOrDefault(node, List.of())) {
                if (allowed.contains(neighbour)
                        && !component.contains(neighbour)) {
                    stack.add(neighbour);
                }
            }
        }
        return component;
    }

    /** The centroid: the node minimising the largest remaining piece (ties: lowest id). */
    private int findCentroid(Set<Integer> component,
                             Map<Integer, List<Integer>> adj) {
        int root = component.stream().min(Integer::compareTo)
                .orElseThrow();
        Map<Integer, Integer> parent = new HashMap<>();
        Map<Integer, Integer> size = new HashMap<>();
        List<Integer> order = new ArrayList<>();
        parent.put(root, -1);
        List<Integer> stack = new ArrayList<>();
        stack.add(root);
        while (!stack.isEmpty()) {
            int node = stack.remove(stack.size() - 1);
            order.add(node);
            for (int neighbour : adj.getOrDefault(node, List.of())) {
                if (component.contains(neighbour)
                        && !parent.containsKey(neighbour)) {
                    parent.put(neighbour, node);
                    stack.add(neighbour);
                }
            }
        }
        for (int i = order.size() - 1; i >= 0; i--) {
            int node = order.get(i);
            int total = 1;
            for (int neighbour : adj.getOrDefault(node, List.of())) {
                if (component.contains(neighbour)
                        && parent.getOrDefault(neighbour, -2) == node) {
                    total += size.get(neighbour);
                }
            }
            size.put(node, total);
        }
        int n = component.size();
        int best = -1;
        int bestMax = Integer.MAX_VALUE;
        for (int node : new TreeSet<>(component)) {
            int maxPiece = n - size.get(node);
            for (int neighbour : adj.getOrDefault(node, List.of())) {
                if (component.contains(neighbour)
                        && parent.getOrDefault(neighbour, -2) == node) {
                    maxPiece = Math.max(maxPiece, size.get(neighbour));
                }
            }
            if (maxPiece < bestMax) {
                bestMax = maxPiece;
                best = node;
            }
        }
        return best;
    }

    /** One NodeDto per node in id order; value = level once picked, else -1. */
    private List<NodeDto> snapshot(List<Integer> nodeIds,
                                   Map<Integer, Integer> level) {
        List<NodeDto> dtos = new ArrayList<>();
        for (int id : nodeIds) {
            NodeDto dto = new NodeDto(id,
                    level.getOrDefault(id, -1), 0, 0);
            dto.setLabel(String.valueOf(id));
            dtos.add(dto);
        }
        return dtos;
    }
}
