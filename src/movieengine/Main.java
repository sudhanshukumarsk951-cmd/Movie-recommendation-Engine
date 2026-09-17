package movieengine;

import javax.swing.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Path data = Paths.get("data");
                CsvDataStore store = new CsvDataStore(data);
                List<Movie> ms = store.loadMovies();
                List<User> us = store.loadUsers();
                if (ms.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "movies.csv is empty or missing in the data folder.");
                    return;
                }
                if (us.isEmpty()) {
                    us.add(new User(1, "Demo User"));
                    store.saveUsers(us);
                }
                MovieRepository repo = new MovieRepository(ms, us);
                MainFrame f = new MainFrame(repo, store);
                f.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Startup error: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
