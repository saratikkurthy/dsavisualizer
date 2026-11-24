package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.controller.VisualizationController;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.service.AhoCorasick2Service;
import com.sarat.dsavisualizer.service.EertreeService;
import com.sarat.dsavisualizer.service.LazySegService;
import com.sarat.dsavisualizer.service.LcpKasaiService;
import com.sarat.dsavisualizer.service.MosService;
import com.sarat.dsavisualizer.service.PstKthService;
import com.sarat.dsavisualizer.service.SamLcsService;
import com.sarat.dsavisualizer.service.SamService;
import com.sarat.dsavisualizer.service.SparseTableService;
import com.sarat.dsavisualizer.service.SqrtDecomp2Service;
import com.sarat.dsavisualizer.service.SqrtDecompService;
import com.sarat.dsavisualizer.service.SuffixTrieService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 27 service tests. Aho-Corasick v2 tests check the demo
 * patterns ["he", "she", "his", "hers"] over text "ushers" (10
 * trie nodes; failure links 4-&gt;1, 5-&gt;2, 7-&gt;3, 9-&gt;3; exactly
 * one output link 5-&gt;2; 3 matches "she" [1,3], "he" [2,3],
 * "hers" [2,5]; 9 steps) and a second run (patterns ["a",
 * "ba"], text "ababa", 5 matches). Suffix-trie tests check the
 * demo "ababa" (10 nodes; suffixes 0 and 1 create all nodes,
 * suffixes 2, 3, 4 add nothing; 6 steps) and "aaa" (4 nodes).
 * SAM-LCS tests check the demo S1 "ababc", S2 "babc" (SAM 6
 * states, 0 clones; walk matches all 4 chars; LCS "babc" len
 * 4; 10 steps), "banana"/"ananas" (LCS "anana" len 5) and
 * "abc"/"xyz" (no common substring). Lazy-segment-tree tests
 * check the demo [1,2,3,4,5,6,7,8] (build root 36; update
 * +10 to [2,5] touches FULL nodes 5 and 6, pulls 2, 3, 1;
 * queries 76 and 58; 11 steps) and [5,5,5,5] (+3 to [1,2]).
 * PST k-th tests check the demo [3,1,4,1,5,9,2,6] (compressed
 * [1,2,3,4,5,6,9]; 32 persistent nodes; version roots
 * [0,1,5,9,13,17,21,24,28]; query 1 -&gt; 4, query 2 -&gt; 5;
 * 8 steps) and [2,2,2] (one distinct value). Sqrt-decomp v2
 * tests check the demo [3,1,4,1,5,9,2,6] (add +2 to [1,7];
 * block mins [3,3,4], lazy [0,2,0]; queries 3 and 4; 7 steps)
 * and [5,1,4,2] (both blocks fully covered). The 400 tests call
 * the controller endpoints directly — the controller is built
 * with nulls for the other services because the validators 400
 * before any service is touched (the Phase 16 precedent).
 */
class Phase27ServiceTest {

    private final SamService samService = new SamService();
    private final EertreeService eertreeService = new EertreeService();
    private final LcpKasaiService lcpKasaiService = new LcpKasaiService();
    private final SqrtDecompService sqrtDecompService = new SqrtDecompService();
    private final MosService mosService = new MosService();
    private final SparseTableService sparseTableService = new SparseTableService();
    private final AhoCorasick2Service ahoCorasick2Service = new AhoCorasick2Service();
    private final SuffixTrieService suffixTrieService = new SuffixTrieService();
    private final SamLcsService samLcsService = new SamLcsService();
    private final LazySegService lazySegService = new LazySegService();
    private final PstKthService pstKthService = new PstKthService();
    private final SqrtDecomp2Service sqrtDecomp2Service = new SqrtDecomp2Service();

