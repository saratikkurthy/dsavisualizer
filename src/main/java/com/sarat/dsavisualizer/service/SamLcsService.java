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
 * Longest common substring via the suffix automaton
 * (Phase 27, string matching v3): build the SAM of S1,
 * then walk S2 through it — the walk stays inside the
 * automaton exactly while the current suffix of S2 is a
 * substring of S1, so the longest match is the LCS, in
 * O(|S1| + |S2|). The idea behind the SAM's substring
 * queries and the "walk the text through the automaton"
 * pattern.
 *
 * <p>Demo S1 "ababc", S2 "babc". SAM of S1 (id: len,
 * link, next): 0: 0, &minus;1, {a:1, b:2, c:5}; 1: 1, 0,
 * {b:2}; 2: 2, 0, {a:3, c:5}; 3: 3, 1, {b:4}; 4: 4, 2,
 * {c:5}; 5: 5, 0, {}. Six states, 0 clones (every
 * extension links straight to an existing state). Walk
 * of S2: 'b' &rarr; state 2 (l=1, best=1), 'a' &rarr;
 * state 3 (l=2, best=2), 'b' &rarr; state 4 (l=3,
 * best=3), 'c' &rarr; state 5 (l=4, best=4) — all four
 * characters match in sequence, so the LCS is "babc"
 * (length 4). Trace: 5 build steps (line 2, the SAM
 * extensions of S1) + 4 walk steps (line 3, one per
 * character of S2) + final (line 0) = 10 steps. Build
 * step: exactly the Phase 26 SAM shape — {@code data} =
 * [state count so far], {@code highlight} = [the new
 * state id] (or [new state, clone] when a clone is
 * created), {@code sorted} = [the last state id].
 * Walk step: {@code highlight} = [current state id],
 * {@code data} = [matched length l, best length so
 * far]. Final step {@code data} = [4, 4] (l=4, best=4).
 * Node {@code value} = the state's {@code len},
 * {@code label} = its longest string ("" for the root).
 * {@code edges} are ordered transitions first (state id
 * order, insertion order within a state — the maps are
 * LinkedHashMaps), then suffix links (state id order;
 * the root's &minus;1 link is no edge). A clone's
 * longest string is the suffix of q's longest string of
 * length len[clone] (the Phase 26 SAM rule).</p>
 *
 * <p>Bad input (a missing/blank S1 or S2) yields a
 * single "empty" Step; the controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class SamLcsService {

    private static final int LINE_BUILD = 2;
    private static final int LINE_WALK = 3;
    private static final int LINE_FINAL = 0;

    /** Per-call mutable state (the service bean is a singleton, so no instance state). */
    private static final class Automaton {
        final List<Integer> len = new ArrayList<>();
        final List<Integer> link = new ArrayList<>();
        final List<Map<Character, Integer>> next = new ArrayList<>();
        final List<String> longest = new ArrayList<>();
        int last = 0;
        int lastCloneSource = -1;

        Automaton() {
            len.add(0);
            link.add(-1);
            next.add(new LinkedHashMap<>());
            longest.add("");
        }

        /**
         * Extends the automaton with one character.
         * Returns the new state's id, or -1 when no clone was
         * created; the clone id (when created) is the last
         * state id after the call.
         */
        int extend(char c, String prefix) {
            int cur = len.size();
            len.add(len.get(last) + 1);
            link.add(0);
            next.add(new LinkedHashMap<>());
            longest.add(prefix);
            int p = last;
            while (p != -1 && !next.get(p).containsKey(c)) {
                next.get(p).put(c, cur);
                p = link.get(p);
            }
            if (p == -1) {
                link.set(cur, 0);
            } else {
                int q = next.get(p).get(c);
                if (len.get(p) + 1 == len.get(q)) {
                    link.set(cur, q);
                } else {
                    int clone = len.size();
                    lastCloneSource = q;
                    len.add(len.get(p) + 1);
                    link.add(link.get(q));
                    next.add(new LinkedHashMap<>(next.get(q)));
                    String qLongest = longest.get(q);
                    longest.add(qLongest.substring(
                            qLongest.length() - len.get(clone)));
                    while (p != -1 && next.get(p).get(c) == q) {
                        next.get(p).put(c, clone);
                        p = link.get(p);
                    }
                    link.set(q, clone);
                    link.set(cur, clone);
                    last = cur;
                    return clone;
                }
            }
            last = cur;
            return -1;
        }

        /** One NodeDto per state created so far, in id order. */
        List<NodeDto> snapshot() {
            List<NodeDto> dtos = new ArrayList<>();
            for (int i = 0; i < len.size(); i++) {
                NodeDto dto = new NodeDto(i, len.get(i), 0, 0);
                dto.setLabel(longest.get(i));
                dtos.add(dto);
            }
            return dtos;
        }

        /** Transitions first, then suffix links (see class Javadoc). */
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

    /**
     * Builds the SAM of {@code s1}, then walks {@code s2}
     * through it to find the longest common substring.
     */
    public List<Step> samLcs(String s1, String s2) {
        List<Step> steps = new ArrayList<>();
        if (s1 == null || s1.isBlank() || s2 == null || s2.isBlank()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Two non-empty strings are needed.",
                    LINE_FINAL));
            return steps;
        }
        Automaton automaton = new Automaton();
        for (int i = 0; i < s1.length(); i++) {
            int clone = automaton.extend(s1.charAt(i),
                    s1.substring(0, i + 1));
            int cur = automaton.last;
            List<Integer> highlight = clone == -1
                    ? List.of(cur)
                    : List.of(cur, clone);
            steps.add(new Step(
                    List.of(automaton.len.size()),
                    highlight, List.of(), List.of(cur),
                    "Extend with '" + s1.charAt(i) + "': state "
                            + cur + (clone == -1
                                    ? " created."
                                    : " created; state "
                                            + automaton.lastCloneSource
                                            + " cloned as state "
                                            + clone + ".")
                            + " " + automaton.len.size()
                            + " states so far.",
                    LINE_BUILD, automaton.snapshot(),
                    automaton.edgeSnapshot()));
        }
        int v = 0;
        int l = 0;
        int best = 0;
        int bestEnd = -1;
        for (int i = 0; i < s2.length(); i++) {
            char c = s2.charAt(i);
            String msg;
            if (automaton.next.get(v).containsKey(c)) {
                v = automaton.next.get(v).get(c);
                l++;
                msg = "'" + c + "' matches — state " + v
                        + " (matched length " + l + ")";
            } else {
                int fellBackFrom = v;
                while (v != -1
                        && !automaton.next.get(v).containsKey(c)) {
                    v = automaton.link.get(v);
                }
                if (v == -1) {
                    v = 0;
                    l = 0;
                    msg = "'" + c + "' matches nothing — back to "
                            + "the root (matched length 0)";
                } else {
                    l = automaton.len.get(v) + 1;
                    v = automaton.next.get(v).get(c);
                    msg = "'" + c + "' does not follow state "
                            + fellBackFrom + " — follow suffix links "
                            + "to state " + v + " (matched length "
                            + l + ")";
                }
            }
            if (l > best) {
                best = l;
                bestEnd = i;
            }
            msg += ". Best so far: " + best + ".";
            steps.add(new Step(List.of(l, best), List.of(v),
                    List.of(), List.of(), "Walk S2 position " + i
                            + ": " + msg,
                    LINE_WALK, automaton.snapshot(),
                    automaton.edgeSnapshot()));
        }
        String lcs = best == 0 ? ""
                : s2.substring(bestEnd - best + 1, bestEnd + 1);
        steps.add(new Step(List.of(best, best), List.of(), List.of(),
                List.of(),
                "Longest common substring '" + lcs + "' (length "
                        + best + ").",
                LINE_FINAL, automaton.snapshot(),
                automaton.edgeSnapshot()));
        return steps;
    }
}
