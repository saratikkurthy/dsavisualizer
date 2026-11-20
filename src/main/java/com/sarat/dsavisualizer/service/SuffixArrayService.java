package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Suffix Array (Phase 18): the suffixes of a word sorted
 * lexicographically, with the LCP (longest common prefix) array
 * between consecutive sorted suffixes. A naive O(n² log n) sort
 * is fine at this size (n &lt;= 12); the trace teaches the
 * result, not the sort.
 *
 * The controller validates the input (1..12 characters,
 * lowercase a–z only); this method guards against bad input
 * anyway with a single "empty" Step.
 *
 * Trace: one rank step (line 2) per suffix placed, then the
 * final step (line 0). {@code data} is the SA indices placed so
 * far; {@code highlight} is the suffix index just placed.
 * {@code aux} is null on rank steps and carries the full LCP
 * array on the final step only.
 */
@Service
public class SuffixArrayService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_FINAL = 0;

    /**
     * Builds the suffix array of {@code word} and the LCP array
     * between consecutive sorted suffixes.
     */
    public List<Step> suffixArray(String word) {
        List<Step> steps = new ArrayList<>();
        if (word == null || word.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A word is needed.", LINE_FINAL));
            return steps;
        }
        int n = word.length();
        List<Integer> sa = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            sa.add(i);
        }
        sa.sort((a, b) -> word.substring(a).compareTo(word.substring(b)));
        List<Integer> placed = new ArrayList<>();
        for (int r = 0; r < n; r++) {
            int index = sa.get(r);
            placed.add(index);
            steps.add(new Step(new ArrayList<>(placed), List.of(index),
                    List.of(), List.of(),
                    "Rank " + r + ": suffix " + index + " '"
                            + word.substring(index) + "'.",
                    LINE_COMPARE));
        }
        List<Integer> lcp = new ArrayList<>();
        for (int i = 0; i + 1 < n; i++) {
            lcp.add(commonPrefix(word, sa.get(i), sa.get(i + 1)));
        }
        Step finalStep = new Step(new ArrayList<>(sa), List.of(),
                List.of(), List.of(),
                "Suffix array: " + sa + "; LCP: " + lcp + ".",
                LINE_FINAL);
        finalStep.setAux(new ArrayList<>(lcp));
        steps.add(finalStep);
        return steps;
    }

    /** Length of the common prefix of the suffixes at {@code a} and {@code b}. */
    private int commonPrefix(String word, int a, int b) {
        int n = 0;
        while (a + n < word.length() && b + n < word.length()
                && word.charAt(a + n) == word.charAt(b + n)) {
            n++;
        }
        return n;
    }
}
