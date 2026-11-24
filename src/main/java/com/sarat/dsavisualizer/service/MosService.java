package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mo's algorithm (Phase 26, range queries v2): offline
 * range queries answered by sliding a window across the
 * array — queries are sorted by (block of l, then r
 * ascending in even blocks and descending in odd blocks),
 * so the window moves only O((n + q) * sqrt(n)) steps in
 * total. Each move adds or removes one element at either
 * end; a frequency map keeps the answer (here the
 * distinct count) current in O(1) per move.
 *
 * <p>Demo array [1, 2, 1, 3, 2, 1, 4, 2], distinct-count
 * queries in INPUT order: [(4,7), (0,3), (1,6), (2,5)]
 * (deliberately unsorted so the sort does visible work).
 * Block size 3. Sorted order (odd-even: even blocks r
 * ascending, odd blocks r descending): [1, 3, 2, 0] —
 * i.e. (0,3), (2,5), (1,6), (4,7). Answers in input
 * order: [3, 3, 4, 3]. Total moves 14 (each move is one
 * add or remove at either end). Trace: 1 sort step (line
 * 1) + 14 move steps (line 2, one per add/remove) + final
 * (line 0) = 16 steps. Sort step: {@code data} = the
 * sorted query order [1, 3, 2, 0]. Move step: {@code
 * highlight} = [the index moved], {@code data} =
 * [distinct count so far], {@code sorted} = [current
 * window l, r]. Final step {@code data} = [14] (total
 * moves), {@code sorted} = the answers in input order
 * [3, 3, 4, 3]. The frequency map is derived by the
 * visualizer from the array + the window bounds — never
 * from the message. {@code nodes} = one NodeDto per array
 * element, in index order. No {@code edges} (null).</p>
 *
 * <p>Move order within one query (a design gap the design
 * leaves open): add-left, add-right, remove-left,
 * remove-right — i.e. the window first grows to cover the
 * new range, then shrinks from the side it no longer
 * needs. The demo's 14 moves are +0,+1,+2,+3 (to (0,3)),
 * +4,+5,&minus;0,&minus;1 (to (2,5)), +1,+6 (to (1,6)),
 * +7,&minus;1,&minus;2,&minus;3 (to (4,7)). Sort
 * ties (same block and same r) break by l ascending,
 * then input order.</p>
 *
 * <p>Bad input (an unparseable array, or a values list
 * that is not an even number of query pairs with
 * 0 &le; l &le; r &lt; n) yields a single "empty" Step;
 * the controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class MosService {

    private static final int LINE_SORT = 1;
    private static final int LINE_MOVE = 2;
    private static final int LINE_FINAL = 0;

    /** Per-call mutable state (the service bean is a singleton, so no instance state). */
    private static final class Window {
        final int[] array;
        final Map<Integer, Integer> freq = new HashMap<>();
        int distinct = 0;
        int left = 0;
        int right = -1;

        Window(int[] array) {
            this.array = array;
        }

        void add(int index) {
            int value = array[index];
            int count = freq.getOrDefault(value, 0);
            if (count == 0) {
                distinct++;
            }
            freq.put(value, count + 1);
        }

        void remove(int index) {
            int value = array[index];
            int count = freq.get(value);
            if (count == 1) {
                distinct--;
                freq.remove(value);
            } else {
                freq.put(value, count - 1);
            }
        }

        /** One NodeDto per array element, in index order. */
        List<NodeDto> snapshot() {
            List<NodeDto> dtos = new ArrayList<>();
            for (int i = 0; i < array.length; i++) {
                NodeDto dto = new NodeDto(i, array[i], 0, 0);
                dto.setLabel(String.valueOf(array[i]));
                dtos.add(dto);
            }
            return dtos;
        }
    }

    /**
     * Answers the distinct-count queries in {@code values}
     * (flattened [l, r] pairs, in input order) over the
     * array in {@code word}, in Mo's order.
     */
    public List<Step> mos(String word, List<Integer> values) {
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
                    List.of(), "The queries are needed as "
                            + "flattened [l, r] pairs.", LINE_FINAL));
            return steps;
        }
        int queryCount = values.size() / 2;
        List<int[]> queries = new ArrayList<>();
        for (int i = 0; i < queryCount; i++) {
            int l = values.get(2 * i);
            int r = values.get(2 * i + 1);
            if (l < 0 || r >= parsed.length || l > r) {
                steps.add(new Step(List.of(), List.of(), List.of(),
                        List.of(), "Query [" + l + ", " + r
                                + "] is out of range.", LINE_FINAL));
                return steps;
            }
            queries.add(new int[]{l, r});
        }
        int blockSize = (int) Math.ceil(Math.sqrt(parsed.length));
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < queryCount; i++) {
            order.add(i);
        }
        order.sort((a, b) -> {
            int blockA = queries.get(a)[0] / blockSize;
            int blockB = queries.get(b)[0] / blockSize;
            if (blockA != blockB) {
                return Integer.compare(blockA, blockB);
            }
            int rA = queries.get(a)[1];
            int rB = queries.get(b)[1];
            if (rA != rB) {
                return blockA % 2 == 0
                        ? Integer.compare(rA, rB)
                        : Integer.compare(rB, rA);
            }
            int lA = queries.get(a)[0];
            int lB = queries.get(b)[0];
            if (lA != lB) {
                return Integer.compare(lA, lB);
            }
            return Integer.compare(a, b);
        });
        Window window = new Window(parsed);
        steps.add(new Step(new ArrayList<>(order), List.of(),
                List.of(), List.of(),
                "Sorted " + queryCount + " queries into Mo's "
                        + "order " + order + ".",
                LINE_SORT, window.snapshot(), null));

        List<Integer> answers = new ArrayList<>();
        for (int i = 0; i < queryCount; i++) {
            answers.add(0);
        }
        int moves = 0;
        for (int qi : order) {
            int targetL = queries.get(qi)[0];
            int targetR = queries.get(qi)[1];
            while (window.left > targetL) {
                window.left--;
                window.add(window.left);
                moves++;
                steps.add(moveStep(window, window.left,
                        "Add a[" + window.left + "] = "
                                + parsed[window.left] + "."));
            }
            while (window.right < targetR) {
                window.right++;
                window.add(window.right);
                moves++;
                steps.add(moveStep(window, window.right,
                        "Add a[" + window.right + "] = "
                                + parsed[window.right] + "."));
            }
            while (window.left < targetL) {
                int removed = window.left;
                window.remove(removed);
                window.left++;
                moves++;
                steps.add(moveStep(window, removed,
                        "Remove a[" + removed + "] = "
                                + parsed[removed] + "."));
            }
            while (window.right > targetR) {
                int removed = window.right;
                window.remove(removed);
                window.right--;
                moves++;
                steps.add(moveStep(window, removed,
                        "Remove a[" + removed + "] = "
                                + parsed[removed] + "."));
            }
            answers.set(qi, window.distinct);
        }
        steps.add(new Step(List.of(moves), List.of(), List.of(),
                new ArrayList<>(answers),
                "Mo's algorithm done: " + moves + " moves; "
                        + "answers " + answers + ".",
                LINE_FINAL, window.snapshot(), null));
        return steps;
    }

    /** One move step: the window and distinct count after the move. */
    private Step moveStep(Window window, int moved, String what) {
        return new Step(List.of(window.distinct), List.of(moved),
                List.of(), List.of(window.left, window.right),
                what + " Window [" + window.left + ", "
                        + window.right + "], distinct "
                        + window.distinct + ".",
                LINE_MOVE, window.snapshot(), null);
    }

    /** Parses "1,2,1,3,2,1,4,2" into an int array, or null. */
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
