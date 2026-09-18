<<<<<<< HEAD
# Movie Recommendation Engine

A Java 26 Swing desktop application for a college project. It uses CSV persistence and a content-based recommendation engine using TF-IDF vectors and cosine similarity.

## Included dataset

- `data/movies.csv` — 200 movies
- `data/users.csv` — 100 users

The application does not hard-code the dataset counts. The Dashboard reads the live `MovieRepository` counts.

## Main functionality

1. Dashboard — live movie/user/current-user counts and a Reload CSV + Refresh action.
2. Movies — full-catalog search, sorting, like, watched, rate, add and delete.
3. Recommendations — direct user search by name or ID, Show All, Top-N recommendations, and automatic refresh when users are added.
4. Users — search by name or ID, select account, add users, automatic CSV saving.
5. Analytics — live dataset statistics.
6. Data menu — Save Data, Reload CSV + Refresh, Refresh All Screens.

## Important behavior

- Adding a user updates the Users page, Recommendations user search/list, Dashboard count, Analytics and `users.csv` immediately.
- Adding a movie updates the Movies page, Dashboard count, Analytics and `movies.csv` immediately.
- `Reload CSV + Refresh` replaces the in-memory dataset from both CSV files and refreshes every screen.
- There is no 20-movie display limit.
- Liked and watched movies are excluded from recommendations.
- Do not delete `out` for normal development; the batch file recompiles it each time.

## Run

Open this project folder in VS Code and run in PowerShell:

```powershell
.\run-gui.bat
```

## Test

```powershell
.\run-tests.bat
```

The tests verify dataset loading, add-user, add-movie, ID/name search matching, recommendation exclusions, CSV save and CSV reload.

## Project structure

```text
src/movieengine/
  Main.java
  MainFrame.java
  Movie.java
  User.java
  MovieRepository.java
  CsvDataStore.java
  TfIdfVectorizer.java
  Recommendation.java
  RecommendationEngine.java
  AnalyticsService.java

test/
  RecommendationEngineTest.java
  ProjectSmokeTest.java

data/
  movies.csv
  users.csv

docs/
  architecture.puml
```

## New-user personalization
When creating a user, the GUI now asks for:
- Name
- Favorite genres (for example `Action|Sci-Fi`)
- Preferred language
- Optional favorite movie IDs

These preferences are saved in `data/users.csv` and are used by the TF-IDF/cosine recommendation engine. A newly created user therefore receives preference-aware recommendations instead of a generic anonymous ranking.
=======
# Movie-recommendation-Engine
A Java-based Movie Recommendation Engine with a Swing GUI, using TF-IDF and Cosine Similarity for personalized content-based recommendations, with 200+ movies, 100 users, search, ratings, watch history, analytics, CRUD operations, CSV persistence, and automated tests.
>>>>>>> ea22ea0780a31124c1dc033c32a13da833c645b6
