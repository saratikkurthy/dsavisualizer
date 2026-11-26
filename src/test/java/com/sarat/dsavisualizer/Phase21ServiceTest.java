package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.controller.VisualizationController;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.service.AhoCorasickService;
import com.sarat.dsavisualizer.service.BridgesService;
import com.sarat.dsavisualizer.service.ManacherService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Phase 21 service tests. Aho-Corasick tests check the classic
 * demo (text "ushers", patterns he/she/his/hers: 10 nodes, the
 * pinned failure links and BFS order, scan states, 3 matches,
 * 25 steps) plus a single-pattern and a no-match case.
 * Bridges tests check the demo (two triangles joined by 1–3,
 * leaf 3–6: disc/low, bridges (1,3),(3,6), articulation 1,3,
 * 16 steps) plus a triangle (no bridges), the default start,
 * and a disconnected graph. Manacher tests check the demo
 * ("forgeeksskeegfor": d1 all 1, the d2 array, longest
 * "geeksskeeg" 3–12, 33 steps) plus "aba" and "a". The 400
 * tests call the controller endpoints directly — the controller
 * is built with nulls for the other services because the
 * validators 400 before any service is touched (the Phase 16
 * precedent).
 */
class Phase21ServiceTest {

    private final AhoCorasickService ahoCorasickService =
            new AhoCorasickService();
    private final BridgesService bridgesService = new BridgesService();
    private final ManacherService manacherService = new ManacherService();

    /** The demo patterns (Phase 21 design §A1, the classic example). */
    private static final List<String> DEMO_PATTERNS = List.of(
            "he", "she", "his", "hers");

    /** The demo graph (Phase 21 design §A2). */
    private static final List<List<Integer>> DEMO_EDGES = List.of(
            List.of(0, 1), List.of(1, 2), List.of(2, 0),
            List.of(1, 3), List.of(3, 4), List.of(4, 5),
            List.of(5, 3), List.of(3, 6));

    /** The demo string (Phase 21 design §A3). */
    private static final String DEMO_WORD = "forgeeksskeegfor";

    /** Final step of a trace. */
    private Step lastStep(List<Step> steps) {
        return steps.get(steps.size() - 1);
    }

    /** A request with word + words set. */
    private TraceRequest ahoRequest(String text, List<String> patterns) {
        TraceRequest request = new TraceRequest();
        request.setWord(text);
        request.setWords(patterns);
        return request;
    }

    /** A request with graphEdges + start set. */
    private TraceRequest bridgesRequest(List<List<Integer>> edges,
                                        Integer start) {
        TraceRequest request = new TraceRequest();
        request.setGraphEdges(edges);
        request.setStart(start);
        return request;
    }

    /** A request with only a word set. */
    private TraceRequest manacherRequest(String word) {
        TraceRequest request = new TraceRequest();
        request.setWord(word);
        return request;
    }

    /** The controller under test: nulls for every service except
     * the Phase 21 ones (validation 400s before any service is
     * called). The parameter order is the constructor's: 38
     * Phase 1–20 params, then aho, bridges, manacher (41 total). */
   
    /** Assert the response is a 400. */
    private void assertBadRequest(ResponseEntity<?> response) {
        assertEquals(400, response.getStatusCode().value());
    }

    // ---- Aho-Corasick tests ----