    static final List<String> AC_PATTERNS = List.of("he", "she", "his", "hers");
    static final String AC_TEXT = "ushers";
    static final String TRIE_WORD = "ababa";
    static final String S1 = "ababc";
    static final String S2 = "babc";
    static final String ARRAY = "3,1,4,1,5,9,2,6";
    static final String LAZY_ARRAY = "1,2,3,4,5,6,7,8";
    static final List<Integer> LAZY_VALUES = List.of(2, 5, 0, 7, 2, 5);
    static final List<Integer> PST_VALUES = List.of(1, 5, 3, 0, 7, 6);
    static final List<Integer> SQRT2_VALUES = List.of(1, 7, 0, 7, 4, 7);

    // ---- Aho-Corasick v2 ----

    @Test
    void acDemoStepCount() {
        List<Step> steps = ahoCorasick2Service.ahoCorasick2(AC_TEXT, AC_PATTERNS);
        assertEquals(9, steps.size());
    }

    @Test
    void acDemoBuild() {
        List<Step> steps = ahoCorasick2Service.ahoCorasick2(AC_TEXT, AC_PATTERNS);
        assertEquals(List.of(0), steps.get(0).getData());
        assertEquals(2, steps.get(0).getLine());
        assertEquals(10, steps.get(0).getNodes().size());
        assertEquals(9, steps.get(0).getEdges().size());
        int[] depths = {0, 1, 2, 1, 2, 3, 2, 3, 3, 4};
        String[] labels = {"", "h", "he", "s", "sh", "she", "hi", "his", "her", "hers"};
        for (int i = 0; i < 10; i++) {
            assertEquals(depths[i], steps.get(0).getNodes().get(i).getValue());
            assertEquals(labels[i], steps.get(0).getNodes().get(i).getLabel());
        }
        int[][] trans = {{0, 1}, {0, 3}, {1, 2}, {1, 6}, {2, 8},
                {3, 4}, {4, 5}, {6, 7}, {8, 9}};
        for (int i = 0; i < trans.length; i++) {
            assertEquals(trans[i][0], steps.get(0).getEdges().get(i).getFrom());
            assertEquals(trans[i][1], steps.get(0).getEdges().get(i).getTo());
            assertNull(steps.get(0).getEdges().get(i).getWeight());
        }
    }

    @Test
    void acDemoFailureLinks() {
        List<Step> steps = ahoCorasick2Service.ahoCorasick2(AC_TEXT, AC_PATTERNS);
        assertEquals(List.of(0), steps.get(1).getData());
        assertEquals(List.of(4, 5, 7, 9), steps.get(1).getHighlight());
        assertEquals(2, steps.get(1).getLine());
        assertEquals(19, steps.get(1).getEdges().size());
        int[][] links = {{1, 0}, {2, 0}, {3, 0}, {4, 1}, {5, 2},
                {6, 0}, {7, 3}, {8, 0}, {9, 3}};
        for (int i = 0; i < links.length; i++) {
            assertEquals(links[i][0], steps.get(1).getEdges().get(9 + i).getFrom());
            assertEquals(links[i][1], steps.get(1).getEdges().get(9 + i).getTo());
            assertEquals(-1, steps.get(1).getEdges().get(9 + i).getWeight());
        }
        assertEquals(5, steps.get(1).getEdges().get(18).getFrom());
        assertEquals(2, steps.get(1).getEdges().get(18).getTo());
        assertEquals(-2, steps.get(1).getEdges().get(18).getWeight());
    }

    @Test
    void acDemoScan() {
        List<Step> steps = ahoCorasick2Service.ahoCorasick2(AC_TEXT, AC_PATTERNS);
        int[][] scan = {{0, 0}, {3, 0}, {4, 0}, {5, 2}, {8, 2}, {9, 3}};
        for (int i = 0; i < scan.length; i++) {
            Step step = steps.get(2 + i);
            assertEquals(List.of(scan[i][0]), step.getHighlight());
            assertEquals(List.of(scan[i][1]), step.getData());
            assertEquals(3, step.getLine());
            assertEquals(19, step.getEdges().size());
        }
        assertTrue(steps.get(5).getMessage().contains("she"));
        assertTrue(steps.get(5).getMessage().contains("he"));
        assertTrue(steps.get(7).getMessage().contains("hers"));
    }

