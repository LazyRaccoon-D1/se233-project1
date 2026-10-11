package se233.project1;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import se233.project1.controller.AppController;
import se233.project1.view.MainView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Launcher extends Application {
    private AppController controller;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        MainView view = new MainView();
        Scene scene = new Scene(view, 1320, 840);
        scene.getStylesheets().add(Launcher.class.getResource("style.css").toExternalForm());
        controller = new AppController(stage, getHostServices(), view, scene);

        stage.setTitle("SE233 Vectorizer - PNG/JPG to SVG");
        stage.setMinWidth(1000);
        stage.setMinHeight(660);
        stage.setScene(scene);
        stage.show();

        // files passed on the command line are opened as if they were dropped
        List<File> files = new ArrayList<>();
        for (String arg : getParameters().getRaw()) {
            File file = new File(arg);
            if (file.exists()) {
                files.add(file);
            }
        }
        if (!files.isEmpty()) {
            controller.handleFiles(files);
        }
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }
}
