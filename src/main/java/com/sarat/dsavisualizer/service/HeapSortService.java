package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Heap sort (max-heap) that records every meaningful moment as a Step.
 *
 * Note the heap here is a MAX-heap, not the min-heap the heap
 * visualizer uses: the largest element must bubble to the root so
 * it can be moved to the end. Line numbers: compare = 2, swap = 3,
 * final = 0.
 */
@Service
public class HeapSortService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_SWAP = 3;
    private static final int LINE_FINAL = 0;

    /**
     * Heap sort. Build phase: heapify from the last parent
     * (index n/2 - 1) down to the root. Extract phase: repeatedly
     * swap the root (max) with the last unsorted element, mark that
     * position sorted, then heapify the root down within the
     * unsorted region.
     */
    public List<Step> heapSort(List<Integer> values) {
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

        // Build phase: heapify from the last parent down to the root.
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapifyDown(a, i, n, steps, sorted);
        }
        // Extract phase: move the max to the end, heapify the root.
        for (int end = n - 1; end >= 1; end--) {
            int max = a.get(0);
            a.set(0, a.get(end));
            a.set(end, max);
            sorted.add(0, end);
            steps.add(new Step(new ArrayList<>(a), List.of(0, end),
                    List.of(0, end), new ArrayList<>(sorted),
                    "Move max " + max + " to position " + end + ".",
                    LINE_SWAP));
            heapifyDown(a, 0, end, steps, sorted);
        }
        steps.add(new Step(new ArrayList<>(a), List.of(), List.of(),
                allIndexes(n), "Sorted!", LINE_FINAL));
        return steps;
    }

    /**
     * Heapify down from index {@code i} within a[0..size): compare
     * with the larger child, swap down while the child is larger.
     * One Step per comparison and per swap.
     */
    private void heapifyDown(List<Integer> a, int i, int size,
                             List<Step> steps, List<Integer> sorted) {
        while (true) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            int largest = i;
            if (left < size && a.get(left) > a.get(largest)) {
                largest = left;
            }
            if (right < size && a.get(right) > a.get(largest)) {
                largest = right;
            }
            if (largest == i) {
                return;
            }
            int parentVal = a.get(i);
            int childVal = a.get(largest);
            steps.add(new Step(new ArrayList<>(a), List.of(i, largest),
                    List.of(), new ArrayList<>(sorted),
                    "Compare " + parentVal + " with larger child "
                            + childVal + ".",
                    LINE_COMPARE));
            a.set(i, childVal);
            a.set(largest, parentVal);
            steps.add(new Step(new ArrayList<>(a), List.of(i, largest),
                    List.of(i, largest), new ArrayList<>(sorted),
                    "Swap " + parentVal + " and " + childVal
                            + " — heapify down.",
                    LINE_SWAP));
            i = largest;
        }
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