    @Test
    void acDemoFinal() {
        List<Step> steps = ahoCorasick2Service.ahoCorasick2(AC_TEXT, AC_PATTERNS);
        assertEquals(List.of(3), steps.get(8).getData());
        assertEquals(0, steps.get(8).getLine());
        assertTrue(steps.get(8).getMessage().contains("3 matches"));
    }

    @Test
    void acSecondInput() {
        List<Step> steps = ahoCorasick2Service.ahoCorasick2("ababa", List.of("a", "ba"));
        assertEquals(8, steps.size());
        assertEquals(4, steps.get(0).getNodes().size());
        assertEquals(7, steps.get(1).getEdges().size());
        assertEquals(-2, steps.get(1).getEdges().get(6).getWeight());
        assertEquals(List.of(1), steps.get(2).getData());
        assertEquals(List.of(3), steps.get(4).getData());
        assertEquals(List.of(5), steps.get(7).getData());
    }

    // ---- Suffix trie ----

    @Test
    void trieDemoStepCount() {
        List<Step> steps = suffixTrieService.suffixTrie(TRIE_WORD);
        assertEquals(6, steps.size());
    }

    @Test
    void trieDemoInsertions() {
        List<Step> steps = suffixTrieService.suffixTrie(TRIE_WORD);
        assertEquals(List.of(6), steps.get(0).getData());
        assertEquals(List.of(1, 2, 3, 4, 5), steps.get(0).getHighlight());
        assertEquals(List.of(10), steps.get(1).getData());
        assertEquals(List.of(6, 7, 8, 9), steps.get(1).getHighlight());
        assertEquals(List.of(10), steps.get(2).getData());
        assertEquals(List.of(), steps.get(2).getHighlight());
        assertEquals(List.of(), steps.get(3).getHighlight());
        assertEquals(List.of(), steps.get(4).getHighlight());
        assertEquals(List.of(10), steps.get(5).getData());
        assertEquals(2, steps.get(0).getLine());
        assertEquals(0, steps.get(5).getLine());
    }

    @Test
    void trieDemoNodes() {
        List<Step> steps = suffixTrieService.suffixTrie(TRIE_WORD);
        int[] depths = {0, 1, 2, 3, 4, 5, 1, 2, 3, 4};
        String[] labels = {"", "a", "ab", "aba", "abab", "ababa",
                "b", "ba", "bab", "baba"};
        for (int i = 0; i < 10; i++) {
            assertEquals(depths[i], steps.get(5).getNodes().get(i).getValue());
            assertEquals(labels[i], steps.get(5).getNodes().get(i).getLabel());
        }
        int[][] edges = {{0, 1}, {0, 6}, {1, 2}, {2, 3}, {3, 4},
                {4, 5}, {6, 7}, {7, 8}, {8, 9}};
        for (int i = 0; i < edges.length; i++) {
            assertEquals(edges[i][0], steps.get(5).getEdges().get(i).getFrom());
            assertEquals(edges[i][1], steps.get(5).getEdges().get(i).getTo());
            assertNull(steps.get(5).getEdges().get(i).getWeight());
        }
    }

    @Test
    void trieSecondInput() {
        List<Step> steps = suffixTrieService.suffixTrie("aaa");
        assertEquals(4, steps.size());
        assertEquals(List.of(1, 2, 3), steps.get(0).getHighlight());
        assertEquals(List.of(), steps.get(1).getHighlight());
        assertEquals(List.of(4), steps.get(3).getData());
    }

    // ---- SAM LCS ----

