package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Line sweep — segment intersections (Phase 24,
 * computational geometry): a vertical sweep line moves
 * left to right; segments join the active set at their
 * left endpoint and leave at their right one. Only
 * neighbours in the active set can cross, so each crossing
 * is found when the sweep line reaches it. (This service
 * precomputes the crossings and emits them as events in
 * sweep order — the trace the learner sees is identical.)
 *
 * <p>Steps: one event step per event (line 2 for a segment
 * start/end, line 3 for a crossing found) + the final Step
 * (line 0). Events sort by x, then y, then kind (start
 * before crossing before end). {@code step.nodes} are the
 * distinct segment endpoints, in first-appearance order in
 * the input word (id = endpoint index, {@code value} = 0,
 * label = the point name); {@code step.edges} are the
 * segments (endpoint node ids, weight null).
 * {@code data} is [intersections found so far];
 * {@code highlight} is [the endpoint node id] for a
 * start/end event, or [the two segment indices] for a
 * crossing; {@code sorted} is the active segment indices,
 * in y-order at the sweep x (empty before the first start).
 * Demo (S1 (1,1)-(6,6), S2 (1,6)-(6,1), S3 (2,0)-(2,7)):
 * intersections S1&times;S2 (3.5,3.5), S1&times;S3 (2,2),
 * S2&times;S3 (2,5). Events: starts (1,1) S1, (1,6) S2,
 * (2,0) S3; crossings (2,2), (2,5), (3.5,3.5); ends (2,7)
 * S3, (6,1) S2, (6,6) S1. 9 event steps + final =
 * 10 steps; final {@code data} = [3].</p>
 *
 * <p>The controller validates the input (word missing or
 * not a "x1,y1,x2,y2;..." segment list &rarr; 400); this
 * method guards against bad input anyway with a single
 * "empty" Step.</p>
 */
@Service
public class LineSweepService {

    private static final int LINE_EVENT = 2;
    private static final int LINE_CROSSING = 3;
    private static final int LINE_FINAL = 0;

    private static final double EPS = 1e-9;

    /** One segment: name, endpoints (start = left, then lower y), node ids. */
    private static class Segment {
        final String name;
        final double x1;
        final double y1;
        final double x2;
        final double y2;
        final int startNode;
        final int endNode;

        Segment(String name, double x1, double y1, double x2, double y2,
                int startNode, int endNode) {
            this.name = name;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.startNode = startNode;
            this.endNode = endNode;
        }
    }

    /** One sweep event: start, end, or a crossing. */
    private static class Event {
        final double x;
        final double y;
        final int kind; // 0 = start, 1 = crossing, 2 = end
        final int segment;
        final int other;
        final int nodeId;

        Event(double x, double y, int kind, int segment, int other,
                int nodeId) {
            this.x = x;
            this.y = y;
            this.kind = kind;
            this.segment = segment;
            this.other = other;
            this.nodeId = nodeId;
        }
    }

    /** One intersection point, with the two segments that meet there. */
    private static class Crossing {
        final double x;
        final double y;
        final int first;
        final int second;

        Crossing(double x, double y, int first, int second) {
            this.x = x;
            this.y = y;
            this.first = first;
            this.second = second;
        }
    }

