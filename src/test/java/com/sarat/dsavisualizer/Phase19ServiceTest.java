package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.controller.VisualizationController;
import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.service.HashTableService;
import com.sarat.dsavisualizer.service.HuffmanService;
import com.sarat.dsavisualizer.service.SccService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 19 service tests. Probing tests check the slot tables,
 * probe counts and messages verbatim for the verified demo keys
 * ([23, 45, 12, 34, 56], table size 11) under all three rules.
 * SCC tests check the finish order, the pass-2 discovery order
 * and the final message for the verified demo graph, plus a
 * single-cycle and a DAG case. Huffman tests check the merge
 * messages, codes and encoded strings verbatim for "banana",
 * "mississippi", the single-character edge case and a two-letter
 * word. The 400 tests call the controller endpoints directly —
 * the controller is built with nulls for the other services
 * because the validators 400 before any service is touched
 * (the Phase 16 precedent).
 */
class Phase19ServiceTest {

    private final HashTableService hashTableService =
            new HashTableService();
    private final SccService sccService = new SccService();
    private final HuffmanService huffmanService = new HuffmanService();

    /** The demo keys (Phase 19 design §A1). */
    private static final List<Integer> DEMO_KEYS =
            List.of(23, 45, 12, 34, 56);

    /** The demo edges (Phase 19 design §A2). */
    private static final List<List<Integer>> DEMO_EDGES = List.of(
            List.of(0, 1), List.of(1, 2), List.of(2, 0),
            List.of(1, 3), List.of(3, 4), List.of(4, 5),
            List.of(5, 3), List.of(6, 5), List.of(6, 7),
            List.of(7, 6));

    /** Final step of a trace. */
    private Step lastStep(List<Step> steps) {
        return steps.get(steps.size() - 1);
    }

    /** Whether an edge from {@code from} to {@code to} exists. */
    private boolean hasEdge(List<EdgeDto> edges, int from, int to) {
        for (EdgeDto edge : edges) {
            if (edge.getFrom() == from && edge.getTo() == to) {
                return true;
            }
        }
        return false;
    }

    /** The node with the given id, or null. */
    private NodeDto nodeById(List<NodeDto> nodes, int id) {
        for (NodeDto node : nodes) {
            if (node.getId() == id) {
                return node;
            }
        }
        return null;
    }

    /** Whether every node sits at a distinct (x, y). */
    private boolean positionsDistinct(List<NodeDto> nodes) {
        Set<String> seen = new HashSet<>();
        for (NodeDto node : nodes) {
            if (!seen.add(node.getX() + "," + node.getY())) {
                return false;
            }
        }
        return true;
    }

    /** A request with values and a word (the probing mode) set. */
    private TraceRequest probingRequest(List<Integer> values,
                                        String mode) {
        TraceRequest request = new TraceRequest();
        request.setValues(values);
        request.setWord(mode);
        return request;
    }

    /** A request with only graphEdges set. */
    private TraceRequest sccRequest(List<List<Integer>> edges) {
        TraceRequest request = new TraceRequest();
        request.setGraphEdges(edges);
        return request;
    }

    /** A request with only a word set. */
    private TraceRequest wordRequest(String word) {
        TraceRequest request = new TraceRequest();
        request.setWord(word);
        return request;
    }

    /** The controller under test: nulls for every service except
     * the Phase 19 ones (validation 400s before any service is
     * called). The parameter order is the constructor's: 7 nulls,
     * hashTable, 25 nulls, scc, huffman (35 total). */
    private VisualizationController controller() {
        return new VisualizationController(
                null, null, null, null, null, null, null,
                hashTableService,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null,
                sccService, huffmanService);
    }

    /** Assert the response is a 400. */
    private void assertBadRequest(ResponseEntity<?> response) {
        assertEquals(400, response.getStatusCode().value());
    }

    // ---- Open addressing tests ----

