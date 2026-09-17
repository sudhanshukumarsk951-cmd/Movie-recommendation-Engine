import movieengine.*;import java.util.*;
public class RecommendationEngineTest {
 public static void main(String[] args){List<Movie> m=List.of(new Movie(1,2024,"Space Mission","Sci-Fi|Adventure","space mission explorer","English",9),new Movie(2,2024,"Deep Space","Sci-Fi","space explorer mission","English",8),new Movie(3,2024,"Comedy Night","Comedy","funny comedy show","English",7));User u=new User(1,"Test");u.like(1);List<Recommendation> r=new RecommendationEngine(m).recommend(u,5);if(r.stream().anyMatch(x->x.movie().getId()==1))throw new AssertionError("Liked movie recommended");if(r.isEmpty()||r.get(0).movie().getId()!=2)throw new AssertionError("Similarity ranking failed");System.out.println("All recommendation tests passed.");}
}
