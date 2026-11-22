package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fenwick tree / Binary Indexed Tree (Phase 16). The tree is
 * 1-based — say so in every label and message; the rest of the
 * app is 0-based and this is the classic Fenwick confusion.
 * BIT[0] is unused and stays 0; it is kept in {@code data} so
 * indices line up (the same convention as the segment tree's
 * tree[0] = 0).
 *
 * Build: add each element one at a time (a point update per
 * element). Prefix sum: walk down stripping the lowest set bit
 * ({@code j -= j & -j}). Update: add the delta, walking up
 * adding the lowest set bit ({@code j += j & -j}). A
 * visited/touched index is line 2, the summary line 0.
 */
@Service
public class FenwickTreeService {

    private static final int LINE_VISIT = 2;
    private static final int LINE_FINAL = 0;

    /** Build the BIT over {@code values} (1-based). */
    public List<Step> build(List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            steps.add(bitStep(List.of(), List.of(),
                    "No values to build a Fenwick tree from.",
                    LINE_FINAL));
            return steps;
        }
        int n = values.size();
        List<Integer> bit = new ArrayList<>(
                Collections.nCopies(n + 1, 0));
        for (int i = 1; i <= n; i++) {
            List<Integer> touched = new ArrayList<>();
            int j = i;
            while (j <= n) {
                bit.set(j, bit.get(j) + values.get(i - 1));
                touched.add(j);
                j += j & -j;
            }
            steps.add(bitStep(bit, touched,
                    "Added a[" + i + "] = " + values.get(i - 1)
                            + ": touched BIT[" + join(touched) + "].",
                    LINE_VISIT));
        }
        steps.add(bitStep(bit, List.of(),
                "Built BIT: [" + join(bit) + "].", LINE_FINAL));
        return steps;
    }

    /**
     * Prefix sum over a[1..index] (1-based, inclusive). One step
     * per visited index. The controller validates the index;
     * this method guards against bad input anyway with a single
     * "empty" Step.
     */
    public List<Step> prefix(List<Integer> values, int index) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()
                || index < 1 || index > values.size()) {
            steps.add(bitStep(List.of(), List.of(),
                    "A valid 1-based index inside the array is needed.",
                    LINE_FINAL));
            return steps;
        }
        List<Integer> bit = buildBit(values);
        int sum = 0;
        int j = index;
        while (j > 0) {
            sum += bit.get(j);
            steps.add(bitStep(bit, List.of(j),
                    "prefix(" + index + "): add BIT[" + j + "] = "
                            + bit.get(j) + " → sum " + sum + ".",
                    LINE_VISIT));
            j -= j & -j;
        }
        steps.add(bitStep(bit, List.of(),
                "prefix(" + index + ") = " + sum + ".", LINE_FINAL));
        return steps;
    }

    /**
     * Point update: add {@code delta} to a[index] (1-based). One
     * step per touched index. The controller validates the index
     * and the delta (a delta of 0 is a 400); this method guards
     * against bad input anyway with a single "empty" Step.
     */
    public List<Step> update(List<Integer> values, int index,
                             int delta) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()
                || index < 1 || index > values.size()) {
            steps.add(bitStep(List.of(), List.of(),
                    "A valid 1-based index inside the array is needed.",
                    LINE_FINAL));
            return steps;
        }
        if (delta == 0) {
            steps.add(bitStep(List.of(), List.of(),
                    "Delta must not be 0.", LINE_FINAL));
            return steps;
        }
        List<Integer> bit = buildBit(values);
        int n = values.size();
        int j = index;
        while (j <= n) {
            bit.set(j, bit.get(j) + delta);
            steps.add(bitStep(bit, List.of(j),
                    "Add " + delta + " to BIT[" + j + "] → "
                            + bit.get(j) + ".",
                    LINE_VISIT));
            j += j & -j;
        }
        steps.add(bitStep(bit, List.of(),
                "Updated BIT: [" + join(bit) + "].", LINE_FINAL));
        return steps;
    }

    /** The BIT for {@code values}, built without any steps. */
    private List<Integer> buildBit(List<Integer> values) {
        int n = values.size();
        List<Integer> bit = new ArrayList<>(
                Collections.nCopies(n + 1, 0));
        for (int i = 1; i <= n; i++) {
            int j = i;
            while (j <= n) {
                bit.set(j, bit.get(j) + values.get(i - 1));
                j += j & -j;
            }
        }
        return bit;
    }

    /** One Fenwick Step: a copy of the BIT array. */
    private Step bitStep(List<Integer> bit, List<Integer> highlight,
                         String message, int line) {
        return new Step(new ArrayList<>(bit), highlight, List.of(),
                List.of(), message, line);
    }

    /**
     * "0, 1, 4, 5, 16, 9, 20" — comma-joined values for
     * messages (the brackets come from the message template,
     * so the list itself carries no brackets).
     */
    private String join(List<Integer> values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(values.get(i));
        }
        return out.toString();
    }
}
