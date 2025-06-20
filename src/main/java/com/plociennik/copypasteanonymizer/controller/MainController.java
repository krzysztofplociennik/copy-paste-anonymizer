package com.plociennik.copypasteanonymizer.controller;

import com.plociennik.copypasteanonymizer.clipboard.SimpleClipboardMonitor;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
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
    private HBox notificationFooter;

    @FXML
    private Label notificationIcon;

    @FXML
    private Label notificationText;

    @FXML
    private RadioButton leftToRightMode;

    @FXML
    private RadioButton rightToLeftMode;

    @FXML
    private RadioButton bidirectionalMode;

    @FXML
    private Button monitoringToggle;

    private ToggleGroup replacementModeGroup;
    private boolean isMonitoringPaused = false;


    @FXML
    private void initialize() {
        replacementModeGroup = new ToggleGroup();
        leftToRightMode.setToggleGroup(replacementModeGroup);
        rightToLeftMode.setToggleGroup(replacementModeGroup);
        bidirectionalMode.setToggleGroup(replacementModeGroup);

        bidirectionalMode.setSelected(true);

        replacementModeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                ReplacementMode mode = getCurrentReplacementMode();
                showNotification("Replacement mode: " + getModeDescription(mode), NotificationType.INFO);
            }
        });

        handleLoadPairs();
        showNotification("App started. Loaded saved pairs.", NotificationType.SUCCESS);
        startClipboardMonitor();
    }

    private ReplacementMode getCurrentReplacementMode() {
        if (leftToRightMode.isSelected()) {
            return ReplacementMode.LEFT_TO_RIGHT;
        } else if (rightToLeftMode.isSelected()) {
            return ReplacementMode.RIGHT_TO_LEFT;
        } else {
            return ReplacementMode.BIDIRECTIONAL;
        }
    }

    private String getModeDescription(ReplacementMode mode) {
        switch (mode) {
            case LEFT_TO_RIGHT:
                return "Left → Right only";
            case RIGHT_TO_LEFT:
                return "Right → Left only";
            case BIDIRECTIONAL:
                return "Bidirectional";
            default:
                return "Unknown";
        }
    }

    @FXML
    private void toggleMonitoring() {
        isMonitoringPaused = !isMonitoringPaused;

        if (isMonitoringPaused) {
            monitoringToggle.setText("▶️ Resume");
            monitoringToggle.getStyleClass().removeAll("active");
            monitoringToggle.getStyleClass().add("paused");
            showNotification("Clipboard monitoring paused", NotificationType.WARNING);
        } else {
            monitoringToggle.setText("⏸️ Pause");
            monitoringToggle.getStyleClass().removeAll("paused");
            monitoringToggle.getStyleClass().add("active");
            showNotification("Clipboard monitoring resumed", NotificationType.SUCCESS);
        }
    }

    @FXML
    private void handleAddPair() {
        addPair("", "");
        showNotification("Added a new empty pair.", NotificationType.INFO);
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
            showNotification("Removed a pair.", NotificationType.WARNING);
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
            showNotification("Successfully saved " + pairs.size() + " pairs.", NotificationType.SUCCESS);;
        } catch (IOException e) {
            e.printStackTrace();
            showNotification("Error saving pairs: " + e.getMessage(), NotificationType.ERROR);
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

    private void startClipboardMonitor() {
        clipboardMonitor = new SimpleClipboardMonitor(this::onClipboardChanged);
        clipboardMonitor.start();
        showNotification("Clipboard monitoring started", NotificationType.SUCCESS);
    }

    private void onClipboardChanged(String content) {
        if (isMonitoringPaused) {
            return;
        }

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
                        showNotification("Clipboard content anonymized", NotificationType.SUCCESS);
                        showAnonymizationSuccessMessage();
                    });

                    Thread.sleep(200);

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showNotification("Error updating clipboard", NotificationType.ERROR));
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
        ReplacementMode mode = getCurrentReplacementMode();

        for (Pair<String, String> pair : replacementPairs) {
            String key = pair.getKey();
            String value = pair.getValue();

            if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
                continue;
            }

            switch (mode) {
                case LEFT_TO_RIGHT:
                    if (input.contains(key)) {
                        String oldResult = result;
                        result = result.replace(key, value);
                        if (!oldResult.equals(result)) {
                            System.out.println("Applied LEFT→RIGHT replacement: '" + key + "' → '" + value + "'");
                        }
                    }
                    break;

                case RIGHT_TO_LEFT:
                    if (input.contains(value)) {
                        String oldResult = result;
                        result = result.replace(value, key);
                        if (!oldResult.equals(result)) {
                            System.out.println("Applied RIGHT→LEFT replacement: '" + value + "' → '" + key + "'");
                        }
                    }
                    break;

                case BIDIRECTIONAL:
                    boolean leftFound = input.contains(key);
                    boolean rightFound = input.contains(value);

                    if (leftFound) {
                        String oldResult = result;
                        result = result.replace(key, value);
                        if (!oldResult.equals(result)) {
                            System.out.println("Applied BIDIRECTIONAL replacement: '" + key + "' → '" + value + "'");
                        }
                    } else if (rightFound) {
                        String oldResult = result;
                        result = result.replace(value, key);
                        if (!oldResult.equals(result)) {
                            System.out.println("Applied BIDIRECTIONAL replacement: '" + value + "' → '" + key + "'");
                        }
                    }
                    break;
            }
        }
        return result;
    }

    private void showAnonymizationSuccessMessage() {
        try {
            Stage toastStage = new Stage();
            toastStage.setResizable(false);
            toastStage.initStyle(StageStyle.TRANSPARENT);
            toastStage.setAlwaysOnTop(true);

            Label label = new Label("Text anonymized");
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

    private void showNotification(String message, NotificationType type) {
        if (notificationFooter == null || notificationIcon == null || notificationText == null) {
            System.out.println("Notification: " + message);
            return;
        }

        Platform.runLater(() -> {
            notificationText.setText(message);

            notificationFooter.getStyleClass().removeAll("info", "success", "warning", "error");

            switch (type) {
                case SUCCESS:
                    notificationIcon.setText("✓");
                    notificationFooter.getStyleClass().add("success");
                    break;
                case WARNING:
                    notificationIcon.setText("!");
                    notificationFooter.getStyleClass().add("warning");
                    break;
                case ERROR:
                    notificationIcon.setText("✗");
                    notificationFooter.getStyleClass().add("error");
                    break;
                case INFO:
                    notificationIcon.setText("i");
                    notificationFooter.getStyleClass().add("info");
                    break;
                case DEFAULT:
                default:
                    notificationIcon.setText("•");
                    break;
            }

            if (type != NotificationType.DEFAULT) {
                PauseTransition pause = new PauseTransition(Duration.seconds(3));
                pause.setOnFinished(e -> returnToDefaultState());
                pause.play();
            }
        });
    }

    private void returnToDefaultState() {
        if (notificationFooter != null) {
            notificationFooter.getStyleClass().removeAll("info", "success", "warning", "error");
            notificationIcon.setText("•");
            notificationText.setText("Ready");
        }
    }
}