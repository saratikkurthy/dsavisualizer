package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Queue operations that record each action as a Step.
 * {@code values} is the current queue, front to rear.
 * {@code sorted} is always empty — a queue has no "sorted" state.
 * Line numbers: action = 2, final = 0 (same convention as the
 * sorting services).
 */
@Service
public class QueueService {

    private static final int LINE_ACTION = 2;
    private static final int LINE_FINAL = 0;

    /** Enqueue a value at the rear of the queue. */
    public List<Step> enqueue(List<Integer> values, int value) {
        List<Integer> queue = values == null
                ? new ArrayList<>() : new ArrayList<>(values);
        queue.add(value);
        int rear = queue.size() - 1;

        List<Step> steps = new ArrayList<>();
        steps.add(new Step(new ArrayList<>(queue), List.of(rear),
                List.of(), List.of(),
                "Enqueue " + value + " at the rear.",
                LINE_ACTION));
        steps.add(new Step(new ArrayList<>(queue), List.of(), List.of(),
                List.of(), "Queue: " + queue + ".", LINE_FINAL));
        return steps;
    }

    /** Dequeue the value at the front of the queue. */
    public List<Step> dequeue(List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(), List.of(),
                    "Queue is empty — nothing to dequeue.", LINE_FINAL));
            return steps;
        }
        List<Integer> queue = new ArrayList<>(values);
        int removed = queue.get(0);

        steps.add(new Step(new ArrayList<>(queue), List.of(0),
                List.of(), List.of(),
                "Dequeue " + removed + " from the front.", LINE_ACTION));
        queue.remove(0);
        steps.add(new Step(new ArrayList<>(queue), List.of(), List.of(),
                List.of(),
                "Dequeued " + removed + ". Queue: " + queue + ".",
                LINE_FINAL));
        return steps;
    }
}
