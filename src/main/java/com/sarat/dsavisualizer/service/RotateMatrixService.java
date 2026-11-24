package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Rotate matrix 90 degrees clockwise (Phase 28,
 * matrices): rotate in place, layer by layer. Each
 * layer is a ring of 4-cycles — (top,left) goes to
 * (top,right), (top,right) to (bottom,right),
 * (bottom,right) to (bottom,left), (bottom,left) to
 * (top,left) — so each cycle is four assignments
 * through one temporary value, and the whole
 * rotation is n^2/4 cycles. A square matrix is
 * required; rotating a rectangle in place is a
 * different problem (it needs a new matrix).
 *
 * <p>Demo 4x4 [[1..4],[5..8],[9..12],[13..16]].
 * Layer 4-cycles in order: [(0,0),(0,3),(3,3),
 * (3,0)], [(0,1),(1,3),(3,2),(2,0)], [(0,2),(2,3),
 * (3,1),(1,0)], [(1,1),(1,2),(2,2),(2,1)]. Result
 * [[13,9,5,1],[14,10,6,2],[15,11,7,3],[16,12,8,4]].
 * Trace: 4 cycle steps (line 2) + final (line 0) =
 * 5 steps. {@code nodes} = one NodeDto per cell: id
 * = r*n+c (0..15), {@code value} = CURRENT cell
 * value (after this step), {@code label} = "r,c". No
 * {@code edges} (null). Cycle steps: {@code
 * highlight} = the 4 cells of the cycle IN ROTATION
 * ORDER (as ids); {@code data} = [cycle index 0..3].
 * The visualizer draws arrows highlight[0]-&gt;
 * highlight[1]-&gt;highlight[2]-&gt;highlight[3]-&gt;
 * highlight[0] and reads the new values from the
 * nodes. Final step: {@code data} = [4] (cycles
 * done), {@code highlight} = [].</p>
 *
 * <p>Bad input (an unparseable or non-square matrix)
 * yields a single "empty" Step; the controller 400s
 * on it (see VisualizationController).</p>
 */
@Service
public class RotateMatrixService {

    private static final int LINE_CYCLE = 2;
    private static final int LINE_FINAL = 0;

    /** Rotates the square matrix in {@code word} 90 degrees clockwise. */
    public List<Step> rotateMatrix(String word) {
        List<Step> steps = new ArrayList<>();
        int[][] matrix = parseMatrix(word);
        if (matrix == null || matrix.length != matrix[0].length) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Rotate needs a square matrix as "
                            + "\"a,b,c;d,e,f\".", LINE_FINAL));
            return steps;
        }
        int n = matrix.length;
        // The 4-cycles, layer by layer, outer to inner.
        List<int[][]> cycles = new ArrayList<>();
        for (int layer = 0; layer < n / 2; layer++) {
            for (int i = layer; i < n - 1 - layer; i++) {
                cycles.add(new int[][]{
                        {layer, i},
                        {i, n - 1 - layer},
                        {n - 1 - layer, n - 1 - i},
                        {n - 1 - i, layer}});
            }
        }
        for (int k = 0; k < cycles.size(); k++) {
            int[][] cycle = cycles.get(k);
            // Each cell takes the previous cell's value
            // (clockwise); snapshot the cycle first.
            int[] values = new int[4];
            for (int t = 0; t < 4; t++) {
                values[t] = matrix[cycle[t][0]][cycle[t][1]];
            }
            List<Integer> highlight = new ArrayList<>();
            for (int t = 0; t < 4; t++) {
                matrix[cycle[t][0]][cycle[t][1]] = values[(t + 3) % 4];
                highlight.add(cycle[t][0] * n + cycle[t][1]);
            }
            steps.add(new Step(List.of(k), highlight, List.of(),
                    List.of(),
                    "Cycle " + (k + 1) + " of " + cycles.size()
                            + ": (" + cycle[0][0] + "," + cycle[0][1]
                            + ") <- (" + cycle[3][0] + "," + cycle[3][1]
                            + "), (" + cycle[1][0] + "," + cycle[1][1]
                            + ") <- (" + cycle[0][0] + "," + cycle[0][1]
                            + "), (" + cycle[2][0] + "," + cycle[2][1]
                            + ") <- (" + cycle[1][0] + "," + cycle[1][1]
                            + "), (" + cycle[3][0] + "," + cycle[3][1]
                            + ") <- (" + cycle[2][0] + "," + cycle[2][1]
                            + ").",
                    LINE_CYCLE, snapshot(matrix), null));
        }
        steps.add(new Step(List.of(cycles.size()), List.of(), List.of(),
                List.of(),
                "Rotate done: " + cycles.size() + " 4-cycles, matrix "
                        + "rotated 90 degrees clockwise.",
                LINE_FINAL, snapshot(matrix), null));
        return steps;
    }

    /** One NodeDto per cell: id = r*n+c, value = current value. */
    private static List<NodeDto> snapshot(int[][] matrix) {
        int n = matrix.length;
        List<NodeDto> nodes = new ArrayList<>();
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                NodeDto dto = new NodeDto(r * n + c, matrix[r][c], 0, 0);
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
