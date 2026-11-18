package com.sarat.dsavisualizer.controller;

import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.model.TraceResponse;
import com.sarat.dsavisualizer.service.SearchService;
import com.sarat.dsavisualizer.service.SortingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Trace endpoints. Each POST returns a TraceResponse the frontend
 * plays back step by step.
 *
 * CORS: allow-all for now (dev). In prod, restrict to the Vercel
 * domain — see DESIGN.md.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VisualizationController {

    private final SortingService sortingService;
    private final SearchService searchService;

    public VisualizationController(SortingService sortingService,
                                   SearchService searchService) {
        this.sortingService = sortingService;
        this.searchService = searchService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/sort/bubble")
    public ResponseEntity<?> bubbleSort(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = sortingService.bubbleSort(request.getValues());
        return ResponseEntity.ok(new TraceResponse("Bubble Sort", steps));
    }

    @PostMapping("/sort/selection")
    public ResponseEntity<?> selectionSort(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = sortingService.selectionSort(request.getValues());
        return ResponseEntity.ok(new TraceResponse("Selection Sort", steps));
    }

    @PostMapping("/sort/insertion")
    public ResponseEntity<?> insertionSort(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = sortingService.insertionSort(request.getValues());
        return ResponseEntity.ok(new TraceResponse("Insertion Sort", steps));
    }

    @PostMapping("/search/linear")
    public ResponseEntity<?> linearSearch(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateSearch(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = searchService.linearSearch(
                request.getValues(), request.getTarget());
        return ResponseEntity.ok(new TraceResponse("Linear Search", steps));
    }

    @PostMapping("/search/binary")
    public ResponseEntity<?> binarySearch(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateSearch(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = searchService.binarySearch(
                request.getValues(), request.getTarget());
        return ResponseEntity.ok(new TraceResponse("Binary Search", steps));
    }

    /** 400 when values are missing or empty. Null means "no error". */
    private ResponseEntity<?> validateValues(TraceRequest request) {
        if (request == null || request.getValues() == null
                || request.getValues().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "values must not be empty"));
        }
        return null;
    }

    /** 400 when values are missing/empty or the target is missing. */
    private ResponseEntity<?> validateSearch(TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        if (request.getTarget() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "target must not be null"));
        }
        return null;
    }
}
