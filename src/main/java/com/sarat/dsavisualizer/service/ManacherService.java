package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Manacher's longest palindromic substring (Phase 21), the real
 * O(n) algorithm: d1[i] is the radius of the longest odd
 * palindrome centred at i, d2[i] the radius of the longest even
 * palindrome centred between i-1 and i. Both arrays use the
 * (l, r) mirror trick — when the centre i lies inside the
 * rightmost palindrome found so far, the starting radius comes
 * from the mirror centre instead of 1 (d1) or 0 (d2), then the
 * radius expands while the characters match.
 *
 * <p>Steps: one per d1 centre (line 3), one per d2 centre
 * (line 5), plus the final Step (line 0). A d1 step carries
 * {@code data} = d1 so far (0 = not yet computed) and
 * {@code aux} = d2 so far (all 0 during the d1 phase); a d2 step
 * carries {@code data} = final d1 and {@code aux} = d2 so far.
 * Every step carries {@code highlight} = [centre i] and
 * {@code sorted} = [l, r] of the best (longest) palindrome found
 * so far, inclusive. The message states the real values and
 * notes when the mirror shortcut supplied the starting radius,
 * and when the step sets a new longest. The final Step carries
 * {@code data} = d1, {@code aux} = d2, {@code sorted} = [l, r]
 * of the longest palindrome. Demo "forgeeksskeegfor": d1 all 1,
 * d2 = [0, 0, 0, 0, 0, 1, 0, 0, 5, 0, 0, 1, 0, 0, 0, 0],
 * longest "geeksskeeg" (3–12), length 10; 16 + 16 + 1 = 33 steps.</p>
 *
 * <p>The controller validates the input (1..20 chars, lowercase
 * a–z); this method guards against bad input anyway with a
 * single "empty" Step.</p>
 */
@Service
public class ManacherService {

    private static final int LINE_D1 = 3;
    private static final int LINE_D2 = 5;
    private static final int LINE_FINAL = 0;

    /**
     * Runs Manacher on {@code word}.
     */
    public List<Step> manacher(String word) {
        List<Step> steps = new ArrayList<>();
        String s = word == null ? "" : word.toLowerCase();
        if (s.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(), List.of(),
                    "A string is needed.", LINE_FINAL));
            return steps;
        }
        int n = s.length();
        int[] d1 = new int[n];
        int[] d2 = new int[n];
        // Best palindrome so far: [l, r] inclusive, and its length.
        int[] best = null;
        int bestLen = 0;

        // Odd palindromes (d1), the sim's mirror formula exactly.
        int l = 0;
        int r = -1;
        for (int i = 0; i < n; i++) {
            boolean mirror = i <= r;
            int k0 = mirror ? Math.min(d1[l + r - i], r - i + 1) : 1;
            int k = k0;
            while (i - k >= 0 && i + k < n && s.charAt(i - k) == s.charAt(i + k)) {
                k++;
            }
            d1[i] = k;
            if (i + k - 1 > r) {
                l = i - k + 1;
                r = i + k - 1;
            }
            int palL = i - k + 1;
            int palR = i + k - 1;
            int len = 2 * k - 1;
            boolean newLongest = false;
            if (len > bestLen) {
                newLongest = best != null;
                best = new int[]{palL, palR};
                bestLen = len;
            }
            StringBuilder message = new StringBuilder();
            message.append("Centre ").append(i).append(" ('")
                    .append(s.charAt(i)).append("'): d1[").append(i)
                    .append("]=").append(k)
                    .append(", palindrome '")
                    .append(s, palL, palR + 1)
                    .append("' (").append(palL).append("\u2013")
                    .append(palR).append(")");
            if (mirror) {
                message.append(" (mirror start ").append(k0).append(")");
            }
            if (newLongest) {
                message.append(" \u2014 new longest");
            }
            message.append(".");
            steps.add(manacherStep(d1List(d1), d2List(d2), List.of(i),
                    bestList(best), message.toString(), LINE_D1));
        }

        // Even palindromes (d2), the sim's mirror formula exactly.
        l = 0;
        r = -1;
        for (int i = 0; i < n; i++) {
            boolean mirror = i <= r;
            int k0 = mirror ? Math.min(d2[l + r - i + 1], r - i + 1) : 0;
            int k = k0;
            while (i - k - 1 >= 0 && i + k < n
                    && s.charAt(i - k - 1) == s.charAt(i + k)) {
                k++;
            }
            d2[i] = k;
            if (i + k - 1 > r) {
                l = i - k;
                r = i + k - 1;
            }
            StringBuilder message = new StringBuilder();
            if (i == 0) {
                message.append("Centre 0 (before '").append(s.charAt(0))
                        .append("'): d2[0]=0, no even palindrome.");
            } else {
                message.append("Centre ").append(i).append(" (between '")
                        .append(s.charAt(i - 1)).append("' and '")
                        .append(s.charAt(i)).append("'): d2[").append(i)
                        .append("]=").append(k);
                if (k == 0) {
                    message.append(", no even palindrome.");
                } else {
                    int palL = i - k;
                    int palR = i + k - 1;
                    int len = 2 * k;
                    boolean newLongest = false;
                    if (len > bestLen) {
                        newLongest = best != null;
                        best = new int[]{palL, palR};
                        bestLen = len;
                    }
                    message.append(", palindrome '")
                            .append(s, palL, palR + 1)
                            .append("' (").append(palL).append("\u2013")
                            .append(palR).append(")");
                    if (mirror) {
                        message.append(" (mirror start ").append(k0)
                                .append(")");
                    }
                    if (newLongest) {
                        message.append(" \u2014 new longest");
                    }
                    message.append(".");
                }
                if (k == 0 && mirror) {
                    // The mirror note belongs on the message even
                    // when there is no palindrome to report.
                    int insertAt = message.length() - 1;
                    message.insert(insertAt, " (mirror start " + k0 + ")");
                }
            }
            steps.add(manacherStep(d1List(d1), d2List(d2), List.of(i),
                    bestList(best), message.toString(), LINE_D2));
        }

        StringBuilder finalMessage = new StringBuilder();
        finalMessage.append("Longest palindromic substring: '")
                .append(s, best[0], best[1] + 1)
                .append("' (").append(best[0]).append("\u2013")
                .append(best[1]).append("), length ")
                .append(bestLen).append(".");
        steps.add(manacherStep(d1List(d1), d2List(d2), List.of(),
                bestList(best), finalMessage.toString(), LINE_FINAL));
        return steps;
    }

    /** d1 as a list (0 = not yet computed). */
    private List<Integer> d1List(int[] d1) {
        List<Integer> out = new ArrayList<>();
        for (int v : d1) {
            out.add(v);
        }
        return out;
    }

    /** d2 as a list (0 = not yet computed). */
    private List<Integer> d2List(int[] d2) {
        List<Integer> out = new ArrayList<>();
        for (int v : d2) {
            out.add(v);
        }
        return out;
    }

    /** Best palindrome [l, r], or empty when none yet. */
    private List<Integer> bestList(int[] best) {
        if (best == null) {
            return List.of();
        }
        return List.of(best[0], best[1]);
    }

    /** One Manacher Step: d1 in data, d2 in aux, best in sorted. */
    private Step manacherStep(List<Integer> d1, List<Integer> d2,
                              List<Integer> highlight,
                              List<Integer> best, String message,
                              int line) {
        Step step = new Step(new ArrayList<>(d1), highlight,
                List.of(), best, message, line);
        step.setAux(new ArrayList<>(d2));
        return step;
    }
}
