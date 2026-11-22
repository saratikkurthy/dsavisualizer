package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Closest pair of points — divide &amp; conquer (Phase 24,
 * computational geometry): sort by x, split in half, solve
 * each half (brute force for &le; 3 points), then check the
 * strip around the split for a pair closer than the best
 * so far. The strip holds the points within the best
 * distance of the split line, in y order; every pair in
 * it is checked (the strip is tiny, and the trace shows
 * each check).
 *
 * <p>Steps: the base-case comparisons (line 2, left half
 * then right half, pairs in x-sorted order), the strip
 * comparison(s) (line 3, pairs in y-sorted order), one
 * choose step per split (line 4), plus the final Step
 * (line 0). {@code step.nodes} are the points (input order,
 * labelled A..F); a node's {@code value} is 0 left / 1
 * right of the top-level split (all 0 when there is no
 * split, i.e. &le; 3 points). {@code step.edges} are the
 * current best pair as one edge (weight null).
 * {@code data} is [bestDistMillis so far] (millis, rounded);
 * {@code highlight} is the two point indices compared, in
 * comparison order (x-sorted for base cases, y-sorted for
 * the strip); {@code sorted} is the best pair so far, listed
 * lower-first (empty before the first comparison). Demo
 * (the K-d demo set: (2,3), (5,4), (9,6), (4,7), (8,1),
 * (7,2)): sorted by x = (2,3), (4,7), (5,4), (7,2), (8,1),
 * (9,6); split at x=6. Left best 3.162 ((2,3)-(5,4), tied
 * with (4,7)-(5,4) — first wins); right best 1.414
 * ((7,2)-(8,1)); strip |x-6| &lt; 1.414 contains (5,4),
 * (7,2) — 1 pair checked, 2.828, rejected. Answer 1.414,
 * pair (7,2)-(8,1). 3 left + 3 right + 1 strip + 1 choose
 * + final = 9 steps; final {@code data} = [1414],
 * {@code sorted} = [4, 5].</p>
 *
 * <p>The controller validates the input (word missing or
 * not a "x,y;x,y;..." point list &rarr; 400, fewer than 2
 * points &rarr; 400); this method guards against bad input
 * anyway with a single "empty" Step.</p>
 */
