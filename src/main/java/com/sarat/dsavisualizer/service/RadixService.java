package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Radix sort (LSD, base 10) that records every meaningful moment
 * as a Step, so the frontend can play the trace back
 * (Visualgo-style).
 *
 * The 10 digit buckets ride on the Step's {@code buckets} field so
 * the frontend can draw them under the bars. During the collect
 * phase {@code data} is the array being rebuilt — slots not filled
 * yet are null (Jackson serialises them as JSON null, same as the
 * hash table's empty buckets).
 *
 * Line numbers: bucket = 2, collect = 3, final = 0. The controller
 * rejects negative values and max &gt; 999999, so there are at
 * most 6 passes.
 */
@Service
public class RadixService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_SWAP = 3;
    private static final int LINE_FINAL = 0;

    /** Place names for the digit being processed, pass 0..5. */
    private static final String[] PLACE_NAMES = {
        "ones", "tens", "hundreds", "thousands",
        "ten thousands", "hundred thousands"
    };

    /**
     * Radix sort. One bucket phase + one collect phase per digit
     * place, from the ones place up to the most significant digit
     * of the max value.
     */
    public List<Step> radixSort(List<Integer> values) {
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
        // Number of digits of the max value; at least 1 pass.
        int passes = String.valueOf(max).length();

        for (int p = 0; p < passes; p++) {
            int place = (int) Math.pow(10, p);
            String placeName = PLACE_NAMES[p];

            // 1. Bucket phase — one Step per element, data unchanged.
            List<List<Integer>> buckets = emptyBuckets();
            for (int i = 0; i < n; i++) {
                int v = a.get(i);
                int digit = (v / place) % 10;
                buckets.get(digit).add(v);
                steps.add(stepWithBuckets(new ArrayList<>(a), List.of(i),
                        v + " has " + placeName + " digit " + digit
                                + " — into bucket " + digit + ".",
                        LINE_COMPARE, copyBuckets(buckets)));
            }

            // 2. Collect phase — buckets 0..9, in insertion order.
            List<Integer> out = new ArrayList<>(
                    Collections.nCopies(n, null));
            int idx = 0;
            for (int b = 0; b < 10; b++) {
                for (int v : buckets.get(b)) {
                    out.set(idx, v);
                    steps.add(stepWithBuckets(new ArrayList<>(out),
                            List.of(idx),
                            "Collect " + v + " from bucket " + b + ".",
                            LINE_SWAP, copyBuckets(buckets)));
                    idx++;
                }
            }
            a = out;
        }

        steps.add(new Step(new ArrayList<>(a), List.of(), List.of(),
                allIndexes(n), "Sorted!", LINE_FINAL));
        return steps;
    }

    /** Ten empty bucket lists. */
    private List<List<Integer>> emptyBuckets() {
        List<List<Integer>> buckets = new ArrayList<>();
        for (int b = 0; b < 10; b++) {
            buckets.add(new ArrayList<>());
        }
        return buckets;
    }

    /** A deep copy of the buckets, so later steps do not mutate
     * earlier snapshots. */
    private List<List<Integer>> copyBuckets(List<List<Integer>> buckets) {
        List<List<Integer>> copy = new ArrayList<>();
        for (List<Integer> bucket : buckets) {
            copy.add(new ArrayList<>(bucket));
        }
        return copy;
    }

    /** A Step carrying a buckets snapshot. */
    private Step stepWithBuckets(List<Integer> data,
                                 List<Integer> highlight,
                                 String message, int line,
                                 List<List<Integer>> buckets) {
        Step step = new Step(data, highlight, List.of(), List.of(),
                message, line);
        step.setBuckets(buckets);
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
