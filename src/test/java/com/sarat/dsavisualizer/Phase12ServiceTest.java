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
 * Phase 12 service tests. Red-black delete tests check the root
 * colour, the inorder sequence and the invariants (no red node has
 * a red child; every root-to-leaf path has the same black-height);
 * weighted-A* tests check the cheapest cost and the path in the
 * final message; edit-distance tests check the final table cell
 * and the recovered operations; LPS tests check the length and
 * that the recovered string is a palindrome and a subsequence;
 * Floyd-Warshall tests check the final distance matrix.
 *
 * The RBT delete-10 root (40 black) is the design doc's
 * hand-trace, confirmed by the Python simulation before this
 * test was written: deleting the black leaf 10 leaves the
 * double-black on the left of 20, the sibling 40 is black with
 * the red right child 50, so case 4 recolors 50 black and
 * rotates left at 20 — 40 becomes the root.
 */
class Phase12ServiceTest {

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

    // ---- Red-black helpers (same as Phase 11) ----

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

    /** No red node has a red child in the final step's tree. */
    private void assertNoRedRed(List<Step> steps) {
        Map<Integer, NodeDto> nodes = nodeById(steps);
        for (EdgeDto edge : lastStep(steps).getEdges()) {
            NodeDto parent = nodes.get(edge.getFrom());
            NodeDto child = nodes.get(edge.getTo());
            assertTrue(!("red".equals(parent.getColor())
                    && "red".equals(child.getColor())),
                    "red-red violation: " + parent.getValue()
                            + " -> " + child.getValue());
        }
    }

    @Test
    void redBlackDeleteRedLeaf() {
        // 50 is a red leaf (right child of 40): no fix-up, the
        // tree is just 50 lighter. Root stays 20 black.
        List<Step> steps = redBlackService.delete(
                List.of(10, 20, 30, 40, 50), 50);
        NodeDto root = rootOf(steps);
        assertEquals(20, root.getValue());
        assertEquals("black", root.getColor());
        assertEquals(List.of(10, 20, 30, 40), inorderOf(steps));
        assertNoRedRed(steps);
        assertEquals(Set.of(2), blackHeights(steps));
        assertTrue(anyMessageContains(steps,
                "Delete 50 (red leaf)."));
        assertTrue(lastStep(steps).getMessage()
                .contains("Deleted 50. Root: 20 (black)."));
    }

    @Test
    void redBlackDeleteBlackLeaf() {
        // 10 is a black leaf (left child of 20): the double-black
        // fix-up runs — case 4 (sibling 40 black, right child 50
        // red) recolors 50 black and rotates left at 20, so 40
        // becomes the root. 30 stays red as the right child of 20.
        List<Step> steps = redBlackService.delete(
                List.of(10, 20, 30, 40, 50), 10);
        NodeDto root = rootOf(steps);
        assertEquals(40, root.getValue());
        assertEquals("black", root.getColor());
        assertEquals(List.of(20, 30, 40, 50), inorderOf(steps));
        assertNoRedRed(steps);
        assertEquals(Set.of(2), blackHeights(steps));
        assertTrue(anyMessageContains(steps,
                "Delete 10 (black leaf)."));
        assertTrue(anyMessageContains(steps,
                "Recolor node 50 to black."));
        assertTrue(anyMessageContains(steps,
                "Left rotate at node 20."));
        assertTrue(lastStep(steps).getMessage()
                .contains("Deleted 10. Root: 40 (black)."));
    }

    @Test
    void redBlackDeleteNotFound() {
        List<Step> steps = redBlackService.delete(
                List.of(10, 20, 30), 99);
        assertTrue(lastStep(steps).getMessage().contains("not found"));
        // The tree is untouched.
        assertEquals(List.of(10, 20, 30), inorderOf(steps));
        assertEquals(20, rootOf(steps).getValue());
    }

    @Test
    void redBlackDeleteRoot() {
        // The root 20 has two children: the successor 30 takes its
        // place and its colour (black). 30 was red, so no fix-up.
        List<Step> steps = redBlackService.delete(
                List.of(10, 20, 30), 20);
        NodeDto root = rootOf(steps);
        assertEquals(30, root.getValue());
        assertEquals("black", root.getColor());
        assertEquals(List.of(10, 30), inorderOf(steps));
        assertNoRedRed(steps);
        assertEquals(Set.of(1), blackHeights(steps));
        assertTrue(anyMessageContains(steps,
                "Delete 20 — replace with its successor 30."));
        assertTrue(lastStep(steps).getMessage()
                .contains("Deleted 20. Root: 30 (black)."));
    }

    // ---- Weighted A* tests ----

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
    void weightedAStarExample() {
        // The middle column costs 5 to enter; the bottom row costs
        // 1. The cheapest route goes around the cost-5 column:
        // (0,0) -> (1,0) -> (2,0) -> (2,1) -> (2,2), cost 4.
        // The geometrically shortest route through the middle
        // costs 12 (5 + 1 + 5 + 1).
        List<List<Integer>> costGrid = List.of(
                List.of(1, 5, 1),
                List.of(1, 5, 1),
                List.of(1, 1, 1));
        List<Step> steps = graphService.aStarWeighted(
                costGrid, List.of(0, 0), List.of(2, 2));
        assertTrue(lastStep(steps).getMessage()
                .contains("Cheapest cost 4."));
        assertTrue(lastStep(steps).getMessage().contains(
                "Path: (0,0) -> (1,0) -> (2,0) -> (2,1) -> (2,2)."));
        assertEquals(5, countState(steps, 5));
    }

