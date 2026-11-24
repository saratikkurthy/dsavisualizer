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
 * Suffix automaton (Phase 26, string algorithms v2): the
 * minimal DFA of all substrings of a word — one state per
 * endpos class, one transition per character. The idea
 * behind "does this pattern occur?" and "how many distinct
 * substrings?" in O(|pattern|) / O(1) per state.
 *
 * <p>Demo string "baaba" — chosen because it builds with
 * exactly ONE clone (the teaching moment). States (id: len,
 * link, next): 0: 0, &minus;1, {b:1, a:4}; 1: 1, 0, {a:2};
 * 2: 2, 4, {a:3}; 3: 3, 4, {b:5}; 4: 1, 0, {a:3, b:5} (the
 * clone, of q=2, at the 3rd character 'a'); 5: 4, 1, {a:6};
 * 6: 5, 2, {}. Longest strings: 0:"", 1:"b", 2:"ba",
 * 3:"baa", 4:"a", 5:"baab", 6:"baaba". Distinct substrings
 * 11 = sum(len[v] &minus; len[link[v]]) over v &gt; 0.
 * Trace: 5 extension steps (line 2, one per character) +
 * final (line 0) = 6 steps. Extension step: {@code data} =
 * [state count so far], {@code highlight} = [the new state
 * id] (or [the new state id, the clone id] when a clone is
 * created — the 3rd step highlights [3, 4]), {@code
 * sorted} = [the last state id]. Final step {@code data} =
 * [11], {@code sorted} = [6]. {@code nodes} / {@code edges}
 * = all states / transitions + suffix links created so far.
 * Node {@code value} = the state's {@code len}, {@code
 * label} = its longest string ("" for the root).
 * {@code edges} are ordered transitions first (state id
 * order, insertion order within a state — the maps are
 * LinkedHashMaps), then suffix links (state id order; the
 * root's &minus;1 link is no edge). A clone's longest
 * string is the suffix of q's longest string of length
 * len[clone] (the clone keeps q's short substrings).</p>
 *
 * <p>Bad input (a missing/blank word) yields a single
 * "empty" Step; the controller 400s on it (see
 * VisualizationController).</p>
 */
@Service
public class SamService {

    private static final int LINE_EXTEND = 2;
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

    /** Builds the suffix automaton of {@code word}, one extension step per character. */
    public List<Step> sam(String word) {
        List<Step> steps = new ArrayList<>();
        if (word == null || word.isBlank()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "A word is needed.", LINE_FINAL));
            return steps;
        }
        Automaton automaton = new Automaton();
        for (int i = 0; i < word.length(); i++) {
            int clone = automaton.extend(word.charAt(i),
                    word.substring(0, i + 1));
            int cur = automaton.last;
            List<Integer> highlight = clone == -1
                    ? List.of(cur)
                    : List.of(cur, clone);
            steps.add(new Step(
                    List.of(automaton.len.size()),
                    highlight, List.of(), List.of(cur),
                    "Extend with '" + word.charAt(i) + "': state "
                            + cur + (clone == -1
                                    ? " created."
                                    : " created; state "
                                            + automaton.lastCloneSource
                                            + " cloned as state "
                                            + clone + ".")
                            + " " + automaton.len.size()
                            + " states so far.",
                    LINE_EXTEND, automaton.snapshot(),
                    automaton.edgeSnapshot()));
        }
        int distinct = 0;
        for (int v = 1; v < automaton.len.size(); v++) {
            distinct += automaton.len.get(v)
                    - automaton.len.get(automaton.link.get(v));
        }
        steps.add(new Step(
                List.of(distinct), List.of(), List.of(),
                List.of(automaton.last),
                "Suffix automaton done: "
                        + automaton.len.size() + " states, "
                        + distinct + " distinct substrings.",
                LINE_FINAL, automaton.snapshot(),
                automaton.edgeSnapshot()));
        return steps;
    }
}