    /**
     * Runs the line sweep. {@code word} is
     * "x1,y1,x2,y2;...".
     */
    public List<Step> lineSweep(String word) {
        List<Step> steps = new ArrayList<>();
        List<int[]> raw = parseSegments(word);
        if (raw == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Segments are needed as "
                            + "\"x1,y1,x2,y2;...\" (e.g. "
                            + "\"1,1,6,6;1,6,6,1\").", LINE_FINAL));
            return steps;
        }

        // Endpoint node ids: first-appearance order in the
        // input word (segment by segment, as written).
        Map<String, Integer> nodeIds = new HashMap<>();
        List<int[]> endpoints = new ArrayList<>();
        for (int[] seg : raw) {
            for (int e = 0; e < 2; e++) {
                String key = seg[e * 2] + "," + seg[e * 2 + 1];
                if (!nodeIds.containsKey(key)) {
                    nodeIds.put(key, endpoints.size());
                    endpoints.add(new int[]{seg[e * 2], seg[e * 2 + 1]});
                }
            }
        }
        List<NodeDto> nodes = new ArrayList<>();
        for (int i = 0; i < endpoints.size(); i++) {
            NodeDto dto = new NodeDto(i, 0,
                    endpoints.get(i)[0], endpoints.get(i)[1]);
            dto.setLabel(nameOf(i));
            nodes.add(dto);
        }

        // Segments, normalised: start = the left endpoint
        // (ties: the lower y).
        List<Segment> segments = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            int[] s = raw.get(i);
            int startNode = nodeIds.get(s[0] + "," + s[1]);
            int endNode = nodeIds.get(s[2] + "," + s[3]);
            if (s[0] < s[2] || (s[0] == s[2] && s[1] <= s[3])) {
                segments.add(new Segment("S" + (i + 1), s[0], s[1],
                        s[2], s[3], startNode, endNode));
            } else {
                segments.add(new Segment("S" + (i + 1), s[2], s[3],
                        s[0], s[1], endNode, startNode));
            }
        }
        List<EdgeDto> edges = new ArrayList<>();
        for (Segment s : segments) {
            edges.add(new EdgeDto(s.startNode, s.endNode, null));
        }

        // All single-point intersections (a shared endpoint is
        // a touch, not a crossing; collinear overlaps have no
        // single point).
        List<Crossing> crossings = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            for (int j = i + 1; j < segments.size(); j++) {
                double[] at = intersection(segments.get(i),
                        segments.get(j));
                if (at != null) {
                    crossings.add(new Crossing(at[0], at[1], i, j));
                }
            }
        }

        // Events: starts, ends, crossings — by x, then y, then
        // kind (start before crossing before end).
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            Segment s = segments.get(i);
            events.add(new Event(s.x1, s.y1, 0, i, -1, s.startNode));
            events.add(new Event(s.x2, s.y2, 2, i, -1, s.endNode));
        }
        for (Crossing c : crossings) {
            events.add(new Event(c.x, c.y, 1, c.first, c.second, -1));
        }
        events.sort(Comparator.comparingDouble((Event e) -> e.x)
                .thenComparingDouble(e -> e.y)
                .thenComparingInt(e -> e.kind)
                .thenComparingInt(e -> e.segment));

        List<Integer> active = new ArrayList<>();
        int found = 0;
        for (Event e : events) {
            Segment s = segments.get(e.segment);
            String message;
            int line;
            List<Integer> highlight;
            if (e.kind == 0) {
                insertActive(active, segments, e.segment, e.x);
                highlight = List.of(e.nodeId);
                line = LINE_EVENT;
                message = "Start " + s.name + " at ("
                        + fmt(e.x) + "," + fmt(e.y) + "): active "
                        + activeText(active, segments) + ".";
            } else if (e.kind == 2) {
                active.remove(Integer.valueOf(e.segment));
                highlight = List.of(e.nodeId);
                line = LINE_EVENT;
                message = "End " + s.name + " at ("
                        + fmt(e.x) + "," + fmt(e.y) + "): active "
                        + activeText(active, segments) + ".";
            } else {
                found++;
                int i = active.indexOf(e.segment);
                int j = active.indexOf(e.other);
                if (i >= 0 && j >= 0) {
                    active.set(i, e.other);
                    active.set(j, e.segment);
                }
                highlight = List.of(e.segment, e.other);
                line = LINE_CROSSING;
                message = "Crossing " + segments.get(e.segment).name
                        + "×" + segments.get(e.other).name + " at ("
                        + fmt(e.x) + "," + fmt(e.y) + ") — "
                        + found + " intersection"
                        + (found == 1 ? "" : "s") + " found.";
            }
            steps.add(new Step(List.of(found), highlight,
                    List.of(), new ArrayList<>(active), message, line,
                    nodes, edges));
        }
        // Crossings in event order (x, then y) for the final message.
        crossings.sort(Comparator.comparingDouble((Crossing c) -> c.x)
                .thenComparingDouble(c -> c.y));
        List<String> foundText = new ArrayList<>();
        for (Crossing c : crossings) {
            foundText.add(segments.get(c.first).name + "×"
                    + segments.get(c.second).name + " at ("
                    + fmt(c.x) + "," + fmt(c.y) + ")");
        }
        steps.add(new Step(List.of(found), List.of(), List.of(),
                new ArrayList<>(active),
                found + " intersection" + (found == 1 ? "" : "s")
                        + (found == 0 ? "."
                        : ": " + String.join(", ", foundText) + "."),
                LINE_FINAL, nodes, edges));
        return steps;
    }

    /** Inserts a segment into the active set, in y-order at the sweep x. */
    private void insertActive(List<Integer> active,
                              List<Segment> segments, int seg, double x) {
        double y = yAt(segments.get(seg), x);
        int pos = 0;
        // The sim's rule: insert after strictly smaller y (a tie
        // puts the new segment before the equal one).
        while (pos < active.size()
                && yAt(segments.get(active.get(pos)), x) < y - EPS) {
            pos++;
        }
        active.add(pos, seg);
    }

    /** The y of a segment at sweep x (a vertical segment sits at its start y). */
    private double yAt(Segment s, double x) {
        if (Math.abs(s.x2 - s.x1) < EPS) {
            return s.y1;
        }
        double t = (x - s.x1) / (s.x2 - s.x1);
        return s.y1 + t * (s.y2 - s.y1);
    }

    /**
     * The single intersection point of two segments, or null
     * when they are parallel, collinear, disjoint, or meet
     * only at a shared endpoint.
     */
    private double[] intersection(Segment a, Segment b) {
        double rx = a.x2 - a.x1;
        double ry = a.y2 - a.y1;
        double sx = b.x2 - b.x1;
        double sy = b.y2 - b.y1;
        double denom = rx * sy - ry * sx;
        if (Math.abs(denom) < EPS) {
            return null;
        }
        double qpx = b.x1 - a.x1;
        double qpy = b.y1 - a.y1;
        double t = (qpx * sy - qpy * sx) / denom;
        double u = (qpx * ry - qpy * rx) / denom;
        if (t < -EPS || t > 1 + EPS || u < -EPS || u > 1 + EPS) {
            return null;
        }
        double x = a.x1 + t * rx;
        double y = a.y1 + t * ry;
        // A shared endpoint is a touch, not a crossing.
        if (isEndpoint(a, x, y) && isEndpoint(b, x, y)) {
            return null;
        }
        return new double[]{x, y};
    }

    private boolean isEndpoint(Segment s, double x, double y) {
        return (Math.abs(s.x1 - x) < EPS && Math.abs(s.y1 - y) < EPS)
                || (Math.abs(s.x2 - x) < EPS && Math.abs(s.y2 - y) < EPS);
    }

    /** "[S1, S2]" rendering of the active set. */
    private String activeText(List<Integer> active,
                              List<Segment> segments) {
        List<String> names = new ArrayList<>();
        for (int i : active) {
            names.add(segments.get(i).name);
        }
        return "[" + String.join(", ", names) + "]";
    }

    /** Endpoint point names: A..Z, then P27, P28, ... */
    private String nameOf(int i) {
        if (i < 26) {
            return String.valueOf((char) ('A' + i));
        }
        return "P" + (i + 1);
    }

    /**
     * Parses "x1,y1,x2,y2;..." into integer segments, or null
     * when the word is missing or unparseable.
     */
    private List<int[]> parseSegments(String word) {
        if (word == null || word.isBlank()) {
            return null;
        }
        List<int[]> segments = new ArrayList<>();
        for (String part : word.split(";", -1)) {
            String[] xy = part.trim().split(",", -1);
            if (xy.length != 4) {
                return null;
            }
            try {
                int[] s = new int[4];
                for (int i = 0; i < 4; i++) {
                    s[i] = Integer.parseInt(xy[i].trim());
                }
                segments.add(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return segments.isEmpty() ? null : segments;
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
