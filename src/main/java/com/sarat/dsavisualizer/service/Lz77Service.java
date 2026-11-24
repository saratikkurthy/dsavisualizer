package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * LZ77 compression (Phase 28, string matching v4): the
 * sliding-window scheme behind DEFLATE/gzip. The text is
 * scanned left to right; at each position the longest
 * match in the text already seen (the search buffer) is
 * emitted as a token (offset, length, next char) — the
 * offset counts back from the current position, the
 * length is how many characters repeat, and the next
 * char is the first character after the match (or END
 * when the match runs to the end of the text). A
 * character with no match is a literal token (0, 0,
 * char). Matches may OVERLAP the current position
 * (the match keeps reading characters that were just
 * copied), which is how "aaaa..." compresses to a
 * single token. Ties between equal-length matches go
 * to the SMALLEST offset (the nearest copy). Decoding
 * is the exact reverse: copy {@code length} characters
 * from {@code offset} back, then append {@code next}.
 *
 * <p>Demo "abracadabra": tokens (0,0,'a'), (0,0,'b'),
 * (0,0,'r'), (3,1,'c'), (2,1,'d'), (7,4,END) — 6 tokens
 * for 11 characters; decoding them gives back
 * "abracadabra". Trace: 6 token steps (line 2) + final
 * (line 0) = 7 steps. {@code nodes} = one NodeDto per
 * input character: id = index, {@code value} = index,
 * {@code label} = the character. No {@code edges}
 * (null). Token steps: {@code highlight} = the token's
 * positions (the matched source range plus the literal
 * char position, in index order); {@code data} =
 * [offset, length, nextCharCode] where nextCharCode is
 * the char's code (97 = 'a') or 0 for END. Pinned
 * data: [0,0,97], [0,0,98], [0,0,114], [3,1,99],
 * [2,1,100], [7,4,0]. The visualizer accumulates the
 * token table from each step's data. Final step:
 * {@code data} = [6] (token count), {@code highlight}
 * = [].</p>
 *
 * <p>Bad input (a missing/blank word, or one that is not
 * lowercase a-z) yields a single "empty" Step; the
 * controller 400s on it (see VisualizationController).</p>
 */
@Service
public class Lz77Service {

    private static final int LINE_TOKEN = 2;
    private static final int LINE_FINAL = 0;

    /** LZ77 tokens of {@code word}, one step per token. */
    public List<Step> lz77(String word) {
        List<Step> steps = new ArrayList<>();
        if (word == null || word.isBlank()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A word is needed for LZ77 "
                            + "compression.", LINE_FINAL));
            return steps;
        }
        String text = word.trim();
        for (char c : text.toCharArray()) {
            if (c < 'a' || c > 'z') {
                steps.add(new Step(List.of(), List.of(), List.of(),
                        List.of(), "LZ77 needs lowercase a-z "
                                + "only.", LINE_FINAL));
                return steps;
            }
        }
        int n = text.length();
        List<NodeDto> nodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            NodeDto dto = new NodeDto(i, i, 0, 0);
            dto.setLabel(String.valueOf(text.charAt(i)));
            nodes.add(dto);
        }

        int tokenCount = 0;
        int i = 0;
        while (i < n) {
            // Longest match in the text before i; ties keep
            // the SMALLEST offset (off grows from 1, and
            // only a strictly longer match replaces it).
            int bestLen = 0;
            int bestOff = 0;
            for (int off = 1; off <= i; off++) {
                int len = 0;
                while (i + len < n
                        && text.charAt(i + len)
                                == text.charAt(i - off + len)) {
                    len++;
                }
                if (len > bestLen) {
                    bestLen = len;
                    bestOff = off;
                }
            }
            List<Integer> highlight = new ArrayList<>();
            List<Integer> data;
            String msg;
            if (bestLen == 0) {
                highlight.add(i);
                data = List.of(0, 0, (int) text.charAt(i));
                msg = "Literal '" + text.charAt(i) + "' at " + i
                        + ": no match in the text so far — token "
                        + "(0, 0, '" + text.charAt(i) + "').";
                i++;
            } else {
                for (int k = i - bestOff; k < i - bestOff + bestLen; k++) {
                    highlight.add(k);
                }
                int nextPos = i + bestLen;
                if (nextPos < n) {
                    highlight.add(nextPos);
                    data = List.of(bestOff, bestLen,
                            (int) text.charAt(nextPos));
                    msg = "Match at " + i + ": '" + text.substring(i, nextPos)
                            + "' repeats the text " + bestOff
                            + " back, length " + bestLen + ", next '"
                            + text.charAt(nextPos) + "' — token ("
                            + bestOff + ", " + bestLen + ", '"
                            + text.charAt(nextPos) + "').";
                    i = nextPos + 1;
                } else {
                    data = List.of(bestOff, bestLen, 0);
                    msg = "Match at " + i + ": '" + text.substring(i, n)
                            + "' repeats the text " + bestOff
                            + " back, length " + bestLen + ", and runs "
                            + "to the end — token (" + bestOff + ", "
                            + bestLen + ", END).";
                    i = n;
                }
            }
            tokenCount++;
            steps.add(new Step(data, highlight, List.of(), List.of(),
                    msg, LINE_TOKEN, nodes, null));
        }
        steps.add(new Step(List.of(tokenCount), List.of(), List.of(),
                List.of(),
                "LZ77 done: " + tokenCount + " tokens for "
                        + n + " characters.",
                LINE_FINAL, nodes, null));
        return steps;
    }
}
