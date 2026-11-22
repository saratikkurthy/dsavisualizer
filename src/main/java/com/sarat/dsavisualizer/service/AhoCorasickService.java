package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Aho-Corasick multi-pattern string matching (Phase 21): build a
 * trie of all patterns, compute failure links with BFS, then scan
 * the text once, reporting every pattern ending at each position
 * (following output links up the failure chain).
 *
 * <p>Node ids are assigned in insertion order (root 0), exactly
 * as in the sim. Failure links are computed by BFS over the trie
 * with children visited in alphabetical order, so the BFS order
 * for the demo (patterns he/she/his/hers) is
 * [1, 3, 2, 6, 4, 8, 7, 5, 9]. Layout reuses the TrieService
 * level-order layout (depth gives y, path order within a depth
 * gives x); nodes carry {@code label} = the character ("" for
 * the root).</p>
 *
 * <p>Steps: one per trie node created (line 1), one per failure
 * link set (line 2; failure links are edges with weight 2,
 * trie edges weight 1), one per text character scanned
 * (line 7 on a match, 5 when a failure transition was followed
 * and no match, 6 otherwise), plus the final Step (line 0).
 * Scan steps carry {@code data} = char codes of the text,
 * {@code highlight} = [text index], {@code aux} = [current
 * state node id], {@code sorted} = pattern indices matched
 * ending at that index (patterns-list order). The final Step
 * carries {@code sorted} = all matched pattern indices
 * (unique, patterns-list order). Demo: 9 + 9 + 6 + 1 = 25 steps.</p>
 *
 * <p>The controller validates the input (text 1..30 chars,
 * 1..5 patterns, each 1..10 chars, all lowercase a–z); this
 * method guards against bad input anyway with a single
 * "empty" Step.</p>
 */
@Service
public class AhoCorasickService {

    private static final int LINE_INSERT = 1;
    private static final int LINE_FAILURE = 2;
    private static final int LINE_MATCH = 7;
    private static final int LINE_FALLBACK = 5;
    private static final int LINE_SCAN = 6;
    private static final int LINE_FINAL = 0;

    /** Internal trie node. Children are a TreeMap for deterministic order. */
    private static class TrieNode {
        final char ch;
        final String path;
        final int id;
        final int depth;
        final TrieNode parent;
        final Map<Character, TrieNode> children = new TreeMap<>();
        final List<Integer> output = new ArrayList<>();
        int fail = 0;

        TrieNode(char ch, String path, int id, int depth, TrieNode parent) {
            this.ch = ch;
            this.path = path;
            this.id = id;
            this.depth = depth;
            this.parent = parent;
        }
    }

    /** A trie under construction: the root, the nodes in creation order. */
    private static class Trie {
        final TrieNode root = new TrieNode('\0', "", 0, 0, null);
        final List<TrieNode> allNodes = new ArrayList<>();
        int nextId = 1;

        Trie() {
            allNodes.add(root);
        }
    }

