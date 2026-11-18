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

    /** [0, 1, ..., n-1] — used for the final "all sorted" step. */
    private List<Integer> allIndexes(int n) {
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            all.add(i);
        }
        return all;
    }
}
