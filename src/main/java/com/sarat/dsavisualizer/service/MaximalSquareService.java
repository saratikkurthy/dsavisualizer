package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Maximal square (Phase 31, Matrices v3): LeetCode 221 —
 * the largest all-1 square in a binary matrix. dp[i][j] =
 * 1 + min(dp[i-1][j], dp[i][j-1], dp[i-1][j-1]) when the
 * cell is 1, else 0 (first row/column: dp = the cell
 * itself).
 *
 * <p>Matrix conventions from Search2dService: {@code nodes}
 * = one NodeDto per cell (id = r*cols+c, {@code value} =
 * the INPUT 0/1 value, {@code label} = "r,c"), no
 * {@code edges} (null); LINE_BUILD = 2, LINE_STEP = 3,
 * LINE_FINAL = 0. The dp value for the current cell rides
 * in {@code data} — {@code nodes.value} stays the input.
 * Step shape: build (line 2, {@code data} = [0, 0]) + one
 * step per cell in row-major order (line 3,
 * {@code highlight} = [current cell id], {@code data} =
 * [i, j, dp[i][j], maxSide so far], {@code sorted} =
 * [cells of the current best square, in id order]) +
 * final (line 0, {@code data} = [maxSide, area],
 * {@code sorted} = [best square cells]). The first square
 * to reach a side wins ties (strict &gt; when tracking
 * the best — the sim's rule).</p>
 *
 * <p>Demo matrix (LeetCode 221 example)
 * "1,0,1,0,0;1,0,1,1,1;1,1,1,1,1;1,0,0,1,0" (4x5). DP
 * table: row 0: [1, 0, 1, 0, 0]; row 1: [1, 0, 1, 1, 1];
 * row 2: [1, 1, 1, 2, 2]; row 3: [1, 0, 0, 1, 0]. Max
 * side 2, area 4, best square top-left (1,2),
 * bottom-right (2,3) — cells [7, 8, 12, 13]. Trace: 1
 * build + 20 cells + 1 final = 22 steps.</p>
 *
 * <p>The controller validates the input (a binary matrix,
 * 1..6 rows and 1..6 columns); this method guards against
 * bad input anyway with a single "empty" Step.</p>
 */
@Service
public class MaximalSquareService {

    private static final int LINE_BUILD = 2;
    private static final int LINE_STEP = 3;
    private static final int LINE_FINAL = 0;

    /** Largest all-1 square in the binary matrix in {@code word}. */
    public List<Step> maximalSquare(String word) {
        List<Step> steps = new ArrayList<>();
        int[][] matrix = parseMatrix(word);
        if (matrix == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Maximal square needs a binary matrix "
                            + "as \"a,b,c;d,e,f\" (1..6 rows and "
                            + "columns, 0s and 1s).",
                    LINE_FINAL));
            return steps;
        }
        int rows = matrix.length;
        int cols = matrix[0].length;
        steps.add(new Step(List.of(0, 0), List.of(), List.of(),
                List.of(),
                "Find the largest all-1 square in the " + rows + "x"
                        + cols + " binary matrix.",
                LINE_BUILD, snapshot(matrix), null));
        int[][] dp = new int[rows][cols];
        int best = 0;
        int bestRow = -1;
        int bestCol = -1;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (matrix[i][j] == 1) {
                    if (i == 0 || j == 0) {
                        dp[i][j] = 1;
                    } else {
                        dp[i][j] = 1 + Math.min(dp[i - 1][j],
                                Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                    }
                    if (dp[i][j] > best) {
                        best = dp[i][j];
                        bestRow = i;
                        bestCol = j;
                    }
                } else {
                    dp[i][j] = 0;
                }
                List<Integer> bestCells = bestSquareCells(
                        bestRow, bestCol, best, cols);
                steps.add(new Step(
                        List.of(i, j, dp[i][j], best),
                        List.of(i * cols + j), List.of(), bestCells,
                        "Cell (" + i + "," + j + ") = " + matrix[i][j]
                                + ": dp = " + dp[i][j]
                                + " (best side " + best + ").",
                        LINE_STEP, snapshot(matrix), null));
            }
        }
        List<Integer> bestCells = bestSquareCells(
                bestRow, bestCol, best, cols);
        steps.add(new Step(List.of(best, best * best), List.of(),
                List.of(), bestCells,
                "Largest square: side " + best + ", area "
                        + (best * best)
                        + (best > 0
                                ? " (top-left (" + (bestRow - best + 1)
                                        + "," + (bestCol - best + 1) + "))."
                                : " (no 1s in the matrix)."),
                LINE_FINAL, snapshot(matrix), null));
        return steps;
    }

    /** Cells of the best square so far, in id order (empty when best = 0). */
    private List<Integer> bestSquareCells(int bottomRow, int bottomCol,
                                          int side, int cols) {
        List<Integer> cells = new ArrayList<>();
        if (side <= 0) {
            return cells;
        }
        for (int r = bottomRow - side + 1; r <= bottomRow; r++) {
            for (int c = bottomCol - side + 1; c <= bottomCol; c++) {
                cells.add(r * cols + c);
            }
        }
        return cells;
    }

    /** One NodeDto per cell: id = r*cols+c, value = input 0/1. */
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

    /** Parses "1,0,1;0,1,0" into a binary matrix (1..6 x 1..6). */
    private int[][] parseMatrix(String word) {
        if (word == null || word.isBlank()) {
            return null;
        }
        String[] rows = word.trim().split(";", -1);
        if (rows.length < 1 || rows.length > 6) {
            return null;
        }
        int[][] result = null;
        for (int i = 0; i < rows.length; i++) {
            String[] cells = rows[i].trim().split(",", -1);
            if (result == null) {
                if (cells.length < 1 || cells.length > 6) {
                    return null;
                }
                result = new int[rows.length][cells.length];
            } else if (cells.length != result[0].length) {
                return null;
            }
            try {
                for (int j = 0; j < cells.length; j++) {
                    int v = Integer.parseInt(cells[j].trim());
                    if (v != 0 && v != 1) {
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