    @Test
    void ahoCorasickDemo() {
        List<Step> steps = ahoCorasickService.ahocorasick(
                "ushers", DEMO_PATTERNS);
        // 9 node-create + 9 failure-link + 6 scan + final = 25 steps.
        assertEquals(25, steps.size());
        // Node-create steps.
        assertEquals("Insert 'h' of pattern 'he' \u2192 node 1.",
                steps.get(0).getMessage());
        assertEquals(1, steps.get(0).getLine());
        assertEquals(List.of(1), steps.get(0).getHighlight());
        assertEquals(List.of(), steps.get(0).getSorted());
        assertEquals("Insert 'e' of pattern 'he' \u2192 node 2.",
                steps.get(1).getMessage());
        assertEquals(List.of(2), steps.get(1).getSorted());
        assertEquals("Insert 's' of pattern 'his' \u2192 node 7.",
                steps.get(6).getMessage());
        assertEquals(List.of(2, 5, 7), steps.get(6).getSorted());
        assertEquals("Insert 's' of pattern 'hers' \u2192 node 9.",
                steps.get(8).getMessage());
        assertEquals(List.of(2, 5, 7, 9), steps.get(8).getSorted());
        // Failure-link steps, BFS order [1, 3, 2, 6, 4, 8, 7, 5, 9].
        assertEquals("Failure link: node 1 ('h') \u2192 node 0.",
                steps.get(9).getMessage());
        assertEquals(2, steps.get(9).getLine());
        assertEquals(List.of(1), steps.get(9).getHighlight());
        assertEquals(10, steps.get(9).getEdges().size());
        assertEquals("Failure link: node 3 ('s') \u2192 node 0.",
                steps.get(10).getMessage());
        assertEquals("Failure link: node 2 ('e') \u2192 node 0.",
                steps.get(11).getMessage());
        assertEquals("Failure link: node 6 ('i') \u2192 node 0.",
                steps.get(12).getMessage());
        assertEquals("Failure link: node 4 ('h') \u2192 node 1.",
                steps.get(13).getMessage());
        assertEquals("Failure link: node 8 ('r') \u2192 node 0.",
                steps.get(14).getMessage());
        assertEquals("Failure link: node 7 ('s') \u2192 node 3.",
                steps.get(15).getMessage());
        assertEquals("Failure link: node 5 ('e') \u2192 node 2.",
                steps.get(16).getMessage());
        assertEquals("Failure link: node 9 ('s') \u2192 node 3.",
                steps.get(17).getMessage());
        assertEquals(18, steps.get(17).getEdges().size());
        // Scan steps: states [0, 3, 4, 5, 8, 9], lines 6, 6, 6, 7, 5, 7.
        assertEquals(List.of(117, 115, 104, 101, 114, 115),
                steps.get(18).getData());
        assertEquals("Read 'u' at 0 \u2192 state 0.",
                steps.get(18).getMessage());
        assertEquals(6, steps.get(18).getLine());
        assertEquals(List.of(0), steps.get(18).getAux());
        assertEquals(List.of(), steps.get(18).getSorted());
        assertEquals("Read 's' at 1 \u2192 state 3.",
                steps.get(19).getMessage());
        assertEquals(List.of(3), steps.get(19).getAux());
        assertEquals("Read 'h' at 2 \u2192 state 4.",
                steps.get(20).getMessage());
        assertEquals(List.of(4), steps.get(20).getAux());
        assertEquals("Read 'e' at 3 \u2192 state 5; match 'she' (1\u20133), "
                        + "'he' (2\u20133).",
                steps.get(21).getMessage());
        assertEquals(7, steps.get(21).getLine());
        assertEquals(List.of(5), steps.get(21).getAux());
        assertEquals(List.of(0, 1), steps.get(21).getSorted());
        assertEquals("Read 'r' at 4 \u2192 state 8 (via failure link).",
                steps.get(22).getMessage());
        assertEquals(5, steps.get(22).getLine());
        assertEquals(List.of(8), steps.get(22).getAux());
        assertEquals("Read 's' at 5 \u2192 state 9; match 'hers' (2\u20135).",
                steps.get(23).getMessage());
        assertEquals(7, steps.get(23).getLine());
        assertEquals(List.of(9), steps.get(23).getAux());
        assertEquals(List.of(3), steps.get(23).getSorted());
        // Final step.
        assertEquals("3 matches: 'she' (1\u20133), 'he' (2\u20133), "
                        + "'hers' (2\u20135).",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        assertEquals(List.of(0, 1, 3), lastStep(steps).getSorted());
        assertEquals(List.of(9), lastStep(steps).getAux());
        assertEquals(List.of(117, 115, 104, 101, 114, 115),
                lastStep(steps).getData());
        assertEquals(10, lastStep(steps).getNodes().size());
        assertEquals(18, lastStep(steps).getEdges().size());
        int failureEdges = 0;
        int trieEdges = 0;
        for (var edge : lastStep(steps).getEdges()) {
            if (edge.getWeight() == 2) {
                failureEdges++;
            } else if (edge.getWeight() == 1) {
                trieEdges++;
            }
        }
        assertEquals(9, failureEdges);
        assertEquals(9, trieEdges);
        // Root node position (TrieService layout: depth 0 → (40, 40)).
        assertEquals(40.0, lastStep(steps).getNodes().get(0).getX());
        assertEquals(40.0, lastStep(steps).getNodes().get(0).getY());
        assertEquals("", lastStep(steps).getNodes().get(0).getLabel());
        assertEquals("h", lastStep(steps).getNodes().get(1).getLabel());
    }

    @Test
    void ahoCorasickSinglePattern() {
        List<Step> steps = ahoCorasickService.ahocorasick(
                "ababa", List.of("aba"));
        // 3 node-create + 3 failure-link + 5 scan + final = 12 steps.
        assertEquals(12, steps.size());
        assertEquals("Failure link: node 3 ('a') \u2192 node 1.",
                steps.get(5).getMessage());
        assertEquals("Read 'a' at 2 \u2192 state 3; match 'aba' (0\u20132).",
                steps.get(8).getMessage());
        assertEquals(7, steps.get(8).getLine());
        assertEquals(List.of(0), steps.get(8).getSorted());
        assertEquals("Read 'b' at 3 \u2192 state 2 (via failure link).",
                steps.get(9).getMessage());
        assertEquals(5, steps.get(9).getLine());
        assertEquals("Read 'a' at 4 \u2192 state 3; match 'aba' (2\u20134).",
                steps.get(10).getMessage());
        assertEquals("2 matches: 'aba' (0\u20132), 'aba' (2\u20134).",
                lastStep(steps).getMessage());
        assertEquals(List.of(0), lastStep(steps).getSorted());
        assertEquals(4, lastStep(steps).getNodes().size());
    }

    @Test
    void ahoCorasickNoMatch() {
        List<Step> steps = ahoCorasickService.ahocorasick(
                "xyz", List.of("abc"));
        // 3 node-create + 3 failure-link + 3 scan + final = 10 steps.
        assertEquals(10, steps.size());
        assertEquals("No matches found.", lastStep(steps).getMessage());
        assertEquals(List.of(), lastStep(steps).getSorted());
        assertEquals(List.of(0), lastStep(steps).getAux());
    }

    // ---- Bridges tests ----

    @Test
    void bridgesDemo() {
        List<Step> steps = bridgesService.bridges(DEMO_EDGES, 0);
        // 7 visits + 2 back edges + 6 returns + final = 16 steps.
        assertEquals(16, steps.size());
        assertEquals("Visit node 0 (disc=0).", steps.get(0).getMessage());
        assertEquals(1, steps.get(0).getLine());
        assertEquals(List.of(0), steps.get(0).getHighlight());
        assertEquals(List.of(0, -1, -1, -1, -1, -1, -1),
                steps.get(0).getData());
        assertEquals(List.of(0, -1, -1, -1, -1, -1, -1),
                steps.get(0).getAux());
        assertEquals("Visit node 2 (disc=2).", steps.get(2).getMessage());
        assertEquals(List.of(0, 1, 2, -1, -1, -1, -1),
                steps.get(2).getData());
        assertEquals("Back edge 2\u20130: low[2] = min(2, 0) = 0.",
                steps.get(3).getMessage());
        assertEquals(8, steps.get(3).getLine());
        assertEquals(List.of(2, 0), steps.get(3).getHighlight());
        assertEquals(List.of(0, 1, 0, -1, -1, -1, -1),
                steps.get(3).getAux());
        assertEquals("Return from 2 to 1: low[1] = min(1, 0) = 0; "
                        + "edge 1\u20132 is not a bridge.",
                steps.get(4).getMessage());
        assertEquals(6, steps.get(4).getLine());
        assertEquals(List.of(2, 1), steps.get(4).getHighlight());
        assertEquals(List.of(0, 0, 0, -1, -1, -1, -1),
                steps.get(4).getAux());
        assertEquals(List.of(), steps.get(4).getMstEdges());
        assertEquals("Visit node 5 (disc=5).", steps.get(7).getMessage());
        assertEquals("Back edge 5\u20133: low[5] = min(5, 3) = 3.",
                steps.get(8).getMessage());
        assertEquals(List.of(0, 0, 0, 3, 4, 3, -1),
                steps.get(8).getAux());
        assertEquals("Return from 5 to 4: low[4] = min(4, 3) = 3; "
                        + "edge 4\u20135 is not a bridge.",
                steps.get(9).getMessage());
        assertEquals(List.of(0, 0, 0, 3, 3, 3, -1),
                steps.get(9).getAux());
        assertEquals("Return from 4 to 3: low[3] = min(3, 3) = 3; "
                        + "edge 3\u20134 is not a bridge; "
                        + "3 is an articulation point.",
                steps.get(10).getMessage());
        assertEquals("Visit node 6 (disc=6).", steps.get(11).getMessage());
        assertEquals("Return from 6 to 3: low[3] = min(3, 6) = 3; "
                        + "edge 3\u20136 is a bridge; "
                        + "3 is an articulation point.",
                steps.get(12).getMessage());
        assertEquals(List.of(List.of(3, 6)),
                steps.get(12).getMstEdges());
        assertEquals("Return from 3 to 1: low[1] = min(0, 3) = 0; "
                        + "edge 1\u20133 is a bridge; "
                        + "1 is an articulation point.",
                steps.get(13).getMessage());
        assertEquals(List.of(List.of(3, 6), List.of(1, 3)),
                steps.get(13).getMstEdges());
        assertEquals("Return from 1 to 0: low[0] = min(0, 0) = 0; "
                        + "edge 0\u20131 is not a bridge.",
                steps.get(14).getMessage());
        assertEquals("Bridges: (1, 3), (3, 6). Articulation points: 1, 3.",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6),
                lastStep(steps).getData());
        assertEquals(List.of(0, 0, 0, 3, 3, 3, 6),
                lastStep(steps).getAux());
        assertEquals(List.of(List.of(3, 6), List.of(1, 3)),
                lastStep(steps).getMstEdges());
        assertEquals(List.of(1, 3), lastStep(steps).getSorted());
        assertEquals(7, lastStep(steps).getNodes().size());
        assertEquals(8, lastStep(steps).getEdges().size());
    }

