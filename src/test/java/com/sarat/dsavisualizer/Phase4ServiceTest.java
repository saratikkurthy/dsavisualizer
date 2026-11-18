package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.service.CountingService;
import com.sarat.dsavisualizer.service.RadixService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Phase 4 service tests. Each test checks the LAST step's data —
 * the trace must end in the correct sorted array.
 */
class Phase4ServiceTest {

    private final CountingService countingService = new CountingService();
    private final RadixService radixService = new RadixService();

    /** Final data of the last step. */
    private List<Integer> lastData(List<Step> steps) {
        return steps.get(steps.size() - 1).getData();
    }

    @Test
    void countingSortClassicExample() {
        List<Step> steps = countingService.countingSort(
                List.of(4, 2, 2, 8, 3, 3, 1));
        assertEquals(List.of(1, 2, 2, 3, 3, 4, 8), lastData(steps));
    }

    @Test
    void countingSortSmall() {
        List<Step> steps = countingService.countingSort(List.of(3, 1, 2));
        assertEquals(List.of(1, 2, 3), lastData(steps));
    }

    @Test
    void countingSortSingleElement() {
        List<Step> steps = countingService.countingSort(List.of(5));
        assertEquals(List.of(5), lastData(steps));
    }

    @Test
    void radixSortClassicExample() {
        // The classic example — 3 passes (ones, tens, hundreds).
        List<Step> steps = radixService.radixSort(
                List.of(170, 45, 75, 90, 802, 24, 2, 66));
        assertEquals(List.of(2, 24, 45, 66, 75, 90, 170, 802),
                lastData(steps));
    }

    @Test
    void radixSortSmall() {
        List<Step> steps = radixService.radixSort(List.of(3, 1, 2));
        assertEquals(List.of(1, 2, 3), lastData(steps));
    }

    @Test
    void radixSortSinglePass() {
        // All single-digit, so one pass (ones place) sorts it.
        List<Step> steps = radixService.radixSort(List.of(5, 2, 8));
        assertEquals(List.of(2, 5, 8), lastData(steps));
    }
}
