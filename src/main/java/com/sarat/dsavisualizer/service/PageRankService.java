package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PageRank (Phase 24, search &amp; ranking): the link
 * analysis behind early Google — a page is important when
 * important pages link to it. Power iteration from a
 * uniform start, damping d = 0.85, with the teleport fix
 * for dangling pages (a page with no out-links spreads its
 * rank evenly over all pages instead of leaking it).
 *
 * <p>Steps: one iteration step per iteration (line 2) +
 * the final Step (line 0). Per iteration step:
 * {@code data} = the ranks in MILLIS for all pages in
 * page order (rounded); {@code highlight} = [the page
 * with the largest change this iteration] (ties: the
 * lowest page index); {@code nodes} = the pages (input
 * order, labelled A..F, x/y = 0 — the frontend draws its
 * circle layout; {@code value} = the rank position,
 * 1-based); {@code edges} = the links (page indices,
 * weight null); {@code sorted} = the ranking so far (page
 * indices, best first, ties by page id). The final step's
 * {@code highlight} is the top-ranked page. Demo graph
 * (pages A–F): A&rarr;B,C; B&rarr;C; C&rarr;A; D&rarr;C;
 * E&rarr;D,C; F&rarr;[] (dangling). Damping 0.85, uniform
 * start 1/6, 10 iterations. Iteration 1: C 0.4736, A
 * 0.1903, B 0.1194, D 0.1194, E 0.0486, F 0.0486. After
 * 10: C 0.3745, A 0.3496, B 0.1761, D 0.0415, E 0.0291,
 * F 0.0291 (sum 1.0). Ranking: C &gt; A &gt; B &gt; D &gt;
 * E = F (tie by page id). 10 iteration steps + final =
 * 11 steps; final {@code data} = [350, 176, 375, 42, 29,
 * 29], {@code sorted} = [2, 0, 1, 3, 4, 5].</p>
 *
 * <p>Page count: a dangling page has no links at all, so
 * the page count cannot be inferred from the links alone
 * (page F is invisible in the edge list). The controller
 * therefore passes the page count in via {@code pageCount}
 * (TraceRequest {@code value} — the frontend sends the
 * number of pages); when it is absent the count defaults
 * to max-node-id + 1. The controller validates the input
 * (graphEdges missing/empty &rarr; 400, an edge that is
 * not a [u, v] pair &rarr; 400, a node id outside the page
 * range &rarr; 400, iterations (index) outside 1..20
 * &rarr; 400, a page count (value) outside 1..20 or below
 * max-node-id + 1 &rarr; 400); this method guards against
 * bad input anyway with a single "empty" Step.</p>
 */
@Service
public class PageRankService {

    private static final int LINE_ITERATION = 2;
    private static final int LINE_FINAL = 0;

    private static final double DAMPING = 0.85;
    private static final int DEFAULT_ITERATIONS = 10;

