package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Rabin-Karp string matching (Phase 15): a rolling hash slides
 * one window at a time over the text, and only a window whose
 * hash equals the pattern's hash gets its characters checked.
 *
 * Base 256, mod 101; {@code h = 256^(m-1) mod 101} is the weight
 * of the outgoing character. The hash of the next window is
 * {@code (256 * (hash - text[i] * h) + text[i + m]) mod 101},
 * normalised into 0..100 (Java's % can go negative).
 *
 * Trace: {@code data} is always [patternHash, windowHash] on
 * every step, so the frontend's hash row always has both numbers.
 * The first step is the window-0 step and its message starts by
 * stating the pattern hash ("Pattern hash: 88. Window 0: ...") —
 * a separate intro step would have had a one-element data array,
 * which the hash row cannot show. A hash match emits a
 * "Checking characters…" step; a real match ends the trace with
 * "Pattern found at index 10.", while a spurious match (window 6
 * in the demo: hash 88, text "BACDABABC" ≠ "ABABCABAB") emits
 * the reject step and scanning continues. No match at all ends
 * with "Pattern not found." A hash match is line 3, a plain
 * window or a spurious reject is line 2, the summary is line 0.
 */
@Service
public class RabinKarpService {

    private static final int BASE = 256;
    private static final int MOD = 101;
    private static final int LINE_COMPARE = 2;
    private static final int LINE_MATCH = 3;
    private static final int LINE_FINAL = 0;

    /**
     * Rabin-Karp search of {@code words} = [text, pattern]. The
     * controller validates the input (exactly 2 non-empty strings,
     * text at most 40 characters, pattern at most 12 and no longer
     * than the text — the same bounds as KMP); this method guards
     * against bad input anyway with a single "empty" Step.
     */
    public List<Step> rabinKarp(List<String> words) {
        List<Step> steps = new ArrayList<>();
        if (words == null || words.size() != 2
                || words.get(0) == null || words.get(1) == null
                || words.get(0).isEmpty() || words.get(1).isEmpty()) {
            steps.add(rkStep(List.of(), List.of(),
                    "A text and a pattern are needed.", LINE_FINAL));
            return steps;
        }
        String text = words.get(0);
        String pattern = words.get(1);
        int n = text.length();
        int m = pattern.length();
        // h = BASE^(m-1) mod MOD: the weight of the character
        // leaving the window.
        int h = 1;
        for (int i = 0; i < m - 1; i++) {
            h = (h * BASE) % MOD;
        }
        int patternHash = 0;
        int windowHash = 0;
        for (int i = 0; i < m; i++) {
            patternHash = (patternHash * BASE + pattern.charAt(i)) % MOD;
            windowHash = (windowHash * BASE + text.charAt(i)) % MOD;
        }
        for (int i = 0; i <= n - m; i++) {
            if (i > 0) {
                // Roll the hash: drop text[i-1], add text[i+m-1].
                windowHash = (BASE * (windowHash - text.charAt(i - 1) * h)
                        + text.charAt(i + m - 1)) % MOD;
                // Java's % can go negative; normalise to 0..100.
                windowHash = ((windowHash % MOD) + MOD) % MOD;
            }
            // The first step also states the pattern hash (see the
            // class javadoc for why it is folded into window 0).
            String prefix = i == 0
                    ? "Pattern hash: " + patternHash + ". "
                    : "";
            List<Integer> data = List.of(patternHash, windowHash);
            if (windowHash == patternHash) {
                steps.add(rkStep(data, List.of(i),
                        prefix + "Window " + i + ": hash " + windowHash
                                + " = pattern hash " + patternHash
                                + ". Checking characters…",
                        LINE_MATCH));
                if (text.substring(i, i + m).equals(pattern)) {
                    steps.add(rkStep(data, List.of(i),
                            "Pattern found at index " + i + ".",
                            LINE_FINAL));
                    return steps;
                }
                // Spurious hit: the hashes match but the text
                // differs — keep scanning.
                steps.add(rkStep(data, List.of(i),
                        "Hash matches at window " + i
                                + ", but the text differs — spurious "
                                + "hit, keep scanning.",
                        LINE_COMPARE));
            } else {
                steps.add(rkStep(data, List.of(i),
                        prefix + "Window " + i + ": hash " + windowHash
                                + " != pattern hash " + patternHash
                                + ". No match.",
                        LINE_COMPARE));
            }
        }
        steps.add(rkStep(List.of(patternHash, windowHash), List.of(),
                "Pattern not found.", LINE_FINAL));
        return steps;
    }

    /** One Rabin-Karp Step: [patternHash, windowHash]. */
    private Step rkStep(List<Integer> data, List<Integer> highlight,
                        String message, int line) {
        return new Step(new ArrayList<>(data), highlight, List.of(),
                List.of(), message, line);
    }
}
