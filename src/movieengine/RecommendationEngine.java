package movieengine;

import java.util.*;

/**
 * Content-based recommendation engine using TF-IDF/cosine plus explicit user
 * preferences.
 */
public class RecommendationEngine {
    private final TfIdfVectorizer tf = new TfIdfVectorizer();
    private final Collection<Movie> catalog;

    public RecommendationEngine(Collection<Movie> catalog) {
        this.catalog = catalog;
    }

    public List<Recommendation> recommend(User u, int k) {
        List<Movie> seeds = new ArrayList<>();
        for (int id : u.getLiked()) {
            Movie m = find(id);
            if (m != null)
                seeds.add(m);
        }
        for (var e : u.getRatings().entrySet())
            if (e.getValue() >= 7) {
                Movie m = find(e.getKey());
                if (m != null)
                    seeds.add(m);
            }

        Map<String, Integer> df = new HashMap<>();
        for (Movie m : catalog)
            for (String t : tf.tokens(m.searchable()))
                df.merge(t, 1, Integer::sum);
        Map<Movie, Map<String, Double>> vec = new HashMap<>();
        for (Movie m : catalog)
            vec.put(m, tf.vector(m.searchable(), df, Math.max(1, catalog.size())));

        List<Recommendation> r = new ArrayList<>();
        for (Movie m : catalog) {
            if (u.getLiked().contains(m.getId()) || u.getWatched().contains(m.getId()))
                continue;

            double sim = 0;
            for (Movie s : seeds)
                sim = Math.max(sim, tf.cosine(vec.get(m), vec.get(s)));
            double genreFromHistory = 0, languageFromHistory = 0;
            for (Movie s : seeds) {
                if (overlap(m.getGenre(), s.getGenre()))
                    genreFromHistory = Math.max(genreFromHistory, 1);
                if (m.getLanguage().equalsIgnoreCase(s.getLanguage()))
                    languageFromHistory = Math.max(languageFromHistory, 1);
            }

            double preferredGenre = genrePreference(m.getGenre(), u.getFavoriteGenres());
            double preferredLanguage = !u.getPreferredLanguage().isBlank() &&
                    m.getLanguage().equalsIgnoreCase(u.getPreferredLanguage()) ? 1 : 0;

            double neg = 0;
            for (var e : u.getRatings().entrySet())
                if (e.getValue() <= 4) {
                    Movie bad = find(e.getKey());
                    if (bad != null)
                        neg = Math.max(neg, tf.cosine(vec.get(m), vec.get(bad)));
                }

            double score = .48 * sim + .14 * genreFromHistory + .10 * (m.getRating() / 10) + .06 * languageFromHistory
                    + .14 * preferredGenre + .08 * preferredLanguage - .08 * neg;
            String reason;
            if (preferredGenre > 0 && preferredLanguage > 0)
                reason = "Matches your preferred genre and language";
            else if (preferredGenre > 0)
                reason = "Matches your preferred genre";
            else if (preferredLanguage > 0)
                reason = "Matches your preferred language";
            else if (sim > .35)
                reason = "Similar to your liked/high-rated movies";
            else
                reason = "Matches catalog relevance and your profile";
            r.add(new Recommendation(m, score, reason));
        }
        r.sort(Comparator.comparingDouble(Recommendation::score).reversed());
        return r.subList(0, Math.min(k, r.size()));
    }

    private double genrePreference(String movieGenre, Set<String> prefs) {
        if (prefs.isEmpty())
            return 0;
        for (String mg : movieGenre.split("\\|"))
            for (String pg : prefs)
                if (mg.trim().equalsIgnoreCase(pg.trim()))
                    return 1;
        return 0;
    }

    private Movie find(int id) {
        for (Movie m : catalog)
            if (m.getId() == id)
                return m;
        return null;
    }

    private boolean overlap(String a, String b) {
        for (String x : a.split("\\|"))
            for (String y : b.split("\\|"))
                if (x.equalsIgnoreCase(y))
                    return true;
        return false;
    }
}