    /**
     * Runs Aho-Corasick: {@code text} is the text to scan,
     * {@code patterns} are the patterns to find.
     */
    public List<Step> ahocorasick(String text, List<String> patterns) {
        List<Step> steps = new ArrayList<>();
        String t = text == null ? "" : text.toLowerCase();
        List<String> pats = new ArrayList<>();
        if (patterns != null) {
            for (String p : patterns) {
                if (p != null && !p.isEmpty()) {
                    pats.add(p.toLowerCase());
                }
            }
        }
        if (t.isEmpty() || pats.isEmpty()) {
            steps.add(ahoStep(new Trie(), List.of(), List.of(),
                    "A text and at least one pattern are needed.",
                    LINE_FINAL, List.of(), List.of()));
            return steps;
        }

        Trie trie = new Trie();
        // Build the trie, one step per node created. Following an
        // existing node is not a step — only creations are.
        for (int pi = 0; pi < pats.size(); pi++) {
            String pat = pats.get(pi);
            TrieNode node = trie.root;
            for (int i = 0; i < pat.length(); i++) {
                char ch = pat.charAt(i);
                TrieNode child = node.children.get(ch);
                if (child == null) {
                    child = new TrieNode(ch, node.path + ch, trie.nextId++,
                            node.depth + 1, node);
                    node.children.put(ch, child);
                    trie.allNodes.add(child);
                    if (i == pat.length() - 1) {
                        // The pattern ends at the node just created,
                        // so the step already shows it as terminal.
                        child.output.add(pi);
                    }
                    steps.add(ahoStep(trie, List.of(child.id),
                            terminalIds(trie),
                            "Insert '" + ch + "' of pattern '" + pat
                                    + "' \u2192 node " + child.id + ".",
                            LINE_INSERT, trieEdges(trie), List.of()));
                }
                node = child;
            }
            if (!node.output.contains(pi)) {
                node.output.add(pi);
            }
        }

        // Failure links by BFS, children in alphabetical order
        // (TreeMap iteration). Root's children fail to the root.
        List<TrieNode> failSet = new ArrayList<>();
        Deque<TrieNode> queue = new ArrayDeque<>();
        for (TrieNode child : trie.root.children.values()) {
            child.fail = 0;
            queue.add(child);
            failSet.add(child);
        }
        while (!queue.isEmpty()) {
            TrieNode u = queue.poll();
            for (TrieNode v : u.children.values()) {
                int f = u.fail;
                while (f != 0 && !trie.allNodes.get(f).children
                        .containsKey(v.ch)) {
                    f = trie.allNodes.get(f).fail;
                }
                TrieNode target = trie.allNodes.get(f).children.get(v.ch);
                v.fail = target == null ? 0 : target.id;
                queue.add(v);
                failSet.add(v);
            }
        }
        // One step per failure link, in BFS order, with the links
        // set so far shown as weight-2 edges.
        List<TrieNode> linksSoFar = new ArrayList<>();
        for (TrieNode v : failSet) {
            linksSoFar.add(v);
            List<EdgeDto> edges = trieEdges(trie);
            edges.addAll(failureEdges(linksSoFar));
            steps.add(ahoStep(trie, List.of(v.id), terminalIds(trie),
                    "Failure link: node " + v.id + " ('" + v.ch
                            + "') \u2192 node " + v.fail + ".",
                    LINE_FAILURE, edges, List.of()));
        }

        // Scan the text once.
        List<Integer> charCodes = charCodes(t);
        List<EdgeDto> fullEdges = trieEdges(trie);
        fullEdges.addAll(failureEdges(failSet));
        List<int[]> allMatches = new ArrayList<>();
        int state = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            boolean fallback = false;
            while (state != 0 && !trie.allNodes.get(state).children
                    .containsKey(c)) {
                state = trie.allNodes.get(state).fail;
                fallback = true;
            }
            TrieNode next = trie.allNodes.get(state).children.get(c);
            state = next == null ? 0 : next.id;
            // Outputs at the state, then up the failure chain.
            List<Integer> found = new ArrayList<>();
            int s = state;
            while (s != 0) {
                found.addAll(trie.allNodes.get(s).output);
                s = trie.allNodes.get(s).fail;
            }
            found.sort(Integer::compareTo);
            for (int pi : found) {
                allMatches.add(new int[]{pi, i});
            }
            int line = !found.isEmpty() ? LINE_MATCH
                    : fallback ? LINE_FALLBACK : LINE_SCAN;
            StringBuilder message = new StringBuilder();
            message.append("Read '").append(c).append("' at ").append(i)
                    .append(" \u2192 state ").append(state);
            if (fallback) {
                message.append(" (via failure link)");
            }
            if (!found.isEmpty()) {
                // Display order is by start, then end (the design's
                // example lists 'she' (1–3) before 'he' (2–3));
                // sorted stays in patterns-list order.
                List<Integer> display = new ArrayList<>(found);
                final int end = i;
                display.sort((a, b) -> {
                    int sa = end - pats.get(a).length() + 1;
                    int sb = end - pats.get(b).length() + 1;
                    return Integer.compare(sa, sb);
                });
                message.append("; match ");
                for (int j = 0; j < display.size(); j++) {
                    if (j > 0) {
                        message.append(", ");
                    }
                    int pi = display.get(j);
                    message.append("'").append(pats.get(pi)).append("' (")
                            .append(i - pats.get(pi).length() + 1)
                            .append("\u2013").append(i).append(")");
                }
                message.append(".");
            } else {
                message.append(".");
            }
            steps.add(ahoStep(trie, List.of(i), new ArrayList<>(found),
                    message.toString(), line, fullEdges, charCodes,
                    List.of(state)));
        }

