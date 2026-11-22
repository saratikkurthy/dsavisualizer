package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bucket sort (Phase 15) that records every meaningful moment as
 * a Step, so the frontend can play the trace back
 * (Visualgo-style).
 *
 * Values are 0..99 (the controller rejects anything else), so
 * {@code nBuckets = max / 10 + 1} and bucket index = value / 10
 * (integer division) — 29 goes in bucket 2, not 3. The buckets
 * ride on the Step's {@code buckets} field, reused from radix
 * sort (Phase 4), so the frontend draws them with the same panel.
 *
 * Trace: one step per value placed (data = the input array, still
 * unsorted; highlight = the value's index in {@code values});
 * then one step per non-empty bucket sorted with insertion sort
 * (data = the output array rebuilt so far, slots not filled yet
 * are null, as in radix's collect phase; highlight = the original
 * indexes of that bucket's values — tracked per bucket, so
 * duplicates resolve to the right index); then a final step with
 * the sorted array. Line numbers: place = 2, sort a bucket = 3,
 * final = 0.
 */
@Service
public class BucketSortService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_SWAP = 3;
    private static final int LINE_FINAL = 0;

    /**
     * Bucket sort. The controller validates the input (non-empty,
     * at most 10 values, each 0..99); this method guards against
     * bad input anyway with a single "empty" Step.
     */
    public List<Step> bucketSort(List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(), List.of(),
                    "No values to sort.", LINE_FINAL));
            return steps;
        }
        int max = Collections.max(values);
        int nBuckets = max / 10 + 1;
        List<List<Integer>> buckets = new ArrayList<>();
        List<List<Integer>> bucketIndexes = new ArrayList<>();
        for (int b = 0; b < nBuckets; b++) {
            buckets.add(new ArrayList<>());
            bucketIndexes.add(new ArrayList<>());
        }
        // 1. Placement phase — one Step per value placed.
        for (int i = 0; i < values.size(); i++) {
            int v = values.get(i);
            int b = v / 10;
            buckets.get(b).add(v);
            bucketIndexes.get(b).add(i);
            steps.add(stepWithBuckets(new ArrayList<>(values), List.of(i),
                    "Put " + v + " into bucket " + b + ".",
                    LINE_COMPARE, copyBuckets(buckets)));
        }
        // 2. Sort each non-empty bucket with insertion sort and
        // rebuild the array bucket by bucket.
        List<Integer> out = new ArrayList<>(
                Collections.nCopies(values.size(), null));
        int idx = 0;
        for (int b = 0; b < nBuckets; b++) {
            List<Integer> bucket = buckets.get(b);
            if (bucket.isEmpty()) {
                continue;
            }
            insertionSort(bucket);
            for (int v : bucket) {
                out.set(idx, v);
                idx++;
            }
            steps.add(stepWithBuckets(new ArrayList<>(out),
                    new ArrayList<>(bucketIndexes.get(b)),
                    "Sorted bucket " + b + ": [" + joinInts(bucket)
                            + "].",
                    LINE_SWAP, copyBuckets(buckets)));
        }
        List<Integer> sorted = new ArrayList<>();
        for (Integer v : out) {
            sorted.add(v);
        }
        steps.add(new Step(sorted, List.of(), List.of(),
                allIndexes(values.size()),
                "Sorted: [" + joinInts(sorted) + "].", LINE_FINAL));
        return steps;
    }

    /** In-place insertion sort of one bucket. */
    private void insertionSort(List<Integer> bucket) {
        for (int i = 1; i < bucket.size(); i++) {
            int key = bucket.get(i);
            int j = i - 1;
            while (j >= 0 && bucket.get(j) > key) {
                bucket.set(j + 1, bucket.get(j));
                j--;
            }
            bucket.set(j + 1, key);
        }
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

    /** "21, 25, 29" from [21, 25, 29]. */
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
