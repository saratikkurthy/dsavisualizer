package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Search a 2D matrix (Phase 29, matrices v2): the matrix is
 * sorted — each row ascending and each row's first element
 * greater than the previous row's last — so it reads as one
 * sorted array of length rows*cols, and a plain binary search
 * over the flattened index finds the target (or proves it
 * absent). mid maps back to (mid / cols, mid % cols).
 *
 * <p>Demo 3x4 [[1,3,5,7],[10,11,16,20],[23,30,34,60]],
 * target 3. Probes (lo, hi, mid, value): (0,11,5,11),
 * (0,4,2,5), (0,1,0,1), (1,1,1,3) — found at (0,1).
 * Trace: 1 build (line 2) + 4 probes (line 3) + final
 * (line 0) = 6 steps. {@code nodes} = one NodeDto per cell:
 * id = r*cols+c, {@code value} = cell value, {@code label}
 * = "r,c". No {@code edges} (null). Build step: {@code data}
 * = [0, rows*cols - 1] ([lo, hi]). Probe steps: {@code data}
 * = [lo, hi, mid, value], {@code highlight} = [mid cell id],
 * {@code sorted} = [found cell id] only on the found probe.
 * Final step: {@code data} = the found probe repeated (or
 * the last probe when the target is absent), {@code sorted}
 * = [found cell id] or [].</p>
 *
 * <p>Bad input (an unparseable matrix or a missing target)
 * yields a single "empty" Step; the controller 400s on it
 * (see VisualizationController).</p>
 */
@Service
public class Search2dService {

    private static final int LINE_BUILD = 2;
    private static final int LINE_PROBE = 3;
    private static final int LINE_FINAL = 0;

    /** Binary search for {@code values.get(0)} in the matrix in {@code word}. */
    public List<Step> search2d(String word, List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        int[][] matrix = parseMatrix(word);
        if (matrix == null || values == null || values.size() != 1
                || values.get(0) == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Search needs a sorted matrix as "
                            + "\"a,b,c;d,e,f\" and a target value.",
                    LINE_FINAL));
            return steps;
        }
        int rows = matrix.length;
        int cols = matrix[0].length;
        int target = values.get(0);
        int total = rows * cols;
        steps.add(new Step(List.of(0, total - 1), List.of(), List.of(),
                List.of(),
                "Binary search for " + target + " in the " + rows + "x"
                        + cols + " matrix (flattened 0.."
                        + (total - 1) + ").",
                LINE_BUILD, snapshot(matrix), null));
        int lo = 0;
        int hi = total - 1;
        int foundId = -1;
        List<Integer> lastProbe = List.of(lo, hi, 0, matrix[0][0]);
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            int r = mid / cols;
            int c = mid % cols;
            int value = matrix[r][c];
            lastProbe = List.of(lo, hi, mid, value);
            if (value == target) {
                foundId = mid;
                steps.add(new Step(lastProbe, List.of(mid), List.of(),
                        List.of(mid),
                        "Probe mid " + mid + " = " + value + " at ("
                                + r + "," + c + "): found " + target
                                + "!",
                        LINE_PROBE, snapshot(matrix), null));
                break;
            }
            steps.add(new Step(lastProbe, List.of(mid), List.of(),
                    List.of(),
                    "Probe mid " + mid + " = " + value + " at ("
                            + r + "," + c + "): " + value
                            + (value < target ? " < " : " > ") + target
                            + ", search the "
                            + (value < target ? "right" : "left")
                            + " half.",
                    LINE_PROBE, snapshot(matrix), null));
            if (value < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        if (foundId >= 0) {
            steps.add(new Step(lastProbe, List.of(), List.of(),
                    List.of(foundId),
                    "Found " + target + " at (" + (foundId / cols)
                            + "," + (foundId % cols) + ").",
                    LINE_FINAL, snapshot(matrix), null));
        } else {
            steps.add(new Step(lastProbe, List.of(), List.of(),
                    List.of(),
                    target + " is not in the matrix.",
                    LINE_FINAL, snapshot(matrix), null));
        }
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
