package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Matrix multiplication (Phase 28, matrices): C =
 * A x B, where C[i][j] is the dot product of row i
 * of A and column j of B — C[i][j] =
 * sum_k A[i][k] * B[k][j]. A must have as many
 * columns as B has rows (the inner dimension); C is
 * (A rows) x (B columns). The naive triple loop is
 * O(n^3); the point of the trace is to show which
 * row and column combine for each cell, and the
 * three products that add up to it.
 *
 * <p>Demo A(2x3) [[1,2,3],[4,5,6]] x B(3x2)
 * [[1,2],[3,4],[5,6]] = C [[22,28],[49,64]]:
 * C[0][0] = 1*1+2*3+3*5 = 22, C[0][1] =
 * 1*2+2*4+3*6 = 28, C[1][0] = 4*1+5*3+6*5 = 49,
 * C[1][1] = 4*2+5*4+6*6 = 64. Trace: 4 cell steps
 * (line 2, row-major) + final (line 0) = 5 steps.
 * {@code nodes} = three grids in disjoint id
 * spaces: A ids 0..5 (r*3+c), B ids 10..15
 * (r*2+c), C ids 20..23 (r*2+c). {@code value} =
 * cell value (C cells are 0 until computed, then
 * the result); {@code label} = "A r,c" / "B r,c" /
 * "C r,c". No {@code edges} (null). Cell steps:
 * {@code highlight} = [the 3 A cells of row i, the
 * 3 B cells of column j, the C cell] (7 ids);
 * {@code data} = [C[i][j], A[i][0]*B[0][j],
 * A[i][1]*B[1][j], A[i][2]*B[2][j]] — e.g. the
 * C[0][0] step has data [22, 1, 6, 15]. The
 * visualizer shows the three products and their sum
 * from data. Final step: {@code data} = [22, 28,
 * 49, 64] (C row-major), {@code highlight} = [].</p>
 *
 * <p>Bad input (an unparseable A or B, or A cols !=
 * B rows) yields a single "empty" Step; the
 * controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class MatrixMultService {

    private static final int LINE_CELL = 2;
    private static final int LINE_FINAL = 0;

    /** Multiplies A ({@code word}) by B ({@code words.get(0)}), one step per C cell. */
    public List<Step> matrixMult(String word, List<String> words) {
        List<Step> steps = new ArrayList<>();
        int[][] a = parseMatrix(word);
        int[][] b = words == null || words.isEmpty()
                ? null : parseMatrix(words.get(0));
        if (a == null || b == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Two matrices are needed as "
                            + "\"a,b,c;d,e,f\".", LINE_FINAL));
            return steps;
        }
        if (a[0].length != b.length) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Matrix multiplication needs A's "
                            + "columns to equal B's rows.", LINE_FINAL));
            return steps;
        }
        int aRows = a.length;
        int aCols = a[0].length;
        int bCols = b[0].length;
        int[][] c = new int[aRows][bCols];
        for (int i = 0; i < aRows; i++) {
            for (int j = 0; j < bCols; j++) {
                List<Integer> data = new ArrayList<>();
                int sum = 0;
                for (int k = 0; k < aCols; k++) {
                    int product = a[i][k] * b[k][j];
                    sum += product;
                    data.add(product);
                }
                c[i][j] = sum;
                data.add(0, sum);
                List<Integer> highlight = new ArrayList<>();
                for (int k = 0; k < aCols; k++) {
                    highlight.add(i * aCols + k);
                }
                for (int k = 0; k < aCols; k++) {
                    highlight.add(10 + k * bCols + j);
                }
                highlight.add(20 + i * bCols + j);
                StringBuilder terms = new StringBuilder();
                for (int k = 0; k < aCols; k++) {
                    if (k > 0) {
                        terms.append("+");
                    }
                    terms.append(a[i][k]).append("*").append(b[k][j]);
                }
                steps.add(new Step(data, highlight, List.of(),
                        List.of(),
                        "C[" + i + "][" + j + "] = " + terms + " = "
                                + sum + ".",
                        LINE_CELL, snapshot(a, b, c), null));
            }
        }
        List<Integer> finalData = new ArrayList<>();
        for (int i = 0; i < aRows; i++) {
            for (int j = 0; j < bCols; j++) {
                finalData.add(c[i][j]);
            }
        }
        steps.add(new Step(finalData, List.of(), List.of(), List.of(),
                "Matrix multiplication done: C = " + finalData + ".",
                LINE_FINAL, snapshot(a, b, c), null));
        return steps;
    }

    /** One NodeDto per cell of A, B and C (disjoint id spaces 0+, 10+, 20+). */
    private static List<NodeDto> snapshot(int[][] a, int[][] b, int[][] c) {
        List<NodeDto> nodes = new ArrayList<>();
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
                NodeDto dto = new NodeDto(i * a[0].length + j, a[i][j], 0, 0);
                dto.setLabel("A " + i + "," + j);
                nodes.add(dto);
            }
        }
        for (int i = 0; i < b.length; i++) {
            for (int j = 0; j < b[0].length; j++) {
                NodeDto dto = new NodeDto(10 + i * b[0].length + j,
                        b[i][j], 0, 0);
                dto.setLabel("B " + i + "," + j);
                nodes.add(dto);
            }
        }
        for (int i = 0; i < c.length; i++) {
            for (int j = 0; j < c[0].length; j++) {
                NodeDto dto = new NodeDto(20 + i * c[0].length + j,
                        c[i][j], 0, 0);
                dto.setLabel("C " + i + "," + j);
                nodes.add(dto);
            }
        }
        return nodes;
    }

    /** Parses "1,2,3;4,5,6" into an int matrix (2..6 x 2..6), or null. */
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
