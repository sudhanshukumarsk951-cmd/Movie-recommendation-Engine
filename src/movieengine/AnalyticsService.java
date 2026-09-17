package movieengine;

import java.util.*;

public class AnalyticsService {
    public String summary(Collection<Movie> ms, Collection<User> us) {
        double avg = ms.stream().mapToDouble(Movie::getRating).average().orElse(0);
        long liked = us.stream().mapToLong(u -> u.getLiked().size()).sum();
        long watched = us.stream().mapToLong(u -> u.getWatched().size()).sum();
        return String.format(Locale.US,
                "Movies: %d\nUsers: %d\nAverage movie rating: %.2f/10\nTotal likes: %d\nTotal watched interactions: %d",
                ms.size(), us.size(), avg, liked, watched);
    }
}
