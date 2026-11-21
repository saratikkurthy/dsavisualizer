package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Hungarian algorithm (Phase 25, matching &amp; flow):
 * assignment in O(n&sup3;) — row-reduce, column-reduce,
 * cover the zeros with the fewest lines, adjust, and repeat
 * until the zeros admit a perfect assignment. The minimum
 * line cover comes from K&ouml;nig's theorem (cover size =
 * maximum matching among the zeros), found by the standard
 * alternating-tree marking from the unassigned rows.
 *
 * <p>Demo cost matrix (3&times;3): [[4,1,3],[2,0,5],[3,2,2]].
 * Row reduction (row mins 1,0,2): [[3,0,2],[2,0,5],[1,0,0]].
 * Column reduction (col mins 1,0,0): [[2,0,2],[1,0,5],
 * [0,0,0]]. Zeros: (0,1),(1,1),(2,0),(2,1),(2,2). Minimum
 * cover: <b>2 lines</b> (column 1 + row 2) &lt; 3, so adjust:
 * minimum uncovered = 1; subtract from uncovered, add to
 * the double-covered cell (2,1): [[1,0,1],[0,0,4],[0,1,0]].
 * Zeros now admit the assignment (0,1),(1,0),(2,2) —
 * optimal cost <b>5</b> (1+2+2; brute force over 6
 * permutations confirms 5 is the minimum). Trace: row
 * reduce (line 2) + col reduce (line 3) + cover (line 4)
 * + adjust (line 5) + assign (line 6) + final (line 0) =
 * 6 steps. Per step: {@code data} = the matrix flattened
 * row-major (9 numbers), {@code highlight} = [the cells
 * changed or covered this step, as r*3+c ids], {@code sorted}
 * = [] until the assign step, then [0,1,1,0,2,2] (the
 * assignment pairs, row&rarr;col flattened). Final step
 * {@code data} = the final matrix flattened; the visualizer
 * computes the cost (5) from the ORIGINAL matrix +
 * {@code sorted}, never from the message. Row-reduce
 * highlight = [0,1,2,6,7,8] (rows 0 and 2 change, row 1
 * does not); col-reduce = [0,3,6]; cover = [1,4,6,7,8]
 * (column 1 + row 2); adjust = [0,2,3,5,7] (the four
 * uncovered cells + the double-covered (2,1)); assign and
 * final highlight = [1,3,8] (the assigned cells).
 * {@code nodes} = one NodeDto per matrix cell, row-major:
 * id = r*3+c, value = the cell's CURRENT value, x = column,
 * y = row, label = the value as a string. No edges.</p>
 *
 * <p><b>Design gaps resolved:</b> the design pins one cover
 * step and one adjust step for the demo, so after an adjust
 * the cover is recomputed <i>silently</i> — a second cover
 * step is emitted only if the matrix still needs another
 * adjust (a matrix needing two adjusts traces row, col,
 * cover, adjust, cover, adjust, assign, final). The cover
 * itself is computed from a maximum zero-matching found by
 * Kuhn's DFS (rows ascending, columns ascending) plus the
 * standard marking (unassigned rows, then columns with a
 * zero in a marked row, then rows assigned to a marked
 * column); for the demo that yields column 1 + row 2, as
 * pinned. The final step repeats the assigned cells in
 * {@code highlight} so the assignment stays visible.</p>
 *
 * <p>Bad input (a missing/unparseable matrix, or a
 * non-square one) yields a single "empty" Step; the
 * controller 400s on it (see VisualizationController).</p>
 */
@Service
public class HungarianService {

    private static final int LINE_ROW = 2;
    private static final int LINE_COL = 3;
    private static final int LINE_COVER = 4;
    private static final int LINE_ADJUST = 5;
    private static final int LINE_ASSIGN = 6;
    private static final int LINE_FINAL = 0;

