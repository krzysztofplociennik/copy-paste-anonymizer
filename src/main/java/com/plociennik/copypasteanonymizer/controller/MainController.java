package com.plociennik.copypasteanonymizer.controller;

import com.plociennik.copypasteanonymizer.clipboard.SimpleClipboardMonitor;
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
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MainController {

    private final List<Pair<String, String>> replacementPairs = new ArrayList<>();
    private final AtomicReference<String> lastProcessedContent = new AtomicReference<>("");
    private SimpleClipboardMonitor clipboardMonitor;

    @FXML
    private VBox pairsContainer;

    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        handleLoadPairs();
        showStatus("App started. Loaded saved pairs.");
        startClipboardMonitor();
    }

    @FXML
    private void handleAddPair() {
        addPair("", "");
        showStatus("Added a new empty pair.");
    }

    private void addPair(String key, String value) {
        HBox row = new HBox(10);
        row.getStyleClass().add("pair-row");

        TextField keyField = new TextField(key);
        keyField.setPromptText("Key");
        keyField.getStyleClass().add("text-field");

        TextField valueField = new TextField(value);
        valueField.setPromptText("Value");
        valueField.getStyleClass().add("text-field");

        Button removeBtn = new Button("❌");
        removeBtn.getStyleClass().addAll("button", "remove-button");
        removeBtn.setOnAction(e -> {
            pairsContainer.getChildren().remove(row);
            showStatus("Removed a pair.");
        });

        row.getChildren().addAll(keyField, valueField, removeBtn);
        pairsContainer.getChildren().add(row);
    }

    @FXML
    public void handleSavePairs() {
        replacementPairs.clear();

        List<String> pairs = new ArrayList<>();

        for (var pairRow : pairsContainer.getChildren()) {
            if (pairRow instanceof HBox row) {

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

        try (PrintWriter writer = new PrintWriter("pairs.txt")) {
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
        if (!file.exists())
            return;

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

    private void startClipboardMonitor() {
        clipboardMonitor = new SimpleClipboardMonitor(this::onClipboardChanged);
        clipboardMonitor.start();
        showStatus("Clipboard monitoring started");
    }

    private void onClipboardChanged(String content) {
        System.out.println("Processing clipboard content: " + content.substring(0, Math.min(30, content.length())) + "...");

        if (content.equals(lastProcessedContent.get())) {
            System.out.println("Skipping - this is content we just set");
            return;
        }

        String replacedContent = applyReplacements(content);

        if (!replacedContent.equals(content)) {
            System.out.println("Content will be replaced");

            lastProcessedContent.set(replacedContent);

            clipboardMonitor.setProcessing(true);

            Thread updater = new Thread(() -> {
                try {
                    Thread.sleep(100);

                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    StringSelection selection = new StringSelection(replacedContent);
                    clipboard.setContents(selection, null);

                    Platform.runLater(() -> {
                        showStatus("Clipboard content anonymized");
                        showAnnotation("Text anonymized");
                    });

                    Thread.sleep(200);

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showStatus("Error updating clipboard"));
                } finally {
                    clipboardMonitor.setProcessing(false);
                }
            });

            updater.setDaemon(true);
            updater.setName("ClipboardUpdater");
            updater.start();
        } else {
            System.out.println("No replacement needed");
            lastProcessedContent.set(content);
        }
    }

    private String applyReplacements(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;
        for (Pair<String, String> pair : replacementPairs) {
            String key = pair.getKey();
            String value = pair.getValue();

            boolean leftFound = StringUtils.isNotBlank(key) && input.contains(key);
            boolean rightFound = StringUtils.isNotBlank(value) && input.contains(value);
            if (leftFound) {
                String oldResult = result;
                result = result.replace(key, value);
                if (!oldResult.equals(result)) {
                    System.out.println("Applied replacement: '" + key + "' -> '" + value + "'");
                }
            } else if (rightFound) {
                String oldResult = result;
                result = result.replace(value, key);
                if (!oldResult.equals(result)) {
                    System.out.println("Applied replacement: '" + value + "' -> '" + key + "'");
                }
            }
        }
        return result;
    }

    private void showAnnotation(String message) {
        try {
            Stage toastStage = new Stage();
            toastStage.setResizable(false);
            toastStage.initStyle(StageStyle.TRANSPARENT);
            toastStage.setAlwaysOnTop(true);

            Label label = new Label(message);
            label.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 5;");
            StackPane root = new StackPane(label);
            root.setStyle("-fx-background-color: transparent;");

            Scene scene = new Scene(root);
            scene.setFill(null);
            toastStage.setScene(scene);

            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            toastStage.setX(bounds.getMaxX() - 250);
            toastStage.setY(bounds.getMaxY() - 80);

            toastStage.show();

            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished(e -> toastStage.close());
            delay.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (clipboardMonitor != null) {
            clipboardMonitor.stop();
        }
    }
}