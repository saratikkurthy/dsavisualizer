package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Sudoku solver (Phase 20): backtracking with MRV — at each
 * step the empty cell with the fewest candidates is picked
 * (ties row-major) and digits 1..9 are tried in order. The
 * search stops at the first solution.
 *
 * <p>One Step per placement try (line 2) + one Step per
 * backtrack (line 4) + one final Step (line 0). {@code data}
 * is the flattened 81-cell grid, row-major, 0 = empty;
 * {@code highlight} is the cell just tried or undone
 * ({@code [r*9+c]}); the final step's highlight is empty.</p>
 *
 * <p>Safety cap: if the search exceeds 5000 steps it stops and
 * the final step says so. If the search finishes with no
 * solution inside the cap, the final step says "No solution
 * exists."</p>
 *
 * <p>The controller validates the input (9×9, values 0..9,
 * consistent givens); this method guards against bad input
 * anyway with a single "empty" Step. The search state lives in
 * a per-call {@link Solver}, never in fields — the service is
 * a singleton.</p>
 */
@Service
public class SudokuService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_BACKTRACK = 4;
    private static final int LINE_FINAL = 0;

    /** Default safety cap on search steps (tries + backtracks). */
    private static final int DEFAULT_STEP_CAP = 5000;

    /** Solves {@code grid} (9×9, 0 = empty). */
    public List<Step> sudoku(List<List<Integer>> grid) {
        return sudoku(grid, DEFAULT_STEP_CAP);
    }

    /**
     * Solves {@code grid} with an explicit step cap — public so
     * the tests can use a small cap to exercise the "stopped"
     * path without running a 5000-step search.
     */
    public List<Step> sudoku(List<List<Integer>> grid, int stepCap) {
        List<Step> steps = new ArrayList<>();
        int[][] board = new int[9][9];
        if (grid == null || grid.size() != 9) {
            steps.add(sudokuStep(flatten(board), List.of(),
                    "A 9\u00d79 grid is needed.", LINE_FINAL));
            return steps;
        }
        for (int r = 0; r < 9; r++) {
            List<Integer> row = grid.get(r);
            if (row == null || row.size() != 9) {
                steps.add(sudokuStep(flatten(board), List.of(),
                        "A 9\u00d79 grid is needed.", LINE_FINAL));
                return steps;
            }
            for (int c = 0; c < 9; c++) {
                Integer v = row.get(c);
                if (v == null || v < 0 || v > 9) {
                    steps.add(sudokuStep(flatten(board), List.of(),
                            "Grid values must be 0..9.", LINE_FINAL));
                    return steps;
                }
                board[r][c] = v;
            }
        }
        if (!givensConsistent(board)) {
            steps.add(sudokuStep(flatten(board), List.of(),
                    "The givens are inconsistent (a duplicate in a "
                            + "row, column or box).",
                    LINE_FINAL));
            return steps;
        }
        Solver solver = new Solver(board, stepCap);
        boolean solved = solver.solve();
        if (solver.capped) {
            steps.addAll(solver.steps);
            steps.add(sudokuStep(flatten(board), List.of(),
                    "Stopped after " + stepCap
                            + " steps (no solution found yet).",
                    LINE_FINAL));
        } else if (solved) {
            steps.addAll(solver.steps);
            steps.add(sudokuStep(flatten(board), List.of(),
                    "Solved in " + solver.tries + " tries, "
                            + solver.backtracks + " backtracks.",
                    LINE_FINAL));
        } else {
            steps.addAll(solver.steps);
            steps.add(sudokuStep(flatten(board), List.of(),
                    "No solution exists.", LINE_FINAL));
        }
        return steps;
    }

    /** No duplicate non-zero digit in any row, column or 3×3 box. */
    private boolean givensConsistent(int[][] board) {
        for (int i = 0; i < 9; i++) {
            boolean[] rowSeen = new boolean[10];
            boolean[] colSeen = new boolean[10];
            boolean[] boxSeen = new boolean[10];
            for (int j = 0; j < 9; j++) {
                int rowVal = board[i][j];
                if (rowVal != 0) {
                    if (rowSeen[rowVal]) {
                        return false;
                    }
                    rowSeen[rowVal] = true;
                }
                int colVal = board[j][i];
                if (colVal != 0) {
                    if (colSeen[colVal]) {
                        return false;
                    }
                    colSeen[colVal] = true;
                }
                int boxVal = board[3 * (i / 3) + j / 3]
                        [3 * (i % 3) + j % 3];
                if (boxVal != 0) {
                    if (boxSeen[boxVal]) {
                        return false;
                    }
                    boxSeen[boxVal] = true;
                }
            }
        }
        return true;
    }

    /** Whether digit d fits at (r, c) against row, column, box. */
    static boolean fits(int[][] board, int r, int c, int d) {
        for (int j = 0; j < 9; j++) {
            if (board[r][j] == d || board[j][c] == d) {
                return false;
            }
        }
        int br = 3 * (r / 3);
        int bc = 3 * (c / 3);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[br + i][bc + j] == d) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Candidates for (r, c): digits 1..9 that fit, in order. */
    static List<Integer> candidates(int[][] board, int r, int c) {
        List<Integer> out = new ArrayList<>();
        for (int d = 1; d <= 9; d++) {
            if (fits(board, r, c, d)) {
                out.add(d);
            }
        }
        return out;
    }

    /** Flattened 81-cell grid, row-major. */
    private List<Integer> flatten(int[][] board) {
        List<Integer> out = new ArrayList<>(81);
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                out.add(board[r][c]);
            }
        }
        return out;
    }

    /** One Sudoku Step: flattened grid, one highlighted cell. */
    private Step sudokuStep(List<Integer> data, List<Integer> highlight,
                            String message, int line) {
        return new Step(new ArrayList<>(data), highlight, List.of(),
                List.of(), message, line);
    }

    /** Per-call search state (the service itself is a singleton). */
    private class Solver {
        final int[][] board;
        final int stepCap;
        final List<Step> steps = new ArrayList<>();
        int tries = 0;
        int backtracks = 0;
        boolean capped = false;

        Solver(int[][] board, int stepCap) {
            this.board = board;
            this.stepCap = stepCap;
        }

        /** True when the board has no empty cell. */
        boolean isFull() {
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (board[r][c] == 0) {
                        return false;
                    }
                }
            }
            return true;
        }

        /** MRV cell: fewest candidates, ties row-major. Null when full. */
        int[] mrvCell() {
            int[] best = null;
            List<Integer> bestCands = null;
            outer:
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (board[r][c] != 0) {
                        continue;
                    }
                    List<Integer> cands = candidates(board, r, c);
                    if (bestCands == null
                            || cands.size() < bestCands.size()) {
                        best = new int[]{r, c};
                        bestCands = cands;
                        if (cands.size() == 1) {
                            break outer;
                        }
                    }
                }
            }
            return best;
        }

        boolean solve() {
            if (capped) {
                return false;
            }
            int[] cell = mrvCell();
            if (cell == null) {
                return true;
            }
            int r = cell[0];
            int c = cell[1];
            List<Integer> cands = candidates(board, r, c);
            for (int d : cands) {
                board[r][c] = d;
                tries++;
                steps.add(sudokuStep(flatten(board),
                        List.of(r * 9 + c),
                        "Try (" + r + "," + c + ") = " + d + " ("
                                + cands.size() + " candidates).",
                        LINE_COMPARE));
                // A solved board is not capped even if this try
                // lands exactly on the cap.
                if (!isFull() && steps.size() >= stepCap) {
                    capped = true;
                    return false;
                }
                if (solve()) {
                    return true;
                }
                if (capped) {
                    return false;
                }
                board[r][c] = 0;
                backtracks++;
                steps.add(sudokuStep(flatten(board),
                        List.of(r * 9 + c),
                        "Backtrack (" + r + "," + c + ") \u2014 remove "
                                + d + ".",
                        LINE_BACKTRACK));
                if (steps.size() >= stepCap) {
                    capped = true;
                    return false;
                }
            }
            return false;
        }
    }
}
