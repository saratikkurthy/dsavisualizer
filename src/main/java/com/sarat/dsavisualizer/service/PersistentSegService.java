package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent segment tree (Phase 25, advanced trees): the
 * versioned segment tree — every update clones only the
 * O(log n) nodes on the path to the changed leaf, so every
 * past version stays queryable. The idea behind "k-th
 * smallest in a range" and range queries over history.
 *
 * <p>Node numbering (a design gap the design leaves open):
 * nodes are numbered in <b>preorder creation order</b> — a
 * node is created before its children, so the build root is
 * id 0 and ids grow down the tree. Demo array [3,1,4,1,5]
 * (n=5). Build: 9 nodes (2n&minus;1), v0 sum 14. Update 1:
 * index 2, 4&rarr;9 — 3 new nodes (path root&rarr;[0,2]
 * &rarr;[2,2]), v1 sum 19, v0 unchanged 14. Update 2:
 * index 0, 3&rarr;7 — 4 new nodes (path root&rarr;[0,2]
 * &rarr;[0,1]&rarr;[0,0]), v2 sum 23, v1 unchanged 19.
 * Total nodes 16. Demo query range [1,3]: v0 = 6, v1 = 11,
 * v2 = 11. Trace: 1 build step (line 1) + 2 update steps
 * (line 2) + 3 query steps (line 3, one per version) +
 * final (line 0) = 7 steps.</p>
 *
 * <p>Build step: {@code data} = [9] (nodes), {@code sorted}
 * = v0 node ids. Update step: {@code data} = [total nodes
 * so far], {@code highlight} = [updated array index],
 * {@code sorted} = the new version's node ids. Query step:
 * {@code data} = [sum for that version], {@code highlight}
 * = [the query range indices], {@code sorted} = that
 * version's node ids. Final step {@code data} = [16],
 * {@code sorted} = the latest version's node ids.
 * {@code nodes} / {@code edges} = all nodes/edges created
 * so far (shared nodes have two parents in the edge list —
 * the tree is a DAG of versions). Node {@code value} = the
 * segment sum, {@code label} = the range "[l,r]". With
 * preorder numbering the demo versions are v0 = [0..8],
 * v1 = [2,3,4,6,7,8,9,10,11], v2 = [4,6,7,8,11,12,13,14,15].</p>
 *
 * <p>Bad input (a missing/unparseable array, or a values
 * list that is not pairs of updates followed by a query
 * range) yields a single "empty" Step; the controller
 * 400s on it (see VisualizationController).</p>
 */
@Service
public class PersistentSegService {

    private static final int LINE_BUILD = 1;
    private static final int LINE_UPDATE = 2;
    private static final int LINE_QUERY = 3;
    private static final int LINE_FINAL = 0;

    /** One segment-tree node: created once, never mutated (persistence). */
    private static final class PstNode {
        final int id;
        final int left;
        final int right;
        int sum;
        PstNode leftChild;
        PstNode rightChild;

        PstNode(int id, int left, int right, int sum) {
            this.id = id;
            this.left = left;
            this.right = right;
            this.sum = sum;
        }
    }

    /** Per-call mutable state (the service bean is a singleton, so no instance state). */
    private static final class Tree {
        final int[] array;
        final List<PstNode> nodes = new ArrayList<>();
        final List<EdgeDto> edges = new ArrayList<>();

        Tree(int[] array) {
            this.array = array;
        }

        /** Builds a version from scratch; ids are preorder (node first, then children). */
        PstNode build(int left, int right) {
            PstNode node = newNode(left, right, 0);
            if (left == right) {
                node.sum = array[left];
                return node;
            }
            int mid = (left + right) / 2;
            node.leftChild = build(left, mid);
            node.rightChild = build(mid + 1, right);
            node.sum = node.leftChild.sum + node.rightChild.sum;
            edges.add(new EdgeDto(node.id, node.leftChild.id, null));
            edges.add(new EdgeDto(node.id, node.rightChild.id, null));
            return node;
        }

        /** Clones the path to index (the only new nodes); every other child is shared. */
        PstNode update(PstNode node, int index, int value) {
            PstNode clone = newNode(node.left, node.right, 0);
            if (node.left == node.right) {
                clone.sum = value;
                return clone;
            }
            int mid = (node.left + node.right) / 2;
            if (index <= mid) {
                clone.leftChild = update(node.leftChild, index, value);
                clone.rightChild = node.rightChild;
            } else {
                clone.leftChild = node.leftChild;
                clone.rightChild = update(node.rightChild, index, value);
            }
            clone.sum = clone.leftChild.sum + clone.rightChild.sum;
            edges.add(new EdgeDto(clone.id, clone.leftChild.id, null));
            edges.add(new EdgeDto(clone.id, clone.rightChild.id, null));
            return clone;
        }

        /** The standard segment-tree range-sum query on one version. */
        int query(PstNode node, int left, int right) {
            if (node.right < left || node.left > right) {
                return 0;
            }
            if (left <= node.left && node.right <= right) {
                return node.sum;
            }
            return query(node.leftChild, left, right)
                    + query(node.rightChild, left, right);
        }

