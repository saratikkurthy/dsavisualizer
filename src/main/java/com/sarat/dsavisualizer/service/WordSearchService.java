package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Word search (Phase 29, matrices v2): depth-first search
 * for a word on a letter board, trying each cell as a
 * start (row-major) and, from a matching cell, the
 * neighbours in the order up, right, down, left — never
 * revisiting a cell on the current path. A cell whose
 * letter does not match the word at that position is a
 * dead end: the search backtracks (no step) and tries the
 * next neighbour.
 *
 * <p>Demo board [["A","B","C","E"],["S","F","C","S"],
 * ["A","D","E","E"]], word "ABCCED". Path (0,0),(0,1),
 * (0,2),(1,2),(2,2),(2,1); dead ends (0,3), (1,3), (2,3).
 * Trace: 1 build (line 2) + 9 examinations (line 3) +
 * final (line 0) = 11 steps. {@code nodes} = one NodeDto
 * per cell: id = r*cols+c, {@code value} = the letter's
 * char code (65 = 'A'), {@code label} = the single letter.
 * No {@code edges} (null). Build step: {@code data} = [0]
 * (matched length). Examination steps: {@code data} = [k,
 * matched 1/0] (k = the word index being matched),
 * {@code highlight} = [the examined cell], {@code sorted}
 * = the path so far as cell ids — a no-match cell is NOT
 * added to it. Final step: {@code data} = [word length]
 * when found ([0] when not found), {@code sorted} = the
 * full path (or []).</p>
 *
 * <p>Bad input (an unparseable board or a missing/invalid
 * word) yields a single "empty" Step; the controller 400s
 * on it (see VisualizationController).</p>
 */
@Service
public class WordSearchService {

    private static final int LINE_BUILD = 2;
    private static final int LINE_EXAMINE = 3;
    private static final int LINE_FINAL = 0;

    /** Depth-first search for {@code words.get(0)} on the board in {@code word}. */
    public List<Step> wordSearch(String word, List<String> words) {
        List<Step> steps = new ArrayList<>();
        char[][] board = parseBoard(word);
        String target = words == null || words.size() != 1
                ? null : words.get(0);
        if (board == null || target == null || target.isEmpty()
                || target.length() > 12 || !isUpperWord(target)) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Word search needs a board of "
                            + "letters as \"A,B,C;D,E,F\" and a word "
                            + "of 1..12 uppercase letters A-Z.",
                    LINE_FINAL));
            return steps;
        }
        int rows = board.length;
        int cols = board[0].length;
        steps.add(new Step(List.of(0), List.of(), List.of(), List.of(),
                "Search for \"" + target + "\" in the " + rows + "x"
                        + cols + " board.",
                LINE_BUILD, snapshot(board), null));
        boolean[][] visited = new boolean[rows][cols];
        List<Integer> path = new ArrayList<>();
        boolean found = false;
        outer:
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c] == target.charAt(0)
                        && dfs(board, target, r, c, 0, visited, path,
                                steps)) {
                    found = true;
                    break outer;
                }
            }
        }
        steps.add(new Step(List.of(path.size()), List.of(), List.of(),
                new ArrayList<>(path),
                found ? "Found \"" + target + "\" — path "
                        + pathText(path, cols) + "."
                        : "\"" + target + "\" is not in the board.",
                LINE_FINAL, snapshot(board), null));
        return steps;
    }

    /**
     * Examine cell (r, c) for target[k]: one Step, then recurse
     * into the neighbours (up, right, down, left) while the
     * letter matches. Returns true once the whole word is
     * matched. A failed branch backtracks silently (no Step).
     */
    private boolean dfs(char[][] board, String target, int r, int c,
                        int k, boolean[][] visited, List<Integer> path,
                        List<Step> steps) {
        int rows = board.length;
        int cols = board[0].length;
        int id = r * cols + c;
        boolean match = board[r][c] == target.charAt(k);
        if (match) {
            visited[r][c] = true;
            path.add(id);
        }
        steps.add(new Step(List.of(k, match ? 1 : 0), List.of(id),
                List.of(), new ArrayList<>(path),
                "Match \"" + target.charAt(k) + "\" at (" + r + ","
                        + c + "): "
                        + (match ? "yes — " + (k + 1) + " of "
                                + target.length() + " letters."
                                : "no, '" + board[r][c]
                                        + "' — dead end."),
                LINE_EXAMINE, snapshot(board), null));
        if (!match) {
            return false;
        }
        if (k == target.length() - 1) {
            return true;
        }
        // Neighbours: up, right, down, left.
        int[][] dirs = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};
        for (int[] dir : dirs) {
            int nr = r + dir[0];
            int nc = c + dir[1];
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols
                    || visited[nr][nc]) {
                continue;
            }
            if (dfs(board, target, nr, nc, k + 1, visited, path,
                    steps)) {
                return true;
            }
        }
        visited[r][c] = false;
        path.remove(path.size() - 1);
        return false;
    }

    /** "(0,0) -> (0,1) -> ..." for a path of cell ids. */
    private String pathText(List<Integer> path, int cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                sb.append(" -> ");
            }
            int id = path.get(i);
            sb.append("(").append(id / cols).append(",")
                    .append(id % cols).append(")");
        }
        return sb.toString();
    }

    /** True when every char is an uppercase letter A-Z. */
    private boolean isUpperWord(String target) {
        for (int i = 0; i < target.length(); i++) {
            char ch = target.charAt(i);
            if (ch < 'A' || ch > 'Z') {
                return false;
            }
        }
        return true;
    }

    /** One NodeDto per cell: id = r*cols+c, value = char code. */
    private static List<NodeDto> snapshot(char[][] board) {
        int rows = board.length;
        int cols = board[0].length;
        List<NodeDto> nodes = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                NodeDto dto = new NodeDto(r * cols + c, board[r][c], 0, 0);
                dto.setLabel(String.valueOf(board[r][c]));
                nodes.add(dto);
            }
        }
        return nodes;
    }

    /** Parses "A,B,C;D,E,F" into a char board (2..6 x 2..6), or null. */
    private char[][] parseBoard(String word) {
        if (word == null || word.isBlank()) {
            return null;
        }
        String[] rows = word.trim().split(";", -1);
        if (rows.length < 2 || rows.length > 6) {
            return null;
        }
        char[][] result = null;
        for (int i = 0; i < rows.length; i++) {
            String[] cells = rows[i].trim().split(",", -1);
            if (result == null) {
                if (cells.length < 2 || cells.length > 6) {
                    return null;
                }
                result = new char[rows.length][cells.length];
            } else if (cells.length != result[0].length) {
                return null;
            }
            for (int j = 0; j < cells.length; j++) {
                String cell = cells[j].trim();
                if (cell.length() != 1 || cell.charAt(0) < 'A'
                        || cell.charAt(0) > 'Z') {
                    return null;
                }
                result[i][j] = cell.charAt(0);
            }
        }
        return result;
    }
}
