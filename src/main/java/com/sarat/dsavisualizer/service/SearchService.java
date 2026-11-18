package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Search algorithms that record every probe as a Step.
 * {@code sorted} is always empty for search — nothing is "sorted"
 * during a search, only probed (highlight) or found.
 */
@Service
public class SearchService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_FINAL = 0;

    /** Linear search: probe each index in order until found or exhausted. */
    public List<Step> linearSearch(List<Integer> values, int target) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(), List.of(),
                    "No values to search.", LINE_FINAL));
            return steps;
        }
        List<Integer> a = new ArrayList<>(values);
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) == target) {
                steps.add(new Step(new ArrayList<>(a), List.of(i),
                        List.of(), List.of(),
                        "Found target " + target + " at index " + i + "!",
                        LINE_FINAL));
                return steps;
            }
            steps.add(new Step(new ArrayList<>(a), List.of(i),
                    List.of(), List.of(),
                    "Check index " + i + ": value is " + a.get(i)
                            + " — not the target.",
                    LINE_COMPARE));
        }
        steps.add(new Step(new ArrayList<>(a), List.of(), List.of(), List.of(),
                "Target " + target + " not found.", LINE_FINAL));
        return steps;
    }

    /**
     * Binary search. {@code values} must already be sorted.
     * Each step probes the middle and narrows the range.
     */
    public List<Step> binarySearch(List<Integer> values, int target) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(), List.of(),
                    "No values to search.", LINE_FINAL));
            return steps;
        }
        List<Integer> a = new ArrayList<>(values);
        int lo = 0;
        int hi = a.size() - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            int midVal = a.get(mid);
            if (midVal == target) {
                steps.add(new Step(new ArrayList<>(a), List.of(mid),
                        List.of(), List.of(),
                        "Found target " + target + " at index " + mid + "!",
                        LINE_FINAL));
                return steps;
            }
            if (midVal < target) {
                steps.add(new Step(new ArrayList<>(a), List.of(mid),
                        List.of(), List.of(),
                        "Check middle index " + mid + ": value is " + midVal
                                + " — target is larger, search the right half ("
                                + (mid + 1) + " to " + hi + ").",
                        LINE_COMPARE));
                lo = mid + 1;
            } else {
                steps.add(new Step(new ArrayList<>(a), List.of(mid),
                        List.of(), List.of(),
                        "Check middle index " + mid + ": value is " + midVal
                                + " — target is smaller, search the left half ("
                                + lo + " to " + (mid - 1) + ").",
                        LINE_COMPARE));
                hi = mid - 1;
            }
        }
        steps.add(new Step(new ArrayList<>(a), List.of(), List.of(), List.of(),
                "Target " + target + " not found.", LINE_FINAL));
        return steps;
    }
}
