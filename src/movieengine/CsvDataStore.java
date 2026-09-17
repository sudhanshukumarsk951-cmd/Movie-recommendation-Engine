package movieengine;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** CSV persistence for movies and users. Supports the older 5-column users.csv format too. */
public class CsvDataStore {
    private final Path dir;
    public CsvDataStore(Path dir) { this.dir = dir; }

    public List<Movie> loadMovies() {
        List<Movie> out = new ArrayList<>();
        Path p = dir.resolve("movies.csv");
        if (!Files.exists(p)) return out;
        try (BufferedReader br = Files.newBufferedReader(p)) {
            br.readLine();
            String s;
            while ((s = br.readLine()) != null) {
                List<String> c = parse(s);
                if (c.size() >= 7) try {
                    out.add(new Movie(Integer.parseInt(c.get(0)), Integer.parseInt(c.get(1)), c.get(2), c.get(3),
                            c.get(4), c.get(5), Double.parseDouble(c.get(6))));
                } catch (Exception ignored) { }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read movies.csv: " + e.getMessage(), e);
        }
        return out;
    }

    public List<User> loadUsers() {
        List<User> out = new ArrayList<>();
        Path p = dir.resolve("users.csv");
        if (!Files.exists(p)) return out;
        try (BufferedReader br = Files.newBufferedReader(p)) {
            br.readLine();
            String s;
            while ((s = br.readLine()) != null) {
                List<String> c = parse(s);
                if (c.size() >= 5) try {
                    String genres = c.size() >= 6 ? c.get(5) : "";
                    String language = c.size() >= 7 ? c.get(6) : "";
                    User u = new User(Integer.parseInt(c.get(0)), c.get(1), genres, language);
                    split(c.get(2)).forEach(u::like);
                    split(c.get(3)).forEach(u::watch);
                    for (String r : splitText(c.get(4), "\\|")) {
                        String[] a = r.split(":");
                        if (a.length == 2) u.rate(Integer.parseInt(a[0]), Integer.parseInt(a[1]));
                    }
                    out.add(u);
                } catch (Exception ignored) { }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot read users.csv: " + e.getMessage(), e);
        }
        return out;
    }

    public void saveMovies(Collection<Movie> ms) {
        try {
            Files.createDirectories(dir);
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dir.resolve("movies.csv")))) {
                w.println("id,year,title,genre,description,language,rating");
                for (Movie m : ms) w.printf(Locale.US, "%d,%d,%s,%s,%s,%s,%.1f%n", m.getId(), m.getYear(),
                        q(m.getTitle()), q(m.getGenre()), q(m.getDescription()), q(m.getLanguage()), m.getRating());
            }
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public void saveUsers(Collection<User> us) {
        try {
            Files.createDirectories(dir);
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dir.resolve("users.csv")))) {
                w.println("id,name,liked,watched,ratings,favoriteGenres,preferredLanguage");
                for (User u : us) w.printf("%d,%s,%s,%s,%s,%s,%s%n", u.getId(), q(u.getName()),
                        join(u.getLiked(), "|"), join(u.getWatched(), "|"), ratings(u.getRatings()),
                        q(u.favoriteGenresCsv()), q(u.getPreferredLanguage()));
            }
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    private String q(String s) { return "\"" + (s == null ? "" : s.replace("\"", "\"\"")) + "\""; }
    private String join(Collection<Integer> c, String sep) { return c.stream().map(String::valueOf).reduce((a,b) -> a + sep + b).orElse(""); }
    private String ratings(Map<Integer,Integer> m) { List<String> a = new ArrayList<>(); m.forEach((k,v) -> a.add(k + ":" + v)); return String.join("|", a); }
    private List<Integer> split(String s) {
        List<Integer> a = new ArrayList<>(); if (s == null) return a;
        for (String x : s.split("\\|")) try { if (!x.isBlank()) a.add(Integer.parseInt(x)); } catch (Exception ignored) { }
        return a;
    }
    private List<String> splitText(String s, String regex) { return s == null || s.isBlank() ? List.of() : Arrays.asList(s.split(regex)); }
    private List<String> parse(String line) {
        List<String> a = new ArrayList<>(); StringBuilder b = new StringBuilder(); boolean quoted = false;
        for (int i=0; i<line.length(); i++) {
            char ch=line.charAt(i);
            if (ch=='\"') {
                if (quoted && i+1<line.length() && line.charAt(i+1)=='\"') { b.append('\"'); i++; }
                else quoted=!quoted;
            } else if (ch==',' && !quoted) { a.add(b.toString()); b.setLength(0); }
            else b.append(ch);
        }
        a.add(b.toString()); return a;
    }
}
