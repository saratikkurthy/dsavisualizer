package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Singly linked list operations that record each action as a Step.
 * The list is carried as a plain Java list; each Step's
 * {@code data} is the list after that step, and {@code highlight}
 * holds the node index being visited or changed.
 * Line numbers: visit = 2, mutation = 3, final = 0 (same
 * convention as the sorting services).
 */
@Service
public class LinkedListService {

    private static final int LINE_VISIT = 2;
    private static final int LINE_WRITE = 3;
    private static final int LINE_FINAL = 0;

    /**
     * Insert a value at {@code index}. The index is clamped to
     * [0, size], so inserting past the end appends.
     */
    public List<Step> insert(List<Integer> values, int value, int index) {
        List<Integer> list = values == null
                ? new ArrayList<>() : new ArrayList<>(values);
        int at = Math.max(0, Math.min(index, list.size()));

        List<Step> steps = new ArrayList<>();
        // Walk to the insertion point, one node per step.
        for (int k = 0; k < at; k++) {
            steps.add(new Step(new ArrayList<>(list), List.of(k),
                    List.of(), List.of(),
                    "Visit node " + k + ": value is " + list.get(k) + ".",
                    LINE_VISIT));
        }
        list.add(at, value);
        steps.add(new Step(new ArrayList<>(list), List.of(),
                List.of(at), List.of(),
                "Inserted " + value + " at index " + at + ".",
                LINE_WRITE));
        steps.add(new Step(new ArrayList<>(list), List.of(), List.of(),
                List.of(), "List: " + list + ".", LINE_FINAL));
        return steps;
    }

    /** Delete the node at {@code index}. */
    public List<Step> delete(List<Integer> values, int index) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(), List.of(),
                    "List is empty.", LINE_FINAL));
            return steps;
        }
        List<Integer> list = new ArrayList<>(values);
        if (index < 0 || index >= list.size()) {
            steps.add(new Step(new ArrayList<>(list), List.of(), List.of(),
                    List.of(),
                    "Index " + index + " is out of range.", LINE_FINAL));
            return steps;
        }

        // Walk to the node, one node per step.
        for (int k = 0; k < index; k++) {
            steps.add(new Step(new ArrayList<>(list), List.of(k),
                    List.of(), List.of(),
                    "Visit node " + k + ": value is " + list.get(k) + ".",
                    LINE_VISIT));
        }
        int removed = list.remove(index);
        steps.add(new Step(new ArrayList<>(list), List.of(index),
                List.of(), List.of(),
                "Deleted " + removed + " at index " + index + ".",
                LINE_WRITE));
        steps.add(new Step(new ArrayList<>(list), List.of(), List.of(),
                List.of(), "List: " + list + ".", LINE_FINAL));
        return steps;
    }

    /** Search for a target, visiting one node per step. */
    public List<Step> search(List<Integer> values, int target) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(), List.of(),
                    "List is empty.", LINE_FINAL));
            return steps;
        }
        List<Integer> list = new ArrayList<>(values);
        for (int k = 0; k < list.size(); k++) {
            if (list.get(k) == target) {
                steps.add(new Step(new ArrayList<>(list), List.of(k),
                        List.of(), List.of(),
                        "Found target at index " + k + "!",
                        LINE_FINAL));
                return steps;
            }
            steps.add(new Step(new ArrayList<>(list), List.of(k),
                    List.of(), List.of(),
                    "Check node " + k + ": value is " + list.get(k) + ".",
                    LINE_VISIT));
        }
        steps.add(new Step(new ArrayList<>(list), List.of(), List.of(),
                List.of(), "Target " + target + " not found.",
                LINE_FINAL));
        return steps;
    }
}
