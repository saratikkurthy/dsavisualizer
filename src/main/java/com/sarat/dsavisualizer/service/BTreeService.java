package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * B-Tree (Phase 17): minimum degree t = 2, so at most 3 keys per
 * node and at least 1 (except the root, which may hold 1..3).
 * Keys are inserted in order; a full node is split on the way
 * down (the CLRS split-full-child rule), so a split step is
 * always emitted before the insert-completes step for that key.
 *
 * A duplicate key is skipped silently (no step): it is already
 * in the tree, so nothing is inserted — the BST precedent.
 *
 * Phase 18 adds search and delete. Search walks down one node
 * per step (a visit step for every node visited, then a found
 * or not-found step). Delete is the full CLRS delete: leaf
 * delete, internal delete by predecessor (or successor when the
 * left child is short), borrow from a sibling with &gt;= 2 keys,
 * merge when both siblings have 1 key. Search and delete build
 * the tree silently first (see {@code buildTree}) so the trace
 * holds only the search/delete steps.
 *
 * Trace: {@code data} is empty and the tree travels in
 * {@code nodes}/{@code edges}, the BST convention. A node's
 * {@code keys} carry its keys (see NodeDto); {@code x} is the
 * in-order rank and {@code y} the depth, the same coordinate
 * convention BstService uses (x = 40 + rank * 70, y = 40 +
 * depth * 70). Split and insert steps are line 2; the final
 * step is line 0 and carries the level-order tree.
 */
