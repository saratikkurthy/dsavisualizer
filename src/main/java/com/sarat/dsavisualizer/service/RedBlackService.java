package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Red-black tree operations that record each action as a Step.
 *
 * Standard CLRS red-black insert: insert as in a BST (the new node
 * is red), then fix violations with recoloring and rotations.
 * Node ids are 0-based in insertion order; rotations do NOT change
 * ids. Each Step carries the full tree as {@code nodes} +
 * {@code edges} (same in-order layout as the BST: x = 40 + rank
 * * 70, y = 40 + depth * 70); {@code data} is empty and
 * {@code highlight} holds node ids. Every node snapshot carries
 * its colour in {@code NodeDto.color} ("red" / "black").
 *
 * The fix-up is iterative with parent pointers (null leaves count
 * as black). Per insertion the trace shows the BST descent steps
 * (line 2), the new node placed (line 3), each recolor (line 3)
 * and each rotation (line 3); the request ends with a final Step
 * (line 0, "Red-black tree built. Root: X (black)."). A rotation
 * Step is emitted only once the pointers are fully re-linked, so
 * its snapshot never misses a detached subtree.
 *
 * Search builds the tree silently (same insert logic, no steps),
 * then walks it like the BST search — one Step per node visited
 * (line 2), then a found / not-found final Step (line 0).
 * Line numbers: compare/visit = 2, mutation = 3, final = 0.
 */
