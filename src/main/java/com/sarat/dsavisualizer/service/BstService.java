package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Binary search tree operations that record each action as a Step.
 *
 * The tree is built by inserting {@code values} in order; node ids
 * are 0-based in insertion order (root id 0). Each Step carries the
 * full tree as {@code nodes} + {@code edges} (see Step), so the
 * frontend can draw it; {@code data} is empty and {@code highlight}
 * holds node ids (not array indexes).
 *
 * Layout (computed on every snapshot): in-order rank gives x
 * (x = 40 + rank * 70), depth gives y (y = 40 + depth * 70).
 * Line numbers: compare/visit = 2, final = 0.
 */
@Service
public class BstService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_FINAL = 0;

    /** Internal tree node. x/y are layout coordinates, recomputed on snapshot. */
    private static class Node {
        final int value;
        final int id;
        double x;
        double y;
        Node left;
        Node right;

        Node(int value, int id) {
            this.value = value;
            this.id = id;
        }
    }

    /**
     * Insert a value. Emits one Step per comparison while walking
     * down, then a final Step with the new node added. If the value
     * is already present the trace ends with "X is already in the
     * tree." and no node is duplicated. If the tree starts empty,
     * the first Step is the new root.
     */
    public List<Step> insert(List<Integer> values, int value) {
        List<Step> steps = new ArrayList<>();
        int[] nextId = new int[]{0};
        Node root = buildTree(values, nextId);

        if (root == null) {
            // Empty tree: the new value becomes the root.
            root = new Node(value, nextId[0]++);
            steps.add(bstStep(List.of(root.id),
                    snapshotNodes(root), snapshotEdges(root),
                    "Inserted " + value + " as the root.", LINE_FINAL));
            return steps;
        }

        // Walk down, one compare Step per visited node.
        Node cur = root;
        Node parent = null;
        boolean goLeft = false;
        while (cur != null) {
            if (value == cur.value) {
                steps.add(bstStep(List.of(cur.id),
                        snapshotNodes(root), snapshotEdges(root),
                        value + " is already in the tree.", LINE_FINAL));
                return steps;
            }
            String direction;
            if (value < cur.value) {
                direction = "left";
            } else {
                direction = "right";
            }
            steps.add(bstStep(List.of(cur.id),
                    snapshotNodes(root), snapshotEdges(root),
                    "Compare " + value + " with node " + cur.value
                            + " — go " + direction + ".",
                    LINE_COMPARE));
            parent = cur;
            if (value < cur.value) {
                goLeft = true;
                cur = cur.left;
            } else {
                goLeft = false;
                cur = cur.right;
            }
        }

        // cur is null: attach the new leaf under parent.
        Node fresh = new Node(value, nextId[0]++);
        if (goLeft) {
            parent.left = fresh;
        } else {
            parent.right = fresh;
        }
        steps.add(bstStep(List.of(fresh.id),
                snapshotNodes(root), snapshotEdges(root),
                "Inserted " + value + ".", LINE_FINAL));
        return steps;
    }

    /**
     * Search for a target. Emits one Step per node visited, then a
     * found or not-found final Step. Each Step carries the full
     * tree snapshot.
     */
    public List<Step> search(List<Integer> values, int target) {
        List<Step> steps = new ArrayList<>();
        int[] nextId = new int[]{0};
        Node root = buildTree(values, nextId);

        if (root == null) {
            steps.add(bstStep(List.of(), List.of(), List.of(),
                    "Tree is empty.", LINE_FINAL));
            return steps;
        }

        Node cur = root;
        while (cur != null) {
            if (target == cur.value) {
                steps.add(bstStep(List.of(cur.id),
                        snapshotNodes(root), snapshotEdges(root),
                        "Found target at node " + cur.value + "!",
                        LINE_FINAL));
                return steps;
            }
            steps.add(bstStep(List.of(cur.id),
                    snapshotNodes(root), snapshotEdges(root),
                    "Visit node " + cur.value + ".", LINE_COMPARE));
            cur = target < cur.value ? cur.left : cur.right;
        }
        steps.add(bstStep(List.of(),
                snapshotNodes(root), snapshotEdges(root),
                "Target " + target + " not found.", LINE_FINAL));
        return steps;
    }

    /** Builds a tree by inserting each value in order (duplicates skipped). */
    private Node buildTree(List<Integer> values, int[] nextId) {
        Node root = null;
        if (values != null) {
            for (int v : values) {
                root = insertSilent(root, v, nextId);
            }
        }
        return root;
    }

    /**
     * Silent BST insert (no steps). Returns the root. A duplicate
     * value is ignored and does not consume an id.
     */
    private Node insertSilent(Node root, int value, int[] nextId) {
        if (root == null) {
            return new Node(value, nextId[0]++);
        }
        Node cur = root;
        while (true) {
            if (value == cur.value) {
                return root;
            }
            if (value < cur.value) {
                if (cur.left == null) {
                    cur.left = new Node(value, nextId[0]++);
                    return root;
                }
                cur = cur.left;
            } else {
                if (cur.right == null) {
                    cur.right = new Node(value, nextId[0]++);
                    return root;
                }
                cur = cur.right;
            }
        }
    }

    /** Builds one Step for the BST: empty data, node ids in highlight. */
    private Step bstStep(List<Integer> highlight,
                         List<NodeDto> nodes, List<EdgeDto> edges,
                         String message, int line) {
        return new Step(List.of(), highlight, List.of(), List.of(),
                message, line, nodes, edges);
    }

    /** Full node snapshot: lays the tree out, then lists nodes in-order. */
    private List<NodeDto> snapshotNodes(Node root) {
        layout(root, 0, new int[]{0});
        List<NodeDto> nodes = new ArrayList<>();
        collectNodes(root, nodes);
        return nodes;
    }

    /** Full edge snapshot: one parent-to-child pair per child. */
    private List<EdgeDto> snapshotEdges(Node root) {
        List<EdgeDto> edges = new ArrayList<>();
        collectEdges(root, edges);
        return edges;
    }

    /**
     * In-order traversal assigning screen coordinates: x from the
     * in-order rank (40 + rank * 70), y from the depth
     * (40 + depth * 70).
     */
    private void layout(Node node, int depth, int[] rank) {
        if (node == null) {
            return;
        }
        layout(node.left, depth + 1, rank);
        node.x = 40 + rank[0] * 70;
        node.y = 40 + depth * 70;
        rank[0]++;
        layout(node.right, depth + 1, rank);
    }

    /** In-order collection of nodes into NodeDto form. */
    private void collectNodes(Node node, List<NodeDto> nodes) {
        if (node == null) {
            return;
        }
        collectNodes(node.left, nodes);
        nodes.add(new NodeDto(node.id, node.value, node.x, node.y));
        collectNodes(node.right, nodes);
    }

    /** Pre-order collection of parent-to-child edges. */
    private void collectEdges(Node node, List<EdgeDto> edges) {
        if (node == null) {
            return;
        }
        if (node.left != null) {
            edges.add(new EdgeDto(node.id, node.left.id));
        }
        if (node.right != null) {
            edges.add(new EdgeDto(node.id, node.right.id));
        }
        collectEdges(node.left, edges);
        collectEdges(node.right, edges);
    }
}
