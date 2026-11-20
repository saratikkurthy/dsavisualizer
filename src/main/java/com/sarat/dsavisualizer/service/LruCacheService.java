package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LRU Cache (Phase 17): a fixed-capacity cache where a get or a
 * put moves the touched key to the front (most recently used) and
 * a put into a full cache evicts the key at the back (least
 * recently used).
 *
 * The ops are strings — "put &lt;key&gt; &lt;value&gt;" or
 * "get &lt;key&gt;" — because the frontend builds the op list
 * from its put/get builder (see the design doc). The controller
 * validates them (capacity 1..5, 1..10 ops, keys 0..99, values
 * 0..999); this method guards against bad input anyway with a
 * single "empty" Step.
 *
 * Trace: one step per op, plus a final step. {@code data} is the
 * cache keys, most-recent first; {@code highlight} is the key the
 * op touched. A get hit is line 3 (a match); every other op step
 * is line 2; the final step is line 0.
 */
@Service
public class LruCacheService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_MATCH = 3;
    private static final int LINE_FINAL = 0;

    /**
     * Runs {@code ops} against a cache of {@code capacity}.
     * Keys are most-recent first in every step's data.
     */
    public List<Step> simulate(int capacity, List<String> ops) {
        List<Step> steps = new ArrayList<>();
        if (capacity < 1 || ops == null || ops.isEmpty()) {
            steps.add(lruStep(List.of(), List.of(),
                    "A capacity of at least 1 and at least one "
                            + "operation are needed.",
                    LINE_FINAL));
            return steps;
        }
        Map<Integer, Integer> values = new HashMap<>();
        // Cache keys, most-recent first.
        List<Integer> order = new ArrayList<>();
        for (String op : ops) {
            String[] parts = op == null ? new String[0]
                    : op.trim().split("\\s+");
            if (parts.length == 3 && "put".equals(parts[0])) {
                int key;
                int value;
                try {
                    key = Integer.parseInt(parts[1]);
                    value = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    steps.add(lruStep(order, List.of(),
                            "Op must be 'put <key> <value>' or "
                                    + "'get <key>'.",
                            LINE_FINAL));
                    return steps;
                }
                if (values.containsKey(key)) {
                    values.put(key, value);
                    order.remove(Integer.valueOf(key));
                    order.add(0, key);
                    steps.add(lruStep(order, List.of(key),
                            "put(" + key + ", " + value + "): updated "
                                    + "value; moved to front; cache "
                                    + order + ".",
                            LINE_COMPARE));
                } else {
                    values.put(key, value);
                    order.add(0, key);
                    if (order.size() > capacity) {
                        int evicted = order.remove(order.size() - 1);
                        values.remove(evicted);
                        steps.add(lruStep(order, List.of(key),
                                "put(" + key + ", " + value + "): "
                                        + "inserted; evicted " + evicted
                                        + " (least recently used); "
                                        + "cache " + order + ".",
                                LINE_COMPARE));
                    } else {
                        steps.add(lruStep(order, List.of(key),
                                "put(" + key + ", " + value + "): "
                                        + "inserted; cache " + order
                                        + ".",
                                LINE_COMPARE));
                    }
                }
            } else if (parts.length == 2 && "get".equals(parts[0])) {
                int key;
                try {
                    key = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    steps.add(lruStep(order, List.of(),
                            "Op must be 'put <key> <value>' or "
                                    + "'get <key>'.",
                            LINE_FINAL));
                    return steps;
                }
                if (values.containsKey(key)) {
                    order.remove(Integer.valueOf(key));
                    order.add(0, key);
                    steps.add(lruStep(order, List.of(key),
                            "get(" + key + "): HIT → " + values.get(key)
                                    + "; moved to front; cache "
                                    + order + ".",
                            LINE_MATCH));
                } else {
                    steps.add(lruStep(order, List.of(key),
                            "get(" + key + "): MISS; cache unchanged "
                                    + order + ".",
                            LINE_COMPARE));
                }
            } else {
                steps.add(lruStep(order, List.of(),
                        "Op must be 'put <key> <value>' or "
                                + "'get <key>'.",
                        LINE_FINAL));
                return steps;
            }
        }
        steps.add(lruStep(order, List.of(),
                "Final cache (most → least recent): " + order + ".",
                LINE_FINAL));
        return steps;
    }

    /** One LRU Step: cache keys (most-recent first), key touched. */
    private Step lruStep(List<Integer> data, List<Integer> highlight,
                         String message, int line) {
        return new Step(new ArrayList<>(data), highlight, List.of(),
                List.of(), message, line);
    }
}
