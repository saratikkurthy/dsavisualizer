package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.controller.VisualizationController;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.service.EertreeService;
import com.sarat.dsavisualizer.service.LcpKasaiService;
import com.sarat.dsavisualizer.service.MosService;
import com.sarat.dsavisualizer.service.SamService;
import com.sarat.dsavisualizer.service.SparseTableService;
import com.sarat.dsavisualizer.service.SqrtDecompService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 26 service tests. SAM tests check the demo "baaba"
 * (7 states, exactly 1 clone — state 4 of q=2 at the 3rd
 * character, distinct substrings 11, 6 steps) and a
 * no-clone run ("aaa") plus a single character. Eertree
 * tests check the demo "ababa" (7 nodes, 5 distinct
 * palindromes, longest "ababa", 6 steps) and a reuse run
 * ("abcab": the 'a' and 'b' at positions 3-4 reuse nodes
 * 2 and 3). LCP/Kasai tests check the demo "banana" (SA
 * [5,3,1,0,4,2], LCP [0,1,3,0,0,2], longest repeated
 * "ana", 8 steps). Sqrt-decomposition tests check the demo
 * [3,1,4,1,5,9,2,6] (blocks [8,15,8], query [2,6]=21,
 * update a[3]=7, query again 27, 9 steps). Mo's tests check
 * the demo [1,2,1,3,2,1,4,2] with queries [(4,7),(0,3),
 * (1,6),(2,5)] (sorted order [1,3,2,0], answers [3,3,4,3],
 * 14 moves, 16 steps). Sparse-table tests check the demo
 * [3,1,4,1,5,9,2,6] (st levels [3,1,4,1,5,9,2,6] /
 * [1,1,1,1,5,2,2] / [1,1,1,1,2] / [1], queries [2,6]=1
 * and [0,7]=1, 9 steps). The 400 tests call the controller
 * endpoints directly — the controller is built with nulls
 * for the other services because the validators 400 before
 * any service is touched (the Phase 16 precedent).
 */
class Phase26ServiceTest {

    private final SamService samService = new SamService();
    private final EertreeService eertreeService = new EertreeService();
    private final LcpKasaiService lcpKasaiService = new LcpKasaiService();
    private final SqrtDecompService sqrtDecompService = new SqrtDecompService();
    private final MosService mosService = new MosService();
    private final SparseTableService sparseTableService = new SparseTableService();

    static final String SAM_WORD = "baaba";
    static final String EERTREE_WORD = "ababa";
    static final String LCP_WORD = "banana";
    static final String ARRAY = "3,1,4,1,5,9,2,6";
    static final List<Integer> SQRT_VALUES = List.of(2, 6);
    static final String MOS_ARRAY = "1,2,1,3,2,1,4,2";
    static final List<Integer> MOS_VALUES = List.of(4, 7, 0, 3, 1, 6, 2, 5);
    static final List<Integer> SPARSE_VALUES = List.of(2, 6, 0, 7);

    // ---- SAM ----

    @Test
    void samDemoStepCount() {
        List<Step> steps = samService.sam(SAM_WORD);
        assertEquals(6, steps.size());
    }

    @Test
    void samDemoExtensions() {
        List<Step> steps = samService.sam(SAM_WORD);
        // One extension per character; state counts 2,3,5,6,7.
        assertEquals(List.of(2), steps.get(0).getData());
        assertEquals(List.of(1), steps.get(0).getHighlight());
        assertEquals(List.of(1), steps.get(0).getSorted());
        assertEquals(List.of(3), steps.get(1).getData());
        assertEquals(List.of(2), steps.get(1).getHighlight());
        assertEquals(List.of(2), steps.get(1).getSorted());
        // The 3rd character clones state 2 as state 4.
        assertEquals(List.of(5), steps.get(2).getData());
        assertEquals(List.of(3, 4), steps.get(2).getHighlight());
        assertEquals(List.of(3), steps.get(2).getSorted());
        assertEquals(List.of(6), steps.get(3).getData());
        assertEquals(List.of(5), steps.get(3).getHighlight());
        assertEquals(List.of(7), steps.get(4).getData());
        assertEquals(List.of(6), steps.get(4).getHighlight());
        assertEquals(List.of(6), steps.get(4).getSorted());
        assertEquals(2, steps.get(0).getLine());
        assertEquals(2, steps.get(4).getLine());
    }

