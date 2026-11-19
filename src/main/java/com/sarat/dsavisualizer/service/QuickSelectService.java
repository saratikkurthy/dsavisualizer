package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * QuickSelect (Phase 16): the k-th smallest element (k 1-based),
 * found by Lomuto partitioning — the exact partition the existing
 * QuickSort uses (last element as pivot) — but only one side is
 * recursed into. It lives in its own service (not in
 * SortingService): QuickSort's helper inlines the Lomuto loop
 * with per-comparison/per-swap steps, two-sided recursion and a
 * {@code sorted} list, none of which QuickSelect reuses —
 * QuickSelect needs one step per partition, one-sided recursion
 * and a target index.
 *
 * Trace: one step per partition; {@code data} is the array
 * snapshot after the partition, {@code highlight} is the pivot
 * index. A partition that keeps searching is line 2; the
 * partition that lands the pivot on the target index is line 3;
 * the summary is line 0 and sets {@code sorted} to the target
 * index (the found element renders green, the existing
 * convention).
 */
@Service
public class QuickSelectService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_MATCH = 3;
    private static final int LINE_FINAL = 0;

    /**
     * The k-th smallest of {@code values} (k is 1-based). The
     * controller validates the input (2..10 values, |v| <= 999,
     * k 1..n); this method guards against bad input anyway with
     * a single "empty" Step.
     */
    public List<Step> quickSelect(List<Integer> values, int k) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()
                || k < 1 || k > values.size()) {
            steps.add(qsStep(List.of(), List.of(), List.of(),
                    "A list of values and a k between 1 and n "
                            + "are needed.",
                    LINE_FINAL));
            return steps;
        }
        List<Integer> a = new ArrayList<>(values);
        int target = k - 1;
        int lo = 0;
        int hi = a.size() - 1;
        while (true) {
            int pivot = a.get(hi);
            int i = lo;
            for (int j = lo; j < hi; j++) {
                if (a.get(j) < pivot) {
                    int tmp = a.get(i);
                    a.set(i, a.get(j));
                    a.set(j, tmp);
                    i++;
                }
            }
            int tmp = a.get(i);
            a.set(i, a.get(hi));
            a.set(hi, tmp);
            if (i == target) {
                steps.add(qsStep(a, List.of(i), List.of(),
                        "Partition [" + lo + ".." + hi + "] with pivot "
                                + pivot + ": pivot lands at index " + i
                                + " — found: the " + ordinal(k)
                                + " smallest is " + a.get(i) + ".",
                        LINE_MATCH));
                break;
            }
            String side = target < i ? "left" : "right";
            steps.add(qsStep(a, List.of(i), List.of(),
                    "Partition [" + lo + ".." + hi + "] with pivot "
                            + pivot + ": pivot lands at index " + i + ". "
                            + ordinal(k) + " smallest is to the " + side
                            + ".",
                    LINE_COMPARE));
            if (target < i) {
                hi = i - 1;
            } else {
                lo = i + 1;
            }
        }
        steps.add(qsStep(a, List.of(), List.of(target),
                "The " + ordinal(k) + " smallest is " + a.get(target)
                        + ".",
                LINE_FINAL));
        return steps;
    }

    /**
     * "1st", "2nd", "3rd", "4th" — the design doc writes "{k}th"
     * throughout, which would render "3th" for k = 3.
     */
    private String ordinal(int k) {
        if (k % 100 >= 11 && k % 100 <= 13) {
            return k + "th";
        }
        switch (k % 10) {
            case 1:
                return k + "st";
            case 2:
                return k + "nd";
            case 3:
                return k + "rd";
            default:
                return k + "th";
        }
    }

    /** One QuickSelect Step: array snapshot, pivot highlight. */
    private Step qsStep(List<Integer> data, List<Integer> highlight,
                        List<Integer> sorted, String message, int line) {
        return new Step(new ArrayList<>(data), highlight, List.of(),
                sorted, message, line);
    }
}
