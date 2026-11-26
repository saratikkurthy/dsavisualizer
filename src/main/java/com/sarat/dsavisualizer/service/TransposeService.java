package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Transpose a matrix (Phase 31, Matrices v3): the (r, c)
 * entry of the source becomes the (c, r) entry of the
 * target. Non-square input is the point — a 2x3 source
 * becomes a 3x2 target.
 *
 * <p>Matrix conventions from Search2dService: {@code nodes}
 * = one NodeDto per cell (id = r*cols+c, {@code value} =
 * the cell value, {@code label} = "r,c"), no {@code edges}
 * (null); LINE_BUILD = 2, LINE_STEP = 3, LINE_FINAL = 0.
 * Step shape: build (line 2, {@code data} = [rows, cols]
 * of the source, {@code nodes} = the TARGET cells with
 * value 0 for not-yet-copied cells) + one copy step per
 * source cell in row-major order (line 3, {@code highlight}
 * = [target cell just written], {@code sorted} = [target
 * cells written so far, in copy order], {@code data} =
 * [copied count]) + final (line 0, {@code data} = [new
 * rows, new cols], {@code sorted} = all target cell ids in
 * id order). The visualizer renders the target grid,
 * blanking unwritten cells (value 0 and not in
 * {@code sorted}) — it must NOT render the 0 as a value.</p>
 *
 * <p>Demo matrix "1,2,3;4,5,6" (2x3) -&gt; [[1,4],[2,5],
 * [3,6]] (3x2). Copy order row-major over the source:
 * (0,0)=1, (0,1)=2, (0,2)=3, (1,0)=4, (1,1)=5, (1,2)=6.
 * Trace: 1 build + 6 copies + 1 final = 8 steps.</p>
 *
 * <p>The controller validates the input (1..6 rows and
 * 1..6 columns, values -99..99); this method guards
 * against bad input anyway with a single "empty" Step.</p>
 */
@Service
public class TransposeService {

    private static final int LINE_BUILD = 2;
    private static final int LINE_STEP = 3;
    private static final int LINE_FINAL = 0;

    /** Transposes the matrix in {@code word} ("a,b,c;d,e,f"). */
    public List<Step> transpose(String word) {
        List<Step> steps = new ArrayList<>();
        int[][] matrix = parseMatrix(word);
        if (matrix == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Transpose needs a matrix as "
                            + "\"a,b,c;d,e,f\" (1..6 rows and columns).",
                    LINE_FINAL));
            return steps;
        }
        int rows = matrix.length;
        int cols = matrix[0].length;
        int newRows = cols;
        int newCols = rows;
        int[][] target = new int[newRows][newCols];
        List<Integer> written = new ArrayList<>();
        steps.add(new Step(List.of(rows, cols), List.of(), List.of(),
                List.of(),
                "Transpose the " + rows + "x" + cols + " matrix into a "
                        + newRows + "x" + newCols + " matrix.",
                LINE_BUILD, snapshot(target), null));
        int copied = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int value = matrix[r][c];
                target[c][r] = value;
                copied++;
                int targetId = c * newCols + r;
                written.add(targetId);
                steps.add(new Step(List.of(copied), List.of(targetId),
                        List.of(), new ArrayList<>(written),
                        "Copy (" + r + "," + c + ") = " + value + " to ("
                                + c + "," + r + ").",
                        LINE_STEP, snapshot(target), null));
            }
        }
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < newRows * newCols; i++) {
            all.add(i);
        }
        steps.add(new Step(List.of(newRows, newCols), List.of(),
                List.of(), all,
                "Transposed: " + newRows + "x" + newCols + " matrix.",
                LINE_FINAL, snapshot(target), null));
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
