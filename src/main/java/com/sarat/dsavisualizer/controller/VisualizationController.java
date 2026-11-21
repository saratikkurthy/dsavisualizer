package com.sarat.dsavisualizer.controller;

import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.model.TraceResponse;
import com.sarat.dsavisualizer.service.AhoCorasick2Service;
import com.sarat.dsavisualizer.service.AhoCorasickService;
import com.sarat.dsavisualizer.service.AvlService;
import com.sarat.dsavisualizer.service.BacktrackingService;
import com.sarat.dsavisualizer.service.BidirectionalDijkstraService;
import com.sarat.dsavisualizer.service.BloomFilterService;
import com.sarat.dsavisualizer.service.BoyerMooreService;
import com.sarat.dsavisualizer.service.BridgesService;
import com.sarat.dsavisualizer.service.BstService;
import com.sarat.dsavisualizer.service.BTreeService;
import com.sarat.dsavisualizer.service.BucketSortService;
import com.sarat.dsavisualizer.service.ClosestPairService;
import com.sarat.dsavisualizer.service.ContractionHierarchiesService;
import com.sarat.dsavisualizer.service.ConvexHullService;
import com.sarat.dsavisualizer.service.CountingService;
import com.sarat.dsavisualizer.service.DpService;
import com.sarat.dsavisualizer.service.EertreeService;
import com.sarat.dsavisualizer.service.FenwickTreeService;
import com.sarat.dsavisualizer.service.GeohashService;
import com.sarat.dsavisualizer.service.GraphService;
import com.sarat.dsavisualizer.service.HashTableService;
import com.sarat.dsavisualizer.service.HeapService;
import com.sarat.dsavisualizer.service.HeapSortService;
import com.sarat.dsavisualizer.service.HuffmanService;
import com.sarat.dsavisualizer.service.CentroidService;
import com.sarat.dsavisualizer.service.HldService;
import com.sarat.dsavisualizer.service.HopcroftKarpService;
import com.sarat.dsavisualizer.service.HungarianService;
import com.sarat.dsavisualizer.service.InvertedIndexService;
import com.sarat.dsavisualizer.service.MinCostFlowService;
import com.sarat.dsavisualizer.service.MosService;
import com.sarat.dsavisualizer.service.PersistentSegService;
import com.sarat.dsavisualizer.service.PstKthService;
import com.sarat.dsavisualizer.service.KdTreeService;
import com.sarat.dsavisualizer.service.KmpService;
import com.sarat.dsavisualizer.service.KnapsackService;
import com.sarat.dsavisualizer.service.LazySegService;
import com.sarat.dsavisualizer.service.LcpKasaiService;
import com.sarat.dsavisualizer.service.LfuCacheService;
import com.sarat.dsavisualizer.service.LineSweepService;
import com.sarat.dsavisualizer.service.LinkedListService;
import com.sarat.dsavisualizer.service.LruCacheService;
import com.sarat.dsavisualizer.service.ManacherService;
import com.sarat.dsavisualizer.service.MaxFlowService;
import com.sarat.dsavisualizer.service.PageRankService;
import com.sarat.dsavisualizer.service.QueueService;
import com.sarat.dsavisualizer.service.QuickSelectService;
import com.sarat.dsavisualizer.service.QuadtreeService;
import com.sarat.dsavisualizer.service.RTreeService;
import com.sarat.dsavisualizer.service.RabinKarpService;
import com.sarat.dsavisualizer.service.TfidfService;
import com.sarat.dsavisualizer.service.TrafficDijkstraService;
import com.sarat.dsavisualizer.service.TspService;
import com.sarat.dsavisualizer.service.YenService;
import com.sarat.dsavisualizer.service.RadixService;
import com.sarat.dsavisualizer.service.RedBlackService;
import com.sarat.dsavisualizer.service.SamLcsService;
import com.sarat.dsavisualizer.service.SamService;
import com.sarat.dsavisualizer.service.SccService;
import com.sarat.dsavisualizer.service.SearchService;
import com.sarat.dsavisualizer.service.SegmentTreeService;
import com.sarat.dsavisualizer.service.ShellSortService;
import com.sarat.dsavisualizer.service.SkipListService;
import com.sarat.dsavisualizer.service.SortingService;
import com.sarat.dsavisualizer.service.SparseTableService;
import com.sarat.dsavisualizer.service.SqrtDecomp2Service;
import com.sarat.dsavisualizer.service.SqrtDecompService;
import com.sarat.dsavisualizer.service.StackService;
import com.sarat.dsavisualizer.service.SudokuService;
import com.sarat.dsavisualizer.service.SuffixArrayService;
import com.sarat.dsavisualizer.service.SuffixTrieService;
import com.sarat.dsavisualizer.service.TrieService;
import com.sarat.dsavisualizer.service.UnionFindService;
import com.sarat.dsavisualizer.service.VoronoiService;
import com.sarat.dsavisualizer.service.ZAlgorithmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * Validation rule (Phase 16): Boyer-Moore and Z-algorithm reuse
 * the KMP validation. Fenwick 400 when values are missing/empty,
 * longer than 8, or any |value| is over 99; prefix/update also
 * 400 when the index is missing or outside 1..n (Fenwick indices
 * are 1-based); update also 400 when the delta (value) is missing,
 * outside -99..99, or 0. QuickSelect 400 when values are
 * missing/empty, shorter than 2 or longer than 10, any |value| is
 * over 999, or k (value) is missing or outside 1..n.
 * Validation rule (Phase 17): LRU 400 when the capacity is
 * missing or outside 1..5, words is missing/empty or longer than
 * 10, or any op fails to parse as "put &lt;key&gt; &lt;value&gt;"
 * or "get &lt;key&gt;" with a key in 0..99 and a value in 0..999.
 * B-tree 400 when values are missing/empty, longer than 12, or
 * any |value| is over 99. Bloom filter 400 when words is
 * missing/empty or longer than 6, any word is empty or longer
 * than 12 characters, or the word to check is missing/empty or
 * longer than 12 characters.
 * Validation rule (Phase 18): B-tree search 400 when values are
 * bad (the B-tree rules) or the target is missing; B-tree delete
 * 400 when values are bad (the B-tree rules) or the value is
 * missing. Suffix array 400 when the word is missing/empty,
 * longer than 12 characters, or not lowercase a–z. Skip list
 * 400 when values are missing/empty, longer than 8, or any
 * |value| is over 99.
 * Validation rule (Phase 19): probing 400 when values are
 * missing/empty, longer than 8, or any |value| is over 99, or
 * the mode (word) is not linear, quadratic or double. SCC 400
 * when graphEdges is missing/empty, there are fewer than 2 or
 * more than 12 edges, any edge is not [from, to] or
 * [from, to, weight], or any node id is missing, below 0 or
 * above 9. Huffman 400 when the word is missing/empty, longer
 * than 12 characters, or not lowercase a–z (the suffix-array
 * word rules).
 * Validation rule (Phase 20): max flow 400 when graphEdges is
 * missing/empty, there are fewer than 2 or more than 12 edges,
 * any edge is not [from, to, capacity], any capacity is outside
 * 1..99, any node id is outside 0..9, or start/target is
 * missing, equal, or not a node in the graph. LFU 400 when the
 * capacity is missing or outside 1..5, or the ops fail the LRU
 * op rules (at least 1, at most 10 ops, "put &lt;key&gt;
 * &lt;value&gt;" / "get &lt;key&gt;", key 0..99, value 0..999).
 * Sudoku 400 when the grid is missing, not 9 rows of 9, any
 * value is outside 0..9, or the givens are inconsistent (a
 * duplicate in a row, column or 3×3 box).
 * Validation rule (Phase 21): Aho-Corasick 400 when the text is
 * missing/empty, longer than 30 characters, or not lowercase
 * a–z, or patterns are missing/empty, more than 5, or any
 * pattern is empty, longer than 10 characters, or not lowercase
 * a–z. Bridges 400 when graphEdges is missing/empty, there are
 * fewer than 2 or more than 12 edges, any edge is not
 * [from, to], or any node id is outside 0..20, or a given start
 * is not a node in the graph (a missing start defaults to the
 * smallest node). Manacher 400 when the word is missing/empty,
 * longer than 20 characters, or not lowercase a–z.
 * Validation rule (Phase 22): bidirectional Dijkstra 400 when
 * graphEdges/start is missing or any weight is &lt;= 0, or the
 * target is missing, start == target, or start/target is not
 * a node in the graph. Contraction hierarchies adds: a given
 * contraction order (values) that is not a permutation of the
 * graph's nodes 400s (a missing order defaults to ascending
 * node id). Geohash 400 when the word is missing or not a
 * "lat,lon" pair, lat is outside [-90, 90], lon is outside
 * [-180, 180], or the precision (index) is outside 1..12
 * (a missing precision defaults to 6).
 * Validation rule (Phase 23): quadtree 400 when the word is
 * missing or not a "x,y;x,y;..." point list, a point is
 * outside the 100x100 map, capacity is outside 1..16 (a
 * missing capacity defaults to 2), or a query rect (values)
 * is not 4 numbers (a missing rect defaults to the whole
 * bounds). K-d tree 400 when the word is missing or not a
 * point list, or a query point (values) is not 2 numbers (a
 * missing query defaults to (9,2)). R-tree 400 when the word
 * is missing or not a "x1,y1,x2,y2;..." rect list, a rect has
 * x1 &gt;= x2 or y1 &gt;= y2, capacity is outside 2..8 (a
 * missing capacity defaults to 4), or a query rect (values)
 * is not 4 numbers (a missing rect defaults to
 * (12,12,30,30)). Yen 400 like bidirectional Dijkstra, plus
 * K (index) outside 1..5 (a missing K defaults to 3).
 * Traffic-aware Dijkstra 400 like bidirectional Dijkstra,
 * plus the departure hour (index) outside 0..23 (a missing
 * hour defaults to 8), or rush factors (values) with a
 * non-positive entry or a length different from the edge
 * count (missing factors default to all 1x). TSP 400 when the
 * word is missing or not a point list, or fewer than 3 points.
 * Validation rule (Phase 24): convex hull 400 when the word is
 * missing or not a "x,y;x,y;..." point list, or fewer than 3
 * points. Closest pair 400 the same, with fewer than 2 points.
 * Line sweep 400 when the word is missing or not a
 * "x1,y1,x2,y2;..." segment list. Voronoi 400 when the word is
 * missing or not a point list, fewer than 2 sites, or a site is
 * outside the fixed [0,0,10,10] box. Inverted index and TF-IDF
 * 400 when the documents (words) are missing/empty (a missing
 * query word defaults to the demo query: "tree traversal" /
 * "binary tree"). PageRank 400 when graphEdges is missing/empty,
 * any edge is not a [u, v] pair, any node id is negative or
 * outside the page range, the iterations (index) are outside
 * 1..20 (a missing count defaults to 10), or the page count
 * (value) is outside 1..20 or below max-node-id + 1 (a missing
 * count defaults to max-node-id + 1 — the frontend sends the
 * page count because a dangling page has no links to infer it
 * from).
 * Validation rule (Phase 25): HLD 400 when graphEdges is
 * missing/empty, any edge is not a [parent, child] pair, any
 * node id is negative or above 20, a node has two parents,
 * there is no single root, or the edges do not connect the
 * tree (a given start/target must be a node in the tree; a
 * missing start/target defaults to 7/5, the demo query).
 * Centroid 400 when graphEdges is missing/empty, any edge is
 * not a [u, v] pair, or any node id is negative or above 20.
 * Persistent segment tree 400 when the word is missing or
 * not a comma list of numbers, the array is longer than 8
 * or any |value| is over 999, values is missing, odd or
 * shorter than 2, an update index is out of range, an update
 * value has |value| over 999, or the query range is out of
 * range or reversed. Hopcroft-Karp 400 when graphEdges is
 * missing/empty, any edge is not a [left, right] pair, any
 * node id is negative or above 9, an edge is a self-loop, or
 * a node appears on both sides. Min-cost max-flow 400 like
 * max flow (2..12 edges, [u, v, cap] triples, capacity
 * 1..99, node ids 0..9), plus a costs list (values) whose
 * length differs from the edge count or any cost outside
 * -99..99 (a missing costs list defaults to all zero; a
 * missing start/target defaults to the smallest/largest node
 * id). Hungarian 400 when the word is missing or not a
 * "a,b,c;d,e,f;..." matrix, the rows are not all the same
 * length, the matrix is not square, its size is outside
 * 2..6, or any cost is outside 0..99.
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
    private final BoyerMooreService boyerMooreService;
    private final ZAlgorithmService zAlgorithmService;
    private final FenwickTreeService fenwickTreeService;
    private final QuickSelectService quickSelectService;
    private final LruCacheService lruCacheService;
    private final BTreeService bTreeService;
    private final BloomFilterService bloomFilterService;
    private final SuffixArrayService suffixArrayService;
    private final SkipListService skipListService;
    private final SccService sccService;
    private final HuffmanService huffmanService;
    private final MaxFlowService maxFlowService;
    private final LfuCacheService lfuCacheService;
    private final SudokuService sudokuService;
    private final AhoCorasickService ahoCorasickService;
    private final BridgesService bridgesService;
    private final ManacherService manacherService;
    private final BidirectionalDijkstraService bidijkstraService;
    private final ContractionHierarchiesService chService;
    private final GeohashService geohashService;
    private final QuadtreeService quadtreeService;
    private final KdTreeService kdTreeService;
    private final RTreeService rTreeService;
    private final YenService yenService;
    private final TrafficDijkstraService trafficService;
    private final TspService tspService;
    private final ConvexHullService convexHullService;
    private final ClosestPairService closestPairService;
    private final LineSweepService lineSweepService;
    private final VoronoiService voronoiService;
    private final InvertedIndexService invertedIndexService;
    private final TfidfService tfidfService;
    private final PageRankService pageRankService;
    private final HldService hldService;
    private final CentroidService centroidService;
    private final PersistentSegService persistentSegService;
    private final HopcroftKarpService hopcroftKarpService;
    private final MinCostFlowService minCostFlowService;
    private final HungarianService hungarianService;
    private final SamService samService;
    private final EertreeService eertreeService;
    private final LcpKasaiService lcpKasaiService;
    private final SqrtDecompService sqrtDecompService;
    private final MosService mosService;
    private final SparseTableService sparseTableService;
    private final AhoCorasick2Service ahoCorasick2Service;
    private final SuffixTrieService suffixTrieService;
    private final SamLcsService samLcsService;
    private final LazySegService lazySegService;
    private final PstKthService pstKthService;
    private final SqrtDecomp2Service sqrtDecomp2Service;

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
                                   RabinKarpService rabinKarpService,
                                   BoyerMooreService boyerMooreService,
                                   ZAlgorithmService zAlgorithmService,
                                   FenwickTreeService fenwickTreeService,
                                   QuickSelectService quickSelectService,
                                   LruCacheService lruCacheService,
                                   BTreeService bTreeService,
                                   BloomFilterService bloomFilterService,
                                   SuffixArrayService suffixArrayService,
                                   SkipListService skipListService,
                                   SccService sccService,
                                   HuffmanService huffmanService,
                                   MaxFlowService maxFlowService,
                                   LfuCacheService lfuCacheService,
                                   SudokuService sudokuService,
                                   AhoCorasickService ahoCorasickService,
                                   BridgesService bridgesService,
                                   ManacherService manacherService,
                                   BidirectionalDijkstraService bidijkstraService,
                                   ContractionHierarchiesService chService,
                                   GeohashService geohashService,
                                   QuadtreeService quadtreeService,
                                   KdTreeService kdTreeService,
                                   RTreeService rTreeService,
                                   YenService yenService,
                                   TrafficDijkstraService trafficService,
                                   TspService tspService,
                                   ConvexHullService convexHullService,
                                   ClosestPairService closestPairService,
                                   LineSweepService lineSweepService,
                                   VoronoiService voronoiService,
                                   InvertedIndexService invertedIndexService,
                                   TfidfService tfidfService,
                                   PageRankService pageRankService,
                                   HldService hldService,
                                   CentroidService centroidService,
                                   PersistentSegService persistentSegService,
                                   HopcroftKarpService hopcroftKarpService,
                                   MinCostFlowService minCostFlowService,
                                   HungarianService hungarianService,
                                   SamService samService,
                                   EertreeService eertreeService,
                                   LcpKasaiService lcpKasaiService,
                                   SqrtDecompService sqrtDecompService,
                                   MosService mosService,
                                   SparseTableService sparseTableService,
                                   AhoCorasick2Service ahoCorasick2Service,
                                   SuffixTrieService suffixTrieService,
                                   SamLcsService samLcsService,
                                   LazySegService lazySegService,
                                   PstKthService pstKthService,
                                   SqrtDecomp2Service sqrtDecomp2Service) {
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
        this.boyerMooreService = boyerMooreService;
        this.zAlgorithmService = zAlgorithmService;
        this.fenwickTreeService = fenwickTreeService;
        this.quickSelectService = quickSelectService;
        this.lruCacheService = lruCacheService;
        this.bTreeService = bTreeService;
        this.bloomFilterService = bloomFilterService;
        this.suffixArrayService = suffixArrayService;
        this.skipListService = skipListService;
        this.sccService = sccService;
        this.huffmanService = huffmanService;
        this.maxFlowService = maxFlowService;
        this.lfuCacheService = lfuCacheService;
        this.sudokuService = sudokuService;
        this.ahoCorasickService = ahoCorasickService;
        this.bridgesService = bridgesService;
        this.manacherService = manacherService;
        this.bidijkstraService = bidijkstraService;
        this.chService = chService;
        this.geohashService = geohashService;
        this.quadtreeService = quadtreeService;
        this.kdTreeService = kdTreeService;
        this.rTreeService = rTreeService;
        this.yenService = yenService;
        this.trafficService = trafficService;
        this.tspService = tspService;
        this.convexHullService = convexHullService;
        this.closestPairService = closestPairService;
        this.lineSweepService = lineSweepService;
        this.voronoiService = voronoiService;
        this.invertedIndexService = invertedIndexService;
        this.tfidfService = tfidfService;
        this.pageRankService = pageRankService;
        this.hldService = hldService;
        this.centroidService = centroidService;
        this.persistentSegService = persistentSegService;
        this.hopcroftKarpService = hopcroftKarpService;
        this.minCostFlowService = minCostFlowService;
        this.hungarianService = hungarianService;
        this.samService = samService;
        this.eertreeService = eertreeService;
        this.lcpKasaiService = lcpKasaiService;
        this.sqrtDecompService = sqrtDecompService;
        this.mosService = mosService;
        this.sparseTableService = sparseTableService;
        this.ahoCorasick2Service = ahoCorasick2Service;
        this.suffixTrieService = suffixTrieService;
        this.samLcsService = samLcsService;
        this.lazySegService = lazySegService;
        this.pstKthService = pstKthService;
        this.sqrtDecomp2Service = sqrtDecomp2Service;
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

    // ---- Sorting (Phase 16) ----

    @PostMapping("/sort/quickselect")
    public ResponseEntity<?> quickSelect(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateQuickSelect(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = quickSelectService.quickSelect(
                request.getValues(), request.getValue());
        return ResponseEntity.ok(new TraceResponse("QuickSelect", steps));
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

    // ---- Fenwick tree (Phase 16) ----

    @PostMapping("/fenwick/build")
    public ResponseEntity<?> fenwickBuild(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateFenwick(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = fenwickTreeService.build(
                request.getValues());
        return ResponseEntity.ok(
                new TraceResponse("Fenwick Tree Build", steps));
    }

    @PostMapping("/fenwick/prefix")
    public ResponseEntity<?> fenwickPrefix(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateFenwickIndex(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = fenwickTreeService.prefix(
                request.getValues(), request.getIndex());
        return ResponseEntity.ok(
                new TraceResponse("Fenwick Tree Prefix Sum", steps));
    }

    @PostMapping("/fenwick/update")
    public ResponseEntity<?> fenwickUpdate(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateFenwickDelta(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = fenwickTreeService.update(
                request.getValues(), request.getIndex(),
                request.getValue());
        return ResponseEntity.ok(
                new TraceResponse("Fenwick Tree Update", steps));
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

    // ---- Boyer-Moore / Z-algorithm string matching (Phase 16) ----

    @PostMapping("/string/boyermoore")
    public ResponseEntity<?> boyerMoore(@RequestBody TraceRequest request) {
        // Same validation bounds as KMP: exactly two non-empty
        // strings, text at most 40 characters, pattern at most 12
        // and no longer than the text.
        ResponseEntity<?> error = validateKmp(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = boyerMooreService.boyerMoore(
                request.getWords());
        return ResponseEntity.ok(
                new TraceResponse("Boyer-Moore String Matching", steps));
    }

    @PostMapping("/string/zalgorithm")
    public ResponseEntity<?> zAlgorithm(@RequestBody TraceRequest request) {
        // Same validation bounds as KMP.
        ResponseEntity<?> error = validateKmp(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = zAlgorithmService.zAlgorithm(
                request.getWords());
        return ResponseEntity.ok(
                new TraceResponse("Z-Algorithm String Matching", steps));
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

    // ---- LRU cache / B-tree / Bloom filter (Phase 17) ----

    @PostMapping("/lru/simulate")
    public ResponseEntity<?> lruSimulate(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateLru(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = lruCacheService.simulate(
                request.getCapacity(), request.getWords());
        return ResponseEntity.ok(new TraceResponse("LRU Cache", steps));
    }

    @PostMapping("/btree/insert")
    public ResponseEntity<?> bTreeInsert(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateBTree(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = bTreeService.insert(request.getValues());
        return ResponseEntity.ok(new TraceResponse("B-Tree", steps));
    }

    @PostMapping("/hash/bloom")
    public ResponseEntity<?> bloomFilter(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateBloom(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = bloomFilterService.bloom(
                request.getWords(), request.getWord());
        return ResponseEntity.ok(
                new TraceResponse("Bloom Filter", steps));
    }

    // ---- B-tree search/delete, suffix array, skip list (Phase 18) ----

    @PostMapping("/btree/search")
    public ResponseEntity<?> bTreeSearch(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateBTreeSearch(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = bTreeService.search(
                request.getValues(), request.getTarget());
        return ResponseEntity.ok(new TraceResponse("B-Tree Search", steps));
    }

    @PostMapping("/btree/delete")
    public ResponseEntity<?> bTreeDelete(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateBTreeDelete(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = bTreeService.delete(
                request.getValues(), request.getValue());
        return ResponseEntity.ok(new TraceResponse("B-Tree Delete", steps));
    }

    @PostMapping("/string/suffixarray")
    public ResponseEntity<?> suffixArray(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateSuffixArray(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = suffixArrayService.suffixArray(
                request.getWord());
        return ResponseEntity.ok(new TraceResponse("Suffix Array", steps));
    }

    @PostMapping("/skiplist/insert")
    public ResponseEntity<?> skipListInsert(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateSkipList(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = skipListService.insert(request.getValues());
        return ResponseEntity.ok(new TraceResponse("Skip List", steps));
    }

    // ---- Open addressing, Kosaraju SCC, Huffman coding (Phase 19) ----

    @PostMapping("/hash/probing")
    public ResponseEntity<?> hashProbing(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateProbing(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = hashTableService.probing(
                request.getValues(), request.getWord());
        return ResponseEntity.ok(
                new TraceResponse("Open Addressing", steps));
    }

    @PostMapping("/graph/scc")
    public ResponseEntity<?> graphScc(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateScc(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = sccService.scc(request.getGraphEdges());
        return ResponseEntity.ok(new TraceResponse(
                "Strongly Connected Components", steps));
    }

    @PostMapping("/string/huffman")
    public ResponseEntity<?> huffman(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateHuffman(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = huffmanService.huffman(request.getWord());
        return ResponseEntity.ok(new TraceResponse("Huffman Coding", steps));
    }

    // ---- Max flow, LFU cache, Sudoku solver (Phase 20) ----

    @PostMapping("/graph/maxflow")
    public ResponseEntity<?> graphMaxFlow(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateMaxFlow(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = maxFlowService.maxflow(
                request.getGraphEdges(), request.getStart(),
                request.getTarget());
        return ResponseEntity.ok(new TraceResponse("Max Flow", steps));
    }

    @PostMapping("/lfu/simulate")
    public ResponseEntity<?> lfuSimulate(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateLfu(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = lfuCacheService.simulate(
                request.getCapacity(), request.getWords());
        return ResponseEntity.ok(new TraceResponse("LFU Cache", steps));
    }

    @PostMapping("/backtracking/sudoku")
    public ResponseEntity<?> sudoku(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateSudoku(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = sudokuService.sudoku(request.getGrid());
        return ResponseEntity.ok(new TraceResponse("Sudoku Solver", steps));
    }

    // ---- Aho-Corasick, bridges, Manacher (Phase 21) ----

    @PostMapping("/string/ahocorasick")
    public ResponseEntity<?> ahoCorasick(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateAhoCorasick(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = ahoCorasickService.ahocorasick(
                request.getWord(), request.getWords());
        return ResponseEntity.ok(new TraceResponse("Aho-Corasick", steps));
    }

    @PostMapping("/graph/bridges")
    public ResponseEntity<?> graphBridges(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateBridges(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = bridgesService.bridges(
                request.getGraphEdges(), request.getStart());
        return ResponseEntity.ok(
                new TraceResponse("Bridges & Articulation", steps));
    }

    @PostMapping("/string/manacher")
    public ResponseEntity<?> manacher(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateManacher(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = manacherService.manacher(request.getWord());
        return ResponseEntity.ok(
                new TraceResponse("Manacher's Algorithm", steps));
    }

    // ---- Bidirectional Dijkstra, contraction hierarchies, geohash (Phase 22) ----

    @PostMapping("/graph/bidijkstra")
    public ResponseEntity<?> graphBidijkstra(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateBidirectionalDijkstra(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = bidijkstraService.bidijkstra(
                request.getGraphEdges(), request.getStart(),
                request.getTarget());
        return ResponseEntity.ok(new TraceResponse(
                "Bidirectional Dijkstra", steps));
    }

    @PostMapping("/graph/ch")
    public ResponseEntity<?> graphCh(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateContractionHierarchies(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = chService.contractionHierarchies(
                request.getGraphEdges(), request.getStart(),
                request.getTarget(), request.getValues());
        return ResponseEntity.ok(new TraceResponse(
                "Contraction Hierarchies", steps));
    }

    @PostMapping("/geo/geohash")
    public ResponseEntity<?> geohash(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateGeohash(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = geohashService.geohash(
                request.getWord(), request.getIndex());
        return ResponseEntity.ok(new TraceResponse("Geohash", steps));
    }

    // ---- Quadtree, k-d tree, R-tree, Yen, traffic, TSP (Phase 23) ----

    @PostMapping("/spatial/quadtree")
    public ResponseEntity<?> spatialQuadtree(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateQuadtree(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = quadtreeService.quadtree(
                request.getWord(), request.getValues(),
                request.getCapacity());
        return ResponseEntity.ok(new TraceResponse("Quadtree", steps));
    }

    @PostMapping("/spatial/kdtree")
    public ResponseEntity<?> spatialKdTree(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateKdTree(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = kdTreeService.kdtree(
                request.getWord(), request.getValues());
        return ResponseEntity.ok(new TraceResponse("K-d Tree", steps));
    }

    @PostMapping("/spatial/rtree")
    public ResponseEntity<?> spatialRTree(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateRTree(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = rTreeService.rtree(
                request.getWord(), request.getValues(),
                request.getCapacity());
        return ResponseEntity.ok(new TraceResponse("R-Tree", steps));
    }

    @PostMapping("/graph/yen")
    public ResponseEntity<?> graphYen(@RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateYen(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = yenService.yen(
                request.getGraphEdges(), request.getStart(),
                request.getTarget(), request.getIndex());
        return ResponseEntity.ok(new TraceResponse(
                "Yen's K-Shortest Paths", steps));
    }

    @PostMapping("/graph/traffic")
    public ResponseEntity<?> graphTraffic(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateTraffic(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = trafficService.traffic(
                request.getGraphEdges(), request.getStart(),
                request.getTarget(), request.getIndex(),
                request.getValues());
        return ResponseEntity.ok(new TraceResponse(
                "Traffic-Aware Dijkstra", steps));
    }

    @PostMapping("/spatial/tsp")
    public ResponseEntity<?> spatialTsp(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateTsp(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = tspService.tsp(request.getWord());
        return ResponseEntity.ok(new TraceResponse(
                "Travelling Salesman Problem", steps));
    }

    // ---- Geometry (Phase 24) ----

    @PostMapping("/geometry/convexhull")
    public ResponseEntity<?> geometryConvexHull(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateConvexHull(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = convexHullService.convexHull(
                request.getWord());
        return ResponseEntity.ok(new TraceResponse(
                "Convex Hull", steps));
    }

    @PostMapping("/geometry/closestpair")
    public ResponseEntity<?> geometryClosestPair(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateClosestPair(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = closestPairService.closestPair(
                request.getWord());
        return ResponseEntity.ok(new TraceResponse(
                "Closest Pair", steps));
    }

    @PostMapping("/geometry/linesweep")
    public ResponseEntity<?> geometryLineSweep(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateLineSweep(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = lineSweepService.lineSweep(
                request.getWord());
        return ResponseEntity.ok(new TraceResponse(
                "Line Sweep", steps));
    }

    @PostMapping("/geometry/voronoi")
    public ResponseEntity<?> geometryVoronoi(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateVoronoi(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = voronoiService.voronoi(request.getWord());
        return ResponseEntity.ok(new TraceResponse(
                "Voronoi Diagram", steps));
    }

    // ---- Search & ranking (Phase 24) ----

    @PostMapping("/search/invertedindex")
    public ResponseEntity<?> searchInvertedIndex(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateInvertedIndex(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = invertedIndexService.invertedIndex(
                request.getWords(), request.getWord());
        return ResponseEntity.ok(new TraceResponse(
                "Inverted Index", steps));
    }

    @PostMapping("/search/tfidf")
    public ResponseEntity<?> searchTfidf(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateTfidf(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = tfidfService.tfidf(
                request.getWords(), request.getWord());
        return ResponseEntity.ok(new TraceResponse(
                "TF-IDF Ranking", steps));
    }

    @PostMapping("/search/pagerank")
    public ResponseEntity<?> searchPageRank(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validatePageRank(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = pageRankService.pagerank(
                request.getGraphEdges(), request.getIndex(),
                request.getValue());
        return ResponseEntity.ok(new TraceResponse(
                "PageRank", steps));
    }

    // ---- Advanced trees (Phase 25) ----

    @PostMapping("/tree/hld")
    public ResponseEntity<?> treeHld(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateHld(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = hldService.hld(
                request.getGraphEdges(), request.getStart(),
                request.getTarget());
        return ResponseEntity.ok(new TraceResponse(
                "Heavy-Light Decomposition", steps));
    }

    @PostMapping("/tree/centroid")
    public ResponseEntity<?> treeCentroid(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateCentroid(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = centroidService.centroid(
                request.getGraphEdges());
        return ResponseEntity.ok(new TraceResponse(
                "Centroid Decomposition", steps));
    }

    @PostMapping("/tree/persistentseg")
    public ResponseEntity<?> treePersistentSeg(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validatePersistentSeg(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = persistentSegService.persistentSeg(
                request.getWord(), request.getValues());
        return ResponseEntity.ok(new TraceResponse(
                "Persistent Segment Tree", steps));
    }

    // ---- Matching & flow (Phase 25) ----

    @PostMapping("/flow/hopcroftkarp")
    public ResponseEntity<?> flowHopcroftKarp(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateHopcroftKarp(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = hopcroftKarpService.hopcroftKarp(
                request.getGraphEdges());
        return ResponseEntity.ok(new TraceResponse(
                "Hopcroft-Karp", steps));
    }

    @PostMapping("/flow/mincostflow")
    public ResponseEntity<?> flowMinCostFlow(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateMinCostFlow(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = minCostFlowService.minCostFlow(
                request.getGraphEdges(), request.getValues(),
                request.getStart(), request.getTarget());
        return ResponseEntity.ok(new TraceResponse(
                "Min-Cost Max-Flow", steps));
    }

    @PostMapping("/flow/hungarian")
    public ResponseEntity<?> flowHungarian(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateHungarian(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = hungarianService.hungarian(
                request.getWord());
        return ResponseEntity.ok(new TraceResponse(
                "Hungarian Algorithm", steps));
    }

    // ---- String algorithms v2 (Phase 26) ----

    @PostMapping("/string/sam")
    public ResponseEntity<?> stringSam(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateSam(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = samService.sam(request.getWord());
        return ResponseEntity.ok(new TraceResponse(
                "Suffix Automaton", steps));
    }

    @PostMapping("/string/eertree")
    public ResponseEntity<?> stringEertree(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateEertree(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = eertreeService.eertree(
                request.getWord());
        return ResponseEntity.ok(new TraceResponse(
                "Palindromic Tree", steps));
    }

    @PostMapping("/string/lcpkasai")
    public ResponseEntity<?> stringLcpKasai(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateLcpKasai(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = lcpKasaiService.lcpKasai(
                request.getWord());
        return ResponseEntity.ok(new TraceResponse(
                "Suffix Array & LCP", steps));
    }

    // ---- Range queries v2 (Phase 26) ----

    @PostMapping("/range/sqrtdecomp")
    public ResponseEntity<?> rangeSqrtDecomp(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateSqrtDecomp(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = sqrtDecompService.sqrtDecomp(
                request.getWord(), request.getValues(),
                request.getIndex(), request.getCapacity());
        return ResponseEntity.ok(new TraceResponse(
                "Sqrt Decomposition", steps));
    }

    @PostMapping("/range/mos")
    public ResponseEntity<?> rangeMos(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateMos(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = mosService.mos(
                request.getWord(), request.getValues());
        return ResponseEntity.ok(new TraceResponse(
                "Mo's Algorithm", steps));
    }

    @PostMapping("/range/sparsetable")
    public ResponseEntity<?> rangeSparseTable(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateSparseTable(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = sparseTableService.sparseTable(
                request.getWord(), request.getValues());
        return ResponseEntity.ok(new TraceResponse(
                "Sparse Table", steps));
    }

    // ---- String matching v3 (Phase 27) ----

    @PostMapping("/string/ahocorasick2")
    public ResponseEntity<?> stringAhoCorasick2(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateAhoCorasick2(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = ahoCorasick2Service.ahoCorasick2(
                request.getWord(), request.getWords());
        return ResponseEntity.ok(new TraceResponse(
                "Aho-Corasick v2", steps));
    }

    @PostMapping("/string/suffixtrie")
    public ResponseEntity<?> stringSuffixTrie(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateSuffixTrie(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = suffixTrieService.suffixTrie(
                request.getWord());
        return ResponseEntity.ok(new TraceResponse(
                "Suffix Trie", steps));
    }

    @PostMapping("/string/samlcs")
    public ResponseEntity<?> stringSamLcs(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateSamLcs(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = samLcsService.samLcs(
                request.getWord(), request.getWords().get(0));
        return ResponseEntity.ok(new TraceResponse(
                "Longest Common Substring", steps));
    }

    // ---- Range queries v3 (Phase 27) ----

    @PostMapping("/range/lazyseg")
    public ResponseEntity<?> rangeLazySeg(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateLazySeg(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = lazySegService.lazySeg(
                request.getWord(), request.getValues(),
                request.getIndex());
        return ResponseEntity.ok(new TraceResponse(
                "Lazy Segment Tree", steps));
    }

    @PostMapping("/range/pstkth")
    public ResponseEntity<?> rangePstKth(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validatePstKth(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = pstKthService.pstKth(
                request.getWord(), request.getValues());
        return ResponseEntity.ok(new TraceResponse(
                "Persistent Segment Tree: K-th Smallest", steps));
    }

    @PostMapping("/range/sqrtdecomp2")
    public ResponseEntity<?> rangeSqrtDecomp2(
            @RequestBody TraceRequest request) {
        ResponseEntity<?> error = validateSqrtDecomp2(request);
        if (error != null) {
            return error;
        }
        List<Step> steps = sqrtDecomp2Service.sqrtDecomp2(
                request.getWord(), request.getValues(),
                request.getIndex());
        return ResponseEntity.ok(new TraceResponse(
                "Sqrt Decomposition v2", steps));
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
     * 400 when the grid is missing, not 9 rows of 9, any value
     * is outside 0..9, or the givens are inconsistent (a
     * duplicate in a row, column or 3×3 box). Null means
     * "no error".
     */
    private ResponseEntity<?> validateSudoku(TraceRequest request) {
        if (request == null || request.getGrid() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "grid must not be null"));
        }
        List<List<Integer>> grid = request.getGrid();
        if (grid.size() != 9) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "grid must be 9 rows of 9"));
        }
        for (List<Integer> row : grid) {
            if (row == null || row.size() != 9) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "grid must be 9 rows of 9"));
            }
            for (Integer v : row) {
                if (v == null || v < 0 || v > 9) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error",
                                    "grid values must be between "
                                            + "0 and 9"));
                }
            }
        }
        for (int i = 0; i < 9; i++) {
            boolean[] rowSeen = new boolean[10];
            boolean[] colSeen = new boolean[10];
            boolean[] boxSeen = new boolean[10];
            for (int j = 0; j < 9; j++) {
                int rowVal = grid.get(i).get(j);
                if (rowVal != 0) {
                    if (rowSeen[rowVal]) {
                        return inconsistentGivens();
                    }
                    rowSeen[rowVal] = true;
                }
                int colVal = grid.get(j).get(i);
                if (colVal != 0) {
                    if (colSeen[colVal]) {
                        return inconsistentGivens();
                    }
                    colSeen[colVal] = true;
                }
                int boxVal = grid.get(3 * (i / 3) + j / 3)
                        .get(3 * (i % 3) + j % 3);
                if (boxVal != 0) {
                    if (boxSeen[boxVal]) {
                        return inconsistentGivens();
                    }
                    boxSeen[boxVal] = true;
                }
            }
        }
        return null;
    }

    /** The 400 for inconsistent Sudoku givens. */
    private ResponseEntity<?> inconsistentGivens() {
        return ResponseEntity.badRequest()
                .body(Map.of("error",
                        "givens must be consistent (no duplicate in "
                                + "a row, column or box)"));
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

    /**
     * 400 when values are missing/empty, longer than 8, or any
     * |value| is over 99 (the BIT strip must stay drawable).
     * Null means "no error".
     */
    private ResponseEntity<?> validateFenwick(TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        if (request.getValues().size() > 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "fenwick tree needs at most 8 values"));
        }
        for (int v : request.getValues()) {
            if (Math.abs(v) > 99) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "fenwick tree needs |values| <= 99"));
            }
        }
        return null;
    }

    /**
     * 400 when the Fenwick values are invalid, or index is
     * missing or outside 1..n (Fenwick indices are 1-based).
     * Null means "no error".
     */
    private ResponseEntity<?> validateFenwickIndex(
            TraceRequest request) {
        ResponseEntity<?> error = validateFenwick(request);
        if (error != null) {
            return error;
        }
        if (request.getIndex() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "index must not be null"));
        }
        int index = request.getIndex();
        int n = request.getValues().size();
        if (index < 1 || index > n) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "index must be between 1 and " + n));
        }
        return null;
    }

    /**
     * 400 when the Fenwick values/index are invalid, or the delta
     * (value) is missing, outside -99..99, or 0. Null means
     * "no error".
     */
    private ResponseEntity<?> validateFenwickDelta(
            TraceRequest request) {
        ResponseEntity<?> error = validateFenwickIndex(request);
        if (error != null) {
            return error;
        }
        if (request.getValue() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "delta must not be null"));
        }
        int delta = request.getValue();
        if (delta == 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Delta must not be 0."));
        }
        if (delta < -99 || delta > 99) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "delta must be between -99 and 99"));
        }
        return null;
    }

    /**
     * 400 when values are missing/empty, shorter than 2 or longer
     * than 10, any |value| is over 999, or k (value) is missing
     * or outside 1..n. Null means "no error".
     */
    private ResponseEntity<?> validateQuickSelect(
            TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        if (request.getValues().size() < 2
                || request.getValues().size() > 10) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "quickselect needs 2 to 10 values"));
        }
        for (int v : request.getValues()) {
            if (Math.abs(v) > 999) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "quickselect needs |values| <= 999"));
            }
        }
        if (request.getValue() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "k must not be null"));
        }
        int k = request.getValue();
        int n = request.getValues().size();
        if (k < 1 || k > n) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "k must be between 1 and " + n));
        }
        return null;
    }

    /**
     * 400 when the capacity is missing or outside 1..5, words is
     * missing/empty or longer than 10, or any op fails to parse
     * as "put &lt;key&gt; &lt;value&gt;" or "get &lt;key&gt;" with
     * a key in 0..99 and a value in 0..999. Null means "no error".
     */
    private ResponseEntity<?> validateLru(TraceRequest request) {
        if (request == null || request.getCapacity() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "capacity must not be null"));
        }
        int capacity = request.getCapacity();
        if (capacity < 1 || capacity > 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "capacity must be between 1 and 5"));
        }
        if (request.getWords() == null || request.getWords().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "lru cache needs at least one operation"));
        }
        if (request.getWords().size() > 10) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "lru cache needs at most 10 operations"));
        }
        for (String op : request.getWords()) {
            ResponseEntity<?> error = validateLruOp(op);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    /**
     * 400 when one op is not "put &lt;key&gt; &lt;value&gt;" or
     * "get &lt;key&gt;" with a key in 0..99 and a value in 0..999.
     * Null means "no error".
     */
    private ResponseEntity<?> validateLruOp(String op) {
        String[] parts = op == null ? new String[0]
                : op.trim().split("\\s+");
        if (parts.length == 3 && "put".equals(parts[0])) {
            try {
                int key = Integer.parseInt(parts[1]);
                int value = Integer.parseInt(parts[2]);
                if (key < 0 || key > 99) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error",
                                    "key must be between 0 and 99"));
                }
                if (value < 0 || value > 999) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error",
                                    "value must be between 0 and 999"));
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "Op must be 'put <key> <value>' or "
                                        + "'get <key>'."));
            }
            return null;
        }
        if (parts.length == 2 && "get".equals(parts[0])) {
            try {
                int key = Integer.parseInt(parts[1]);
                if (key < 0 || key > 99) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error",
                                    "key must be between 0 and 99"));
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "Op must be 'put <key> <value>' or "
                                        + "'get <key>'."));
            }
            return null;
        }
        return ResponseEntity.badRequest()
                .body(Map.of("error",
                        "Op must be 'put <key> <value>' or "
                                + "'get <key>'."));
    }

    /**
     * 400 when the capacity is missing or outside 1..5, or the
     * ops fail the LRU op rules (at least 1, at most 10 ops).
     * The op format is identical to LRU, so validateLruOp is
     * reused. Null means "no error".
     */
    private ResponseEntity<?> validateLfu(TraceRequest request) {
        if (request == null || request.getCapacity() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "capacity must not be null"));
        }
        int capacity = request.getCapacity();
        if (capacity < 1 || capacity > 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "capacity must be between 1 and 5"));
        }
        if (request.getWords() == null || request.getWords().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "lfu cache needs at least one operation"));
        }
        if (request.getWords().size() > 10) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "lfu cache needs at most 10 operations"));
        }
        for (String op : request.getWords()) {
            ResponseEntity<?> error = validateLruOp(op);
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    /**
     * 400 when values are missing/empty, longer than 12, or any
     * |value| is over 99. Null means "no error".
     */
    private ResponseEntity<?> validateBTree(TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        if (request.getValues().size() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "b-tree needs at most 12 values"));
        }
        for (int v : request.getValues()) {
            if (Math.abs(v) > 99) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "b-tree needs |values| <= 99"));
            }
        }
        return null;
    }

    /**
     * 400 when words is missing/empty or longer than 6, any word
     * is empty or longer than 12 characters, or the word to check
     * is missing/empty or longer than 12 characters. Null means
     * "no error".
     */
    private ResponseEntity<?> validateBloom(TraceRequest request) {
        if (request == null || request.getWords() == null
                || request.getWords().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "bloom filter needs words to insert"));
        }
        if (request.getWords().size() > 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "bloom filter needs at most 6 words"));
        }
        for (String word : request.getWords()) {
            if (word == null || word.isEmpty()
                    || word.length() > 12) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "words must be 1 to 12 characters"));
            }
        }
        if (request.getWord() == null || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "the word to check must not be empty"));
        }
        if (request.getWord().length() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "the word to check must be at most 12 "
                                    + "characters"));
        }
        return null;
    }

    /** Null-safe values accessor (null request or null values → null). */
    private List<Integer> valuesOf(TraceRequest request) {
        return request == null ? null : request.getValues();
    }

    /**
     * 400 when values are bad (the B-tree rules) or the target is
     * missing. Null means "no error".
     */
    private ResponseEntity<?> validateBTreeSearch(TraceRequest request) {
        ResponseEntity<?> error = validateBTree(request);
        if (error != null) {
            return error;
        }
        if (request.getTarget() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "target must not be null"));
        }
        return null;
    }

    /**
     * 400 when values are bad (the B-tree rules) or the value is
     * missing. Null means "no error".
     */
    private ResponseEntity<?> validateBTreeDelete(TraceRequest request) {
        ResponseEntity<?> error = validateBTree(request);
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
     * 400 when graphEdges is missing/empty, there are fewer than
     * 2 or more than 12 edges, any edge is not
     * [from, to, capacity], any capacity is outside 1..99, any
     * node id is outside 0..9, or start/target is missing,
     * equal, or not a node in the graph. The 2..12 edge cap is
     * the SCC cap. Null means "no error".
     */
    private ResponseEntity<?> validateMaxFlow(TraceRequest request) {
        if (request == null || request.getGraphEdges() == null
                || request.getGraphEdges().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "graphEdges must not be empty"));
        }
        if (request.getGraphEdges().size() < 2
                || request.getGraphEdges().size() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "max flow needs between 2 and 12 edges"));
        }
        Set<Integer> nodes = new HashSet<>();
        for (List<Integer> edge : request.getGraphEdges()) {
            if (edge == null || edge.size() != 3) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "each edge must be "
                                        + "[from, to, capacity]"));
            }
            Integer u = edge.get(0);
            Integer v = edge.get(1);
            Integer c = edge.get(2);
            if (u == null || v == null || u < 0 || u > 9
                    || v < 0 || v > 9) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "max flow node ids must be between "
                                        + "0 and 9"));
            }
            if (c == null || c < 1 || c > 99) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "max flow capacity must be between "
                                        + "1 and 99"));
            }
            nodes.add(u);
            nodes.add(v);
        }
        if (request.getStart() == null || request.getTarget() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "start and target must not be null"));
        }
        if (request.getStart().equals(request.getTarget())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "source and sink must be different"));
        }
        if (!nodes.contains(request.getStart())
                || !nodes.contains(request.getTarget())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "start and target must be nodes in the "
                                    + "graph"));
        }
        return null;
    }

    /**
     * 400 when the text is missing/empty, longer than 30
     * characters, or not lowercase a–z, or patterns are
     * missing/empty, more than 5, or any pattern is empty,
     * longer than 10 characters, or not lowercase a–z.
     * Null means "no error".
     */
    private ResponseEntity<?> validateAhoCorasick(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        if (request.getWord().length() > 30) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "aho-corasick needs a text of at most 30 "
                                    + "characters"));
        }
        for (char c : request.getWord().toCharArray()) {
            if (c < 'a' || c > 'z') {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "aho-corasick needs lowercase a-z only"));
            }
        }
        if (request.getWords() == null || request.getWords().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "aho-corasick needs at least one pattern"));
        }
        if (request.getWords().size() > 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "aho-corasick needs at most 5 patterns"));
        }
        for (String pattern : request.getWords()) {
            if (pattern == null || pattern.isEmpty()
                    || pattern.length() > 10) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "patterns must be 1 to 10 characters"));
            }
            for (char c : pattern.toCharArray()) {
                if (c < 'a' || c > 'z') {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error",
                                    "patterns need lowercase a-z only"));
                }
            }
        }
        return null;
    }

    /**
     * 400 when graphEdges is missing/empty, there are fewer than
     * 2 or more than 12 edges, any edge is not [from, to], any
     * node id is outside 0..20, or a given start is not a node
     * in the graph. A missing start is allowed — it defaults to
     * the smallest node. Null means "no error".
     */
    private ResponseEntity<?> validateBridges(TraceRequest request) {
        if (request == null || request.getGraphEdges() == null
                || request.getGraphEdges().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "graphEdges must not be empty"));
        }
        if (request.getGraphEdges().size() < 2
                || request.getGraphEdges().size() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "bridges need between 2 and 12 edges"));
        }
        Set<Integer> nodes = new HashSet<>();
        for (List<Integer> edge : request.getGraphEdges()) {
            if (edge == null || edge.size() != 2) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "each edge must be [from, to]"));
            }
            Integer u = edge.get(0);
            Integer v = edge.get(1);
            if (u == null || v == null || u < 0 || u > 20
                    || v < 0 || v > 20) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "bridges node ids must be between "
                                        + "0 and 20"));
            }
            nodes.add(u);
            nodes.add(v);
        }
        if (request.getStart() != null
                && !nodes.contains(request.getStart())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "start must be a node in the graph"));
        }
        return null;
    }

    /**
     * 400 when the word is missing/empty, longer than 20
     * characters, or not lowercase a–z. Null means "no error".
     */
    private ResponseEntity<?> validateManacher(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        if (request.getWord().length() > 20) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "manacher needs a word of at most 20 "
                                    + "characters"));
        }
        for (char c : request.getWord().toCharArray()) {
            if (c < 'a' || c > 'z') {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "manacher needs lowercase a-z only"));
            }
        }
        return null;
    }

    /** The node ids appearing in graphEdges (well-formed pairs only). */
    private Set<Integer> graphNodes(TraceRequest request) {
        Set<Integer> nodes = new HashSet<>();
        for (List<Integer> edge : request.getGraphEdges()) {
            if (edge == null || (edge.size() != 2 && edge.size() != 3)) {
                continue;
            }
            if (edge.get(0) != null) {
                nodes.add(edge.get(0));
            }
            if (edge.get(1) != null) {
                nodes.add(edge.get(1));
            }
        }
        return nodes;
    }

    /**
     * 400 when graphEdges/start is missing or any weight is
     * &lt;= 0, or the target is missing, start == target, or
     * start/target is not a node in the graph. Null means
     * "no error".
     */
    private ResponseEntity<?> validateBidirectionalDijkstra(
            TraceRequest request) {
        ResponseEntity<?> error = validateGraph(request);
        if (error != null) {
            return error;
        }
        error = validateWeights(request, "Bidirectional Dijkstra");
        if (error != null) {
            return error;
        }
        return validateTargetNodes(request);
    }

    /**
     * 400 when the target is missing, start == target, or
     * start/target is not a node in the graph. (The graph and
     * weight checks happen in the caller.) Null means
     * "no error".
     */
    private ResponseEntity<?> validateTargetNodes(TraceRequest request) {
        if (request.getTarget() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "target must not be null"));
        }
        if (request.getStart().equals(request.getTarget())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "start and target must be different"));
        }
        Set<Integer> nodes = graphNodes(request);
        if (!nodes.contains(request.getStart())
                || !nodes.contains(request.getTarget())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "start and target must be nodes in the "
                                    + "graph"));
        }
        return null;
    }

    /**
     * 400 when graphEdges/start is missing or any weight is
     * &lt;= 0, the target is missing, start == target,
     * start/target is not a node in the graph, or a given
     * contraction order (values) is not a permutation of the
     * graph's nodes (a missing order defaults to ascending
     * node id). Null means "no error".
     */
    private ResponseEntity<?> validateContractionHierarchies(
            TraceRequest request) {
        ResponseEntity<?> error = validateGraph(request);
        if (error != null) {
            return error;
        }
        error = validateWeights(request, "Contraction Hierarchies");
        if (error != null) {
            return error;
        }
        error = validateTargetNodes(request);
        if (error != null) {
            return error;
        }
        List<Integer> order = request.getValues();
        if (order != null) {
            Set<Integer> nodes = graphNodes(request);
            if (order.size() != nodes.size()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "contraction order must be a "
                                        + "permutation of the graph's "
                                        + "nodes"));
            }
            Set<Integer> seen = new HashSet<>();
            for (Integer node : order) {
                if (node == null || !seen.add(node)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error",
                                    "contraction order must be a "
                                            + "permutation of the graph's "
                                            + "nodes"));
                }
            }
            if (!seen.equals(nodes)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "contraction order must be a "
                                        + "permutation of the graph's "
                                        + "nodes"));
            }
        }
        return null;
    }

    /**
     * 400 when the word is missing or not a "lat,lon" pair,
     * lat is outside [-90, 90], lon is outside [-180, 180],
     * or the precision (index) is outside 1..12 (a missing
     * precision defaults to 6). Null means "no error".
     */
    private ResponseEntity<?> validateGeohash(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        String[] parts = request.getWord().split(",", -1);
        if (parts.length != 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "geohash needs a \"lat,lon\" pair"));
        }
        double lat;
        double lon;
        try {
            lat = Double.parseDouble(parts[0].trim());
            lon = Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "geohash needs a \"lat,lon\" pair"));
        }
        if (!Double.isFinite(lat) || !Double.isFinite(lon)
                || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "geohash needs latitude between -90 and 90 "
                                    + "and longitude between -180 and "
                                    + "180"));
        }
        Integer precision = request.getIndex();
        if (precision != null && (precision < 1 || precision > 12)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "geohash needs a precision between 1 and 12"));
        }
        return null;
    }

    /**
     * 400 when the word is missing or not a "x,y;x,y;..."
     * point list, a point is outside the 100x100 map, the
     * capacity is outside 1..16 (a missing capacity defaults
     * to 2), or a query rect (values) is not 4 numbers (a
     * missing rect defaults to the whole bounds). Null means
     * "no error".
     */
    private ResponseEntity<?> validateQuadtree(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        List<int[]> points = parsePointList(request.getWord());
        if (points == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "quadtree needs points as \"x,y;x,y;...\""));
        }
        for (int[] p : points) {
            if (p[0] < 0 || p[0] > 100 || p[1] < 0 || p[1] > 100) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "quadtree points must be inside the "
                                        + "100x100 map"));
            }
        }
        Integer capacity = request.getCapacity();
        if (capacity != null && (capacity < 1 || capacity > 16)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "quadtree needs a capacity between 1 and 16"));
        }
        List<Integer> rect = request.getValues();
        if (rect != null && !rect.isEmpty() && rect.size() != 4) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "quadtree needs a query rect "
                                    + "[x0, y0, x1, y1]"));
        }
        return null;
    }

    /**
     * 400 when the word is missing or not a "x,y;x,y;..."
     * point list, or a query point (values) is not 2 numbers
     * (a missing query defaults to (9,2)). Null means
     * "no error".
     */
    private ResponseEntity<?> validateKdTree(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        if (parsePointList(request.getWord()) == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "k-d tree needs points as \"x,y;x,y;...\""));
        }
        List<Integer> query = request.getValues();
        if (query != null && !query.isEmpty() && query.size() != 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "k-d tree needs a query point [x, y]"));
        }
        return null;
    }

    /**
     * 400 when the word is missing or not a
     * "x1,y1,x2,y2;..." rect list, a rect has x1 &gt;= x2 or
     * y1 &gt;= y2, the capacity is outside 2..8 (a missing
     * capacity defaults to 4), or a query rect (values) is not
     * 4 numbers (a missing rect defaults to (12,12,30,30)).
     * Null means "no error".
     */
    private ResponseEntity<?> validateRTree(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        List<int[]> rects = parseRectList(request.getWord());
        if (rects == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "R-tree needs rectangles as "
                                    + "\"x1,y1,x2,y2;...\""));
        }
        for (int[] r : rects) {
            if (r[0] >= r[2] || r[1] >= r[3]) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "R-tree rectangles need x1 < x2 and "
                                        + "y1 < y2"));
            }
        }
        Integer capacity = request.getCapacity();
        if (capacity != null && (capacity < 2 || capacity > 8)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "R-tree needs a capacity between 2 and 8"));
        }
        List<Integer> query = request.getValues();
        if (query != null && !query.isEmpty() && query.size() != 4) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "R-tree needs a query rect "
                                    + "[x0, y0, x1, y1]"));
        }
        return null;
    }

    /**
     * 400 when graphEdges/start is missing or any weight is
     * &lt;= 0, the target is missing, start == target, or
     * start/target is not a node in the graph, or K (index) is
     * outside 1..5 (a missing K defaults to 3). Null means
     * "no error".
     */
    private ResponseEntity<?> validateYen(TraceRequest request) {
        ResponseEntity<?> error = validateBidirectionalDijkstra(request);
        if (error != null) {
            return error;
        }
        Integer k = request.getIndex();
        if (k != null && (k < 1 || k > 5)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Yen's algorithm needs K between 1 and 5"));
        }
        return null;
    }

    /**
     * 400 when graphEdges/start is missing or any weight is
     * &lt;= 0, the target is missing, start == target, or
     * start/target is not a node in the graph, the departure
     * hour (index) is outside 0..23 (a missing hour defaults
     * to 8), or rush factors (values) have a non-positive entry
     * or a length different from the edge count (missing
     * factors default to all 1x). Null means "no error".
     */
    private ResponseEntity<?> validateTraffic(TraceRequest request) {
        ResponseEntity<?> error = validateBidirectionalDijkstra(request);
        if (error != null) {
            return error;
        }
        Integer hour = request.getIndex();
        if (hour != null && (hour < 0 || hour > 23)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "traffic needs a departure hour between "
                                    + "0 and 23"));
        }
        List<Integer> factors = request.getValues();
        if (factors != null && !factors.isEmpty()) {
            if (factors.size() != request.getGraphEdges().size()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "rush factors must be parallel to the "
                                        + "edges"));
            }
            for (Integer factor : factors) {
                if (factor == null || factor <= 0) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error",
                                    "rush factors must be positive"));
                }
            }
        }
        return null;
    }

    /**
     * 400 when the word is missing or not a "x,y;x,y;..."
     * point list, or fewer than 3 points. Null means
     * "no error".
     */
    private ResponseEntity<?> validateTsp(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        List<int[]> points = parsePointList(request.getWord());
        if (points == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "TSP needs points as \"x,y;x,y;...\""));
        }
        if (points.size() < 3) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "TSP needs at least 3 points"));
        }
        return null;
    }

    /**
     * 400 when the word is missing or not a "x,y;x,y;..."
     * point list, or fewer than 3 points. Null means
     * "no error".
     */
    private ResponseEntity<?> validateConvexHull(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        List<int[]> points = parsePointList(request.getWord());
        if (points == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "convex hull needs points as "
                                    + "\"x,y;x,y;...\""));
        }
        if (points.size() < 3) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "convex hull needs at least 3 points"));
        }
        return null;
    }

    /**
     * 400 when the word is missing or not a "x,y;x,y;..."
     * point list, or fewer than 2 points. Null means
     * "no error".
     */
    private ResponseEntity<?> validateClosestPair(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        List<int[]> points = parsePointList(request.getWord());
        if (points == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "closest pair needs points as "
                                    + "\"x,y;x,y;...\""));
        }
        if (points.size() < 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "closest pair needs at least 2 points"));
        }
        return null;
    }

    /**
     * 400 when the word is missing or not a
     * "x1,y1,x2,y2;..." segment list. Null means
     * "no error".
     */
    private ResponseEntity<?> validateLineSweep(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        if (parseSegmentList(request.getWord()) == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "line sweep needs segments as "
                                    + "\"x1,y1,x2,y2;...\""));
        }
        return null;
    }

    /**
     * 400 when the word is missing or not a "x,y;x,y;..."
     * point list, fewer than 2 sites, or a site is outside
     * the fixed [0,0,10,10] box. Null means "no error".
     */
    private ResponseEntity<?> validateVoronoi(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        List<int[]> points = parsePointList(request.getWord());
        if (points == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Voronoi needs sites as "
                                    + "\"x,y;x,y;...\""));
        }
        if (points.size() < 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Voronoi needs at least 2 sites"));
        }
        for (int[] p : points) {
            if (p[0] < 0 || p[0] > 10 || p[1] < 0 || p[1] > 10) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "Voronoi sites must be inside the "
                                        + "[0,0,10,10] box"));
            }
        }
        return null;
    }

    /**
     * 400 when the documents (words) are missing/empty (a
     * missing query word defaults to "tree traversal" in the
     * service). Null means "no error".
     */
    private ResponseEntity<?> validateInvertedIndex(TraceRequest request) {
        if (request == null || request.getWords() == null
                || request.getWords().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "inverted index needs documents (words)"));
        }
        return null;
    }

    /**
     * 400 when the documents (words) are missing/empty (a
     * missing query word defaults to "binary tree" in the
     * service). Null means "no error".
     */
    private ResponseEntity<?> validateTfidf(TraceRequest request) {
        if (request == null || request.getWords() == null
                || request.getWords().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "TF-IDF needs documents (words)"));
        }
        return null;
    }

    /**
     * 400 when graphEdges is missing/empty, any edge is not
     * a [u, v] pair, any node id is negative or outside the
     * page range, the iterations (index) are outside 1..20
     * (a missing count defaults to 10), or the page count
     * (value) is outside 1..20 or below max-node-id + 1 (a
     * missing count defaults to max-node-id + 1). Null means
     * "no error".
     */
    private ResponseEntity<?> validatePageRank(TraceRequest request) {
        if (request == null || request.getGraphEdges() == null
                || request.getGraphEdges().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "PageRank needs links (graphEdges)"));
        }
        int maxNode = -1;
        for (List<Integer> edge : request.getGraphEdges()) {
            if (edge == null || edge.size() < 2
                    || edge.get(0) == null || edge.get(1) == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "PageRank links must be [u, v] pairs"));
            }
            if (edge.get(0) < 0 || edge.get(1) < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "PageRank page ids must be >= 0"));
            }
            maxNode = Math.max(maxNode,
                    Math.max(edge.get(0), edge.get(1)));
        }
        Integer pageCount = request.getValue();
        if (pageCount != null) {
            if (pageCount < 1 || pageCount > 20) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "PageRank needs a page count "
                                        + "between 1 and 20"));
            }
            if (pageCount <= maxNode) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "PageRank page count must cover "
                                        + "every linked page"));
            }
        } else if (maxNode >= 20) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "PageRank needs at most 20 pages"));
        }
        Integer iterations = request.getIndex();
        if (iterations != null && (iterations < 1 || iterations > 20)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "PageRank needs 1..20 iterations"));
        }
        return null;
    }

    /**
     * 400 when graphEdges is missing/empty, any edge is not
     * a [parent, child] pair, any node id is negative or
     * above 20, a node has two parents, there is no single
     * root, or the edges do not connect the tree (a given
     * start/target must be a node in the tree; a missing
     * start/target defaults to 7/5). Null means "no error".
     */
    private ResponseEntity<?> validateHld(TraceRequest request) {
        if (request == null || request.getGraphEdges() == null
                || request.getGraphEdges().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "HLD needs tree edges (graphEdges)"));
        }
        Map<Integer, Integer> parent = new java.util.HashMap<>();
        Set<Integer> nodes = new HashSet<>();
        for (List<Integer> edge : request.getGraphEdges()) {
            if (edge == null || edge.size() < 2
                    || edge.get(0) == null || edge.get(1) == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "HLD edges must be [parent, child] "
                                        + "pairs"));
            }
            if (edge.get(0) < 0 || edge.get(1) < 0
                    || edge.get(0) > 20 || edge.get(1) > 20) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "HLD node ids must be between "
                                        + "0 and 20"));
            }
            if (parent.containsKey(edge.get(1))) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "HLD needs a tree: node "
                                        + edge.get(1)
                                        + " has two parents"));
            }
            parent.put(edge.get(1), edge.get(0));
            nodes.add(edge.get(0));
            nodes.add(edge.get(1));
        }
        int roots = 0;
        int root = -1;
        for (int node : nodes) {
            if (!parent.containsKey(node)) {
                roots++;
                root = node;
            }
        }
        if (roots != 1) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "HLD needs a tree with exactly one "
                                    + "root"));
        }
        // Connectivity: every node reachable from the root.
        Set<Integer> seen = new HashSet<>();
        List<Integer> stack = new ArrayList<>();
        stack.add(root);
        Map<Integer, List<Integer>> children = new java.util.HashMap<>();
        for (List<Integer> edge : request.getGraphEdges()) {
            children.computeIfAbsent(edge.get(0),
                    k -> new ArrayList<>()).add(edge.get(1));
        }
        while (!stack.isEmpty()) {
            int node = stack.remove(stack.size() - 1);
            if (!seen.add(node)) {
                continue;
            }
            stack.addAll(children.getOrDefault(node, List.of()));
        }
        if (seen.size() != nodes.size()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "HLD needs a connected tree"));
        }
        if (request.getStart() != null
                && !nodes.contains(request.getStart())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "HLD start must be a node in the tree"));
        }
        if (request.getTarget() != null
                && !nodes.contains(request.getTarget())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "HLD target must be a node in the tree"));
        }
        return null;
    }

    /**
     * 400 when graphEdges is missing/empty, any edge is not
     * a [u, v] pair, or any node id is negative or above 20.
     * Null means "no error".
     */
    private ResponseEntity<?> validateCentroid(TraceRequest request) {
        if (request == null || request.getGraphEdges() == null
                || request.getGraphEdges().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "centroid decomposition needs tree "
                                    + "edges (graphEdges)"));
        }
        for (List<Integer> edge : request.getGraphEdges()) {
            if (edge == null || edge.size() < 2
                    || edge.get(0) == null || edge.get(1) == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "centroid edges must be [u, v] "
                                        + "pairs"));
            }
            if (edge.get(0) < 0 || edge.get(1) < 0
                    || edge.get(0) > 20 || edge.get(1) > 20) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "centroid node ids must be between "
                                        + "0 and 20"));
            }
        }
        return null;
    }

    /**
     * 400 when the word is missing or not a comma list of
     * numbers, the array is longer than 8 or any |value| is
     * over 999, values is missing, odd or shorter than 2, an
     * update index is out of range, an update value has
     * |value| over 999, or the query range is out of range
     * or reversed. Null means "no error".
     */
    private ResponseEntity<?> validatePersistentSeg(
            TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "persistent segment tree needs an "
                                    + "array (word)"));
        }
        int[] array = parseIntList(request.getWord());
        if (array == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "the array must be comma-separated "
                                    + "numbers"));
        }
        if (array.length < 1 || array.length > 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "the array needs between 1 and 8 "
                                    + "elements"));
        }
        for (int value : array) {
            if (Math.abs(value) > 999) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "array values must be between "
                                        + "-999 and 999"));
            }
        }
        if (request.getValues() == null
                || request.getValues().size() < 2
                || request.getValues().size() % 2 != 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "persistent segment tree needs "
                                    + "[updateIdx, updateVal, ..., "
                                    + "queryL, queryR] (values)"));
        }
        int updateCount = (request.getValues().size() - 2) / 2;
        for (int i = 0; i < updateCount; i++) {
            int index = request.getValues().get(2 * i);
            int value = request.getValues().get(2 * i + 1);
            if (index < 0 || index >= array.length) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "update index " + index
                                        + " is out of range"));
            }
            if (Math.abs(value) > 999) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "update values must be between "
                                        + "-999 and 999"));
            }
        }
        int queryLeft = request.getValues()
                .get(request.getValues().size() - 2);
        int queryRight = request.getValues()
                .get(request.getValues().size() - 1);
        if (queryLeft < 0 || queryRight >= array.length
                || queryLeft > queryRight) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "query range [" + queryLeft + ", "
                                    + queryRight + "] is out of "
                                    + "range"));
        }
        return null;
    }

    /**
     * 400 when graphEdges is missing/empty, any edge is not
     * a [left, right] pair, any node id is negative or above
     * 9, an edge is a self-loop, or a node appears on both
     * sides. Null means "no error".
     */
    private ResponseEntity<?> validateHopcroftKarp(
            TraceRequest request) {
        if (request == null || request.getGraphEdges() == null
                || request.getGraphEdges().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Hopcroft-Karp needs bipartite edges "
                                    + "(graphEdges)"));
        }
        Set<Integer> left = new HashSet<>();
        Set<Integer> right = new HashSet<>();
        for (List<Integer> edge : request.getGraphEdges()) {
            if (edge == null || edge.size() < 2
                    || edge.get(0) == null || edge.get(1) == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "Hopcroft-Karp edges must be "
                                        + "[left, right] pairs"));
            }
            if (edge.get(0) < 0 || edge.get(1) < 0
                    || edge.get(0) > 9 || edge.get(1) > 9) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "Hopcroft-Karp node ids must be "
                                        + "between 0 and 9"));
            }
            if (edge.get(0).equals(edge.get(1))) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "Hopcroft-Karp edges must not be "
                                        + "self-loops"));
            }
            left.add(edge.get(0));
            right.add(edge.get(1));
        }
        for (int node : left) {
            if (right.contains(node)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "Hopcroft-Karp node " + node
                                        + " appears on both sides"));
            }
        }
        return null;
    }

    /**
     * 400 like max flow (2..12 edges, [u, v, cap] triples,
     * capacity 1..99, node ids 0..9), plus a costs list
     * (values) whose length differs from the edge count or
     * any cost outside -99..99 (a missing costs list
     * defaults to all zero; a missing start/target defaults
     * to the smallest/largest node id). Null means "no error".
     */
    private ResponseEntity<?> validateMinCostFlow(
            TraceRequest request) {
        if (request == null || request.getGraphEdges() == null
                || request.getGraphEdges().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "min-cost max-flow needs network edges "
                                    + "(graphEdges)"));
        }
        if (request.getGraphEdges().size() < 2
                || request.getGraphEdges().size() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "min-cost max-flow needs between 2 and "
                                    + "12 edges"));
        }
        Set<Integer> nodes = new HashSet<>();
        for (List<Integer> edge : request.getGraphEdges()) {
            if (edge == null || edge.size() != 3) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "each edge must be "
                                        + "[from, to, capacity]"));
            }
            Integer u = edge.get(0);
            Integer v = edge.get(1);
            Integer c = edge.get(2);
            if (u == null || v == null || u < 0 || u > 9
                    || v < 0 || v > 9) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "min-cost max-flow node ids must be "
                                        + "between 0 and 9"));
            }
            if (c == null || c < 1 || c > 99) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "min-cost max-flow capacity must be "
                                        + "between 1 and 99"));
            }
            nodes.add(u);
            nodes.add(v);
        }
        if (request.getValues() != null) {
            if (request.getValues().size()
                    != request.getGraphEdges().size()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "min-cost max-flow needs one cost "
                                        + "per edge"));
            }
            for (Integer cost : request.getValues()) {
                if (cost == null || cost < -99 || cost > 99) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error",
                                    "min-cost max-flow costs must be "
                                            + "between -99 and 99"));
                }
            }
        }
        if (request.getStart() != null
                && request.getTarget() != null
                && request.getStart().equals(request.getTarget())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "source and sink must be different"));
        }
        if (request.getStart() != null
                && !nodes.contains(request.getStart())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "start must be a node in the network"));
        }
        if (request.getTarget() != null
                && !nodes.contains(request.getTarget())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "target must be a node in the network"));
        }
        return null;
    }

    /**
     * 400 when the word is missing or not a "a,b,c;d,e,f;..."
     * matrix, the rows are not all the same length, the
     * matrix is not square, its size is outside 2..6, or any
     * cost is outside 0..99. Null means "no error".
     */
    private ResponseEntity<?> validateHungarian(
            TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Hungarian algorithm needs a cost "
                                    + "matrix (word)"));
        }
        String[] rows = request.getWord().trim().split(";", -1);
        if (rows.length < 2 || rows.length > 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Hungarian matrix size must be "
                                    + "between 2 and 6"));
        }
        for (String row : rows) {
            String[] cells = row.trim().split(",", -1);
            if (cells.length != rows.length) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "Hungarian matrix must be square"));
            }
            for (String cell : cells) {
                try {
                    int cost = Integer.parseInt(cell.trim());
                    if (cost < 0 || cost > 99) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error",
                                        "Hungarian costs must be "
                                                + "between 0 and 99"));
                    }
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error",
                                    "Hungarian matrix cells must be "
                                            + "numbers"));
                }
            }
        }
        return null;
    }

    private ResponseEntity<?> validateSam(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        if (request.getWord().length() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "sam needs a word of at most 12 "
                                    + "characters"));
        }
        for (char c : request.getWord().toCharArray()) {
            if (c < 'a' || c > 'z') {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "sam needs lowercase a-z only"));
            }
        }
        return null;
    }

    private ResponseEntity<?> validateEertree(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        if (request.getWord().length() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "eertree needs a word of at most 12 "
                                    + "characters"));
        }
        for (char c : request.getWord().toCharArray()) {
            if (c < 'a' || c > 'z') {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "eertree needs lowercase a-z only"));
            }
        }
        return null;
    }

    private ResponseEntity<?> validateLcpKasai(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        if (request.getWord().length() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "lcpkasai needs a word of at most 12 "
                                    + "characters"));
        }
        for (char c : request.getWord().toCharArray()) {
            if (c < 'a' || c > 'z') {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "lcpkasai needs lowercase a-z only"));
            }
        }
        return null;
    }

    /** Parses the array in {@code word}; null when it is not comma-separated ints. */
    private int[] parseArrayField(String word) {
        if (word == null || word.isBlank()) {
            return null;
        }
        String[] parts = word.trim().split(",", -1);
        if (parts.length < 1 || parts.length > 16) {
            return null;
        }
        int[] result = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                result[i] = Integer.parseInt(parts[i].trim());
                if (result[i] < -999 || result[i] > 999) {
                    return null;
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return result;
    }

    private ResponseEntity<?> validateSqrtDecomp(TraceRequest request) {
        int[] array = parseArrayField(
                request == null ? null : request.getWord());
        if (array == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "sqrt decomposition needs an array of "
                                    + "1..16 numbers, each between "
                                    + "-999 and 999"));
        }
        if (request.getValues() == null
                || request.getValues().size() != 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "sqrt decomposition needs the query "
                                    + "range as [l, r]"));
        }
        int left = request.getValues().get(0);
        int right = request.getValues().get(1);
        if (left < 0 || right >= array.length || left > right) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "sqrt decomposition query range [" + left
                                    + ", " + right + "] is out of "
                                    + "range"));
        }
        if (request.getIndex() == null || request.getIndex() < 0
                || request.getIndex() >= array.length
                || request.getCapacity() == null
                || request.getCapacity() < -999
                || request.getCapacity() > 999) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "sqrt decomposition needs an update "
                                    + "index in range and a value "
                                    + "between -999 and 999"));
        }
        return null;
    }

    private ResponseEntity<?> validateMos(TraceRequest request) {
        int[] array = parseArrayField(
                request == null ? null : request.getWord());
        if (array == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Mo's algorithm needs an array of 1..16 "
                                    + "numbers, each between -999 "
                                    + "and 999"));
        }
        if (request.getValues() == null
                || request.getValues().size() < 2
                || request.getValues().size() % 2 != 0
                || request.getValues().size() > 16) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "Mo's algorithm needs the queries as "
                                    + "flattened [l, r] pairs"));
        }
        for (int i = 0; i < request.getValues().size(); i += 2) {
            int left = request.getValues().get(i);
            int right = request.getValues().get(i + 1);
            if (left < 0 || right >= array.length || left > right) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "Mo's algorithm query range [" + left
                                        + ", " + right + "] is out "
                                        + "of range"));
            }
        }
        return null;
    }

    private ResponseEntity<?> validateSparseTable(TraceRequest request) {
        int[] array = parseArrayField(
                request == null ? null : request.getWord());
        if (array == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "sparse table needs an array of 1..16 "
                                    + "numbers, each between -999 "
                                    + "and 999"));
        }
        if (request.getValues() == null
                || request.getValues().size() != 4) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "sparse table needs two query ranges "
                                    + "as [l1, r1, l2, r2]"));
        }
        for (int i = 0; i < 4; i += 2) {
            int left = request.getValues().get(i);
            int right = request.getValues().get(i + 1);
            if (left < 0 || right >= array.length || left > right) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "sparse table query range [" + left
                                        + ", " + right + "] is out "
                                        + "of range"));
            }
        }
        return null;
    }

    private ResponseEntity<?> validateAhoCorasick2(TraceRequest request) {
        if (request == null || request.getWords() == null
                || request.getWords().isEmpty()
                || request.getWords().size() > 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "aho-corasick v2 needs 1..8 patterns"));
        }
        for (String pattern : request.getWords()) {
            if (pattern == null || pattern.isEmpty()
                    || pattern.length() > 8) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "aho-corasick v2 needs each pattern "
                                        + "to be 1..8 letters"));
            }
            for (char c : pattern.toCharArray()) {
                if (c < 'a' || c > 'z') {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error",
                                    "aho-corasick v2 needs lowercase "
                                            + "a-z only"));
                }
            }
        }
        if (request.getWord() == null || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        if (request.getWord().length() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "aho-corasick v2 needs a text of at most "
                                    + "12 characters"));
        }
        for (char c : request.getWord().toCharArray()) {
            if (c < 'a' || c > 'z') {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "aho-corasick v2 needs lowercase "
                                        + "a-z only"));
            }
        }
        return null;
    }

    private ResponseEntity<?> validateSuffixTrie(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        if (request.getWord().length() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "suffix trie needs a word of at most 12 "
                                    + "characters"));
        }
        for (char c : request.getWord().toCharArray()) {
            if (c < 'a' || c > 'z') {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "suffix trie needs lowercase a-z "
                                        + "only"));
            }
        }
        return null;
    }

    private ResponseEntity<?> validateSamLcs(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        if (request.getWord().length() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "longest common substring needs S1 of at "
                                    + "most 12 characters"));
        }
        for (char c : request.getWord().toCharArray()) {
            if (c < 'a' || c > 'z') {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "longest common substring needs "
                                        + "lowercase a-z only"));
            }
        }
        if (request.getWords() == null
                || request.getWords().size() != 1
                || request.getWords().get(0) == null
                || request.getWords().get(0).isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "longest common substring needs exactly "
                                    + "one S2 string"));
        }
        if (request.getWords().get(0).length() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "longest common substring needs S2 of at "
                                    + "most 12 characters"));
        }
        for (char c : request.getWords().get(0).toCharArray()) {
            if (c < 'a' || c > 'z') {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "longest common substring needs "
                                        + "lowercase a-z only"));
            }
        }
        return null;
    }

    private ResponseEntity<?> validateLazySeg(TraceRequest request) {
        int[] array = parseArrayField(
                request == null ? null : request.getWord());
        if (array == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "lazy segment tree needs an array of "
                                    + "1..16 numbers, each between "
                                    + "-999 and 999"));
        }
        if (request.getValues() == null
                || request.getValues().size() != 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "lazy segment tree needs the update "
                                    + "range and the two query ranges "
                                    + "as [ul, ur, q1l, q1r, q2l, q2r]"));
        }
        for (int i = 0; i < 6; i += 2) {
            int left = request.getValues().get(i);
            int right = request.getValues().get(i + 1);
            if (left < 0 || right >= array.length || left > right) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "lazy segment tree range [" + left
                                        + ", " + right + "] is out "
                                        + "of range"));
            }
        }
        if (request.getIndex() == null || request.getIndex() < -999
                || request.getIndex() > 999) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "lazy segment tree needs an add amount "
                                    + "between -999 and 999"));
        }
        return null;
    }

    private ResponseEntity<?> validatePstKth(TraceRequest request) {
        int[] array = parseArrayField(
                request == null ? null : request.getWord());
        if (array == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "persistent k-th smallest needs an array "
                                    + "of 1..16 numbers, each between "
                                    + "-999 and 999"));
        }
        if (request.getValues() == null
                || request.getValues().size() != 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "persistent k-th smallest needs two "
                                    + "queries as "
                                    + "[l1, r1, k1, l2, r2, k2]"));
        }
        for (int i = 0; i < 6; i += 3) {
            int left = request.getValues().get(i);
            int right = request.getValues().get(i + 1);
            int k = request.getValues().get(i + 2);
            if (left < 0 || right >= array.length || left > right
                    || k < 1 || k > right - left + 1) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "persistent k-th smallest query ["
                                        + left + ", " + right + "] "
                                        + "k=" + k + " is out of "
                                        + "range"));
            }
        }
        return null;
    }

    private ResponseEntity<?> validateSqrtDecomp2(TraceRequest request) {
        int[] array = parseArrayField(
                request == null ? null : request.getWord());
        if (array == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "sqrt decomposition v2 needs an array of "
                                    + "1..16 numbers, each between "
                                    + "-999 and 999"));
        }
        if (request.getValues() == null
                || request.getValues().size() != 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "sqrt decomposition v2 needs the update "
                                    + "range and the two query ranges "
                                    + "as [ul, ur, q1l, q1r, q2l, q2r]"));
        }
        for (int i = 0; i < 6; i += 2) {
            int left = request.getValues().get(i);
            int right = request.getValues().get(i + 1);
            if (left < 0 || right >= array.length || left > right) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "sqrt decomposition v2 range [" + left
                                        + ", " + right + "] is out "
                                        + "of range"));
            }
        }
        if (request.getIndex() == null || request.getIndex() < -999
                || request.getIndex() > 999) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "sqrt decomposition v2 needs an add "
                                    + "amount between -999 and 999"));
        }
        return null;
    }

    /** Parses "3,1,4,1,5" into an int array, or null. */
    private int[] parseIntList(String word) {
        String[] parts = word.trim().split(",", -1);
        int[] result = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                result[i] = Integer.parseInt(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return result;
    }

    /** Parses "x,y;x,y;..." into integer point pairs, or null. */
    private List<int[]> parsePointList(String word) {
        List<int[]> points = new ArrayList<>();
        for (String part : word.split(";", -1)) {
            String[] xy = part.trim().split(",", -1);
            if (xy.length != 2) {
                return null;
            }
            try {
                points.add(new int[]{
                        Integer.parseInt(xy[0].trim()),
                        Integer.parseInt(xy[1].trim())});
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return points.isEmpty() ? null : points;
    }

    /** Parses "x1,y1,x2,y2;..." into integer rects, or null. */
    private List<int[]> parseRectList(String word) {
        List<int[]> rects = new ArrayList<>();
        for (String part : word.split(";", -1)) {
            String[] xy = part.trim().split(",", -1);
            if (xy.length != 4) {
                return null;
            }
            try {
                int[] r = new int[4];
                for (int i = 0; i < 4; i++) {
                    r[i] = Integer.parseInt(xy[i].trim());
                }
                rects.add(r);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return rects.isEmpty() ? null : rects;
    }

    /** Parses "x1,y1,x2,y2;..." into integer segments, or null. */
    private List<int[]> parseSegmentList(String word) {
        List<int[]> segments = new ArrayList<>();
        for (String part : word.split(";", -1)) {
            String[] xy = part.trim().split(",", -1);
            if (xy.length != 4) {
                return null;
            }
            try {
                int[] s = new int[4];
                for (int i = 0; i < 4; i++) {
                    s[i] = Integer.parseInt(xy[i].trim());
                }
                segments.add(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return segments.isEmpty() ? null : segments;
    }

    /**
     * 400 when the word is missing/empty, longer than 12
     * characters, or not lowercase a–z. Null means "no error".
     */
    private ResponseEntity<?> validateSuffixArray(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        if (request.getWord().length() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "suffix array needs a word of at most 12 "
                                    + "characters"));
        }
        for (char c : request.getWord().toCharArray()) {
            if (c < 'a' || c > 'z') {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "suffix array needs lowercase a-z only"));
            }
        }
        return null;
    }

    /**
     * 400 when values are missing/empty, longer than 8, or any
     * |value| is over 99. Null means "no error".
     */
    private ResponseEntity<?> validateSkipList(TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        if (request.getValues().size() > 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "skip list needs at most 8 values"));
        }
        for (int v : request.getValues()) {
            if (Math.abs(v) > 99) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "skip list needs |values| <= 99"));
            }
        }
        return null;
    }

    /**
     * 400 when values are missing/empty, longer than 8, any
     * |value| is over 99, or the mode (word) is not linear,
     * quadratic or double. Null means "no error".
     */
    private ResponseEntity<?> validateProbing(TraceRequest request) {
        ResponseEntity<?> error = validateValues(request);
        if (error != null) {
            return error;
        }
        if (request.getValues().size() > 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "probing needs at most 8 values"));
        }
        for (int v : request.getValues()) {
            if (Math.abs(v) > 99) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "probing needs |values| <= 99"));
            }
        }
        String mode = request.getWord();
        if (mode == null || (!mode.equals("linear")
                && !mode.equals("quadratic") && !mode.equals("double"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "probing mode must be linear, quadratic "
                                    + "or double"));
        }
        return null;
    }

    /**
     * 400 when graphEdges is missing/empty, there are fewer than
     * 2 or more than 12 edges, any edge is not [from, to] or
     * [from, to, weight], or any node id is missing, below 0 or
     * above 9. Null means "no error".
     */
    private ResponseEntity<?> validateScc(TraceRequest request) {
        if (request == null || request.getGraphEdges() == null
                || request.getGraphEdges().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "graphEdges must not be empty"));
        }
        if (request.getGraphEdges().size() < 2
                || request.getGraphEdges().size() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "SCC needs between 2 and 12 edges"));
        }
        for (List<Integer> pair : request.getGraphEdges()) {
            if (pair == null || (pair.size() != 2 && pair.size() != 3)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "each edge must be [from, to]"));
            }
            Integer u = pair.get(0);
            Integer v = pair.get(1);
            if (u == null || v == null || u < 0 || u > 9
                    || v < 0 || v > 9) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "SCC node ids must be between 0 and 9"));
            }
        }
        return null;
    }

    /**
     * 400 when the word is missing/empty, longer than 12
     * characters, or not lowercase a–z. Null means "no error".
     */
    private ResponseEntity<?> validateHuffman(TraceRequest request) {
        if (request == null || request.getWord() == null
                || request.getWord().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "word must not be empty"));
        }
        if (request.getWord().length() > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "huffman needs a word of at most 12 "
                                    + "characters"));
        }
        for (char c : request.getWord().toCharArray()) {
            if (c < 'a' || c > 'z') {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "huffman needs lowercase a-z only"));
            }
        }
        return null;
    }
}