    @Test
    void samDemoFinal() {
        List<Step> steps = samService.sam(SAM_WORD);
        assertEquals(List.of(11), steps.get(5).getData());
        assertEquals(List.of(6), steps.get(5).getSorted());
        assertEquals(0, steps.get(5).getLine());
    }

    @Test
    void samDemoNodes() {
        List<Step> steps = samService.sam(SAM_WORD);
        // len values in id order.
        assertEquals(0, steps.get(5).getNodes().get(0).getValue());
        assertEquals(1, steps.get(5).getNodes().get(1).getValue());
        assertEquals(2, steps.get(5).getNodes().get(2).getValue());
        assertEquals(3, steps.get(5).getNodes().get(3).getValue());
        assertEquals(1, steps.get(5).getNodes().get(4).getValue());
        assertEquals(4, steps.get(5).getNodes().get(5).getValue());
        assertEquals(5, steps.get(5).getNodes().get(6).getValue());
        // Longest strings in id order.
        assertEquals("", steps.get(5).getNodes().get(0).getLabel());
        assertEquals("b", steps.get(5).getNodes().get(1).getLabel());
        assertEquals("ba", steps.get(5).getNodes().get(2).getLabel());
        assertEquals("baa", steps.get(5).getNodes().get(3).getLabel());
        assertEquals("a", steps.get(5).getNodes().get(4).getLabel());
        assertEquals("baab", steps.get(5).getNodes().get(5).getLabel());
        assertEquals("baaba", steps.get(5).getNodes().get(6).getLabel());
        assertEquals(7, steps.get(5).getNodes().size());
    }

    @Test
    void samDemoEdges() {
        List<Step> steps = samService.sam(SAM_WORD);
        // Transitions first (state id order), then suffix links.
        assertEquals(2, steps.get(0).getEdges().size());
        assertEquals(5, steps.get(1).getEdges().size());
        assertEquals(9, steps.get(2).getEdges().size());
        assertEquals(12, steps.get(3).getEdges().size());
        assertEquals(14, steps.get(4).getEdges().size());
        assertEquals(14, steps.get(5).getEdges().size());
        // The final transitions, in emission order.
        assertEquals(0, steps.get(5).getEdges().get(0).getFrom());
        assertEquals(1, steps.get(5).getEdges().get(0).getTo());
        assertNull(steps.get(5).getEdges().get(0).getWeight());
        assertEquals(0, steps.get(5).getEdges().get(1).getFrom());
        assertEquals(4, steps.get(5).getEdges().get(1).getTo());
        assertEquals(1, steps.get(5).getEdges().get(2).getFrom());
        assertEquals(2, steps.get(5).getEdges().get(2).getTo());
        assertEquals(2, steps.get(5).getEdges().get(3).getFrom());
        assertEquals(3, steps.get(5).getEdges().get(3).getTo());
        assertEquals(3, steps.get(5).getEdges().get(4).getFrom());
        assertEquals(5, steps.get(5).getEdges().get(4).getTo());
        assertEquals(4, steps.get(5).getEdges().get(5).getFrom());
        assertEquals(3, steps.get(5).getEdges().get(5).getTo());
        assertEquals(4, steps.get(5).getEdges().get(6).getFrom());
        assertEquals(5, steps.get(5).getEdges().get(6).getTo());
        assertEquals(5, steps.get(5).getEdges().get(7).getFrom());
        assertEquals(6, steps.get(5).getEdges().get(7).getTo());
        // The suffix links, weight -1, state id order.
        assertEquals(1, steps.get(5).getEdges().get(8).getFrom());
        assertEquals(0, steps.get(5).getEdges().get(8).getTo());
        assertEquals(-1, steps.get(5).getEdges().get(8).getWeight());
        assertEquals(2, steps.get(5).getEdges().get(9).getFrom());
        assertEquals(4, steps.get(5).getEdges().get(9).getTo());
        assertEquals(3, steps.get(5).getEdges().get(10).getFrom());
        assertEquals(4, steps.get(5).getEdges().get(10).getTo());
        assertEquals(4, steps.get(5).getEdges().get(11).getFrom());
        assertEquals(0, steps.get(5).getEdges().get(11).getTo());
        assertEquals(5, steps.get(5).getEdges().get(12).getFrom());
        assertEquals(1, steps.get(5).getEdges().get(12).getTo());
        assertEquals(6, steps.get(5).getEdges().get(13).getFrom());
        assertEquals(2, steps.get(5).getEdges().get(13).getTo());
    }

