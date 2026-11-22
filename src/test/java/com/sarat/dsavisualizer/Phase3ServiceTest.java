package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.service.AvlService;
import com.sarat.dsavisualizer.service.GraphService;
import com.sarat.dsavisualizer.service.HashTableService;
import com.sarat.dsavisualizer.service.HeapService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 service tests. Each test checks the LAST step's state —
 * the trace must end in the correct final state — except the graph
 * tests, which check the order of the "Visit node" steps.
 */
class Phase3ServiceTest {

    private final HeapService heapService = new HeapService();
    private final HashTableService hashTableService = new HashTableService();
    private final GraphService graphService = new GraphService();
    private final AvlService avlService = new AvlService();

    /** Final data of the last step. */
    private List<Integer> lastData(List<Step> steps) {
        return steps.get(steps.size() - 1).getData();
    }

    @Test
    void heapInsertBubblesToRoot() {
        List<Step> steps = heapService.insert(List.of(3, 1), 0);
        // 0 is appended at index 2, then swaps with its parent 3
        // (index 0) and lands at the root: [0, 1, 3].
        assertEquals(List.of(0, 1, 3), lastData(steps));
    }

    @Test
    void heapExtractMinRemovesRoot() {
        List<Step> steps = heapService.extractMin(List.of(1, 3, 2));
        assertEquals(List.of(2, 3), lastData(steps));
    }

    @Test
    void hashInsertPlacesAtHashBucket() {
        List<Step> steps = hashTableService.insert(List.of(), 5, 11);
        assertEquals(Integer.valueOf(5), lastData(steps).get(5));
    }

    @Test
    void hashInsertProbesPastCollision() {
        // 16 hashes to bucket 5, which 5 already holds, so linear
        // probing places 16 at bucket 6.
        List<Step> steps = hashTableService.insert(List.of(5), 16, 11);
        assertEquals(Integer.valueOf(16), lastData(steps).get(6));
    }

    @Test
    void hashSearchFindsTarget() {
        List<Step> steps = hashTableService.search(List.of(5, 16), 16, 11);
        String lastMessage = steps.get(steps.size() - 1).getMessage();
        assertTrue(lastMessage.contains("Found"),
                "Last step should say the target was found, was: "
                        + lastMessage);
    }

    @Test
    void graphBfsVisitsInBreadthOrder() {
        List<List<Integer>> edges = List.of(
                List.of(0, 1), List.of(0, 2),
                List.of(1, 3), List.of(2, 3));
        List<Step> steps = graphService.bfs(edges, 0);
        assertEquals(List.of(0, 1, 2, 3), visitOrder(steps));
    }

    @Test
    void graphDfsVisitsInDepthOrder() {
        List<List<Integer>> edges = List.of(
                List.of(0, 1), List.of(0, 2),
                List.of(1, 3), List.of(2, 3));
        List<Step> steps = graphService.dfs(edges, 0);
        assertEquals(List.of(0, 1, 3, 2), visitOrder(steps));
    }

    @Test
    void avlInsertRotatesRightRight() {
        // 10, 20, 30 is a right-right case: after the left rotation
        // at 10, the root is 20.
        List<Step> steps = avlService.insert(List.of(10, 20), 30);
        assertEquals(20, rootValue(steps.get(steps.size() - 1)));
    }

    @Test
    void avlInsertRotatesLeftRight() {
        // 30, 10, 20 is a left-right case: after the double
        // rotation, the root is 20.
        List<Step> steps = avlService.insert(List.of(30, 10), 20);
        assertEquals(20, rootValue(steps.get(steps.size() - 1)));
    }

    @Test
    void avlSearchFindsTarget() {
        List<Step> steps = avlService.search(List.of(10, 20, 30), 20);
        String lastMessage = steps.get(steps.size() - 1).getMessage();
        assertTrue(lastMessage.contains("Found"),
                "Last step should say the target was found, was: "
                        + lastMessage);
    }

    /** Node ids in "Visit node X" order, from the step messages. */
    private List<Integer> visitOrder(List<Step> steps) {
        List<Integer> order = new ArrayList<>();
        for (Step step : steps) {
            String message = step.getMessage();
            if (message.startsWith("Visit node ")) {
                int end = message.indexOf('.', "Visit node ".length());
                order.add(Integer.parseInt(
                        message.substring("Visit node ".length(), end)));
            }
        }
        return order;
    }

    /**
     * Value of the root node in a tree step: the node no edge
     * points TO.
     */
    private int rootValue(Step step) {
        Set<Integer> hasParent = new HashSet<>();
        for (EdgeDto edge : step.getEdges()) {
            hasParent.add(edge.getTo());
        }
        for (NodeDto node : step.getNodes()) {
            if (!hasParent.contains(node.getId())) {
                return node.getValue();
            }
        }
        throw new AssertionError("No root node found in step");
    }
}
