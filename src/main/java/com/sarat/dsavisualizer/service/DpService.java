package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dynamic-programming problems beyond 0/1 Knapsack
 * (KnapsackService): longest common subsequence, minimum coin
 * change (Phase 11), edit distance and longest palindromic
 * subsequence (Phase 12), longest increasing subsequence and
 * matrix chain multiplication (Phase 15). All reuse the Step fields the knapsack
 * already introduced — {@code table} for the DP tables,
 * {@code data} for the coin-change array — so no new Step field
 * is needed.
 *
 * LCS: the table has rows 0..m (s1) and columns 0..n (s2); row 0
 * and column 0 are the base case (0) and are pre-filled, so the
 * trace starts at (1, 1). Uncomputed cells are JSON null; the
 * frontend renders them empty/grey. {@code highlight} is
 * [row, col]. A matching pair of characters is line 3, a
 * max-of-up-or-left cell is line 2. The final Step traces back
 * from (m, n) to recover one LCS. Tie-breaking: when the
 * characters differ and dp[i-1][j] &gt;= dp[i][j-1], move up
 * (skip s1's character); otherwise move left.
 *
 * Coin change: dp[0..amount], dp[0] = 0, unreachable amounts are
 * JSON null (infinity). {@code highlight} is [the amount being
 * computed]. A reachable amount is line 3, an unreachable one is
 * line 2. The final Step traces back from the amount (using the
 * coin recorded for each amount) to recover the coins used. When
 * several coins achieve the same minimum for an amount, the last
 * coin in the given order wins — with the usual ascending coin
 * order that is the largest coin (so [1,5,10,25]/30 gives
 * coins [25, 5], not [5, 25]).
 *
 * Edit distance: the table has rows 0..m (s1) and columns 0..n
 * (s2); row 0 and column 0 are the base cases (the number of
 * insertions / deletions) and are pre-filled. Uncomputed cells
 * are JSON null. A matching pair of characters is line 3, a
 * computed edit cost is line 2. The final Step traces back from
 * (m, n) to recover the operations. Traceback rule: if the
 * characters match, move diagonally (no operation); otherwise
 * move in the direction of the smallest of dp[i-1][j] (delete),
 * dp[i][j-1] (insert), dp[i-1][j-1] (substitute), breaking ties
 * in that order — delete first, then insert, then substitute.
 *
 * LIS (Phase 15): dp[i] = 1 + max(dp[j]) over j &lt; i with
 * a[j] &lt; a[i]; the update is only on strict &gt; (the first j
 * wins ties). The trace is one step per i with the dp array so
 * far (null = not yet computed, knapsack convention); the final
 * Step traces back from the LAST index with the max dp, following
 * {@code prev} links. With that rule [10, 9, 2, 5, 3, 7, 101, 18]
 * reconstructs to [2, 5, 7, 18] — not [2, 3, 7, 18], which is
 * also length 4 but is not what this rule builds.
 *
 * MCM (Phase 15): the table is (n+1)x(n+1), 1-based (row 0 and
 * column 0 are unused nulls); the diagonal is 0 and uncomputed
 * cells are null. Cells are computed by increasing chain length
 * L = 2..n, one step per chain length — the step's message lists
 * every cell computed at that length with its split k, and
 * {@code highlight} is [i, j] of the last cell computed at that
 * length. The split table records the argmin (first k wins
 * ties); the final Step builds the parenthesization from it by
 * the usual recursion, matrices named A1..An in order.
 *
 * LPS: the table has rows 0..n-1 and columns 0..n-1;
 * dp[i][j] is the LPS length of s[i..j]; cells with i &gt; j are
 * JSON null and the diagonal is pre-filled with 1. Cells are
 * computed by increasing substring length. A matching pair of
 * characters is line 3, a max-of-down-or-left cell is line 2.
 * The final Step traces back from (0, n-1) to recover one LPS.
 * Traceback rule: if s[i] == s[j], take both characters and move
 * to (i+1, j-1); otherwise if dp[i+1][j] &gt;= dp[i][j-1], move
 * down (skip s[i]); else move left (skip s[j]).
 */
@Service
public class DpService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_TAKE = 3;
    private static final int LINE_FINAL = 0;

    /**
     * Longest common subsequence of the two strings in
     * {@code words} ([s1, s2]). The controller validates the
     * input (exactly 2 non-empty strings, each at most 12
     * characters); this method guards against bad input anyway
     * with a single "empty" Step.
     */
    public List<Step> lcs(List<String> words) {
        List<Step> steps = new ArrayList<>();
        if (words == null || words.size() != 2
                || words.get(0) == null || words.get(1) == null
                || words.get(0).isEmpty() || words.get(1).isEmpty()) {
            steps.add(dpStep(List.of(), List.of(),
                    "Two non-empty strings are needed.", LINE_FINAL));
            return steps;
        }
        String s1 = words.get(0);
        String s2 = words.get(1);
        int m = s1.length();
        int n = s2.length();
        // Table rows 0..m, columns 0..n; null = uncomputed.
        // Row 0 and column 0 are the base case (0), pre-filled.
        List<List<Integer>> table = new ArrayList<>();
        for (int i = 0; i <= m; i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j <= n; j++) {
                row.add(i == 0 || j == 0 ? 0 : null);
            }
            table.add(row);
        }
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char a = s1.charAt(i - 1);
                char b = s2.charAt(j - 1);
                int value;
                int line;
                String message;
                if (a == b) {
                    // Match: extend the diagonal subsequence.
                    value = table.get(i - 1).get(j - 1) + 1;
                    line = LINE_TAKE;
                    message = "dp[" + i + "][" + j + "] = dp["
                            + (i - 1) + "][" + (j - 1) + "] + 1 = "
                            + value + " ('" + a + "' == '" + b + "').";
                } else {
                    int up = table.get(i - 1).get(j);
                    int left = table.get(i).get(j - 1);
                    value = Math.max(up, left);
                    line = LINE_COMPARE;
                    message = "dp[" + i + "][" + j + "] = max(dp["
                            + (i - 1) + "][" + j + "], dp[" + i + "]["
                            + (j - 1) + "]) = max(" + up + ", " + left
                            + ") = " + value + ".";
                }
                table.get(i).set(j, value);
                steps.add(dpStep(table, List.of(i, j), message, line));
            }
        }
        // Trace back from (m, n) to recover one LCS.
        StringBuilder lcs = new StringBuilder();
        int i = m;
        int j = n;
        while (i > 0 && j > 0) {
            if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                lcs.append(s1.charAt(i - 1));
                i--;
                j--;
            } else if (table.get(i - 1).get(j) >= table.get(i).get(j - 1)) {
                i--; // move up (skip s1's character)
            } else {
                j--; // move left (skip s2's character)
            }
        }
        String lcsText = lcs.reverse().toString();
        String finalMessage = lcsText.isEmpty()
                ? "LCS length: 0. No common subsequence."
                : "LCS length: " + table.get(m).get(n) + ". LCS: "
                        + lcsText + ".";
        steps.add(dpStep(table, List.of(m, n), finalMessage, LINE_FINAL));
        return steps;
    }

    /**
     * Minimum coins to make {@code target} with the denominations
     * in {@code values}. The controller validates the input
     * (non-empty coins, all &gt; 0, amount 0..50); this method
     * guards against bad input anyway with a single "empty" Step.
     */
    public List<Step> coinChange(List<Integer> values, Integer target) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty() || target == null) {
            steps.add(coinStep(List.of(), List.of(),
                    "No coins or amount.", LINE_FINAL));
            return steps;
        }
        int amount = target;
        // dp[0..amount], null = unreachable; choice[a] = the coin
        // used to make amount a. dp[0] = 0 (no coins needed).
        List<Integer> dp = new ArrayList<>();
        List<Integer> choice = new ArrayList<>();
        for (int a = 0; a <= amount; a++) {
            dp.add(null);
            choice.add(null);
        }
        dp.set(0, 0);
        for (int a = 1; a <= amount; a++) {
            Integer best = null;
            Integer bestCoin = null;
            StringBuilder candidates = new StringBuilder();
            for (int coin : values) {
                // Defensive: the controller rejects coins <= 0, but
                // a non-positive coin would index out of range here.
                if (coin <= 0 || coin > a) {
                    continue;
                }
                Integer prev = dp.get(a - coin);
                if (prev == null) {
                    continue; // that remainder is unreachable
                }
                int candidate = prev + 1;
                if (candidates.length() > 0) {
                    candidates.append(", ");
                }
                candidates.append("1 + dp[").append(a - coin).append("]");
                // "<=" so the last coin achieving the minimum wins
                // (with ascending coins, the largest one).
                if (best == null || candidate <= best) {
                    best = candidate;
                    bestCoin = coin;
                }
            }
            String message;
            int line;
            if (best == null) {
                line = LINE_COMPARE;
                message = "dp[" + a + "] = ∞ (no combination of coins "
                        + "makes " + a + ").";
            } else {
                dp.set(a, best);
                choice.set(a, bestCoin);
                line = LINE_TAKE;
                message = "dp[" + a + "] = min(" + candidates + ") = "
                        + best + " (coin " + bestCoin + ").";
            }
            steps.add(coinStep(dp, List.of(a), message, line));
        }
        if (dp.get(amount) == null) {
            steps.add(coinStep(dp, List.of(amount),
                    "No combination possible.", LINE_FINAL));
            return steps;
        }
        // Trace back from the amount to recover the coins used.
        List<Integer> used = new ArrayList<>();
        int remaining = amount;
        while (remaining > 0) {
            int coin = choice.get(remaining);
            used.add(coin);
            remaining -= coin;
        }
        String usedText = used.isEmpty() ? "none" : joinInts(used);
        steps.add(coinStep(dp, List.of(amount),
                "Minimum coins: " + dp.get(amount) + ". Coins used: "
                        + usedText + ".",
                LINE_FINAL));
        return steps;
    }

    /**
     * Edit distance (Levenshtein) between the two strings in
     * {@code words} ([s1, s2]). The controller validates the
     * input (exactly 2 non-empty strings, each at most 12
     * characters); this method guards against bad input anyway
     * with a single "empty" Step.
     */
    public List<Step> editDistance(List<String> words) {
        List<Step> steps = new ArrayList<>();
        if (words == null || words.size() != 2
                || words.get(0) == null || words.get(1) == null
                || words.get(0).isEmpty() || words.get(1).isEmpty()) {
            steps.add(dpStep(List.of(), List.of(),
                    "Two non-empty strings are needed.", LINE_FINAL));
            return steps;
        }
        String s1 = words.get(0);
        String s2 = words.get(1);
        int m = s1.length();
        int n = s2.length();
        // Table rows 0..m, columns 0..n; null = uncomputed.
        // Row 0 and column 0 are the base cases (insertions /
        // deletions), pre-filled.
        List<List<Integer>> table = new ArrayList<>();
        for (int i = 0; i <= m; i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j <= n; j++) {
                row.add(i == 0 ? j : (j == 0 ? i : null));
            }
            table.add(row);
        }
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char a = s1.charAt(i - 1);
                char b = s2.charAt(j - 1);
                int value;
                int line;
                String message;
                if (a == b) {
                    // Match: no operation — take the diagonal cost.
                    value = table.get(i - 1).get(j - 1);
                    line = LINE_TAKE;
                    message = "dp[" + i + "][" + j + "] = dp["
                            + (i - 1) + "][" + (j - 1) + "] = " + value
                            + " ('" + a + "' == '" + b + "').";
                } else {
                    int del = table.get(i - 1).get(j);
                    int ins = table.get(i).get(j - 1);
                    int sub = table.get(i - 1).get(j - 1);
                    value = 1 + Math.min(del, Math.min(ins, sub));
                    line = LINE_COMPARE;
                    message = "dp[" + i + "][" + j + "] = 1 + min(dp["
                            + (i - 1) + "][" + j + "], dp[" + i + "]["
                            + (j - 1) + "], dp[" + (i - 1) + "]["
                            + (j - 1) + "]) = 1 + min(" + del + ", "
                            + ins + ", " + sub + ") = " + value + ".";
                }
                table.get(i).set(j, value);
                steps.add(dpStep(table, List.of(i, j), message, line));
            }
        }
        // Trace back from (m, n) to recover the operations.
        // Ties break delete first, then insert, then substitute.
        List<String> operations = new ArrayList<>();
        int i = m;
        int j = n;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0
                    && s1.charAt(i - 1) == s2.charAt(j - 1)) {
                i--; // match: move diagonally, no operation
                j--;
            } else if (i > 0 && j > 0) {
                int del = table.get(i - 1).get(j);
                int ins = table.get(i).get(j - 1);
                int sub = table.get(i - 1).get(j - 1);
                if (del <= ins && del <= sub) {
                    operations.add("delete " + s1.charAt(i - 1));
                    i--; // delete s1[i-1]
                } else if (ins <= sub) {
                    operations.add("insert " + s2.charAt(j - 1));
                    j--; // insert s2[j-1]
                } else {
                    operations.add("substitute " + s1.charAt(i - 1)
                            + "→" + s2.charAt(j - 1));
                    i--;
                    j--;
                }
            } else if (i > 0) {
                // j == 0: only deletions remain.
                operations.add("delete " + s1.charAt(i - 1));
                i--;
            } else {
                // i == 0: only insertions remain.
                operations.add("insert " + s2.charAt(j - 1));
                j--;
            }
        }
        Collections.reverse(operations);
        int distance = table.get(m).get(n);
        String finalMessage = operations.isEmpty()
                ? "Edit distance: " + distance + "."
                : "Edit distance: " + distance + ". Operations: "
                        + String.join(", ", operations) + ".";
        steps.add(dpStep(table, List.of(m, n), finalMessage, LINE_FINAL));
        return steps;
    }

    /**
     * Longest palindromic subsequence of the string in
     * {@code words} ([s]). The controller validates the input
     * (exactly 1 non-empty string, at most 12 characters); this
     * method guards against bad input anyway with a single
     * "empty" Step.
     */
    public List<Step> lps(List<String> words) {
        List<Step> steps = new ArrayList<>();
        if (words == null || words.size() != 1
                || words.get(0) == null || words.get(0).isEmpty()) {
            steps.add(dpStep(List.of(), List.of(),
                    "One non-empty string is needed.", LINE_FINAL));
            return steps;
        }
        String s = words.get(0);
        int n = s.length();
        // Table rows 0..n-1, columns 0..n-1; null = uncomputed or
        // i > j (an empty substring). The diagonal is 1 (a single
        // character is a palindrome of length 1).
        List<List<Integer>> table = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                row.add(i == j ? 1 : null);
            }
            table.add(row);
        }
        // Fill by increasing substring length (2..n).
        for (int len = 2; len <= n; len++) {
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                char a = s.charAt(i);
                char b = s.charAt(j);
                int value;
                int line;
                String message;
                if (a == b) {
                    // Match: both ends plus the inner LPS.
                    int inner = len == 2 ? 0 : table.get(i + 1).get(j - 1);
                    value = inner + 2;
                    line = LINE_TAKE;
                    message = "dp[" + i + "][" + j + "] = dp["
                            + (i + 1) + "][" + (j - 1) + "] + 2 = "
                            + value + " ('" + a + "' == '" + b + "').";
                } else {
                    int down = table.get(i + 1).get(j);
                    int left = table.get(i).get(j - 1);
                    value = Math.max(down, left);
                    line = LINE_COMPARE;
                    message = "dp[" + i + "][" + j + "] = max(dp["
                            + (i + 1) + "][" + j + "], dp[" + i + "]["
                            + (j - 1) + "]) = max(" + down + ", "
                            + left + ") = " + value + ".";
                }
                table.get(i).set(j, value);
                steps.add(dpStep(table, List.of(i, j), message, line));
            }
        }
        // Trace back from (0, n-1) to recover one LPS. When the
        // ends match, take both; otherwise move down when
        // dp[i+1][j] >= dp[i][j-1], else left.
        StringBuilder leftPart = new StringBuilder();
        StringBuilder rightPart = new StringBuilder();
        int i = 0;
        int j = n - 1;
        while (i <= j) {
            if (i == j) {
                // The middle character of an odd-length LPS.
                leftPart.append(s.charAt(i));
                break;
            }
            if (s.charAt(i) == s.charAt(j)) {
                leftPart.append(s.charAt(i));
                rightPart.insert(0, s.charAt(j));
                i++;
                j--;
            } else if (table.get(i + 1).get(j)
                    >= table.get(i).get(j - 1)) {
                i++; // move down (skip s[i])
            } else {
                j--; // move left (skip s[j])
            }
        }
        String lpsText = leftPart.toString() + rightPart.toString();
        steps.add(dpStep(table, List.of(0, n - 1),
                "LPS length: " + table.get(0).get(n - 1) + ". LPS: "
                        + lpsText + ".",
                LINE_FINAL));
        return steps;
    }

    /**
     * Longest increasing subsequence of {@code values} (Phase 15).
     * dp[i] = 1 + max(dp[j]) over j &lt; i with a[j] &lt; a[i]; the
     * update is only on strict &gt;, so the first j wins ties. The
     * controller validates the input (non-empty, at most 10 values,
     * each |value| &lt;= 999); this method guards against bad input
     * anyway with a single "empty" Step.
     */
    public List<Step> lis(List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            steps.add(coinStep(List.of(), List.of(),
                    "No values.", LINE_FINAL));
            return steps;
        }
        int n = values.size();
        // dp[i] = LIS length ending at i (null = not yet computed);
        // prev[i] = the j that achieved it, -1 when there is none.
        List<Integer> dp = new ArrayList<>();
        List<Integer> prev = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            dp.add(null);
            prev.add(-1);
        }
        for (int i = 0; i < n; i++) {
            int best = 1;
            int bestPrev = -1;
            for (int j = 0; j < i; j++) {
                // Strict ">" so the first j wins ties.
                if (values.get(j) < values.get(i)
                        && dp.get(j) + 1 > best) {
                    best = dp.get(j) + 1;
                    bestPrev = j;
                }
            }
            dp.set(i, best);
            prev.set(i, bestPrev);
            String message;
            int line;
            if (bestPrev == -1) {
                line = LINE_COMPARE;
                message = "dp[" + i + "] = 1 (no smaller element "
                        + "before it).";
            } else {
                line = LINE_TAKE;
                message = "dp[" + i + "] = " + best
                        + " (best previous: " + values.get(bestPrev)
                        + " at index " + bestPrev + ").";
            }
            steps.add(coinStep(dp, List.of(i), message, line));
        }
        // Trace back from the LAST index with the max dp ("&gt;=" so
        // the last max index wins), following prev links.
        int maxLen = 0;
        int maxIdx = 0;
        for (int i = 0; i < n; i++) {
            if (dp.get(i) >= maxLen) {
                maxLen = dp.get(i);
                maxIdx = i;
            }
        }
        List<Integer> lis = new ArrayList<>();
        int cur = maxIdx;
        while (cur != -1) {
            lis.add(values.get(cur));
            cur = prev.get(cur);
        }
        Collections.reverse(lis);
        steps.add(coinStep(dp, List.of(maxIdx),
                "Longest increasing subsequence has length " + maxLen
                        + ": [" + joinInts(lis) + "].",
                LINE_FINAL));
        return steps;
    }

    /**
     * Matrix chain multiplication (Phase 15): minimum scalar
     * multiplications for the chain whose dimensions are
     * {@code values} (n+1 numbers for n matrices). Standard O(n^3)
     * DP: m[i][j] = min over k of m[i][k] + m[k+1][j] +
     * p[i-1]*p[k]*p[j]; the split table records the argmin (first
     * k wins ties). The controller validates the input (3..7
     * dimensions, each 1..99); this method guards against bad
     * input anyway with a single "empty" Step.
     */
    public List<Step> mcm(List<Integer> values) {
        List<Step> steps = new ArrayList<>();
        if (values == null || values.size() < 2) {
            steps.add(dpStep(List.of(), List.of(),
                    "At least two dimensions are needed.", LINE_FINAL));
            return steps;
        }
        int n = values.size() - 1; // number of matrices
        // m[i][j] and s[i][j] for 1 <= i <= j <= n; the table is
        // (n+1)x(n+1) and 1-based, so row 0 and column 0 stay
        // null (unused) and the diagonal is pre-filled with 0.
        List<List<Integer>> table = new ArrayList<>();
        List<List<Integer>> split = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            List<Integer> mRow = new ArrayList<>();
            List<Integer> sRow = new ArrayList<>();
            for (int j = 0; j <= n; j++) {
                mRow.add(i == j && i > 0 ? 0 : null);
                sRow.add(null);
            }
            table.add(mRow);
            split.add(sRow);
        }
        // One step per chain length L = 2..n.
        for (int len = 2; len <= n; len++) {
            StringBuilder message = new StringBuilder(
                    "Chain length " + len + ": ");
            int lastI = -1;
            int lastJ = -1;
            for (int i = 1; i <= n - len + 1; i++) {
                int j = i + len - 1;
                int best = Integer.MAX_VALUE;
                int bestK = i;
                for (int k = i; k < j; k++) {
                    int cost = table.get(i).get(k)
                            + table.get(k + 1).get(j)
                            + values.get(i - 1) * values.get(k)
                            * values.get(j);
                    // Strict "<" so the first k wins ties.
                    if (cost < best) {
                        best = cost;
                        bestK = k;
                    }
                }
                table.get(i).set(j, best);
                split.get(i).set(j, bestK);
                if (i > 1) {
                    message.append(", ");
                }
                message.append("m[").append(i).append("][").append(j)
                        .append("] = ").append(best)
                        .append(" (k = ").append(bestK).append(")");
                lastI = i;
                lastJ = j;
            }
            message.append(".");
            steps.add(dpStep(table, List.of(lastI, lastJ),
                    message.toString(), LINE_COMPARE));
        }
        // Final step: the minimum cost and the parenthesization
        // built from the split table.
        String parens = parenthesize(1, n, split);
        steps.add(dpStep(table, List.of(1, n),
                "Minimum cost " + table.get(1).get(n) + ". " + parens
                        + ".",
                LINE_FINAL));
        return steps;
    }

    /**
     * The optimal parenthesization of matrices i..j from the split
     * table: "(left)(right)" split at s[i][j], "Ai" for one matrix.
     */
    private String parenthesize(int i, int j,
                                List<List<Integer>> split) {
        if (i == j) {
            return "A" + i;
        }
        int k = split.get(i).get(j);
        return "(" + parenthesize(i, k, split)
                + parenthesize(k + 1, j, split) + ")";
    }

    /**
     * One LCS Step: a deep copy of the table, highlight =
     * [row, col] (the cell being computed).
     */
    private Step dpStep(List<List<Integer>> table,
                        List<Integer> highlight,
                        String message, int line) {
        Step step = new Step(List.of(), highlight, List.of(), List.of(),
                message, line);
        step.setTable(copyTable(table));
        return step;
    }

    /**
     * One coin-change Step: a copy of the dp array (null =
     * unreachable), highlight = [the amount being computed].
     */
    private Step coinStep(List<Integer> dp,
                          List<Integer> highlight,
                          String message, int line) {
        return new Step(new ArrayList<>(dp), highlight, List.of(),
                List.of(), message, line);
    }

    /**
     * Deep copy of the DP table (null cells included), so later
     * steps never mutate earlier snapshots.
     */
    private List<List<Integer>> copyTable(List<List<Integer>> table) {
        List<List<Integer>> copy = new ArrayList<>();
        for (List<Integer> row : table) {
            copy.add(new ArrayList<>(row));
        }
        return copy;
    }

    /** "25, 5" from [25, 5]. */
    private String joinInts(List<Integer> ints) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ints.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(ints.get(i));
        }
        return sb.toString();
    }
}