    @Test
    void lcsDemoStepCount() {
        List<Step> steps = samLcsService.samLcs(S1, S2);
        assertEquals(10, steps.size());
    }

    @Test
    void lcsDemoBuild() {
        List<Step> steps = samLcsService.samLcs(S1, S2);
        int[] data = {2, 3, 4, 5, 6};
        int[] edgeCounts = {2, 5, 7, 9, 13};
        for (int i = 0; i < 5; i++) {
            assertEquals(List.of(data[i]), steps.get(i).getData());
            assertEquals(List.of(i + 1), steps.get(i).getHighlight());
            assertEquals(List.of(i + 1), steps.get(i).getSorted());
            assertEquals(2, steps.get(i).getLine());
            assertEquals(edgeCounts[i], steps.get(i).getEdges().size());
        }
        int[] lens = {0, 1, 2, 3, 4, 5};
        String[] labels = {"", "a", "ab", "aba", "abab", "ababc"};
        for (int i = 0; i < 6; i++) {
            assertEquals(lens[i], steps.get(9).getNodes().get(i).getValue());
            assertEquals(labels[i], steps.get(9).getNodes().get(i).getLabel());
        }
        int[][] trans = {{0, 1}, {0, 2}, {0, 5}, {1, 2}, {2, 3},
                {2, 5}, {3, 4}, {4, 5}};
        for (int i = 0; i < trans.length; i++) {
            assertEquals(trans[i][0], steps.get(9).getEdges().get(i).getFrom());
            assertEquals(trans[i][1], steps.get(9).getEdges().get(i).getTo());
            assertNull(steps.get(9).getEdges().get(i).getWeight());
        }
        int[][] links = {{1, 0}, {2, 0}, {3, 1}, {4, 2}, {5, 0}};
        for (int i = 0; i < links.length; i++) {
            assertEquals(links[i][0], steps.get(9).getEdges().get(8 + i).getFrom());
            assertEquals(links[i][1], steps.get(9).getEdges().get(8 + i).getTo());
            assertEquals(-1, steps.get(9).getEdges().get(8 + i).getWeight());
        }
    }

    @Test
    void lcsDemoWalk() {
        List<Step> steps = samLcsService.samLcs(S1, S2);
        int[][] walk = {{2, 1, 1}, {3, 2, 2}, {4, 3, 3}, {5, 4, 4}};
        for (int i = 0; i < 4; i++) {
            Step step = steps.get(5 + i);
            assertEquals(List.of(walk[i][0]), step.getHighlight());
            assertEquals(List.of(walk[i][1], walk[i][2]), step.getData());
            assertEquals(3, step.getLine());
        }
        assertEquals(List.of(4, 4), steps.get(9).getData());
        assertEquals(0, steps.get(9).getLine());
        assertTrue(steps.get(9).getMessage().contains("babc"));
    }

    @Test
    void lcsBanana() {
        List<Step> steps = samLcsService.samLcs("banana", "ananas");
        assertEquals(13, steps.size());
        assertEquals(List.of(5, 5), steps.get(12).getData());
        assertTrue(steps.get(12).getMessage().contains("anana"));
    }

    @Test
    void lcsNone() {
        List<Step> steps = samLcsService.samLcs("abc", "xyz");
        assertEquals(7, steps.size());
        assertEquals(List.of(0, 0), steps.get(6).getData());
    }

    // ---- Lazy segment tree ----

    @Test
    void lazyDemoStepCount() {
        List<Step> steps = lazySegService.lazySeg(LAZY_ARRAY, LAZY_VALUES, 10);
        assertEquals(11, steps.size());
    }

