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
 * Suffix trie (Phase 27, string matching v3): the trie of
 * all suffixes of a word — every substring is a prefix of
 * some suffix, so a substring search is a walk down the
 * trie. The idea behind suffix trees (which compress the
 * trie by merging single-child chains) and the suffix
 * array (which sorts the same suffixes).
 *
 * <p>Demo word "ababa". Nodes (id: string, depth): 0: "",
 * 0; 1: "a", 1; 2: "ab", 2; 3: "aba", 3; 4: "abab", 4;
 * 5: "ababa", 5; 6: "b", 1; 7: "ba", 2; 8: "bab", 3;
 * 9: "baba", 4. Ten nodes (root + 9). Suffix 0 ("ababa")
 * creates nodes 1..5; suffix 1 ("baba") creates nodes
 * 6..9; suffixes 2 ("aba"), 3 ("ba") and 4 ("a") are
 * already present and add nothing — the teaching moment:
 * a suffix trie has one node per DISTINCT substring
 * prefix, and the later suffixes are all prefixes of
 * earlier ones here. Trace: 5 suffix steps (line 2, one
 * per suffix) + final (line 0) = 6 steps. Suffix step:
 * {@code highlight} = the node ids created by that
 * insertion ([] when nothing is created), {@code data} =
 * [node count so far]. Final step {@code data} = [10].
 * Node {@code value} = depth, {@code label} = the string
 * ("" for the root). Edges weight null (the character is
 * the last char of the target node's label — the SAM
 * incoming-label theorem), ordered node id order,
 * insertion order within a node (the maps are
 * LinkedHashMaps). {@code nodes} / {@code edges} = the
 * trie created so far.</p>
 *
 * <p>Bad input (a missing/blank word) yields a single
 * "empty" Step; the controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class SuffixTrieService {

    private static final int LINE_INSERT = 2;
    private static final int LINE_FINAL = 0;

    /** Per-call mutable state (the service bean is a singleton, so no instance state). */
    private static final class Trie {
        final List<Map<Character, Integer>> next = new ArrayList<>();
        final List<String> label = new ArrayList<>();
        final List<Integer> depth = new ArrayList<>();

        Trie() {
            next.add(new LinkedHashMap<>());
            label.add("");
            depth.add(0);
        }

        /**
         * Inserts one suffix, returning the node ids it
         * created (empty when the suffix is already present).
         */
        List<Integer> insert(String suffix) {
            List<Integer> created = new ArrayList<>();
            int v = 0;
            for (int i = 0; i < suffix.length(); i++) {
                char c = suffix.charAt(i);
                Integer nxt = next.get(v).get(c);
                if (nxt == null) {
                    nxt = next.size();
                    next.get(v).put(c, nxt);
                    next.add(new LinkedHashMap<>());
                    label.add(label.get(v) + c);
                    depth.add(depth.get(v) + 1);
                    created.add(nxt);
                }
                v = nxt;
            }
            return created;
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

        /** Transitions, node id order, insertion order within a node. */
        List<EdgeDto> edgeSnapshot() {
            List<EdgeDto> edges = new ArrayList<>();
            for (int i = 0; i < next.size(); i++) {
                for (int to : next.get(i).values()) {
                    edges.add(new EdgeDto(i, to, null));
                }
            }
            return edges;
        }
    }

    /** Builds the suffix trie of {@code word}, one suffix step per suffix. */
    public List<Step> suffixTrie(String word) {
        List<Step> steps = new ArrayList<>();
        if (word == null || word.isBlank()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A word is needed.", LINE_FINAL));
            return steps;
        }
        Trie trie = new Trie();
        List<Integer> buildingSuffixes = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            String suffix = word.substring(i);
            List<Integer> created = trie.insert(suffix);
            if (!created.isEmpty()) {
                buildingSuffixes.add(i);
            }
            String msg;
            if (created.isEmpty()) {
                msg = "Suffix " + i + " '" + suffix
                        + "': already present — no new nodes. "
                        + trie.next.size() + " nodes so far.";
            } else {
                msg = "Suffix " + i + " '" + suffix + "': "
                        + created.size() + " new node"
                        + (created.size() == 1 ? "" : "s") + " "
                        + created + ". " + trie.next.size()
                        + " nodes so far.";
            }
            steps.add(new Step(List.of(trie.next.size()),
                    new ArrayList<>(created), List.of(), List.of(),
                    msg, LINE_INSERT, trie.snapshot(),
                    trie.edgeSnapshot()));
        }
        steps.add(new Step(List.of(trie.next.size()), List.of(),
                List.of(), List.of(),
                "Suffix trie done: " + trie.next.size()
                        + " nodes (root + " + (trie.next.size() - 1)
                        + "); suffixes " + buildingSuffixes
                        + " created nodes, the rest were already "
                        + "present.",
                LINE_FINAL, trie.snapshot(), trie.edgeSnapshot()));
        return steps;
    }
}
