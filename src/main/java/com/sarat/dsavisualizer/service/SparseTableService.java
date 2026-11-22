package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Sparse table RMQ (Phase 26, range queries v2): st[k][i]
 * = the minimum over [i, i+2^k) — built in O(n log n) by
 * doubling, then any range-minimum query is answered in
 * O(1) by overlapping two blocks of size 2^k that cover
 * the range (overlap is fine for an idempotent operation
 * like min). The static-array counterpart of the segment
 * tree: faster queries, no updates.
 *
 * <p>Demo array [3, 1, 4, 1, 5, 9, 2, 6] (same as the
 * sqrt-decomposition demo, for the comparison). st[0] =
 * [3, 1, 4, 1, 5, 9, 2, 6]; st[1] = [1, 1, 1, 1, 5, 2,
 * 2]; st[2] = [1, 1, 1, 1, 2]; st[3] = [1]. Demo query
 * [2, 6] (len 5, k=2): intervals [2, 5] and [3, 6] &rarr;
 * min 1. Second demo query [0, 7] (len 8, k=3): the two
 * intervals coincide ([0, 7] and [0, 7]) &rarr; min 1 —
 * the edge case where the overlap is total. Trace: 4 build
 * steps (line 1, one per k = 0..3) + 2 query steps per
 * query (line 2, one per interval) &times; 2 queries +
 * final (line 0) = 9 steps. Build step: {@code data} =
 * st[k] (the level's values), {@code sorted} = the same
 * st[k] values. Query step: {@code highlight} = [the
 * interval's indices], {@code data} = [the interval min].
 * Final step {@code data} = [1, 1] (the two query
 * answers). The visualizer reconstructs the table from
 * the build steps' {@code data} — never from the message.
 * {@code nodes} = one NodeDto per array element, in index
 * order. No {@code edges} (null).</p>
 *
 * <p>Bad input (an unparseable array, or a values list
 * that is not exactly two [l, r] query ranges with
 * 0 &le; l &le; r &lt; n) yields a single "empty" Step;
 * the controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class SparseTableService {

    private static final int LINE_BUILD = 1;
    private static final int LINE_QUERY = 2;
    private static final int LINE_FINAL = 0;

    /**
     * Builds the sparse table over the array in {@code
     * word} and answers the two query ranges in {@code
     * values} ([l1, r1, l2, r2]).
     */
    public List<Step> sparseTable(String word, List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        int[] parsed = parseArray(word);
        if (parsed == null || parsed.length == 0) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The array is needed as "
                            + "comma-separated numbers.", LINE_FINAL));
            return steps;
        }
        if (values == null || values.size() != 4) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Two query ranges are needed as "
                            + "[l1, r1, l2, r2].", LINE_FINAL));
            return steps;
        }
        int n = parsed.length;
        for (int i = 0; i < 4; i += 2) {
            int l = values.get(i);
            int r = values.get(i + 1);
            if (l < 0 || r >= n || l > r) {
                steps.add(new Step(List.of(), List.of(), List.of(),
                        List.of(), "Query range [" + l + ", " + r
                                + "] is out of range.", LINE_FINAL));
                return steps;
            }
        }
        int maxK = 31 - Integer.numberOfLeadingZeros(n);
        List<List<Integer>> table = new ArrayList<>();
        List<Integer> level0 = new ArrayList<>();
        for (int value : parsed) {
            level0.add(value);
        }
        table.add(level0);
        for (int k = 1; k <= maxK; k++) {
            List<Integer> prev = table.get(k - 1);
            List<Integer> level = new ArrayList<>();
            int span = 1 << (k - 1);
            for (int i = 0; i + span < prev.size(); i++) {
                level.add(Math.min(prev.get(i), prev.get(i + span)));
            }
            table.add(level);
        }
        List<NodeDto> nodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            NodeDto dto = new NodeDto(i, parsed[i], 0, 0);
            dto.setLabel(String.valueOf(parsed[i]));
            nodes.add(dto);
        }
        for (int k = 0; k <= maxK; k++) {
            List<Integer> level = table.get(k);
            steps.add(new Step(new ArrayList<>(level), List.of(),
                    List.of(), new ArrayList<>(level),
                    "Level k=" + k + ": minima over intervals of "
                            + "length " + (1 << k) + " — " + level
                            + ".",
                    LINE_BUILD, new ArrayList<>(nodes), null));
        }
        List<Integer> answers = new ArrayList<>();
        for (int q = 0; q < 2; q++) {
            int l = values.get(2 * q);
            int r = values.get(2 * q + 1);
            int len = r - l + 1;
            int k = 31 - Integer.numberOfLeadingZeros(len);
            int span = 1 << k;
            int first = rangeMin(table, k, l);
            int second = rangeMin(table, k, r - span + 1);
            steps.add(queryStep(table, k, l, span,
                    "Query [" + l + ", " + r + "]: interval ["
                            + l + ", " + (l + span - 1) + "] min "
                            + first + ".", nodes));
            steps.add(queryStep(table, k, r - span + 1, span,
                    "Query [" + l + ", " + r + "]: interval ["
                            + (r - span + 1) + ", " + r + "] min "
                            + second + ".", nodes));
            answers.add(Math.min(first, second));
        }
        steps.add(new Step(new ArrayList<>(answers), List.of(),
                List.of(), List.of(),
                "Sparse table done: query answers " + answers
                        + ".",
                LINE_FINAL, new ArrayList<>(nodes), null));
        return steps;
    }

    /** One query step: the interval's indices and its minimum. */
    private Step queryStep(List<List<Integer>> table, int k, int start,
                           int span, String message, List<NodeDto> nodes) {
        List<Integer> highlight = new ArrayList<>();
        for (int i = start; i < start + span; i++) {
            highlight.add(i);
        }
        return new Step(List.of(table.get(k).get(start)), highlight,
                List.of(), List.of(), message, LINE_QUERY,
                new ArrayList<>(nodes), null);
    }

    /** The precomputed minimum over [start, start + 2^k). */
    private int rangeMin(List<List<Integer>> table, int k, int start) {
        return table.get(k).get(start);
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