    /**
     * Runs the Hungarian algorithm on the cost matrix in
     * {@code word} ("4,1,3;2,0,5;3,2,2" — rows separated by
     * ';', cells by ',').
     */
    public List<Step> hungarian(String word) {
        List<Step> steps = new ArrayList<>();
        int[][] matrix = parseMatrix(word);
        if (matrix == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The cost matrix is needed as "
                            + "\"a,b,c;d,e,f;...\" rows.", LINE_FINAL));
            return steps;
        }
        int n = matrix.length;
        int[][] current = copy(matrix);

        // Row reduction: subtract each row's minimum.
        List<Integer> rowChanged = new ArrayList<>();
        for (int r = 0; r < n; r++) {
            int min = Integer.MAX_VALUE;
            for (int c = 0; c < n; c++) {
                min = Math.min(min, current[r][c]);
            }
            for (int c = 0; c < n; c++) {
                if (min != 0) {
                    current[r][c] -= min;
                    rowChanged.add(r * n + c);
                }
            }
        }
        steps.add(new Step(
                flatten(current), rowChanged, List.of(), List.of(),
                "Row reduction: subtract each row's minimum.",
                LINE_ROW, snapshot(current), null));

        // Column reduction: subtract each column's minimum.
        List<Integer> colChanged = new ArrayList<>();
        for (int c = 0; c < n; c++) {
            int min = Integer.MAX_VALUE;
            for (int r = 0; r < n; r++) {
                min = Math.min(min, current[r][c]);
            }
            for (int r = 0; r < n; r++) {
                if (min != 0) {
                    current[r][c] -= min;
                    colChanged.add(r * n + c);
                }
            }
        }
        steps.add(new Step(
                flatten(current), colChanged, List.of(), List.of(),
                "Column reduction: subtract each column's minimum.",
                LINE_COL, snapshot(current), null));

        // Cover-adjust loop (the cover is rechecked silently after an adjust).
        List<Integer> assignment = null;
        while (true) {
            Cover cover = minimumCover(current);
            if (cover.lines() == n) {
                assignment = assignment(current);
                break;
            }
            steps.add(new Step(
                    flatten(current), cover.cells(), List.of(),
                    List.of(),
                    "Minimum cover: " + cover.lines()
                            + " lines (" + cover.describe()
                            + ") < " + n + " — adjust.",
                    LINE_COVER, snapshot(current), null));
            int minUncovered = Integer.MAX_VALUE;
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    if (!cover.rows.contains(r)
                            && !cover.cols.contains(c)) {
                        minUncovered = Math.min(minUncovered,
                                current[r][c]);
                    }
                }
            }
            List<Integer> adjustChanged = new ArrayList<>();
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    boolean coveredRow = cover.rows.contains(r);
                    boolean coveredCol = cover.cols.contains(c);
                    if (!coveredRow && !coveredCol) {
                        current[r][c] -= minUncovered;
                        adjustChanged.add(r * n + c);
                    } else if (coveredRow && coveredCol) {
                        current[r][c] += minUncovered;
                        adjustChanged.add(r * n + c);
                    }
                }
            }
            adjustChanged.sort(Integer::compareTo);
            steps.add(new Step(
                    flatten(current), adjustChanged, List.of(),
                    List.of(),
                    "Adjust: minimum uncovered is " + minUncovered
                            + " — subtract it from uncovered cells, "
                            + "add it to double-covered cells.",
                    LINE_ADJUST, snapshot(current), null));
        }

        List<Integer> assignedCells = new ArrayList<>();
        List<Integer> flatAssignment = new ArrayList<>();
        for (int r = 0; r < n; r++) {
            int c = assignment.get(r);
            assignedCells.add(r * n + c);
            flatAssignment.add(r);
            flatAssignment.add(c);
        }
        int cost = 0;
        for (int r = 0; r < n; r++) {
            cost += matrix[r][assignment.get(r)];
        }
        steps.add(new Step(
                flatten(current), assignedCells, List.of(),
                new ArrayList<>(flatAssignment),
                "Assignment: " + assignedCells
                        + " — optimal cost " + cost + ".",
                LINE_ASSIGN, snapshot(current), null));
        steps.add(new Step(
                flatten(current), assignedCells, List.of(),
                new ArrayList<>(flatAssignment),
                "Hungarian algorithm done: optimal assignment "
                        + flatAssignment + ", cost " + cost + ".",
                LINE_FINAL, snapshot(current), null));
        return steps;
    }

    /** Parses "4,1,3;2,0,5;3,2,2" into a square matrix, or null. */
    private int[][] parseMatrix(String word) {
        if (word == null || word.isBlank()) {
            return null;
        }
        String[] rows = word.trim().split(";", -1);
        int n = rows.length;
        int[][] matrix = new int[n][];
        try {
            for (int r = 0; r < n; r++) {
                String[] cells = rows[r].trim().split(",", -1);
                if (cells.length != n) {
                    return null;
                }
                matrix[r] = new int[n];
                for (int c = 0; c < n; c++) {
                    matrix[r][c] = Integer.parseInt(cells[c].trim());
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return matrix;
    }

    /** A minimum line cover of the zeros, via a maximum matching (Kuhn) + standard marking. */
    private Cover minimumCover(int[][] matrix) {
        int n = matrix.length;
        // Maximum matching among the zero cells: col -> row.
        int[] matchCol = new int[n];
        for (int c = 0; c < n; c++) {
            matchCol[c] = -1;
        }
        for (int r = 0; r < n; r++) {
            Set<Integer> seen = new HashSet<>();
            augment(r, matrix, matchCol, seen);
        }
        // Marking: unassigned rows, then columns with a zero in a marked row,
        // then rows assigned to a marked column; repeat until stable.
        Set<Integer> markedRows = new HashSet<>();
        Set<Integer> markedCols = new HashSet<>();
        Set<Integer> assignedRows = new HashSet<>();
        for (int c = 0; c < n; c++) {
            if (matchCol[c] != -1) {
                assignedRows.add(matchCol[c]);
            }
        }
        for (int r = 0; r < n; r++) {
            if (!assignedRows.contains(r)) {
                markedRows.add(r);
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int r : new ArrayList<>(markedRows)) {
                for (int c = 0; c < n; c++) {
                    if (matrix[r][c] == 0 && !markedCols.contains(c)) {
                        markedCols.add(c);
                        changed = true;
                    }
                }
            }
            for (int c : new ArrayList<>(markedCols)) {
                if (matchCol[c] != -1
                        && !markedRows.contains(matchCol[c])) {
                    markedRows.add(matchCol[c]);
                    changed = true;
                }
            }
        }
        // Cover = unmarked rows + marked columns.
        Set<Integer> coverRows = new HashSet<>();
        for (int r = 0; r < n; r++) {
            if (!markedRows.contains(r)) {
                coverRows.add(r);
            }
        }
        return new Cover(coverRows, markedCols, n);
    }

    /** Kuhn's DFS for one row of the zero-matching. */
    private boolean augment(int row, int[][] matrix,
                            int[] matchCol, Set<Integer> seen) {
        int n = matrix.length;
        for (int c = 0; c < n; c++) {
            if (matrix[row][c] != 0 || seen.contains(c)) {
                continue;
            }
            seen.add(c);
            if (matchCol[c] == -1
                    || augment(matchCol[c], matrix, matchCol, seen)) {
                matchCol[c] = row;
                return true;
            }
        }
        return false;
    }

    /** A perfect assignment among the zeros (row -> column); the cover guarantees one exists. */
    private List<Integer> assignment(int[][] matrix) {
        int n = matrix.length;
        int[] matchCol = new int[n];
        for (int c = 0; c < n; c++) {
            matchCol[c] = -1;
        }
        for (int r = 0; r < n; r++) {
            augment(r, matrix, matchCol, new HashSet<>());
        }
        List<Integer> assignment = new ArrayList<>();
        for (int r = 0; r < n; r++) {
            assignment.add(-1);
        }
        for (int c = 0; c < n; c++) {
            if (matchCol[c] != -1) {
                assignment.set(matchCol[c], c);
            }
        }
        return assignment;
    }

    /** A line cover: the covered rows and columns. */
    private static final class Cover {
        final Set<Integer> rows;
        final Set<Integer> cols;
        final int n;

        Cover(Set<Integer> rows, Set<Integer> cols, int n) {
            this.rows = rows;
            this.cols = cols;
            this.n = n;
        }

        int lines() {
            return rows.size() + cols.size();
        }

        /** The covered cells, row-major, as r*n+c ids. */
        List<Integer> cells() {
            List<Integer> cells = new ArrayList<>();
            for (int r : rows) {
                for (int c = 0; c < n; c++) {
                    cells.add(r * n + c);
                }
            }
            for (int c : cols) {
                for (int r = 0; r < n; r++) {
                    int id = r * n + c;
                    if (!cells.contains(id)) {
                        cells.add(id);
                    }
                }
            }
            cells.sort(Integer::compareTo);
            return cells;
        }

        String describe() {
            List<String> parts = new ArrayList<>();
            for (int r : rows) {
                parts.add("row " + r);
            }
            for (int c : cols) {
                parts.add("column " + c);
            }
            parts.sort(String::compareTo);
            return String.join(" + ", parts);
        }
    }

    private int[][] copy(int[][] matrix) {
        int[][] result = new int[matrix.length][];
        for (int r = 0; r < matrix.length; r++) {
            result[r] = matrix[r].clone();
        }
        return result;
    }

    private List<Integer> flatten(int[][] matrix) {
        List<Integer> flat = new ArrayList<>();
        for (int[] row : matrix) {
            for (int cell : row) {
                flat.add(cell);
            }
        }
        return flat;
    }

    /** One NodeDto per matrix cell, row-major: id = r*n+c, value = the current cell value, x = col, y = row. */
    private List<NodeDto> snapshot(int[][] matrix) {
        List<NodeDto> dtos = new ArrayList<>();
        int n = matrix.length;
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                NodeDto dto = new NodeDto(r * n + c, matrix[r][c],
                        c, r);
                dto.setLabel(String.valueOf(matrix[r][c]));
                dtos.add(dto);
            }
        }
        return dtos;
    }
}
