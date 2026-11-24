package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Spiral order (Phase 28, matrices): read a matrix
 * in a clockwise spiral by walking its boundaries —
 * the top row left to right, the right column top to
 * bottom, the bottom row right to left, the left
 * column bottom to top — then shrink the boundaries
 * and repeat. Each boundary is one step; the loop
 * stops when the boundaries cross. The walk uses
 * explicit index ranges for every boundary (a
 * reversed range for the bottom row and the left
 * column), never slices — a slice with the wrong
 * bounds silently returns nothing and the row
 * vanishes.
 *
 * <p>Demo 3x4 [[1..4],[5..8],[9..12]]. Spiral:
 * [1,2,3,4,8,12,11,10,9,5,6,7]. Boundaries in
 * order: top, right, bottom, left, top. Trace: 5
 * boundary steps (line 2) + final (line 0) = 6
 * steps. {@code nodes} = one NodeDto per cell: id =
 * r*cols+c (0..11), {@code value} = cell value,
 * {@code label} = "r,c". No {@code edges} (null).
 * Boundary steps: {@code highlight} = the boundary
 * cells IN VISIT ORDER; {@code data} = [values
 * collected so far] (grows: [1,2,3,4],
 * [1,2,3,4,8,12], [1,2,3,4,8,12,11,10,9],
 * [1,2,3,4,8,12,11,10,9,5],
 * [1,2,3,4,8,12,11,10,9,5,6,7]). Final step:
 * {@code data} = the full list, {@code highlight} =
 * [].</p>
 *
 * <p>Bad input (an unparseable matrix) yields a
 * single "empty" Step; the controller 400s on it
 * (see VisualizationController).</p>
 */
@Service
public class SpiralOrderService {

    private static final int LINE_BOUNDARY = 2;
    private static final int LINE_FINAL = 0;

    /** Spiral order of the matrix in {@code word}, one step per boundary. */
    public List<Step> spiralOrder(String word) {
        List<Step> steps = new ArrayList<>();
        int[][] matrix = parseMatrix(word);
        if (matrix == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A matrix is needed as "
                            + "\"a,b,c;d,e,f\".", LINE_FINAL));
            return steps;
        }
        int rows = matrix.length;
        int cols = matrix[0].length;
        List<NodeDto> nodes = snapshot(matrix);
        List<Integer> collected = new ArrayList<>();
        String[] names = {"top", "right", "bottom", "left", "top",
                "right", "bottom", "left"};
        int top = 0;
        int bottom = rows - 1;
        int left = 0;
        int right = cols - 1;
        int boundary = 0;
        while (top <= bottom && left <= right) {
            List<Integer> highlight = new ArrayList<>();
            // Top row, left to right.
            for (int c = left; c <= right; c++) {
                highlight.add(top * cols + c);
                collected.add(matrix[top][c]);
            }
            steps.add(boundaryStep(names[boundary++], highlight,
                    collected, nodes));
            top++;
            if (top > bottom) {
                break;
            }
            // Right column, top to bottom.
            highlight = new ArrayList<>();
            for (int r = top; r <= bottom; r++) {
                highlight.add(r * cols + right);
                collected.add(matrix[r][right]);
            }
            steps.add(boundaryStep(names[boundary++], highlight,
                    collected, nodes));
            right--;
            if (left > right) {
                break;
            }
            // Bottom row, right to left (explicit index range).
            highlight = new ArrayList<>();
            for (int c = right; c >= left; c--) {
                highlight.add(bottom * cols + c);
                collected.add(matrix[bottom][c]);
            }
            steps.add(boundaryStep(names[boundary++], highlight,
                    collected, nodes));
            bottom--;
            if (top > bottom) {
                break;
            }
            // Left column, bottom to top (explicit index range).
            highlight = new ArrayList<>();
            for (int r = bottom; r >= top; r--) {
                highlight.add(r * cols + left);
                collected.add(matrix[r][left]);
            }
            steps.add(boundaryStep(names[boundary++], highlight,
                    collected, nodes));
            left++;
        }
        steps.add(new Step(new ArrayList<>(collected), List.of(),
                List.of(), List.of(),
                "Spiral done: " + collected + ".",
                LINE_FINAL, nodes, null));
        return steps;
    }

    private Step boundaryStep(String name, List<Integer> highlight,
                              List<Integer> collected,
                              List<NodeDto> nodes) {
        return new Step(new ArrayList<>(collected), highlight,
                List.of(), List.of(),
                "Boundary '" + name + "': collected " + collected + ".",
                LINE_BOUNDARY, nodes, null);
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
