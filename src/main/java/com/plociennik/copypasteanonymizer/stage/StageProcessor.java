package com.plociennik.copypasteanonymizer.stage;

import com.plociennik.copypasteanonymizer.CopyPasteAnonymizerApplication;
import com.plociennik.copypasteanonymizer.controller.MainController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class StageProcessor {

    public static void setup(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(CopyPasteAnonymizerApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 520, 440);
        stage.setTitle("Copy-Paste Anonymizer");
        stage.setScene(scene);
        stage.setIconified(false);

        stage.setMinWidth(600);
        stage.setMinHeight(400);

        scene.getStylesheets().add(
                CopyPasteAnonymizerApplication.class.getResource("style.css").toExternalForm()
        );

        stage.getIcons().add(
                new Image(Objects.requireNonNull(StageProcessor.class.getResourceAsStream("/com/plociennik/copypasteanonymizer/icons/unknown.png")))
        );

        setOnCloseRequest(stage, fxmlLoader);
    }

    private static void setOnCloseRequest(Stage stage, FXMLLoader fxmlLoader) {
        stage.setOnCloseRequest(event -> {
            MainController controller = fxmlLoader.getController();
            controller.handleSavePairs();
            controller.shutdown();
            Platform.exit();
            System.exit(0);
        });
    }
}