package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LFU Cache (Phase 20): a fixed-capacity cache where every
 * access bumps a key's frequency, and a put into a full cache
 * evicts the lowest-frequency key — ties broken by least
 * recently used within that frequency (the standard LFU rule,
 * LeetCode 460).
 *
 * <p>The ops are strings — "put &lt;key&gt; &lt;value&gt;" or
 * "get &lt;key&gt;" — exactly the LRU wire format. The
 * controller validates them (capacity 1..5, 1..10 ops, keys
 * 0..99, values 0..999); this method guards against bad input
 * anyway with a single "empty" Step.</p>
 *
 * <p>Trace: one step per op, plus a final step — mirroring
 * {@code LruCacheService.lruStep} exactly. {@code data} is the
 * cache keys in eviction order (frequency ascending, then
 * recency ascending within a frequency); {@code aux} is the
 * frequencies, aligned with {@code data}; {@code highlight}
 * is the key the op touched. A get hit is line 3, a get miss
 * is line 2, a put with eviction is line 4, a put without
 * eviction is line 5, a put on an existing key is line 3, and
 * the final step is line 0.</p>
 */
@Service
public class LfuCacheService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_MATCH = 3;
    private static final int LINE_EVICT = 4;
    private static final int LINE_INSERT = 5;
    private static final int LINE_FINAL = 0;

    /**
     * Runs {@code ops} against a cache of {@code capacity}.
     * Keys are in eviction order in every step's data.
     */
    public List<Step> simulate(int capacity, List<String> ops) {
        List<Step> steps = new ArrayList<>();
        if (capacity < 1 || ops == null || ops.isEmpty()) {
            steps.add(lfuStep(List.of(), List.of(), List.of(),
                    "A capacity of at least 1 and at least one "
                            + "operation are needed.",
                    LINE_FINAL));
            return steps;
        }
        Map<Integer, Integer> values = new HashMap<>();
        Map<Integer, Integer> freq = new HashMap<>();
        // Recency stamp within a frequency: bumped on every
        // touch, so the lowest stamp in a frequency is the
        // least recently used key of that frequency.
        Map<Integer, Integer> stamp = new HashMap<>();
        int tick = 0;
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
                    steps.add(lfuStep(evictionOrder(values, freq, stamp),
                            frequencies(values, freq, stamp), List.of(),
                            "Op must be 'put <key> <value>' or "
                                    + "'get <key>'.",
                            LINE_FINAL));
                    return steps;
                }
                if (values.containsKey(key)) {
                    values.put(key, value);
                    freq.put(key, freq.get(key) + 1);
                    tick++;
                    stamp.put(key, tick);
                    steps.add(lfuStep(
                            evictionOrder(values, freq, stamp),
                            frequencies(values, freq, stamp),
                            List.of(key),
                            "Put " + key + "=" + value + ": update "
                                    + "(freq " + freq.get(key) + ").",
                            LINE_MATCH));
                } else {
                    Integer evicted = null;
                    int evictedFreq = 0;
                    if (values.size() >= capacity) {
                        evicted = evictionOrder(values, freq, stamp)
                                .get(0);
                        evictedFreq = freq.get(evicted);
                        values.remove(evicted);
                        freq.remove(evicted);
                        stamp.remove(evicted);
                    }
                    values.put(key, value);
                    freq.put(key, 1);
                    tick++;
                    stamp.put(key, tick);
                    if (evicted == null) {
                        steps.add(lfuStep(
                                evictionOrder(values, freq, stamp),
                                frequencies(values, freq, stamp),
                                List.of(key),
                                "Put " + key + "=" + value
                                        + " (freq 1).",
                                LINE_INSERT));
                    } else {
                        steps.add(lfuStep(
                                evictionOrder(values, freq, stamp),
                                frequencies(values, freq, stamp),
                                List.of(key),
                                "Put " + key + "=" + value + ": evict "
                                        + evicted + " (freq "
                                        + evictedFreq
                                        + ", least recently used).",
                                LINE_EVICT));
                    }
                }
            } else if (parts.length == 2 && "get".equals(parts[0])) {
                int key;
                try {
                    key = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    steps.add(lfuStep(evictionOrder(values, freq, stamp),
                            frequencies(values, freq, stamp), List.of(),
                            "Op must be 'put <key> <value>' or "
                                    + "'get <key>'.",
                            LINE_FINAL));
                    return steps;
                }
                if (values.containsKey(key)) {
                    freq.put(key, freq.get(key) + 1);
                    tick++;
                    stamp.put(key, tick);
                    steps.add(lfuStep(
                            evictionOrder(values, freq, stamp),
                            frequencies(values, freq, stamp),
                            List.of(key),
                            "Get " + key + " \u2192 " + values.get(key)
                                    + " (freq " + freq.get(key) + ").",
                            LINE_MATCH));
                } else {
                    steps.add(lfuStep(
                            evictionOrder(values, freq, stamp),
                            frequencies(values, freq, stamp),
                            List.of(key),
                            "Get " + key + " \u2192 -1 (not in cache).",
                            LINE_COMPARE));
                }
            } else {
                steps.add(lfuStep(evictionOrder(values, freq, stamp),
                        frequencies(values, freq, stamp), List.of(),
                        "Op must be 'put <key> <value>' or "
                                + "'get <key>'.",
                        LINE_FINAL));
                return steps;
            }
        }
        List<Integer> order = evictionOrder(values, freq, stamp);
        StringBuilder cache = new StringBuilder("Cache: ");
        for (int i = 0; i < order.size(); i++) {
            int key = order.get(i);
            if (i > 0) {
                cache.append(", ");
            }
            cache.append(key).append("=").append(values.get(key))
                    .append(" (freq ").append(freq.get(key)).append(")");
        }
        cache.append(".");
        steps.add(lfuStep(order, frequencies(values, freq, stamp),
                List.of(), cache.toString(), LINE_FINAL));
        return steps;
    }

    /** Cache keys in eviction order: frequency, then recency. */
    private List<Integer> evictionOrder(Map<Integer, Integer> values,
                                        Map<Integer, Integer> freq,
                                        Map<Integer, Integer> stamp) {
        List<Integer> order = new ArrayList<>(values.keySet());
        order.sort(Comparator
                .comparingInt((Integer k) -> freq.get(k))
                .thenComparingInt(stamp::get));
        return order;
    }

    /** Frequencies aligned with {@link #evictionOrder}. */
    private List<Integer> frequencies(Map<Integer, Integer> values,
                                      Map<Integer, Integer> freq,
                                      Map<Integer, Integer> stamp) {
        List<Integer> out = new ArrayList<>();
        for (int key : evictionOrder(values, freq, stamp)) {
            out.add(freq.get(key));
        }
        return out;
    }

    /** One LFU Step: cache keys in eviction order, frequencies in aux. */
    private Step lfuStep(List<Integer> data, List<Integer> aux,
                         List<Integer> highlight, String message,
                         int line) {
        Step step = new Step(new ArrayList<>(data), highlight,
                List.of(), List.of(), message, line);
        step.setAux(new ArrayList<>(aux));
        return step;
    }
}
