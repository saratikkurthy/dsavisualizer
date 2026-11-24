package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Burrows-Wheeler Transform (Phase 28, string matching v4):
 * append a sentinel '$' (it sorts before 'a'), write all
 * rotations of the string, sort them, and read the last
 * column — that column is the BWT, the reversible
 * permutation that makes the text compressible (bzip2's
 * core step: equal characters cluster together in the
 * last column, so run-length coding after the BWT is
 * cheap). Sorting the rotations is O(n^2 log n) here
 * (n &le; 12, the strings are compared directly); the
 * production version sorts suffixes instead.
 *
 * <p>Demo "banana" -&gt; "banana$" (7 rotations). Sorted
 * order by start index [6, 5, 3, 1, 0, 4, 2]; the last
 * column reads "annb$aa". Trace: 1 build step (line 2)
 * + 7 placement steps (line 3, one per sorted row) +
 * final (line 0) = 9 steps. {@code nodes} = one NodeDto
 * per rotation, in ORIGINAL order: id = start index,
 * {@code value} = start index, {@code label} = the
 * rotation string. No {@code edges} (null). Build step:
 * {@code sorted} = [] (nothing placed yet). Placement
 * steps: {@code sorted} grows by one id per step in
 * sorted order ([6], [6, 5], ...), {@code data} = [rows
 * placed so far], {@code highlight} = [the rotation id
 * placed this step]. Final step: {@code data} = [7]; the
 * visualizer renders the table in {@code sorted} order
 * and reads the BWT from the last character of each
 * row's label — never from message text.</p>
 *
 * <p>Bad input (a missing/blank word, or one that is not
 * lowercase a-z) yields a single "empty" Step; the
 * controller 400s on it (see VisualizationController).</p>
 */
@Service
public class BwtService {

    private static final int LINE_BUILD = 2;
    private static final int LINE_PLACE = 3;
    private static final int LINE_FINAL = 0;

    /** BWT of {@code word} (the '$' sentinel is appended internally). */
    public List<Step> bwt(String word) {
        List<Step> steps = new ArrayList<>();
        if (word == null || word.isBlank()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A word is needed for the "
                            + "Burrows-Wheeler transform.", LINE_FINAL));
            return steps;
        }
        String text = word.trim();
        for (char c : text.toCharArray()) {
            if (c < 'a' || c > 'z') {
                steps.add(new Step(List.of(), List.of(), List.of(),
                        List.of(), "The Burrows-Wheeler transform "
                                + "needs lowercase a-z only.",
                        LINE_FINAL));
                return steps;
            }
        }
        String s = text + "$";
        int n = s.length();
        String[] rotations = new String[n];
        for (int i = 0; i < n; i++) {
            rotations[i] = s.substring(i) + s.substring(0, i);
        }
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> rotations[a].compareTo(rotations[b]));

        List<NodeDto> nodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            NodeDto dto = new NodeDto(i, i, 0, 0);
            dto.setLabel(rotations[i]);
            nodes.add(dto);
        }
        steps.add(new Step(List.of(0), List.of(), List.of(), List.of(),
                "Built " + n + " rotations of '" + s + "' — the '$' "
                        + "sentinel sorts before 'a'.",
                LINE_BUILD, nodes, null));

        List<Integer> placed = new ArrayList<>();
        StringBuilder lastColumn = new StringBuilder();
        for (int k = 0; k < n; k++) {
            int id = order[k];
            placed.add(id);
            lastColumn.append(rotations[id].charAt(n - 1));
            steps.add(new Step(List.of(placed.size()), List.of(id),
                    List.of(), new ArrayList<>(placed),
                    "Placed rotation " + id + " '" + rotations[id]
                            + "' at sorted position " + (k + 1)
                            + " — last column so far '"
                            + lastColumn + "'.",
                    LINE_PLACE, nodes, null));
        }
        steps.add(new Step(List.of(n), List.of(), List.of(),
                new ArrayList<>(placed),
                "Burrows-Wheeler transform done: BWT = '"
                        + lastColumn + "' (the last column of the "
                        + "sorted rotations).",
                LINE_FINAL, nodes, null));
        return steps;
    }
}
