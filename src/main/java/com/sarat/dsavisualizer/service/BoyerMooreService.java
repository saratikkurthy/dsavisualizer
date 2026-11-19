package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Boyer-Moore string matching (Phase 16): the full algorithm —
 * bad-character rule (last-occurrence table) plus the good-suffix
 * rule (the Wikipedia {@code bpos}/{@code shift} construction).
 * Comparisons run right to left; the shift taken is
 * max(bad-char, good-suffix, 1) — the bad-character shift can be
 * negative (the mismatching text character's last occurrence in
 * the pattern sits right of the mismatch), which is why the
 * good-suffix rule and the floor of 1 matter. Finds ALL
 * occurrences, unlike KMP (which stops at the first).
 *
 * Trace: {@code data} is [s] (the window start) on every step,
 * {@code highlight} is [s+j, j] (text index, pattern index being
 * compared). A mismatch is line 2, a found pattern line 3, the
 * summary line 0.
 */
@Service
public class BoyerMooreService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_MATCH = 3;
    private static final int LINE_FINAL = 0;

    /**
     * Boyer-Moore search of {@code words} = [text, pattern]. The
     * controller validates the input (exactly 2 non-empty strings,
     * text at most 40 characters, pattern at most 12 and no longer
     * than the text — the same bounds as KMP); this method guards
     * against bad input anyway with a single "empty" Step.
     */
    public List<Step> boyerMoore(List<String> words) {
        List<Step> steps = new ArrayList<>();
        if (words == null || words.size() != 2
                || words.get(0) == null || words.get(1) == null
                || words.get(0).isEmpty() || words.get(1).isEmpty()) {
            steps.add(bmStep(List.of(), List.of(),
                    "A text and a pattern are needed.", LINE_FINAL));
            return steps;
        }
        String text = words.get(0);
        String pattern = words.get(1);
        int n = text.length();
        int m = pattern.length();
        int[] shift = buildShift(pattern);
        Map<Character, Integer> last = new HashMap<>();
        for (int i = 0; i < m; i++) {
            last.put(pattern.charAt(i), i);
        }
        List<Integer> matches = new ArrayList<>();
        int s = 0;
        int lastWindow = 0;
        while (s <= n - m) {
            lastWindow = s;
            int j = m - 1;
            while (j >= 0 && pattern.charAt(j) == text.charAt(s + j)) {
                j--;
            }
            if (j < 0) {
                matches.add(s);
                steps.add(bmStep(List.of(s), List.of(s + m - 1, m - 1),
                        "Pattern found at index " + s + ".", LINE_MATCH));
                s += shift[0];
            } else {
                char tc = text.charAt(s + j);
                char pc = pattern.charAt(j);
                int bc = j - last.getOrDefault(tc, -1);
                int gs = shift[j + 1];
                int use = Math.max(bc, Math.max(gs, 1));
                steps.add(bmStep(List.of(s), List.of(s + j, j),
                        "Window " + s + ": mismatch at j=" + j
                                + " (text '" + tc + "' vs pattern '" + pc
                                + "'). Bad-char shift " + bc
                                + ", good-suffix shift " + gs + " → shift "
                                + use + ".",
                        LINE_COMPARE));
                s += use;
            }
        }
        if (matches.isEmpty()) {
            steps.add(bmStep(List.of(lastWindow), List.of(),
                    "Pattern not found.", LINE_FINAL));
        } else {
            steps.add(bmStep(List.of(lastWindow), List.of(),
                    "Found " + matches.size() + " match(es): ["
                            + join(matches) + "].", LINE_FINAL));
        }
        return steps;
    }

    /**
     * The Wikipedia good-suffix construction: {@code bpos} is the
     * border array; {@code shift[j]} is the shift to apply when a
     * mismatch happens at pattern index j-1, so the main loop
     * reads {@code shift[j + 1]} at a mismatch at j. (The inner
     * {@code while} short-circuits on {@code j <= m} before
     * reading {@code pattern.charAt(j - 1)}, so j = m + 1 — the
     * initial value — never indexes out of bounds.)
     */
    private int[] buildShift(String pattern) {
        int m = pattern.length();
        int[] shift = new int[m + 1];
        int[] bpos = new int[m + 1];
        int i = m;
        int j = m + 1;
        bpos[m] = j;
        while (i > 0) {
            while (j <= m
                    && pattern.charAt(i - 1) != pattern.charAt(j - 1)) {
                if (shift[j] == 0) {
                    shift[j] = j - i;
                }
                j = bpos[j];
            }
            i--;
            j--;
            bpos[i] = j;
        }
        j = bpos[0];
        for (i = 0; i <= m; i++) {
            if (shift[i] == 0) {
                shift[i] = j;
            }
            if (i == j) {
                j = bpos[j];
            }
        }
        return shift;
    }

    /** One Boyer-Moore Step: data is [window start]. */
    private Step bmStep(List<Integer> data, List<Integer> highlight,
                        String message, int line) {
        return new Step(new ArrayList<>(data), highlight, List.of(),
                List.of(), message, line);
    }

    /** "4, 10" — comma-joined values for messages. */
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
