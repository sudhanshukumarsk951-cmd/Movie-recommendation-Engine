package movieengine;

import java.util.*;

/** User profile and interaction history used by the recommender. */
public class User {
    private final int id;
    private String name;
    private final Set<Integer> liked = new LinkedHashSet<>();
    private final Set<Integer> watched = new LinkedHashSet<>();
    private final Map<Integer, Integer> ratings = new LinkedHashMap<>();
    private final LinkedHashSet<String> favoriteGenres = new LinkedHashSet<>();
    private String preferredLanguage = "";

    public User(int id, String name) {
        this(id, name, "", "");
    }

    public User(int id, String name, String favoriteGenres, String preferredLanguage) {
        this.id = id;
        this.name = name == null ? "User" : name.trim();
        setFavoriteGenres(favoriteGenres);
        setPreferredLanguage(preferredLanguage);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Set<Integer> getLiked() { return liked; }
    public Set<Integer> getWatched() { return watched; }
    public Map<Integer, Integer> getRatings() { return ratings; }
    public Set<String> getFavoriteGenres() { return Collections.unmodifiableSet(favoriteGenres); }
    public String getPreferredLanguage() { return preferredLanguage; }

    public void like(int movieId) { liked.add(movieId); }
    public void watch(int movieId) { watched.add(movieId); }
    public void rate(int movieId, int rating) { ratings.put(movieId, Math.max(1, Math.min(10, rating))); }

    public void setFavoriteGenres(String text) {
        favoriteGenres.clear();
        if (text == null) return;
        for (String g : text.split("\\|")) {
            String value = g.trim();
            if (!value.isBlank()) favoriteGenres.add(value);
        }
    }

    public void setPreferredLanguage(String language) {
        preferredLanguage = language == null ? "" : language.trim();
    }

    public boolean hasPreferences() {
        return !favoriteGenres.isEmpty() || !preferredLanguage.isBlank() || !liked.isEmpty() || !ratings.isEmpty();
    }

    public String favoriteGenresCsv() {
        return String.join("|", favoriteGenres);
    }
}
