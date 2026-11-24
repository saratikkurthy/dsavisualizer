package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bitap / Shift-Or (Phase 28, string matching v4):
 * exact string matching with bit parallelism — the
 * whole pattern is simulated in one machine word. For
 * a pattern of length m, each character c has a mask
 * of m bits where bit i is 0 when pattern[i] == c and
 * 1 otherwise (bit i = pattern position i, LSB =
 * position 0). The state S starts at all 1s; for each
 * text character, S = ((S &lt;&lt; 1) | mask[c]) &amp;
 * ((1 &lt;&lt; m) - 1). A 0 bit in S means "the prefix
 * ending here matches so far"; when bit m-1 becomes 0
 * the full pattern ends at this position. One step per
 * text character, O(n) time with O(1) work per
 * character (for m &le; the word size — here m &le; 8,
 * far inside an int).
 *
 * <p>Demo text "xabcy", pattern "abc" (m = 3). Masks:
 * a = 110, b = 101, c = 011, any other character =
 * 111. S per character: x = 111, a = 110, b = 101,
 * c = 011 (MATCH), y = 111. The match "abc" ends at
 * index 3, so it starts at [1, 3]. Trace: 1 build
 * step (line 2) + 5 char steps (line 3) = 6 steps (no
 * separate final step — the last char step closes the
 * trace). {@code nodes} = one NodeDto per TEXT
 * character: id = index, {@code value} = index,
 * {@code label} = the character. No {@code edges}
 * (null). Build step: {@code data} = [maskA, maskB,
 * maskC] = [6, 5, 3] (3-bit masks, LSB = pattern
 * position 0), {@code highlight} = []. Char steps:
 * {@code highlight} = [text index], {@code data} =
 * [S] as an integer (7, 6, 5, 3, 7); the visualizer
 * renders the 3-bit binary from data[0]. Match step
 * (index 3): {@code sorted} = [1] (match start);
 * other steps {@code sorted} = [].</p>
 *
 * <p>Bad input (a missing/blank text or pattern, or
 * one that is not lowercase a-z) yields a single
 * "empty" Step; the controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class BitapService {

    private static final int LINE_BUILD = 2;
    private static final int LINE_CHAR = 3;

    /** Shift-Or scan of {@code word} for pattern {@code words.get(0)}. */
    public List<Step> bitap(String word, List<String> words) {
        List<Step> steps = new ArrayList<>();
        if (word == null || word.isBlank()
                || words == null || words.isEmpty()
                || words.get(0) == null || words.get(0).isBlank()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Bitap needs a text and a "
                            + "pattern.", 0));
            return steps;
        }
        String text = word.trim();
        String pattern = words.get(0).trim();
        for (char c : text.toCharArray()) {
            if (c < 'a' || c > 'z') {
                steps.add(new Step(List.of(), List.of(), List.of(),
                        List.of(), "Bitap needs lowercase a-z "
                                + "only.", 0));
                return steps;
            }
        }
        for (char c : pattern.toCharArray()) {
            if (c < 'a' || c > 'z') {
                steps.add(new Step(List.of(), List.of(), List.of(),
                        List.of(), "Bitap needs the pattern in "
                                + "lowercase a-z only.", 0));
                return steps;
            }
        }
        int m = pattern.length();
        int allOnes = (1 << m) - 1;

        // One mask per distinct pattern character, in
        // first-occurrence order; bit i is 0 exactly
        // when pattern[i] == the character.
        Map<Character, Integer> masks = new LinkedHashMap<>();
        for (int i = 0; i < m; i++) {
            char c = pattern.charAt(i);
            masks.putIfAbsent(c, allOnes);
        }
        for (Map.Entry<Character, Integer> e : masks.entrySet()) {
            int mask = 0;
            for (int i = 0; i < m; i++) {
                if (pattern.charAt(i) != e.getKey()) {
                    mask |= (1 << i);
                }
            }
            e.setValue(mask);
        }

        List<NodeDto> nodes = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            NodeDto dto = new NodeDto(i, i, 0, 0);
            dto.setLabel(String.valueOf(text.charAt(i)));
            nodes.add(dto);
        }

        List<Integer> buildData = new ArrayList<>(masks.values());
        StringBuilder maskMsg = new StringBuilder();
        for (Map.Entry<Character, Integer> e : masks.entrySet()) {
            if (maskMsg.length() > 0) {
                maskMsg.append(", ");
            }
            maskMsg.append(e.getKey()).append("=")
                    .append(toBinary(e.getValue(), m));
        }
        steps.add(new Step(buildData, List.of(), List.of(), List.of(),
                "Built Shift-Or masks for pattern '" + pattern
                        + "': " + maskMsg + " (bit i is 0 when "
                        + "pattern[i] matches).",
                LINE_BUILD, nodes, null));

        int state = allOnes;
        for (int idx = 0; idx < text.length(); idx++) {
            char c = text.charAt(idx);
            int mask = masks.getOrDefault(c, allOnes);
            state = ((state << 1) | mask) & allOnes;
            boolean match = ((state >> (m - 1)) & 1) == 0;
            List<Integer> sorted = match
                    ? List.of(idx - m + 1) : List.of();
            String msg = "Read '" + c + "' at " + idx + ": S = "
                    + toBinary(state, m)
                    + (match
                            ? " — match! '" + pattern + "' found at ["
                                    + (idx - m + 1) + ", " + idx + "]."
                            : " — no match yet.");
            steps.add(new Step(List.of(state), List.of(idx),
                    List.of(), sorted, msg, LINE_CHAR, nodes, null));
        }
        return steps;
    }

    /** m-bit binary string of {@code value} (MSB first). */
    private String toBinary(int value, int m) {
        StringBuilder sb = new StringBuilder();
        for (int i = m - 1; i >= 0; i--) {
            sb.append(((value >> i) & 1) == 1 ? '1' : '0');
        }
        return sb.toString();
    }
}
