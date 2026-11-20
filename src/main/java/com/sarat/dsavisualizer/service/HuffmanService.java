package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Huffman coding (Phase 19): build the optimal prefix-code tree
 * for a word by repeatedly merging the two lowest-frequency
 * nodes, and read the code of each character off the tree
 * (left = 0, right = 1).
 *
 * <p>Leaves are created in first-appearance order in the word.
 * The priority queue is ordered by (frequency, insertion
 * sequence); a merged node takes the next sequence number. The
 * left child is the first popped node, the right child the
 * second. One Step per merge (line 2), then the final Step
 * (line 0) with the codes, the encoded string and the bit
 * count.</p>
 *
 * <p>Steps carry the forest/tree being built ({@code nodes}/
 * {@code edges}); {@code data} is empty. NodeDto {@code value}
 * is the frequency, {@code label} is the character for leaves
 * and null for internal nodes, {@code highlight} is the new
 * merged node's id. Layout is fixed from the final tree so node
 * positions never move between steps: leaves sit on the bottom
 * row in the final tree's left-to-right leaf order (90 px apart),
 * each internal node sits centred above its children, one level
 * (70 px) per unit of height above the leaves. {@code edges}
 * connect each parent to its two children.</p>
 *
 * <p>The final message lists the codes in the tree's
 * left-to-right leaf order — which is also code order
 * ("0" &lt; "100" &lt; "101" &lt; "11") — so "banana" reads
 * "Codes: a=0, b=10, n=11." and "mississippi" reads
 * "Codes: s=0, m=100, p=101, i=11.". A single distinct
 * character has no merges and the code "0".</p>
 *
 * <p>The controller validates the input (1..12 characters,
 * lowercase a–z only); this method guards against bad input
 * anyway with a single "empty" Step.</p>
 */
@Service
public class HuffmanService {

    private static final int LINE_COMPARE = 2;
    private static final int LINE_FINAL = 0;

    /** Horizontal gap between neighbouring leaves, in px. */
    private static final double LEAF_GAP = 90;
    /** Left edge of the leaf row, in px. */
    private static final double LEAF_X0 = 60;
    /** The leaf row's y, in px; each level above it is one row up. */
    private static final double LEAF_Y = 320;
    /** Vertical gap between tree levels, in px. */
    private static final double LEVEL_GAP = 70;

    /** One node of the Huffman tree while it is being built. */
    private static class HuffNode {
        final int id;
        final int freq;
        final Character ch; // null for internal nodes
        final HuffNode left;
        final HuffNode right;
        final int seq;
        double x;
        double y;

        HuffNode(int id, int freq, Character ch,
                 HuffNode left, HuffNode right, int seq) {
            this.id = id;
            this.freq = freq;
            this.ch = ch;
            this.left = left;
            this.right = right;
            this.seq = seq;
        }

        boolean isLeaf() {
            return ch != null;
        }
    }

    /** One merge: the two children and the node they became. */
    private static class Merge {
        final HuffNode first;
        final HuffNode second;
        final HuffNode parent;

        Merge(HuffNode first, HuffNode second, HuffNode parent) {
            this.first = first;
            this.second = second;
            this.parent = parent;
        }
    }

    /**
     * Builds the Huffman tree for {@code word} and returns one
     * Step per merge plus the final Step.
     */
    public List<Step> huffman(String word) {
        List<Step> steps = new ArrayList<>();
        if (word == null || word.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A word is needed.", LINE_FINAL));
            return steps;
        }
        // Leaves in first-appearance order; ids and sequence
        // numbers share one counter, leaves first.
        Map<Character, Integer> freq = new LinkedHashMap<>();
        for (char c : word.toCharArray()) {
            freq.merge(c, 1, Integer::sum);
        }
        PriorityQueue<HuffNode> pq = new PriorityQueue<>(
                Comparator.comparingInt((HuffNode n) -> n.freq)
                        .thenComparingInt(n -> n.seq));
        List<HuffNode> leaves = new ArrayList<>();
        int nextId = 0;
        for (Map.Entry<Character, Integer> e : freq.entrySet()) {
            HuffNode leaf = new HuffNode(nextId, e.getValue(), e.getKey(),
                    null, null, nextId);
            nextId++;
            leaves.add(leaf);
            pq.add(leaf);
        }
        List<Merge> merges = new ArrayList<>();
        while (pq.size() > 1) {
            HuffNode first = pq.poll();
            HuffNode second = pq.poll();
            HuffNode parent = new HuffNode(nextId,
                    first.freq + second.freq, null, first, second, nextId);
            nextId++;
            merges.add(new Merge(first, second, parent));
            pq.add(parent);
        }
        HuffNode root = pq.poll();
        layout(root);

        // One step per merge: the forest so far.
        for (Merge merge : merges) {
            steps.add(new Step(List.of(), List.of(merge.parent.id),
                    List.of(), List.of(),
                    "Merge " + describe(merge.first) + " + "
                            + describe(merge.second) + " → node ("
                            + merge.parent.freq + ").",
                    LINE_COMPARE,
                    forestNodes(leaves, merges, merge.parent.id),
                    forestEdges(merges, merge.parent.id)));
        }

        // Codes in the tree's left-to-right leaf order.
        Map<Character, String> codes = new LinkedHashMap<>();
        walkCodes(root, "", codes);
        StringBuilder encoded = new StringBuilder();
        for (char c : word.toCharArray()) {
            encoded.append(codes.get(c));
        }
        StringBuilder codeList = new StringBuilder();
        for (Map.Entry<Character, String> e : codes.entrySet()) {
            if (codeList.length() > 0) {
                codeList.append(", ");
            }
            codeList.append(e.getKey()).append("=").append(e.getValue());
        }
        List<NodeDto> finalNodes;
        List<EdgeDto> finalEdges;
        if (merges.isEmpty()) {
            finalNodes = forestNodes(leaves, merges, -1);
            finalEdges = List.of();
        } else {
            int lastId = merges.get(merges.size() - 1).parent.id;
            finalNodes = forestNodes(leaves, merges, lastId);
            finalEdges = forestEdges(merges, lastId);
        }
        steps.add(new Step(List.of(), List.of(), List.of(), List.of(),
                "Codes: " + codeList + ". \"" + word + "\" → " + encoded
                        + " (" + encoded.length() + " bits vs "
                        + (word.length() * 8) + ").",
                LINE_FINAL, finalNodes, finalEdges));
        return steps;
    }

