package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Sqrt decomposition v2 — range add with lazy blocks
 * (Phase 27, range queries v3): the array is split into
 * blocks of about sqrt(n); a fully covered block is
 * updated by its lazy tag alone (O(1)), a partial block
 * is updated element by element and its min recomputed.
 * A range minimum reads the partial blocks directly and
 * the full blocks' stored minima. Range add and range
 * minimum are both O(sqrt(n)). The idea behind lazy
 * propagation without a tree — the same lazy tag idea
 * as the lazy segment tree (Phase 27), in block form.
 *
 * <p>Demo array [3, 1, 4, 1, 5, 9, 2, 6] (n=8, block size
 * 3 = ceil(sqrt(8))). Block mins start [1, 1, 2], lazy
 * [0, 0, 0]. Demo update: add +2 to [1, 7]. Block 0 is
 * partial (indices 1, 2 += 2, stored [3, 3, 6], block
 * min 1 -&gt; 3); block 1 is FULLY covered (lazy[1] += 2,
 * block min 1 -&gt; 3); block 2 is partial (indices 6, 7
 * += 2, stored [4, 8], block min 2 -&gt; 4). After the
 * update: block mins [3, 3, 4], lazy [0, 2, 0], stored
 * array [3, 3, 6, 1, 5, 9, 4, 8], true array [3, 3, 6,
 * 3, 7, 11, 4, 8]. (An earlier demo — add +2 to [1, 5]
 * — covered NO full block, so no lazy tag was ever set;
 * the demo was changed to [1, 7] so block 1 is fully
 * covered.) Demo queries: min [0, 7] = 3 (min of the
 * three block mins), min [4, 7] = 4 (min of a[4]=7,
 * a[5]=11 and block 2's min 4). Trace: 1 build step
 * (line 1) + 3 update steps (line 2, one per block
 * touched) + 2 query steps (line 3, one per query) +
 * final (line 0) = 7 steps. {@code nodes} = one NodeDto
 * per array element, in index order, {@code value} =
 * the TRUE element value (stored value + lazy[block]) —
 * the user sees true values — {@code label} = the value
 * as a string. {@code data} = block mins after that
 * step ([3, 3, 4] after the update); {@code sorted} =
 * lazy tags ([0, 2, 0] after the update). {@code
 * highlight} = element indices touched this step, plus
 * block highlights = block id + 100 (100/101/102, the
 * Phase 26 convention — element highlights are indices
 * 0..7, block highlights are 100+, so the two can never
 * collide). Query steps: {@code data} = [answer]
 * (their {@code sorted} is still the lazy tags). No
 * {@code edges} (null).</p>
 *
 * <p>Bad input (an unparseable array, a range that is not
 * [l, r] with 0 &le; l &le; r &lt; n, or a missing add
 * amount) yields a single "empty" Step; the controller
 * 400s on it (see VisualizationController).</p>
 */
@Service
public class SqrtDecomp2Service {

    private static final int LINE_BUILD = 1;
    private static final int LINE_UPDATE = 2;
    private static final int LINE_QUERY = 3;
    private static final int LINE_FINAL = 0;

    /** Per-call mutable state (the service bean is a singleton, so no instance state). */
    private static final class Blocks {
        final int[] stored;
        final int blockSize;
        final int[] lazy;

        Blocks(int[] array) {
            this.stored = array.clone();
            this.blockSize = (int) Math.ceil(Math.sqrt(array.length));
            int blockCount = (array.length + blockSize - 1) / blockSize;
            this.lazy = new int[blockCount];
        }

        int blockCount() {
            return lazy.length;
        }

        int blockStart(int b) {
            return b * blockSize;
        }

        int blockEnd(int b) {
            return Math.min(blockStart(b) + blockSize, stored.length) - 1;
        }

        /** True value of a[i] (stored value + its block's lazy tag). */
        int trueValue(int i) {
            return stored[i] + lazy[i / blockSize];
        }

        /** True minimum of block b (min of true values). */
        int blockMin(int b) {
            int min = Integer.MAX_VALUE;
            for (int i = blockStart(b); i <= blockEnd(b); i++) {
                min = Math.min(min, trueValue(i));
            }
            return min;
        }

        /** All block mins, in block order. */
        List<Integer> minList() {
            List<Integer> list = new ArrayList<>();
            for (int b = 0; b < blockCount(); b++) {
                list.add(blockMin(b));
            }
            return list;
        }

        /** All lazy tags, in block order. */
        List<Integer> lazyList() {
            List<Integer> list = new ArrayList<>();
            for (int tag : lazy) {
                list.add(tag);
            }
            return list;
        }

        /** One NodeDto per array element, in index order (TRUE values). */
        List<NodeDto> snapshot() {
            List<NodeDto> dtos = new ArrayList<>();
            for (int i = 0; i < stored.length; i++) {
                int value = trueValue(i);
                NodeDto dto = new NodeDto(i, value, 0, 0);
                dto.setLabel(String.valueOf(value));
                dtos.add(dto);
            }
            return dtos;
        }

        /**
         * Pushes block b's tag into its stored values
         * (needed before a partial update of a block that
         * already carries a tag).
         */
        void push(int b) {
            if (lazy[b] != 0) {
                for (int i = blockStart(b); i <= blockEnd(b); i++) {
                    stored[i] += lazy[b];
                }
                lazy[b] = 0;
            }
        }
    }

    /**
     * Adds {@code index} to [values[0], values[1]], then
     * answers min [values[2], values[3]] and min
     * [values[4], values[5]] over the array in
     * {@code word}.
     */
    public List<Step> sqrtDecomp2(String word, List<Integer> values,
                                  Integer index) {
        List<Step> steps = new ArrayList<>();
        int[] parsed = parseArray(word);
        if (parsed == null || parsed.length == 0) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The array is needed as "
                            + "comma-separated numbers.", LINE_FINAL));
            return steps;
        }
        if (values == null || values.size() != 6) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The update range and the two "
                            + "query ranges are needed as "
                            + "[ul, ur, q1l, q1r, q2l, q2r].",
                    LINE_FINAL));
            return steps;
        }
        int ul = values.get(0);
        int ur = values.get(1);
        int q1l = values.get(2);
        int q1r = values.get(3);
        int q2l = values.get(4);
        int q2r = values.get(5);
        if (!inRange(ul, ur, parsed.length)
                || !inRange(q1l, q1r, parsed.length)
                || !inRange(q2l, q2r, parsed.length)) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A range is out of bounds.",
                    LINE_FINAL));
            return steps;
        }
        if (index == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The add amount is needed.",
                    LINE_FINAL));
            return steps;
        }
        Blocks blocks = new Blocks(parsed);
        steps.add(new Step(blocks.minList(), List.of(), List.of(),
                blocks.lazyList(),
                "Built " + blocks.blockCount() + " blocks of size "
                        + blocks.blockSize + ": block mins "
                        + blocks.minList() + ".",
                LINE_BUILD, blocks.snapshot(), null));

        int bl = ul / blocks.blockSize;
        int br = ur / blocks.blockSize;
        for (int b = bl; b <= br; b++) {
            int from = Math.max(ul, blocks.blockStart(b));
            int to = Math.min(ur, blocks.blockEnd(b));
            List<Integer> highlight = new ArrayList<>();
            String msg;
            // Only the blocks strictly between the first and
            // last touched blocks are fully covered (the
            // standard sqrt-decomposition convention, as in
            // the sim): the first and last blocks are always
            // updated element by element, even when the range
            // happens to cover every element of the last
            // block (block 2 in the demo: [1,7] covers 6..7,
            // yet it is a partial update, so no lazy tag).
            if (b > bl && b < br) {
                int beforeMin = blocks.blockMin(b);
                blocks.lazy[b] += index;
                for (int i = from; i <= to; i++) {
                    highlight.add(i);
                }
                highlight.add(b + 100);
                msg = "Block " + b + " fully covered: lazy[" + b
                        + "] += " + index + " (tag " + blocks.lazy[b]
                        + ") — block min " + beforeMin + " -> "
                        + blocks.blockMin(b) + ".";
            } else {
                int beforeMin = blocks.blockMin(b);
                blocks.push(b);
                for (int i = from; i <= to; i++) {
                    blocks.stored[i] += index;
                    highlight.add(i);
                }
                highlight.add(b + 100);
                msg = "Block " + b + " partial: a[" + from + ".."
                        + to + "] += " + index + " — block min "
                        + beforeMin + " -> " + blocks.blockMin(b)
                        + ".";
            }
            steps.add(new Step(blocks.minList(),
                    highlight, List.of(), blocks.lazyList(),
                    msg, LINE_UPDATE, blocks.snapshot(), null));
        }

        int answer1 = rangeMin(blocks, q1l, q1r);
        steps.add(new Step(List.of(answer1),
                queryHighlight(blocks, q1l, q1r), List.of(),
                blocks.lazyList(),
                "Query min[" + q1l + "," + q1r + "] = " + answer1
                        + ".",
                LINE_QUERY, blocks.snapshot(), null));
        int answer2 = rangeMin(blocks, q2l, q2r);
        steps.add(new Step(List.of(answer2),
                queryHighlight(blocks, q2l, q2r), List.of(),
                blocks.lazyList(),
                "Query min[" + q2l + "," + q2r + "] = " + answer2
                        + ".",
                LINE_QUERY, blocks.snapshot(), null));
        steps.add(new Step(blocks.minList(), List.of(), List.of(),
                blocks.lazyList(),
                "Sqrt decomposition v2 done: min[" + q1l + "," + q1r
                        + "]=" + answer1 + ", min[" + q2l + "," + q2r
                        + "]=" + answer2 + "; block mins "
                        + blocks.minList() + ", lazy tags "
                        + blocks.lazyList() + ".",
                LINE_FINAL, blocks.snapshot(), null));
        return steps;
    }

    /** Minimum true value over [l, r]. */
    private int rangeMin(Blocks blocks, int l, int r) {
        int min = Integer.MAX_VALUE;
        int bl = l / blocks.blockSize;
        int br = r / blocks.blockSize;
        if (bl == br) {
            for (int i = l; i <= r; i++) {
                min = Math.min(min, blocks.trueValue(i));
            }
            return min;
        }
        for (int i = l; i <= blocks.blockEnd(bl); i++) {
            min = Math.min(min, blocks.trueValue(i));
        }
        for (int b = bl + 1; b <= br - 1; b++) {
            min = Math.min(min, blocks.blockMin(b));
        }
        for (int i = blocks.blockStart(br); i <= r; i++) {
            min = Math.min(min, blocks.trueValue(i));
        }
        return min;
    }

    /** Highlight for a query step: partial-block elements + fully covered blocks. */
    private List<Integer> queryHighlight(Blocks blocks, int l, int r) {
        List<Integer> highlight = new ArrayList<>();
        int bl = l / blocks.blockSize;
        int br = r / blocks.blockSize;
        if (bl == br) {
            for (int i = l; i <= r; i++) {
                highlight.add(i);
            }
            return highlight;
        }
        for (int i = l; i <= blocks.blockEnd(bl); i++) {
            highlight.add(i);
        }
        for (int b = bl + 1; b <= br - 1; b++) {
            highlight.add(b + 100);
        }
        for (int i = blocks.blockStart(br); i <= r; i++) {
            highlight.add(i);
        }
        return highlight;
    }

    private boolean inRange(int l, int r, int n) {
        return l >= 0 && r < n && l <= r;
    }

    /** Parses "3,1,4,1,5,9,2,6" into an int array, or null. */
    private int[] parseArray(String word) {
        if (word == null || word.isBlank()) {
            return null;
        }
        String[] parts = word.trim().split(",", -1);
        int[] result = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                result[i] = Integer.parseInt(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return result;
    }
}
