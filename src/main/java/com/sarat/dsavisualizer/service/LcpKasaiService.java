package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Suffix array + LCP (Kasai) (Phase 26, string algorithms
 * v2): the suffixes of a word in lexicographic order, with
 * the LCP array — the longest common prefix between each
 * suffix and its predecessor in that order. Kasai's trick:
 * the LCP of suffix i+1 is at least the LCP of suffix i
 * minus 1, so one linear scan computes the whole array.
 * The idea behind "the longest repeated substring" (the
 * maximum LCP) and the suffix-array / LCP pair behind
 * suffix-array search.
 *
 * <p>Demo string "banana" (the classic). Suffix array:
 * [5, 3, 1, 0, 4, 2] (suffixes a, ana, anana, banana, na,
 * nana). LCP array (Kasai): [0, 1, 3, 0, 0, 2]. Longest
 * repeated substring "ana" (len 3, from LCP 3 at rank 2).
 * Kasai h values, in i order 0..5: i=0 rank 3 lcp 0 (h
 * 0&rarr;0); i=1 rank 2 lcp 3 (h 0&rarr;3); i=2 rank 5
 * lcp 2 (h 2&rarr;2 — the decrement after i=1); i=3 rank
 * 1 lcp 1 (h 1&rarr;1); i=4 rank 4 lcp 0 (h 0&rarr;0);
 * i=5 rank 0 lcp 0 (h 0&rarr;0). Trace: 1 SA step (line
 * 2) + 6 Kasai steps (line 3, one per i in 0..5) + final
 * (line 0) = 8 steps. SA step: {@code data} = the SA
 * [5, 3, 1, 0, 4, 2], {@code sorted} = the SA. Kasai
 * step: {@code highlight} = [i, sa[rank[i]&minus;1]] (the
 * two suffixes being compared; [] when rank[i] is 0),
 * {@code data} = [lcp[rank[i]], h before this step],
 * {@code sorted} = the LCP values computed so far in rank
 * order (lcp[0] = 0 counts as computed on the i=5 step).
 * Final step {@code data} = [3], {@code sorted} = the
 * full LCP [0, 1, 3, 0, 0, 2]. {@code nodes} = one NodeDto
 * per suffix, in suffix-start order: {@code id} = the
 * suffix start index, {@code value} = its rank in the
 * suffix array, {@code label} = the suffix string. No
 * {@code edges} (null).</p>
 *
 * <p>Bad input (a missing/blank word) yields a single
 * "empty" Step; the controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class LcpKasaiService {

    private static final int LINE_SA = 2;
    private static final int LINE_KASAI = 3;
    private static final int LINE_FINAL = 0;

    /** Builds the suffix array and LCP array of {@code word} with Kasai's algorithm. */
    public List<Step> lcpKasai(String word) {
        List<Step> steps = new ArrayList<>();
        if (word == null || word.isBlank()) {
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
        List<Integer> rank = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            rank.add(0);
        }
        for (int r = 0; r < n; r++) {
            rank.set(sa.get(r), r);
        }
        List<NodeDto> nodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            NodeDto dto = new NodeDto(i, rank.get(i), 0, 0);
            dto.setLabel(word.substring(i));
            nodes.add(dto);
        }
        steps.add(new Step(new ArrayList<>(sa), List.of(),
                List.of(), new ArrayList<>(sa),
                "Suffix array: " + sa + ".", LINE_SA,
                new ArrayList<>(nodes), null));

        List<Integer> lcp = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            lcp.add(0);
        }
        List<Integer> computed = new ArrayList<>();
        int h = 0;
        for (int i = 0; i < n; i++) {
            int r = rank.get(i);
            int hBefore = h;
            if (r == 0) {
                computed.add(0);
                computed.sort(Integer::compareTo);
                List<Integer> soFar = new ArrayList<>();
                for (int rr : computed) {
                    soFar.add(lcp.get(rr));
                }
                steps.add(new Step(List.of(0, hBefore), List.of(),
                        List.of(), soFar,
                        "i=" + i + ": rank 0 has no predecessor — "
                                + "lcp[0] = 0.",
                        LINE_KASAI, new ArrayList<>(nodes), null));
                continue;
            }
            int j = sa.get(r - 1);
            while (i + h < n && j + h < n
                    && word.charAt(i + h) == word.charAt(j + h)) {
                h++;
            }
            lcp.set(r, h);
            computed.add(r);
            computed.sort(Integer::compareTo);
            List<Integer> soFar = new ArrayList<>();
            for (int rr : computed) {
                soFar.add(lcp.get(rr));
            }
            steps.add(new Step(List.of(h, hBefore), List.of(i, j),
                    List.of(), soFar,
                    "i=" + i + ": suffix " + i + " vs suffix " + j
                            + " — lcp[" + r + "] = " + h + ".",
                    LINE_KASAI, new ArrayList<>(nodes), null));
            if (h > 0) {
                h--;
            }
        }
        int best = 0;
        int bestRank = 0;
        for (int r = 1; r < n; r++) {
            if (lcp.get(r) > best) {
                best = lcp.get(r);
                bestRank = r;
            }
        }
        String longest = best == 0 ? ""
                : word.substring(sa.get(bestRank),
                        sa.get(bestRank) + best);
        steps.add(new Step(List.of(best), List.of(), List.of(),
                new ArrayList<>(lcp),
                "Suffix array + LCP done: longest repeated "
                        + "substring \"" + longest + "\" (len "
                        + best + ").",
                LINE_FINAL, new ArrayList<>(nodes), null));
        return steps;
    }
}
