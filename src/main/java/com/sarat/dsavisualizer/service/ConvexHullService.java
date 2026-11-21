package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Convex hull — Graham scan (Phase 24, computational
 * geometry): the lowest point anchors the hull, the rest
 * sort by polar angle around it, then a scan keeps only
 * left turns — a right turn pops the middle point off the
 * hull. The classic "wrap the points in a rubber band".
 *
 * <p>Steps: one sort step (line 1) + one scan step per
 * point in sorted order (line 2) + the final Step (line 0).
 * {@code step.nodes} are the points (input order, labelled
 * A..H); a node's {@code value} is 1 while it is on the
 * hull so far, else 0. {@code step.edges} are the current
 * hull, closed back to the first point (TSP precedent:
 * edge count = hull size — a 1-point hull is a single
 * self-edge). {@code data} is [hull size so far];
 * {@code highlight} is [the index of the point being
 * processed]; {@code swapped} holds the popped point index
 * on the pop steps and is [] elsewhere (the design's only
 * use of {@code swapped}). The sort step carries
 * {@code data} = [] (the design pins nothing for it; the
 * frontend reads the sort order from the input). Demo
 * (A(1,1) B(2,4) C(4,6) D(6,5) E(7,2) F(5,1) G(3,3)
 * H(4,2)): sorted by polar angle around A(1,1) =
 * A, F, E, H, D, G, C, B. Hull (6): A, F, E, D, C, B.
 * Pops: H(4,2) when D(6,5) arrives (right turn), G(3,3)
 * when C(4,6) arrives (right turn). 1 sort + 8 scan +
 * final = 10 steps; final {@code data} = [6].</p>
 *
 * <p>The controller validates the input (word missing or
 * not a "x,y;x,y;..." point list &rarr; 400, fewer than 3
 * points &rarr; 400); this method guards against bad input
 * anyway with a single "empty" Step.</p>
 */
@Service
public class ConvexHullService {

    private static final int LINE_SORT = 1;
    private static final int LINE_SCAN = 2;
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

    /**
     * Runs Graham scan. {@code word} is "x,y;x,y;...".
     */
    public List<Step> convexHull(String word) {
        List<Step> steps = new ArrayList<>();
        List<int[]> coords = parsePoints(word);
        if (coords == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Points are needed as "
                            + "\"x,y;x,y;...\" (e.g. "
                            + "\"1,1;5,1;7,2\").", LINE_FINAL));
            return steps;
        }
        if (coords.size() < 3) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "At least 3 points are needed.",
                    LINE_FINAL));
            return steps;
        }
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < coords.size(); i++) {
            points.add(new Point(nameOf(i), coords.get(i)[0],
                    coords.get(i)[1], i));
        }

        // Lowest point (min y, then min x) anchors the hull.
        Point anchor = points.get(0);
        for (Point p : points) {
            if (p.y < anchor.y || (p.y == anchor.y && p.x < anchor.x)) {
                anchor = p;
            }
        }
        final Point base = anchor;
        List<Point> sorted = new ArrayList<>(points);
        sorted.sort(Comparator
                .comparingInt((Point p) -> p == base ? -1 : 0)
                .thenComparingDouble(p -> Math.atan2(
                        p.y - base.y, p.x - base.x))
                .thenComparingDouble(p -> dist(base, p)));

        steps.add(new Step(List.of(), List.of(), List.of(),
                List.of(),
                "Sort by polar angle around " + base.name + "("
                        + base.x + "," + base.y + "): "
                        + names(sorted) + ".",
                LINE_SORT, snapshotNodes(points, List.of()),
                List.of()));

        // Scan: keep only left turns (cross <= 0 pops — the
        // <= also drops collinear points nearer than the
        // farthest, which the angle+distance sort put first).
        List<Point> hull = new ArrayList<>();
        for (Point p : sorted) {
            List<Integer> popped = new ArrayList<>();
            while (hull.size() >= 2
                    && cross(hull.get(hull.size() - 2),
                            hull.get(hull.size() - 1), p) <= EPS) {
                popped.add(hull.remove(hull.size() - 1).index);
            }
            hull.add(p);
            String message;
            if (popped.isEmpty()) {
                message = p.name + "(" + p.x + "," + p.y + ") added; "
                        + "hull " + names(hull) + " (" + hull.size()
                        + " points).";
            } else {
                List<String> poppedNames = new ArrayList<>();
                for (int idx : popped) {
                    poppedNames.add(points.get(idx).name + "("
                            + points.get(idx).x + ","
                            + points.get(idx).y + ")");
                }
                message = p.name + "(" + p.x + "," + p.y + "): "
                        + String.join(", ", poppedNames)
                        + " make" + (popped.size() == 1 ? "s" : "")
                        + " a right turn — popped; hull "
                        + names(hull) + " (" + hull.size()
                        + " points).";
            }
            steps.add(new Step(List.of(hull.size()),
                    List.of(p.index), popped, List.of(), message,
                    LINE_SCAN, snapshotNodes(points, hull),
                    hullEdges(hull)));
        }
        steps.add(new Step(List.of(hull.size()), List.of(),
                List.of(), List.of(),
                "Convex hull: " + names(hull) + " (" + hull.size()
                        + " points).",
                LINE_FINAL, snapshotNodes(points, hull),
                hullEdges(hull)));
        return steps;
    }

    /** Cross product (b - a) x (c - a): > 0 is a left turn. */
    private double cross(Point a, Point b, Point c) {
        return (double) (b.x - a.x) * (c.y - a.y)
                - (double) (b.y - a.y) * (c.x - a.x);
    }

    /** Euclidean distance between two points. */
    private double dist(Point a, Point b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }

    /** The hull as closed edges (count = hull size). */
    private List<EdgeDto> hullEdges(List<Point> hull) {
        List<EdgeDto> edges = new ArrayList<>();
        for (int i = 0; i < hull.size(); i++) {
            Point from = hull.get(i);
            Point to = hull.get((i + 1) % hull.size());
            edges.add(new EdgeDto(from.index, to.index, null));
        }
        return edges;
    }

    /** One NodeDto per point (id = index, label = name, value 1 on the hull). */
    private List<NodeDto> snapshotNodes(List<Point> points,
                                        List<Point> hull) {
        List<NodeDto> dtos = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            NodeDto dto = new NodeDto(i, hull.contains(p) ? 1 : 0,
                    p.x, p.y);
            dto.setLabel(p.name);
            dtos.add(dto);
        }
        return dtos;
    }

    /** "A, F, E" rendering of a point list. */
    private String names(List<Point> points) {
        List<String> names = new ArrayList<>();
        for (Point p : points) {
            names.add(p.name);
        }
        return String.join(", ", names);
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
}
