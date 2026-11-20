package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Bloom Filter (Phase 17): a 16-bit filter with 2 hash
 * functions. Inserting a word sets the two bits its hashes point
 * at; checking a word reads those two bits — both set means
 * "possibly present" (the word may never have been inserted: a
 * false positive), either clear means "definitely not present"
 * (a Bloom filter never gives a false negative).
 *
 * <ul>
 *   <li>{@code h1(w) = (sum of char codes) % 16}</li>
 *   <li>{@code h2(w) = (sum of char code × (position + 1)) % 16}
 *       — position is 0-based, so the first character counts
 *       once, the second twice, …</li>
 * </ul>
 *
 * The controller validates the input (1..6 words to insert, each
 * at most 12 characters; the word to check is required and at
 * most 12 characters); this method guards against bad input
 * anyway with a single "empty" Step.
 *
 * Trace: one step per inserted word, then the check step, then a
 * final step. {@code data} is the 16-bit array (0/1);
 * {@code highlight} is the bit positions touched (insert) or
 * checked. An insert step is line 2; a check that finds the word
 * possibly present is line 3 (a match); a definitely-absent
 * check is line 2; the final step is line 0.
 */
@Service
public class BloomFilterService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_MATCH = 3;
    private static final int LINE_FINAL = 0;

    /** Number of bits in the filter. */
    private static final int BITS = 16;

    /**
     * Inserts {@code words}, then checks {@code word}.
     */
    public List<Step> bloom(List<String> words, String word) {
        List<Step> steps = new ArrayList<>();
        if (words == null || words.isEmpty()
                || word == null || word.isEmpty()) {
            steps.add(bloomStep(bits(), List.of(),
                    "Words to insert and a word to check are needed.",
                    LINE_FINAL));
            return steps;
        }
        List<Integer> bits = bits();
        for (String w : words) {
            int h1 = hash1(w);
            int h2 = hash2(w);
            bits.set(h1, 1);
            bits.set(h2, 1);
            steps.add(bloomStep(bits, List.of(h1, h2),
                    "Insert '" + w + "': h1 = " + h1 + ", h2 = " + h2
                            + "; bits set.",
                    LINE_COMPARE));
        }
        int h1 = hash1(word);
        int h2 = hash2(word);
        int bit1 = bits.get(h1);
        int bit2 = bits.get(h2);
        if (bit1 == 1 && bit2 == 1) {
            String message = "Check '" + word + "': h1 = " + h1
                    + " (" + bit1 + "), h2 = " + h2 + " (" + bit2
                    + ") → possibly present.";
            if (!words.contains(word)) {
                message += " '" + word + "' was never inserted, so "
                        + "this is a false positive.";
            }
            steps.add(bloomStep(bits, List.of(h1, h2), message,
                    LINE_MATCH));
        } else {
            steps.add(bloomStep(bits, List.of(h1, h2),
                    "Check '" + word + "': h1 = " + h1 + " (" + bit1
                            + "), h2 = " + h2 + " (" + bit2 + ") → "
                            + "definitely not present.",
                    LINE_COMPARE));
        }
        List<Integer> setBits = new ArrayList<>();
        for (int i = 0; i < BITS; i++) {
            if (bits.get(i) == 1) {
                setBits.add(i);
            }
        }
        steps.add(bloomStep(bits, List.of(),
                "Bloom filter: bits set " + setBits + ".",
                LINE_FINAL));
        return steps;
    }

    /** h1(w) = (sum of char codes) % 16. */
    private int hash1(String word) {
        int sum = 0;
        for (int i = 0; i < word.length(); i++) {
            sum += word.charAt(i);
        }
        return sum % BITS;
    }

    /** h2(w) = (sum of char code × (position + 1)) % 16. */
    private int hash2(String word) {
        int sum = 0;
        for (int i = 0; i < word.length(); i++) {
            sum += word.charAt(i) * (i + 1);
        }
        return sum % BITS;
    }

    /** A fresh 16-bit array, all clear. */
    private List<Integer> bits() {
        List<Integer> bits = new ArrayList<>();
        for (int i = 0; i < BITS; i++) {
            bits.add(0);
        }
        return bits;
    }

    /** One Bloom Step: bit array snapshot, positions touched. */
    private Step bloomStep(List<Integer> data, List<Integer> highlight,
                           String message, int line) {
        return new Step(new ArrayList<>(data), highlight, List.of(),
                List.of(), message, line);
    }
}
