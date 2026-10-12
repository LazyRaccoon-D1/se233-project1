package se233.project1;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import se233.project1.controller.AppController;
import se233.project1.view.MainView;

public class Launcher extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        MainView view = new MainView();
        Scene scene = new Scene(view, 1320, 840);
        scene.getStylesheets().add(Launcher.class.getResource("style.css").toExternalForm());
        AppController controller = new AppController(stage, getHostServices(), view, scene);

        // stop the background work when the window is closed
        stage.setOnCloseRequest(e -> controller.shutdown());
        stage.setTitle("SE233 Vectorizer - PNG/JPG to SVG");
        stage.setMinWidth(1000);
        stage.setMinHeight(660);
        stage.setScene(scene);
        stage.show();
    }
}
