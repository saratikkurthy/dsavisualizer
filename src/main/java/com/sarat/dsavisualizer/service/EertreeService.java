package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Palindromic tree / eertree (Phase 26, string algorithms
 * v2): one node per distinct palindromic substring, plus
 * two roots — node 0 (len &minus;1, label "\u22121") and
 * node 1 (len 0, label "\u03b5"). Each position adds at
 * most one new node, so the tree has at most n + 2 nodes.
 * The idea behind "how many distinct palindromes?" and
 * "the longest palindromic substring?" in O(n).
 *
 * <p>Demo string "ababa". Nodes (id: len, link, next):
 * 0: &minus;1, 0, {a:2, b:3}; 1: 0, 0, {}; 2: 1, 1, {b:5};
 * 3: 1, 1, {a:4}; 4: 3, 2, {}; 5: 3, 3, {a:6}; 6: 5, 4, {}.
 * Palindromes: "a", "b", "aba", "bab", "ababa" — 5
 * distinct palindromic substrings; longest "ababa" (len 5).
 * Trace: 5 character steps (line 2, one per character) +
 * final (line 0) = 6 steps. Character step: {@code data} =
 * [node count so far], {@code highlight} = [the new node
 * id] (or [the reused node id] when the palindrome already
 * exists — none reused in this demo), {@code sorted} = [the
 * longest suffix-palindrome's node id]. Final step {@code
 * data} = [5] (distinct palindromes), {@code sorted} = [6].
 * {@code nodes} / {@code edges} = all nodes / next-edges +
 * suffix links created so far. Node {@code value} = the
 * node's {@code len}, {@code label} = the palindrome
 * string ("\u22121" for the len &minus;1 root, "\u03b5"
 * for the len 0 root). {@code edges} are ordered
 * next-edges first (node id order, insertion order within
 * a node — the maps are LinkedHashMaps), then suffix links
 * (node id order; the len &minus;1 root's self-link is
 * not drawn).</p>
 *
 * <p>Bad input (a missing/blank word) yields a single
 * "empty" Step; the controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class EertreeService {

    private static final int LINE_ADD = 2;
    private static final int LINE_FINAL = 0;

    /** Per-call mutable state (the service bean is a singleton, so no instance state). */
    private static final class Tree {
        final List<Integer> len = new ArrayList<>();
        final List<Integer> link = new ArrayList<>();
        final List<Map<Character, Integer>> next = new ArrayList<>();
        final List<String> palindrome = new ArrayList<>();
        int last = 1;

        Tree() {
            len.add(-1);
            link.add(0);
            next.add(new LinkedHashMap<>());
            palindrome.add("\u22121");
            len.add(0);
            link.add(0);
            next.add(new LinkedHashMap<>());
            palindrome.add("\u03b5");
        }

        /**
         * Adds s[pos] to the tree. Returns the node id of the
         * longest suffix-palindrome ending at pos; whether it
         * was newly created is recorded in {@code created}.
         */
        int add(String s, int pos, boolean[] created) {
            char c = s.charAt(pos);
            int cur = last;
            while (true) {
                int curLen = len.get(cur);
                if (pos - 1 - curLen >= 0
                        && s.charAt(pos - 1 - curLen) == c) {
                    break;
                }
                cur = link.get(cur);
            }
            if (next.get(cur).containsKey(c)) {
                last = next.get(cur).get(c);
                created[0] = false;
                return last;
            }
            int node = len.size();
            len.add(len.get(cur) + 2);
            link.add(0);
            next.add(new LinkedHashMap<>());
            palindrome.add(s.substring(pos - len.get(node) + 1,
                    pos + 1));
            next.get(cur).put(c, node);
            if (len.get(node) == 1) {
                link.set(node, 1);
            } else {
                int tmp = link.get(cur);
                while (true) {
                    int tmpLen = len.get(tmp);
                    if (pos - 1 - tmpLen >= 0
                            && s.charAt(pos - 1 - tmpLen) == c) {
                        break;
                    }
                    tmp = link.get(tmp);
                }
                link.set(node, next.get(tmp).get(c));
            }
            last = node;
            created[0] = true;
            return node;
        }

        /** One NodeDto per node created so far, in id order. */
        List<NodeDto> snapshot() {
            List<NodeDto> dtos = new ArrayList<>();
            for (int i = 0; i < len.size(); i++) {
                NodeDto dto = new NodeDto(i, len.get(i), 0, 0);
                dto.setLabel(palindrome.get(i));
                dtos.add(dto);
            }
            return dtos;
        }

        /** Next-edges first, then suffix links (see class Javadoc). */
        List<EdgeDto> edgeSnapshot() {
            List<EdgeDto> edges = new ArrayList<>();
            for (int i = 0; i < len.size(); i++) {
                for (Map.Entry<Character, Integer> e
                        : next.get(i).entrySet()) {
                    edges.add(new EdgeDto(i, e.getValue(), null));
                }
            }
            for (int i = 1; i < len.size(); i++) {
                edges.add(new EdgeDto(i, link.get(i), -1));
            }
            return edges;
        }
    }

    /** Builds the palindromic tree of {@code word}, one character step per position. */
    public List<Step> eertree(String word) {
        List<Step> steps = new ArrayList<>();
        if (word == null || word.isBlank()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A word is needed.", LINE_FINAL));
            return steps;
        }
        Tree tree = new Tree();
        for (int i = 0; i < word.length(); i++) {
            boolean[] created = new boolean[1];
            int node = tree.add(word, i, created);
            steps.add(new Step(
                    List.of(tree.len.size()), List.of(node),
                    List.of(), List.of(node),
                    "Position " + i + " '" + word.charAt(i) + "': "
                            + (created[0]
                                    ? "new palindrome \""
                                            + tree.palindrome.get(node)
                                            + "\" (node " + node + ")."
                                    : "palindrome \""
                                            + tree.palindrome.get(node)
                                            + "\" already known (node "
                                            + node + ").")
                            + " " + tree.len.size()
                            + " nodes so far.",
                    LINE_ADD, tree.snapshot(),
                    tree.edgeSnapshot()));
        }
        int distinct = tree.len.size() - 2;
        int longestNode = tree.last;
        steps.add(new Step(
                List.of(distinct), List.of(), List.of(),
                List.of(longestNode),
                "Palindromic tree done: " + distinct
                        + " distinct palindromes; longest \""
                        + tree.palindrome.get(longestNode) + "\".",
                LINE_FINAL, tree.snapshot(),
                tree.edgeSnapshot()));
        return steps;
    }
}
