package se233.project1.view;

import javafx.scene.control.Alert;

import java.util.List;

/** Informative message boxes shown to the user. */
public class Dialogs {

    public static void error(String header, String content) {
        show(Alert.AlertType.ERROR, header, content);
    }

    public static void warning(String header, String content) {
        show(Alert.AlertType.WARNING, header, content);
    }

    public static void info(String header, String content) {
        show(Alert.AlertType.INFORMATION, header, content);
    }

    /** A message followed by a list of details, one per line (e.g. the files that failed). */
    public static void withDetails(Alert.AlertType type, String header, String content, List<String> details) {
        String text = content;
        for (String line : details) {
            text = text + "\n  - " + line;
        }
        show(type, header, text);
    }

    private static void show(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("SE233 Vectorizer");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