    @Test
    void samNoClone() {
        List<Step> steps = samService.sam("aaa");
        assertEquals(4, steps.size());
        assertEquals(List.of(3), steps.get(3).getData());
        assertEquals(List.of(3), steps.get(3).getSorted());
        assertEquals(4, steps.get(3).getNodes().size());
        assertEquals(List.of(2), steps.get(1).getHighlight());
        assertEquals(List.of(3), steps.get(2).getHighlight());
    }

    @Test
    void samSingleChar() {
        List<Step> steps = samService.sam("b");
        assertEquals(2, steps.size());
        assertEquals(List.of(1), steps.get(1).getData());
        assertEquals(List.of(1), steps.get(1).getSorted());
    }

    // ---- Eertree ----

    @Test
    void eertreeDemoStepCount() {
        List<Step> steps = eertreeService.eertree(EERTREE_WORD);
        assertEquals(6, steps.size());
    }

    @Test
    void eertreeDemoSteps() {
        List<Step> steps = eertreeService.eertree(EERTREE_WORD);
        assertEquals(List.of(3), steps.get(0).getData());
        assertEquals(List.of(2), steps.get(0).getHighlight());
        assertEquals(List.of(2), steps.get(0).getSorted());
        assertEquals(List.of(4), steps.get(1).getData());
        assertEquals(List.of(3), steps.get(1).getHighlight());
        assertEquals(List.of(5), steps.get(2).getData());
        assertEquals(List.of(4), steps.get(2).getHighlight());
        assertEquals(List.of(6), steps.get(3).getData());
        assertEquals(List.of(5), steps.get(3).getHighlight());
        assertEquals(List.of(7), steps.get(4).getData());
        assertEquals(List.of(6), steps.get(4).getHighlight());
        assertEquals(List.of(6), steps.get(4).getSorted());
        assertEquals(2, steps.get(0).getLine());
        assertEquals(2, steps.get(4).getLine());
    }

    @Test
    void eertreeDemoFinal() {
        List<Step> steps = eertreeService.eertree(EERTREE_WORD);
        assertEquals(List.of(5), steps.get(5).getData());
        assertEquals(List.of(6), steps.get(5).getSorted());
        assertEquals(0, steps.get(5).getLine());
        assertTrue(steps.get(5).getMessage().contains("ababa"));
    }

    @Test
    void eertreeDemoNodes() {
        List<Step> steps = eertreeService.eertree(EERTREE_WORD);
        assertEquals(-1, steps.get(5).getNodes().get(0).getValue());
        assertEquals(0, steps.get(5).getNodes().get(1).getValue());
        assertEquals(1, steps.get(5).getNodes().get(2).getValue());
        assertEquals(1, steps.get(5).getNodes().get(3).getValue());
        assertEquals(3, steps.get(5).getNodes().get(4).getValue());
        assertEquals(3, steps.get(5).getNodes().get(5).getValue());
        assertEquals(5, steps.get(5).getNodes().get(6).getValue());
        assertEquals("\u22121", steps.get(5).getNodes().get(0).getLabel());
        assertEquals("\u03b5", steps.get(5).getNodes().get(1).getLabel());
        assertEquals("a", steps.get(5).getNodes().get(2).getLabel());
        assertEquals("b", steps.get(5).getNodes().get(3).getLabel());
        assertEquals("aba", steps.get(5).getNodes().get(4).getLabel());
        assertEquals("bab", steps.get(5).getNodes().get(5).getLabel());
        assertEquals("ababa", steps.get(5).getNodes().get(6).getLabel());
        assertEquals(7, steps.get(5).getNodes().size());
    }

