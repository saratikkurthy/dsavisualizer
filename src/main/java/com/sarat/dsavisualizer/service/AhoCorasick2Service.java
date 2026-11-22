package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Aho-Corasick v2 (Phase 27, string matching v3): the trie of
 * several patterns plus failure links (BFS) and output links,
 * scanned once over the text in O(text + matches). The idea
 * behind multi-pattern search — grep, intrusion detection,
 * bioinformatics.
 *
 * <p>Demo patterns ["he", "she", "his", "hers"], text
 * "ushers". Trie nodes (id: string, depth, fail): 0: "", 0,
 * 0; 1: "h", 1, 0; 2: "he", 2, 0 (pattern end); 3: "s", 1,
 * 0; 4: "sh", 2, 1; 5: "she", 3, 2 (pattern end, output
 * link 5 -&gt; 2); 6: "hi", 2, 0; 7: "his", 3, 3 (pattern
 * end); 8: "her", 3, 0; 9: "hers", 4, 3 (pattern end). Ten
 * nodes, nine transitions, nine failure links (every node
 * except the root has one; the root's self-link is no edge),
 * exactly ONE output link: 5 -&gt; 2 ("she" also reports
 * "he"). Scan of "ushers": u (state 0, no match), s
 * (state 3), h (state 4), e (state 5 — "she" [1,3], then
 * the output link reports "he" [2,3]), r (state 8, via
 * failure link 5 -&gt; 2), s (state 9 — "hers" [2,5]).
 * Three matches. Trace: 1 build step (line 1) + 1
 * failure-links step (line 2) + 6 scan steps (line 3, one
 * per character) + final (line 0) = 9 steps. Build step:
 * {@code nodes} = the 10 trie nodes, {@code edges} = the 9
 * transitions only, {@code data} = [0] (matches so far).
 * Failure-links step: {@code edges} = transitions +
 * failure links + the output link (19 edges),
 * {@code highlight} = [4, 5, 7, 9] (the nodes whose
 * failure link is not the root), {@code data} = [0].
 * Scan step: {@code highlight} = [current state id],
 * {@code data} = [matches found so far]. Final step
 * {@code data} = [3]. Node {@code value} = depth,
 * {@code label} = the string itself ("" for the root).
 * Transitions have weight null (the character is the last
 * char of the target node's label — the SAM
 * incoming-label theorem); failure links have weight
 * &minus;1 (dashed); the output link has weight &minus;2
 * (dotted). {@code edges} are ordered transitions first
 * (node id order, insertion order within a node — the
 * maps are LinkedHashMaps), then failure links (node id
 * order), then the output link.</p>
 *
 * <p>Bad input (no patterns, a pattern that is empty or
 * longer than 8 letters, a non-lowercase pattern, or a
 * missing/blank text) yields a single "empty" Step; the
 * controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class AhoCorasick2Service {

    private static final int LINE_BUILD = 2;
    private static final int LINE_FAILURE = 2;
    private static final int LINE_SCAN = 3;
    private static final int LINE_FINAL = 0;

    /** Per-call mutable state (the service bean is a singleton, so no instance state). */
    private static final class Trie {
        final List<Map<Character, Integer>> next = new ArrayList<>();
        final List<Integer> fail = new ArrayList<>();
        final List<List<String>> out = new ArrayList<>();
        final List<String> label = new ArrayList<>();
        final List<Integer> depth = new ArrayList<>();
        final List<Integer> outLink = new ArrayList<>();

        Trie() {
            next.add(new LinkedHashMap<>());
            fail.add(0);
            out.add(new ArrayList<>());
            label.add("");
            depth.add(0);
            outLink.add(-1);
        }

        /** Inserts one pattern; returns the node id where it ends. */
        int insert(String pattern) {
            int v = 0;
            for (int i = 0; i < pattern.length(); i++) {
                char c = pattern.charAt(i);
                Integer nxt = next.get(v).get(c);
                if (nxt == null) {
                    nxt = next.size();
                    next.get(v).put(c, nxt);
                    next.add(new LinkedHashMap<>());
                    fail.add(0);
                    out.add(new ArrayList<>());
                    label.add(label.get(v) + c);
                    depth.add(depth.get(v) + 1);
                    outLink.add(-1);
                }
                v = nxt;
            }
            out.get(v).add(pattern);
            return v;
        }

        /** BFS failure links, then one output link per node. */
        void buildFailureLinks() {
            Queue<Integer> queue = new ArrayDeque<>();
            for (int u : next.get(0).values()) {
                fail.set(u, 0);
                queue.add(u);
            }
            while (!queue.isEmpty()) {
                int v = queue.remove();
                for (Map.Entry<Character, Integer> e
                        : next.get(v).entrySet()) {
                    char c = e.getKey();
                    int u = e.getValue();
                    int f = fail.get(v);
                    while (f != 0
                            && !next.get(f).containsKey(c)) {
                        f = fail.get(f);
                    }
                    fail.set(u, next.get(f).getOrDefault(c, 0));
                    queue.add(u);
                }
            }
            for (int v = 1; v < next.size(); v++) {
                int f = fail.get(v);
                while (f != 0) {
                    if (!out.get(f).isEmpty()) {
                        outLink.set(v, f);
                        break;
                    }
                    f = fail.get(f);
                }
            }
        }

        /** One NodeDto per trie node, in id order. */
        List<NodeDto> snapshot() {
            List<NodeDto> dtos = new ArrayList<>();
            for (int i = 0; i < next.size(); i++) {
                NodeDto dto = new NodeDto(i, depth.get(i), 0, 0);
                dto.setLabel(label.get(i));
                dtos.add(dto);
            }
            return dtos;
        }

        /** Transitions only (the build step, before failure links exist). */
        List<EdgeDto> transitionEdges() {
            List<EdgeDto> edges = new ArrayList<>();
            for (int i = 0; i < next.size(); i++) {
                for (int to : next.get(i).values()) {
                    edges.add(new EdgeDto(i, to, null));
                }
            }
            return edges;
        }

        /** Transitions, then failure links, then output links (see class Javadoc). */
        List<EdgeDto> fullEdges() {
            List<EdgeDto> edges = transitionEdges();
            for (int i = 1; i < next.size(); i++) {
                edges.add(new EdgeDto(i, fail.get(i), -1));
            }
            for (int i = 1; i < next.size(); i++) {
                if (outLink.get(i) != -1) {
                    edges.add(new EdgeDto(i, outLink.get(i), -2));
                }
            }
            return edges;
        }

        /** Node ids whose failure link is not the root. */
        List<Integer> nonRootFailNodes() {
            List<Integer> ids = new ArrayList<>();
            for (int i = 1; i < next.size(); i++) {
                if (fail.get(i) != 0) {
                    ids.add(i);
                }
            }
            return ids;
        }
    }

    /** One match: the pattern and its [start, end] span in the text. */
    private static final class Match {
        final String pattern;
        final int start;
        final int end;

        Match(String pattern, int start, int end) {
            this.pattern = pattern;
            this.start = start;
            this.end = end;
        }
    }

    /**
     * Builds the automaton of {@code patterns} and scans
     * {@code text} once, reporting every pattern occurrence.
     */
    public List<Step> ahoCorasick2(String text, List<String> patterns) {
        List<Step> steps = new ArrayList<>();
        if (patterns == null || patterns.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "At least one pattern is needed.",
                    LINE_FINAL));
            return steps;
        }
        for (String p : patterns) {
            if (p == null || p.isEmpty() || p.length() > 8) {
                steps.add(new Step(List.of(), List.of(), List.of(),
                        List.of(), "Each pattern needs 1..8 letters.",
                        LINE_FINAL));
                return steps;
            }
            for (char c : p.toCharArray()) {
                if (c < 'a' || c > 'z') {
                    steps.add(new Step(List.of(), List.of(), List.of(),
                            List.of(), "Patterns need lowercase "
                                    + "a-z only.", LINE_FINAL));
                    return steps;
                }
            }
        }
        if (text == null || text.isBlank()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A text is needed.", LINE_FINAL));
            return steps;
        }
        Trie trie = new Trie();
        for (String p : patterns) {
            trie.insert(p);
        }
        steps.add(new Step(List.of(0), List.of(), List.of(),
                List.of(),
                "Built the trie of " + patterns.size()
                        + " patterns: " + trie.next.size() + " nodes.",
                LINE_BUILD, trie.snapshot(), trie.transitionEdges()));
        trie.buildFailureLinks();
        StringBuilder failMsg = new StringBuilder(
                "Failure links set: ");
        List<String> links = new ArrayList<>();
        for (int i = 1; i < trie.next.size(); i++) {
            if (trie.fail.get(i) != 0) {
                links.add(i + "->" + trie.fail.get(i));
            }
        }
        failMsg.append(String.join(", ", links));
        failMsg.append(" (all others ->0)");
        int outputLinks = 0;
        for (int i = 1; i < trie.next.size(); i++) {
            if (trie.outLink.get(i) != -1) {
                outputLinks++;
                failMsg.append("; output link ").append(i)
                        .append("->").append(trie.outLink.get(i))
                        .append(" ('").append(trie.label.get(i))
                        .append("' also reports '")
                        .append(trie.label.get(trie.outLink.get(i)))
                        .append("')");
            }
        }
        if (outputLinks == 0) {
            failMsg.append("; no output links");
        }
        failMsg.append(".");
        steps.add(new Step(List.of(0), trie.nonRootFailNodes(),
                List.of(), List.of(), failMsg.toString(),
                LINE_FAILURE, trie.snapshot(), trie.fullEdges()));

        List<Match> matches = new ArrayList<>();
        int v = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int fellBackFrom = -1;
            while (v != 0 && !trie.next.get(v).containsKey(c)) {
                fellBackFrom = v;
                v = trie.fail.get(v);
            }
            v = trie.next.get(v).getOrDefault(c, 0);
            List<Match> foundHere = new ArrayList<>();
            int u = v;
            while (u != 0) {
                for (String p : trie.out.get(u)) {
                    foundHere.add(new Match(p, i - p.length() + 1, i));
                }
                u = trie.outLink.get(u);
                if (u == -1) {
                    break;
                }
            }
            matches.addAll(foundHere);
            StringBuilder msg = new StringBuilder("Position " + i
                    + " '" + c + "': ");
            if (fellBackFrom != -1) {
                msg.append("state ").append(fellBackFrom)
                        .append(" has no '").append(c)
                        .append("' — follow failure link ")
                        .append(fellBackFrom).append("->")
                        .append(v == 0 ? 0 : trie.fail.get(fellBackFrom))
                        .append(", then '").append(c)
                        .append("' to state ").append(v);
            } else {
                msg.append("state ").append(v);
            }
            if (!foundHere.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (Match m : foundHere) {
                    names.add("'" + m.pattern + "' [" + m.start
                            + "," + m.end + "]");
                }
                msg.append(" — match ").append(String.join(", ", names));
                if (foundHere.size() > 1) {
                    msg.append(" (via the output link)");
                }
            } else {
                msg.append(" — no match here");
            }
            msg.append(". ").append(matches.size())
                    .append(matches.size() == 1 ? " match" : " matches")
                    .append(" so far.");
            steps.add(new Step(List.of(matches.size()), List.of(v),
                    List.of(), List.of(), msg.toString(), LINE_SCAN,
                    trie.snapshot(), trie.fullEdges()));
        }
        List<String> names = new ArrayList<>();
        for (Match m : matches) {
            names.add("'" + m.pattern + "' [" + m.start + ","
                    + m.end + "]");
        }
        steps.add(new Step(List.of(matches.size()), List.of(),
                List.of(), List.of(),
                "Aho-Corasick done: " + matches.size()
                        + (matches.size() == 1 ? " match" : " matches")
                        + (names.isEmpty() ? "."
                                : " — " + String.join(", ", names) + "."),
                LINE_FINAL, trie.snapshot(), trie.fullEdges()));
        return steps;
    }
}
