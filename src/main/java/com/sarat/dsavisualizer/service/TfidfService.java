package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TF-IDF ranking (Phase 24, search &amp; ranking): the
 * classic relevance score — a term matters more when it
 * is frequent in a document (tf) and rare across the
 * corpus (idf). Rank the documents by their summed score
 * for the query terms and the most relevant document
 * comes first.
 *
 * <p>Steps: one term step per query term (line 2, in query
 * order) + the final Step (line 0). Per term step:
 * {@code data} = the cumulative scores in MILLIS for all
 * docs in doc order (rounded); {@code highlight} = [docs
 * with score &gt; 0 so far]; {@code nodes} = the docs
 * (input order, labelled D1..D6, x/y = 0 — layout is the
 * frontend's; {@code value} = 1 if scored else 0);
 * {@code sorted} = the ranking so far (doc indices, best
 * first, ties by doc id). tf = count / doc length;
 * idf = ln(N/df), N = the doc count. Demo corpus (the
 * inverted-index corpus, 6 docs), demo query (the default
 * {@code word}): "binary tree". Scores: D1 0.5973 (binary
 * 1/3&times;ln3 + tree 1/3&times;ln2), D2 0.4479 (binary
 * 1/4&times;ln3 + tree 1/4&times;ln2), D5 0.1733 (tree
 * 1/4&times;ln2), D3/D4/D6 0. Ranking: D1 &gt; D2 &gt; D5
 * &gt; D3 = D4 = D6 (ties by doc id). After "binary":
 * [366, 275, 0, 0, 0, 0]; after "tree":
 * [597, 448, 0, 0, 173, 0]. 2 term steps + final =
 * 3 steps; final {@code data} = [597, 448, 0, 0, 173, 0],
 * {@code sorted} = [0, 1, 4, 2, 3, 5]. Second query for
 * the tryIt: "graph traversal" &rarr; D6 0.4479, D3 0.3662,
 * D2 = D5 0.1733, D1 = D4 0.</p>
 *
 * <p>The controller validates the input (words missing or
 * empty &rarr; 400); a missing/blank query defaults to
 * "binary tree" here. This method guards against bad input
 * anyway with a single "empty" Step.</p>
 */
@Service
public class TfidfService {

    private static final int LINE_TERM = 2;
    private static final int LINE_FINAL = 0;

    /** The demo query, used when the word is missing/blank. */
    static final String DEFAULT_QUERY = "binary tree";

    /**
     * Ranks {@code words} (the doc texts) for {@code word}
     * (the query).
     */
    public List<Step> tfidf(List<String> words, String word) {
        List<Step> steps = new ArrayList<>();
        if (words == null || words.isEmpty()) {
            steps.add(new Step(List.of(), List.of(), List.of(),
                    List.of(), "Documents are needed (words).",
                    LINE_FINAL));
            return steps;
        }
        String query = word == null || word.isBlank()
                ? DEFAULT_QUERY : word;
        List<List<String>> docs = new ArrayList<>();
        for (String text : words) {
            docs.add(InvertedIndexService.tokenize(text));
        }
        int n = docs.size();

        // Document frequency per term.
        Map<String, Integer> df = new HashMap<>();
        for (List<String> doc : docs) {
            for (String term : new java.util.HashSet<>(doc)) {
                df.merge(term, 1, Integer::sum);
            }
        }

        double[] scores = new double[n];
        for (String term : InvertedIndexService.tokenize(query)) {
            Integer docFreq = df.get(term);
            // A term in no document adds nothing — but it still
            // gets its term step (one step per query term).
            if (docFreq != null) {
                double idf = Math.log((double) n / docFreq);
                for (int d = 0; d < n; d++) {
                    int count = 0;
                    for (String token : docs.get(d)) {
                        if (token.equals(term)) {
                            count++;
                        }
                    }
                    if (count > 0) {
                        scores[d] += (double) count / docs.get(d).size()
                                * idf;
                    }
                }
            }
            List<Integer> millis = millis(scores);
            List<Integer> highlight = new ArrayList<>();
            for (int d = 0; d < n; d++) {
                if (scores[d] > 0) {
                    highlight.add(d);
                }
            }
            steps.add(new Step(millis, highlight, List.of(),
                    ranking(scores),
                    "After '" + term + "': "
                            + scoresText(scores, millis) + ".",
                    LINE_TERM, snapshotNodes(n, scores), List.of()));
        }
        steps.add(new Step(millis(scores), rankingHighlight(scores),
                List.of(), ranking(scores),
                "Query '" + query + "': "
                        + scoresText(scores, millis(scores))
                        + ". Ranking: " + rankingText(scores) + ".",
                LINE_FINAL, snapshotNodes(n, scores), List.of()));
        return steps;
    }

    /** The scores in MILLIS (rounded), in doc order. */
    private List<Integer> millis(double[] scores) {
        List<Integer> millis = new ArrayList<>();
        for (double score : scores) {
            millis.add((int) Math.round(score * 1000));
        }
        return millis;
    }

    /** Doc indices with score > 0 (the final step's highlight). */
    private List<Integer> rankingHighlight(double[] scores) {
        List<Integer> highlight = new ArrayList<>();
        for (int d = 0; d < scores.length; d++) {
            if (scores[d] > 0) {
                highlight.add(d);
            }
        }
        return highlight;
    }

    /** Doc indices best first (exact scores, ties by doc id). */
    private List<Integer> ranking(double[] scores) {
        List<Integer> order = new ArrayList<>();
        for (int d = 0; d < scores.length; d++) {
            order.add(d);
        }
        order.sort((a, b) -> {
            int byScore = Double.compare(scores[b], scores[a]);
            return byScore != 0 ? byScore : Integer.compare(a, b);
        });
        return order;
    }

    /** "D1 0.597, D2 0.448, D5 0.173" for the scored docs. */
    private String scoresText(double[] scores, List<Integer> millis) {
        List<String> parts = new ArrayList<>();
        for (int d = 0; d < scores.length; d++) {
            if (scores[d] > 0) {
                parts.add(InvertedIndexService.docName(d) + " "
                        + String.format(java.util.Locale.ROOT, "%.3f",
                                millis.get(d) / 1000.0));
            }
        }
        return parts.isEmpty() ? "no matches"
                : String.join(", ", parts);
    }

    /** "D1 > D2 > D5 > D3 = D4 = D6" for the ranking. */
    private String rankingText(double[] scores) {
        List<Integer> order = ranking(scores);
        List<String> groups = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        double prev = Double.NaN;
        for (int d : order) {
            if (!Double.isNaN(prev)
                    && Math.abs(scores[d] - prev) < 1e-12) {
                current.append(" = ")
                        .append(InvertedIndexService.docName(d));
            } else {
                if (current.length() > 0) {
                    groups.add(current.toString());
                }
                current = new StringBuilder(
                        InvertedIndexService.docName(d));
            }
            prev = scores[d];
        }
        if (current.length() > 0) {
            groups.add(current.toString());
        }
        return String.join(" > ", groups);
    }

    /** One NodeDto per doc (id = doc index, label = D1.., value 1 if scored). */
    private List<NodeDto> snapshotNodes(int docCount, double[] scores) {
        List<NodeDto> dtos = new ArrayList<>();
        for (int i = 0; i < docCount; i++) {
            NodeDto dto = new NodeDto(i, scores[i] > 0 ? 1 : 0, 0, 0);
            dto.setLabel(InvertedIndexService.docName(i));
            dtos.add(dto);
        }
        return dtos;
    }
}
