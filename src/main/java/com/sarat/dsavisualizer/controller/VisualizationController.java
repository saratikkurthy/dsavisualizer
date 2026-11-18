package com.sarat.dsavisualizer.controller;

import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.model.TraceResponse;
import com.sarat.dsavisualizer.service.AvlService;
import com.sarat.dsavisualizer.service.BstService;
import com.sarat.dsavisualizer.service.CountingService;
import com.sarat.dsavisualizer.service.GraphService;
import com.sarat.dsavisualizer.service.HashTableService;
import com.sarat.dsavisualizer.service.HeapService;
import com.sarat.dsavisualizer.service.LinkedListService;
import com.sarat.dsavisualizer.service.QueueService;
import com.sarat.dsavisualizer.service.RadixService;
import com.sarat.dsavisualizer.service.SearchService;
import com.sarat.dsavisualizer.service.SortingService;
import com.sarat.dsavisualizer.service.StackService;
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
 *
 * Validation rule (Phase 2): sorting endpoints 400 when values are
 * missing/empty. Stack push, queue enqueue and BST insert treat an
 * empty values list as "start empty" and only 400 when the required
 * value is missing. Pop, dequeue, list delete and list/BST search
 * return the service's single "empty" Step with 200 on an empty
 * list, and only 400 when the required index/target is missing.
 * Validation rule (Phase 3): heap insert, hash insert and AVL insert
 * only 400 when the required value is missing (empty values =
 * "start empty"). Hash search 400s when the target is missing.
 * Graph BFS/DFS 400 when graphEdges or start is missing.
 * Heap extract-min returns the service's single "empty" Step
 * with 200 on an empty heap.
 * Validation rule (Phase 4): counting/radix 400 when values are
 * missing/empty, any value is negative, or the max exceeds the
 * limit (counting 100, radix 999999).
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VisualizationController {

    private final SortingService sortingService;
    private final SearchService searchService;
    private final StackService stackService;
    private final QueueService queueService;
    private final LinkedListService linkedListService;
    private final BstService bstService;
    private final HeapService heapService;
    private final HashTableService hashTableService;
    private final GraphService graphService;
    private final AvlService avlService;
    private final CountingService countingService;
    private final RadixService radixService;

    public VisualizationController(SortingService sortingService,
                                   SearchService searchService,
                                   StackService stackService,
                                   QueueService queueService,
                                   LinkedListService linkedListService,
                                   BstService bstService,
                                   HeapService heapService,
                                   HashTableService hashTableService,
                                   GraphService graphService,
                                   AvlService avlService,
                                   CountingService countingService,
                                   RadixService radixService) {
        this.sortingService = sortingService;
        this.searchService = searchService;
        this.stackService = stackService;
        this.queueService = queueService;
        this.linkedListService = linkedListService;
        this.bstService = bstService;
        this.heapService = heapService;
        this.hashTableService = hashTableService;
        this.graphService = graphService;
        this.avlService = avlService;
        this.countingService = countingService;
        this.radixService = radixService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    // ---- Sorting (Phase 1) ----

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

    // ---- Sorting (Phase 2) ----

    @PostMapping("/sort/merge")
    public ResponseEntity<?> mergeSort(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = sortingService.mergeSort(request.getValues());
        return ResponseEntity.ok(new TraceResponse("Merge Sort", steps));
    }

    @PostMapping("/sort/quick")
    public ResponseEntity<?> quickSort(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = sortingService.quickSort(request.getValues());
        return ResponseEntity.ok(new TraceResponse("Quick Sort", steps));
    }

    // ---- Sorting (Phase 4) ----

    @PostMapping("/sort/counting")
    public ResponseEntity<?> countingSort(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateCountingRadix(
                request, 100, "Counting sort");
        if (error != null) {
            return error;
        }
        List<Step> steps = countingService.countingSort(request.getValues());
        return ResponseEntity.ok(new TraceResponse("Counting Sort", steps));
    }

    @PostMapping("/sort/radix")
    public ResponseEntity<?> radixSort(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateCountingRadix(
                request, 999999, "Radix sort");
        if (error != null) {
            return error;
        }
        List<Step> steps = radixService.radixSort(request.getValues());
        return ResponseEntity.ok(new TraceResponse("Radix Sort", steps));
    }

    // ---- Search (Phase 1) ----

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

    // ---- Stack (Phase 2) ----

    @PostMapping("/stack/push")
    public ResponseEntity<?> stackPush(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValue(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = stackService.push(
                valuesOf(request), request.getValue());
        return ResponseEntity.ok(new TraceResponse("Stack Push", steps));
    }

    @PostMapping("/stack/pop")
    public ResponseEntity<?> stackPop(@RequestBody TraceRequest request) {
        List<Step> steps = stackService.pop(valuesOf(request));
        return ResponseEntity.ok(new TraceResponse("Stack Pop", steps));
    }

    // ---- Queue (Phase 2) ----

    @PostMapping("/queue/enqueue")
    public ResponseEntity<?> queueEnqueue(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValue(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = queueService.enqueue(
                valuesOf(request), request.getValue());
        return ResponseEntity.ok(new TraceResponse("Queue Enqueue", steps));
    }

    @PostMapping("/queue/dequeue")
    public ResponseEntity<?> queueDequeue(@RequestBody TraceRequest request) {
        List<Step> steps = queueService.dequeue(valuesOf(request));
        return ResponseEntity.ok(new TraceResponse("Queue Dequeue", steps));
    }

    // ---- Linked list (Phase 2) ----

    @PostMapping("/list/insert")
    public ResponseEntity<?> listInsert(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValue(request);
        if (error != null) {
            return error;
        }
        error = validateIndex(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = linkedListService.insert(
                valuesOf(request), request.getValue(), request.getIndex());
        return ResponseEntity.ok(
                new TraceResponse("Linked List Insert", steps));
    }

    @PostMapping("/list/delete")
    public ResponseEntity<?> listDelete(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateIndex(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = linkedListService.delete(
                valuesOf(request), request.getIndex());
        return ResponseEntity.ok(
                new TraceResponse("Linked List Delete", steps));
    }

    @PostMapping("/list/search")
    public ResponseEntity<?> listSearch(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateTargetOnly(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = linkedListService.search(
                valuesOf(request), request.getTarget());
        return ResponseEntity.ok(
                new TraceResponse("Linked List Search", steps));
    }

    // ---- BST (Phase 2) ----

    @PostMapping("/bst/insert")
    public ResponseEntity<?> bstInsert(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValue(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = bstService.insert(
                valuesOf(request), request.getValue());
        return ResponseEntity.ok(new TraceResponse("BST Insert", steps));
    }

    @PostMapping("/bst/search")
    public ResponseEntity<?> bstSearch(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateTargetOnly(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = bstService.search(
                valuesOf(request), request.getTarget());
        return ResponseEntity.ok(new TraceResponse("BST Search", steps));
    }

    // ---- Min-Heap (Phase 3) ----

    @PostMapping("/heap/insert")
    public ResponseEntity<?> heapInsert(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValue(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = heapService.insert(
                valuesOf(request), request.getValue());
        return ResponseEntity.ok(new TraceResponse("Min-Heap Insert", steps));
    }

    @PostMapping("/heap/extract-min")
    public ResponseEntity<?> heapExtractMin(@RequestBody TraceRequest request) {
        List<Step> steps = heapService.extractMin(valuesOf(request));
        return ResponseEntity.ok(
                new TraceResponse("Min-Heap Extract-Min", steps));
    }

    // ---- Hash table (Phase 3) ----

    @PostMapping("/hash/insert")
    public ResponseEntity<?> hashInsert(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValue(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = hashTableService.insert(
                valuesOf(request), request.getValue(), request.getCapacity());
        return ResponseEntity.ok(
                new TraceResponse("Hash Table Insert", steps));
    }

    @PostMapping("/hash/search")
    public ResponseEntity<?> hashSearch(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateTargetOnly(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = hashTableService.search(
                valuesOf(request), request.getTarget(), request.getCapacity());
        return ResponseEntity.ok(
                new TraceResponse("Hash Table Search", steps));
    }

    // ---- Graph BFS/DFS (Phase 3) ----

    @PostMapping("/graph/bfs")
    public ResponseEntity<?> graphBfs(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateGraph(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = graphService.bfs(
                request.getGraphEdges(), request.getStart());
        return ResponseEntity.ok(new TraceResponse("Graph BFS", steps));
    }

    @PostMapping("/graph/dfs")
    public ResponseEntity<?> graphDfs(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateGraph(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = graphService.dfs(
                request.getGraphEdges(), request.getStart());
        return ResponseEntity.ok(new TraceResponse("Graph DFS", steps));
    }

    // ---- AVL tree (Phase 3) ----

    @PostMapping("/avl/insert")
    public ResponseEntity<?> avlInsert(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValue(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = avlService.insert(
                valuesOf(request), request.getValue());
        return ResponseEntity.ok(new TraceResponse("AVL Insert", steps));
    }

    @PostMapping("/avl/search")
    public ResponseEntity<?> avlSearch(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateTargetOnly(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = avlService.search(
                valuesOf(request), request.getTarget());
        return ResponseEntity.ok(new TraceResponse("AVL Search", steps));
    }

    // ---- Validation helpers ----

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

    /** 400 when the value (push/enqueue/insert) is missing. */
    private ResponseEntity<?> validateValue(TraceRequest request) {
        if (request == null || request.getValue() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "value must not be null"));
        }
        return null;
    }

    /** 400 when the index (list insert/delete) is missing. */
    private ResponseEntity<?> validateIndex(TraceRequest request) {
        if (request == null || request.getIndex() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "index must not be null"));
        }
        return null;
    }

    /** 400 when the target (list/BST search) is missing. */
    private ResponseEntity<?> validateTargetOnly(TraceRequest request) {
        if (request == null || request.getTarget() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "target must not be null"));
        }
        return null;
    }

    /** 400 when graphEdges is missing/empty or the start node is missing. */
    private ResponseEntity<?> validateGraph(TraceRequest request) {
        if (request == null || request.getGraphEdges() == null
                || request.getGraphEdges().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "graphEdges must not be empty"));
        }
        if (request.getStart() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "start must not be null"));
        }
        return null;
    }

    /**
     * 400 when values are missing/empty, any value is negative, or
     * the max exceeds {@code maxAllowed} (counting: 100, radix:
     * 999999). Null means "no error".
     */
    private ResponseEntity<?> validateCountingRadix(TraceRequest request,
                                                    int maxAllowed,
                                                    String algorithm) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        int max = Integer.MIN_VALUE;
        for (int v : request.getValues()) {
            if (v < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", algorithm
                                + " needs non-negative values"));
            }
            if (v > max) {
                max = v;
            }
        }
        if (max > maxAllowed) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", algorithm
                            + " needs values with max <= " + maxAllowed));
        }
        return null;
    }

    /** Null-safe values accessor (null request or null values → null). */
    private List<Integer> valuesOf(TraceRequest request) {
        return request == null ? null : request.getValues();
    }
}
