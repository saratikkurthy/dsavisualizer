package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.service.BstService;
import com.sarat.dsavisualizer.service.LinkedListService;
import com.sarat.dsavisualizer.service.QueueService;
import com.sarat.dsavisualizer.service.SortingService;
import com.sarat.dsavisualizer.service.StackService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 service tests. Each test checks the LAST step's data —
 * the trace must end in the correct final state.
 */
class Phase2ServiceTest {

    private final SortingService sortingService = new SortingService();
    private final StackService stackService = new StackService();
    private final QueueService queueService = new QueueService();
    private final LinkedListService linkedListService = new LinkedListService();
    private final BstService bstService = new BstService();

    /** Final data of the last step. */
    private List<Integer> lastData(List<Step> steps) {
        return steps.get(steps.size() - 1).getData();
    }

    @Test
    void mergeSortSorts() {
        List<Step> steps = sortingService.mergeSort(List.of(5, 2, 8, 1));
        assertEquals(List.of(1, 2, 5, 8), lastData(steps));
    }

    @Test
    void quickSortSorts() {
        List<Step> steps = sortingService.quickSort(List.of(3, 1, 2));
        assertEquals(List.of(1, 2, 3), lastData(steps));
    }

    @Test
    void stackPushAppendsOnTop() {
        List<Step> steps = stackService.push(List.of(1, 2), 3);
        assertEquals(List.of(1, 2, 3), lastData(steps));
    }

    @Test
    void stackPopRemovesTop() {
        List<Step> steps = stackService.pop(List.of(1, 2));
        assertEquals(List.of(1), lastData(steps));
    }

    @Test
    void queueEnqueueAppendsAtRear() {
        List<Step> steps = queueService.enqueue(List.of(1, 2), 3);
        assertEquals(List.of(1, 2, 3), lastData(steps));
    }

    @Test
    void queueDequeueRemovesFront() {
        List<Step> steps = queueService.dequeue(List.of(1, 2));
        assertEquals(List.of(2), lastData(steps));
    }

    @Test
    void listInsertAtIndex() {
        List<Step> steps = linkedListService.insert(List.of(1, 3), 2, 1);
        assertEquals(List.of(1, 2, 3), lastData(steps));
    }

    @Test
    void listDeleteAtIndex() {
        List<Step> steps = linkedListService.delete(List.of(1, 2, 3), 1);
        assertEquals(List.of(1, 3), lastData(steps));
    }

    @Test
    void bstInsertAddsNodeAsChildOfThree() {
        List<Step> steps = bstService.insert(List.of(5, 3, 7), 4);
        Step last = steps.get(steps.size() - 1);
        assertEquals(4, last.getNodes().size());

        int idOfThree = -1;
        int idOfFour = -1;
        for (NodeDto node : last.getNodes()) {
            if (node.getValue() == 3) {
                idOfThree = node.getId();
            }
            if (node.getValue() == 4) {
                idOfFour = node.getId();
            }
        }
        assertTrue(idOfThree >= 0, "Node with value 3 must be present");
        assertTrue(idOfFour >= 0, "Node with value 4 must be present");

        boolean edgeFound = false;
        for (EdgeDto edge : last.getEdges()) {
            if (edge.getFrom() == idOfThree && edge.getTo() == idOfFour) {
                edgeFound = true;
            }
        }
        assertTrue(edgeFound,
                "Expected an edge from the node with value 3 "
                        + "to the node with value 4");
    }

    @Test
    void bstSearchFindsTarget() {
        List<Step> steps = bstService.search(List.of(5, 3, 7), 7);
        String lastMessage = steps.get(steps.size() - 1).getMessage();
        assertTrue(lastMessage.contains("Found"),
                "Last step should say the target was found, was: "
                        + lastMessage);
    }
}
