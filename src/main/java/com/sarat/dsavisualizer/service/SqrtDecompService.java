package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Sqrt decomposition (Phase 26, range queries v2): the
 * array is split into blocks of about sqrt(n); each block
 * keeps its sum, so a range sum touches at most two
 * partial blocks element-by-element and reads the whole
 * blocks in between in O(1) each — O(sqrt(n)) per query
 * and per point update. The idea behind Mo's algorithm
 * (Phase 26) and the sqrt-decomposition family.
 *
 * <p>Demo array [3, 1, 4, 1, 5, 9, 2, 6] (n=8, block size
 * 3 = ceil(sqrt(8))). Block sums: [8, 15, 8]. Demo query
 * [2, 6]: a[2]=4 + block 1=15 + a[6]=2 = 21. Demo update:
 * a[3] = 7 (was 1) &rarr; block sums [8, 21, 8]; the same
 * query [2, 6] then gives 27. Trace: 1 build step (line 1)
 * + 3 query steps (line 2: a[2], block 1, a[6]) + 1 update
 * step (line 3) + 3 query steps (line 2 again, post-update)
 * + final (line 0) = 9 steps. Build step: {@code data} =
 * the block sums [8, 15, 8]. Query step: {@code highlight}
 * = [the element index] for a single element or [the block
 * id + 100] for a whole block (element highlights are
 * indices 0..7, block highlights are 100, 101, 102, so
 * the two can never collide), {@code data} = [partial sum
 * so far], {@code sorted} = the partial-sum parts (the
 * summands) so far. Update step: {@code highlight} = [3],
 * {@code data} = [21] (the new block sum). Final step
 * {@code data} = [27], {@code sorted} = the last query's
 * parts [4, 21, 2]. {@code nodes} = one NodeDto per array
 * element, in index order ({@code value} = the element
 * value, {@code label} = the value as a string), showing
 * the current array at each step. No {@code edges}
 * (null).</p>
 *
 * <p>Bad input (an unparseable array, a query range that
 * is not [l, r] with 0 &le; l &le; r &lt; n, or a missing
 * / out-of-range update index or value) yields a single
 * "empty" Step; the controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class SqrtDecompService {

    private static final int LINE_BUILD = 1;
    private static final int LINE_QUERY = 2;
    private static final int LINE_UPDATE = 3;
    private static final int LINE_FINAL = 0;

    /** Per-call mutable state (the service bean is a singleton, so no instance state). */
    private static final class Blocks {
        final int[] array;
        final int blockSize;
        final int[] sums;

        Blocks(int[] array) {
            this.array = array;
            this.blockSize = (int) Math.ceil(Math.sqrt(array.length));
            int blockCount = (array.length + blockSize - 1) / blockSize;
            this.sums = new int[blockCount];
            for (int i = 0; i < array.length; i++) {
                sums[i / blockSize] += array[i];
            }
        }

        /** One NodeDto per array element, in index order. */
        List<NodeDto> snapshot() {
            List<NodeDto> dtos = new ArrayList<>();
            for (int i = 0; i < array.length; i++) {
                NodeDto dto = new NodeDto(i, array[i], 0, 0);
                dto.setLabel(String.valueOf(array[i]));
                dtos.add(dto);
            }
            return dtos;
        }

        /** The block sums as a list. */
        List<Integer> sumList() {
            List<Integer> list = new ArrayList<>();
            for (int sum : sums) {
                list.add(sum);
            }
            return list;
        }
    }

    /**
     * Runs query [l, r] (from {@code values}), then the
     * point update a[index] = capacity, then the same query
     * again, over the array in {@code word}.
     */
    public List<Step> sqrtDecomp(String word, List<Integer> values,
                                 Integer index, Integer capacity) {
        List<Step> steps = new ArrayList<>();
        int[] parsed = parseArray(word);
        if (parsed == null || parsed.length == 0) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The array is needed as "
                            + "comma-separated numbers.", LINE_FINAL));
            return steps;
        }
        if (values == null || values.size() != 2) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The query range is needed as "
                            + "[l, r].", LINE_FINAL));
            return steps;
        }
        int left = values.get(0);
        int right = values.get(1);
        if (left < 0 || right >= parsed.length || left > right) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Query range [" + left + ", "
                            + right + "] is out of range.",
                    LINE_FINAL));
            return steps;
        }
        if (index == null || index < 0 || index >= parsed.length
                || capacity == null) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "The update needs an index in "
                            + "range and a value.", LINE_FINAL));
            return steps;
        }
        Blocks blocks = new Blocks(parsed);
        steps.add(new Step(blocks.sumList(), List.of(), List.of(),
                List.of(),
                "Built " + blocks.sums.length + " blocks of size "
                        + blocks.blockSize + ": sums "
                        + blocks.sumList() + ".",
                LINE_BUILD, blocks.snapshot(), null));

        querySteps(steps, blocks, left, right);
        int oldValue = parsed[index];
        parsed[index] = capacity;
        blocks.sums[index / blocks.blockSize] += capacity - oldValue;
        steps.add(new Step(List.of(blocks.sums[index / blocks.blockSize]),
                List.of(index), List.of(), List.of(),
                "Update: a[" + index + "] becomes " + capacity
                        + " (was " + oldValue + ") — block "
                        + (index / blocks.blockSize) + " sum is now "
                        + blocks.sums[index / blocks.blockSize] + ".",
                LINE_UPDATE, blocks.snapshot(), null));
        List<Integer> newParts = querySteps(steps, blocks, left, right);
        steps.add(new Step(List.of(newParts.stream()
                        .mapToInt(Integer::intValue).sum()),
                List.of(), List.of(), new ArrayList<>(newParts),
                "Sqrt decomposition done: query [" + left + ", "
                        + right + "] = "
                        + newParts.stream()
                                .mapToInt(Integer::intValue).sum()
                        + " after the update.",
                LINE_FINAL, blocks.snapshot(), null));
        return steps;
    }

    /**
     * Emits one step per piece of the range sum (left
     * partial elements, whole blocks, right partial
     * elements) and returns the parts (the summands).
     */
    private List<Integer> querySteps(List<Step> steps, Blocks blocks,
                                     int left, int right) {
        List<Integer> parts = new ArrayList<>();
        int sum = 0;
        int i = left;
        while (i <= right) {
            int block = i / blocks.blockSize;
            int blockStart = block * blocks.blockSize;
            int blockEnd = Math.min(blockStart + blocks.blockSize,
                    blocks.array.length) - 1;
            if (i == blockStart && blockEnd <= right) {
                parts.add(blocks.sums[block]);
                sum += blocks.sums[block];
                steps.add(new Step(List.of(sum), List.of(block + 100),
                        List.of(), new ArrayList<>(parts),
                        "Block " + block + " = " + blocks.sums[block]
                                + " — partial sum " + sum + ".",
                        LINE_QUERY, blocks.snapshot(), null));
                i = blockEnd + 1;
            } else {
                parts.add(blocks.array[i]);
                sum += blocks.array[i];
                steps.add(new Step(List.of(sum), List.of(i),
                        List.of(), new ArrayList<>(parts),
                        "a[" + i + "] = " + blocks.array[i]
                                + " — partial sum " + sum + ".",
                        LINE_QUERY, blocks.snapshot(), null));
                i++;
            }
        }
        return parts;
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