    @Test
    void eertreeDemoEdges() {
        List<Step> steps = eertreeService.eertree(EERTREE_WORD);
        assertEquals(3, steps.get(0).getEdges().size());
        assertEquals(5, steps.get(1).getEdges().size());
        assertEquals(7, steps.get(2).getEdges().size());
        assertEquals(9, steps.get(3).getEdges().size());
        assertEquals(11, steps.get(4).getEdges().size());
        assertEquals(11, steps.get(5).getEdges().size());
        // Next-edges first: 0-a->2, 0-b->3, 2-b->5, 3-a->4, 5-a->6.
        assertEquals(0, steps.get(5).getEdges().get(0).getFrom());
        assertEquals(2, steps.get(5).getEdges().get(0).getTo());
        assertNull(steps.get(5).getEdges().get(0).getWeight());
        assertEquals(0, steps.get(5).getEdges().get(1).getFrom());
        assertEquals(3, steps.get(5).getEdges().get(1).getTo());
        assertEquals(2, steps.get(5).getEdges().get(2).getFrom());
        assertEquals(5, steps.get(5).getEdges().get(2).getTo());
        assertEquals(3, steps.get(5).getEdges().get(3).getFrom());
        assertEquals(4, steps.get(5).getEdges().get(3).getTo());
        assertEquals(5, steps.get(5).getEdges().get(4).getFrom());
        assertEquals(6, steps.get(5).getEdges().get(4).getTo());
        // Suffix links (weight -1), node id order; the -1 root's self-link is skipped.
        assertEquals(1, steps.get(5).getEdges().get(5).getFrom());
        assertEquals(0, steps.get(5).getEdges().get(5).getTo());
        assertEquals(-1, steps.get(5).getEdges().get(5).getWeight());
        assertEquals(2, steps.get(5).getEdges().get(6).getFrom());
        assertEquals(1, steps.get(5).getEdges().get(6).getTo());
        assertEquals(3, steps.get(5).getEdges().get(7).getFrom());
        assertEquals(1, steps.get(5).getEdges().get(7).getTo());
        assertEquals(4, steps.get(5).getEdges().get(8).getFrom());
        assertEquals(2, steps.get(5).getEdges().get(8).getTo());
        assertEquals(5, steps.get(5).getEdges().get(9).getFrom());
        assertEquals(3, steps.get(5).getEdges().get(9).getTo());
        assertEquals(6, steps.get(5).getEdges().get(10).getFrom());
        assertEquals(4, steps.get(5).getEdges().get(10).getTo());
    }

    @Test
    void eertreeReuse() {
        List<Step> steps = eertreeService.eertree("abcab");
        assertEquals(6, steps.size());
        // The 'a' and 'b' at positions 3-4 reuse nodes 2 and 3.
        assertEquals(List.of(5), steps.get(2).getData());
        assertEquals(List.of(5), steps.get(3).getData());
        assertEquals(List.of(2), steps.get(3).getHighlight());
        assertEquals(List.of(5), steps.get(4).getData());
        assertEquals(List.of(3), steps.get(4).getHighlight());
        assertEquals(List.of(3), steps.get(5).getData());
        assertEquals(List.of(3), steps.get(5).getSorted());
        assertEquals(5, steps.get(5).getNodes().size());
    }

    // ---- LCP / Kasai ----

    @Test
    void lcpDemoStepCount() {
        List<Step> steps = lcpKasaiService.lcpKasai(LCP_WORD);
        assertEquals(8, steps.size());
    }

    @Test
    void lcpDemoSaStep() {
        List<Step> steps = lcpKasaiService.lcpKasai(LCP_WORD);
        assertEquals(List.of(5, 3, 1, 0, 4, 2), steps.get(0).getData());
        assertEquals(List.of(5, 3, 1, 0, 4, 2), steps.get(0).getSorted());
        assertEquals(2, steps.get(0).getLine());
        assertNull(steps.get(0).getEdges());
        // Nodes in suffix-start order: id = start, value = rank.
        assertEquals(3, steps.get(0).getNodes().get(0).getValue());
        assertEquals("banana", steps.get(0).getNodes().get(0).getLabel());
        assertEquals(2, steps.get(0).getNodes().get(1).getValue());
        assertEquals("anana", steps.get(0).getNodes().get(1).getLabel());
        assertEquals(5, steps.get(0).getNodes().get(2).getValue());
        assertEquals("nana", steps.get(0).getNodes().get(2).getLabel());
        assertEquals(1, steps.get(0).getNodes().get(3).getValue());
        assertEquals("ana", steps.get(0).getNodes().get(3).getLabel());
        assertEquals(4, steps.get(0).getNodes().get(4).getValue());
        assertEquals("na", steps.get(0).getNodes().get(4).getLabel());
        assertEquals(0, steps.get(0).getNodes().get(5).getValue());
        assertEquals("a", steps.get(0).getNodes().get(5).getLabel());
    }

