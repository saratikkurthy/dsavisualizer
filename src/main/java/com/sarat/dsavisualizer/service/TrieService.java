package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Trie (prefix tree) insert/search that records each character step
 * as a Step.
 *
 * Words are lowercased; empty words are ignored. Node ids are
 * 0-based in creation order (the root is id 0). Layout (computed
 * on every snapshot): level-order — depth gives y
 * (y = 40 + depth * 70), and a node's position among the nodes at
 * its depth, ordered by path, gives x (x = 40 + index * 70).
 * {@code sorted} holds the word-end node ids (green = a complete
 * word ends here). Trie nodes carry {@code label} = the character
 * ("" for the root) and {@code value} = 0; the frontend shows the
 * label when present. Line numbers: follow/create = 2, final = 0.
 */
@Service
public class TrieService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_FINAL = 0;

    /** Internal trie node. Children are a TreeMap for deterministic order. */
    private static class TrieNode {
        final char ch;
        final String path;
        final int id;
        final int depth;
        final TrieNode parent;
        final Map<Character, TrieNode> children = new TreeMap<>();
        boolean isWordEnd;

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

        /** Inserts a word silently (no steps); returns its end node. */
        TrieNode insertSilent(String word) {
            TrieNode node = root;
            for (int i = 0; i < word.length(); i++) {
                char ch = word.charAt(i);
                TrieNode child = node.children.get(ch);
                if (child == null) {
                    child = new TrieNode(ch, node.path + ch, nextId++,
                            node.depth + 1, node);
                    node.children.put(ch, child);
                    allNodes.add(child);
                }
                node = child;
            }
            node.isWordEnd = true;
            return node;
        }
    }

    /** Builds a trie from words (lowercased, empty ignored). */
    private Trie buildTrie(List<String> words) {
        Trie trie = new Trie();
        if (words != null) {
            for (String w : words) {
                if (w == null) {
                    continue;
                }
                String word = w.toLowerCase();
                if (!word.isEmpty()) {
                    trie.insertSilent(word);
                }
            }
        }
        return trie;
    }

    /**
     * Insert a word. One Step per character (follow an existing
     * node, or create a new one), then a final Step. If the word
     * already exists, the final Step says so instead.
     */
    public List<Step> insert(List<String> words, String word) {
        List<Step> steps = new ArrayList<>();
        Trie trie = buildTrie(words);
        String target = word == null ? "" : word.toLowerCase();
        TrieNode node = trie.root;
        for (int i = 0; i < target.length(); i++) {
            char ch = target.charAt(i);
            TrieNode child = node.children.get(ch);
            if (child != null) {
                node = child;
                steps.add(trieStep(trie, List.of(node.id),
                        "Follow '" + ch + "' — node exists.",
                        LINE_COMPARE));
            } else {
                child = new TrieNode(ch, node.path + ch, trie.nextId++,
                        node.depth + 1, node);
                node.children.put(ch, child);
                trie.allNodes.add(child);
                node = child;
                steps.add(trieStep(trie, List.of(node.id),
                        "Create node '" + ch + "'.",
                        LINE_COMPARE));
            }
        }
        boolean alreadyExists = node.isWordEnd;
        node.isWordEnd = true;
        String message = alreadyExists
                ? "'" + target + "' is already in the trie."
                : "Inserted '" + target + "'.";
        steps.add(trieStep(trie, List.of(node.id), message, LINE_FINAL));
        return steps;
    }

    /**
     * Search for a word. One Step per character followed, then a
     * final Step: found, prefix-but-not-a-word, or not found.
     */
    public List<Step> search(List<String> words, String target) {
        List<Step> steps = new ArrayList<>();
        Trie trie = buildTrie(words);
        String word = target == null ? "" : target.toLowerCase();
        TrieNode node = trie.root;
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            TrieNode child = node.children.get(ch);
            if (child == null) {
                steps.add(trieStep(trie, List.of(node.id),
                        "Not found — no '" + ch + "' after '"
                                + prefix + "'.",
                        LINE_FINAL));
                return steps;
            }
            node = child;
            prefix.append(ch);
            steps.add(trieStep(trie, List.of(node.id),
                    "Follow '" + ch + "'.",
                    LINE_COMPARE));
        }
        String message = node.isWordEnd
                ? "Found '" + word + "'!"
                : "'" + word + "' is a prefix but not a word.";
        steps.add(trieStep(trie, List.of(node.id), message, LINE_FINAL));
        return steps;
    }

    /** Word-end node ids in the current trie state. */
    private List<Integer> wordEndIds(Trie trie) {
        List<Integer> ids = new ArrayList<>();
        for (TrieNode node : trie.allNodes) {
            if (node.isWordEnd) {
                ids.add(node.id);
            }
        }
        return ids;
    }

    /** One trie Step: empty data, highlight = node ids, sorted = word ends. */
    private Step trieStep(Trie trie, List<Integer> highlight,
                          String message, int line) {
        return new Step(List.of(), highlight, List.of(),
                wordEndIds(trie), message, line,
                snapshotNodes(trie), snapshotEdges(trie));
    }

    /**
     * Full node snapshot: level-order layout — nodes at each depth
     * ordered by path; x = 40 + index * 70, y = 40 + depth * 70.
     * Nodes are listed in creation (id) order.
     */
    private List<NodeDto> snapshotNodes(Trie trie) {
        // Group nodes by depth, then sort each depth by path.
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

    /** Full edge snapshot: one parent-to-child pair per non-root node. */
    private List<EdgeDto> snapshotEdges(Trie trie) {
        List<EdgeDto> edges = new ArrayList<>();
        for (TrieNode node : trie.allNodes) {
            if (node.parent != null) {
                edges.add(new EdgeDto(node.parent.id, node.id));
            }
        }
        return edges;
    }
}
