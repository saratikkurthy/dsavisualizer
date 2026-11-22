package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Voronoi diagram — half-plane intersection (Phase 24,
 * computational geometry): each site's cell is the
 * bounding box clipped by the perpendicular bisector
 * half-plane against every other site (Sutherland–
 * Hodgman). The cell of a site is the set of points
 * closer to it than to any other site — the "nearest
 * shop" map.
 *
 * <p>Steps: one clip step per (site, other-site) pair
 * (line 2, sites in input order, others in input order)
 * + the final Step (line 0). Per clip step:
 * {@code highlight} = [the site index being clipped];
 * {@code data} = the CURRENT cell polygon flattened in
 * HUNDREDTHS ([x1,y1,...] &times;100, integers);
 * {@code nodes} = the sites (input order, labelled
 * A..D; {@code value} = 1 for the site being clipped
 * else 0); {@code edges} = []. The final step has
 * {@code data} = [] and its message lists all four cells.
 * Each polygon is canonicalised by rotating it to start
 * at its lexicographically smallest vertex (min x, then
 * min y) — the design's listed cell orders are rotations
 * of the Sutherland–Hodgman output, and this rotation
 * reproduces them exactly for A, B and D (C up to
 * rotation). Bounding box [0,0,10,10], fixed. Demo sites
 * A(2,2), B(8,2), C(5,8), D(5,5). Cells (areas sum to
 * 100): A [(0,0),(5,0),(5,2),(0.5,6.5),(0,6.75)],
 * B [(5,0),(10,0),(10,6.75),(9.5,6.5),(5,2)],
 * C [(10,6.75),(10,10),(0,10),(0,6.75),(0.5,6.5),
 * (9.5,6.5)], D [(0.5,6.5),(5,2),(9.5,6.5)]. 12 clip
 * steps + final = 13 steps.</p>
 *
 * <p>The controller validates the input (word missing or
 * not a "x,y;x,y;..." point list &rarr; 400, fewer than 2
 * sites &rarr; 400, a site outside the [0,0,10,10] box
 * &rarr; 400); this method guards against bad input
 * anyway with a single "empty" Step.</p>
 */
@Service
public class VoronoiService {

    private static final int LINE_CLIP = 2;
    private static final int LINE_FINAL = 0;

    private static final double EPS = 1e-9;
    private static final double BOX_MIN = 0;
    private static final double BOX_MAX = 10;

    /** One site: name, coordinates, original index (its node id). */
    private static class Point {
        final String name;
        final double x;
        final double y;
        final int index;

