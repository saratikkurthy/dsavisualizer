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
 * Delete (Phase 12) builds the tree silently, searches for the
 * key with compare Steps, then removes it (CLRS delete with the
 * double-black fix-up) and ends with a final Step naming the new
 * root. Line numbers: compare/visit = 2, mutation = 3, final = 0.
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
     * Delete a key. The tree is built by inserting all values
     * first (silently), then the key is searched for with compare
     * Steps (line 2). If it is not there, the trace ends with a
     * single "not found" final Step (line 0). Otherwise the node
     * is deleted (line 3), the double-black fix-up runs when the
     * removed node was black (recolor / rotation Steps, line 3),
     * and the trace ends with a final Step naming the new root
     * (line 0). Handles a leaf, one child, and two children
     * (successor replacement). Duplicates are skipped silently
     * during the build, like insert.
     */
    public List<Step> delete(List<Integer> values, int value) {
        List<Step> steps = new ArrayList<>();
        Node[] rootRef = new Node[1];
        int[] nextId = new int[]{0};
        if (values == null || values.isEmpty()) {
            steps.add(rbtStep(List.of(), List.of(), List.of(),
                    "Tree is empty.", LINE_FINAL));
            return steps;
        }
        for (int v : values) {
            insertValue(rootRef, v, nextId, null);
        }
        Node root = rootRef[0];
        // BST search for the key, one compare Step per node visited.
        Node target = root;
        while (target != null && target.value != value) {
            String direction = value < target.value ? "left" : "right";
            steps.add(rbtStep(List.of(target.id),
                    snapshotNodes(root), snapshotEdges(root),
                    "Compare " + value + " with " + target.value
                            + " — go " + direction + ".",
                    LINE_COMPARE));
            target = value < target.value ? target.left : target.right;
        }
        if (target == null) {
            steps.add(rbtStep(List.of(),
                    snapshotNodes(root), snapshotEdges(root),
                    "Key " + value + " not found in the tree.",
                    LINE_FINAL));
            return steps;
        }
        deleteNode(rootRef, target, steps);
        Node newRoot = rootRef[0];
        if (newRoot == null) {
            steps.add(rbtStep(List.of(), List.of(), List.of(),
                    "Deleted " + value + ". The tree is now empty.",
                    LINE_FINAL));
        } else {
            steps.add(rbtStep(List.of(), snapshotNodes(newRoot),
                    snapshotEdges(newRoot),
                    "Deleted " + value + ". Root: " + newRoot.value
                            + " (" + colorName(newRoot) + ").",
                    LINE_FINAL));
        }
        return steps;
    }

    /**
     * One CLRS delete (RB-DELETE). {@code z} is the node to remove.
     * {@code y} is the node actually spliced out of the tree; when
     * z has two children y is z's successor, which takes z's place
     * and z's colour. {@code x} is the child that replaces y (it
     * may be null — a null leaf); {@code xParent} and
     * {@code xIsLeft} say where x sits, because a null x has no
     * parent pointer of its own. When the spliced-out node was
     * black, the double-black fix-up runs. The deletion Step is
     * emitted once the pointers are fully re-linked, so its
     * snapshot never misses a detached subtree.
     */
    private void deleteNode(Node[] rootRef, Node z, List<Step> steps) {
        Node y = z;
        boolean yOriginalRed = y.red;
        Node x;
        Node xParent;
        boolean xIsLeft;
        String message;
        if (z.left == null) {
            x = z.right;
            xParent = z.parent;
            xIsLeft = z.parent != null && z == z.parent.left;
            transplant(rootRef, z, z.right);
            message = "Delete " + z.value + " (" + colorName(z)
                    + (z.right == null ? " leaf" : ", one child") + ").";
        } else if (z.right == null) {
            x = z.left;
            xParent = z.parent;
            xIsLeft = z.parent != null && z == z.parent.left;
            transplant(rootRef, z, z.left);
            message = "Delete " + z.value + " (" + colorName(z)
                    + ", one child).";
        } else {
            // Two children: the successor is the minimum of the
            // right subtree; it takes z's place and z's colour.
            y = minimum(z.right);
            yOriginalRed = y.red;
            x = y.right;
            if (y.parent == z) {
                // y is z's right child: x stays y's right child.
                xParent = y;
                xIsLeft = false;
            } else {
                // x takes y's place as the left child of y's
                // parent (y is the leftmost node, so it is a left
                // child); then y takes z's right subtree.
                xParent = y.parent;
                xIsLeft = true;
                transplant(rootRef, y, y.right);
                y.right = z.right;
                y.right.parent = y;
            }
            transplant(rootRef, z, y);
            y.left = z.left;
            y.left.parent = y;
            y.red = z.red;
            message = "Delete " + z.value
                    + " — replace with its successor " + y.value + ".";
        }
        // Highlight the node that took z's place: the successor
        // (two children), the child (one child), or the parent
        // (a leaf) — z itself is gone from the snapshot, so
        // highlighting it would highlight nothing.
        List<Integer> highlight;
        if (y != z) {
            highlight = List.of(y.id);
        } else if (x != null) {
            highlight = List.of(x.id);
        } else if (xParent != null) {
            highlight = List.of(xParent.id);
        } else {
            highlight = List.of();
        }
        steps.add(rbtStep(highlight,
                snapshotNodes(rootRef[0]), snapshotEdges(rootRef[0]),
                message, LINE_SWAP));
        if (!yOriginalRed) {
            deleteFixup(rootRef, x, xParent, xIsLeft, steps);
        }
    }

    /**
     * CLRS delete fix-up. {@code x} is the double-black node (it
     * may be null — a null leaf counts as black); {@code parent}
     * is x's parent and {@code isLeft} says whether x is its left
     * child, because a null x has no parent pointer. Cases (x a
     * left child; the right-child side is the mirror image):
     * case 1, the sibling is red — recolor and rotate at the
     * parent, then recompute the sibling; case 2, the sibling is
     * black with two black children — recolor the sibling red and
     * move the double-black up to the parent; case 3, the sibling
     * is black with a red left child and a black right child —
     * recolor and rotate at the sibling, then fall through to
     * case 4; case 4, the sibling is black with a red right
     * child — the sibling takes the parent's colour, the parent
     * and the sibling's right child go black, rotate at the
     * parent, and the double-black is resolved. The root is
     * forced black at the end. A rotation or recolor Step is
     * emitted only when the pointers / colour actually change
     * (the helpers skip no-ops), so the trace never shows a
     * step that did nothing.
     */
    private void deleteFixup(Node[] rootRef, Node x, Node parent,
                             boolean isLeft, List<Step> steps) {
        Node current = x;
        Node currentParent = parent;
        boolean currentIsLeft = isLeft;
        while (current != rootRef[0] && !isRed(current)) {
            if (currentIsLeft) {
                Node w = currentParent.right;
                if (isRed(w)) {
                    // Case 1: the sibling is red.
                    recolor(w, false, rootRef, steps);
                    recolor(currentParent, true, rootRef, steps);
                    rotateLeft(rootRef, currentParent, steps);
                    w = currentParent.right;
                }
                if (!isRed(w.left) && !isRed(w.right)) {
                    // Case 2: both of the sibling's children black.
                    recolor(w, true, rootRef, steps);
                    current = currentParent;
                    currentParent = current.parent;
                    currentIsLeft = currentParent != null
                            && current == currentParent.left;
                } else {
                    if (!isRed(w.right)) {
                        // Case 3: the sibling's right child is
                        // black, so the left child must be red.
                        recolor(w.left, false, rootRef, steps);
                        recolor(w, true, rootRef, steps);
                        rotateRight(rootRef, w, steps);
                        w = currentParent.right;
                    }
                    // Case 4: the sibling's right child is red.
                    recolor(w, currentParent.red, rootRef, steps);
                    recolor(currentParent, false, rootRef, steps);
                    recolor(w.right, false, rootRef, steps);
                    rotateLeft(rootRef, currentParent, steps);
                    current = rootRef[0];
                }
            } else {
                Node w = currentParent.left;
                if (isRed(w)) {
                    // Case 1 (mirror): the sibling is red.
                    recolor(w, false, rootRef, steps);
                    recolor(currentParent, true, rootRef, steps);
                    rotateRight(rootRef, currentParent, steps);
                    w = currentParent.left;
                }
                if (!isRed(w.right) && !isRed(w.left)) {
                    // Case 2 (mirror): both children black.
                    recolor(w, true, rootRef, steps);
                    current = currentParent;
                    currentParent = current.parent;
                    currentIsLeft = currentParent != null
                            && current == currentParent.left;
                } else {
                    if (!isRed(w.left)) {
                        // Case 3 (mirror): the sibling's left
                        // child is black, so the right must be red.
                        recolor(w.right, false, rootRef, steps);
                        recolor(w, true, rootRef, steps);
                        rotateLeft(rootRef, w, steps);
                        w = currentParent.left;
                    }
                    // Case 4 (mirror): the sibling's left child
                    // is red.
                    recolor(w, currentParent.red, rootRef, steps);
                    recolor(currentParent, false, rootRef, steps);
                    recolor(w.left, false, rootRef, steps);
                    rotateRight(rootRef, currentParent, steps);
                    current = rootRef[0];
                }
            }
        }
        // The root (or the node that took the double-black's place)
        // is always black.
        if (current != null && current.red) {
            recolor(current, false, rootRef, steps);
        }
    }

    /**
     * CLRS transplant: replace subtree {@code u} with subtree
     * {@code v} (which may be null). {@code v}'s parent becomes
     * {@code u}'s parent; {@code u}'s parent (or the root) now
     * points at {@code v}.
     */
    private void transplant(Node[] rootRef, Node u, Node v) {
        if (v != null) {
            v.parent = u.parent;
        }
        if (u.parent == null) {
            rootRef[0] = v;
        } else if (u == u.parent.left) {
            u.parent.left = v;
        } else {
            u.parent.right = v;
        }
    }

    /** The minimum (leftmost) node of a subtree. */
    private Node minimum(Node node) {
        Node cur = node;
        while (cur.left != null) {
            cur = cur.left;
        }
        return cur;
    }

    /** True when the node exists and is red; a null leaf counts as black. */
    private boolean isRed(Node node) {
        return node != null && node.red;
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
