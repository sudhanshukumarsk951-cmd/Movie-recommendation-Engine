package movieengine;

import java.util.*;

/** In-memory repository used by all Swing panels. */
public class MovieRepository {
    private final Map<Integer, Movie> movies = new LinkedHashMap<>();
    private final Map<Integer, User> users = new LinkedHashMap<>();

    public MovieRepository(List<Movie> ms, List<User> us) {
        replaceData(ms, us);
    }

    public synchronized void replaceData(List<Movie> ms, List<User> us) {
        movies.clear();
        users.clear();
        if (ms != null)
            ms.forEach(m -> movies.put(m.getId(), m));
        if (us != null)
            us.forEach(u -> users.put(u.getId(), u));
    }

    public Collection<Movie> movies() {
        return Collections.unmodifiableCollection(movies.values());
    }

    public Collection<User> users() {
        return Collections.unmodifiableCollection(users.values());
    }

    public Movie getMovie(int id) {
        return movies.get(id);
    }

    public User getUser(int id) {
        return users.get(id);
    }

    public void addMovie(Movie m) {
        movies.put(m.getId(), m);
    }

    public void deleteMovie(int id) {
        movies.remove(id);
        users.values().forEach(u -> {
            u.getLiked().remove(id);
            u.getWatched().remove(id);
            u.getRatings().remove(id);
        });
    }

    public void addUser(User u) {
        users.put(u.getId(), u);
    }

    public int nextMovieId() {
        return movies.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
    }

    public int nextUserId() {
        return users.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
    }
}
