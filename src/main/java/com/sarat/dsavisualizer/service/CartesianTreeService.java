package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cartesian tree (Phase 28, range queries v4): the
 * tree built from an array so that an inorder
 * traversal gives the array back and every parent is
 * smaller than its children (min-heap order). It is
 * the tree a range-minimum query walks: the LCA of
 * two positions is the minimum of the range between
 * them. The linear build keeps a stack of the
 * right spine: for each new element, pop while the
 * stack top is LARGER (strictly — equal values stay,
 * so the first of two equal values becomes the
 * ancestor), the last popped node becomes the new
 * node's left child, and the new node becomes the
 * right child of whatever is left on the stack.
 *
 * <p>Demo [3, 1, 4, 1, 5, 9, 2, 6]. Root = index 1
 * (value 1). Parents: {0:1, 2:3, 3:1, 4:6, 5:4, 6:3,
 * 7:6}; left {1:0, 3:2, 6:4}, right {1:3, 3:6, 4:5,
 * 6:7}. Trace: 8 insertion steps (line 2) + final
 * (line 0) = 9 steps. {@code nodes}: id = array
 * index 0..7, {@code value} = a[i], {@code label} =
 * "a[i]" (e.g. "3", "1"). {@code edges}:
 * parent-&gt;child, weight null. Insertion steps:
 * nodes = the tree so far (only inserted nodes);
 * {@code highlight} = [the new node id]; {@code
 * data} = [stack size after this insertion] — the
 * visualizer shows the stack as the right-spine
 * path (demo stack sizes: [1, 1, 2, 2, 3, 4, 3, 4]).
 * Final step: nodes = full tree, {@code data} = [1]
 * (root index). Nodes are listed by id ascending,
 * edges sorted by (from, to).</p>
 *
 * <p>Bad input (a missing/unparseable array, or one
 * with fewer than 2 elements) yields a single
 * "empty" Step; the controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class CartesianTreeService {

    private static final int LINE_INSERT = 2;
    private static final int LINE_FINAL = 0;

    /** Cartesian tree of the array in {@code word}, one step per insertion. */
    public List<Step> cartesianTree(String word) {
        List<Step> steps = new ArrayList<>();
        int[] parsed = parseArray(word);
        if (parsed == null || parsed.length < 2) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The Cartesian tree needs an "
                            + "array of at least 2 numbers.",
                    LINE_FINAL));
            return steps;
        }
        int n = parsed.length;
        Map<Integer, Integer> parent = new HashMap<>();
        Map<Integer, Integer> left = new HashMap<>();
        Map<Integer, Integer> right = new HashMap<>();
        Deque<Integer> stack = new ArrayDeque<>();
        int root = -1;
        for (int i = 0; i < n; i++) {
            Integer last = null;
            while (!stack.isEmpty()
                    && parsed[stack.peek()] > parsed[i]) {
                last = stack.pop();
            }
            if (!stack.isEmpty()) {
                right.put(stack.peek(), i);
                parent.put(i, stack.peek());
            }
            if (last != null) {
                left.put(i, last);
                parent.put(last, i);
            }
            stack.push(i);
            root = findRoot(parent, i);
            steps.add(new Step(List.of(stack.size()), List.of(i),
                    List.of(), List.of(),
                    "Inserted a[" + i + "]=" + parsed[i]
                            + (last != null
                                    ? ": popped " + last + " as the "
                                            + "left child"
                                    : ": no pop")
                            + "; stack size " + stack.size() + ".",
                    LINE_INSERT, snapshot(parsed, i),
                    edges(parent, i)));
        }
        steps.add(new Step(List.of(root), List.of(), List.of(),
                List.of(),
                "Cartesian tree done: root is index " + root
                        + " (value " + parsed[root] + "), inorder is "
                        + "the array, heap order holds.",
                LINE_FINAL, snapshot(parsed, n - 1),
                edges(parent, n - 1)));
        return steps;
    }

    /** The node with no parent among 0..last (the tree-so-far root). */
    private static int findRoot(Map<Integer, Integer> parent, int last) {
        for (int i = 0; i <= last; i++) {
            if (!parent.containsKey(i)) {
                return i;
            }
        }
        return last;
    }

    /** Nodes 0..last, by id ascending. */
    private static List<NodeDto> snapshot(int[] array, int last) {
        List<NodeDto> nodes = new ArrayList<>();
        for (int i = 0; i <= last; i++) {
            NodeDto dto = new NodeDto(i, array[i], 0, 0);
            dto.setLabel(String.valueOf(array[i]));
            nodes.add(dto);
        }
        return nodes;
    }

    /** Parent-&gt;child edges among 0..last, sorted by (from, to). */
    private static List<EdgeDto> edges(Map<Integer, Integer> parent, int last) {
        List<EdgeDto> out = new ArrayList<>();
        for (int i = 0; i <= last; i++) {
            Integer p = parent.get(i);
            if (p != null) {
                out.add(new EdgeDto(p, i));
            }
        }
        out.sort((x, y) -> x.getFrom() != y.getFrom()
                ? Integer.compare(x.getFrom(), y.getFrom())
                : Integer.compare(x.getTo(), y.getTo()));
        return out;
    }

    /** Parses "3,1,4,1,5,9,2,6" into an int array, or null. */
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