    /** "'b' (1)" for a leaf, "node (3)" for an internal node. */
    private String describe(HuffNode node) {
        if (node.isLeaf()) {
            return "'" + node.ch + "' (" + node.freq + ")";
        }
        return "node (" + node.freq + ")";
    }

    /** Left-first walk recording each leaf's code (left = 0). */
    private void walkCodes(HuffNode node, String prefix,
                           Map<Character, String> codes) {
        if (node.isLeaf()) {
            codes.put(node.ch, prefix.isEmpty() ? "0" : prefix);
            return;
        }
        walkCodes(node.left, prefix + "0", codes);
        walkCodes(node.right, prefix + "1", codes);
    }

    /**
     * Fixes every node's (x, y) from the final tree: leaves in
     * left-to-right order on the bottom row, each internal node
     * centred above its children, one level per unit of height
     * above the leaves. Positions never move between steps.
     */
    private void layout(HuffNode root) {
        int[] slot = {0};
        layoutLeaves(root, slot);
        layoutHeights(root);
    }

    /** Assigns leaf x slots in left-to-right order; returns nothing. */
    private void layoutLeaves(HuffNode node, int[] slot) {
        if (node.isLeaf()) {
            node.x = LEAF_X0 + slot[0] * LEAF_GAP;
            node.y = LEAF_Y;
            slot[0]++;
            return;
        }
        layoutLeaves(node.left, slot);
        layoutLeaves(node.right, slot);
    }

    /**
     * Assigns internal positions bottom-up; returns the node's
     * height above the leaves (leaves are 0).
     */
    private int layoutHeights(HuffNode node) {
        if (node.isLeaf()) {
            return 0;
        }
        int leftHeight = layoutHeights(node.left);
        int rightHeight = layoutHeights(node.right);
        node.x = (node.left.x + node.right.x) / 2;
        int height = 1 + Math.max(leftHeight, rightHeight);
        node.y = LEAF_Y - height * LEVEL_GAP;
        return height;
    }

    /**
     * The forest snapshot after the merge that created
     * {@code upToId} (all leaves plus every merged node up to and
     * including that id; -1 means leaves only).
     */
    private List<NodeDto> forestNodes(List<HuffNode> leaves,
                                      List<Merge> merges, int upToId) {
        List<NodeDto> nodes = new ArrayList<>();
        for (HuffNode leaf : leaves) {
            nodes.add(toDto(leaf));
        }
        for (Merge merge : merges) {
            if (merge.parent.id > upToId) {
                break;
            }
            nodes.add(toDto(merge.parent));
        }
        return nodes;
    }

    /** Parent-to-child edges for every merged node up to {@code upToId}. */
    private List<EdgeDto> forestEdges(List<Merge> merges, int upToId) {
        List<EdgeDto> edges = new ArrayList<>();
        for (Merge merge : merges) {
            if (merge.parent.id > upToId) {
                break;
            }
            edges.add(new EdgeDto(merge.parent.id, merge.first.id));
            edges.add(new EdgeDto(merge.parent.id, merge.second.id));
        }
        return edges;
    }

    /** A HuffNode as a NodeDto: value = frequency, label = the leaf char. */
    private NodeDto toDto(HuffNode node) {
        NodeDto dto = new NodeDto(node.id, node.freq, node.x, node.y);
        if (node.isLeaf()) {
            dto.setLabel(String.valueOf(node.ch));
        }
        return dto;
    }
}
