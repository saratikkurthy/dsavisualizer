package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Set matrix zeroes (Phase 29, matrices v2): the O(1)-space
 * algorithm — the first row and column double as markers.
 * A zero at (r, c) marks a[0][c] = 0 (column c must be
 * zeroed) and a[r][0] = 0 (row r must be zeroed); a zero in
 * row 0 or column 0 is remembered by the firstRowHasZero /
 * firstColHasZero flags instead, because its marker would be
 * itself. The marked rows and columns are then zeroed, rows
 * first (ascending) then columns (ascending).
 *
 * <p>Demo 3x3 [[1,1,1],[1,0,1],[1,1,1]], zero at (1,1).
 * Value progression (flat): build [1,1,1, 1,0,1, 1,1,1]
 * -&gt; mark [1,0,1, 0,0,1, 1,1,1] -&gt; zero row 1 [1,0,1,
 * 0,0,0, 1,1,1] -&gt; zero col 1 [1,0,1, 0,0,0, 1,0,1].
 * Trace: 1 build (line 2) + 1 mark (line 2) + 2 zero
 * (line 3) + final (line 0) = 5 steps. {@code nodes} = one
 * NodeDto per cell: id = r*cols+c, {@code value} = CURRENT
 * cell value (after this step), {@code label} = "r,c". No
 * {@code edges} (null). Build step: {@code data} = [0]
 * (zeroes found so far). Mark steps (one per zero, scan
 * order): {@code data} = [r, c] of the zero, {@code
 * highlight} = [zero cell, col marker, row marker] — the
 * marker ids are omitted when the zero sits in row 0 or
 * column 0, since the marker IS the zero cell. Zero-row
 * steps: {@code data} = [0, row], {@code highlight} = the
 * row's cells. Zero-col steps: {@code data} = [1, col],
 * {@code highlight} = the column's cells. Final step:
 * {@code data} = [zeroes found]. For a general input with Z
 * zeroes in R distinct rows and C distinct columns: 1 build
 * + Z mark + (R + C) zero + 1 final steps.</p>
 *
 * <p>Bad input (an unparseable matrix) yields a single
 * "empty" Step; the controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class SetZeroesService {

    private static final int LINE_BUILD = 2;
    private static final int LINE_MARK = 2;
    private static final int LINE_ZERO = 3;
    private static final int LINE_FINAL = 0;

    /** Zeroes every row and column that contains a zero, in place. */
    public List<Step> setZeroes(String word) {
        List<Step> steps = new ArrayList<>();
        int[][] a = parseMatrix(word);
        if (a == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Set zeroes needs a matrix as "
                            + "\"a,b,c;d,e,f\".", LINE_FINAL));
            return steps;
        }
        int m = a.length;
        int n = a[0].length;
        // Zeroes in scan order (row-major).
        List<int[]> zeros = new ArrayList<>();
        for (int r = 0; r < m; r++) {
            for (int c = 0; c < n; c++) {
                if (a[r][c] == 0) {
                    zeros.add(new int[]{r, c});
                }
            }
        }
        steps.add(new Step(List.of(0), List.of(), List.of(), List.of(),
                "Scan the " + m + "x" + n + " matrix for zeroes.",
                LINE_BUILD, snapshot(a), null));
        boolean firstRowHasZero = false;
        boolean firstColHasZero = false;
        for (int[] z : zeros) {
            if (z[0] == 0) {
                firstRowHasZero = true;
            }
            if (z[1] == 0) {
                firstColHasZero = true;
            }
        }
        // Mark steps: write the row/column markers into the
        // first row and column. A zero in row 0 or column 0
        // writes no marker — the flags above remember it, and
        // the marker cell IS the zero cell there.
        for (int[] z : zeros) {
            int r = z[0];
            int c = z[1];
            List<Integer> highlight = new ArrayList<>();
            highlight.add(r * n + c);
            StringBuilder msg = new StringBuilder(
                    "Zero at (" + r + "," + c + "): ");
            boolean wrote = false;
            if (c > 0 && r != 0) {
                a[0][c] = 0;
                highlight.add(c);
                msg.append("mark column ").append(c)
                        .append(" in the first row");
                wrote = true;
            }
            if (r > 0 && c != 0) {
                a[r][0] = 0;
                highlight.add(r * n);
                if (wrote) {
                    msg.append(" and ");
                }
                msg.append("mark row ").append(r)
                        .append(" in the first column");
                wrote = true;
            }
            if (!wrote) {
                if (r == 0 && c == 0) {
                    msg.append("the first row and column are "
                            + "marked by flag");
                } else if (r == 0) {
                    msg.append("the first row is marked by flag");
                } else {
                    msg.append("the first column is marked by flag");
                }
            }
            msg.append(".");
            steps.add(new Step(List.of(r, c), highlight, List.of(),
                    List.of(), msg.toString(), LINE_MARK, snapshot(a),
                    null));
        }
        // Rows/columns to zero, read from the marker state
        // BEFORE any zero step runs: rows 1..m-1 whose first
        // column cell is 0, plus row 0 when the flag says so;
        // same for columns.
        List<Integer> rowsToZero = new ArrayList<>();
        if (firstRowHasZero) {
            rowsToZero.add(0);
        }
        for (int r = 1; r < m; r++) {
            if (a[r][0] == 0) {
                rowsToZero.add(r);
            }
        }
        List<Integer> colsToZero = new ArrayList<>();
        if (firstColHasZero) {
            colsToZero.add(0);
        }
        for (int c = 1; c < n; c++) {
            if (a[0][c] == 0) {
                colsToZero.add(c);
            }
        }
        for (int r : rowsToZero) {
            for (int c = 0; c < n; c++) {
                a[r][c] = 0;
            }
            List<Integer> highlight = new ArrayList<>();
            for (int c = 0; c < n; c++) {
                highlight.add(r * n + c);
            }
            steps.add(new Step(List.of(0, r), highlight, List.of(),
                    List.of(), "Zero row " + r + ".", LINE_ZERO,
                    snapshot(a), null));
        }
        for (int c : colsToZero) {
            for (int r = 0; r < m; r++) {
                a[r][c] = 0;
            }
            List<Integer> highlight = new ArrayList<>();
            for (int r = 0; r < m; r++) {
                highlight.add(r * n + c);
            }
            steps.add(new Step(List.of(1, c), highlight, List.of(),
                    List.of(), "Zero column " + c + ".", LINE_ZERO,
                    snapshot(a), null));
        }
        steps.add(new Step(List.of(zeros.size()), List.of(), List.of(),
                List.of(),
                "Done: " + zeros.size()
                        + (zeros.size() == 1 ? " zero" : " zeroes")
                        + " found, " + rowsToZero.size()
                        + (rowsToZero.size() == 1 ? " row" : " rows")
                        + " and " + colsToZero.size()
                        + (colsToZero.size() == 1 ? " column"
                                : " columns")
                        + " zeroed.",
                LINE_FINAL, snapshot(a), null));
        return steps;
    }

    /** One NodeDto per cell: id = r*cols+c, value = current value. */
    private static List<NodeDto> snapshot(int[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        List<NodeDto> nodes = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                NodeDto dto = new NodeDto(r * cols + c, matrix[r][c], 0, 0);
                dto.setLabel(r + "," + c);
                nodes.add(dto);
            }
        }
        return nodes;
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
