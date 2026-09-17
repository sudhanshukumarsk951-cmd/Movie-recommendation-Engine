package movieengine;

import java.util.*;

public class TfIdfVectorizer {
    public Map<String, Double> vector(String text, Map<String, Integer> df, int docs) {
        Map<String, Integer> tf = new HashMap<>();
        for (String t : tokens(text))
            tf.merge(t, 1, Integer::sum);
        Map<String, Double> v = new HashMap<>();
        for (var e : tf.entrySet()) {
            double idf = Math.log((docs + 1.0) / (df.getOrDefault(e.getKey(), 0) + 1.0)) + 1;
            v.put(e.getKey(), e.getValue() * idf);
        }
        return v;
    }

    public Set<String> tokens(String s) {
        Set<String> out = new HashSet<>();
        for (String t : s.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
            if (t.length() > 2)
                out.add(t);
        return out;
    }

    public double cosine(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0, na = 0, nb = 0;
        for (double x : a.values())
            na += x * x;
        for (double x : b.values())
            nb += x * x;
        for (var e : a.entrySet())
            dot += e.getValue() * b.getOrDefault(e.getKey(), 0.0);
        return na == 0 || nb == 0 ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
