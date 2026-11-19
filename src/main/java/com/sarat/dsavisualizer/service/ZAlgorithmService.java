package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Z-algorithm string matching (Phase 16): builds the Z-array on
 * S = pattern + '#' + text in one linear pass (the [l, r] window
 * is the rightmost Z-box), then reads the matches off it — a
 * match is a position with Z = pattern length. Finds ALL
 * occurrences.
 *
 * Phase 1: one step per i = 1..len(S)-1; {@code data} is the Z
 * array built so far (uncomputed entries 0, Z[0] = len(S)),
 * {@code highlight} is [i, z[i]]. Phase 2: one step per match;
 * {@code data} is the full Z array, {@code highlight} is the
 * text index. A Z computation is line 2, a match line 3, the
 * summary line 0.
 */
@Service
public class ZAlgorithmService {

    private static final int LINE_VISIT = 2;
    private static final int LINE_MATCH = 3;
    private static final int LINE_FINAL = 0;

    /**
     * Z-algorithm search of {@code words} = [text, pattern]. The
     * controller validates the input (exactly 2 non-empty strings,
     * text at most 40 characters, pattern at most 12 and no longer
     * than the text — the same bounds as KMP); this method guards
     * against bad input anyway with a single "empty" Step.
     */
    public List<Step> zAlgorithm(List<String> words) {
        List<Step> steps = new ArrayList<>();
        if (words == null || words.size() != 2
                || words.get(0) == null || words.get(1) == null
                || words.get(0).isEmpty() || words.get(1).isEmpty()) {
            steps.add(zStep(List.of(), List.of(),
                    "A text and a pattern are needed.", LINE_FINAL));
            return steps;
        }
        String text = words.get(0);
        String pattern = words.get(1);
        String s = pattern + "#" + text;
        int n = s.length();
        int m = pattern.length();
        List<Integer> z = new ArrayList<>(Collections.nCopies(n, 0));
        z.set(0, n);
        // Phase 1: build the Z array, one step per i.
        int l = 0;
        int r = 0;
        for (int i = 1; i < n; i++) {
            boolean wasInside = i <= r;
            int oldL = l;
            int oldR = r;
            if (wasInside) {
                z.set(i, Math.min(r - i + 1, z.get(i - l)));
            }
            while (i + z.get(i) < n
                    && s.charAt(z.get(i)) == s.charAt(i + z.get(i))) {
                z.set(i, z.get(i) + 1);
            }
            if (i + z.get(i) - 1 > r) {
                l = i;
                r = i + z.get(i) - 1;
            }
            String message = "Z[" + i + "] = " + z.get(i) + ".";
            if (wasInside) {
                message += " (inside the [" + oldL + ", " + oldR
                        + "] window, started from Z[" + (i - oldL)
                        + "]).";
            }
            steps.add(zStep(z, List.of(i, z.get(i)), message,
                    LINE_VISIT));
        }
        // Phase 2: matches are the positions with Z = pattern length.
        List<Integer> matches = new ArrayList<>();
        for (int i = m + 1; i < n; i++) {
            if (z.get(i) == m) {
                int t = i - m - 1;
                matches.add(t);
                steps.add(zStep(z, List.of(t),
                        "Match at text index " + t + ".", LINE_MATCH));
            }
        }
        if (matches.isEmpty()) {
            steps.add(zStep(z, List.of(), "Pattern not found.",
                    LINE_FINAL));
        } else {
            steps.add(zStep(z, List.of(),
                    "Found " + matches.size() + " match(es): ["
                            + join(matches) + "].", LINE_FINAL));
        }
        return steps;
    }

    /** One Z-algorithm Step: a copy of the Z array so far. */
    private Step zStep(List<Integer> z, List<Integer> highlight,
                       String message, int line) {
        return new Step(new ArrayList<>(z), highlight, List.of(),
                List.of(), message, line);
    }

    /** "0, 4" — comma-joined values for messages. */
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