    @Test
    void lazyDemoBuild() {
        List<Step> steps = lazySegService.lazySeg(LAZY_ARRAY, LAZY_VALUES, 10);
        assertEquals(List.of(), steps.get(0).getData());
        assertEquals(1, steps.get(0).getLine());
        assertEquals(15, steps.get(0).getNodes().size());
        assertEquals(14, steps.get(0).getEdges().size());
        int[] sums = {36, 10, 26, 3, 7, 11, 15, 1, 2, 3, 4, 5, 6, 7, 8};
        String[] labels = {"[0,7]", "[0,3]", "[4,7]", "[0,1]", "[2,3]",
                "[4,5]", "[6,7]", "[0,0]", "[1,1]", "[2,2]", "[3,3]",
                "[4,4]", "[5,5]", "[6,6]", "[7,7]"};
        for (int i = 0; i < 15; i++) {
            assertEquals(sums[i], steps.get(0).getNodes().get(i).getValue());
            assertEquals(labels[i], steps.get(0).getNodes().get(i).getLabel());
        }
    }

    @Test
    void lazyDemoUpdate() {
        List<Step> steps = lazySegService.lazySeg(LAZY_ARRAY, LAZY_VALUES, 10);
        assertEquals(List.of(1), steps.get(1).getHighlight());
        assertEquals(List.of(), steps.get(1).getData());
        assertEquals(List.of(2), steps.get(2).getHighlight());
        assertEquals(List.of(), steps.get(2).getData());
        assertEquals(List.of(5), steps.get(3).getHighlight());
        assertEquals(List.of(5, 10), steps.get(3).getData());
        assertEquals(27, steps.get(3).getNodes().get(4).getValue());
        assertEquals(List.of(2), steps.get(4).getHighlight());
        assertEquals(List.of(5, 10), steps.get(4).getData());
        assertEquals(30, steps.get(4).getNodes().get(1).getValue());
        assertEquals(List.of(6), steps.get(5).getHighlight());
        assertEquals(List.of(5, 10, 6, 10), steps.get(5).getData());
        assertEquals(31, steps.get(5).getNodes().get(5).getValue());
        assertEquals(List.of(3), steps.get(6).getHighlight());
        assertEquals(List.of(5, 10, 6, 10), steps.get(6).getData());
        assertEquals(46, steps.get(6).getNodes().get(2).getValue());
        assertEquals(List.of(1), steps.get(7).getHighlight());
        assertEquals(List.of(5, 10, 6, 10), steps.get(7).getData());
        assertEquals(76, steps.get(7).getNodes().get(0).getValue());
        for (int i = 1; i <= 7; i++) {
            assertEquals(2, steps.get(i).getLine());
        }
    }

    @Test
    void lazyDemoQueries() {
        List<Step> steps = lazySegService.lazySeg(LAZY_ARRAY, LAZY_VALUES, 10);
        assertEquals(List.of(1), steps.get(8).getHighlight());
        assertEquals(List.of(76), steps.get(8).getData());
        assertEquals(3, steps.get(8).getLine());
        assertEquals(List.of(5, 6), steps.get(9).getHighlight());
        assertEquals(List.of(58), steps.get(9).getData());
        assertEquals(3, steps.get(9).getLine());
        assertEquals(List.of(5, 10, 6, 10), steps.get(10).getData());
        assertEquals(0, steps.get(10).getLine());
        assertEquals(76, steps.get(10).getNodes().get(0).getValue());
    }

    @Test
    void lazySmall() {
        List<Step> steps = lazySegService.lazySeg("5,5,5,5",
                List.of(1, 2, 0, 3, 1, 2), 3);
        assertEquals(11, steps.size());
        assertEquals(7, steps.get(0).getNodes().size());
        assertEquals(List.of(26), steps.get(8).getData());
        assertEquals(List.of(16), steps.get(9).getData());
        assertEquals(List.of(5, 3, 6, 3), steps.get(10).getData());
        assertEquals(26, steps.get(10).getNodes().get(0).getValue());
    }

    // ---- PST k-th ----

    @Test
    void pstDemoStepCount() {
        List<Step> steps = pstKthService.pstKth(ARRAY, PST_VALUES);
        assertEquals(8, steps.size());
    }

