package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.service.AvlService;
import com.sarat.dsavisualizer.service.BstService;
import com.sarat.dsavisualizer.service.GraphService;
import com.sarat.dsavisualizer.service.HeapSortService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 7 service tests. Dijkstra/heapsort tests check the LAST
 * step's data; tree-delete tests check the root value and the
 * in-order values of the final tree.
 */
class Phase7ServiceTest {

    private final GraphService graphService = new GraphService();
    private final HeapSortService heapSortService = new HeapSortService();
    private final BstService bstService = new BstService();
    private final AvlService avlService = new AvlService();

    /** Final data of the last step. */
    private List<Integer> lastData(List<Step> steps) {
        return steps.get(steps.size() - 1).getData();
    }

    /** Final step of a trace. */
    private Step lastStep(List<Step> steps) {
        return steps.get(steps.size() - 1);
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

    /**
     * Values of the nodes in a tree step, in the order the service
     * lists them (in-order).
     */
    private List<Integer> inOrderValues(Step step) {
        List<Integer> values = new ArrayList<>();
        for (NodeDto node : step.getNodes()) {
            values.add(node.getValue());
        }
        return values;
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

    @Test
    void dijkstraWeightedExample() {
        // 0->2 is 2, 2->1 is 1 so 1 is 3, 1->3 is 5 so 3 is 8,
        // 3->4 is 2 so 4 is 10, 4->5 is 3 so 5 is 13.
        List<List<Integer>> edges = List.of(
                List.of(0, 1, 4), List.of(0, 2, 2), List.of(1, 2, 1),
                List.of(1, 3, 5), List.of(2, 3, 8), List.of(2, 4, 10),
                List.of(3, 4, 2), List.of(3, 5, 6), List.of(4, 5, 3));
        List<Step> steps = graphService.dijkstra(edges, 0);
        assertEquals(List.of(0, 3, 2, 8, 10, 13), lastData(steps));
    }

    @Test
    void dijkstraUnweightedDefaultsToOne() {
        // NOTE: the design doc's draft said [0, 1, 2] here, but node 2
        // is directly connected to 0 with weight 1, so its shortest
        // distance is 1, not 2. [0, 1, 1] is the correct result.
        List<List<Integer>> edges = List.of(
                List.of(0, 1), List.of(1, 2), List.of(0, 2));
        List<Step> steps = graphService.dijkstra(edges, 0);
        assertEquals(List.of(0, 1, 1), lastData(steps));
    }

    @Test
    void heapSortClassicExample() {
        // Build phase gives [10, 5, 3, 4, 1]; then extract
        // 10, 5, 4, 3, 1.
        List<Step> steps = heapSortService.heapSort(
                List.of(4, 10, 3, 5, 1));
        assertEquals(List.of(1, 3, 4, 5, 10), lastData(steps));
    }

    @Test
    void heapSortSmall() {
        List<Step> steps = heapSortService.heapSort(List.of(3, 1, 2));
        assertEquals(List.of(1, 2, 3), lastData(steps));
    }

    @Test
    void heapSortSingleElement() {
        List<Step> steps = heapSortService.heapSort(List.of(5));
        assertEquals(List.of(5), lastData(steps));
    }

    @Test
    void bstDeleteRootWithTwoChildrenUsesSuccessor() {
        // 20 has two children; the successor 30 takes its place,
        // 10 stays on the left.
        List<Step> steps = bstService.delete(List.of(20, 10, 30), 20);
        Step last = lastStep(steps);
        assertEquals(30, rootValue(last));
        assertEquals(List.of(10, 30), inOrderValues(last));
    }

    @Test
    void bstDeleteLeaf() {
        List<Step> steps = bstService.delete(List.of(20, 10, 30), 10);
        Step last = lastStep(steps);
        assertEquals(20, rootValue(last));
        assertEquals(List.of(20, 30), inOrderValues(last));
    }

    @Test
    void bstDeleteOneChildIsReplacedByChild() {
        // 10 has one child (5); 5 takes its place under 20.
        List<Step> steps = bstService.delete(List.of(20, 10, 30, 5), 10);
        Step last = lastStep(steps);
        assertEquals(20, rootValue(last));
        assertEquals(List.of(5, 20, 30), inOrderValues(last));
    }

    @Test
    void avlDeleteLeafNeedsNoRotation() {
        // [10, 20, 30] builds root 20 (left 10, right 30); removing
        // the leaf 10 leaves a balanced tree, no rotation.
        List<Step> steps = avlService.delete(List.of(10, 20, 30), 10);
        Step last = lastStep(steps);
        assertEquals(20, rootValue(last));
        assertEquals(List.of(20, 30), inOrderValues(last));
        assertFalse(anyMessageContains(steps, "rotation"),
                "No rotation should happen, but a rotation step was found");
    }

    @Test
    void avlDeleteRotatesLeftAtTwenty() {
        // Before the delete the tree is root 20, left 10, right 40
        // (left 30, right 50). Removing 10 leaves 20 right-heavy
        // by 2 with a balanced right child, so a left rotation at
        // 20 makes 40 the root.
        List<Step> steps = avlService.delete(
                List.of(10, 20, 30, 40, 50), 10);
        Step last = lastStep(steps);
        assertEquals(40, rootValue(last));
        assertEquals(List.of(20, 30, 40, 50), inOrderValues(last));
        assertTrue(anyMessageContains(steps, "Left rotation at node 20."),
                "Expected a left rotation at node 20");
    }
}
