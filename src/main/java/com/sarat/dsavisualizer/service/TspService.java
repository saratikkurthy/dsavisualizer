package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * TSP — nearest neighbour + 2-opt (Phase 23, multi-stop
 * delivery route planning): the greedy nearest-neighbour
 * tour jumps across the map, then 2-opt untangles it by
 * reversing segments until a full pass finds no improvement.
 * Distances are Euclidean, shown to 1 decimal (full precision
 * internally). Nearest-neighbour ties break alphabetically by
 * point name.
 *
 * <p>Steps: one NN step per stop added (line 2), one swap
 * step per improving 2-opt swap (line 3), one no-improvement
 * step for the final full pass (line 4), plus the final Step
 * (line 0). {@code step.nodes} are the points (fixed
 * positions, labelled A..F, depot marked by being first);
 * {@code step.edges} are the current tour as ordered edges —
 * every NN step's tour is the path so far closed back to the
 * depot (the closing edge moves to the new last stop), so the
 * edge count grows by exactly one per NN step, and swap /
 * no-improvement / final steps carry the full cycle edges.
 * {@code data} is [tour length &times; 10] (tenths, rounded)
 * — the length of the drawn tour at that step. Demo (depot
 * A(10,50), B(20,20), C(20,80), D(80,20), E(80,80),
 * F(50,50)): NN tour A-B-F-C-E-D = 312.6 (greedy jumps across
 * the map). 2-opt swap 1 (i=2,j=5): 312.6 &rarr; 294.0 (tour
 * A-B-D-E-C-F). 2-opt swap 2 (i=4,j=5): 294.0 &rarr; 268.1
 * (tour A-B-D-E-F-C). The final full pass finds no
 * improvement. Final tour A-B-D-E-F-C = 268.1 = the
 * brute-force optimum (120 tours checked — by the sim, not
 * by this service). 5 NN steps + 2 swap steps + 1
 * no-improvement pass + final = 9 steps.</p>
 *
 * <p>The controller validates the input (word missing or not a
 * "x,y;x,y;..." point list &rarr; 400, fewer than 3 points
 * &rarr; 400); this method guards against bad input anyway
 * with a single "empty" Step.</p>
 */
@Service
public class TspService {

    private static final int LINE_NN = 2;
    private static final int LINE_SWAP = 3;
    private static final int LINE_NO_IMPROVEMENT = 4;
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
     * Runs nearest neighbour + 2-opt. {@code word} is
     * "x,y;x,y;..." — the first point is the depot.
     */
    public List<Step> tsp(String word) {
        List<Step> steps = new ArrayList<>();
        List<int[]> coords = parsePoints(word);
        if (coords == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Points are needed as "
                            + "\"x,y;x,y;...\" (e.g. "
                            + "\"10,50;20,20;20,80\").", LINE_FINAL));
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
        List<NodeDto> nodeDtos = snapshotNodes(points);

        // Nearest neighbour (ties break alphabetically — the
        // points are visited in name order with a strict test).
        List<Point> tour = new ArrayList<>();
        tour.add(points.get(0));
        List<Point> rest = new ArrayList<>(points.subList(1, points.size()));
        Point cur = points.get(0);
        while (!rest.isEmpty()) {
            Point best = null;
            double bestDist = Double.POSITIVE_INFINITY;
            for (Point p : rest) {
                double d = dist(cur, p);
                if (d < bestDist - EPS) {
                    bestDist = d;
                    best = p;
                }
            }
            tour.add(best);
            rest.remove(best);
            cur = best;
            steps.add(new Step(
                    List.of((int) Math.round(tourLength(tour) * 10)),
                    List.of(), List.of(), List.of(),
                    "Add " + best.name + "(" + best.x + "," + best.y
                            + "): tour " + tourText(tour) + ", length "
                            + fmt(tourLength(tour)) + ".",
                    LINE_NN, nodeDtos, tourEdges(tour)));
        }

        // 2-opt until a full pass finds no improvement (the sim's
        // loop: the nested loops keep going after a swap, with
        // the updated tour).
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 1; i < tour.size() - 1; i++) {
                for (int j = i + 1; j < tour.size(); j++) {
                    List<Point> cand = new ArrayList<>();
                    cand.addAll(tour.subList(0, i));
                    for (int k = j; k >= i; k--) {
                        cand.add(tour.get(k));
                    }
                    cand.addAll(tour.subList(j + 1, tour.size()));
                    double oldLen = tourLength(tour);
                    double newLen = tourLength(cand);
                    if (newLen < oldLen - EPS) {
                        tour = cand;
                        improved = true;
                        steps.add(new Step(
                                List.of((int) Math.round(newLen * 10)),
                                List.of(), List.of(), List.of(),
                                "2-opt swap (i=" + i + ", j=" + j + "): "
                                        + fmt(oldLen) + " → "
                                        + fmt(newLen) + " — tour "
                                        + tourText(tour) + ".",
                                LINE_SWAP, nodeDtos, tourEdges(tour)));
                    }
                }
            }
            if (!improved) {
                steps.add(new Step(
                        List.of((int) Math.round(tourLength(tour) * 10)),
                        List.of(), List.of(), List.of(),
                        "Full 2-opt pass: no improvement — tour "
                                + tourText(tour) + ", length "
                                + fmt(tourLength(tour)) + ".",
                        LINE_NO_IMPROVEMENT, nodeDtos, tourEdges(tour)));
            }
        }
        steps.add(new Step(
                List.of((int) Math.round(tourLength(tour) * 10)),
                List.of(), List.of(), List.of(),
                "Tour: " + tourText(tour) + ", length "
                        + fmt(tourLength(tour)) + ".",
                LINE_FINAL, nodeDtos, tourEdges(tour)));
        return steps;
    }

    /** The cycle length of a tour (including the return to the depot). */
    private double tourLength(List<Point> tour) {
        double total = 0;
        for (int i = 0; i < tour.size(); i++) {
            total += dist(tour.get(i), tour.get((i + 1) % tour.size()));
        }
        return total;
    }

    /** Euclidean distance between two points. */
    private double dist(Point a, Point b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }

    /** The tour as ordered edges (including the return to the depot). */
    private List<EdgeDto> tourEdges(List<Point> tour) {
        List<EdgeDto> edges = new ArrayList<>();
        for (int i = 0; i < tour.size(); i++) {
            Point from = tour.get(i);
            Point to = tour.get((i + 1) % tour.size());
            edges.add(new EdgeDto(from.index, to.index, null));
        }
        return edges;
    }

    /** "A → B → C" rendering of a tour. */
    private String tourText(List<Point> tour) {
        List<String> names = new ArrayList<>();
        for (Point p : tour) {
            names.add(p.name);
        }
        return String.join(" → ", names);
    }

    /** One NodeDto per point (id = index, label = name). */
    private List<NodeDto> snapshotNodes(List<Point> points) {
        List<NodeDto> dtos = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            NodeDto dto = new NodeDto(i, i, p.x, p.y);
            dto.setLabel(p.name);
            dtos.add(dto);
        }
        return dtos;
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

    /** A length with 1 decimal (the design's display precision). */
    private String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
