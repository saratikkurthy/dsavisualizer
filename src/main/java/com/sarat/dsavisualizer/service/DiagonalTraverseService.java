package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagonal traverse (Phase 31, Matrices v3): LeetCode 498 —
 * read the matrix diagonal by diagonal, alternating
 * up-right and down-left, bouncing off the edges.
 *
 * <p>Matrix conventions from Search2dService: {@code nodes}
 * = one NodeDto per cell (id = r*cols+c, {@code value} =
 * the cell value, {@code label} = "r,c"), no {@code edges}
 * (null); LINE_BUILD = 2, LINE_STEP = 3, LINE_FINAL = 0.
 * Step shape: build (line 2, {@code data} = [0, 0]) + one
 * visit step per cell (line 3, {@code highlight} =
 * [current cell id], {@code sorted} = [visited cell ids in
 * order], {@code data} = [k, value] where k is the
 * 0-based visit index) + final (line 0, {@code data} =
 * [total cells], {@code sorted} = [visited cell ids in
 * order]). Direction rule (the sim's): going up-right, at
 * the last column go down, at the first row go right, else
 * step up-right; going down-left, at the last row go
 * right, at the first column go down, else step
 * down-left.</p>
 *
 * <p>Demo matrix "1,2,3;4,5,6;7,8,9" (3x3). Traversal
 * order: [1, 2, 4, 7, 5, 3, 6, 8, 9]. Trace: 1 build + 9
 * visits + 1 final = 11 steps.</p>
 *
 * <p>The controller validates the input (1..6 rows and
 * 1..6 columns, values -99..99); this method guards
 * against bad input anyway with a single "empty" Step.</p>
 */
@Service
public class DiagonalTraverseService {

    private static final int LINE_BUILD = 2;
    private static final int LINE_STEP = 3;
    private static final int LINE_FINAL = 0;

    /** Diagonal traversal of the matrix in {@code word}. */
    public List<Step> diagonalTraverse(String word) {
        List<Step> steps = new ArrayList<>();
        int[][] matrix = parseMatrix(word);
        if (matrix == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Diagonal traverse needs a matrix as "
                            + "\"a,b,c;d,e,f\" (1..6 rows and columns).",
                    LINE_FINAL));
            return steps;
        }
        int rows = matrix.length;
        int cols = matrix[0].length;
        int total = rows * cols;
        steps.add(new Step(List.of(0, 0), List.of(), List.of(),
                List.of(),
                "Traverse the " + rows + "x" + cols
                        + " matrix diagonally, alternating up-right "
                        + "and down-left.",
                LINE_BUILD, snapshot(matrix), null));
        List<Integer> visited = new ArrayList<>();
        int r = 0;
        int c = 0;
        boolean up = true;
        for (int k = 0; k < total; k++) {
            int id = r * cols + c;
            int value = matrix[r][c];
            visited.add(id);
            steps.add(new Step(List.of(k, value), List.of(id),
                    List.of(), new ArrayList<>(visited),
                    "Visit (" + r + "," + c + ") = " + value + " ("
                            + (k + 1) + " of " + total + ").",
                    LINE_STEP, snapshot(matrix), null));
            if (up) {
                if (c == cols - 1) {
                    r++;
                    up = false;
                } else if (r == 0) {
                    c++;
                    up = false;
                } else {
                    r--;
                    c++;
                }
            } else {
                if (r == rows - 1) {
                    c++;
                    up = true;
                } else if (c == 0) {
                    r++;
                    up = true;
                } else {
                    r++;
                    c--;
                }
            }
        }
        steps.add(new Step(List.of(total), List.of(), List.of(),
                new ArrayList<>(visited),
                "Diagonal traversal done: " + total + " cells.",
                LINE_FINAL, snapshot(matrix), null));
        return steps;
    }

    /** One NodeDto per cell: id = r*cols+c, value = cell value. */
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

    /** Parses "1,2,3;4,5,6" into an int matrix (1..6 x 1..6, -99..99). */
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
                    if (v < -99 || v > 99) {
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