    @Test
    void bridgesTriangle() {
        List<Step> steps = bridgesService.bridges(List.of(
                List.of(0, 1), List.of(1, 2), List.of(2, 0)), 0);
        // 3 visits + 1 back edge + 2 returns + final = 7 steps.
        assertEquals(7, steps.size());
        assertEquals("Back edge 2\u20130: low[2] = min(2, 0) = 0.",
                steps.get(3).getMessage());
        assertEquals("No bridges. No articulation points.",
                lastStep(steps).getMessage());
        assertEquals(List.of(0, 1, 2), lastStep(steps).getData());
        assertEquals(List.of(0, 0, 0), lastStep(steps).getAux());
        assertEquals(List.of(), lastStep(steps).getMstEdges());
        assertEquals(List.of(), lastStep(steps).getSorted());
    }

    @Test
    void bridgesDefaultStart() {
        // A missing start defaults to the smallest node (0 here):
        // the trace is identical to the start-0 run.
        List<Step> withStart = bridgesService.bridges(DEMO_EDGES, 0);
        List<Step> defaulted = bridgesService.bridges(DEMO_EDGES, null);
        assertEquals(withStart.size(), defaulted.size());
        assertEquals(lastStep(withStart).getData(),
                lastStep(defaulted).getData());
        assertEquals(lastStep(withStart).getAux(),
                lastStep(defaulted).getAux());
        assertEquals(lastStep(withStart).getMstEdges(),
                lastStep(defaulted).getMstEdges());
        assertEquals(lastStep(withStart).getSorted(),
                lastStep(defaulted).getSorted());
        assertEquals(lastStep(withStart).getMessage(),
                lastStep(defaulted).getMessage());
    }