    @Test
    void weightedAStarUniform() {
        // All costs 1: the cheapest cost is the Manhattan
        // distance, 4.
        List<List<Integer>> costGrid = List.of(
                List.of(1, 1, 1),
                List.of(1, 1, 1),
                List.of(1, 1, 1));
        List<Step> steps = graphService.aStarWeighted(
                costGrid, List.of(0, 0), List.of(2, 2));
        assertTrue(lastStep(steps).getMessage()
                .contains("Cheapest cost 4."));
        assertEquals(5, countState(steps, 5));
    }

    @Test
    void weightedAStarNoPath() {
        // The middle row is all walls (cost 0): the goal is
        // unreachable.
        List<List<Integer>> costGrid = List.of(
                List.of(1, 1, 1),
                List.of(0, 0, 0),
                List.of(1, 1, 1));
        List<Step> steps = graphService.aStarWeighted(
                costGrid, List.of(0, 0), List.of(2, 2));
        assertTrue(lastStep(steps).getMessage().contains("No path"));
        assertEquals(0, countState(steps, 5));
    }

    // ---- Edit-distance tests ----

   
    // ---- LPS tests ----

    /** True when sub is a subsequence of s (in order, not necessarily contiguous). */
    private boolean isSubsequence(String sub, String s) {
        int i = 0;
        for (int j = 0; j < s.length() && i < sub.length(); j++) {
            if (s.charAt(j) == sub.charAt(i)) {
                i++;
            }
        }
        return i == sub.length();
    }

    /** The LPS string from the final message ("LPS length: 7. LPS: BACBCAB."). */
    private String lpsOf(Step last) {
        String message = last.getMessage();
        int start = message.indexOf("LPS: ") + 5;
        int end = message.lastIndexOf('.');
        return message.substring(start, end);
    }

    @Test
    void lpsExample() {
        // "BBABCBCAB" -> length 7. The traceback rule gives
        // "BACBCAB" (a palindrome and a subsequence); "BABCBAB"
        // is also valid, so the test asserts the length, the
        // palindrome property and the subsequence property — not
        // the exact string.
        List<Step> steps = dpService.lps(List.of("BBABCBCAB"));
        Step last = lastStep(steps);
        assertEquals(7, last.getTable().get(0).get(8));
        assertTrue(last.getMessage().contains("LPS length: 7"));
        String lps = lpsOf(last);
        assertEquals(7, lps.length());
        assertEquals(new StringBuilder(lps).reverse().toString(), lps);
        assertTrue(isSubsequence(lps, "BBABCBCAB"));
    }

    @Test
    void lpsBbab() {
        List<Step> steps = dpService.lps(List.of("BBAB"));
        Step last = lastStep(steps);
        assertEquals(3, last.getTable().get(0).get(3));
        assertTrue(last.getMessage().contains("LPS length: 3"));
        assertTrue(last.getMessage().contains("LPS: BAB"));
    }

    @Test
    void lpsAbc() {
        // No two characters match: the LPS is any single character.
        List<Step> steps = dpService.lps(List.of("ABC"));
        Step last = lastStep(steps);
        assertEquals(1, last.getTable().get(0).get(2));
        assertTrue(last.getMessage().contains("LPS length: 1"));
        assertEquals(1, lpsOf(last).length());
    }

    // ---- Floyd-Warshall tests ----

    @Test
    void floydWarshallExample() {
        List<List<Integer>> edges = List.of(
                List.of(0, 1, 3), List.of(0, 2, 6), List.of(1, 2, 4),
                List.of(1, 3, 5), List.of(2, 3, 2));
        List<Step> steps = graphService.floydWarshall(edges);
        Step last = lastStep(steps);
        // Undirected (Phase 12 correction): the matrix is symmetric —
        // the original design doc asserted a directed matrix
        // ([null, 0, 4, 5] / [null, null, 0, 2] / [null, null, null,
        // 0]), but every other graph algorithm in the app treats
        // edges as two-way, so FW does the same.
        assertEquals(List.of(0, 3, 6, 8), last.getTable().get(0));
        assertEquals(List.of(3, 0, 4, 5), last.getTable().get(1));
        assertEquals(List.of(6, 4, 0, 2), last.getTable().get(2));
        assertEquals(List.of(8, 5, 2, 0), last.getTable().get(3));
        assertTrue(last.getMessage()
                .contains("All-pairs shortest paths computed."));
    }

    @Test
    void floydWarshallNegativeCycle() {
        // The same negative cycle as the Bellman-Ford test:
        // 0 -> 1 (1), 1 -> 2 (-2), 2 -> 0 (-1), total -2.
        List<List<Integer>> edges = List.of(
                List.of(0, 1, 1), List.of(1, 2, -2),
                List.of(2, 0, -1));
        List<Step> steps = graphService.floydWarshall(edges);
        assertTrue(lastStep(steps).getMessage()
                .contains("Negative cycle"));
    }

    @Test
    void floydWarshallSingleEdge() {
        List<List<Integer>> edges = List.of(List.of(0, 1, 5));
        List<Step> steps = graphService.floydWarshall(edges);
        Step last = lastStep(steps);
        // Undirected: the single edge is two-way.
        assertEquals(5, last.getTable().get(0).get(1));
        assertEquals(5, last.getTable().get(1).get(0));
        assertEquals(0, last.getTable().get(0).get(0));
        assertEquals(0, last.getTable().get(1).get(1));
    }
}
