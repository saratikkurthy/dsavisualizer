package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * KMP string matching (Phase 13): two phases in one trace.
 *
 * Phase 1 builds the LPS (prefix-function) array of the pattern:
 * {@code data} is the LPS built so far, {@code highlight} is
 * [i, len]. Phase 2 searches the text: {@code data} is the final
 * LPS array, {@code highlight} is [i (text index), j (pattern
 * index)], and the message describes each match, mismatch and
 * shift ("Mismatch at i=4, j=2. Fall back to lps[1] = 0.").
 * The trace stops at the first match. A match is line 3, a
 * mismatch / fallback line 2, the summary line 0.
 */
@Service
public class KmpService {

    private static final int LINE_VISIT = 2;
    private static final int LINE_MATCH = 3;
    private static final int LINE_FINAL = 0;

    /**
     * KMP search of {@code words} = [text, pattern]. The
     * controller validates the input (exactly 2 non-empty strings,
     * text at most 40 characters, pattern at most 12 and no longer
     * than the text); this method guards against bad input anyway
     * with a single "empty" Step.
     */
    public List<Step> kmp(List<String> words) {
        List<Step> steps = new ArrayList<>();
        if (words == null || words.size() != 2
                || words.get(0) == null || words.get(1) == null
                || words.get(0).isEmpty() || words.get(1).isEmpty()) {
            steps.add(kmpStep(List.of(), List.of(),
                    "A text and a pattern are needed.", LINE_FINAL));
            return steps;
        }
        String text = words.get(0);
        String pattern = words.get(1);
        int n = text.length();
        int m = pattern.length();
        List<Integer> lps = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            lps.add(0);
        }
        // Phase 1: build the LPS array.
        int len = 0;
        int i = 1;
        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps.set(i, len);
                steps.add(kmpStep(lps, List.of(i, len),
                        "lps[" + i + "] = " + len + " ('"
                                + pattern.charAt(i)
                                + "' extends the prefix of length "
                                + (len - 1) + ").",
                        LINE_MATCH));
                i++;
            } else if (len > 0) {
                int oldLen = len;
                len = lps.get(len - 1);
                steps.add(kmpStep(lps, List.of(i, len),
                        "Mismatch at i=" + i + ", len=" + oldLen
                                + ". Fall back to lps[" + (oldLen - 1)
                                + "] = " + len + ".",
                        LINE_VISIT));
            } else {
                lps.set(i, 0);
                steps.add(kmpStep(lps, List.of(i, 0),
                        "lps[" + i + "] = 0 (no proper prefix "
                                + "matches).",
                        LINE_VISIT));
                i++;
            }
        }
        // Phase 2: search the text.
        int ti = 0;
        int j = 0;
        int foundAt = -1;
        while (ti < n) {
            if (text.charAt(ti) == pattern.charAt(j)) {
                steps.add(kmpStep(lps, List.of(ti, j),
                        "Match: text[" + ti + "] == pattern[" + j
                                + "] ('" + text.charAt(ti) + "').",
                        LINE_MATCH));
                ti++;
                j++;
                if (j == m) {
                    foundAt = ti - j;
                    break;
                }
            } else if (j > 0) {
                int oldJ = j;
                j = lps.get(j - 1);
                steps.add(kmpStep(lps, List.of(ti, j),
                        "Mismatch at i=" + ti + ", j=" + oldJ
                                + ". Fall back to lps[" + (oldJ - 1)
                                + "] = " + j + ".",
                        LINE_VISIT));
            } else {
                steps.add(kmpStep(lps, List.of(ti, j),
                        "Mismatch at i=" + ti + ", j=0. Move to text["
                                + (ti + 1) + "].",
                        LINE_VISIT));
                ti++;
            }
        }
        if (foundAt >= 0) {
            steps.add(kmpStep(lps,
                    List.of(foundAt + m - 1, m - 1),
                    "Pattern found at index " + foundAt + ".",
                    LINE_FINAL));
        } else {
            steps.add(kmpStep(lps, List.of(),
                    "Pattern not found.", LINE_FINAL));
        }
        return steps;
    }

    /** One KMP Step: a copy of the LPS array so far. */
    private Step kmpStep(List<Integer> lps, List<Integer> highlight,
                         String message, int line) {
        return new Step(new ArrayList<>(lps), highlight, List.of(),
                List.of(), message, line);
    }
}