@Service
public class RedBlackService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_SWAP = 3;
    private static final int LINE_FINAL = 0;

    /** Internal tree node. x/y are layout coordinates, recomputed on snapshot. */
    private static class Node {
        int value;
        final int id;
        boolean red;
        double x;
        double y;
        Node left;
        Node right;
        Node parent;

        Node(int value, int id) {
            this.value = value;
            this.id = id;
            this.red = true; // a new node is always red
        }
    }

    /**
     * Insert every value in order, with the descent and fix-up
     * steps for each. Ends with a final Step naming the root.
     * Duplicate values are skipped silently (as in the BST build).
     */
    public List<Step> insert(List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        Node[] rootRef = new Node[1];
        int[] nextId = new int[]{0};
        if (values == null || values.isEmpty()) {
            steps.add(rbtStep(List.of(), List.of(), List.of(),
                    "Tree is empty.", LINE_FINAL));
            return steps;
        }
        for (int value : values) {
            insertValue(rootRef, value, nextId, steps);
        }
        Node root = rootRef[0];
        steps.add(rbtStep(List.of(), snapshotNodes(root),
                snapshotEdges(root),
                "Red-black tree built. Root: " + root.value + " ("
                        + colorName(root) + ").", LINE_FINAL));
        return steps;
    }

    /**
     * Search for a target. The tree is built by inserting all
     * values first (silently), then the search walks it. Each Step
     * carries the full tree snapshot with node colours.
     */
    public List<Step> search(List<Integer> values, int target) {
        List<Step> steps = new ArrayList<>();
        Node[] rootRef = new Node[1];
        int[] nextId = new int[]{0};
        if (values == null || values.isEmpty()) {
            steps.add(rbtStep(List.of(), List.of(), List.of(),
                    "Tree is empty.", LINE_FINAL));
            return steps;
        }
        for (int value : values) {
            insertValue(rootRef, value, nextId, null);
        }
        Node root = rootRef[0];
        Node cur = root;
        while (cur != null) {
            if (target == cur.value) {
                steps.add(rbtStep(List.of(cur.id),
                        snapshotNodes(root), snapshotEdges(root),
                        "Found target at node " + cur.value + "!",
                        LINE_FINAL));
                return steps;
            }
            steps.add(rbtStep(List.of(cur.id),
                    snapshotNodes(root), snapshotEdges(root),
                    "Visit node " + cur.value + ".", LINE_COMPARE));
            cur = target < cur.value ? cur.left : cur.right;
        }
        steps.add(rbtStep(List.of(),
                snapshotNodes(root), snapshotEdges(root),
                "Target " + target + " not found.", LINE_FINAL));
        return steps;
    }

    /**
     * One CLRS insert: BST descent (compare steps), attach the new
     * red node (placed step), then fix-up (recolor / rotation
     * steps). When {@code steps} is null the insert is silent
     * (used to build the tree for search). The root is always
     * black after the fix-up.
     */
    private void insertValue(Node[] rootRef, int value, int[] nextId,
                             List<Step> steps) {
        Node root = rootRef[0];
        if (root == null) {
            Node fresh = new Node(value, nextId[0]++);
            fresh.red = false; // the root is always black
            rootRef[0] = fresh;
            if (steps != null) {
                steps.add(rbtStep(List.of(fresh.id),
                        snapshotNodes(fresh), snapshotEdges(fresh),
                        "Insert " + value + " as the root (black).",
                        LINE_SWAP));
            }
            return;
        }
        // BST descent, one compare Step per visited node.
        Node cur = root;
        Node parent = null;
        boolean goLeft = false;
        while (cur != null) {
            if (value == cur.value) {
                return; // duplicate: skip silently
            }
            if (steps != null) {
                String direction = value < cur.value ? "left" : "right";
                steps.add(rbtStep(List.of(cur.id),
                        snapshotNodes(rootRef[0]),
                        snapshotEdges(rootRef[0]),
                        "Compare " + value + " with " + cur.value
                                + " — go " + direction + ".",
                        LINE_COMPARE));
            }
            parent = cur;
            if (value < cur.value) {
                goLeft = true;
                cur = cur.left;
            } else {
                goLeft = false;
                cur = cur.right;
            }
        }
        // Attach the new red leaf under parent.
        Node fresh = new Node(value, nextId[0]++);
        fresh.parent = parent;
        if (goLeft) {
            parent.left = fresh;
        } else {
            parent.right = fresh;
        }
        if (steps != null) {
            steps.add(rbtStep(List.of(fresh.id),
                    snapshotNodes(rootRef[0]),
                    snapshotEdges(rootRef[0]),
                    "Insert " + value + " as the "
                            + (goLeft ? "left" : "right") + " child of "
                            + parent.value + " (red).",
                    LINE_SWAP));
        }
        fixUp(rootRef, fresh, steps);
    }

    /**
     * CLRS insert fix-up. While the parent is red, the grandparent
     * exists (a red parent cannot be the root — the root is always
     * black) and one of the three cases applies. Case 1 (red
     * uncle): recolor parent, uncle and grandparent, move up.
     * Case 2 (triangle): rotate at the parent, then fall through
     * to case 3 — the parent/grandparent are recomputed after the
     * rotation, because the rotated nodes changed places. Case 3
     * (line): recolor parent black and grandparent red, rotate at
     * the grandparent. The root is forced black at the end.
     */
    private void fixUp(Node[] rootRef, Node node, List<Step> steps) {
        Node current = node;
        while (current.parent != null && current.parent.red) {
            Node parent = current.parent;
            Node grandparent = parent.parent;
            if (parent == grandparent.left) {
                Node uncle = grandparent.right;
                if (uncle != null && uncle.red) {
                    // Case 1: red uncle — recolor and move up.
                    recolor(parent, false, rootRef, steps);
                    recolor(uncle, false, rootRef, steps);
                    recolor(grandparent, true, rootRef, steps);
                    current = grandparent;
                } else {
                    // Case 2: triangle — rotate at the parent.
                    if (current == parent.right) {
                        current = parent;
                        rotateLeft(rootRef, parent, steps);
                        parent = current.parent;
                        grandparent = parent.parent;
                    }
                    // Case 3: line — recolor and rotate at grandparent.
                    recolor(parent, false, rootRef, steps);
                    recolor(grandparent, true, rootRef, steps);
                    rotateRight(rootRef, grandparent, steps);
                }
            } else {
                Node uncle = grandparent.left;
                if (uncle != null && uncle.red) {
                    // Case 1 (mirror): red uncle — recolor and move up.
                    recolor(parent, false, rootRef, steps);
                    recolor(uncle, false, rootRef, steps);
                    recolor(grandparent, true, rootRef, steps);
                    current = grandparent;
                } else {
                    // Case 2 (mirror): triangle — rotate at the parent.
                    if (current == parent.left) {
                        current = parent;
                        rotateRight(rootRef, parent, steps);
                        parent = current.parent;
                        grandparent = parent.parent;
                    }
                    // Case 3 (mirror): recolor and rotate at grandparent.
                    recolor(parent, false, rootRef, steps);
                    recolor(grandparent, true, rootRef, steps);
                    rotateLeft(rootRef, grandparent, steps);
                }
            }
        }
        // The root is always black.
        Node root = rootRef[0];
        if (root != null && root.red) {
            recolor(root, false, rootRef, steps);
        }
    }

    /** Recolor one node (a Step, unless the colour is unchanged or steps is null). */
    private void recolor(Node node, boolean red, Node[] rootRef,
                         List<Step> steps) {
        if (node.red == red) {
            return;
        }
        node.red = red;
        if (steps != null) {
            steps.add(rbtStep(List.of(node.id),
                    snapshotNodes(rootRef[0]),
                    snapshotEdges(rootRef[0]),
                    "Recolor node " + node.value + " to "
                            + (red ? "red" : "black") + ".",
                    LINE_SWAP));
        }
    }

    /** Left rotation at x (y's left subtree becomes x's right subtree). */
    private void rotateLeft(Node[] rootRef, Node x, List<Step> steps) {
        Node y = x.right;
        x.right = y.left;
        if (y.left != null) {
            y.left.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == null) {
            rootRef[0] = y;
        } else if (x == x.parent.left) {
            x.parent.left = y;
        } else {
            x.parent.right = y;
        }
        y.left = x;
        x.parent = y;
        if (steps != null) {
            steps.add(rbtStep(List.of(x.id),
                    snapshotNodes(rootRef[0]),
                    snapshotEdges(rootRef[0]),
                    "Left rotate at node " + x.value + ".",
                    LINE_SWAP));
        }
    }

    /** Right rotation at x (y's right subtree becomes x's left subtree). */
    private void rotateRight(Node[] rootRef, Node x, List<Step> steps) {
        Node y = x.left;
        x.left = y.right;
        if (y.right != null) {
            y.right.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == null) {
            rootRef[0] = y;
        } else if (x == x.parent.right) {
            x.parent.right = y;
        } else {
            x.parent.left = y;
        }
        y.right = x;
        x.parent = y;
        if (steps != null) {
            steps.add(rbtStep(List.of(x.id),
                    snapshotNodes(rootRef[0]),
                    snapshotEdges(rootRef[0]),
                    "Right rotate at node " + x.value + ".",
                    LINE_SWAP));
        }
    }

    /** "red" / "black" for a node. */
    private String colorName(Node node) {
        return node.red ? "red" : "black";
    }

    /** Builds one Step for the red-black tree: empty data, node ids in highlight. */
    private Step rbtStep(List<Integer> highlight,
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
     * (40 + depth * 70). Same layout as the BST (BstService).
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

    /** In-order collection of nodes into NodeDto form (with colour). */
    private void collectNodes(Node node, List<NodeDto> nodes) {
        if (node == null) {
            return;
        }
        collectNodes(node.left, nodes);
        NodeDto dto = new NodeDto(node.id, node.value, node.x, node.y);
        dto.setColor(colorName(node));
        nodes.add(dto);
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