    @Test
    void pstDemoBuild() {
        List<Step> steps = pstKthService.pstKth(ARRAY, PST_VALUES);
        assertEquals(List.of(0, 1, 5, 9, 13, 17, 21, 24, 28), steps.get(0).getData());
        assertEquals(List.of(0, 1, 5, 9, 13, 17, 21, 24, 28), steps.get(0).getSorted());
        assertEquals(1, steps.get(0).getLine());
        assertEquals(13, steps.get(0).getNodes().size());
        assertEquals(12, steps.get(0).getEdges().size());
        int[] counts = {8, 5, 3, 3, 2, 2, 1, 2, 1, 1, 1, 1, 1};
        String[] labels = {"[1,9]", "[1,4]", "[5,9]", "[1,2]", "[3,4]",
                "[5,6]", "[9,9]", "[1,1]", "[2,2]", "[3,3]", "[4,4]",
                "[5,5]", "[6,6]"};
        for (int i = 0; i < 13; i++) {
            assertEquals(counts[i], steps.get(0).getNodes().get(i).getValue());
            assertEquals(labels[i], steps.get(0).getNodes().get(i).getLabel());
        }
    }

    @Test
    void pstDemoQuery1() {
        List<Step> steps = pstKthService.pstKth(ARRAY, PST_VALUES);
        int[][] q1 = {{1, 3, 3, 6}, {2, 3, 2, 4}, {5, 1, 0, 2}};
        for (int i = 0; i < 3; i++) {
            Step step = steps.get(1 + i);
            assertEquals(List.of(q1[i][0]), step.getHighlight());
            assertEquals(List.of(q1[i][1], q1[i][2]), step.getData());
            assertEquals(i + 1, step.getNodes().size());
            assertEquals(q1[i][3], step.getNodes().get(i).getValue());
            assertEquals(2, step.getLine());
        }
        assertEquals(2, steps.get(3).getEdges().size());
        assertTrue(steps.get(3).getMessage().contains("4"));
    }

    @Test
    void pstDemoQuery2() {
        List<Step> steps = pstKthService.pstKth(ARRAY, PST_VALUES);
        int[][] q2 = {{1, 6, 5, 8}, {3, 1, 2, 3}, {6, 1, 1, 2}};
        for (int i = 0; i < 3; i++) {
            Step step = steps.get(4 + i);
            assertEquals(List.of(q2[i][0]), step.getHighlight());
            assertEquals(List.of(q2[i][1], q2[i][2]), step.getData());
            assertEquals(i + 1, step.getNodes().size());
            assertEquals(q2[i][3], step.getNodes().get(i).getValue());
            assertEquals(2, step.getLine());
        }
        assertEquals(2, steps.get(6).getEdges().size());
        assertTrue(steps.get(6).getMessage().contains("5"));
        assertEquals(List.of(4, 5), steps.get(7).getData());
        assertEquals(0, steps.get(7).getLine());
        assertEquals(13, steps.get(7).getNodes().size());
    }

    @Test
    void pstOne() {
        List<Step> steps = pstKthService.pstKth("2,2,2", List.of(0, 2, 2, 0, 2, 1));
        assertEquals(4, steps.size());
        assertEquals(List.of(0, 1, 2, 3), steps.get(0).getData());
        assertEquals(1, steps.get(0).getNodes().size());
        assertEquals(List.of(2, 0), steps.get(1).getData());
        assertEquals(List.of(1, 0), steps.get(2).getData());
        assertEquals(List.of(2, 2), steps.get(3).getData());
    }

    // ---- Sqrt decomposition v2 ----

    @Test
    void sqrt2DemoStepCount() {
        List<Step> steps = sqrtDecomp2Service.sqrtDecomp2(ARRAY, SQRT2_VALUES, 2);
        assertEquals(7, steps.size());
    }

