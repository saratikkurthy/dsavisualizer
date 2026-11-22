package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * N-Queens backtracking (Phase 13): rows in order, columns
 * ascending. {@code table} is the board snapshot (1 = queen,
 * 0 = empty); {@code highlight} is [row, col] of the placement
 * just attempted. The trace runs until the FIRST solution; the
 * service also counts ALL solutions (cheap for n &le; 8) and the
 * final message reports it: "Solution 1 of 2 for n = 4."
 * For n = 2 or 3 there is no solution at all, so the trace is a
 * single step: "No solution exists for n = 2."
 * A placement is line 3, the summary line 0.
 */
@Service
public class BacktrackingService {

    private static final int LINE_PLACE = 3;
    private static final int LINE_FINAL = 0;

    /**
     * First N-Queens solution for board size {@code n}, plus the
     * total solution count. The controller validates n (1..8);
     * this method guards against bad input anyway with a single
     * "empty" Step.
     */
    public List<Step> nQueens(int n) {
        List<Step> steps = new ArrayList<>();
        if (n < 1 || n > 8) {
            steps.add(boardStep(emptyBoard(n), List.of(),
                    "Board size n must be between 1 and 8.",
                    LINE_FINAL));
            return steps;
        }
        int[] cols = new int[n];
        Arrays.fill(cols, -1);
        boolean[] usedCol = new boolean[n];
        boolean[] usedDiag1 = new boolean[2 * n]; // row + col
        boolean[] usedDiag2 = new boolean[2 * n]; // row - col + n
        boolean found = place(0, n, cols, usedCol, usedDiag1,
                usedDiag2, steps);
        if (!found) {
            // n = 2 or 3: no solution exists. The trace is a
            // single step — the dead-end placements are not shown.
            return List.of(boardStep(emptyBoard(n), List.of(),
                    "No solution exists for n = " + n + ".",
                    LINE_FINAL));
        }
        int total = countAll(n);
        steps.add(boardStep(boardFrom(cols),
                List.of(n - 1, cols[n - 1]),
                "Solution 1 of " + total + " for n = " + n + ".",
                LINE_FINAL));
        return steps;
    }

    /**
     * Traced search for the first solution: place queens row by
     * row, columns ascending, emitting one Step per placement.
     * Returns true once the first full board is found.
     */
    private boolean place(int row, int n, int[] cols,
                          boolean[] usedCol, boolean[] usedDiag1,
                          boolean[] usedDiag2, List<Step> steps) {
        for (int col = 0; col < n; col++) {
            if (usedCol[col] || usedDiag1[row + col]
                    || usedDiag2[row - col + n]) {
                continue; // attacked — try the next column
            }
            cols[row] = col;
            usedCol[col] = true;
            usedDiag1[row + col] = true;
            usedDiag2[row - col + n] = true;
            steps.add(boardStep(boardFrom(cols), List.of(row, col),
                    "Place queen at (" + row + ", " + col + ").",
                    LINE_PLACE));
            if (row == n - 1) {
                return true; // first solution found
            }
            if (place(row + 1, n, cols, usedCol, usedDiag1,
                    usedDiag2, steps)) {
                return true;
            }
            // Backtrack: lift the queen and try the next column.
            // No Step is emitted for the removal — the next
            // placement's board snapshot shows it gone.
            cols[row] = -1;
            usedCol[col] = false;
            usedDiag1[row + col] = false;
            usedDiag2[row - col + n] = false;
        }
        return false;
    }

    /** Silent count of ALL solutions for board size {@code n}. */
    private int countAll(int n) {
        boolean[] usedCol = new boolean[n];
        boolean[] usedDiag1 = new boolean[2 * n];
        boolean[] usedDiag2 = new boolean[2 * n];
        return countFrom(0, n, usedCol, usedDiag1, usedDiag2);
    }

    private int countFrom(int row, int n, boolean[] usedCol,
                          boolean[] usedDiag1, boolean[] usedDiag2) {
        if (row == n) {
            return 1;
        }
        int count = 0;
        for (int col = 0; col < n; col++) {
            if (usedCol[col] || usedDiag1[row + col]
                    || usedDiag2[row - col + n]) {
                continue;
            }
            usedCol[col] = true;
            usedDiag1[row + col] = true;
            usedDiag2[row - col + n] = true;
            count += countFrom(row + 1, n, usedCol, usedDiag1,
                    usedDiag2);
            usedCol[col] = false;
            usedDiag1[row + col] = false;
            usedDiag2[row - col + n] = false;
        }
        return count;
    }

    /** Board snapshot for the placed queens (1 = queen). */
    private List<List<Integer>> boardFrom(int[] cols) {
        int n = cols.length;
        List<List<Integer>> board = new ArrayList<>();
        for (int row = 0; row < n; row++) {
            List<Integer> boardRow = new ArrayList<>();
            for (int col = 0; col < n; col++) {
                boardRow.add(cols[row] == col ? 1 : 0);
            }
            board.add(boardRow);
        }
        return board;
    }

    /** An empty n&times;n board (all 0). */
    private List<List<Integer>> emptyBoard(int n) {
        List<List<Integer>> board = new ArrayList<>();
        for (int row = 0; row < n; row++) {
            List<Integer> boardRow = new ArrayList<>();
            for (int col = 0; col < n; col++) {
                boardRow.add(0);
            }
            board.add(boardRow);
        }
        return board;
    }

    /** One N-Queens Step: the board snapshot in {@code table}. */
    private Step boardStep(List<List<Integer>> board,
                           List<Integer> highlight, String message,
                           int line) {
        Step step = new Step(List.of(), highlight, List.of(),
                List.of(), message, line);
        step.setTable(board);
        return step;
    }
}
