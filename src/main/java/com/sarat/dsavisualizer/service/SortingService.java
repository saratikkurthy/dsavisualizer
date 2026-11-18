package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Sorting algorithms that record every meaningful moment as a Step,
 * so the frontend can play the trace back (Visualgo-style).
 *
 * Line numbers are small ints kept simple on purpose:
 * compare = 2, swap = 3, final = 0. The frontend highlights the
 * matching line in its code panel.
 */
@Service
public class SortingService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_SWAP = 3;
    private static final int LINE_FINAL = 0;

    /** Classic bubble sort. Emits a Step per comparison and per swap. */
    public List<Step> bubbleSort(List<Integer> values) {
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
        List<Integer> sorted = new ArrayList<>();

        for (int pass = 0; pass < n - 1; pass++) {
            for (int j = 0; j < n - 1 - pass; j++) {
                steps.add(new Step(new ArrayList<>(a), List.of(j, j + 1),
                        List.of(), new ArrayList<>(sorted),
                        "Compare " + a.get(j) + " and " + a.get(j + 1) + ".",
                        LINE_COMPARE));
                if (a.get(j) > a.get(j + 1)) {
                    int tmp = a.get(j);
                    a.set(j, a.get(j + 1));
                    a.set(j + 1, tmp);
                    steps.add(new Step(new ArrayList<>(a), List.of(j, j + 1),
                            List.of(j, j + 1), new ArrayList<>(sorted),
                            "Swap " + a.get(j + 1) + " and " + a.get(j) + ".",
                            LINE_SWAP));
                }
            }
            // The largest of this pass has bubbled to its final place.
            sorted.add(0, n - 1 - pass);
        }
        steps.add(new Step(new ArrayList<>(a), List.of(), List.of(),
                allIndexes(n), "Sorted!", LINE_FINAL));
        return steps;
    }

    /** Selection sort. Emits a Step per element checked and per swap. */
    public List<Step> selectionSort(List<Integer> values) {
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
        List<Integer> sorted = new ArrayList<>();

        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++) {
                steps.add(new Step(new ArrayList<>(a), List.of(minIdx, j),
                        List.of(), new ArrayList<>(sorted),
                        "Compare current minimum " + a.get(minIdx)
                                + " with " + a.get(j) + ".",
                        LINE_COMPARE));
                if (a.get(j) < a.get(minIdx)) {
                    minIdx = j;
                }
            }
            if (minIdx != i) {
                int tmp = a.get(i);
                a.set(i, a.get(minIdx));
                a.set(minIdx, tmp);
                steps.add(new Step(new ArrayList<>(a), List.of(i, minIdx),
                        List.of(i, minIdx), new ArrayList<>(sorted),
                        "Swap " + a.get(minIdx) + " and " + a.get(i)
                                + " (smallest found).",
                        LINE_SWAP));
            }
            sorted.add(i);
        }
        steps.add(new Step(new ArrayList<>(a), List.of(), List.of(),
                allIndexes(n), "Sorted!", LINE_FINAL));
        return steps;
    }

    /** Insertion sort. Emits a Step per comparison, per shift, per place. */
    public List<Step> insertionSort(List<Integer> values) {
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
        List<Integer> sorted = new ArrayList<>();
        sorted.add(0);

        for (int i = 1; i < n; i++) {
            int key = a.get(i);
            int j = i - 1;
            while (j >= 0 && a.get(j) > key) {
                steps.add(new Step(new ArrayList<>(a), List.of(j, i),
                        List.of(), new ArrayList<>(sorted),
                        "Compare key " + key + " with " + a.get(j) + ".",
                        LINE_COMPARE));
                a.set(j + 1, a.get(j));
                steps.add(new Step(new ArrayList<>(a), List.of(j, j + 1),
                        List.of(), new ArrayList<>(sorted),
                        "Shift " + a.get(j + 1) + " one place to the right.",
                        LINE_SWAP));
                j--;
            }
            a.set(j + 1, key);
            steps.add(new Step(new ArrayList<>(a), List.of(j + 1),
                    List.of(), new ArrayList<>(sorted),
                    "Place key " + key + " at index " + (j + 1) + ".",
                    LINE_SWAP));
            sorted.add(i);
        }
        steps.add(new Step(new ArrayList<>(a), List.of(), List.of(),
                allIndexes(n), "Sorted!", LINE_FINAL));
        return steps;
    }

    /** Merge sort. Emits a Step per comparison and per write-back. */
    public List<Step> mergeSort(List<Integer> values) {
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
        List<Integer> tmp = new ArrayList<>(values);
        mergeSortHelper(a, tmp, 0, a.size() - 1, steps);
        steps.add(new Step(new ArrayList<>(a), List.of(), List.of(),
                allIndexes(a.size()), "Sorted!", LINE_FINAL));
        return steps;
    }

    /** Recursive merge sort over a[lo..hi], merging into tmp. */
    private void mergeSortHelper(List<Integer> a, List<Integer> tmp,
                                 int lo, int hi, List<Step> steps) {
        if (lo >= hi) {
            return;
        }
        int mid = lo + (hi - lo) / 2;
        mergeSortHelper(a, tmp, lo, mid, steps);
        mergeSortHelper(a, tmp, mid + 1, hi, steps);
        merge(a, tmp, lo, mid, hi, steps);
    }

    /**
     * Merges the two sorted halves a[lo..mid] and a[mid+1..hi].
     * Comparisons read from a (positions are array indexes); the
     * merged result is staged in tmp, then written back into a
     * one position at a time (one Step per write-back).
     */
    private void merge(List<Integer> a, List<Integer> tmp,
                       int lo, int mid, int hi, List<Step> steps) {
        int i = lo;
        int j = mid + 1;
        int k = lo;
        while (i <= mid && j <= hi) {
            steps.add(new Step(new ArrayList<>(a), List.of(i, j),
                    List.of(), List.of(),
                    "Compare " + a.get(i) + " and " + a.get(j) + ".",
                    LINE_COMPARE));
            if (a.get(i) <= a.get(j)) {
                tmp.set(k++, a.get(i++));
            } else {
                tmp.set(k++, a.get(j++));
            }
        }
        while (i <= mid) {
            tmp.set(k++, a.get(i++));
        }
        while (j <= hi) {
            tmp.set(k++, a.get(j++));
        }
        for (int p = lo; p <= hi; p++) {
            a.set(p, tmp.get(p));
            steps.add(new Step(new ArrayList<>(a), List.of(p),
                    List.of(p), List.of(),
                    "Write " + a.get(p) + " to index " + p + ".",
                    LINE_SWAP));
        }
    }

    /**
     * Quick sort (Lomuto partition, last element as pivot).
     * Emits a Step per comparison with the pivot, per swap, and
     * when the pivot lands in its final place.
     */
    public List<Step> quickSort(List<Integer> values) {
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
        List<Integer> sorted = new ArrayList<>();
        quickSortHelper(a, 0, a.size() - 1, steps, sorted);
        steps.add(new Step(new ArrayList<>(a), List.of(), List.of(),
                allIndexes(a.size()), "Sorted!", LINE_FINAL));
        return steps;
    }

    /** Recursive quick sort over a[lo..hi]. */
    private void quickSortHelper(List<Integer> a, int lo, int hi,
                                 List<Step> steps, List<Integer> sorted) {
        if (lo > hi) {
            return;
        }
        if (lo == hi) {
            // A single element is already in its final place.
            if (!sorted.contains(lo)) {
                sorted.add(lo);
            }
            return;
        }
        int pivotIdx = hi;
        int pivot = a.get(pivotIdx);
        // i = boundary: elements < pivot live in [lo, i).
        int i = lo;
        for (int j = lo; j < hi; j++) {
            steps.add(new Step(new ArrayList<>(a), List.of(j, pivotIdx),
                    List.of(), new ArrayList<>(sorted),
                    "Compare " + a.get(j) + " with pivot " + pivot + ".",
                    LINE_COMPARE));
            if (a.get(j) < pivot) {
                if (i != j) {
                    int x = a.get(i);
                    int y = a.get(j);
                    a.set(i, y);
                    a.set(j, x);
                    steps.add(new Step(new ArrayList<>(a), List.of(i, j),
                            List.of(i, j), new ArrayList<>(sorted),
                            "Swap " + x + " and " + y + ".",
                            LINE_SWAP));
                }
                i++;
            }
        }
        // Place the pivot in its final position (index i).
        if (i != pivotIdx) {
            int x = a.get(i);
            a.set(i, pivot);
            a.set(pivotIdx, x);
            steps.add(new Step(new ArrayList<>(a), List.of(i, pivotIdx),
                    List.of(i, pivotIdx), new ArrayList<>(sorted),
                    "Swap " + x + " and " + pivot + " (pivot moves).",
                    LINE_SWAP));
        }
        sorted.add(i);
        steps.add(new Step(new ArrayList<>(a), List.of(i),
                List.of(), new ArrayList<>(sorted),
                "Pivot " + pivot + " is in its final place at index " + i
                        + ".",
                LINE_SWAP));
        quickSortHelper(a, lo, i - 1, steps, sorted);
        quickSortHelper(a, i + 1, hi, steps, sorted);
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