    @Test
    void bridgesDisconnected() {
        List<Step> steps = bridgesService.bridges(List.of(
                List.of(0, 1), List.of(2, 3)), 0);
        // 4 visits + 0 back edges + 2 returns + final = 7 steps.
        assertEquals(7, steps.size());
        assertEquals("Visit node 2 (disc=2).", steps.get(3).getMessage());
        assertEquals("Bridges: (0, 1), (2, 3). No articulation points.",
                lastStep(steps).getMessage());
        assertEquals(List.of(List.of(0, 1), List.of(2, 3)),
                lastStep(steps).getMstEdges());
        assertEquals(List.of(), lastStep(steps).getSorted());
    }

    // ---- Manacher tests ----

    @Test
    void manacherDemo() {
        List<Step> steps = manacherService.manacher(DEMO_WORD);
        // 16 d1 + 16 d2 + final = 33 steps.
        assertEquals(33, steps.size());
        assertEquals("Centre 0 ('f'): d1[0]=1, palindrome 'f' (0\u20130).",
                steps.get(0).getMessage());
        assertEquals(3, steps.get(0).getLine());
        assertEquals(List.of(0), steps.get(0).getHighlight());
        assertEquals(List.of(1, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0), steps.get(0).getData());
        assertEquals(List.of(0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0), steps.get(0).getAux());
        assertEquals(List.of(0, 0), steps.get(0).getSorted());
        assertEquals("Centre 2 ('r'): d1[2]=1, palindrome 'r' (2\u20132).",
                steps.get(2).getMessage());
        assertEquals(List.of(1, 1, 1, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0), steps.get(2).getData());
        assertEquals(List.of(1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1), lastStep(steps).getData());
        assertEquals("Centre 0 (before 'f'): d2[0]=0, no even palindrome.",
                steps.get(16).getMessage());
        assertEquals(5, steps.get(16).getLine());
        assertEquals(List.of(0), steps.get(16).getHighlight());
        assertEquals("Centre 5 (between 'e' and 'e'): d2[5]=1, "
                        + "palindrome 'ee' (4\u20135) \u2014 new longest.",
                steps.get(21).getMessage());
        assertEquals(List.of(4, 5), steps.get(21).getSorted());
        assertEquals("Centre 8 (between 's' and 's'): d2[8]=5, "
                        + "palindrome 'geeksskeeg' (3\u201312) "
                        + "\u2014 new longest.",
                steps.get(24).getMessage());
        assertEquals(List.of(0, 0, 0, 0, 0, 1, 0, 0, 5, 0,
                0, 0, 0, 0, 0, 0), steps.get(24).getAux());
        assertEquals(List.of(3, 12), steps.get(24).getSorted());
        assertEquals("Centre 9 (between 's' and 'k'): d2[9]=0, "
                        + "no even palindrome (mirror start 0).",
                steps.get(25).getMessage());
        assertEquals("Centre 11 (between 'e' and 'e'): d2[11]=1, "
                        + "palindrome 'ee' (10\u201311) (mirror start 1).",
                steps.get(27).getMessage());
        assertEquals(List.of(0, 0, 0, 0, 0, 1, 0, 0, 5, 0,
                0, 1, 0, 0, 0, 0), lastStep(steps).getAux());
        assertEquals("Longest palindromic substring: 'geeksskeeg' "
                        + "(3\u201312), length 10.",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        assertEquals(List.of(3, 12), lastStep(steps).getSorted());
    }

    @Test
    void manacherOddPalindrome() {
        List<Step> steps = manacherService.manacher("aba");
        // 3 d1 + 3 d2 + final = 7 steps.
        assertEquals(7, steps.size());
        assertEquals("Centre 1 ('b'): d1[1]=2, palindrome 'aba' (0\u20132) "
                        + "\u2014 new longest.",
                steps.get(1).getMessage());
        assertEquals("Centre 2 ('a'): d1[2]=1, palindrome 'a' (2\u20132) "
                        + "(mirror start 1).",
                steps.get(2).getMessage());
        assertEquals(List.of(1, 2, 1), steps.get(2).getData());
        assertEquals("Centre 0 (before 'a'): d2[0]=0, no even palindrome.",
                steps.get(3).getMessage());
        assertEquals(List.of(0, 0, 0), lastStep(steps).getAux());
        assertEquals("Longest palindromic substring: 'aba' (0\u20132), "
                        + "length 3.",
                lastStep(steps).getMessage());
        assertEquals(List.of(0, 2), lastStep(steps).getSorted());
    }

    @Test
    void manacherSingleChar() {
        List<Step> steps = manacherService.manacher("a");
        // 1 d1 + 1 d2 + final = 3 steps.
        assertEquals(3, steps.size());
        assertEquals(List.of(1), lastStep(steps).getData());
        assertEquals(List.of(0), lastStep(steps).getAux());
        assertEquals("Longest palindromic substring: 'a' (0\u20130), "
                        + "length 1.",
                lastStep(steps).getMessage());
        assertEquals(List.of(0, 0), lastStep(steps).getSorted());
    }

    // ---- 400 tests (controller validation) ----


}
