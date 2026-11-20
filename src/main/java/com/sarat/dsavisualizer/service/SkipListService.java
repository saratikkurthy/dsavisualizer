package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Skip List (Phase 18): a sorted linked list with express
 * lanes. Each key gets a tower whose height is drawn at insert
 * time; a tower of height h links forward at levels 1..h, so
 * search skips ahead on the higher lanes.
 *
 * Levels are random in a real skip list; this demo uses a fixed
 * seed so the trace is reproducible: {@code new Random(42)},
 * level starts at 1, {@code while (random.nextDouble() < 0.5
 * && level < 4) level++}. Max level 4. A duplicate key is
 * skipped silently (no step, and no random draw — the B-tree
 * precedent).
 *
 * The controller validates the input (1..8 values, |v| &lt;= 99);
 * this method guards against bad input anyway with a single
 * "empty" Step.
 *
 * Trace: one step per insert (line 2), then the final step
 * (line 0). {@code nodes} are the tower cells (NodeDto: value =
 * key, x = sorted index 0-based, y = level 1-based, 1 = bottom);
 * cell ids are assigned at insert time in insertion order and
 * stay stable across steps. {@code edges} are the horizontal
 * forward links per level plus the vertical tower links
 * (bottom to top). The header column is NOT a node — the
 * frontend draws it. {@code highlight} is the new tower's cell
 * ids.
 */
