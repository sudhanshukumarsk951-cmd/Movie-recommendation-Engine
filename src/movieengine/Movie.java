package movieengine;

public class Movie {
    private int id, year;
    private String title, genre, description, language;
    private double rating;

    public Movie(int id, int year, String title, String genre, String description, String language, double rating) {
        if (id <= 0 || year < 1888 || title == null || title.isBlank())
            throw new IllegalArgumentException("Invalid movie data");
        this.id = id;
        this.year = year;
        this.title = title.trim();
        this.genre = genre == null ? "" : genre.trim();
        this.description = description == null ? "" : description.trim();
        this.language = language == null ? "" : language.trim();
        this.rating = Math.max(0, Math.min(10, rating));
    }

    public int getId() {
        return id;
    }

    public int getYear() {
        return year;
    }

    public String getTitle() {
        return title;
    }

    public String getGenre() {
        return genre;
    }

    public String getDescription() {
        return description;
    }

    public String getLanguage() {
        return language;
    }

    public double getRating() {
        return rating;
    }

    public String searchable() {
        return (title + " " + genre + " " + description + " " + language).toLowerCase();
    }
}
