package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.service.SearchService;
import com.sarat.dsavisualizer.service.SortingService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the trace services (no Spring context needed). */
class SortingServiceTest {

	private final SortingService sortingService = new SortingService();
	private final SearchService searchService = new SearchService();

	@Test
	void bubbleSortEndsSorted() {
		List<Step> steps = sortingService.bubbleSort(List.of(5, 2, 8, 1));
		assertEquals(List.of(1, 2, 5, 8), steps.get(steps.size() - 1).getData());
	}

	@Test
	void bubbleSortEmptyReturnsOneStep() {
		List<Step> steps = sortingService.bubbleSort(List.of());
		assertEquals(1, steps.size());
	}

	@Test
	void selectionSortEndsSorted() {
		List<Step> steps = sortingService.selectionSort(List.of(3, 1, 2));
		assertEquals(List.of(1, 2, 3), steps.get(steps.size() - 1).getData());
	}

	@Test
	void insertionSortEndsSorted() {
		List<Step> steps = sortingService.insertionSort(List.of(3, 1, 2));
		assertEquals(List.of(1, 2, 3), steps.get(steps.size() - 1).getData());
	}

	@Test
	void linearSearchFindsTarget() {
		List<Step> steps = searchService.linearSearch(List.of(4, 7, 2, 9), 2);
		String lastMessage = steps.get(steps.size() - 1).getMessage();
		assertTrue(lastMessage.contains("Found"));
	}

	@Test
	void binarySearchFindsTarget() {
		List<Step> steps = searchService.binarySearch(List.of(1, 2, 5, 8), 5);
		String lastMessage = steps.get(steps.size() - 1).getMessage();
		assertTrue(lastMessage.contains("Found"));
	}
}