@Service
public class SkipListService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_FINAL = 0;

    /** Maximum tower height. */
    private static final int MAX_LEVEL = 4;

    /** One key's tower: its level and its cell ids (bottom to top). */
    private static class Tower {
        final int key;
        final int level;
        final List<Integer> cellIds = new ArrayList<>();

        Tower(int key, int level) {
            this.key = key;
            this.level = level;
        }
    }

    /**
     * Inserts {@code values} in order, drawing each new key's
     * level from a {@code Random(42)}.
     */
    public List<Step> insert(List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A list of values is needed.",
                    LINE_FINAL));
            return steps;
        }
        Random random = new Random(42);
        List<Tower> towers = new ArrayList<>();
        int nextId = 0;
        for (int key : values) {
            if (contains(towers, key)) {
                continue;
            }
            int level = 1;
            while (random.nextDouble() < 0.5 && level < MAX_LEVEL) {
                level++;
            }
            String message = insertMessage(towers, key, level);
            Tower tower = new Tower(key, level);
            for (int l = 0; l < level; l++) {
                tower.cellIds.add(nextId++);
            }
            int pos = 0;
            while (pos < towers.size() && towers.get(pos).key < key) {
                pos++;
            }
            towers.add(pos, tower);
            steps.add(skipListStep(new ArrayList<>(tower.cellIds),
                    snapshotNodes(towers), snapshotEdges(towers),
                    message, LINE_COMPARE));
        }
        List<Integer> heights = new ArrayList<>();
        int maxLevel = 0;
        for (Tower tower : towers) {
            heights.add(tower.level);
            maxLevel = Math.max(maxLevel, tower.level);
        }
        steps.add(skipListStep(List.of(),
                snapshotNodes(towers), snapshotEdges(towers),
                "Skip list (max level " + maxLevel + "): "
                        + towers.size() + " keys; tower heights "
                        + joinInts(heights) + ".",
                LINE_FINAL));
        return steps;
    }

    /** "[1, 1, 3]" without the brackets: "1, 1, 3". */
    private String joinInts(List<Integer> values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(values.get(i));
        }
        return out.toString();
    }

    /** Whether a tower for {@code key} already exists. */
    private boolean contains(List<Tower> towers, int key) {
        for (Tower tower : towers) {
            if (tower.key == key) {
                return true;
            }
        }
        return false;
    }

    /**
     * The insert-step message, composed from the actual
     * predecessors per level (the nearest tower to the left whose
     * height reaches that level, or none). The first key gets the
     * "first key" message; a key whose every level shares one
     * predecessor gets "linked after {p} at level {l}"; otherwise
     * the higher-level predecessor runs come first, then level 1,
     * then the levels that start at the header.
     */
    private String insertMessage(List<Tower> towers, int key, int level) {
        if (towers.isEmpty()) {
            return "Insert " + key + " (level " + level
                    + "): first key; tower of height " + level + ".";
        }
        int pos = 0;
        while (pos < towers.size() && towers.get(pos).key < key) {
            pos++;
        }
        // Predecessor key per level (null when none reaches it).
        List<Integer> pred = new ArrayList<>();
        for (int l = 1; l <= level; l++) {
            Integer found = null;
            for (int j = pos - 1; j >= 0; j--) {
                if (towers.get(j).level >= l) {
                    found = towers.get(j).key;
                    break;
                }
            }
            pred.add(found);
        }
        StringBuilder message = new StringBuilder("Insert " + key
                + " (level " + level + "): ");
        if (pred.get(0) == null) {
            // New smallest key: every level starts at the header.
            return message + (level == 1
                    ? "level 1 starts at the header."
                    : "levels 1–" + level + " start at the header.");
        }
        boolean allSame = true;
        for (int l = 1; l < level; l++) {
            if (!pred.get(0).equals(pred.get(l))) {
                allSame = false;
            }
        }
        if (allSame) {
            return message + "linked after " + pred.get(0)
                    + " at level " + level + ".";
        }
        List<String> linkParts = new ArrayList<>();
        List<String> headerParts = new ArrayList<>();
        // Runs of levels >= 2 with the same predecessor, highest first.
        int l = level;
        while (l >= 2) {
            Integer p = pred.get(l - 1);
            if (p == null) {
                l--;
                continue;
            }
            int low = l;
            while (low - 1 >= 2
                    && p.equals(pred.get(low - 2))) {
                low--;
            }
            linkParts.add("linked after " + p + (low == l
                    ? " at level " + l
                    : " at levels " + low + "–" + l));
            l = low - 1;
        }
        linkParts.add((linkParts.isEmpty() ? "linked after "
                : "after ") + pred.get(0) + " at level 1");
        // Runs of levels with no predecessor (start at the header).
        int h = level;
        while (h >= 1) {
            if (pred.get(h - 1) != null) {
                h--;
                continue;
            }
            int low = h;
            while (low - 1 >= 1 && pred.get(low - 2) == null) {
                low--;
            }
            headerParts.add((low == h ? "level " + h : "levels "
                    + low + "–" + h) + (low == h ? " starts" : " start")
                    + " at the header");
            h = low - 1;
        }
        // Link clauses join with ", "; the header clause follows
        // after "; " (the design doc's format 4).
        String joined = String.join(", ", linkParts);
        if (!headerParts.isEmpty()) {
            joined += "; " + String.join("; ", headerParts);
        }
        return message + joined + ".";
    }

    /** One skip-list Step: new-tower cell ids in highlight. */
    private Step skipListStep(List<Integer> highlight,
                              List<NodeDto> nodes, List<EdgeDto> edges,
                              String message, int line) {
        return new Step(List.of(), highlight, List.of(), List.of(),
                message, line, nodes, edges);
    }

    /** Tower cells: by sorted index, then level (bottom to top). */
    private List<NodeDto> snapshotNodes(List<Tower> towers) {
        List<NodeDto> nodes = new ArrayList<>();
        for (int x = 0; x < towers.size(); x++) {
            Tower tower = towers.get(x);
            for (int l = 0; l < tower.level; l++) {
                nodes.add(new NodeDto(tower.cellIds.get(l), tower.key,
                        x, l + 1));
            }
        }
        return nodes;
    }

    /**
     * Edges: vertical tower links (bottom to top) per tower,
     * then horizontal forward links per level (level 1 first).
     */
    private List<EdgeDto> snapshotEdges(List<Tower> towers) {
        List<EdgeDto> edges = new ArrayList<>();
        for (Tower tower : towers) {
            for (int l = 0; l + 1 < tower.level; l++) {
                edges.add(new EdgeDto(tower.cellIds.get(l),
                        tower.cellIds.get(l + 1)));
            }
        }
        for (int level = 1; level <= MAX_LEVEL; level++) {
            Integer prev = null;
            for (Tower tower : towers) {
                if (tower.level >= level) {
                    if (prev != null) {
                        edges.add(new EdgeDto(prev,
                                tower.cellIds.get(level - 1)));
                    }
                    prev = tower.cellIds.get(level - 1);
                }
            }
        }
        return edges;
    }
}
