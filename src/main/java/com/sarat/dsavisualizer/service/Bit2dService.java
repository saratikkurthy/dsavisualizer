package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 2D Fenwick tree / 2D BIT (Phase 28, range queries v4):
 * the 1D Fenwick idea in two dimensions. BIT[i][j]
 * (1-indexed) stores the sum of a rectangle of the
 * original matrix; a point update walks i += i &amp;
 * -i, j += j &amp; -j and adds the delta to every BIT
 * cell it visits (O(log^2 n)), a prefix sum walks
 * i -= i &amp; -i, j -= j &amp; -j and adds the BIT
 * cells it visits, and a rectangle sum is four prefix
 * sums by inclusion-exclusion:
 * rect(x1,y1,x2,y2) = S(x2,y2) - S(x1-1,y2) -
 * S(x2,y1-1) + S(x1-1,y1-1) (a term is skipped when
 * its corner is out of range). All coordinates in the
 * trace are 0-indexed; the BIT itself is 1-indexed.
 *
 * <p>Demo 4x4 matrix [[1..4],[5..8],[9..12],[13..16]].
 * The pinned BIT state after build (rows [1,3,3,10],
 * [6,14,10,36], [9,19,11,42], [28,60,36,136]) has
 * BIT[4][4] = 136, the total sum. Query rect (0,0)-
 * (2,2) = 54, then +5 at (1,1), then query rect
 * (1,1)-(3,3) = 104. Trace: 1 build step (line 2) +
 * query 1 (line 3) + update (line 4) + query 2 (line
 * 3) + final (line 0) = 5 steps. The order matters:
 * query 1 runs BEFORE the update (54 is the
 * pre-update sum), query 2 after it. {@code nodes} =
 * one NodeDto per BIT cell (1-indexed (i,j)): id =
 * (i-1)*cols + (j-1), {@code value} = BIT[i][j],
 * {@code label} = "(i,j)". No {@code edges} (null).
 * Build step: nodes carry the pinned BIT values;
 * {@code data} = [136] (total sum). Update step (+5
 * at (1,1) 0-indexed = BIT cell (2,2)): {@code
 * highlight} = the BIT cells the update touches (the
 * standard BIT update walk from (2,2): [5, 7, 13,
 * 15] as ids); {@code data} = [5] (delta). Query
 * steps: {@code highlight} = the BIT cells the
 * prefix-sum walk reads for that rectangle's four
 * inclusion-exclusion terms, in walk order (query 1:
 * [10, 9, 6, 5]; query 2: [15, 3, 12, 0]); {@code
 * data} = [answer] (54, then 104). Final step:
 * {@code data} = [104], {@code highlight} = [].
 * Nodes are snapshotted per step, so the query 1
 * nodes are the pre-update BIT and the later steps
 * carry the post-update values.</p>
 *
 * <p>Bad input (an unparseable matrix, or a values
 * list that is not exactly [ux, uy, delta, q1x1,
 * q1y1, q1x2, q1y2, q2x1, q2y1, q2x2, q2y2]) yields a
 * single "empty" Step; the controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class Bit2dService {

    private static final int LINE_BUILD = 2;
    private static final int LINE_QUERY = 3;
    private static final int LINE_UPDATE = 4;
    private static final int LINE_FINAL = 0;

    /**
     * Builds a 2D BIT over the matrix in {@code word},
     * answers query 1, applies the point update, then
     * answers query 2. {@code values} =
     * [ux, uy, delta, q1x1, q1y1, q1x2, q1y2, q2x1,
     * q2y1, q2x2, q2y2] (all 0-indexed; x = row, y =
     * column).
     */
    public List<Step> bit2d(String word, List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        int[][] matrix = parseMatrix(word);
        if (matrix == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A matrix is needed as "
                            + "\"a,b,c;d,e,f\".", LINE_FINAL));
            return steps;
        }
        if (values == null || values.size() != 11) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The update and the two query "
                            + "rectangles are needed as [ux, uy, "
                            + "delta, q1x1, q1y1, q1x2, q1y2, q2x1, "
                            + "q2y1, q2x2, q2y2].", LINE_FINAL));
            return steps;
        }
        int rows = matrix.length;
        int cols = matrix[0].length;
        int ux = values.get(0);
        int uy = values.get(1);
        int delta = values.get(2);
        int q1x1 = values.get(3);
        int q1y1 = values.get(4);
        int q1x2 = values.get(5);
        int q1y2 = values.get(6);
        int q2x1 = values.get(7);
        int q2y1 = values.get(8);
        int q2x2 = values.get(9);
        int q2y2 = values.get(10);
        if (!inRange(ux, uy, rows, cols)
                || !rectInRange(q1x1, q1y1, q1x2, q1y2, rows, cols)
                || !rectInRange(q2x1, q2y1, q2x2, q2y2, rows, cols)) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A coordinate or rectangle is out "
                            + "of range.", LINE_FINAL));
            return steps;
        }

        int[][] bit = new int[rows + 1][cols + 1];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                add(bit, rows, cols, i, j, matrix[i][j]);
            }
        }
        int total = prefixSum(bit, rows - 1, cols - 1);
        steps.add(new Step(List.of(total), List.of(), List.of(),
                List.of(),
                "Built a " + rows + "x" + cols + " 2D BIT — BIT["
                        + rows + "][" + cols + "] = " + total
                        + ", the total sum.",
                LINE_BUILD, snapshot(bit, rows, cols), null));

        int answer1 = rectSum(bit, q1x1, q1y1, q1x2, q1y2);
        steps.add(new Step(List.of(answer1),
                rectHighlight(cols, q1x1, q1y1, q1x2, q1y2),
                List.of(), List.of(),
                "Query rect (" + q1x1 + "," + q1y1 + ")-(" + q1x2
                        + "," + q1y2 + ") = " + answer1 + ".",
                LINE_QUERY, snapshot(bit, rows, cols), null));

        List<Integer> updateCells = add(bit, rows, cols, ux, uy, delta);
        steps.add(new Step(List.of(delta), updateCells, List.of(),
                List.of(),
                "Update +" + delta + " at (" + ux + "," + uy + "): "
                        + updateCells.size() + " BIT cells touched.",
                LINE_UPDATE, snapshot(bit, rows, cols), null));

        int answer2 = rectSum(bit, q2x1, q2y1, q2x2, q2y2);
        steps.add(new Step(List.of(answer2),
                rectHighlight(cols, q2x1, q2y1, q2x2, q2y2),
                List.of(), List.of(),
                "Query rect (" + q2x1 + "," + q2y1 + ")-(" + q2x2
                        + "," + q2y2 + ") = " + answer2 + ".",
                LINE_QUERY, snapshot(bit, rows, cols), null));

        steps.add(new Step(List.of(answer2), List.of(), List.of(),
                List.of(),
                "2D BIT done: rect (" + q1x1 + "," + q1y1 + ")-("
                        + q1x2 + "," + q1y2 + ")=" + answer1
                        + ", after +" + delta + " at (" + ux + ","
                        + uy + ") rect (" + q2x1 + "," + q2y1
                        + ")-(" + q2x2 + "," + q2y2 + ")=" + answer2
                        + ".",
                LINE_FINAL, snapshot(bit, rows, cols), null));
        return steps;
    }

    /** BIT point update at 0-indexed (x, y); returns the touched cell ids. */
    private List<Integer> add(int[][] bit, int rows, int cols,
                              int x, int y, int delta) {
        List<Integer> touched = new ArrayList<>();
        for (int i = x + 1; i <= rows; i += i & -i) {
            for (int j = y + 1; j <= cols; j += j & -j) {
                bit[i][j] += delta;
                touched.add((i - 1) * cols + (j - 1));
            }
        }
        return touched;
    }

    /** Prefix sum over (0,0)..(x,y) inclusive, 0-indexed. */
    private int prefixSum(int[][] bit, int x, int y) {
        int sum = 0;
        for (int i = x + 1; i > 0; i -= i & -i) {
            for (int j = y + 1; j > 0; j -= j & -j) {
                sum += bit[i][j];
            }
        }
        return sum;
    }

    /** The BIT cell ids the prefix-sum walk for (x,y) reads, in walk order. */
    private List<Integer> prefixCells(int cols, int x, int y) {
        List<Integer> cells = new ArrayList<>();
        for (int i = x + 1; i > 0; i -= i & -i) {
            for (int j = y + 1; j > 0; j -= j & -j) {
                cells.add((i - 1) * cols + (j - 1));
            }
        }
        return cells;
    }

    /** Rectangle sum by inclusion-exclusion over four prefix sums. */
    private int rectSum(int[][] bit, int x1, int y1, int x2, int y2) {
        int sum = prefixSum(bit, x2, y2);
        if (x1 > 0) {
            sum -= prefixSum(bit, x1 - 1, y2);
        }
        if (y1 > 0) {
            sum -= prefixSum(bit, x2, y1 - 1);
        }
        if (x1 > 0 && y1 > 0) {
            sum += prefixSum(bit, x1 - 1, y1 - 1);
        }
        return sum;
    }

    /** BIT cell ids the rectangle's four prefix-sum walks read, in term order. */
    private List<Integer> rectHighlight(int cols,
                                        int x1, int y1, int x2, int y2) {
        List<Integer> cells = new ArrayList<>(prefixCells(cols, x2, y2));
        if (x1 > 0) {
            cells.addAll(prefixCells(cols, x1 - 1, y2));
        }
        if (y1 > 0) {
            cells.addAll(prefixCells(cols, x2, y1 - 1));
        }
        if (x1 > 0 && y1 > 0) {
            cells.addAll(prefixCells(cols, x1 - 1, y1 - 1));
        }
        return cells;
    }

    /** One NodeDto per BIT cell, row-major (1-indexed (i,j)). */
    private List<NodeDto> snapshot(int[][] bit, int rows, int cols) {
        List<NodeDto> nodes = new ArrayList<>();
        for (int i = 1; i <= rows; i++) {
            for (int j = 1; j <= cols; j++) {
                NodeDto dto = new NodeDto((i - 1) * cols + (j - 1),
                        bit[i][j], 0, 0);
                dto.setLabel("(" + i + "," + j + ")");
                nodes.add(dto);
            }
        }
        return nodes;
    }

    private boolean inRange(int x, int y, int rows, int cols) {
        return x >= 0 && x < rows && y >= 0 && y < cols;
    }

    private boolean rectInRange(int x1, int y1, int x2, int y2,
                                int rows, int cols) {
        return inRange(x1, y1, rows, cols) && inRange(x2, y2, rows, cols)
                && x1 <= x2 && y1 <= y2;
    }

    /** Parses "1,2,3,4;5,6,7,8" into an int matrix (2..6 x 2..6), or null. */
    private int[][] parseMatrix(String word) {
        if (word == null || word.isBlank()) {
            return null;
        }
        String[] rows = word.trim().split(";", -1);
        if (rows.length < 2 || rows.length > 6) {
            return null;
        }
        int[][] result = null;
        for (int i = 0; i < rows.length; i++) {
            String[] cells = rows[i].trim().split(",", -1);
            if (result == null) {
                if (cells.length < 2 || cells.length > 6) {
                    return null;
                }
                result = new int[rows.length][cells.length];
            } else if (cells.length != result[0].length) {
                return null;
            }
            try {
                for (int j = 0; j < cells.length; j++) {
                    int v = Integer.parseInt(cells[j].trim());
                    if (v < -999 || v > 999) {
                        return null;
                    }
                    result[i][j] = v;
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return result;
    }
}
