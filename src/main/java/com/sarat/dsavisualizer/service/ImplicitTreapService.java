package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implicit treap (Phase 28, range queries v4): a
 * binary search tree keyed by POSITION, not by value
 * — the inorder traversal is the array, and split /
 * merge by size give O(log n) split, range reverse
 * and range queries. Each node carries a priority
 * (heap order on priority, inorder on position); the
 * shape is whatever the priorities dictate. A range
 * reverse is a lazy flag on a subtree root: the flag
 * means "this subtree is reversed", and push()
 * swaps the children and passes the flag down before
 * anyone descends. split(t, k) cuts the first k
 * nodes off; merge(a, b) joins two trees whose keys
 * are all ordered (every key of a before every key
 * of b). Reverse [l, r] = split at l-1, split the
 * rest at r-l+1, flip the middle tree's flag, merge
 * twice.
 *
 * <p>Demo array [1..8]. Priorities are PINNED (not
 * random) so the shape is deterministic: {1:5, 2:2,
 * 3:8, 4:1, 5:6, 6:3, 7:7, 8:4} (smaller = higher).
 * Build is merge-append in value order; the built
 * tree is root 4, left 2 (left 1, right 3), right 6
 * (left 5, right 8 (left 7)). split@3 -&gt; [1,2,3]
 * | [4..8] (merged back silently), then reverse
 * [2,5] via split@1 and split@5, a lazy flag on the
 * middle tree, and two merges: final [1,5,4,3,2,6,
 * 7,8]. Trace: 8 build steps (line 2) + split@3
 * (line 3) + split@1 (line 3) + split@5 (line 3) +
 * reverse (line 4) + 2 merge steps (line 5) + final
 * (line 0) = 15 steps. The split/merge here are the
 * SAME recursion as the sim (push() before
 * descending, in both split and merge) or the shapes
 * differ. {@code nodes}: id = the VALUE (unique),
 * {@code value} = priority, {@code label} = the value
 * as a string. {@code edges}: parent-&gt;child,
 * weight null. Build steps: nodes = the tree so far;
 * {@code highlight} = [the new node id]; {@code
 * data} = [size so far]. Split steps: nodes = BOTH
 * resulting trees (the visualizer draws them side by
 * side); {@code highlight} = [the last value of the
 * left tree's inorder — the split point: 3, then 1,
 * then 5]; {@code data} = [leftSize, rightSize].
 * Reverse step: nodes = A + B + C with B's lazy flag
 * SET (the visualizer shows a "REV" badge on the B
 * root and draws B's children SWAPPED — the flag,
 * not a re-link, is the pin); {@code data} = [2, 5]
 * (range); {@code highlight} = [B's root id, 4]. The
 * {@code rev} flag lives in the node: a node with the
 * flag set has {@code value} = priority and {@code
 * label} = value + " (REV)" — the label suffix is the
 * ONLY place the flag appears, and the snapshot edges
 * are the true (unswapped) links. Merge steps: nodes
 * = the merged tree; {@code highlight} = [the root
 * id of the tree just merged in] (6 for merge(B, C),
 * 4 for merge(A, BC)); {@code data} = [size]. Final
 * step: nodes = final tree, {@code data} = [8].
 * Snapshots never push: they read the true links and
 * flags as they stand, so a set flag survives into
 * the snapshot (nodes 5 and 3 still carry "(REV)" in
 * the final tree — both are childless, so the swap
 * is invisible). Nodes are listed by id ascending,
 * edges sorted by (from, to).</p>
 *
 * <p>Bad input (an unparseable array, a values list
 * that is not exactly [splitPos, l-1, r], or an out
 * of range split/reverse) yields a single "empty"
 * Step; the controller 400s on it (see
 * VisualizationController). Duplicate values are
 * also rejected — node ids ARE the values, so two
 * equal values would collide.</p>
 */
@Service
public class ImplicitTreapService {

    private static final int LINE_BUILD = 2;
    private static final int LINE_SPLIT = 3;
    private static final int LINE_REVERSE = 4;
    private static final int LINE_MERGE = 5;
    private static final int LINE_FINAL = 0;

    /** Pinned priorities by value (smaller = higher in the tree). */
    private static final Map<Integer, Integer> PINNED_PRIO = Map.of(
            1, 5, 2, 2, 3, 8, 4, 1, 5, 6, 6, 3, 7, 7, 8, 4);

    /** One treap node (per-call state; the service bean is a singleton). */
    private static final class Node {
        final int value;
        final int prio;
        Node left;
        Node right;
        int size = 1;
        boolean rev;

        Node(int value, int prio) {
            this.value = value;
            this.prio = prio;
        }
    }

    /** Split result: first {@code k} nodes | the rest. */
    private static final class Pair {
        final Node left;
        final Node right;

        Pair(Node left, Node right) {
            this.left = left;
            this.right = right;
        }
    }

    private static int size(Node t) {
        return t == null ? 0 : t.size;
    }

    private static void update(Node t) {
        if (t != null) {
            t.size = 1 + size(t.left) + size(t.right);
        }
    }

    /** Pushes a set lazy flag down: swap the children, pass the flag on. */
    private static void push(Node t) {
        if (t != null && t.rev) {
            Node tmp = t.left;
            t.left = t.right;
            t.right = tmp;
            if (t.left != null) {
                t.left.rev ^= true;
            }
            if (t.right != null) {
                t.right.rev ^= true;
            }
            t.rev = false;
        }
    }

    /** Merges two trees whose keys are ordered (all of a before all of b). */
    private static Node merge(Node a, Node b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (a.prio < b.prio) {
            push(a);
            a.right = merge(a.right, b);
            update(a);
            return a;
        }
        push(b);
        b.left = merge(a, b.left);
        update(b);
        return b;
    }

    /** Splits off the first {@code k} nodes (inorder) into the left tree. */
    private static Pair split(Node t, int k) {
        if (t == null) {
            return new Pair(null, null);
        }
        push(t);
        if (size(t.left) >= k) {
            Pair p = split(t.left, k);
            t.left = p.right;
            update(t);
            return new Pair(p.left, t);
        }
        Pair p = split(t.right, k - size(t.left) - 1);
        t.right = p.left;
        update(t);
        return new Pair(t, p.right);
    }

    /** Priority of a value: pinned for 1..8, else a deterministic hash. */
    private static int priority(int value) {
        Integer pinned = PINNED_PRIO.get(value);
        if (pinned != null) {
            return pinned;
        }
        return (int) ((((long) value * 2654435761L) >>> 16) & 0x7fffffff);
    }

    /**
     * Builds the treap by merge-append, splits at
     * {@code values[0]}, then reverses [values[1]+1,
     * values[2]] (1-indexed) via split@1 and split@r.
     */
    public List<Step> implicitTreap(String word, List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        int[] parsed = parseArray(word);
        if (parsed == null || parsed.length == 0) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The array is needed as "
                            + "comma-separated numbers.", LINE_FINAL));
            return steps;
        }
        Set<Integer> seen = new HashSet<>();
        for (int v : parsed) {
            if (!seen.add(v)) {
                steps.add(new Step(List.of(), List.of(), List.of(),
                        List.of(), "The implicit treap needs "
                                + "distinct values (node ids are the "
                                + "values).", LINE_FINAL));
                return steps;
            }
        }
        if (values == null || values.size() != 3) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The split position and the reverse "
                            + "range are needed as [splitPos, l-1, "
                            + "r].", LINE_FINAL));
            return steps;
        }
        int n = parsed.length;
        int splitPos = values.get(0);
        int l = values.get(1) + 1;
        int r = values.get(2);
        if (splitPos < 0 || splitPos > n || l < 1 || r > n || l > r) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The split position or the reverse "
                            + "range is out of range.", LINE_FINAL));
            return steps;
        }

        Node root = null;
        for (int v : parsed) {
            root = merge(root, new Node(v, priority(v)));
            steps.add(new Step(List.of(size(root)), List.of(v),
                    List.of(), List.of(),
                    "Appended " + v + " (priority " + priority(v)
                            + "): tree size " + size(root) + ".",
                    LINE_BUILD, snapshot(root), edges(root)));
        }

        Pair first = split(root, splitPos);
        steps.add(new Step(List.of(size(first.left), size(first.right)),
                List.of(lastValue(first.left)), List.of(), List.of(),
                "Split at " + splitPos + ": left ["
                        + inorderValues(first.left) + "], right ["
                        + inorderValues(first.right) + "].",
                LINE_SPLIT, snapshot(first.left, first.right),
                edges(first.left, first.right)));
        root = merge(first.left, first.right);

        Pair a = split(root, l - 1);
        steps.add(new Step(List.of(size(a.left), size(a.right)),
                List.of(lastValue(a.left)), List.of(), List.of(),
                "Split at " + (l - 1) + " for the reverse: A ["
                        + inorderValues(a.left) + "], rest ["
                        + inorderValues(a.right) + "].",
                LINE_SPLIT, snapshot(a.left, a.right),
                edges(a.left, a.right)));
        Pair bc = split(a.right, r - l + 1);
        steps.add(new Step(List.of(size(bc.left), size(bc.right)),
                List.of(lastValue(bc.left)), List.of(), List.of(),
                "Split at " + r + " for the reverse: B ["
                        + inorderValues(bc.left) + "], C ["
                        + inorderValues(bc.right) + "].",
                LINE_SPLIT, snapshot(bc.left, bc.right),
                edges(bc.left, bc.right)));

        bc.left.rev ^= true;
        steps.add(new Step(List.of(l, r), List.of(bc.left.value),
                List.of(), List.of(),
                "Reversed [" + l + ", " + r + "]: lazy REV flag set "
                        + "on B's root " + bc.left.value + " — B's "
                        + "children are drawn swapped, no re-link.",
                LINE_REVERSE,
                snapshot(a.left, bc.left, bc.right),
                edges(a.left, bc.left, bc.right)));

        int cRoot = bc.right == null ? -1 : bc.right.value;
        Node mergedBc = merge(bc.left, bc.right);
        steps.add(new Step(List.of(size(mergedBc)),
                List.of(cRoot), List.of(), List.of(),
                "Merged B and C: size " + size(mergedBc) + ".",
                LINE_MERGE, snapshot(mergedBc), edges(mergedBc)));
        int bcRoot = mergedBc.value;
        root = merge(a.left, mergedBc);
        steps.add(new Step(List.of(size(root)),
                List.of(bcRoot), List.of(), List.of(),
                "Merged A and BC: final tree size " + size(root)
                        + ", inorder " + inorderValues(root) + ".",
                LINE_MERGE, snapshot(root), edges(root)));

        steps.add(new Step(List.of(n), List.of(), List.of(), List.of(),
                "Implicit treap done: reversed [" + l + ", " + r
                        + "], final inorder " + inorderValues(root)
                        + ".",
                LINE_FINAL, snapshot(root), edges(root)));
        return steps;
    }

    /**
     * Inorder values WITHOUT pushing: a set rev flag means
     * "visit right, then the node, then left". This must
     * never push — pushing would clear the flags the
     * snapshots still have to show. Only for messages.
     */
    private static List<Integer> inorderValues(Node t) {
        List<Integer> out = new ArrayList<>();
        inorderInto(t, out);
        return out;
    }

    private static void inorderInto(Node t, List<Integer> out) {
        if (t == null) {
            return;
        }
        if (t.rev) {
            inorderInto(t.right, out);
            out.add(t.value);
            inorderInto(t.left, out);
        } else {
            inorderInto(t.left, out);
            out.add(t.value);
            inorderInto(t.right, out);
        }
    }

    /** Last value of a tree's inorder (-1 for an empty tree). */
    private static int lastValue(Node t) {
        List<Integer> values = inorderValues(t);
        return values.isEmpty() ? -1 : values.get(values.size() - 1);
    }

    /** Snapshot of one tree: nodes by id ascending, WITHOUT pushing. */
    private static List<NodeDto> snapshot(Node... trees) {
        List<NodeDto> nodes = new ArrayList<>();
        for (Node t : trees) {
            collectNodes(t, nodes);
        }
        nodes.sort((x, y) -> Integer.compare(x.getId(), y.getId()));
        return nodes;
    }

    private static void collectNodes(Node t, List<NodeDto> out) {
        if (t == null) {
            return;
        }
        NodeDto dto = new NodeDto(t.value, t.prio, 0, 0);
        dto.setLabel(t.rev ? t.value + " (REV)" : String.valueOf(t.value));
        out.add(dto);
        collectNodes(t.left, out);
        collectNodes(t.right, out);
    }

    /** Edges of one tree, sorted by (from, to), WITHOUT pushing. */
    private static List<EdgeDto> edges(Node... trees) {
        List<EdgeDto> out = new ArrayList<>();
        for (Node t : trees) {
            collectEdges(t, out);
        }
        out.sort((x, y) -> x.getFrom() != y.getFrom()
                ? Integer.compare(x.getFrom(), y.getFrom())
                : Integer.compare(x.getTo(), y.getTo()));
        return out;
    }

    private static void collectEdges(Node t, List<EdgeDto> out) {
        if (t == null) {
            return;
        }
        if (t.left != null) {
            out.add(new EdgeDto(t.value, t.left.value));
        }
        if (t.right != null) {
            out.add(new EdgeDto(t.value, t.right.value));
        }
        collectEdges(t.left, out);
        collectEdges(t.right, out);
    }

    /** Parses "1,2,3,4,5,6,7,8" into an int array, or null. */
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