@Service
public class ClosestPairService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_STRIP = 3;
    private static final int LINE_CHOOSE = 4;
    private static final int LINE_FINAL = 0;

    private static final double EPS = 1e-9;

    /** One point: name, coordinates, original index (its node id). */
    private static class Point {
        final String name;
        final int x;
        final int y;
        final int index;

        Point(String name, int x, int y, int index) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.index = index;
        }
    }

    /** The best pair found so far (shared across the recursion). */
    private static class Best {
        double dist = Double.POSITIVE_INFINITY;
        int first = -1;
        int second = -1;
    }

    private List<Point> points;
    private List<Step> steps;
    private Best best;
    private List<Boolean> rightOfSplit;

    /**
     * Runs divide &amp; conquer closest pair. {@code word}
     * is "x,y;x,y;...".
     */
    public List<Step> closestPair(String word) {
        List<Step> result = new ArrayList<>();
        List<int[]> coords = parsePoints(word);
        if (coords == null) {
            result.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Points are needed as "
                            + "\"x,y;x,y;...\" (e.g. "
                            + "\"2,3;5,4;9,6\").", LINE_FINAL));
            return result;
        }
        if (coords.size() < 2) {
            result.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "At least 2 points are needed.",
                    LINE_FINAL));
            return result;
        }
        points = new ArrayList<>();
        for (int i = 0; i < coords.size(); i++) {
            points.add(new Point(nameOf(i), coords.get(i)[0],
                    coords.get(i)[1], i));
        }
        steps = result;
        best = new Best();

        // Points sorted by x (ties by y, then input index).
        List<Point> byX = new ArrayList<>(points);
        byX.sort(Comparator.comparingInt((Point p) -> p.x)
                .thenComparingInt(p -> p.y)
                .thenComparingInt(p -> p.index));

        // Top-level split (for the node values): left half 0,
        // right half 1. No split for <= 3 points (all 0).
        rightOfSplit = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            rightOfSplit.add(false);
        }
        if (byX.size() > 3) {
            int mid = byX.size() / 2;
            for (int i = mid; i < byX.size(); i++) {
                rightOfSplit.set(byX.get(i).index, true);
            }
        }

        solve(byX);
        steps.add(new Step(
                List.of((int) Math.round(best.dist * 1000)),
                List.of(), List.of(), bestPairSorted(),
                "Closest pair: " + pairText(best.first, best.second)
                        + ", distance " + fmt(best.dist) + ".",
                LINE_FINAL, snapshotNodes(), bestEdge()));
        return steps;
    }

    /**
     * Solves the closest pair for the points in {@code px}
     * (x-sorted). Returns the best distance in this subproblem.
     * Emits the comparison steps (line 2), the strip steps
     * (line 3) and one choose step (line 4) for a split.
     */
    private double solve(List<Point> px) {
        if (px.size() <= 3) {
            double localBest = Double.POSITIVE_INFINITY;
            for (int i = 0; i < px.size(); i++) {
                for (int j = i + 1; j < px.size(); j++) {
                    double d = dist(px.get(i), px.get(j));
                    compare(px.get(i), px.get(j), d, LINE_COMPARE);
                    localBest = Math.min(localBest, d);
                }
            }
            return localBest;
        }
        int mid = px.size() / 2;
        double splitX = (px.get(mid - 1).x + px.get(mid).x) / 2.0;
        double leftBest = solve(new ArrayList<>(px.subList(0, mid)));
        double rightBest = solve(new ArrayList<>(px.subList(mid, px.size())));

        // Strip: points within the best distance of the split,
        // in y order; only neighbours within the best distance
        // in y can beat it.
        List<Point> strip = new ArrayList<>();
        for (Point p : px) {
            if (Math.abs(p.x - splitX) < best.dist) {
                strip.add(p);
            }
        }
        strip.sort(Comparator.comparingInt((Point p) -> p.y)
                .thenComparingInt(p -> p.x)
                .thenComparingInt(p -> p.index));
        // Every pair in the strip is checked (the design pins
        // "1 pair checked" for the demo — the y-difference
        // early exit would skip it, since the pair differs by
        // 2 in y but only 2.828 in distance).
        double stripBest = Double.POSITIVE_INFINITY;
        for (int i = 0; i < strip.size(); i++) {
            for (int j = i + 1; j < strip.size(); j++) {
                double d = dist(strip.get(i), strip.get(j));
                compare(strip.get(i), strip.get(j), d, LINE_STRIP);
                stripBest = Math.min(stripBest, d);
            }
        }
        steps.add(new Step(
                List.of((int) Math.round(best.dist * 1000)),
                bestPairSorted(), List.of(), bestPairSorted(),
                "Choose: left best " + fmt(leftBest) + ", right best "
                        + fmt(rightBest) + ", strip best "
                        + (stripBest == Double.POSITIVE_INFINITY
                                ? "none" : fmt(stripBest))
                        + " — closest pair so far "
                        + pairText(best.first, best.second) + ", "
                        + fmt(best.dist) + ".",
                LINE_CHOOSE, snapshotNodes(), bestEdge()));
        // The best within THIS subproblem (the global best may
        // already hold a better pair from a sibling half).
        return Math.min(Math.min(leftBest, rightBest), stripBest);
    }

    /** One comparison step (line 2 base case / line 3 strip). */
    private void compare(Point a, Point b, double d, int line) {
        boolean improved = d < best.dist - EPS;
        if (improved) {
            best.dist = d;
            best.first = a.index;
            best.second = b.index;
        }
        steps.add(new Step(
                List.of((int) Math.round(best.dist * 1000)),
                List.of(a.index, b.index), List.of(),
                bestPairSorted(),
                "Compare " + a.name + "(" + a.x + "," + a.y + ")-"
                        + b.name + "(" + b.x + "," + b.y + "): "
                        + fmt(d) + "; best so far "
                        + pairText(best.first, best.second) + ", "
                        + fmt(best.dist) + ".",
                line, snapshotNodes(), bestEdge()));
    }

    /** The current best pair as [lower, higher], or [] if none yet. */
    private List<Integer> bestPairSorted() {
        if (best.first < 0) {
            return List.of();
        }
        return List.of(Math.min(best.first, best.second),
                Math.max(best.first, best.second));
    }

    /** The current best pair as one edge, or [] if none yet. */
    private List<EdgeDto> bestEdge() {
        if (best.first < 0) {
            return List.of();
        }
        return List.of(new EdgeDto(
                Math.min(best.first, best.second),
                Math.max(best.first, best.second), null));
    }

    /** "(7,2)-(8,1)" rendering of a pair of point indices. */
    private String pairText(int first, int second) {
        if (first < 0) {
            return "none";
        }
        Point a = points.get(first);
        Point b = points.get(second);
        return a.name + "(" + a.x + "," + a.y + ")-"
                + b.name + "(" + b.x + "," + b.y + ")";
    }

    /** One NodeDto per point (id = index, label = name, value 0 left / 1 right). */
    private List<NodeDto> snapshotNodes() {
        List<NodeDto> dtos = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            NodeDto dto = new NodeDto(i,
                    rightOfSplit.get(i) ? 1 : 0, p.x, p.y);
            dto.setLabel(p.name);
            dtos.add(dto);
        }
        return dtos;
    }

    /** Euclidean distance between two points. */
    private double dist(Point a, Point b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }

    /** Point names: A..Z, then P27, P28, ... */
    private String nameOf(int i) {
        if (i < 26) {
            return String.valueOf((char) ('A' + i));
        }
        return "P" + (i + 1);
    }

    /**
     * Parses "x,y;x,y;..." into integer point pairs, or null
     * when the word is missing or unparseable.
     */
    private List<int[]> parsePoints(String word) {
        if (word == null || word.isBlank()) {
            return null;
        }
        List<int[]> points = new ArrayList<>();
        for (String part : word.split(";", -1)) {
            String[] xy = part.trim().split(",", -1);
            if (xy.length != 2) {
                return null;
            }
            try {
                int x = Integer.parseInt(xy[0].trim());
                int y = Integer.parseInt(xy[1].trim());
                points.add(new int[]{x, y});
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return points.isEmpty() ? null : points;
    }

    /** A distance with 3 decimals (the design's display precision). */
    private String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
