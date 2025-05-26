package com.plociennik.copypasteanonymizer;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.Pair;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class HelloController {

    private final List<Pair<String, String>> replacementPairs = new ArrayList<>();

    @FXML
    private VBox pairsContainer;

    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        handleLoadPairs();
        showStatus("App started. Loaded saved pairs.");
        startClipboardWatcher();
    }

    @FXML
    private void handleAddPair() {
        addPair("", "");
        showStatus("Added a new empty pair.");
    }

    private void addPair(String key, String value) {
        HBox row = new HBox(10);

        TextField keyField = new TextField(key);
        keyField.setPromptText("Key");

        TextField valueField = new TextField(value);
        valueField.setPromptText("Value");

        Button removeBtn = new Button("❌");
        removeBtn.setOnAction(e -> {
            pairsContainer.getChildren().remove(row);
            showStatus("Removed a pair.");
        });

        row.getChildren().addAll(keyField, valueField, removeBtn);
        pairsContainer.getChildren().add(row);
    }

    @FXML
    void handleSavePairs() {
        replacementPairs.clear(); // rebuild model list

        List<String> pairs = new ArrayList<>();

        for (var node : pairsContainer.getChildren()) {
            if (node instanceof HBox row) {
                List<TextField> textFields = row.getChildren().stream()
                        .filter(n -> n instanceof TextField)
                        .map(n -> (TextField) n)
                        .toList();

                if (textFields.size() >= 2) {
                    String key = textFields.get(0).getText().trim();
                    String value = textFields.get(1).getText().trim();
                    if (!key.isEmpty() || !value.isEmpty()) {
                        replacementPairs.add(new Pair<>(key, value));
                        pairs.add(key + " = " + value);
                    }
                }
            }
        }

        try (PrintWriter writer = new PrintWriter(new File("pairs.txt"))) {
            for (String pair : pairs) {
                writer.println(pair);
            }
            showStatus("Saved " + pairs.size() + " pairs.");
        } catch (IOException e) {
            e.printStackTrace();
            showStatus("Error saving pairs.");
        }
    }


    private void handleLoadPairs() {
        File file = new File("pairs.txt");
        if (!file.exists()) return;

        List<String> lines;
        try {
            lines = Files.readAllLines(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        pairsContainer.getChildren().clear();
        replacementPairs.clear();

        for (String line : lines) {
            String[] parts = line.split("=", 2);
            String key = parts.length > 0 ? parts[0].trim() : "";
            String value = parts.length > 1 ? parts[1].trim() : "";
            addPair(key, value);
            replacementPairs.add(new Pair<>(key, value));
        }
    }


    private void showStatus(String message) {
        if (statusLabel == null) {
            System.out.println("Status label is null");
            return;
        }

        statusLabel.setText(message);

        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> statusLabel.setText(""));
        pause.play();
    }

    private void startClipboardWatcher() {
        Thread watcher = new Thread(() -> {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            String lastContent = "";

            while (true) {
                try {
                    Transferable contents = clipboard.getContents(null);

                    // Only process plain text
                    if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        String content = (String) contents.getTransferData(DataFlavor.stringFlavor);

                        if (!content.equals(lastContent)) {
                            lastContent = content;

                            String finalContent = content;

                            // Delay clipboard change by 300ms
                            new Thread(() -> {
                                try {
                                    Thread.sleep(300); // let source app finish writing
                                    String replaced = applyReplacements(finalContent);
                                    if (!replaced.equals(finalContent)) {
                                        StringSelection selection = new StringSelection(replaced);
                                        clipboard.setContents(selection, null);
                                        Platform.runLater(() -> {
                                            showStatus("Clipboard content modified");
                                            showAnnotation("Clipboard modified");
                                        });
                                    }
                                } catch (InterruptedException ignored) {}
                            }).start();
                        }
                    }

                    Thread.sleep(300); // ~3 checks per second
                } catch (UnsupportedFlavorException | IOException e) {
                    // Ignore unsupported flavors like IntelliJ's serialized Java objects
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        watcher.setDaemon(true);
        watcher.start();
    }



    private String applyReplacements(String input) {
        for (Pair<String, String> pair : replacementPairs) {
            if (!pair.getKey().isEmpty()) {
                input = input.replace(pair.getKey(), pair.getValue());
            }
        }
        return input;
    }


    private void showAnnotation(String message) {
        Stage toastStage = new Stage();
        toastStage.initOwner(pairsContainer.getScene().getWindow());
        toastStage.setResizable(false);
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.setAlwaysOnTop(true);

        Label label = new Label(message);
        label.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 5;");
        StackPane root = new StackPane(label);
        root.setStyle("-fx-background-color: transparent;");
        root.setOpacity(0.85);

        Scene scene = new Scene(root);
        scene.setFill(null);
        toastStage.setScene(scene);

        // Position bottom right
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        toastStage.setX(bounds.getMaxX() - 300);
        toastStage.setY(bounds.getMaxY() - 100);

        toastStage.show();

        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> toastStage.close());
        delay.play();
    }




}