    @Test
    void lcpDemoKasaiSteps() {
        List<Step> steps = lcpKasaiService.lcpKasai(LCP_WORD);
        // data = [lcp[rank[i]], h before]; highlight = [i, sa[rank[i]-1]].
        assertEquals(List.of(0, 0), steps.get(1).getData());
        assertEquals(List.of(0, 1), steps.get(1).getHighlight());
        assertEquals(List.of(0), steps.get(1).getSorted());
        assertEquals(List.of(3, 0), steps.get(2).getData());
        assertEquals(List.of(1, 3), steps.get(2).getHighlight());
        assertEquals(List.of(3, 0), steps.get(2).getSorted());
        assertEquals(List.of(2, 2), steps.get(3).getData());
        assertEquals(List.of(2, 4), steps.get(3).getHighlight());
        assertEquals(List.of(3, 0, 2), steps.get(3).getSorted());
        assertEquals(List.of(1, 1), steps.get(4).getData());
        assertEquals(List.of(3, 5), steps.get(4).getHighlight());
        assertEquals(List.of(1, 3, 0, 2), steps.get(4).getSorted());
        assertEquals(List.of(0, 0), steps.get(5).getData());
        assertEquals(List.of(4, 0), steps.get(5).getHighlight());
        assertEquals(List.of(1, 3, 0, 0, 2), steps.get(5).getSorted());
        assertEquals(List.of(0, 0), steps.get(6).getData());
        assertEquals(List.of(), steps.get(6).getHighlight());
        assertEquals(List.of(0, 1, 3, 0, 0, 2), steps.get(6).getSorted());
        assertEquals(3, steps.get(1).getLine());
        assertEquals(3, steps.get(6).getLine());
    }

    @Test
    void lcpDemoFinal() {
        List<Step> steps = lcpKasaiService.lcpKasai(LCP_WORD);
        assertEquals(List.of(3), steps.get(7).getData());
        assertEquals(List.of(0, 1, 3, 0, 0, 2), steps.get(7).getSorted());
        assertEquals(0, steps.get(7).getLine());
        assertTrue(steps.get(7).getMessage().contains("ana"));
    }

    @Test
    void lcpSingleChar() {
        List<Step> steps = lcpKasaiService.lcpKasai("a");
        assertEquals(3, steps.size());
        assertEquals(List.of(0), steps.get(0).getData());
        assertEquals(List.of(0, 0), steps.get(1).getData());
        assertEquals(List.of(0), steps.get(2).getData());
        assertEquals(List.of(0), steps.get(2).getSorted());
    }

    // ---- Sqrt decomposition ----

    @Test
    void sqrtDemoStepCount() {
        List<Step> steps = sqrtDecompService.sqrtDecomp(
                ARRAY, SQRT_VALUES, 3, 7);
        assertEquals(9, steps.size());
    }

    @Test
    void sqrtDemoBuildAndQuery() {
        List<Step> steps = sqrtDecompService.sqrtDecomp(
                ARRAY, SQRT_VALUES, 3, 7);
        assertEquals(List.of(8, 15, 8), steps.get(0).getData());
        assertEquals(1, steps.get(0).getLine());
        // Query [2,6]: a[2]=4, block 1=15, a[6]=2.
        assertEquals(List.of(4), steps.get(1).getData());
        assertEquals(List.of(2), steps.get(1).getHighlight());
        assertEquals(List.of(4), steps.get(1).getSorted());
        assertEquals(List.of(19), steps.get(2).getData());
        assertEquals(List.of(101), steps.get(2).getHighlight());
        assertEquals(List.of(4, 15), steps.get(2).getSorted());
        assertEquals(List.of(21), steps.get(3).getData());
        assertEquals(List.of(6), steps.get(3).getHighlight());
        assertEquals(List.of(4, 15, 2), steps.get(3).getSorted());
        assertEquals(2, steps.get(1).getLine());
        assertEquals(2, steps.get(3).getLine());
        // Array nodes show the original array.
        assertEquals(3, steps.get(3).getNodes().get(0).getValue());
        assertEquals("3", steps.get(3).getNodes().get(0).getLabel());
        assertEquals(1, steps.get(3).getNodes().get(3).getValue());
        assertNull(steps.get(3).getEdges());
    }

