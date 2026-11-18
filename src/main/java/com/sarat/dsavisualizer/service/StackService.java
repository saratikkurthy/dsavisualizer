package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Stack operations that record each action as a Step.
 * {@code values} is the current stack, bottom to top.
 * {@code sorted} is always empty — a stack has no "sorted" state.
 * Line numbers: action = 2, final = 0 (same convention as the
 * sorting services).
 */
@Service
public class StackService {

    private static final int LINE_ACTION = 2;
    private static final int LINE_FINAL = 0;

    /** Push a value onto the top of the stack. */
    public List<Step> push(List<Integer> values, int value) {
        List<Integer> stack = values == null
                ? new ArrayList<>() : new ArrayList<>(values);
        stack.add(value);
        int top = stack.size() - 1;

        List<Step> steps = new ArrayList<>();
        steps.add(new Step(new ArrayList<>(stack), List.of(top),
                List.of(), List.of(),
                "Push " + value + " onto the stack.",
                LINE_ACTION));
        steps.add(new Step(new ArrayList<>(stack), List.of(), List.of(),
                List.of(), "Stack: " + stack + ".", LINE_FINAL));
        return steps;
    }

    /** Pop the top value off the stack. */
    public List<Step> pop(List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(), List.of(),
                    "Stack is empty — nothing to pop.", LINE_FINAL));
            return steps;
        }
        List<Integer> stack = new ArrayList<>(values);
        int top = stack.size() - 1;
        int popped = stack.get(top);

        steps.add(new Step(new ArrayList<>(stack), List.of(top),
                List.of(), List.of(),
                "Pop " + popped + " from the top.", LINE_ACTION));
        stack.remove(top);
        steps.add(new Step(new ArrayList<>(stack), List.of(), List.of(),
                List.of(), "Popped " + popped + ". Stack: " + stack + ".",
                LINE_FINAL));
        return steps;
    }
}
