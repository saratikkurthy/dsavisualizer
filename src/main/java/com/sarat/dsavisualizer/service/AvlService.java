package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AVL tree operations that record each action as a Step.
 *
 * A real AVL tree: insert rebalances with LL, RR, LR and RL
 * rotations. Node ids are 0-based in insertion order; rotations do
 * NOT change ids. Each Step carries the full tree as
 * {@code nodes} + {@code edges} (same in-order layout as the BST:
 * x = 40 + rank * 70, y = 40 + depth * 70); {@code data} is empty
 * and {@code highlight} holds node ids.
 *
 * One insert causes at most one rotation, so the rotation is
 * recorded during the recursive insert and its Step is emitted by
 * the caller once the tree is fully re-linked: a snapshot taken
 * mid-rotation would miss the subtree detached by the rotation.
 * Line numbers: compare/visit = 2, rotation = 3, final = 0.
 */
@Service
public class AvlService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_SWAP = 3;
    private static final int LINE_FINAL = 0;

    /** Internal tree node. x/y are layout coordinates, recomputed on snapshot. */
    private static class Node {
        final int value;
        final int id;
        int height;
        double x;
        double y;
        Node left;
        Node right;

        Node(int value, int id) {
            this.value = value;
            this.id = id;
            this.height = 1;
        }
    }

    /**
     * Insert a value. The tree is built by inserting {@code values}
     * in order (compare and rotation steps included), then the new
     * value. Ends with a final Step, or "X is already in the tree."
     * for a duplicate. If the tree starts empty, the first Step is
     * the new root.
     */
    public List<Step> insert(List<Integer> values, int value) {
        List<Step> steps = new ArrayList<>();
        int[] nextId = new int[]{0};
        Node[] rootRef = new Node[]{null};

        if (values != null) {
            for (int v : values) {
                String[] rotation = new String[]{null};
                int[] rotationNodeId = new int[]{-1};
                rootRef[0] = insertRec(rootRef[0], v, steps, nextId, rootRef,
                        new boolean[]{false}, rotation, rotationNodeId);
                emitRotationStep(steps, rootRef[0], rotation, rotationNodeId);
            }
        }

        int newId = nextId[0];
        boolean[] duplicate = new boolean[]{false};
        String[] rotation = new String[]{null};
        int[] rotationNodeId = new int[]{-1};
        rootRef[0] = insertRec(rootRef[0], value, steps, nextId, rootRef,
                duplicate, rotation, rotationNodeId);
        emitRotationStep(steps, rootRef[0], rotation, rotationNodeId);

        if (duplicate[0]) {
            steps.add(avlStep(List.of(),
                    snapshotNodes(rootRef[0]), snapshotEdges(rootRef[0]),
                    value + " is already in the tree.", LINE_FINAL));
        } else {
            steps.add(avlStep(List.of(newId),
                    snapshotNodes(rootRef[0]), snapshotEdges(rootRef[0]),
                    "Inserted " + value + ". Tree is balanced.", LINE_FINAL));
        }
        return steps;
    }

    /**
     * Search for a target. The tree is built silently (no steps),
     * then one Step per node visited, then a found or not-found
     * final Step.
     */
    public List<Step> search(List<Integer> values, int target) {
        List<Step> steps = new ArrayList<>();
        int[] nextId = new int[]{0};
        Node[] rootRef = new Node[]{null};
        if (values != null) {
            for (int v : values) {
                rootRef[0] = insertRec(rootRef[0], v, null, nextId, rootRef,
                        new boolean[]{false}, new String[]{null},
                        new int[]{-1});
            }
        }
        if (rootRef[0] == null) {
            steps.add(avlStep(List.of(), List.of(), List.of(),
                    "Tree is empty.", LINE_FINAL));
            return steps;
        }
        Node cur = rootRef[0];
        while (cur != null) {
            if (target == cur.value) {
                steps.add(avlStep(List.of(cur.id),
                        snapshotNodes(rootRef[0]), snapshotEdges(rootRef[0]),
                        "Found target at node " + cur.value + "!",
                        LINE_FINAL));
                return steps;
            }
            steps.add(avlStep(List.of(cur.id),
                    snapshotNodes(rootRef[0]), snapshotEdges(rootRef[0]),
                    "Visit node " + cur.value + ".", LINE_COMPARE));
            cur = target < cur.value ? cur.left : cur.right;
        }
        steps.add(avlStep(List.of(),
                snapshotNodes(rootRef[0]), snapshotEdges(rootRef[0]),
                "Target " + target + " not found.", LINE_FINAL));
        return steps;
    }

    /**
     * Recursive AVL insert. Returns the (possibly new) subtree root.
     * Emits a compare Step per node visited (when {@code steps} is
     * non-null). Records any rotation in {@code rotation} /
     * {@code rotationNodeId} instead of emitting it, so the caller
     * can snapshot the fully re-linked tree.
     */
    private Node insertRec(Node node, int value, List<Step> steps,
                           int[] nextId, Node[] rootRef, boolean[] duplicate,
                           String[] rotation, int[] rotationNodeId) {
        if (node == null) {
            return new Node(value, nextId[0]++);
        }
        if (value == node.value) {
            duplicate[0] = true;
            return node;
        }
        if (steps != null) {
            String direction = value < node.value ? "left" : "right";
            steps.add(avlStep(List.of(node.id),
                    snapshotNodes(rootRef[0]), snapshotEdges(rootRef[0]),
                    "Compare " + value + " with node " + node.value
                            + " — go " + direction + ".",
                    LINE_COMPARE));
        }
        if (value < node.value) {
            node.left = insertRec(node.left, value, steps, nextId, rootRef,
                    duplicate, rotation, rotationNodeId);
        } else {
            node.right = insertRec(node.right, value, steps, nextId, rootRef,
                    duplicate, rotation, rotationNodeId);
        }
        updateHeight(node);
        int bal = balance(node);
        Node newRoot = node;
        if (bal > 1 && value < node.left.value) {
            // Left-left: right rotation at node.
            newRoot = rotateRight(node);
            rotation[0] = "Right rotation at node " + node.value + ".";
            rotationNodeId[0] = node.id;
        } else if (bal < -1 && value > node.right.value) {
            // Right-right: left rotation at node.
            newRoot = rotateLeft(node);
            rotation[0] = "Left rotation at node " + node.value + ".";
            rotationNodeId[0] = node.id;
        } else if (bal > 1 && value > node.left.value) {
            // Left-right: left rotation at the child, then right at node.
            node.left = rotateLeft(node.left);
            newRoot = rotateRight(node);
            rotation[0] = "Left-Right rotation at node " + node.value + ".";
            rotationNodeId[0] = node.id;
        } else if (bal < -1 && value < node.right.value) {
            // Right-left: right rotation at the child, then left at node.
            node.right = rotateRight(node.right);
            newRoot = rotateLeft(node);
            rotation[0] = "Right-Left rotation at node " + node.value + ".";
            rotationNodeId[0] = node.id;
        }
        if (node == rootRef[0]) {
            rootRef[0] = newRoot;
        }
        return newRoot;
    }

    /** Emits the recorded rotation Step (if any) with a fresh snapshot. */
    private void emitRotationStep(List<Step> steps, Node root,
                                  String[] rotation, int[] rotationNodeId) {
        if (rotation[0] == null) {
            return;
        }
        steps.add(avlStep(List.of(rotationNodeId[0]),
                snapshotNodes(root), snapshotEdges(root),
                rotation[0], LINE_SWAP));
    }

    private static int height(Node node) {
        return node == null ? 0 : node.height;
    }

    private static void updateHeight(Node node) {
        node.height = 1 + Math.max(height(node.left), height(node.right));
    }

    private static int balance(Node node) {
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    /** Right rotation: x becomes the subtree root, y its right child. */
    private static Node rotateRight(Node y) {
        Node x = y.left;
        Node t2 = x.right;
        x.right = y;
        y.left = t2;
        updateHeight(y);
        updateHeight(x);
        return x;
    }

    /** Left rotation: y becomes the subtree root, x its left child. */
    private static Node rotateLeft(Node x) {
        Node y = x.right;
        Node t2 = y.left;
        y.left = x;
        x.right = t2;
        updateHeight(x);
        updateHeight(y);
        return y;
    }

    /** Builds one Step for the AVL tree: empty data, node ids in highlight. */
    private Step avlStep(List<Integer> highlight,
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