    @Test
    void sqrtDemoUpdateAndRequery() {
        List<Step> steps = sqrtDecompService.sqrtDecomp(
                ARRAY, SQRT_VALUES, 3, 7);
        assertEquals(List.of(21), steps.get(4).getData());
        assertEquals(List.of(3), steps.get(4).getHighlight());
        assertEquals(3, steps.get(4).getLine());
        // Post-update query: a[2]=4, block 1=21, a[6]=2.
        assertEquals(List.of(4), steps.get(5).getData());
        assertEquals(List.of(25), steps.get(6).getData());
        assertEquals(List.of(101), steps.get(6).getHighlight());
        assertEquals(List.of(4, 21), steps.get(6).getSorted());
        assertEquals(List.of(27), steps.get(7).getData());
        assertEquals(List.of(6), steps.get(7).getHighlight());
        assertEquals(List.of(4, 21, 2), steps.get(7).getSorted());
        // The update shows in the array nodes.
        assertEquals(7, steps.get(7).getNodes().get(3).getValue());
        assertEquals(List.of(27), steps.get(8).getData());
        assertEquals(List.of(4, 21, 2), steps.get(8).getSorted());
        assertEquals(0, steps.get(8).getLine());
    }

    @Test
    void sqrtPartialBlockQuery() {
        List<Step> steps = sqrtDecompService.sqrtDecomp(
                "5,6,7,8", List.of(1, 2), 0, 10);
        // n=4, block size 2: query [1,2] touches no whole block.
        assertEquals(7, steps.size());
        assertEquals(List.of(11, 15), steps.get(0).getData());
        assertEquals(List.of(6), steps.get(1).getData());
        assertEquals(List.of(1), steps.get(1).getHighlight());
        assertEquals(List.of(13), steps.get(2).getData());
        assertEquals(List.of(2), steps.get(2).getHighlight());
        assertEquals(List.of(6, 7), steps.get(2).getSorted());
        assertEquals(List.of(16), steps.get(3).getData());
        assertEquals(List.of(0), steps.get(3).getHighlight());
        assertEquals(List.of(6), steps.get(4).getData());
        assertEquals(List.of(13), steps.get(5).getData());
        assertEquals(List.of(13), steps.get(6).getData());
        assertEquals(List.of(6, 7), steps.get(6).getSorted());
    }

    // ---- Mo's algorithm ----

    @Test
    void mosDemoStepCount() {
        List<Step> steps = mosService.mos(MOS_ARRAY, MOS_VALUES);
        assertEquals(16, steps.size());
    }

    @Test
    void mosDemoSortStep() {
        List<Step> steps = mosService.mos(MOS_ARRAY, MOS_VALUES);
        assertEquals(List.of(1, 3, 2, 0), steps.get(0).getData());
        assertEquals(1, steps.get(0).getLine());
    }

    @Test
    void mosDemoMoves() {
        List<Step> steps = mosService.mos(MOS_ARRAY, MOS_VALUES);
        // +0,+1,+2,+3 (to (0,3)); +4,+5,-0,-1 (to (2,5));
        // +1,+6 (to (1,6)); +7,-1,-2,-3 (to (4,7)).
        int[][] expected = {
                {0, 1, 0, 0}, {1, 2, 0, 1}, {2, 2, 0, 2},
                {3, 3, 0, 3}, {4, 3, 0, 4}, {5, 3, 0, 5},
                {0, 3, 1, 5}, {1, 3, 2, 5}, {1, 3, 1, 5},
                {6, 4, 1, 6}, {7, 4, 1, 7}, {1, 4, 2, 7},
                {2, 4, 3, 7}, {3, 3, 4, 7},
        };
        for (int i = 0; i < expected.length; i++) {
            Step step = steps.get(1 + i);
            assertEquals(List.of(expected[i][0]),
                    step.getHighlight(), "move " + i + " highlight");
            assertEquals(List.of(expected[i][1]),
                    step.getData(), "move " + i + " distinct");
            assertEquals(List.of(expected[i][2], expected[i][3]),
                    step.getSorted(), "move " + i + " window");
            assertEquals(2, step.getLine(), "move " + i + " line");
        }
    }

