package io.github.valossa515.pricetracker.marketplace;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Token-set Jaccard similarity for product titles, with light Portuguese normalization.
 * Returns a score in [0.0, 1.0]. Titles are lowercased, accent-stripped, punctuation-removed,
 * and tokens shorter than 2 chars or in a small stopword list are dropped.
 */
@Component
public class ProductTitleMatcher {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9 ]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Set<String> STOPWORDS = Set.of(
            "de", "da", "do", "das", "dos",
            "a", "e", "o", "para", "com", "sem",
            "the", "and", "or", "for", "with"
    );

    public double similarity(String a, String b) {
        Set<String> ta = tokenize(a);
        Set<String> tb = tokenize(b);
        if (ta.isEmpty() || tb.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(ta);
        intersection.retainAll(tb);
        Set<String> union = new HashSet<>(ta);
        union.addAll(tb);
        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String input) {
        if (input == null || input.isBlank()) return Set.of();
        String stripped = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
        String cleaned = NON_ALNUM.matcher(stripped).replaceAll(" ");
        cleaned = WHITESPACE.matcher(cleaned).replaceAll(" ").trim();
        if (cleaned.isEmpty()) return Set.of();
        Set<String> tokens = new HashSet<>(Arrays.asList(cleaned.split(" ")));
        tokens.removeIf(t -> t.length() < 2 || STOPWORDS.contains(t));
        return tokens;
    }
}
