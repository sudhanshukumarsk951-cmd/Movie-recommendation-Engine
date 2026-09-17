package movieengine;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;

/**
 * Main Swing window. All screens use the same live MovieRepository instance.
 * Counts and lists are refreshed after every data-changing operation.
 */
public class MainFrame extends JFrame {
    private final MovieRepository repo;
    private final CsvDataStore store;
    private User current;

    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel status = new JLabel(" Ready");

    // Dashboard - always updated from repo, never hard-coded.
    private JLabel dashboardMovies;
    private JLabel dashboardUsers;
    private JLabel dashboardCurrent;
    private JLabel dashboardDataPath;

    // Movies.
    private JTable movieTable;
    private DefaultTableModel movieModel;
    private JTextField movieSearch;

    // Users.
    private DefaultListModel<UserChoice> usersModel;
    private JList<UserChoice> usersList;
    private JTextField userSearch;
    private JLabel userInfo;
    private final List<User> visibleUsers = new ArrayList<>();

    // Recommendations - direct searchable user selector.
    private JTextField recommendationUserSearch;
    private DefaultListModel<UserChoice> recommendationUsersModel;
    private JList<UserChoice> recommendationUsersList;
    private JTextArea recommendationOutput;
    private JSpinner recommendationTopN;

    // Analytics.
    private JTextArea analyticsOutput;

    public MainFrame(MovieRepository repo, CsvDataStore store) {
        super("Movie Recommendation Engine");
        this.repo = Objects.requireNonNull(repo);
        this.store = Objects.requireNonNull(store);
        this.current = repo.users().stream().findFirst().orElse(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 760);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        build();
        refreshAllViews();
    }

