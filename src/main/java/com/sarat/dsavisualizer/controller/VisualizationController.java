package com.sarat.dsavisualizer.controller;

import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.model.TraceResponse;
import com.sarat.dsavisualizer.service.AvlService;
import com.sarat.dsavisualizer.service.BacktrackingService;
import com.sarat.dsavisualizer.service.BstService;
import com.sarat.dsavisualizer.service.BucketSortService;
import com.sarat.dsavisualizer.service.CountingService;
import com.sarat.dsavisualizer.service.DpService;
import com.sarat.dsavisualizer.service.GraphService;
import com.sarat.dsavisualizer.service.HashTableService;
import com.sarat.dsavisualizer.service.HeapService;
import com.sarat.dsavisualizer.service.HeapSortService;
import com.sarat.dsavisualizer.service.KmpService;
import com.sarat.dsavisualizer.service.KnapsackService;
import com.sarat.dsavisualizer.service.LinkedListService;
import com.sarat.dsavisualizer.service.QueueService;
import com.sarat.dsavisualizer.service.RabinKarpService;
import com.sarat.dsavisualizer.service.RadixService;
import com.sarat.dsavisualizer.service.RedBlackService;
import com.sarat.dsavisualizer.service.SearchService;
import com.sarat.dsavisualizer.service.SegmentTreeService;
import com.sarat.dsavisualizer.service.ShellSortService;
import com.sarat.dsavisualizer.service.SortingService;
import com.sarat.dsavisualizer.service.StackService;
import com.sarat.dsavisualizer.service.TrieService;
import com.sarat.dsavisualizer.service.UnionFindService;
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
 * Validation rule (Phase 7): heapsort 400 when values are
 * missing/empty. Dijkstra 400 when graphEdges/start is missing or
 * any weight is <= 0 (Dijkstra needs non-negative weights). BST/AVL
 * delete 400 when the value is missing; an empty tree returns the
 * service's single "empty" Step with 200.
 * Validation rule (Phase 8): shell sort 400 when values are
 * missing/empty. Prim's 400 when graphEdges/start is missing or any
 * weight is <= 0. Kruskal's 400 when graphEdges is missing/empty
 * or any weight is <= 0. Trie insert/search 400 when the word is
 * missing/empty.
 * Validation rule (Phase 10): Bellman-Ford 400 when graphEdges/
 * start is missing — negative and zero weights are legal, so there
 * is no weight validation. Topological sort 400 when graphEdges is
 * missing/empty. Knapsack 400 when weights/values are missing/
 * empty, their lengths differ, the capacity is missing, negative
 * or over 30, there are more than 10 items, any weight is <= 0,
 * or any value is negative.
 * Validation rule (Phase 11): A* 400 when the grid is
 * missing/empty, rows differ in length, startCell/goalCell is
 * missing or not [row, col], or start/goal is out of bounds or on
 * a wall. Red-black insert 400 when values are missing/empty;
 * red-black search 400 when values are missing/empty or the
 * target is missing. LCS 400 when words is not exactly two
 * non-empty strings or either string is longer than 12
 * characters. Coin change 400 when values are missing/empty, the
 * target is missing, negative or over 50, or any coin is <= 0.
 * Validation rule (Phase 12): red-black delete 400 when values
 * are missing/empty or the value is missing. Weighted A* 400 when
 * the cost grid is missing/empty, rows differ in length, any cost
 * is outside 0-9, or startCell/goalCell is missing, not
 * [row, col], out of bounds, or on a wall (a cost-0 cell).
 * Edit distance reuses the LCS validation. LPS 400 when words is
 * not exactly one non-empty string or the string is longer than
 * 12 characters. Floyd-Warshall 400 when graphEdges is
 * missing/empty, any edge is not [from, to] or [from, to,
 * weight], any node id is missing, below 0 or above 9, or a
 * 3-element edge has a null weight; negative weights are allowed.
 * Validation rule (Phase 13): segment tree build/query/update 400
 * when values are missing/empty, longer than 8, or any |value| is
 * over 999; query also 400 when index/target is missing, out of
 * range, or left &gt; right; update also 400 when index is missing
 * or out of range or value is missing. Union-Find 400 when values
 * are missing/empty or longer than 8, graphEdges is missing/empty,
 * any pair is not [a, b] of two distinct elements from the set.
 * KMP 400 when words is not exactly two non-empty strings, the
 * text is longer than 40 characters, or the pattern is longer than
 * 12 characters or longer than the text. N-Queens 400 when value
 * is missing, below 1 or above 8.
 * Validation rule (Phase 15): bucket sort 400 when values are
 * missing/empty, longer than 10, or any value is outside 0..99.
 * Rabin-Karp reuses the KMP validation. LIS 400 when values are
 * missing/empty, longer than 10, or any |value| is over 999. MCM
 * 400 when values are missing/empty, shorter than 3 or longer
 * than 7, or any dimension is outside 1..99. Trie delete reuses
 * the trie word validation (400 when the word is missing/empty).
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
    private final HeapSortService heapSortService;
    private final ShellSortService shellSortService;
    private final TrieService trieService;
    private final KnapsackService knapsackService;
    private final RedBlackService redBlackService;
    private final DpService dpService;
    private final SegmentTreeService segmentTreeService;
    private final UnionFindService unionFindService;
    private final KmpService kmpService;
    private final BacktrackingService backtrackingService;
    private final BucketSortService bucketSortService;
    private final RabinKarpService rabinKarpService;

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
                                   RadixService radixService,
                                   HeapSortService heapSortService,
                                   ShellSortService shellSortService,
                                   TrieService trieService,
                                   KnapsackService knapsackService,
                                   RedBlackService redBlackService,
                                   DpService dpService,
                                   SegmentTreeService segmentTreeService,
                                   UnionFindService unionFindService,
                                   KmpService kmpService,
                                   BacktrackingService backtrackingService,
                                   BucketSortService bucketSortService,
                                   RabinKarpService rabinKarpService) {
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
        this.heapSortService = heapSortService;
        this.shellSortService = shellSortService;
        this.trieService = trieService;
        this.knapsackService = knapsackService;
        this.redBlackService = redBlackService;
        this.dpService = dpService;
        this.segmentTreeService = segmentTreeService;
        this.unionFindService = unionFindService;
        this.kmpService = kmpService;
        this.backtrackingService = backtrackingService;
        this.bucketSortService = bucketSortService;
        this.rabinKarpService = rabinKarpService;
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

    // ---- Sorting (Phase 7) ----

    @PostMapping("/sort/heapsort")
    public ResponseEntity<?> heapSort(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = heapSortService.heapSort(request.getValues());
        return ResponseEntity.ok(new TraceResponse("Heap Sort", steps));
    }

    // ---- Sorting (Phase 8) ----

    @PostMapping("/sort/shell")
    public ResponseEntity<?> shellSort(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = shellSortService.shellSort(request.getValues());
        return ResponseEntity.ok(new TraceResponse("Shell Sort", steps));
    }

    // ---- Sorting (Phase 15) ----

    @PostMapping("/sort/bucket")
    public ResponseEntity<?> bucketSort(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateBucketSort(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = bucketSortService.bucketSort(
                request.getValues());
        return ResponseEntity.ok(new TraceResponse("Bucket Sort", steps));
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

    // ---- BST delete (Phase 7) ----

    @PostMapping("/bst/delete")
    public ResponseEntity<?> bstDelete(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValue(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = bstService.delete(
                valuesOf(request), request.getValue());
        return ResponseEntity.ok(new TraceResponse("BST Delete", steps));
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

    // ---- Graph Dijkstra (Phase 7) ----

    @PostMapping("/graph/dijkstra")
    public ResponseEntity<?> graphDijkstra(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateDijkstra(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = graphService.dijkstra(
                request.getGraphEdges(), request.getStart());
        return ResponseEntity.ok(
                new TraceResponse("Graph Dijkstra", steps));
    }

    // ---- Graph Prim's / Kruskal's MST (Phase 8) ----

    @PostMapping("/graph/prim")
    public ResponseEntity<?> graphPrim(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validatePrim(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = graphService.prim(
                request.getGraphEdges(), request.getStart());
        return ResponseEntity.ok(
                new TraceResponse("Graph Prim's", steps));
    }

    @PostMapping("/graph/kruskal")
    public ResponseEntity<?> graphKruskal(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateKruskal(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = graphService.kruskal(request.getGraphEdges());
        return ResponseEntity.ok(
                new TraceResponse("Graph Kruskal's", steps));
    }

    // ---- Graph Bellman-Ford / Topological sort (Phase 10) ----

    @PostMapping("/graph/bellmanford")
    public ResponseEntity<?> graphBellmanFord(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateBellmanFord(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = graphService.bellmanFord(
                request.getGraphEdges(), request.getStart());
        return ResponseEntity.ok(
                new TraceResponse("Graph Bellman-Ford", steps));
    }

    @PostMapping("/graph/toposort")
    public ResponseEntity<?> graphTopoSort(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateTopoSort(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = graphService.topoSort(request.getGraphEdges());
        return ResponseEntity.ok(
                new TraceResponse("Graph Topological Sort", steps));
    }

    // ---- A* pathfinding (Phase 11) ----

    @PostMapping("/graph/astar")
    public ResponseEntity<?> graphAStar(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateAStar(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = graphService.aStar(
                request.getGrid(), request.getStartCell(),
                request.getGoalCell());
        return ResponseEntity.ok(
                new TraceResponse("A* Pathfinding", steps));
    }

    // ---- Weighted A* pathfinding (Phase 12) ----

    @PostMapping("/graph/astarweighted")
    public ResponseEntity<?> graphAStarWeighted(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateAStarWeighted(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = graphService.aStarWeighted(
                request.getCostGrid(), request.getStartCell(),
                request.getGoalCell());
        return ResponseEntity.ok(
                new TraceResponse("Weighted A* Pathfinding", steps));
    }

    // ---- Floyd-Warshall all-pairs shortest paths (Phase 12) ----

    @PostMapping("/graph/floydwarshall")
    public ResponseEntity<?> graphFloydWarshall(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateFloydWarshall(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = graphService.floydWarshall(
                request.getGraphEdges());
        return ResponseEntity.ok(
                new TraceResponse("Floyd-Warshall", steps));
    }

    // ---- 0/1 Knapsack (Phase 10) ----

    @PostMapping("/dp/knapsack")
    public ResponseEntity<?> knapsack(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateKnapsack(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = knapsackService.knapsack(
                request.getWeights(), request.getValues(),
                request.getCapacity());
        return ResponseEntity.ok(
                new TraceResponse("0/1 Knapsack", steps));
    }

    // ---- Longest common subsequence / Coin change (Phase 11) ----

    @PostMapping("/dp/lcs")
    public ResponseEntity<?> lcs(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateLcs(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = dpService.lcs(request.getWords());
        return ResponseEntity.ok(
                new TraceResponse("Longest Common Subsequence", steps));
    }

    @PostMapping("/dp/coinchange")
    public ResponseEntity<?> coinChange(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateCoinChange(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = dpService.coinChange(
                request.getValues(), request.getTarget());
        return ResponseEntity.ok(
                new TraceResponse("Coin Change", steps));
    }

    // ---- Edit distance / Longest palindromic subsequence (Phase 12) ----

    @PostMapping("/dp/editdistance")
    public ResponseEntity<?> editDistance(
            @RequestBody TraceRequest request) {
        // Same validation as LCS: exactly two non-empty strings,
        // each at most 12 characters.
        ResponseEntity<?> error = validateLcs(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = dpService.editDistance(request.getWords());
        return ResponseEntity.ok(
                new TraceResponse("Edit Distance", steps));
    }

    @PostMapping("/dp/lps")
    public ResponseEntity<?> lps(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateLps(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = dpService.lps(request.getWords());
        return ResponseEntity.ok(
                new TraceResponse("Longest Palindromic Subsequence",
                        steps));
    }

    // ---- Longest increasing subsequence / Matrix chain
    // multiplication (Phase 15) ----

    @PostMapping("/dp/lis")
    public ResponseEntity<?> lis(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateLis(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = dpService.lis(request.getValues());
        return ResponseEntity.ok(
                new TraceResponse("Longest Increasing Subsequence",
                        steps));
    }

    @PostMapping("/dp/mcm")
    public ResponseEntity<?> mcm(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateMcm(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = dpService.mcm(request.getValues());
        return ResponseEntity.ok(
                new TraceResponse("Matrix Chain Multiplication", steps));
    }

    // ---- Segment tree (Phase 13) ----

    @PostMapping("/segmenttree/build")
    public ResponseEntity<?> segmentTreeBuild(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateSegmentTree(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = segmentTreeService.build(
                valuesOf(request));
        return ResponseEntity.ok(
                new TraceResponse("Segment Tree Build", steps));
    }

    @PostMapping("/segmenttree/query")
    public ResponseEntity<?> segmentTreeQuery(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateSegmentTreeQuery(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = segmentTreeService.query(
                valuesOf(request), request.getIndex(),
                request.getTarget());
        return ResponseEntity.ok(
                new TraceResponse("Segment Tree Range Sum", steps));
    }

    @PostMapping("/segmenttree/update")
    public ResponseEntity<?> segmentTreeUpdate(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateSegmentTreeUpdate(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = segmentTreeService.update(
                valuesOf(request), request.getIndex(),
                request.getValue());
        return ResponseEntity.ok(
                new TraceResponse("Segment Tree Update", steps));
    }

    // ---- Union-Find (Phase 13) ----

    @PostMapping("/unionfind/union")
    public ResponseEntity<?> unionFind(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateUnionFind(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = unionFindService.union(
                valuesOf(request), request.getGraphEdges());
        return ResponseEntity.ok(
                new TraceResponse("Union-Find", steps));
    }

    // ---- KMP string matching (Phase 13) ----

    @PostMapping("/string/kmp")
    public ResponseEntity<?> kmp(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateKmp(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = kmpService.kmp(request.getWords());
        return ResponseEntity.ok(
                new TraceResponse("KMP String Matching", steps));
    }

    // ---- Rabin-Karp string matching (Phase 15) ----

    @PostMapping("/string/rabinkarp")
    public ResponseEntity<?> rabinKarp(@RequestBody TraceRequest request) {
        // Same validation bounds as KMP: exactly two non-empty
        // strings, text at most 40 characters, pattern at most 12
        // and no longer than the text.
        ResponseEntity<?> error = validateKmp(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = rabinKarpService.rabinKarp(
                request.getWords());
        return ResponseEntity.ok(
                new TraceResponse("Rabin-Karp String Matching", steps));
    }

    // ---- N-Queens backtracking (Phase 13) ----

    @PostMapping("/backtracking/nqueens")
    public ResponseEntity<?> nQueens(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateNQueens(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = backtrackingService.nQueens(
                request.getValue());
        return ResponseEntity.ok(
                new TraceResponse("N-Queens", steps));
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

    // ---- AVL delete (Phase 7) ----

    @PostMapping("/avl/delete")
    public ResponseEntity<?> avlDelete(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValue(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = avlService.delete(
                valuesOf(request), request.getValue());
        return ResponseEntity.ok(new TraceResponse("AVL Delete", steps));
    }

    // ---- Red-black tree (Phase 11) ----

    @PostMapping("/rbtree/insert")
    public ResponseEntity<?> redBlackInsert(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = redBlackService.insert(
                valuesOf(request));
        return ResponseEntity.ok(
                new TraceResponse("Red-Black Tree Insert", steps));
    }

    @PostMapping("/rbtree/search")
    public ResponseEntity<?> redBlackSearch(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateSearch(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = redBlackService.search(
                valuesOf(request), request.getTarget());
        return ResponseEntity.ok(
                new TraceResponse("Red-Black Tree Search", steps));
    }

    // ---- Red-black tree delete (Phase 12) ----

    @PostMapping("/rbtree/delete")
    public ResponseEntity<?> redBlackDelete(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateRbtreeDelete(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = redBlackService.delete(
                valuesOf(request), request.getValue());
        return ResponseEntity.ok(
                new TraceResponse("Red-Black Tree Delete", steps));
    }

    // ---- Trie (Phase 8) ----

    @PostMapping("/trie/insert")
    public ResponseEntity<?> trieInsert(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateWord(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = trieService.insert(
                request.getWords(), request.getWord());
        return ResponseEntity.ok(new TraceResponse("Trie Insert", steps));
    }

    @PostMapping("/trie/search")
    public ResponseEntity<?> trieSearch(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateWord(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = trieService.search(
                request.getWords(), request.getWord());
        return ResponseEntity.ok(new TraceResponse("Trie Search", steps));
    }

    // ---- Trie delete (Phase 15) ----

    @PostMapping("/trie/delete")
    public ResponseEntity<?> trieDelete(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateWord(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = trieService.delete(
                request.getWords(), request.getWord());
        return ResponseEntity.ok(new TraceResponse("Trie Delete", steps));
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
     * 400 when graphEdges/start is missing or any weight is <= 0
     * (Dijkstra needs non-negative weights). A null weight defaults
     * to 1 and is not an error. Null means "no error".
     */
    private ResponseEntity<?> validateDijkstra(TraceRequest request) {
        ResponseEntity<?> error = validateGraph(request);
        if (error != null) {
            return error;
        }
        return validateWeights(request, "Dijkstra");
    }

    /**
     * 400 when graphEdges/start is missing or any weight is <= 0
     * (Prim's needs non-negative weights). Null means "no error".
     */
    private ResponseEntity<?> validatePrim(TraceRequest request) {
        ResponseEntity<?> error = validateGraph(request);
        if (error != null) {
            return error;
        }
        return validateWeights(request, "Prim's");
    }

    /**
     * 400 when graphEdges is missing/empty or any weight is <= 0
     * (Kruskal's needs non-negative weights). Null means "no error".
     */
    private ResponseEntity<?> validateKruskal(TraceRequest request) {
        if (request == null || request.getGraphEdges() == null
                || request.getGraphEdges().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "graphEdges must not be empty"));
        }
        return validateWeights(request, "Kruskal's");
    }

    /**
     * 400 when any 3-element edge has a weight <= 0 (a null weight
     * defaults to 1 and is not an error). Null means "no error".
     */
    private ResponseEntity<?> validateWeights(TraceRequest request,
                                              String algorithm) {
        for (List<Integer> pair : request.getGraphEdges()) {
            if (pair != null && pair.size() == 3 && pair.get(2) != null
                    && pair.get(2) <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", algorithm
                                + " needs non-negative weights"));
            }
        }
        return null;
    }

    /** 400 when the word (trie insert/search) is missing or empty. */
    private ResponseEntity<?> validateWord(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        return null;
    }

    /**
     * 400 when graphEdges/start is missing. Bellman-Ford takes
     * negative and zero weights — there is no weight validation
     * at all. Null means "no error".
     */
    private ResponseEntity<?> validateBellmanFord(TraceRequest request) {
        return validateGraph(request);
    }

    /**
     * 400 when graphEdges is missing/empty (topological sort takes
     * directed edges; weights are ignored). Null means "no error".
     */
    private ResponseEntity<?> validateTopoSort(TraceRequest request) {
        if (request == null || request.getGraphEdges() == null
                || request.getGraphEdges().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "graphEdges must not be empty"));
        }
        return null;
    }

    /**
     * 400 when weights/values are missing/empty, their lengths
     * differ, the capacity is missing, negative or over 30, there
     * are more than 10 items, any weight is <= 0, or any value is
     * negative. Null means "no error".
     */
    private ResponseEntity<?> validateKnapsack(TraceRequest request) {
        if (request == null || request.getWeights() == null
                || request.getWeights().isEmpty()
                || request.getValues() == null
                || request.getValues().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "weights and values must not be empty"));
        }
        if (request.getWeights().size() != request.getValues().size()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "weights and values must have the same length"));
        }
        if (request.getCapacity() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "capacity must not be null"));
        }
        if (request.getCapacity() < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "capacity must not be negative"));
        }
        if (request.getCapacity() > 30) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "capacity must be <= 30"));
        }
        if (request.getWeights().size() > 10) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "at most 10 items are allowed"));
        }
        for (int w : request.getWeights()) {
            if (w <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "weights must be greater than 0"));
            }
        }
        for (int v : request.getValues()) {
            if (v < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "values must not be negative"));
            }
        }
        return null;
    }

    /**
     * 400 when the grid is missing/empty, rows differ in length,
     * or startCell/goalCell is missing, not [row, col], out of
     * bounds, or on a wall. Null means "no error".
     */
    private ResponseEntity<?> validateAStar(TraceRequest request) {
        if (request == null || request.getGrid() == null
                || request.getGrid().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "grid must not be empty"));
        }
        int cols = -1;
        for (List<Integer> row : request.getGrid()) {
            if (row == null || row.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "grid rows must not be empty"));
            }
            if (cols == -1) {
                cols = row.size();
            } else if (row.size() != cols) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "grid rows must all have the same length"));
            }
        }
        ResponseEntity<?> error = validateCell(
                request.getStartCell(), request.getGrid(), "startCell");
        if (error != null) {
            return error;
        }
        return validateCell(
                request.getGoalCell(), request.getGrid(), "goalCell");
    }

    /**
     * 400 when a cell is missing, not [row, col], out of bounds,
     * or on a wall (any non-zero grid cell). Null means "no error".
     */
    private ResponseEntity<?> validateCell(List<Integer> cell,
                                           List<List<Integer>> grid,
                                           String name) {
        if (cell == null || cell.size() != 2
                || cell.get(0) == null || cell.get(1) == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            name + " must be [row, col]"));
        }
        int r = cell.get(0);
        int c = cell.get(1);
        if (r < 0 || r >= grid.size() || c < 0
                || c >= grid.get(0).size()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", name + " is out of bounds"));
        }
        Integer v = grid.get(r).get(c);
        if (v != null && v != 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            name + " must not be on a wall"));
        }
        return null;
    }

    /**
     * 400 when words is not exactly two non-empty strings or
     * either string is longer than 12 characters (the table must
     * stay drawable). Null means "no error".
     */
    private ResponseEntity<?> validateLcs(TraceRequest request) {
        if (request == null || request.getWords() == null
                || request.getWords().size() != 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "words must contain exactly two strings"));
        }
        for (String s : request.getWords()) {
            if (s == null || s.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "words must not be empty"));
            }
            if (s.length() > 12) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "each string must be at most 12 characters"));
            }
        }
        return null;
    }

    /**
     * 400 when values are missing/empty, the target is missing,
     * negative or over 50 (the array must stay drawable), or any
     * coin is <= 0. Null means "no error".
     */
    private ResponseEntity<?> validateCoinChange(TraceRequest request) {
        if (request == null || request.getValues() == null
                || request.getValues().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "values must not be empty"));
        }
        if (request.getTarget() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "target must not be null"));
        }
        if (request.getTarget() < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "target must not be negative"));
        }
        if (request.getTarget() > 50) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "target must be <= 50"));
        }
        for (int coin : request.getValues()) {
            if (coin <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "coins must be greater than 0"));
            }
        }
        return null;
    }

    /** 400 when values are missing/empty or the value is missing. */
    private ResponseEntity<?> validateRbtreeDelete(TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        if (request.getValue() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "value must not be null"));
        }
        return null;
    }

    /**
     * 400 when the cost grid is missing/empty, rows differ in
     * length, any cost is outside 0-9, or startCell/goalCell is
     * missing, not [row, col], out of bounds, or on a wall (a
     * cost-0 cell — the opposite of the binary grid, where a
     * non-zero cell is the wall). Null means "no error".
     */
    private ResponseEntity<?> validateAStarWeighted(
            TraceRequest request) {
        if (request == null || request.getCostGrid() == null
                || request.getCostGrid().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "costGrid must not be empty"));
        }
        int cols = -1;
        for (List<Integer> row : request.getCostGrid()) {
            if (row == null || row.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "costGrid rows must not be empty"));
            }
            if (cols == -1) {
                cols = row.size();
            } else if (row.size() != cols) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "costGrid rows must all have the same "
                                        + "length"));
            }
            for (Integer cost : row) {
                if (cost == null || cost < 0 || cost > 9) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error",
                                    "costs must be between 0 and 9"));
                }
            }
        }
        ResponseEntity<?> error = validateWeightedCell(
                request.getStartCell(), request.getCostGrid(),
                "startCell");
        if (error != null) {
            return error;
        }
        return validateWeightedCell(
                request.getGoalCell(), request.getCostGrid(), "goalCell");
    }

    /**
     * 400 when a cell is missing, not [row, col], out of bounds,
     * or on a wall (a cost-0 cell in the weighted grid). Null
     * means "no error".
     */
    private ResponseEntity<?> validateWeightedCell(
            List<Integer> cell, List<List<Integer>> costGrid,
            String name) {
        if (cell == null || cell.size() != 2
                || cell.get(0) == null || cell.get(1) == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            name + " must be [row, col]"));
        }
        int r = cell.get(0);
        int c = cell.get(1);
        if (r < 0 || r >= costGrid.size() || c < 0
                || c >= costGrid.get(0).size()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", name + " is out of bounds"));
        }
        Integer v = costGrid.get(r).get(c);
        if (v == null || v == 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            name + " must not be on a wall"));
        }
        return null;
    }

    /**
     * 400 when words is not exactly one non-empty string or the
     * string is longer than 12 characters (the table must stay
     * drawable). Null means "no error".
     */
    private ResponseEntity<?> validateLps(TraceRequest request) {
        if (request == null || request.getWords() == null
                || request.getWords().size() != 1) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "words must contain exactly one string"));
        }
        String s = request.getWords().get(0);
        if (s == null || s.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "words must not be empty"));
        }
        if (s.length() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "the string must be at most 12 characters"));
        }
        return null;
    }

    /**
     * 400 when values are missing/empty, longer than 8, or any
     * |value| is over 999 (the tree must stay drawable). Null
     * means "no error".
     */
    private ResponseEntity<?> validateSegmentTree(
            TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        if (request.getValues().size() > 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "segment tree needs at most 8 values"));
        }
        for (int v : request.getValues()) {
            if (Math.abs(v) > 999) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "segment tree needs |values| <= 999"));
            }
        }
        return null;
    }

    /**
     * 400 when the segment-tree values are invalid, or index
     * (left) / target (right) is missing, out of range, or left
     * is greater than right. Null means "no error".
     */
    private ResponseEntity<?> validateSegmentTreeQuery(
            TraceRequest request) {
        ResponseEntity<?> error = validateSegmentTree(request);
        if (error != null) {
            return error;
        }
        if (request.getIndex() == null || request.getTarget() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "index and target are required"));
        }
        int left = request.getIndex();
        int right = request.getTarget();
        int n = request.getValues().size();
        if (left < 0 || right < 0 || left >= n || right >= n
                || left > right) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "range must satisfy 0 <= left <= right < "
                                    + n));
        }
        return null;
    }

    /**
     * 400 when the segment-tree values are invalid, or index is
     * missing or out of range, or value is missing. Null means
     * "no error".
     */
    private ResponseEntity<?> validateSegmentTreeUpdate(
            TraceRequest request) {
        ResponseEntity<?> error = validateSegmentTree(request);
        if (error != null) {
            return error;
        }
        if (request.getIndex() == null || request.getValue() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "index and value are required"));
        }
        int index = request.getIndex();
        int n = request.getValues().size();
        if (index < 0 || index >= n) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "index must be between 0 and " + (n - 1)));
        }
        return null;
    }

    /**
     * 400 when values are missing/empty or longer than 8,
     * graphEdges is missing/empty, or any pair is not [a, b] of
     * two distinct elements from the set. (Element labels are
     * arbitrary integers — unlike the segment tree there is no
     * |value| limit here.) Null means "no error".
     */
    private ResponseEntity<?> validateUnionFind(
            TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        if (request.getValues().size() > 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "union-find needs at most 8 elements"));
        }
        if (request.getGraphEdges() == null
                || request.getGraphEdges().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "graphEdges must not be empty"));
        }
        for (List<Integer> pair : request.getGraphEdges()) {
            if (pair == null || pair.size() != 2
                    || pair.get(0) == null || pair.get(1) == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "each union pair must be [a, b]"));
            }
            int a = pair.get(0);
            int b = pair.get(1);
            if (!request.getValues().contains(a)
                    || !request.getValues().contains(b)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "union pair elements must be in the "
                                        + "element set"));
            }
            if (a == b) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "a union pair must have two distinct "
                                        + "elements"));
            }
        }
        return null;
    }

    /**
     * 400 when words is not exactly two non-empty strings, the
     * text is longer than 40 characters, or the pattern is longer
     * than 12 characters or longer than the text. Null means
     * "no error".
     */
    private ResponseEntity<?> validateKmp(TraceRequest request) {
        if (request == null || request.getWords() == null
                || request.getWords().size() != 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "words must contain exactly two strings"));
        }
        String text = request.getWords().get(0);
        String pattern = request.getWords().get(1);
        if (text == null || pattern == null
                || text.isEmpty() || pattern.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "text and pattern must not be empty"));
        }
        if (text.length() > 40) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "the text must be at most 40 characters"));
        }
        if (pattern.length() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "the pattern must be at most 12 "
                                    + "characters"));
        }
        if (pattern.length() > text.length()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "the pattern must not be longer than the "
                                    + "text"));
        }
        return null;
    }

    /**
     * 400 when value (n) is missing, below 1 or above 8. Null
     * means "no error".
     */
    private ResponseEntity<?> validateNQueens(TraceRequest request) {
        if (request == null || request.getValue() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "value is required"));
        }
        int n = request.getValue();
        if (n < 1 || n > 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "n must be between 1 and 8"));
        }
        return null;
    }

    /**
     * 400 when graphEdges is missing/empty, any edge is not
     * [from, to] or [from, to, weight], any node id is missing,
     * below 0 or above 9 (the matrix must stay drawable — 10x10
     * max), or a 3-element edge has a null weight. Negative
     * weights are allowed. A non-numeric weight never reaches
     * this validator — Jackson rejects it with a 400 first.
     * Null means "no error".
     */
    private ResponseEntity<?> validateFloydWarshall(
            TraceRequest request) {
        if (request == null || request.getGraphEdges() == null
                || request.getGraphEdges().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "graphEdges must not be empty"));
        }
        for (List<Integer> edge : request.getGraphEdges()) {
            if (edge == null || (edge.size() != 2 && edge.size() != 3)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "each edge must be [from, to] or "
                                        + "[from, to, weight]"));
            }
            Integer from = edge.get(0);
            Integer to = edge.get(1);
            if (from == null || to == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "edge endpoints must not be null"));
            }
            if (from < 0 || from > 9 || to < 0 || to > 9) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "node ids must be between 0 and 9"));
            }
            if (edge.size() == 3 && edge.get(2) == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "edge weight must not be null"));
            }
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

    /**
     * 400 when values are missing/empty, longer than 10, or any
     * value is outside 0..99 (bucket index = value / 10 must stay
     * drawable). Null means "no error".
     */
    private ResponseEntity<?> validateBucketSort(
            TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        if (request.getValues().size() > 10) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "bucket sort needs at most 10 values"));
        }
        for (int v : request.getValues()) {
            if (v < 0 || v > 99) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "bucket sort needs values between 0 "
                                        + "and 99"));
            }
        }
        return null;
    }

    /**
     * 400 when values are missing/empty, longer than 10, or any
     * |value| is over 999 (the dp array must stay drawable).
     * Null means "no error".
     */
    private ResponseEntity<?> validateLis(TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        if (request.getValues().size() > 10) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "LIS needs at most 10 values"));
        }
        for (int v : request.getValues()) {
            if (Math.abs(v) > 999) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "LIS needs |values| <= 999"));
            }
        }
        return null;
    }

    /**
     * 400 when values (the dimensions) are missing/empty, shorter
     * than 3 or longer than 7, or any dimension is outside 1..99
     * (the cost table must stay drawable). Null means "no error".
     */
    private ResponseEntity<?> validateMcm(TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        if (request.getValues().size() < 3
                || request.getValues().size() > 7) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "matrix chain needs 3 to 7 dimensions"));
        }
        for (int v : request.getValues()) {
            if (v < 1 || v > 99) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "dimensions must be between 1 and 99"));
            }
        }
        return null;
    }

    /** Null-safe values accessor (null request or null values → null). */
    private List<Integer> valuesOf(TraceRequest request) {
        return request == null ? null : request.getValues();
    }
}