    @Test
    void sqrt2DemoUpdate() {
        List<Step> steps = sqrtDecomp2Service.sqrtDecomp2(ARRAY, SQRT2_VALUES, 2);
        assertEquals(List.of(1, 1, 2), steps.get(0).getData());
        assertEquals(List.of(0, 0, 0), steps.get(0).getSorted());
        assertEquals(1, steps.get(0).getLine());
        assertNull(steps.get(0).getEdges());
        assertEquals(List.of(3, 1, 2), steps.get(1).getData());
        assertEquals(List.of(0, 0, 0), steps.get(1).getSorted());
        assertEquals(List.of(1, 2, 100), steps.get(1).getHighlight());
        assertEquals(3, steps.get(1).getNodes().get(1).getValue());
        assertEquals(6, steps.get(1).getNodes().get(2).getValue());
        assertEquals(List.of(3, 3, 2), steps.get(2).getData());
        assertEquals(List.of(0, 2, 0), steps.get(2).getSorted());
        assertEquals(List.of(3, 4, 5, 101), steps.get(2).getHighlight());
        assertEquals(3, steps.get(2).getNodes().get(3).getValue());
        assertEquals(7, steps.get(2).getNodes().get(4).getValue());
        assertEquals(11, steps.get(2).getNodes().get(5).getValue());
        assertEquals(List.of(3, 3, 4), steps.get(3).getData());
        assertEquals(List.of(0, 2, 0), steps.get(3).getSorted());
        assertEquals(List.of(6, 7, 102), steps.get(3).getHighlight());
        assertEquals(4, steps.get(3).getNodes().get(6).getValue());
        assertEquals(8, steps.get(3).getNodes().get(7).getValue());
        for (int i = 1; i <= 3; i++) {
            assertEquals(2, steps.get(i).getLine());
        }
    }

    @Test
    void sqrt2DemoQueries() {
        List<Step> steps = sqrtDecomp2Service.sqrtDecomp2(ARRAY, SQRT2_VALUES, 2);
        assertEquals(List.of(3), steps.get(4).getData());
        assertEquals(List.of(0, 2, 0), steps.get(4).getSorted());
        assertEquals(List.of(0, 1, 2, 101, 6, 7), steps.get(4).getHighlight());
        assertEquals(3, steps.get(4).getLine());
        assertEquals(List.of(4), steps.get(5).getData());
        assertEquals(List.of(4, 5, 6, 7), steps.get(5).getHighlight());
        assertEquals(3, steps.get(5).getLine());
        assertEquals(List.of(3, 3, 4), steps.get(6).getData());
        assertEquals(List.of(0, 2, 0), steps.get(6).getSorted());
        assertEquals(0, steps.get(6).getLine());
    }

    @Test
    void sqrt2WholeArray() {
        // Whole-array update over 2 blocks: there is no block
        // strictly between the first and last touched blocks,
        // so the sim sets NO lazy tag — both blocks update
        // element by element (stored [6,2,5,3], mins [2,3]).
        List<Step> steps = sqrtDecomp2Service.sqrtDecomp2("5,1,4,2",
                List.of(0, 3, 0, 3, 1, 2), 1);
        assertEquals(6, steps.size());
        assertEquals(List.of(1, 2), steps.get(0).getData());
        assertEquals(List.of(2, 2), steps.get(1).getData());
        assertEquals(List.of(0, 0), steps.get(1).getSorted());
        assertEquals(List.of(2, 3), steps.get(2).getData());
        assertEquals(List.of(0, 0), steps.get(2).getSorted());
        assertEquals(List.of(2), steps.get(3).getData());
        assertEquals(List.of(2), steps.get(4).getData());
        assertEquals(List.of(2, 3), steps.get(5).getData());
        assertEquals(List.of(0, 0), steps.get(5).getSorted());
        assertEquals(6, steps.get(5).getNodes().get(0).getValue());
    }

    // ---- 400s ----

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