    private void build() {
        tabs.addTab("Dashboard", dashboard());
        tabs.addTab("Movies", movies());
        tabs.addTab("Recommendations", recommendations());
        tabs.addTab("Users", users());
        tabs.addTab("Analytics", analytics());
        tabs.addChangeListener(e -> refreshAllViews());

        add(tabs, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        JMenuBar mb = new JMenuBar();
        JMenu data = new JMenu("Data");
        JMenuItem save = new JMenuItem("Save Data");
        JMenuItem reload = new JMenuItem("Reload CSV + Refresh");
        JMenuItem refresh = new JMenuItem("Refresh All Screens");
        save.addActionListener(e -> save());
        reload.addActionListener(e -> reloadCsv());
        refresh.addActionListener(e -> refreshAllViews());
        data.add(save);
        data.add(reload);
        data.add(refresh);
        mb.add(data);
        setJMenuBar(mb);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                save();
            }
        });
    }

    private JPanel dashboard() {
        JPanel p = base("Movie Recommendation Engine",
                "Java Swing content-based recommender with CSV persistence and TF-IDF + cosine similarity.");

        JPanel cards = new JPanel(new GridLayout(1, 3, 15, 15));
        dashboardMovies = bigValue("0", "Movies in live dataset");
        dashboardUsers = bigValue("0", "Users in live dataset");
        dashboardCurrent = bigValue("None", "Current user");
        cards.add(dashboardMovies.getParent());
        cards.add(dashboardUsers.getParent());
        cards.add(dashboardCurrent.getParent());

        JButton refresh = new JButton("Refresh Dataset Counts");
        refresh.setToolTipText("Reload movies.csv/users.csv and update every screen");
        refresh.addActionListener(e -> reloadCsv());

        dashboardDataPath = new JLabel(" Data folder: " + storeDirectoryText());
        JPanel tools = new JPanel(new BorderLayout(10, 5));
        tools.add(refresh, BorderLayout.WEST);
        tools.add(dashboardDataPath, BorderLayout.CENTER);

        JTextArea info = area();
        info.setText("\n LIVE DATASET STATUS\n\n"
                + "The dashboard reads counts from the shared in-memory repository.\n"
                + "Adding or deleting a movie/user refreshes these counters immediately.\n"
                + "The Refresh Dataset Counts button reloads both CSV files and refreshes all screens.\n\n"
                + " FEATURES\n\n"
                + " • Search the complete movie catalog\n"
                + " • Search users by name or ID\n"
                + " • Direct user search inside Recommendations\n"
                + " • Like, watch and rate movies\n"
                + " • Personalized TF-IDF recommendations\n"
                + " • Separate user profiles\n"
                + " • Automatic CSV saving\n"
                + " • Analytics dashboard\n");

        JPanel center = new JPanel(new BorderLayout(15, 15));
        center.add(cards, BorderLayout.NORTH);
        center.add(tools, BorderLayout.CENTER);
        center.add(new JScrollPane(info), BorderLayout.SOUTH);
        // Give the information area the remaining space while keeping the controls
        // visible.
        JPanel body = new JPanel(new BorderLayout(12, 12));
        body.add(cards, BorderLayout.NORTH);
        body.add(tools, BorderLayout.CENTER);
        body.add(new JScrollPane(info), BorderLayout.SOUTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    private String storeDirectoryText() {
        return "data/ (movies.csv + users.csv)";
    }

    private JLabel bigValue(String value, String caption) {
        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 190, 205)),
                BorderFactory.createEmptyBorder(12, 8, 12, 8)));
        card.add(valueLabel, BorderLayout.CENTER);
        JLabel cap = new JLabel(caption, SwingConstants.CENTER);
        cap.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        card.add(cap, BorderLayout.SOUTH);
        valueLabel.putClientProperty("card", card);
        return valueLabel;
    }

    private JPanel movies() {
        JPanel p = base("Movie Catalog", "Search the complete live dataset. There is no 20-movie display limit.");
        JPanel top = new JPanel(new BorderLayout(8, 8));
        movieSearch = new JTextField();
        movieSearch.setToolTipText("Search title, genre, description or language");
        JButton search = new JButton("Search");
        JButton all = new JButton("Show All");
        top.add(movieSearch, BorderLayout.CENTER);
        top.add(search, BorderLayout.EAST);
        top.add(all, BorderLayout.WEST);
        p.add(top, BorderLayout.NORTH);

        movieModel = new DefaultTableModel(new Object[] { "ID", "Year", "Title", "Genre", "Language", "Rating" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        movieTable = new JTable(movieModel);
        movieTable.setRowHeight(28);
        movieTable.setAutoCreateRowSorter(true);
        p.add(new JScrollPane(movieTable), BorderLayout.CENTER);

        JPanel actions = new JPanel();
        JButton like = new JButton("Like");
        JButton watched = new JButton("Watched");
        JButton rate = new JButton("Rate");
        JButton add = new JButton("Add Movie");
        JButton del = new JButton("Delete");
        actions.add(like);
        actions.add(watched);
        actions.add(rate);
        actions.add(add);
        actions.add(del);
        p.add(actions, BorderLayout.SOUTH);

        search.addActionListener(e -> refreshMovies(movieSearch.getText()));
        all.addActionListener(e -> {
            movieSearch.setText("");
            refreshMovies("");
        });
        movieSearch.addActionListener(e -> refreshMovies(movieSearch.getText()));
        like.addActionListener(e -> interact("like"));
        watched.addActionListener(e -> interact("watched"));
        rate.addActionListener(e -> interact("rate"));
        add.addActionListener(e -> addMovie());
        del.addActionListener(e -> deleteMovie());
        return p;
    }

    private void refreshMovies(String q) {
        if (movieModel == null)
            return;
        movieModel.setRowCount(0);
        String x = q == null ? "" : q.toLowerCase(Locale.ROOT).trim();
        for (Movie m : repo.movies()) {
            if (x.isBlank() || m.searchable().contains(x)) {
                movieModel.addRow(new Object[] { m.getId(), m.getYear(), m.getTitle(),
                        m.getGenre().replace('|', ','), m.getLanguage(), m.getRating() });
            }
        }
        status.setText(" Showing " + movieModel.getRowCount() + " of " + repo.movies().size() + " movies");
    }

    private int selectedMovieId() {
        int viewRow = movieTable.getSelectedRow();
        if (viewRow < 0)
            return -1;
        int row = movieTable.convertRowIndexToModel(viewRow);
        return (int) movieModel.getValueAt(row, 0);
    }

    private void interact(String type) {
        if (current == null) {
            msg("No user available. Add a user first.");
            return;
        }
        int id = selectedMovieId();
        if (id < 0) {
            msg("Select a movie first.");
            return;
        }

        if (type.equals("like"))
            current.like(id);
        else if (type.equals("watched"))
            current.watch(id);
        else {
            String s = JOptionPane.showInputDialog(this, "Rating (1-10):", "8");
            if (s == null)
                return;
            try {
                int r = Integer.parseInt(s.trim());
                if (r < 1 || r > 10)
                    throw new NumberFormatException();
                current.rate(id, r);
            } catch (NumberFormatException e) {
                msg("Enter a whole number from 1 to 10.");
                return;
            }
        }
        save();
        refreshAllViews();
    }

    private void addMovie() {
        JTextField t = new JTextField();
        JTextField g = new JTextField("Drama");
        JTextField d = new JTextField();
        JTextField l = new JTextField("English");
        JTextField y = new JTextField("2026");
        JTextField r = new JTextField("8.0");
        Object[] fields = { "Title", t, "Genre", g, "Description", d, "Language", l, "Year", y, "Rating", r };
        if (JOptionPane.showConfirmDialog(this, fields, "Add Movie",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return;
        try {
            if (t.getText().isBlank())
                throw new IllegalArgumentException("Title is required");
            int year = Integer.parseInt(y.getText().trim());
            double rating = Double.parseDouble(r.getText().trim());
            if (year < 1888)
                throw new IllegalArgumentException("Year must be 1888 or later");
            if (rating < 0 || rating > 10)
                throw new IllegalArgumentException("Rating must be 0-10");
            repo.addMovie(
                    new Movie(repo.nextMovieId(), year, t.getText(), g.getText(), d.getText(), l.getText(), rating));
            save();
            refreshAllViews();
            tabs.setSelectedIndex(1);
        } catch (Exception e) {
            msg("Invalid movie details: " + e.getMessage());
        }
    }

    private void deleteMovie() {
        int id = selectedMovieId();
        if (id < 0) {
            msg("Select a movie first.");
            return;
        }
        Movie m = repo.getMovie(id);
        if (m == null)
            return;
        if (JOptionPane.showConfirmDialog(this, "Delete movie: " + m.getTitle() + "?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;
        repo.deleteMovie(id);
        save();
        refreshAllViews();
    }

    private JPanel recommendations() {
        JPanel p = base("Recommendations",
                "Search for a user directly here, select the user, and generate personalized recommendations.");

        JPanel searchRow = new JPanel(new BorderLayout(8, 8));
        recommendationUserSearch = new JTextField();
        recommendationUserSearch.setToolTipText("Type user ID or name (e.g. 101 or Rahul)");
        JButton search = new JButton("Search User");
        JButton all = new JButton("Show All");
        searchRow.add(new JLabel(" User search: "), BorderLayout.WEST);
        searchRow.add(recommendationUserSearch, BorderLayout.CENTER);
        searchRow.add(search, BorderLayout.EAST);
        searchRow.add(all, BorderLayout.SOUTH);

        recommendationUsersModel = new DefaultListModel<>();
        recommendationUsersList = new JList<>(recommendationUsersModel);
        recommendationUsersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recommendationUsersList.setVisibleRowCount(6);
        recommendationUsersList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        JScrollPane usersScroll = new JScrollPane(recommendationUsersList);
        usersScroll.setBorder(BorderFactory.createTitledBorder("Matching users — search by ID or name"));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        recommendationTopN = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
        JButton generate = new JButton("Generate Recommendations");
        controls.add(new JLabel("Top N:"));
        controls.add(recommendationTopN);
        controls.add(generate);

        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setPreferredSize(new Dimension(360, 0));
        left.add(searchRow, BorderLayout.NORTH);
        left.add(usersScroll, BorderLayout.CENTER);
        left.add(controls, BorderLayout.SOUTH);

        recommendationOutput = area();
        recommendationOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JPanel result = new JPanel(new BorderLayout());
        result.setBorder(BorderFactory.createTitledBorder("Recommendation Results"));
        result.add(new JScrollPane(recommendationOutput), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, result);
        split.setResizeWeight(0.30);
        p.add(split, BorderLayout.CENTER);

        search.addActionListener(e -> refreshRecommendationUsers(recommendationUserSearch.getText()));
        all.addActionListener(e -> {
            recommendationUserSearch.setText("");
            refreshRecommendationUsers("");
        });
        recommendationUserSearch.addActionListener(e -> refreshRecommendationUsers(recommendationUserSearch.getText()));
        addDocumentListener(recommendationUserSearch,
                () -> refreshRecommendationUsers(recommendationUserSearch.getText()));
        generate.addActionListener(e -> generateRecommendations((Integer) recommendationTopN.getValue()));
        recommendationUsersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                generateRecommendationsIfSelected();
        });
        return p;
    }

    private void refreshRecommendationUsers(String query) {
        if (recommendationUsersModel == null)
            return;
        int oldId = selectedRecommendationUserId();
        recommendationUsersModel.clear();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        int matches = 0;
        for (User u : repo.users()) {
            String text = u.getId() + " - " + u.getName();
            if (q.isBlank() || text.toLowerCase(Locale.ROOT).contains(q)) {
                recommendationUsersModel.addElement(new UserChoice(u));
                matches++;
            }
        }

        int preferred = oldId > 0 ? oldId : current == null ? -1 : current.getId();
        int selected = -1;
        for (int i = 0; i < recommendationUsersModel.size(); i++) {
            if (recommendationUsersModel.getElementAt(i).user().getId() == preferred) {
                selected = i;
                break;
            }
        }
        if (selected >= 0)
            recommendationUsersList.setSelectedIndex(selected);
        else if (matches > 0)
            recommendationUsersList.setSelectedIndex(0);

        if (matches == 0)
            recommendationOutput.setText("No users match: " + (query == null ? "" : query));
    }

    private int selectedRecommendationUserId() {
        UserChoice choice = recommendationUsersList == null ? null : recommendationUsersList.getSelectedValue();
        return choice == null ? -1 : choice.user().getId();
    }

    private User selectedRecommendationUser() {
        UserChoice choice = recommendationUsersList == null ? null : recommendationUsersList.getSelectedValue();
        return choice == null ? null : choice.user();
    }

    private void generateRecommendationsIfSelected() {
        User u = selectedRecommendationUser();
        if (u != null && recommendationOutput != null)
            generateRecommendations((Integer) recommendationTopN.getValue());
    }

    private void generateRecommendations(int k) {
        User u = selectedRecommendationUser();
        if (u == null) {
            msg("Search for and select a user first.");
            return;
        }
        current = u;
        List<Recommendation> rs = new RecommendationEngine(repo.movies()).recommend(u, k);
        StringBuilder b = new StringBuilder();
        b.append("Recommendations for ").append(u.getName()).append(" (User ID ").append(u.getId()).append(")\n");
        b.append("Catalog: ").append(repo.movies().size()).append(" movies | Users: ").append(repo.users().size())
                .append("\n");
        b.append("=".repeat(90)).append("\n\n");
        if (rs.isEmpty())
            b.append("No eligible movies are available for this user.\n");
        else {
            int i = 1;
            for (Recommendation x : rs) {
                b.append(String.format(Locale.US, "%2d. %-42s Score %.3f%n", i++, x.movie().getTitle(), x.score()));
                b.append("    ").append(x.reason()).append("\n\n");
            }
        }
        recommendationOutput.setText(b.toString());
        recommendationOutput.setCaretPosition(0);
        refreshDashboardOnly();
    }

    private record UserChoice(User user) {
        @Override
        public String toString() {
            return user.getId() + " - " + user.getName();
        }
    }

    private JPanel users() {
        JPanel p = base("User Accounts",
                "Search users by ID or name, switch accounts, and add users. Changes are saved automatically.");
        usersModel = new DefaultListModel<>();
        usersList = new JList<>(usersModel);
        usersList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        userSearch = new JTextField();
        JButton find = new JButton("Search");
        JButton all = new JButton("Show All");
        JButton use = new JButton("Use Selected User");
        JButton add = new JButton("Add User");
        userInfo = new JLabel(" Current: None");

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(userSearch, BorderLayout.CENTER);
        top.add(find, BorderLayout.EAST);
        top.add(all, BorderLayout.WEST);

        JPanel bottom = new JPanel();
        bottom.add(use);
        bottom.add(add);

        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(usersList), BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        p.add(userInfo, BorderLayout.WEST);

        find.addActionListener(e -> refreshUsers(userSearch.getText()));
        all.addActionListener(e -> {
            userSearch.setText("");
            refreshUsers("");
        });
        userSearch.addActionListener(e -> refreshUsers(userSearch.getText()));
        use.addActionListener(e -> useSelectedUser());
        add.addActionListener(e -> addUser());
        return p;
    }

    private void refreshUsers(String query) {
        if (usersModel == null)
            return;
        usersModel.clear();
        visibleUsers.clear();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (User u : repo.users()) {
            String line = u.getId() + " - " + u.getName();
            if (q.isBlank() || line.toLowerCase(Locale.ROOT).contains(q)) {
                usersModel.addElement(new UserChoice(u));
                visibleUsers.add(u);
            }
        }
        status.setText(" Showing " + visibleUsers.size() + " of " + repo.users().size() + " users");
    }

    private void useSelectedUser() {
        UserChoice choice = usersList.getSelectedValue();
        if (choice == null) {
            msg("Select a user first.");
            return;
        }
        current = choice.user();
        refreshAllViews();
        selectUserInRecommendation(current.getId());
        save();
    }

    private void addUser() {
        JTextField name = new JTextField();
        JTextField genres = new JTextField("Action|Drama");
        JComboBox<String> language = new JComboBox<>(new String[] {
                "English", "Hindi", "Tamil", "Telugu", "Korean", "Spanish"
        });
        JTextField likedMovies = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.add(new JLabel("User name *"));
        form.add(name);
        form.add(new JLabel("Favorite genres * (separate with |, e.g. Action|Sci-Fi)"));
        form.add(genres);
        form.add(new JLabel("Preferred language *"));
        form.add(language);
        form.add(new JLabel("Favorite movie IDs (optional, comma-separated, e.g. 3,12,25)"));
        form.add(likedMovies);

        JOptionPane option = new JOptionPane(form, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = option.createDialog(this, "Create Personalized User Profile");
        dialog.setModal(true);
        dialog.setVisible(true);
        Object result = option.getValue();
        if (!(result instanceof Integer) || (Integer) result != JOptionPane.OK_OPTION)
            return;

        String newName = name.getText().trim();
        String newGenres = genres.getText().trim();
        String newLanguage = String.valueOf(language.getSelectedItem()).trim();
        if (newName.isBlank()) {
            msg("User name cannot be empty.");
            return;
        }
        if (newGenres.isBlank()) {
            msg("Please enter at least one favorite genre.");
            return;
        }
        if (newLanguage.isBlank()) {
            msg("Please select a preferred language.");
            return;
        }
        if (repo.users().stream().anyMatch(u -> u.getName().equalsIgnoreCase(newName))) {
            msg("A user with this name already exists.");
            return;
        }

        User u = new User(repo.nextUserId(), newName, newGenres, newLanguage);
        String ids = likedMovies.getText().trim();
        if (!ids.isBlank()) {
            for (String token : ids.split(",")) {
                try {
                    int id = Integer.parseInt(token.trim());
                    if (repo.getMovie(id) != null)
                        u.like(id);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        repo.addUser(u);
        current = u;
        save();
        refreshAllViews();
        userSearch.setText("");
        refreshUsers("");
        selectUserInUsers(u.getId());
        recommendationUserSearch.setText(String.valueOf(u.getId()));
        refreshRecommendationUsers(recommendationUserSearch.getText());
        selectUserInRecommendation(u.getId());
        status.setText(" User added: " + u.getName() + " (ID " + u.getId() + ") • "
                + repo.users().size() + " users total • personalized profile saved");
        JOptionPane.showMessageDialog(this,
                "User created successfully.\n\n" +
                        "Genres: " + u.favoriteGenresCsv().replace('|', ',') + "\n" +
                        "Language: " + u.getPreferredLanguage() + "\n" +
                        "Liked movies: " + u.getLiked().size() + "\n\n" +
                        "Recommendations will now use these preferences.",
                "Personalized Profile Created", JOptionPane.INFORMATION_MESSAGE);
    }

    private void selectUserInUsers(int id) {
        if (usersModel == null)
            return;
        for (int i = 0; i < usersModel.size(); i++) {
            if (usersModel.getElementAt(i).user().getId() == id) {
                usersList.setSelectedIndex(i);
                usersList.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    private void selectUserInRecommendation(int id) {
        if (recommendationUsersModel == null)
            return;
        for (int i = 0; i < recommendationUsersModel.size(); i++) {
            if (recommendationUsersModel.getElementAt(i).user().getId() == id) {
                recommendationUsersList.setSelectedIndex(i);
                recommendationUsersList.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    private JPanel analytics() {
        JPanel p = base("Analytics", "Live statistics from the current in-memory dataset.");
        analyticsOutput = area();
        p.add(new JScrollPane(analyticsOutput), BorderLayout.CENTER);
        return p;
    }

    /** Refresh every visible data-dependent component from the repository. */
    private void refreshAllViews() {
        refreshDashboardOnly();
        if (movieSearch != null)
            refreshMovies(movieSearch.getText());
        if (userSearch != null)
            refreshUsers(userSearch.getText());
        if (recommendationUsersModel != null)
            refreshRecommendationUsers(recommendationUserSearch == null ? "" : recommendationUserSearch.getText());
        if (analyticsOutput != null)
            analyticsOutput.setText(new AnalyticsService().summary(repo.movies(), repo.users()));
    }

    private void refreshDashboardOnly() {
        if (dashboardMovies != null)
            dashboardMovies.setText(String.valueOf(repo.movies().size()));
        if (dashboardUsers != null)
            dashboardUsers.setText(String.valueOf(repo.users().size()));
        if (dashboardCurrent != null)
            dashboardCurrent.setText(current == null ? "None" : current.getName());
        if (dashboardDataPath != null)
            dashboardDataPath.setText(" Data folder: data/ • Live movies=" + repo.movies().size() + " • Live users="
                    + repo.users().size());
        revalidate();
        repaint();
    }

    /**
     * Reload both CSV files and replace the shared repository data, then refresh
     * every screen.
     */
    private void reloadCsv() {
        try {
            List<Movie> ms = store.loadMovies();
            List<User> us = store.loadUsers();
            if (ms.isEmpty())
                throw new IllegalStateException("movies.csv is empty or missing in the data folder");
            if (us.isEmpty())
                throw new IllegalStateException("users.csv is empty or missing in the data folder");
            int currentId = current == null ? -1 : current.getId();
            repo.replaceData(ms, us);
            current = repo.getUser(currentId);
            if (current == null)
                current = repo.users().stream().findFirst().orElse(null);
            refreshAllViews();
            status.setText(" CSV reloaded successfully • " + repo.movies().size() + " movies • " + repo.users().size()
                    + " users");
        } catch (Exception e) {
            msg("Reload failed: " + e.getMessage());
        }
    }

    private void addDocumentListener(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            private void run() {
                SwingUtilities.invokeLater(action);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                run();
            }
        });
    }

    private JPanel base(String title, String sub) {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JLabel h = new JLabel("<html><h1>" + title + "</h1><div>" + sub + "</div></html>");
        p.add(h, BorderLayout.NORTH);
        return p;
    }

    private JTextArea area() {
        JTextArea a = new JTextArea();
        a.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setMargin(new Insets(12, 12, 12, 12));
        return a;
    }

    private void save() {
        try {
            store.saveMovies(repo.movies());
            store.saveUsers(repo.users());
            refreshDashboardOnly();
            status.setText(" Data saved automatically • " + repo.movies().size() + " movies • " + repo.users().size()
                    + " users");
        } catch (Exception e) {
            status.setText(" Save error: " + e.getMessage());
        }
    }

    private void msg(String s) {
        JOptionPane.showMessageDialog(this, s);
    }
}