@Service
public class BTreeService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_MATCH = 3;
    private static final int LINE_FINAL = 0;

    /** Maximum keys per node (2t - 1 with t = 2). */
    private static final int MAX_KEYS = 3;

    /** Minimum keys a non-root node must hold (t = 2). */
    private static final int MIN_KEYS = 2;

    /** Internal tree node. x/y are layout coordinates, recomputed on snapshot. */
    private static class Node {
        List<Integer> keys = new ArrayList<>();
        List<Node> children = new ArrayList<>();
        boolean leaf = true;
        final int id;
        double x;
        double y;

        Node(int id) {
            this.id = id;
        }
    }

    private List<Step> steps;
    private int nextId;
    private Node root;

    /**
     * Whether the insert helpers emit steps. Insert runs with it
     * on; {@link #buildTree} runs with it off so search and delete
     * build the tree silently and the trace holds only their own
     * steps.
     */
    private boolean emitting = true;

    /**
     * Inserts {@code values} in order. The controller validates
     * the input (1..12 values, |v| &lt;= 99); this method guards
     * against bad input anyway with a single "empty" Step.
     */
    public List<Step> insert(List<Integer> values) {
        steps = new ArrayList<>();
        nextId = 0;
        if (values == null || values.isEmpty()) {
            steps.add(btreeStep(List.of(), List.of(), List.of(),
                    "A list of values is needed.", LINE_FINAL));
            return steps;
        }
        root = new Node(nextId++);
        for (int key : values) {
            insertKey(key);
        }
        steps.add(btreeStep(List.of(), snapshotNodes(root),
                snapshotEdges(root),
                "Final tree: " + levelOrder(root) + ".", LINE_FINAL));
        return steps;
    }

    /** Inserts one key, splitting the root first if it is full. */
    private void insertKey(int key) {
        if (root.keys.size() == MAX_KEYS) {
            Node oldRoot = root;
            Node newRoot = new Node(nextId++);
            newRoot.leaf = false;
            newRoot.children.add(oldRoot);
            int median = splitChild(newRoot, 0, oldRoot);
            root = newRoot;
            if (emitting) {
                steps.add(btreeStep(List.of(newRoot.id),
                        snapshotNodes(root), snapshotEdges(root),
                        "Root is full — split around median " + median
                                + "; new root [" + median + "], children "
                                + oldRoot.keys + " and "
                                + newRoot.children.get(1).keys + ".",
                        LINE_COMPARE));
            }
        }
        insertNonFull(root, key);
    }

    /**
     * Inserts {@code key} into a node that is not full, splitting
     * a full child on the way down. A key already present is
     * skipped silently (no step).
     */
    private void insertNonFull(Node node, int key) {
        if (node.leaf) {
            int i = 0;
            while (i < node.keys.size() && node.keys.get(i) < key) {
                i++;
            }
            if (i < node.keys.size() && node.keys.get(i) == key) {
                return;
            }
            node.keys.add(i, key);
            if (emitting) {
                steps.add(btreeStep(List.of(node.id),
                        snapshotNodes(root), snapshotEdges(root),
                        "Insert " + key + " → leaf " + node.keys + ".",
                        LINE_COMPARE));
            }
            return;
        }
        int i = 0;
        while (i < node.keys.size() && node.keys.get(i) < key) {
            i++;
        }
        if (i < node.keys.size() && node.keys.get(i) == key) {
            return;
        }
        Node child = node.children.get(i);
        if (child.keys.size() == MAX_KEYS) {
            int median = splitChild(node, i, child);
            if (emitting) {
                steps.add(btreeStep(List.of(child.id),
                        snapshotNodes(root), snapshotEdges(root),
                        "Split full node " + childBeforeSplitKeys(node, i)
                                + " around median " + median + ": left "
                                + node.children.get(i).keys + ", right "
                                + node.children.get(i + 1).keys + ".",
                        LINE_COMPARE));
            }
            if (key > median) {
                i++;
            }
            child = node.children.get(i);
        }
        insertNonFull(child, key);
    }

    // ---- Search & delete (Phase 18) ----

    /**
     * Builds the tree from {@code values} without emitting any
     * steps, so a search/delete trace holds only its own steps.
     * The controller validates the input; this guards against
     * bad input anyway by building nothing.
     */
    private void buildTree(List<Integer> values) {
        nextId = 0;
        root = new Node(nextId++);
        emitting = false;
        for (int key : values) {
            insertKey(key);
        }
        emitting = true;
    }

    /**
     * Searches for {@code target} in the tree built from
     * {@code values}: one visit step (line 2) per node visited,
     * then a found step (line 3) or a not-found step (line 2),
     * then the final step (line 0).
     */
    public List<Step> search(List<Integer> values, Integer target) {
        steps = new ArrayList<>();
        if (values == null || values.isEmpty() || target == null) {
            steps.add(btreeStep(List.of(), List.of(), List.of(),
                    "A list of values and a target are needed.",
                    LINE_FINAL));
            return steps;
        }
        buildTree(values);
        searchNode(root, target);
        steps.add(btreeStep(List.of(), snapshotNodes(root),
                snapshotEdges(root),
                "Search complete: " + target
                        + (found ? " found." : " not found."),
                LINE_FINAL));
        return steps;
    }

    /** Result of the last search walk, read by {@link #search}. */
    private boolean found;

    /**
     * Walks down from {@code node} looking for {@code target}:
     * a visit step per node, then a found step (line 3) when the
     * key is in the node or a not-found step (line 2) at a leaf.
     * Returns whether the key was found.
     */
    private boolean searchNode(Node node, int target) {
        int i = 0;
        while (i < node.keys.size() && node.keys.get(i) < target) {
            i++;
        }
        if (i < node.keys.size() && node.keys.get(i) == target) {
            steps.add(btreeStep(List.of(node.id),
                    snapshotNodes(root), snapshotEdges(root),
                    "Visit node " + node.keys + ": " + target
                            + visitComparison(node, target, i) + ".",
                    LINE_COMPARE));
            steps.add(btreeStep(List.of(node.id),
                    snapshotNodes(root), snapshotEdges(root),
                    "Found " + target + " in node " + node.keys
                            + " at position " + i + ".",
                    LINE_MATCH));
            found = true;
            return true;
        }
        if (node.leaf) {
            steps.add(btreeStep(List.of(node.id),
                    snapshotNodes(root), snapshotEdges(root),
                    "Visit node " + node.keys + ": " + target
                            + visitComparison(node, target, i) + ".",
                    LINE_COMPARE));
            steps.add(btreeStep(List.of(node.id),
                    snapshotNodes(root), snapshotEdges(root),
                    target + " is not in the tree (reached leaf "
                            + node.keys + ").",
                    LINE_COMPARE));
            found = false;
            return false;
        }
        steps.add(btreeStep(List.of(node.id),
                snapshotNodes(root), snapshotEdges(root),
                "Visit node " + node.keys + ": " + target
                        + visitComparison(node, target, i)
                        + " — go to the " + childName(i, node)
                        + " child.",
                LINE_COMPARE));
        found = searchNode(node.children.get(i), target);
        return found;
    }

    /**
     * The comparison clause of a visit message: how {@code target}
     * compares with the keys around child index {@code i} — the
     * first index whose key is &gt;= the target, or the key count
     * when the target is greater than every key.
     */
    private String visitComparison(Node node, int target, int i) {
        if (i < node.keys.size() && node.keys.get(i) == target) {
            if (i == 0) {
                return " is equal to " + node.keys.get(0);
            }
            return " is greater than " + node.keys.get(i - 1)
                    + ", equal to " + node.keys.get(i);
        }
        if (i == 0) {
            return " is less than " + node.keys.get(0);
        }
        if (i < node.keys.size()) {
            return " is greater than " + node.keys.get(i - 1)
                    + ", less than " + node.keys.get(i);
        }
        return " is greater than "
                + node.keys.get(node.keys.size() - 1);
    }

    /** "first" / "middle" / "last" for child index {@code i}. */
    private String childName(int i, Node node) {
        if (i == 0) {
            return "first";
        }
        if (i < node.keys.size()) {
            return "middle";
        }
        return "last";
    }

    /**
     * Deletes {@code key} from the tree built from
     * {@code values} (full CLRS delete, t = 2): one step per
     * phase of the delete, then the final step (line 0) with
     * the level-order tree. A key that is not in the tree
     * produces a not-present step and the unchanged tree.
     */
    public List<Step> delete(List<Integer> values, Integer key) {
        steps = new ArrayList<>();
        if (values == null || values.isEmpty() || key == null) {
            steps.add(btreeStep(List.of(), List.of(), List.of(),
                    "A list of values and a key are needed.",
                    LINE_FINAL));
            return steps;
        }
        buildTree(values);
        deleteKey(root, key);
        if (root.keys.isEmpty() && !root.leaf) {
            root = root.children.get(0);
        }
        steps.add(btreeStep(List.of(), snapshotNodes(root),
                snapshotEdges(root),
                "Final tree: " + levelOrder(root) + ".", LINE_FINAL));
        return steps;
    }

    /**
     * CLRS delete of {@code k} from the subtree rooted at
     * {@code node}. Emits one step per phase (leaf delete,
     * predecessor/successor replacement, borrow, merge) and
     * recurses; a missing key emits the not-present step.
     */
    private void deleteKey(Node node, int k) {
        int i = 0;
        while (i < node.keys.size() && node.keys.get(i) < k) {
            i++;
        }
        if (i < node.keys.size() && node.keys.get(i) == k) {
            if (node.leaf) {
                List<Integer> before = new ArrayList<>(node.keys);
                node.keys.remove(i);
                steps.add(btreeStep(List.of(node.id),
                        snapshotNodes(root), snapshotEdges(root),
                        "Delete " + k + " from leaf " + before
                                + " → " + node.keys + ".",
                        LINE_COMPARE));
                return;
            }
            Node left = node.children.get(i);
            Node right = node.children.get(i + 1);
            String where = node == root ? "root" : "node";
            if (left.keys.size() >= MIN_KEYS) {
                Node pred = left;
                while (!pred.leaf) {
                    pred = pred.children.get(pred.children.size() - 1);
                }
                int pk = pred.keys.get(pred.keys.size() - 1);
                List<Integer> before = new ArrayList<>(node.keys);
                node.keys.set(i, pk);
                steps.add(btreeStep(List.of(node.id),
                        snapshotNodes(root), snapshotEdges(root),
                        "Delete " + k + " from " + where + " " + before
                                + ": replaced by predecessor " + pk + ".",
                        LINE_COMPARE));
                deleteKey(left, pk);
            } else if (right.keys.size() >= MIN_KEYS) {
                Node succ = right;
                while (!succ.leaf) {
                    succ = succ.children.get(0);
                }
                int sk = succ.keys.get(0);
                List<Integer> before = new ArrayList<>(node.keys);
                node.keys.set(i, sk);
                steps.add(btreeStep(List.of(node.id),
                        snapshotNodes(root), snapshotEdges(root),
                        "Delete " + k + " from " + where + " " + before
                                + ": replaced by successor " + sk + ".",
                        LINE_COMPARE));
                deleteKey(right, sk);
            } else {
                List<Integer> before = new ArrayList<>(node.keys);
                List<Integer> leftKeys = new ArrayList<>(left.keys);
                List<Integer> rightKeys = new ArrayList<>(right.keys);
                mergeChildren(node, i);
                steps.add(btreeStep(List.of(node.id),
                        snapshotNodes(root), snapshotEdges(root),
                        "Delete " + k + " from " + where + " " + before
                                + ": both children have 1 key — merging "
                                + leftKeys + ", " + k + " and "
                                + rightKeys + " into " + left.keys + ".",
                        LINE_COMPARE));
                deleteKey(left, k);
            }
            return;
        }
        if (node.leaf) {
            steps.add(btreeStep(List.of(node.id),
                    snapshotNodes(root), snapshotEdges(root),
                    "Key " + k + " is not in the tree.",
                    LINE_COMPARE));
            return;
        }
        Node child = node.children.get(i);
        if (child.keys.size() == MIN_KEYS - 1) {
            fillChild(node, i);
            if (i > node.keys.size()) {
                i--;
            }
            child = node.children.get(i);
        }
        deleteKey(child, k);
    }

    /**
     * Ensures {@code parent}'s child {@code i} (which holds only
     * MIN_KEYS - 1 = 1 key) has at least MIN_KEYS keys before the
     * delete descends into it: borrow from a sibling with &gt;=
     * MIN_KEYS keys (a step is emitted), or merge with a sibling
     * (no step — the merge is part of the descent and the next
     * delete step shows the merged tree).
     */
    private void fillChild(Node parent, int i) {
        Node child = parent.children.get(i);
        String kind = child.leaf ? "Leaf" : "Node";
        if (i > 0
                && parent.children.get(i - 1).keys.size() >= MIN_KEYS) {
            Node sib = parent.children.get(i - 1);
            List<Integer> childBefore = new ArrayList<>(child.keys);
            List<Integer> sibBefore = new ArrayList<>(sib.keys);
            int up = sib.keys.get(sib.keys.size() - 1);
            int down = parent.keys.get(i - 1);
            child.keys.add(0, down);
            parent.keys.set(i - 1, up);
            sib.keys.remove(sib.keys.size() - 1);
            if (!child.leaf) {
                child.children.add(0,
                        sib.children.remove(sib.children.size() - 1));
            }
            steps.add(btreeStep(List.of(child.id),
                    snapshotNodes(root), snapshotEdges(root),
                    kind + " " + childBefore + " has only 1 key — "
                            + "borrowing from left sibling " + sibBefore
                            + ": " + up + " moves up, " + down
                            + " moves down.",
                    LINE_COMPARE));
        } else if (i < parent.children.size() - 1
                && parent.children.get(i + 1).keys.size() >= MIN_KEYS) {
            Node sib = parent.children.get(i + 1);
            List<Integer> childBefore = new ArrayList<>(child.keys);
            List<Integer> sibBefore = new ArrayList<>(sib.keys);
            int up = sib.keys.get(0);
            int down = parent.keys.get(i);
            child.keys.add(down);
            parent.keys.set(i, up);
            sib.keys.remove(0);
            if (!child.leaf) {
                child.children.add(sib.children.remove(0));
            }
            steps.add(btreeStep(List.of(child.id),
                    snapshotNodes(root), snapshotEdges(root),
                    kind + " " + childBefore + " has only 1 key — "
                            + "borrowing from right sibling " + sibBefore
                            + ": " + up + " moves up, " + down
                            + " moves down.",
                    LINE_COMPARE));
        } else if (i < parent.children.size() - 1) {
            mergeChildren(parent, i);
        } else {
            mergeChildren(parent, i - 1);
        }
    }

    /**
     * Merges {@code parent}'s children {@code i} and {@code i + 1}
     * around the parent key at {@code i}: the key drops into the
     * left child with the right child's keys and children, and
     * the right child leaves the parent.
     */
    private void mergeChildren(Node parent, int i) {
        Node left = parent.children.get(i);
        Node right = parent.children.get(i + 1);
        left.keys.add(parent.keys.remove(i));
        left.keys.addAll(right.keys);
        left.children.addAll(right.children);
        parent.children.remove(i + 1);
    }

    /**
     * Splits the full child {@code full} of {@code parent} at
     * index {@code i}: the median key moves up into the parent,
     * the keys below it stay in {@code full} (the left child) and
     * the keys above it go into a new right child. Returns the
     * median key. The caller emits the split step.
     */
    private int splitChild(Node parent, int i, Node full) {
        int median = full.keys.get(1);
        Node right = new Node(nextId++);
        right.leaf = full.leaf;
        right.keys.add(full.keys.get(2));
        if (!full.leaf) {
            right.children.add(full.children.get(2));
            right.children.add(full.children.get(3));
            full.children.subList(2, 4).clear();
        }
        full.keys.subList(1, 3).clear();
        parent.keys.add(i, median);
        parent.children.add(i + 1, right);
        return median;
    }

    /**
     * The full node's keys before the split, for the split-step
     * message. After {@link #splitChild} the parent holds the
     * median at index {@code i} and the left/right children hold
     * the keys below/above it, so the pre-split keys are
     * [left..., median, right...].
     */
    private List<Integer> childBeforeSplitKeys(Node parent, int i) {
        List<Integer> keys = new ArrayList<>(parent.children.get(i).keys);
        keys.add(parent.keys.get(i));
        keys.addAll(parent.children.get(i + 1).keys);
        return keys;
    }

    /** Level-order tree: levels joined by " | ", nodes by a space. */
    private String levelOrder(Node root) {
        List<String> levels = new ArrayList<>();
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<String> level = new ArrayList<>();
            for (int i = 0; i < levelSize; i++) {
                Node node = queue.remove();
                level.add(node.keys.toString());
                queue.addAll(node.children);
            }
            levels.add(String.join(" ", level));
        }
        return String.join(" | ", levels);
    }

    /** Builds one Step for the B-tree: empty data, node ids in highlight. */
    private Step btreeStep(List<Integer> highlight,
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
     * (40 + depth * 70). A node sits between its first and second
     * child, so it is ranked after its first subtree and before
     * the rest.
     */
    private void layout(Node node, int depth, int[] rank) {
        if (node == null) {
            return;
        }
        if (!node.leaf) {
            layout(node.children.get(0), depth + 1, rank);
        }
        node.x = 40 + rank[0] * 70;
        node.y = 40 + depth * 70;
        rank[0]++;
        if (!node.leaf) {
            for (int i = 1; i < node.children.size(); i++) {
                layout(node.children.get(i), depth + 1, rank);
            }
        }
    }

    /** In-order collection of nodes into NodeDto form (keys set). */
    private void collectNodes(Node node, List<NodeDto> nodes) {
        if (node == null) {
            return;
        }
        if (!node.leaf) {
            collectNodes(node.children.get(0), nodes);
        }
        nodes.add(new NodeDto(node.id, new ArrayList<>(node.keys),
                node.x, node.y));
        if (!node.leaf) {
            for (int i = 1; i < node.children.size(); i++) {
                collectNodes(node.children.get(i), nodes);
            }
        }
    }

    /** Pre-order collection of parent-to-child edges. */
    private void collectEdges(Node node, List<EdgeDto> edges) {
        if (node == null) {
            return;
        }
        for (Node child : node.children) {
            edges.add(new EdgeDto(node.id, child.id));
        }
        for (Node child : node.children) {
            collectEdges(child, edges);
        }
    }
}