    /**
     * Runs power iteration PageRank. {@code graphEdges} are
     * links as [u, v] pairs; {@code iterations} is the
     * iteration count (null &rarr; 10); {@code pageCount} is
     * the number of pages (null &rarr; max node id + 1).
     */
    public List<Step> pagerank(List<List<Integer>> graphEdges,
                               Integer iterations, Integer pageCount) {
        List<Step> steps = new ArrayList<>();
        if (graphEdges == null || graphEdges.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Links are needed as [u, v] pairs.",
                    LINE_FINAL));
            return steps;
        }
        // Dedupe links (a link counted twice would inflate the
        // source page's out-degree).
        Set<String> seen = new HashSet<>();
        List<int[]> links = new ArrayList<>();
        int maxNode = -1;
        for (List<Integer> edge : graphEdges) {
            if (edge == null || edge.size() < 2
                    || edge.get(0) == null || edge.get(1) == null) {
                continue;
            }
            int from = edge.get(0);
            int to = edge.get(1);
            if (from < 0 || to < 0) {
                continue;
            }
            String key = from + "->" + to;
            if (seen.add(key)) {
                links.add(new int[]{from, to});
                maxNode = Math.max(maxNode, Math.max(from, to));
            }
        }
        if (links.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Links are needed as [u, v] pairs.",
                    LINE_FINAL));
            return steps;
        }
        int n = pageCount != null ? pageCount : maxNode + 1;
        if (n < 1) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "At least 1 page is needed.",
                    LINE_FINAL));
            return steps;
        }
        int iters = iterations != null ? iterations : DEFAULT_ITERATIONS;

        // Out-degree and in-neighbours per page.
        int[] outDegree = new int[n];
        List<List<Integer>> inNeighbours = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            inNeighbours.add(new ArrayList<>());
        }
        List<EdgeDto> edges = new ArrayList<>();
        for (int[] link : links) {
            if (link[0] >= n || link[1] >= n) {
                continue;
            }
            outDegree[link[0]]++;
            inNeighbours.get(link[1]).add(link[0]);
            edges.add(new EdgeDto(link[0], link[1], null));
        }

        double[] ranks = new double[n];
        for (int i = 0; i < n; i++) {
            ranks[i] = 1.0 / n;
        }
        for (int iter = 1; iter <= iters; iter++) {
            double danglingMass = 0;
            for (int i = 0; i < n; i++) {
                if (outDegree[i] == 0) {
                    danglingMass += ranks[i];
                }
            }
            double[] next = new double[n];
            for (int p = 0; p < n; p++) {
                double sum = danglingMass / n;
                for (int q : inNeighbours.get(p)) {
                    sum += ranks[q] / outDegree[q];
                }
                next[p] = (1 - DAMPING) / n + DAMPING * sum;
            }
            int largestChange = 0;
            double largestDelta = -1;
            for (int p = 0; p < n; p++) {
                double delta = Math.abs(next[p] - ranks[p]);
                if (delta > largestDelta) {
                    largestDelta = delta;
                    largestChange = p;
                }
            }
            ranks = next;
            List<Integer> millis = millis(ranks);
            steps.add(new Step(millis, List.of(largestChange),
                    List.of(), ranking(ranks),
                    "Iteration " + iter + ": " + ranksText(ranks)
                            + ". Largest change: "
                            + nameOf(largestChange) + ".",
                    LINE_ITERATION,
                    snapshotNodes(n, ranks), edges));
        }
        List<Integer> finalRanking = ranking(ranks);
        steps.add(new Step(millis(ranks), List.of(finalRanking.get(0)),
                List.of(), finalRanking,
                "After " + iters + " iteration"
                        + (iters == 1 ? "" : "s") + ": "
                        + ranksText(ranks) + ". Ranking: "
                        + rankingText(ranks) + ".",
                LINE_FINAL, snapshotNodes(n, ranks), edges));
        return steps;
    }

    /** The ranks in MILLIS (rounded), in page order. */
    private List<Integer> millis(double[] ranks) {
        List<Integer> millis = new ArrayList<>();
        for (double rank : ranks) {
            millis.add((int) Math.round(rank * 1000));
        }
        return millis;
    }

    /** Page indices best first (exact ranks, ties by page id). */
    private List<Integer> ranking(double[] ranks) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < ranks.length; i++) {
            order.add(i);
        }
        order.sort((a, b) -> {
            int byRank = Double.compare(ranks[b], ranks[a]);
            return byRank != 0 ? byRank : Integer.compare(a, b);
        });
        return order;
    }

    /** Rank positions (1-based, ties by page id) per page index. */
    private int[] positions(double[] ranks) {
        int[] positions = new int[ranks.length];
        List<Integer> order = ranking(ranks);
        for (int pos = 0; pos < order.size(); pos++) {
            positions[order.get(pos)] = pos + 1;
        }
        return positions;
    }

    /** "C 0.474, A 0.190, B 0.119, ..." in page order. */
    private String ranksText(double[] ranks) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < ranks.length; i++) {
            parts.add(nameOf(i) + " " + String.format(
                    java.util.Locale.ROOT, "%.3f", ranks[i]));
        }
        return String.join(", ", parts);
    }

    /** "C > A > B > D > E = F" for the ranking. */
    private String rankingText(double[] ranks) {
        List<Integer> order = ranking(ranks);
        List<String> groups = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        double prev = Double.NaN;
        for (int p : order) {
            if (!Double.isNaN(prev)
                    && Math.abs(ranks[p] - prev) < 1e-12) {
                current.append(" = ").append(nameOf(p));
            } else {
                if (current.length() > 0) {
                    groups.add(current.toString());
                }
                current = new StringBuilder(nameOf(p));
            }
            prev = ranks[p];
        }
        if (current.length() > 0) {
            groups.add(current.toString());
        }
        return String.join(" > ", groups);
    }

    /** One NodeDto per page (id = page index, label = A.., value = rank position). */
    private List<NodeDto> snapshotNodes(int pageCount, double[] ranks) {
        int[] positions = positions(ranks);
        List<NodeDto> dtos = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            NodeDto dto = new NodeDto(i, positions[i], 0, 0);
            dto.setLabel(nameOf(i));
            dtos.add(dto);
        }
        return dtos;
    }

    /** Page names: A..Z, then P27, P28, ... */
    private String nameOf(int i) {
        if (i < 26) {
            return String.valueOf((char) ('A' + i));
        }
        return "P" + (i + 1);
    }
}
