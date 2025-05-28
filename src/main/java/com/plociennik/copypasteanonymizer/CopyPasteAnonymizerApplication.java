package com.plociennik.copypasteanonymizer;

import com.plociennik.copypasteanonymizer.stage.StageProcessor;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class CopyPasteAnonymizerApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        StageProcessor.setup(stage);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}