    @Test
    void mosDemoFinal() {
        List<Step> steps = mosService.mos(MOS_ARRAY, MOS_VALUES);
        assertEquals(List.of(14), steps.get(15).getData());
        assertEquals(List.of(3, 3, 4, 3), steps.get(15).getSorted());
        assertEquals(0, steps.get(15).getLine());
    }

    @Test
    void mosSingleQuery() {
        List<Step> steps = mosService.mos("1,1,2", List.of(0, 2));
        assertEquals(5, steps.size());
        assertEquals(List.of(0), steps.get(0).getData());
        assertEquals(List.of(2), steps.get(3).getData());
        assertEquals(List.of(0, 2), steps.get(3).getSorted());
        assertEquals(List.of(3), steps.get(4).getData());
        assertEquals(List.of(2), steps.get(4).getSorted());
    }

    // ---- Sparse table ----

    @Test
    void sparseDemoStepCount() {
        List<Step> steps = sparseTableService.sparseTable(
                ARRAY, SPARSE_VALUES);
        assertEquals(9, steps.size());
    }

    @Test
    void sparseDemoBuildLevels() {
        List<Step> steps = sparseTableService.sparseTable(
                ARRAY, SPARSE_VALUES);
        assertEquals(List.of(3, 1, 4, 1, 5, 9, 2, 6),
                steps.get(0).getData());
        assertEquals(List.of(3, 1, 4, 1, 5, 9, 2, 6),
                steps.get(0).getSorted());
        assertEquals(List.of(1, 1, 1, 1, 5, 2, 2),
                steps.get(1).getData());
        assertEquals(List.of(1, 1, 1, 1, 5, 2, 2),
                steps.get(1).getSorted());
        assertEquals(List.of(1, 1, 1, 1, 2), steps.get(2).getData());
        assertEquals(List.of(1), steps.get(3).getData());
        assertEquals(1, steps.get(0).getLine());
        assertEquals(1, steps.get(3).getLine());
    }

    @Test
    void sparseDemoQueries() {
        List<Step> steps = sparseTableService.sparseTable(
                ARRAY, SPARSE_VALUES);
        // Query [2,6]: intervals [2,5] and [3,6].
        assertEquals(List.of(2, 3, 4, 5), steps.get(4).getHighlight());
        assertEquals(List.of(1), steps.get(4).getData());
        assertEquals(List.of(3, 4, 5, 6), steps.get(5).getHighlight());
        assertEquals(List.of(1), steps.get(5).getData());
        // Query [0,7]: the two intervals coincide.
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7),
                steps.get(6).getHighlight());
        assertEquals(List.of(1), steps.get(6).getData());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7),
                steps.get(7).getHighlight());
        assertEquals(List.of(1), steps.get(7).getData());
        assertEquals(2, steps.get(4).getLine());
        assertEquals(2, steps.get(7).getLine());
    }

    @Test
    void sparseDemoFinal() {
        List<Step> steps = sparseTableService.sparseTable(
                ARRAY, SPARSE_VALUES);
        assertEquals(List.of(1, 1), steps.get(8).getData());
        assertEquals(List.of(), steps.get(8).getSorted());
        assertEquals(0, steps.get(8).getLine());
    }

    @Test
    void sparseSingleElement() {
        List<Step> steps = sparseTableService.sparseTable(
                "7", List.of(0, 0, 0, 0));
        assertEquals(6, steps.size());
        assertEquals(List.of(7), steps.get(0).getData());
        assertEquals(List.of(7), steps.get(1).getData());
        assertEquals(List.of(7, 7), steps.get(5).getData());
    }

    // ---- 400s (controller validators, real services) ----

    private TraceRequest wordRequest(String word) {
        TraceRequest request = new TraceRequest();
        request.setWord(word);
        return request;
    }

    private void assertStatus(int expected,
                              ResponseEntity<?> response) {
        assertEquals(expected, response.getStatusCode().value());
    }


}
