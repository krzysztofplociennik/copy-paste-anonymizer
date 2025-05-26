package com.plociennik.copypasteanonymizer;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class StageProcessor {

    public static void setup(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 520, 440);
        stage.setTitle("copy-paste anonymizer");
        stage.setScene(scene);

        HelloController controller = fxmlLoader.getController();

        stage.setOnCloseRequest(event -> {
            controller.handleSavePairs();
            Platform.exit();
        });
    }
}
