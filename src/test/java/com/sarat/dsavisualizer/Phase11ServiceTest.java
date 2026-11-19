package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.service.DpService;
import com.sarat.dsavisualizer.service.GraphService;
import com.sarat.dsavisualizer.service.RedBlackService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 11 service tests. A* tests check the path length in the
 * final message (and the number of path cells in the final grid);
 * red-black tests check the root colour, the inorder sequence and
 * the invariants (no red node has a red child; every root-to-leaf
 * path has the same black-height); LCS tests check the final table
 * cell and the recovered subsequence; coin-change tests check the
 * final dp value and the coins in the final message.
 *
 * NOTE: the design doc lists 11 cases but says 12. The 12th test
 * here (redBlackTriangle) inserts [30, 10, 20] — the left-right
 * (case 2) rotation path, which no listed test exercises.
 */
class Phase11ServiceTest {

    private final GraphService graphService = new GraphService();
    private final RedBlackService redBlackService = new RedBlackService();
    private final DpService dpService = new DpService();

    /** Final step of a trace. */
    private Step lastStep(List<Step> steps) {
        return steps.get(steps.size() - 1);
    }

    /** True when any step's message contains the given text. */
    private boolean anyMessageContains(List<Step> steps, String text) {
        for (Step step : steps) {
            if (step.getMessage().contains(text)) {
                return true;
            }
        }
        return false;
    }

    // ---- A* helpers ----

    /** 5x5 grid with the given walls (each [row, col]). */
    private List<List<Integer>> grid5(List<List<Integer>> walls) {
        List<List<Integer>> grid = new ArrayList<>();
        for (int r = 0; r < 5; r++) {
            List<Integer> row = new ArrayList<>();
            for (int c = 0; c < 5; c++) {
                row.add(0);
            }
            grid.add(row);
        }
        for (List<Integer> wall : walls) {
            grid.get(wall.get(0)).set(wall.get(1), 1);
        }
        return grid;
    }