        /** The node ids of one version's tree, in id order. */
        List<Integer> versionIds(PstNode root) {
            List<Integer> ids = new ArrayList<>();
            collect(root, ids);
            ids.sort(Integer::compareTo);
            return ids;
        }

        private void collect(PstNode node, List<Integer> ids) {
            if (node == null) {
                return;
            }
            ids.add(node.id);
            collect(node.leftChild, ids);
            collect(node.rightChild, ids);
        }

        private PstNode newNode(int left, int right, int sum) {
            PstNode node = new PstNode(nodes.size(), left, right, sum);
            nodes.add(node);
            return node;
        }

        /** One NodeDto per node created so far, in creation (id) order. */
        List<NodeDto> snapshot() {
            List<NodeDto> dtos = new ArrayList<>();
            for (PstNode node : nodes) {
                NodeDto dto = new NodeDto(node.id, node.sum, 0, 0);
                dto.setLabel("[" + node.left + "," + node.right + "]");
                dtos.add(dto);
            }
            return dtos;
        }
    }

    /**
     * Builds the persistent segment tree over the array in
     * {@code word} ("3,1,4,1,5"), applies the updates in
     * {@code values} ([updateIdx, updateVal, ...,
     * queryL, queryR] — the demo is [2,9,0,7,1,3]: 2 updates,
     * then the query [1,3]), and queries every version.
     */
    public List<Step> persistentSeg(String word,
                                    List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        int[] parsed = parseArray(word);
        if (parsed == null || parsed.length == 0) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The array is needed as "
                            + "comma-separated numbers.", LINE_FINAL));
            return steps;
        }
        if (values == null || values.size() < 2
                || values.size() % 2 != 0) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Updates and the query range are "
                            + "needed as [updateIdx, updateVal, "
                            + "..., queryL, queryR].", LINE_FINAL));
            return steps;
        }
        Tree tree = new Tree(parsed);
        int updateCount = (values.size() - 2) / 2;
        int queryLeft = values.get(values.size() - 2);
        int queryRight = values.get(values.size() - 1);
        for (int i = 0; i < updateCount; i++) {
            int index = values.get(2 * i);
            if (index < 0 || index >= parsed.length) {
                steps.add(new Step(List.of(), List.of(), List.of(),
                        List.of(), "Update index " + index
                                + " is out of range.", LINE_FINAL));
                return steps;
            }
        }
        if (queryLeft < 0 || queryRight >= parsed.length
                || queryLeft > queryRight) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Query range [" + queryLeft + ", "
                            + queryRight + "] is out of range.",
                    LINE_FINAL));
            return steps;
        }

        List<PstNode> versions = new ArrayList<>();
        PstNode root = tree.build(0, parsed.length - 1);
        versions.add(root);
        steps.add(new Step(
                List.of(tree.nodes.size()), List.of(), List.of(),
                tree.versionIds(root),
                "Built version 0 over " + parsed.length
                        + " elements: " + tree.nodes.size()
                        + " nodes, sum " + root.sum + ".",
                LINE_BUILD, tree.snapshot(),
                new ArrayList<>(tree.edges)));

        for (int i = 0; i < updateCount; i++) {
            int index = values.get(2 * i);
            int value = values.get(2 * i + 1);
            PstNode previous = versions.get(versions.size() - 1);
            PstNode updated = tree.update(previous, index, value);
            versions.add(updated);
            steps.add(new Step(
                    List.of(tree.nodes.size()), List.of(index),
                    List.of(), tree.versionIds(updated),
                    "Update " + (i + 1) + ": index " + index
                            + " becomes " + value + " — version "
                            + (versions.size() - 1) + " (sum "
                            + updated.sum + "), " + tree.nodes.size()
                            + " nodes in all.",
                    LINE_UPDATE, tree.snapshot(),
                    new ArrayList<>(tree.edges)));
        }

        List<Integer> range = new ArrayList<>();
        for (int i = queryLeft; i <= queryRight; i++) {
            range.add(i);
        }
        for (int v = 0; v < versions.size(); v++) {
            int sum = tree.query(versions.get(v), queryLeft,
                    queryRight);
            steps.add(new Step(
                    List.of(sum), new ArrayList<>(range),
                    List.of(), tree.versionIds(versions.get(v)),
                    "Query [" + queryLeft + ", " + queryRight
                            + "] on version " + v + ": sum " + sum
                            + ".",
                    LINE_QUERY, tree.snapshot(),
                    new ArrayList<>(tree.edges)));
        }
        PstNode latest = versions.get(versions.size() - 1);
        steps.add(new Step(
                List.of(tree.nodes.size()), List.of(), List.of(),
                tree.versionIds(latest),
                "Persistent segment tree done: "
                        + versions.size() + " versions, "
                        + tree.nodes.size() + " nodes in all.",
                LINE_FINAL, tree.snapshot(),
                new ArrayList<>(tree.edges)));
        return steps;
    }

    /** Parses "3,1,4,1,5" into an int array, or null. */
    private int[] parseArray(String word) {
        if (word == null || word.isBlank()) {
            return null;
        }
        String[] parts = word.trim().split(",", -1);
        int[] result = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                result[i] = Integer.parseInt(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return result;
    }
}
