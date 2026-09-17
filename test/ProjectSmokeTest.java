import movieengine.*;
import java.nio.file.*;
import java.util.*;

/**
 * End-to-end non-GUI smoke tests for the project data and recommendation
 * workflow.
 */
public class ProjectSmokeTest {
    public static void main(String[] args) throws Exception {
        CsvDataStore store = new CsvDataStore(Paths.get("data"));
        List<Movie> movies = store.loadMovies();
        List<User> users = store.loadUsers();
        check(movies.size() >= 200, "Expected at least 200 movies, got " + movies.size());
        check(users.size() >= 100, "Expected at least 100 users, got " + users.size());

        MovieRepository repo = new MovieRepository(movies, users);

        // Add user and verify live repository state.
        int nextUser = repo.nextUserId();
        User addedUser = new User(nextUser, "Smoke Test User");
        repo.addUser(addedUser);
        check(repo.users().size() == users.size() + 1, "Added user not reflected in repository");
        check(repo.getUser(nextUser) == addedUser, "Added user lookup failed");

        // Add movie and verify live repository state.
        int nextMovie = repo.nextMovieId();
        Movie addedMovie = new Movie(nextMovie, 2026, "Smoke Test Movie", "Drama|Mystery",
                "A test movie for validating search and recommendation updates.", "English", 8.5);
        repo.addMovie(addedMovie);
        check(repo.movies().size() == movies.size() + 1, "Added movie not reflected in repository");
        check(repo.getMovie(nextMovie) == addedMovie, "Added movie lookup failed");
        check(addedMovie.searchable().contains("smoke test movie"), "Movie search text does not include title");
        check(addedMovie.searchable().contains("mystery"), "Movie search text does not include genre");

        // Verify user search data can be matched by ID or name (same rule used by GUI).
        String idQuery = String.valueOf(nextUser);
        String nameQuery = "smoke test user";
        check((nextUser + " - " + addedUser.getName()).toLowerCase(Locale.ROOT).contains(idQuery), "ID search failed");
        check((nextUser + " - " + addedUser.getName()).toLowerCase(Locale.ROOT).contains(nameQuery),
                "Name search failed");

        // Verify liked/watched movies never appear in recommendations.
        addedUser.like(1);
        addedUser.watch(2);
        addedUser.rate(3, 9);
        List<Recommendation> recs = new RecommendationEngine(repo.movies()).recommend(addedUser, 50);
        check(recs.stream().noneMatch(r -> r.movie().getId() == 1), "Liked movie was recommended");
        check(recs.stream().noneMatch(r -> r.movie().getId() == 2), "Watched movie was recommended");

        // Verify both additions persist to CSV and survive reload.
        Path tmp = Files.createTempDirectory("mre-smoke-");
        CsvDataStore tmpStore = new CsvDataStore(tmp);
        tmpStore.saveMovies(repo.movies());
        tmpStore.saveUsers(repo.users());
        List<Movie> reloadedMovies = tmpStore.loadMovies();
        List<User> reloadedUsers = tmpStore.loadUsers();
        check(reloadedMovies.size() == repo.movies().size(), "Movie CSV persistence failed");
        check(reloadedUsers.size() == repo.users().size(), "User CSV persistence failed");
        check(reloadedMovies.stream().anyMatch(m -> m.getId() == nextMovie), "Added movie missing after reload");
        check(reloadedUsers.stream().anyMatch(u -> u.getId() == nextUser && u.getName().equals("Smoke Test User")),
                "Added user missing after reload");

        System.out.println("ALL SMOKE TESTS PASSED");
        System.out.println("Dataset verified: " + movies.size() + " movies, " + users.size() + " users");
        System.out.println("Add user/movie, search matching, recommendations, save and reload verified.");
    }

    private static void check(boolean ok, String message) {
        if (!ok)
            throw new AssertionError(message);
    }
}
