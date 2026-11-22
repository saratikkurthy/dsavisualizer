package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Shell sort that records every meaningful moment as a Step.
 *
 * Gaps n/2, n/4, ..., 1 (integer division). For each gap, a
 * gapped insertion sort that swaps out-of-order neighbours gap
 * apart. Steps: compare (line 2), swap (line 3), final "Sorted!"
 * (line 0). No index is in its final place until the last gap
 * completes, so {@code sorted} stays empty during the passes.
 */
@Service
public class ShellSortService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_SWAP = 3;
    private static final int LINE_FINAL = 0;

    /**
     * Shell sort. For each gap, run a gapped insertion sort:
     * walk i from gap to n-1, and while a[j] < a[j - gap], swap
     * them and step j back by gap.
     */
    public List<Step> shellSort(List<Integer> values) {
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
        for (int gap = n / 2; gap >= 1; gap /= 2) {
            for (int i = gap; i < n; i++) {
                int j = i;
                while (j >= gap) {
                    int left = a.get(j - gap);
                    int right = a.get(j);
                    steps.add(new Step(new ArrayList<>(a), List.of(j, j - gap),
                            List.of(), List.of(),
                            "Gap " + gap + ": compare " + left + " and "
                                    + right + ".",
                            LINE_COMPARE));
                    if (right >= left) {
                        break;
                    }
                    a.set(j - gap, right);
                    a.set(j, left);
                    steps.add(new Step(new ArrayList<>(a), List.of(j, j - gap),
                            List.of(j, j - gap), List.of(),
                            "Gap " + gap + ": swap " + left + " and "
                                    + right + ".",
                            LINE_SWAP));
                    j -= gap;
                }
            }
        }
        steps.add(new Step(new ArrayList<>(a), List.of(), List.of(),
                allIndexes(n), "Sorted!", LINE_FINAL));
        return steps;
    }

    /** All indexes 0..n-1. */
    private List<Integer> allIndexes(int n) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            indexes.add(i);
        }
        return indexes;
    }
}
