package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Counting sort that records every meaningful moment as a Step,
 * so the frontend can play the trace back (Visualgo-style).
 *
 * The count array rides on the Step's {@code aux} field so the
 * frontend can draw it under the bars. During the placement phase
 * {@code data} is the output array being built — slots not filled
 * yet are null (Jackson serialises them as JSON null, same as the
 * hash table's empty buckets).
 *
 * Line numbers: count = 2, cumulative / place = 3, final = 0.
 * The controller rejects negative values and max &gt; 100, so the
 * count array always stays drawable.
 */
@Service
public class CountingService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_SWAP = 3;
    private static final int LINE_FINAL = 0;

    /** Counting sort. Emits a Step per count, per cumulative index,
     * and per element placed (stable: input is read last to first). */
    public List<Step> countingSort(List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(), List.of(),
                    "No values to sort.", LINE_FINAL));
            return steps;
        }
        if (values.size() == 1) {
            steps.add(new Step(new ArrayList<>(values), List.of(), List.of(),
                    List.of(0), "Already sorted.", LINE_FINAL));
            return steps;
        }

        List<Integer> a = new ArrayList<>(values);
        int n = a.size();
        int max = Collections.max(a);
        List<Integer> count = new ArrayList<>(
                Collections.nCopies(max + 1, 0));

        // 1. Count phase — one Step per element, data unchanged.
        for (int i = 0; i < n; i++) {
            int v = a.get(i);
            count.set(v, count.get(v) + 1);
            steps.add(stepWithAux(new ArrayList<>(a), List.of(i),
                    "Count " + v + ": count[" + v + "] is now "
                            + count.get(v) + ".",
                    LINE_COMPARE, new ArrayList<>(count)));
        }

        // 2. Cumulative phase — one Step per index j = 1..max.
        for (int j = 1; j <= max; j++) {
            count.set(j, count.get(j) + count.get(j - 1));
            steps.add(stepWithAux(new ArrayList<>(a), List.of(),
                    "Cumulative count up to " + j + " is "
                            + count.get(j) + ".",
                    LINE_SWAP, new ArrayList<>(count)));
        }

        // 3. Placement phase — input read LAST to FIRST (stable).
        List<Integer> out = new ArrayList<>(
                Collections.nCopies(n, null));
        for (int i = n - 1; i >= 0; i--) {
            int v = a.get(i);
            int pos = count.get(v) - 1;
            out.set(pos, v);
            count.set(v, count.get(v) - 1);
            steps.add(stepWithAux(new ArrayList<>(out), List.of(pos),
                    "Place " + v + " at position " + pos + ".",
                    LINE_SWAP, new ArrayList<>(count)));
        }

        // 4. Final — sorted output, aux cleared.
        steps.add(new Step(new ArrayList<>(out), List.of(), List.of(),
                allIndexes(n), "Sorted!", LINE_FINAL));
        return steps;
    }

    /** A Step carrying an aux (count array) snapshot. */
    private Step stepWithAux(List<Integer> data, List<Integer> highlight,
                             String message, int line, List<Integer> aux) {
        Step step = new Step(data, highlight, List.of(), List.of(),
                message, line);
        step.setAux(aux);
        return step;
    }

    /** [0, 1, ..., n-1] — used for the final "all sorted" step. */
    private List<Integer> allIndexes(int n) {
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            all.add(i);
        }
        return all;
    }
}
