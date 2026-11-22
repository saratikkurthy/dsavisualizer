package com.sarat.dsavisualizer.service;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Inverted index (Phase 24, search &amp; ranking): the
 * data structure behind every search engine — a map from
 * each word to the documents that contain it. Building it
 * is one pass over the corpus; answering a multi-word
 * query is an AND (intersection) of the posting lists.
 *
 * <p>Steps: one build step per doc (line 2, in input
 * order) + one query step per query term (line 3, in
 * query order) + the final Step (line 0).
 * {@code step.nodes} are the documents (input order,
 * labelled D1..D6, x/y = 0 — layout is the frontend's).
 * Build step: {@code highlight} = [doc index],
 * {@code data} = [doc index], a node's {@code value} is 1
 * if the doc is already indexed else 0. Query step:
 * {@code highlight} = the term's posting list (doc
 * indices), {@code data} = the posting list, a node's
 * {@code value} is 1 if the doc is in this posting list
 * else 0. Final step: {@code data} = the AND result,
 * {@code highlight} = the AND result. Demo corpus (6
 * docs): D1 "binary tree search", D2 "binary search tree
 * traversal", D3 "graph shortest path", D4 "hash table
 * lookup", D5 "tree traversal preorder inorder", D6 "graph
 * traversal breadth depth". Demo query (the default
 * {@code word}): "tree traversal" (AND) &rarr; [D2, D5].
 * 6 build + 2 query + final = 9 steps; final
 * {@code data} = [1, 4].</p>
 *
 * <p>The controller validates the input (words missing or
 * empty &rarr; 400); a missing/blank query defaults to
 * "tree traversal" here. This method guards against bad
 * input anyway with a single "empty" Step.</p>
 */
@Service
public class InvertedIndexService {

    private static final int LINE_BUILD = 2;
    private static final int LINE_QUERY = 3;
    private static final int LINE_FINAL = 0;

    /** The demo query, used when the word is missing/blank. */
    static final String DEFAULT_QUERY = "tree traversal";

    /**
     * Builds the index over {@code words} (the doc texts)
     * and answers {@code word} (the query, AND).
     */
    public List<Step> invertedIndex(List<String> words, String word) {
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
            docs.add(tokenize(text));
        }

        // Build: term -> sorted doc indices (a doc lists once
        // per term, however often the term appears in it).
        Map<String, TreeSet<Integer>> index = new HashMap<>();
        for (int d = 0; d < docs.size(); d++) {
            for (String term : new TreeSet<>(docs.get(d))) {
                index.computeIfAbsent(term, k -> new TreeSet<>()).add(d);
            }
            List<String> terms = new ArrayList<>(
                    new TreeSet<>(docs.get(d)));
            steps.add(new Step(List.of(d), List.of(d), List.of(),
                    List.of(),
                    "Indexed " + docName(d) + ": "
                            + String.join(", ", terms) + ".",
                    LINE_BUILD, snapshotNodes(docs.size(), d, null),
                    List.of()));
        }

        // Query: one step per term, then the AND of the lists.
        List<String> queryTerms = tokenize(query);
        List<Integer> result = null;
        for (String term : queryTerms) {
            List<Integer> posting = new ArrayList<>(
                    index.getOrDefault(term, new TreeSet<>()));
            if (result == null) {
                result = new ArrayList<>(posting);
            } else {
                result.retainAll(posting);
            }
            steps.add(new Step(new ArrayList<>(posting),
                    new ArrayList<>(posting), List.of(), List.of(),
                    "Query term '" + term + "': posting list "
                            + docNames(posting) + ".",
                    LINE_QUERY,
                    snapshotNodes(docs.size(), -1, posting),
                    List.of()));
        }
        if (result == null) {
            result = List.of();
        }
        steps.add(new Step(new ArrayList<>(result),
                new ArrayList<>(result), List.of(), List.of(),
                result.isEmpty()
                        ? "Query '" + query + "' (AND): no matches."
                        : "Query '" + query + "' (AND): "
                                + docNames(result) + ".",
                LINE_FINAL, snapshotNodes(docs.size(), -1, result),
                List.of()));
        return steps;
    }

    /**
     * One NodeDto per doc (id = doc index, label = D1..).
     * When {@code posting} is null, {@code value} is 1 for
     * docs indexed so far (index &lt;= {@code indexedThrough});
     * otherwise {@code value} is 1 for docs in the posting
     * list. (The two cases are the build steps and the
     * query/final steps.)
     */
    private List<NodeDto> snapshotNodes(int docCount,
                                        int indexedThrough,
                                        List<Integer> posting) {
        List<NodeDto> dtos = new ArrayList<>();
        for (int i = 0; i < docCount; i++) {
            int value;
            if (posting != null) {
                value = posting.contains(i) ? 1 : 0;
            } else {
                value = i <= indexedThrough ? 1 : 0;
            }
            NodeDto dto = new NodeDto(i, value, 0, 0);
            dto.setLabel(docName(i));
            dtos.add(dto);
        }
        return dtos;
    }

    /** Lowercase words: split on non-alphanumerics, drop empties. */
    static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        for (String token : text.toLowerCase(java.util.Locale.ROOT)
                .split("[^a-z0-9]+")) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    /** "D1".."D6" for a doc index. */
    static String docName(int i) {
        return "D" + (i + 1);
    }

    /** "[D1, D2, D5]" rendering of a doc-index list. */
    static String docNames(List<Integer> docs) {
        List<String> names = new ArrayList<>();
        for (int d : docs) {
            names.add(docName(d));
        }
        return "[" + String.join(", ", names) + "]";
    }
}