        Point(String name, double x, double y, int index) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.index = index;
        }
    }

    /**
     * Runs half-plane intersection Voronoi. {@code word} is
     * "x,y;x,y;...".
     */
    public List<Step> voronoi(String word) {
        List<Step> steps = new ArrayList<>();
        List<int[]> coords = parsePoints(word);
        if (coords == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Sites are needed as "
                            + "\"x,y;x,y;...\" (e.g. "
                            + "\"2,2;8,2;5,8\").", LINE_FINAL));
            return steps;
        }
        if (coords.size() < 2) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "At least 2 sites are needed.",
                    LINE_FINAL));
            return steps;
        }
        List<Point> sites = new ArrayList<>();
        for (int i = 0; i < coords.size(); i++) {
            sites.add(new Point(nameOf(i), coords.get(i)[0],
                    coords.get(i)[1], i));
        }

        List<List<double[]>> cells = new ArrayList<>();
        for (int i = 0; i < sites.size(); i++) {
            // The bounding box, counter-clockwise.
            List<double[]> cell = new ArrayList<>();
            cell.add(new double[]{BOX_MIN, BOX_MIN});
            cell.add(new double[]{BOX_MAX, BOX_MIN});
            cell.add(new double[]{BOX_MAX, BOX_MAX});
            cell.add(new double[]{BOX_MIN, BOX_MAX});
            for (int j = 0; j < sites.size(); j++) {
                if (j == i) {
                    continue;
                }
                cell = clip(cell, sites.get(i), sites.get(j));
                steps.add(new Step(flattenHundredths(cell),
                        List.of(i), List.of(), List.of(),
                        "Clip " + sites.get(i).name + " vs "
                                + sites.get(j).name + ": cell "
                                + sites.get(i).name + " = "
                                + polygonText(cell) + ".",
                        LINE_CLIP, snapshotNodes(sites, i),
                        List.of()));
            }
            cells.add(cell);
        }
        List<String> cellTexts = new ArrayList<>();
        for (int i = 0; i < sites.size(); i++) {
            cellTexts.add(sites.get(i).name + " "
                    + polygonText(cells.get(i)));
        }
        steps.add(new Step(List.of(), List.of(), List.of(),
                List.of(),
                "Cells: " + String.join("; ", cellTexts) + ".",
                LINE_FINAL, snapshotNodes(sites, -1), List.of()));
        return steps;
    }

    /**
     * Clips a polygon to the half-plane of points closer to
     * {@code keep} than to {@code other} (Sutherland–
     * Hodgman against the perpendicular bisector). Returns
     * the polygon canonicalised to start at its
     * lexicographically smallest vertex.
     */
    private List<double[]> clip(List<double[]> polygon, Point keep,
                                Point other) {
        if (polygon.isEmpty()) {
            return polygon;
        }
        double mx = (keep.x + other.x) / 2.0;
        double my = (keep.y + other.y) / 2.0;
        double nx = other.x - keep.x;
        double ny = other.y - keep.y;
        List<double[]> out = new ArrayList<>();
        int n = polygon.size();
        for (int i = 0; i < n; i++) {
            double[] cur = polygon.get(i);
            double[] prev = polygon.get((i + n - 1) % n);
            double dCur = (cur[0] - mx) * nx + (cur[1] - my) * ny;
            double dPrev = (prev[0] - mx) * nx + (prev[1] - my) * ny;
            boolean curIn = dCur <= EPS;
            boolean prevIn = dPrev <= EPS;
            if (curIn) {
                if (!prevIn) {
                    out.add(intersect(prev, cur, dPrev, dCur));
                }
                out.add(cur);
            } else if (prevIn) {
                out.add(intersect(prev, cur, dPrev, dCur));
            }
        }
        // Drop consecutive duplicates (a vertex exactly on the
        // bisector is emitted once as the crossing point).
        List<double[]> clean = new ArrayList<>();
        for (double[] p : out) {
            if (clean.isEmpty() || !samePoint(clean.get(clean.size() - 1), p)) {
                clean.add(p);
            }
        }
        if (clean.size() > 1
                && samePoint(clean.get(0), clean.get(clean.size() - 1))) {
            clean.remove(clean.size() - 1);
        }
        return canonicalise(clean);
    }

    /** The crossing point of an edge with the bisector plane. */
    private double[] intersect(double[] a, double[] b,
                               double dA, double dB) {
        double t = dA / (dA - dB);
        return new double[]{
                a[0] + t * (b[0] - a[0]),
                a[1] + t * (b[1] - a[1])};
    }

    private boolean samePoint(double[] a, double[] b) {
        return Math.abs(a[0] - b[0]) < EPS
                && Math.abs(a[1] - b[1]) < EPS;
    }

    /**
     * Rotates a polygon to start at its lexicographically
     * smallest vertex (min x, then min y).
     */
    private List<double[]> canonicalise(List<double[]> polygon) {
        if (polygon.isEmpty()) {
            return polygon;
        }
        int start = 0;
        for (int i = 1; i < polygon.size(); i++) {
            double[] p = polygon.get(i);
            double[] best = polygon.get(start);
            if (p[0] < best[0] - EPS
                    || (Math.abs(p[0] - best[0]) < EPS
                            && p[1] < best[1] - EPS)) {
                start = i;
            }
        }
        List<double[]> rotated = new ArrayList<>();
        for (int i = 0; i < polygon.size(); i++) {
            rotated.add(polygon.get((start + i) % polygon.size()));
        }
        return rotated;
    }

    /** A polygon flattened to hundredths ([x1,y1,...] x100, integers). */
    private List<Integer> flattenHundredths(List<double[]> polygon) {
        List<Integer> flat = new ArrayList<>();
        for (double[] p : polygon) {
            flat.add((int) Math.round(p[0] * 100));
            flat.add((int) Math.round(p[1] * 100));
        }
        return flat;
    }

    /** "(0,0),(5,0),(5,2)" rendering of a polygon. */
    private String polygonText(List<double[]> polygon) {
        List<String> vertices = new ArrayList<>();
        for (double[] p : polygon) {
            vertices.add("(" + fmt(p[0]) + "," + fmt(p[1]) + ")");
        }
        return String.join(",", vertices);
    }

    /** One NodeDto per site (id = index, label = name, value 1 for the clipped site). */
    private List<NodeDto> snapshotNodes(List<Point> sites, int clipped) {
        List<NodeDto> dtos = new ArrayList<>();
        for (int i = 0; i < sites.size(); i++) {
            Point p = sites.get(i);
            NodeDto dto = new NodeDto(i, i == clipped ? 1 : 0,
                    p.x, p.y);
            dto.setLabel(p.name);
            dtos.add(dto);
        }
        return dtos;
    }

    /** Site names: A..Z, then P27, P28, ... */
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

    /** A coordinate with up to 2 decimals, trailing zeros trimmed. */
    private String fmt(double value) {
        if (Math.abs(value - Math.rint(value)) < EPS) {
            return Long.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value)
                .replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
