package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 0/1 Knapsack that records the DP table as it fills, one Step per
 * cell computed.
 *
 * The table has rows 0..n (row i = first i items) and columns 0..W
 * (capacity). Row 0 is the base case (no items → value 0) and is
 * pre-filled, so the trace starts at row 1. Uncomputed cells are
 * JSON null in the snapshot; the frontend renders them empty/grey.
 * {@code highlight} is [row, col] (the cell being computed).
 * Message format for a computed cell:
 * {@code dp[2][5] = max(dp[1][5], 4 + dp[1][2]) = max(3, 7) = 7.}
 * When the item is taken (the value improves over the cell above),
 * the step is line 3 instead of line 2. The final Step reports the
 * maximum value and the taken items (0-based indices, traced back
 * from (n, W)) with the table complete and highlight = [n, W].
 */
@Service
public class KnapsackService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_TAKE = 3;
    private static final int LINE_FINAL = 0;

    /**
     * 0/1 Knapsack. {@code weights} and {@code values} must have
     * the same length; {@code capacity} is the knapsack capacity.
     * The controller validates the limits (at most 10 items,
     * capacity 0..30, weights > 0, values >= 0); this method
     * guards against bad input anyway with a single "empty" Step.
     */
    public List<Step> knapsack(List<Integer> weights, List<Integer> values,
                               Integer capacity) {
        List<Step> steps = new ArrayList<>();
        if (weights == null || values == null || weights.isEmpty()
                || values.isEmpty() || weights.size() != values.size()
                || capacity == null) {
            steps.add(knapsackStep(List.of(), List.of(),
                    "No items to pack.", LINE_FINAL));
            return steps;
        }
        int n = weights.size();
        int w = capacity;
        // Table rows 0..n, columns 0..W; null = uncomputed.
        // Row 0 is the base case (no items → value 0), pre-filled.
        List<List<Integer>> table = new ArrayList<>();
        List<Integer> row0 = new ArrayList<>();
        for (int c = 0; c <= w; c++) {
            row0.add(0);
        }
        table.add(row0);
        for (int i = 1; i <= n; i++) {
            List<Integer> row = new ArrayList<>();
            for (int c = 0; c <= w; c++) {
                row.add(null);
            }
            table.add(row);
        }
        for (int i = 1; i <= n; i++) {
            int wi = weights.get(i - 1);
            int vi = values.get(i - 1);
            for (int c = 0; c <= w; c++) {
                int value;
                int line;
                String message;
                if (wi > c) {
                    // The item does not fit: take the cell above.
                    value = table.get(i - 1).get(c);
                    line = LINE_COMPARE;
                    message = "dp[" + i + "][" + c + "] = dp[" + (i - 1)
                            + "][" + c + "] = " + value + " (item weighs "
                            + wi + ", too heavy for capacity " + c + ").";
                } else {
                    int skip = table.get(i - 1).get(c);
                    int take = vi + table.get(i - 1).get(c - wi);
                    if (take > skip) {
                        value = take;
                        line = LINE_TAKE;
                    } else {
                        value = skip;
                        line = LINE_COMPARE;
                    }
                    message = "dp[" + i + "][" + c + "] = max(dp["
                            + (i - 1) + "][" + c + "], " + vi + " + dp["
                            + (i - 1) + "][" + (c - wi) + "]) = max("
                            + skip + ", " + take + ") = " + value + ".";
                }
                table.get(i).set(c, value);
                steps.add(knapsackStep(table, List.of(i, c), message, line));
            }
        }
        // Trace back from (n, W) to find the taken items (0-based).
        List<Integer> taken = new ArrayList<>();
        int c = w;
        for (int i = n; i >= 1; i--) {
            if (!table.get(i).get(c).equals(table.get(i - 1).get(c))) {
                taken.add(i - 1);
                c -= weights.get(i - 1);
            }
        }
        Collections.reverse(taken);
        String takenText = taken.isEmpty() ? "none" : joinInts(taken);
        steps.add(knapsackStep(table, List.of(n, w),
                "Maximum value: " + table.get(n).get(w)
                        + ". Items taken: " + takenText + ".",
                LINE_FINAL));
        return steps;
    }

    /**
     * One knapsack Step: a deep copy of the table, highlight =
     * [row, col] (the cell being computed).
     */
    private Step knapsackStep(List<List<Integer>> table,
                              List<Integer> highlight,
                              String message, int line) {
        Step step = new Step(List.of(), highlight, List.of(), List.of(),
                message, line);
        step.setTable(copyTable(table));
        return step;
    }

    /**
     * Deep copy of the DP table (null cells included), so later
     * steps never mutate earlier snapshots.
     */
    private List<List<Integer>> copyTable(List<List<Integer>> table) {
        List<List<Integer>> copy = new ArrayList<>();
        for (List<Integer> row : table) {
            copy.add(new ArrayList<>(row));
        }
        return copy;
    }

    /** "0, 1" from [0, 1]. */
    private String joinInts(List<Integer> ints) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ints.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(ints.get(i));
        }
        return sb.toString();
    }
}