    @Test
    void probingLinearDemo() {
        List<Step> steps = hashTableService.probing(DEMO_KEYS, "linear");
        // 15 probes + final = 16 steps.
        assertEquals(16, steps.size());
        assertEquals("Insert 23: h(23) = 23 % 11 = 1. Slot 1 is "
                        + "empty — placed.",
                steps.get(0).getMessage());
        assertEquals(List.of(1), steps.get(0).getHighlight());
        assertEquals(2, steps.get(0).getLine());
        assertEquals("Insert 45: slot 1 holds 23 — collision, "
                        + "probe 1 → slot 2.",
                steps.get(1).getMessage());
        assertEquals("Insert 45: h(45) = 45 % 11 = 1. Slot 2 is "
                        + "empty — placed.",
                steps.get(2).getMessage());
        assertEquals("Insert 12: slot 2 holds 45 — collision, "
                        + "probe 2 → slot 3.",
                steps.get(4).getMessage());
        assertEquals("Insert 34: h(34) = 34 % 11 = 1. Slot 4 is "
                        + "empty — placed.",
                steps.get(9).getMessage());
        assertEquals("Insert 56: h(56) = 56 % 11 = 1. Slot 5 is "
                        + "empty — placed.",
                steps.get(14).getMessage());
        assertEquals("Open addressing (linear): 5 keys placed in "
                        + "15 probes.",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        assertEquals(
                Arrays.asList(null, 23, 45, 12, 34, 56,
                        null, null, null, null, null),
                lastStep(steps).getData());
    }

    @Test
    void probingQuadraticDemo() {
        List<Step> steps = hashTableService.probing(DEMO_KEYS,
                "quadratic");
        // 15 probes + final = 16 steps.
        assertEquals(16, steps.size());
        assertEquals("Insert 34: slot 5 holds 12 — collision, "
                        + "probe 3 → slot 10.",
                steps.get(8).getMessage());
        assertEquals("Insert 34: h(34) = 34 % 11 = 1. Slot 10 is "
                        + "empty — placed.",
                steps.get(9).getMessage());
        assertEquals("Insert 56: h(56) = 56 % 11 = 1. Slot 6 is "
                        + "empty — placed.",
                steps.get(14).getMessage());
        assertEquals("Open addressing (quadratic): 5 keys placed in "
                        + "15 probes.",
                lastStep(steps).getMessage());
        assertEquals(
                Arrays.asList(null, 23, 45, null, null, 12, 56,
                        null, null, null, 34),
                lastStep(steps).getData());
    }

    @Test
    void probingDoubleDemo() {
        List<Step> steps = hashTableService.probing(DEMO_KEYS, "double");
        // 9 probes + final = 10 steps.
        assertEquals(10, steps.size());
        assertEquals("Insert 45: slot 1 holds 23 — collision, "
                        + "probe 1 → slot 5.",
                steps.get(1).getMessage());
        assertEquals("Insert 34: slot 1 holds 23 — collision, "
                        + "probe 1 → slot 2.",
                steps.get(5).getMessage());
        assertEquals("Insert 34: h(34) = 34 % 11 = 1. Slot 2 is "
                        + "empty — placed.",
                steps.get(6).getMessage());
        assertEquals("Open addressing (double): 5 keys placed in "
                        + "9 probes.",
                lastStep(steps).getMessage());
        assertEquals(
                Arrays.asList(null, 23, 34, 12, null, 45,
                        null, null, 56, null, null),
                lastStep(steps).getData());
    }

    @Test
    void probingDuplicateSkipped() {
        List<Step> steps = hashTableService.probing(
                List.of(23, 45, 23), "linear");
        // The duplicate 23 draws no probe and emits no step:
        // 1 + 2 probes + final = 4 steps.
        assertEquals(4, steps.size());
        assertEquals("Open addressing (linear): 2 keys placed in "
                        + "3 probes.",
                lastStep(steps).getMessage());
        assertEquals(
                Arrays.asList(null, 23, 45, null, null, null,
                        null, null, null, null, null),
                lastStep(steps).getData());
    }

    // ---- Kosaraju SCC tests ----

    @Test
    void sccDemo() {
        List<Step> steps = sccService.scc(DEMO_EDGES);
        // 8 finish + 3 SCC + final = 12 steps.
        assertEquals(12, steps.size());
        assertEquals("Finish 2 (order 1).", steps.get(0).getMessage());
        assertEquals(List.of(2), steps.get(0).getHighlight());
        assertEquals(2, steps.get(0).getLine());
        assertEquals("Finish 5 (order 2).", steps.get(1).getMessage());
        assertEquals("Finish 4 (order 3).", steps.get(2).getMessage());
        assertEquals("Finish 3 (order 4).", steps.get(3).getMessage());
        assertEquals("Finish 1 (order 5).", steps.get(4).getMessage());
        assertEquals("Finish 0 (order 6).", steps.get(5).getMessage());
        assertEquals("Finish 7 (order 7).", steps.get(6).getMessage());
        assertEquals("Finish 6 (order 8).", steps.get(7).getMessage());
        assertEquals(List.of(6), steps.get(7).getHighlight());
        assertEquals("SCC 1: [6, 7].", steps.get(8).getMessage());
        assertEquals(List.of(6, 7), steps.get(8).getHighlight());
        assertEquals(3, steps.get(8).getLine());
        assertEquals("SCC 2: [0, 1, 2].", steps.get(9).getMessage());
        assertEquals(List.of(0, 1, 2), steps.get(9).getHighlight());
        assertEquals("SCC 3: [3, 4, 5].", steps.get(10).getMessage());
        assertEquals(List.of(3, 4, 5), steps.get(10).getHighlight());
        assertEquals("3 strongly connected components: [6, 7], "
                        + "[0, 1, 2], [3, 4, 5].",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        // 8 nodes, 10 directed edges (a reversed pair is its own edge).
        assertEquals(8, lastStep(steps).getNodes().size());
        assertEquals(10, lastStep(steps).getEdges().size());
        assertTrue(hasEdge(lastStep(steps).getEdges(), 0, 1));
        assertTrue(!hasEdge(lastStep(steps).getEdges(), 1, 0));
        assertTrue(hasEdge(lastStep(steps).getEdges(), 6, 7));
        assertTrue(hasEdge(lastStep(steps).getEdges(), 7, 6));
        assertTrue(hasEdge(lastStep(steps).getEdges(), 6, 5));
        assertTrue(!hasEdge(lastStep(steps).getEdges(), 5, 6));
    }

    @Test
    void sccSingleCycle() {
        List<Step> steps = sccService.scc(List.of(
                List.of(0, 1), List.of(1, 2), List.of(2, 0)));
        // 3 finish + 1 SCC + final = 5 steps.
        assertEquals(5, steps.size());
        assertEquals("Finish 2 (order 1).", steps.get(0).getMessage());
        assertEquals("Finish 1 (order 2).", steps.get(1).getMessage());
        assertEquals("Finish 0 (order 3).", steps.get(2).getMessage());
        assertEquals("SCC 1: [0, 1, 2].", steps.get(3).getMessage());
        assertEquals("1 strongly connected components: [0, 1, 2].",
                lastStep(steps).getMessage());
    }

    @Test
    void sccDag() {
        List<Step> steps = sccService.scc(List.of(
                List.of(0, 1), List.of(1, 2)));
        // 3 finish + 3 singleton SCCs + final = 7 steps.
        assertEquals(7, steps.size());
        assertEquals("Finish 2 (order 1).", steps.get(0).getMessage());
        assertEquals("SCC 1: [0].", steps.get(3).getMessage());
        assertEquals("SCC 2: [1].", steps.get(4).getMessage());
        assertEquals("SCC 3: [2].", steps.get(5).getMessage());
        assertEquals("3 strongly connected components: [0], [1], [2].",
                lastStep(steps).getMessage());
    }

    // ---- Huffman tests ----

    @Test
    void huffmanBanana() {
        List<Step> steps = huffmanService.huffman("banana");
        // 2 merges + final = 3 steps.
        assertEquals(3, steps.size());
        assertEquals("Merge 'b' (1) + 'n' (2) → node (3).",
                steps.get(0).getMessage());
        assertEquals(List.of(3), steps.get(0).getHighlight());
        assertEquals(2, steps.get(0).getLine());
        assertEquals(4, steps.get(0).getNodes().size());
        assertEquals(2, steps.get(0).getEdges().size());
        assertEquals("Merge 'a' (3) + node (3) → node (6).",
                steps.get(1).getMessage());
        assertEquals(List.of(4), steps.get(1).getHighlight());
        assertEquals("Codes: a=0, b=10, n=11. \"banana\" → 100110110 "
                        + "(9 bits vs 48).",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        assertEquals(5, lastStep(steps).getNodes().size());
        assertEquals(4, lastStep(steps).getEdges().size());
        assertTrue(positionsDistinct(lastStep(steps).getNodes()));
        // Leaves: b=0 (freq 1), a=1 (freq 3), n=2 (freq 2).
        assertEquals(1, nodeById(lastStep(steps).getNodes(), 0).getValue());
        assertEquals("b",
                nodeById(lastStep(steps).getNodes(), 0).getLabel());
        assertEquals(3, nodeById(lastStep(steps).getNodes(), 1).getValue());
        assertEquals("a",
                nodeById(lastStep(steps).getNodes(), 1).getLabel());
        assertNull(nodeById(lastStep(steps).getNodes(), 3).getLabel());
        assertEquals(6, nodeById(lastStep(steps).getNodes(), 4).getValue());
        assertNull(nodeById(lastStep(steps).getNodes(), 4).getLabel());
        // The root sits centred above its children (a at x=60,
        // node 3 at x=195) and above the leaves.
        assertEquals(127.5,
                nodeById(lastStep(steps).getNodes(), 4).getX(), 0.001);
        assertEquals(180.0,
                nodeById(lastStep(steps).getNodes(), 4).getY(), 0.001);
        assertEquals(320.0,
                nodeById(lastStep(steps).getNodes(), 0).getY(), 0.001);
        assertTrue(hasEdge(lastStep(steps).getEdges(), 4, 3));
        assertTrue(hasEdge(lastStep(steps).getEdges(), 4, 1));
        assertTrue(hasEdge(lastStep(steps).getEdges(), 3, 0));
        assertTrue(hasEdge(lastStep(steps).getEdges(), 3, 2));
    }

    @Test
    void huffmanMississippi() {
        List<Step> steps = huffmanService.huffman("mississippi");
        // 3 merges + final = 4 steps.
        assertEquals(4, steps.size());
        assertEquals("Merge 'm' (1) + 'p' (2) → node (3).",
                steps.get(0).getMessage());
        assertEquals(List.of(4), steps.get(0).getHighlight());
        assertEquals("Merge node (3) + 'i' (4) → node (7).",
                steps.get(1).getMessage());
        assertEquals(List.of(5), steps.get(1).getHighlight());
        assertEquals("Merge 's' (4) + node (7) → node (11).",
                steps.get(2).getMessage());
        assertEquals(List.of(6), steps.get(2).getHighlight());
        assertEquals("Codes: s=0, m=100, p=101, i=11. "
                        + "\"mississippi\" → 100110011001110110111 "
                        + "(21 bits vs 88).",
                lastStep(steps).getMessage());
        assertEquals(7, lastStep(steps).getNodes().size());
        assertEquals(6, lastStep(steps).getEdges().size());
        assertTrue(positionsDistinct(lastStep(steps).getNodes()));
        assertEquals(11, nodeById(lastStep(steps).getNodes(), 6).getValue());
    }

    @Test
    void huffmanSingleChar() {
        List<Step> steps = huffmanService.huffman("aaa");
        // One leaf, no merges: final step only.
        assertEquals(1, steps.size());
        assertEquals("Codes: a=0. \"aaa\" → 000 (3 bits vs 24).",
                lastStep(steps).getMessage());
        assertEquals(1, lastStep(steps).getNodes().size());
        assertEquals(0, lastStep(steps).getEdges().size());
        assertEquals(3, nodeById(lastStep(steps).getNodes(), 0).getValue());
        assertEquals("a",
                nodeById(lastStep(steps).getNodes(), 0).getLabel());
    }

    @Test
    void huffmanTwoChars() {
        List<Step> steps = huffmanService.huffman("ab");
        // 1 merge + final = 2 steps.
        assertEquals(2, steps.size());
        assertEquals("Merge 'a' (1) + 'b' (1) → node (2).",
                steps.get(0).getMessage());
        assertEquals("Codes: a=0, b=1. \"ab\" → 01 (2 bits vs 16).",
                lastStep(steps).getMessage());
        assertEquals(3, lastStep(steps).getNodes().size());
        assertEquals(2, lastStep(steps).getEdges().size());
    }

    // ---- 400 tests (controller validation) ----

    @Test
    void probing400() {
        assertBadRequest(controller().hashProbing(
                probingRequest(List.of(23, 45), "cubic")));
        assertBadRequest(controller().hashProbing(
                probingRequest(List.of(), "linear")));
        assertBadRequest(controller().hashProbing(
                probingRequest(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9),
                        "linear")));
        assertBadRequest(controller().hashProbing(
                probingRequest(List.of(100), "linear")));
    }

    @Test
    void scc400() {
        assertBadRequest(controller().graphScc(sccRequest(List.of(
                List.of(0), List.of(0, 1)))));
        assertBadRequest(controller().graphScc(sccRequest(List.of(
                List.of(0, 10), List.of(1, 2)))));
        assertBadRequest(controller().graphScc(sccRequest(List.of(
                List.of(0, 1)))));
        assertBadRequest(controller().graphScc(sccRequest(List.of())));
    }

    @Test
    void huffman400() {
        assertBadRequest(controller().huffman(
                wordRequest("abcdefghijklm")));
        assertBadRequest(controller().huffman(
                wordRequest("Banana")));
    }
}