    /** How many cells in the final grid snapshot have the given state. */
    private long countState(List<Step> steps, int state) {
        long count = 0;
        for (List<Integer> row : lastStep(steps).getTable()) {
            for (int cell : row) {
                if (cell == state) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void aStarExample() {
        // Walls (1,1),(1,2),(1,3) block the middle; the row-0 route
        // is clear: 4 right + 4 down = 8 moves.
        List<List<Integer>> grid = grid5(List.of(
                List.of(1, 1), List.of(1, 2), List.of(1, 3)));
        List<Step> steps = graphService.aStar(
                grid, List.of(0, 0), List.of(4, 4));
        assertTrue(lastStep(steps).getMessage()
                .contains("Path found! Length 8."));
        // 9 path cells (start + 8 moves), all state 5.
        assertEquals(9, countState(steps, 5));
        // The goal cell itself is on the path.
        assertEquals(5, lastStep(steps).getTable().get(4).get(4));
        // The visit step for (0,3) shows f = g + h = 3 + 5 = 8.
        // (2,3) is NOT visited — it only ever enters the open set
        // (from (2,4)) with f = 10, after the goal is already found.
        assertTrue(anyMessageContains(steps,
                "Visit (0,3). f = g + h = 3 + 5 = 8."));
    }

    @Test
    void aStarAroundWall() {
        // Walls (2,1),(2,2),(2,3) block the row; the path must go
        // around via row 1 or row 3: 1 + 4 + 1 = 6 moves.
        List<List<Integer>> grid = grid5(List.of(
                List.of(2, 1), List.of(2, 2), List.of(2, 3)));
        List<Step> steps = graphService.aStar(
                grid, List.of(2, 0), List.of(2, 4));
        assertTrue(lastStep(steps).getMessage()
                .contains("Path found! Length 6."));
        assertEquals(7, countState(steps, 5));
    }

    @Test
    void aStarNoPath() {
        // Entire row 1 walled: the goal is unreachable.
        List<List<Integer>> grid = grid5(List.of(
                List.of(1, 0), List.of(1, 1), List.of(1, 2),
                List.of(1, 3), List.of(1, 4)));
        List<Step> steps = graphService.aStar(
                grid, List.of(0, 0), List.of(4, 4));
        assertTrue(lastStep(steps).getMessage().contains("No path"));
        assertEquals(0, countState(steps, 5));
    }

    @Test
    void aStarDefault8x8() {
        // The frontend default: 8x8, walls row 3 cols 1-6. The path
        // must pass (3,0) or (3,7); Manhattan distance is 14.
        List<List<Integer>> grid = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            List<Integer> row = new ArrayList<>();
            for (int c = 0; c < 8; c++) {
                row.add(r == 3 && c >= 1 && c <= 6 ? 1 : 0);
            }
            grid.add(row);
        }
        List<Step> steps = graphService.aStar(
                grid, List.of(0, 0), List.of(7, 7));
        assertTrue(lastStep(steps).getMessage()
                .contains("Path found! Length 14."));
        assertEquals(15, countState(steps, 5));
    }

    // ---- Red-black helpers ----

    /** Node id -> NodeDto for the final step's tree. */
    private Map<Integer, NodeDto> nodeById(List<Step> steps) {
        Map<Integer, NodeDto> map = new HashMap<>();
        for (NodeDto node : lastStep(steps).getNodes()) {
            map.put(node.getId(), node);
        }
        return map;
    }

    /** Parent id -> child ids for the final step's tree. */
    private Map<Integer, List<Integer>> childrenOf(List<Step> steps) {
        Map<Integer, List<Integer>> children = new HashMap<>();
        for (EdgeDto edge : lastStep(steps).getEdges()) {
            children.computeIfAbsent(edge.getFrom(),
                    k -> new ArrayList<>()).add(edge.getTo());
        }
        return children;
    }

    /** The root: the node no edge points to. */
    private NodeDto rootOf(List<Step> steps) {
        Set<Integer> hasParent = new HashSet<>();
        for (EdgeDto edge : lastStep(steps).getEdges()) {
            hasParent.add(edge.getTo());
        }
        for (NodeDto node : lastStep(steps).getNodes()) {
            if (!hasParent.contains(node.getId())) {
                return node;
            }
        }
        throw new IllegalStateException("no root found");
    }

    /** Inorder values of the final step's tree. */
    private List<Integer> inorderOf(List<Step> steps) {
        Map<Integer, NodeDto> nodes = nodeById(steps);
        Map<Integer, List<Integer>> children = childrenOf(steps);
        List<Integer> inorder = new ArrayList<>();
        inorderWalk(rootOf(steps).getId(), nodes, children, inorder);
        return inorder;
    }

    private void inorderWalk(int id, Map<Integer, NodeDto> nodes,
                             Map<Integer, List<Integer>> children,
                             List<Integer> inorder) {
        List<Integer> kids = children.getOrDefault(id, List.of());
        // Left child first (smaller value), then the node, then right.
        NodeDto node = nodes.get(id);
        Integer left = null;
        Integer right = null;
        for (int kid : kids) {
            if (nodes.get(kid).getValue() < node.getValue()) {
                left = kid;
            } else {
                right = kid;
            }
        }
        if (left != null) {
            inorderWalk(left, nodes, children, inorder);
        }
        inorder.add(node.getValue());
        if (right != null) {
            inorderWalk(right, nodes, children, inorder);
        }
    }

    /** All black-heights (black nodes on each root-to-null path). */
    private Set<Integer> blackHeights(List<Step> steps) {
        Map<Integer, NodeDto> nodes = nodeById(steps);
        Map<Integer, List<Integer>> children = childrenOf(steps);
        Set<Integer> heights = new HashSet<>();
        blackHeightWalk(rootOf(steps).getId(), 0, nodes, children,
                heights);
        return heights;
    }

    private void blackHeightWalk(int id, int blacks,
                                 Map<Integer, NodeDto> nodes,
                                 Map<Integer, List<Integer>> children,
                                 Set<Integer> heights) {
        NodeDto node = nodes.get(id);
        int count = blacks + ("black".equals(node.getColor()) ? 1 : 0);
        List<Integer> kids = children.getOrDefault(id, List.of());
        if (kids.isEmpty()) {
            heights.add(count); // a null leaf ends this path
            return;
        }
        // A missing child is also a null leaf on this path.
        if (kids.size() == 1) {
            heights.add(count);
        }
        for (int kid : kids) {
            blackHeightWalk(kid, count, nodes, children, heights);
        }
    }

    @Test
    void redBlackInsertThree() {
        // 10 black root; 20 red right of 10; 30 red right of 20
        // -> red-red violation -> left rotate at 10 -> root 20
        // black, 10 red, 30 red.
        List<Step> steps = redBlackService.insert(List.of(10, 20, 30));
        NodeDto root = rootOf(steps);
        assertEquals(20, root.getValue());
        assertEquals("black", root.getColor());
        Map<Integer, NodeDto> nodes = nodeById(steps);
        Map<Integer, List<Integer>> children = childrenOf(steps);
        List<Integer> kids = children.get(root.getId());
        assertEquals(2, kids.size());
        for (int kid : kids) {
            NodeDto child = nodes.get(kid);
            assertEquals("red", child.getColor());
            if (child.getValue() == 10) {
                assertTrue(child.getValue() < root.getValue());
            } else {
                assertEquals(30, child.getValue());
            }
        }
        assertTrue(lastStep(steps).getMessage()
                .contains("Red-black tree built. Root: 20 (black)."));
        assertTrue(anyMessageContains(steps, "Left rotate at node 10."));
    }

    @Test
    void redBlackInsertFive() {
        // After 40: recolor (uncle 10 is red) -> 20 black, 10 black,
        // 30 black, 40 red. After 50: right-right case -> left
        // rotate at 30 -> 40 black (right of 20), 30 red (left of
        // 40), 50 red (right of 40). Black-height 2 on every path.
        List<Step> steps = redBlackService.insert(
                List.of(10, 20, 30, 40, 50));
        NodeDto root = rootOf(steps);
        assertEquals(20, root.getValue());
        assertEquals("black", root.getColor());
        assertEquals(List.of(10, 20, 30, 40, 50), inorderOf(steps));
        // No red node has a red child.
        Map<Integer, NodeDto> nodes = nodeById(steps);
        for (EdgeDto edge : lastStep(steps).getEdges()) {
            NodeDto parent = nodes.get(edge.getFrom());
            NodeDto child = nodes.get(edge.getTo());
            assertTrue(!("red".equals(parent.getColor())
                    && "red".equals(child.getColor())),
                    "red-red violation: " + parent.getValue()
                            + " -> " + child.getValue());
        }
        // Every root-to-leaf path has black-height 2.
        assertEquals(Set.of(2), blackHeights(steps));
        assertTrue(anyMessageContains(steps, "Left rotate at node 30."));
    }

    @Test
    void redBlackTriangle() {
        // Left-right case: 30 root, 10 red left, 20 red right of 10
        // -> left rotate at 10, then recolor + right rotate at 30
        // -> root 20 black, 10 red, 30 red. This is the case-2
        // rotation path no other test exercises.
        List<Step> steps = redBlackService.insert(List.of(30, 10, 20));
        NodeDto root = rootOf(steps);
        assertEquals(20, root.getValue());
        assertEquals("black", root.getColor());
        assertEquals(List.of(10, 20, 30), inorderOf(steps));
        assertTrue(anyMessageContains(steps, "Left rotate at node 10."));
        assertTrue(anyMessageContains(steps, "Right rotate at node 30."));
    }

    // ---- LCS tests ----

    @Test
    void lcsExample() {
        List<Step> steps = dpService.lcs(List.of("ABCBDAB", "BDCABA"));
        Step last = lastStep(steps);
        assertEquals(4, last.getTable().get(7).get(6));
        assertTrue(last.getMessage().contains("LCS length: 4"));
        assertTrue(last.getMessage().contains("LCS: BCBA"));
        // A few verified table rows from the design doc.
        assertEquals(List.of(0, 0, 0, 0, 1, 1, 1),
                last.getTable().get(1));
        assertEquals(List.of(0, 1, 1, 1, 1, 2, 2),
                last.getTable().get(2));
        assertEquals(List.of(0, 1, 2, 2, 3, 4, 4),
                last.getTable().get(7));
    }

    @Test
    void lcsSmall() {
        List<Step> steps = dpService.lcs(List.of("ABC", "AC"));
        Step last = lastStep(steps);
        assertEquals(2, last.getTable().get(3).get(2));
        assertTrue(last.getMessage().contains("LCS length: 2"));
        assertTrue(last.getMessage().contains("LCS: AC"));
    }

    // ---- Coin-change tests ----

    @Test
    void coinChangeExample() {
        List<Step> steps = dpService.coinChange(
                List.of(1, 5, 10, 25), 30);
        Step last = lastStep(steps);
        assertEquals(2, last.getData().get(30));
        assertTrue(last.getMessage().contains("Minimum coins: 2"));
        // The largest coin wins the tie: 25 first, then 5.
        assertTrue(last.getMessage().contains("Coins used: 25, 5"));
    }

    @Test
    void coinChangeImpossible() {
        List<Step> steps = dpService.coinChange(List.of(2, 5), 3);
        assertTrue(lastStep(steps).getMessage()
                .contains("No combination"));
        // dp[1] and dp[3] are unreachable (null = infinity).
        assertEquals(null, lastStep(steps).getData().get(1));
        assertEquals(null, lastStep(steps).getData().get(3));
        assertEquals(1, lastStep(steps).getData().get(2));
    }

    @Test
    void coinChangeZero() {
        List<Step> steps = dpService.coinChange(
                List.of(1, 5, 10, 25), 0);
        Step last = lastStep(steps);
        assertEquals(List.of(0), last.getData());
        assertTrue(last.getMessage().contains("Minimum coins: 0"));
        assertTrue(last.getMessage().contains("Coins used: none"));
    }
}