        // Final step: all matched pattern indices, unique, in
        // patterns-list order; matches listed by start, then end.
        List<Integer> matched = new ArrayList<>();
        for (int[] m : allMatches) {
            if (!matched.contains(m[0])) {
                matched.add(m[0]);
            }
        }
        matched.sort(Integer::compareTo);
        List<int[]> byPosition = new ArrayList<>(allMatches);
        byPosition.sort((a, b) -> {
            int sa = a[1] - pats.get(a[0]).length() + 1;
            int sb = b[1] - pats.get(b[0]).length() + 1;
            if (sa != sb) {
                return Integer.compare(sa, sb);
            }
            return Integer.compare(a[1], b[1]);
        });
        StringBuilder finalMessage = new StringBuilder();
        if (allMatches.isEmpty()) {
            finalMessage.append("No matches found.");
        } else {
            finalMessage.append(allMatches.size())
                    .append(allMatches.size() == 1 ? " match: " : " matches: ");
            for (int j = 0; j < byPosition.size(); j++) {
                if (j > 0) {
                    finalMessage.append(", ");
                }
                int[] m = byPosition.get(j);
                finalMessage.append("'").append(pats.get(m[0])).append("' (")
                        .append(m[1] - pats.get(m[0]).length() + 1)
                        .append("\u2013").append(m[1]).append(")");
            }
            finalMessage.append(".");
        }
        steps.add(ahoStep(trie, List.of(), matched,
                finalMessage.toString(), LINE_FINAL, fullEdges, charCodes,
                List.of(state)));
        return steps;
    }

    /** Terminal (word-end) node ids in the current trie state. */
    private List<Integer> terminalIds(Trie trie) {
        List<Integer> ids = new ArrayList<>();
        for (TrieNode node : trie.allNodes) {
            if (!node.output.isEmpty()) {
                ids.add(node.id);
            }
        }
        return ids;
    }

    /** Char codes of the text. */
    private List<Integer> charCodes(String text) {
        List<Integer> codes = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            codes.add((int) text.charAt(i));
        }
        return codes;
    }

    /** Trie edges (weight 1), one per non-root node. */
    private List<EdgeDto> trieEdges(Trie trie) {
        List<EdgeDto> edges = new ArrayList<>();
        for (TrieNode node : trie.allNodes) {
            if (node.parent != null) {
                edges.add(new EdgeDto(node.parent.id, node.id, 1));
            }
        }
        return edges;
    }

    /** Failure-link edges (weight 2) for the given nodes. */
    private List<EdgeDto> failureEdges(List<TrieNode> nodes) {
        List<EdgeDto> edges = new ArrayList<>();
        for (TrieNode node : nodes) {
            edges.add(new EdgeDto(node.id, node.fail, 2));
        }
        return edges;
    }

    /**
     * Full node snapshot: the TrieService level-order layout —
     * nodes at each depth ordered by path; x = 40 + index * 70,
     * y = 40 + depth * 70. Nodes are listed in creation (id) order.
     */
    private List<NodeDto> snapshotNodes(Trie trie) {
        Map<Integer, List<TrieNode>> byDepth = new HashMap<>();
        for (TrieNode node : trie.allNodes) {
            byDepth.computeIfAbsent(node.depth, k -> new ArrayList<>())
                    .add(node);
        }
        Map<Integer, double[]> coords = new HashMap<>();
        for (Map.Entry<Integer, List<TrieNode>> entry : byDepth.entrySet()) {
            List<TrieNode> level = entry.getValue();
            level.sort((a, b) -> a.path.compareTo(b.path));
            for (int i = 0; i < level.size(); i++) {
                TrieNode node = level.get(i);
                coords.put(node.id, new double[]{
                        40 + i * 70, 40 + node.depth * 70});
            }
        }
        List<NodeDto> dtos = new ArrayList<>();
        for (TrieNode node : trie.allNodes) {
            double[] xy = coords.get(node.id);
            NodeDto dto = new NodeDto(node.id, 0, xy[0], xy[1]);
            dto.setLabel(node == trie.root ? "" : String.valueOf(node.ch));
            dtos.add(dto);
        }
        return dtos;
    }

    /** One Aho-Corasick Step: data = char codes, aux unset. */
    private Step ahoStep(Trie trie, List<Integer> highlight,
                         List<Integer> sorted, String message, int line,
                         List<EdgeDto> edges, List<Integer> data) {
        return new Step(new ArrayList<>(data), highlight,
                List.of(), sorted, message, line,
                snapshotNodes(trie), new ArrayList<>(edges));
    }

    /** One Aho-Corasick Step with aux = [state] (scan/final steps). */
    private Step ahoStep(Trie trie, List<Integer> highlight,
                         List<Integer> sorted, String message, int line,
                         List<EdgeDto> edges, List<Integer> data,
                         List<Integer> aux) {
        Step step = ahoStep(trie, highlight, sorted, message, line,
                edges, data);
        step.setAux(new ArrayList<>(aux));
        return step;
    }
}
