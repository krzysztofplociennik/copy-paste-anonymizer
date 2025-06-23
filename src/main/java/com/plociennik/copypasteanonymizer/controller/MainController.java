package com.plociennik.copypasteanonymizer.controller;

import com.plociennik.copypasteanonymizer.clipboard.SimpleClipboardMonitor;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

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

    private PairValidationService pairValidationService;
    private ReplacementService replacementService;
    private NotifyService notifyService;

    public MainController() {
        this.pairValidationService = new PairValidationService();
        this.replacementService = new ReplacementService();
        this.notifyService = new NotifyService();
    }


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
                this.notifyService.showNotification("Replacement mode: " + getModeDescription(mode), NotificationType.INFO, notificationFooter, notificationIcon, notificationText);
            }
        });

        handleLoadPairs();
        this.notifyService.showNotification("App started. Loaded saved pairs.", NotificationType.SUCCESS, notificationFooter, notificationIcon, notificationText);
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
            monitoringToggle.setText("Resume");
            monitoringToggle.getStyleClass().removeAll("active");
            monitoringToggle.getStyleClass().add("paused");
            this.notifyService.showNotification("Clipboard monitoring paused", NotificationType.WARNING, notificationFooter, notificationIcon, notificationText);
        } else {
            monitoringToggle.setText("Pause");
            monitoringToggle.getStyleClass().removeAll("paused");
            monitoringToggle.getStyleClass().add("active");
            notifyService.showNotification("Clipboard monitoring resumed", NotificationType.SUCCESS, notificationFooter, notificationIcon, notificationText);
        }
    }

    @FXML
    private void handleAddPair() {
        boolean arePairsNotValid = this.pairValidationService.arePairsNotValid(pairsContainer);
        if (arePairsNotValid) {
            notifyService.showNotification("Please fix the errors before adding a new pair.", NotificationType.ERROR, notificationFooter, notificationIcon, notificationText);
            return;
        }

        addPair("", "");
        notifyService.showNotification("Added a new empty pair.", NotificationType.INFO, notificationFooter, notificationIcon, notificationText);
    }

    private void addPair(String key, String value) {
        VBox pairContainer = new VBox(5);
        pairContainer.getStyleClass().add("pair-container");

        HBox row = new HBox(10);
        row.getStyleClass().add("pair-row");

        TextField keyField = new TextField(key);
        keyField.setPromptText("Original Text");
        keyField.getStyleClass().add("text-field");

        TextField valueField = new TextField(value);
        valueField.setPromptText("Replacement Text");
        valueField.getStyleClass().add("text-field");

        Button removeBtn = new Button("❌");
        removeBtn.getStyleClass().addAll("button", "remove-button");
        removeBtn.setOnAction(e -> {
            pairsContainer.getChildren().remove(pairContainer);
            notifyService.showNotification("Removed a pair.", NotificationType.WARNING, notificationFooter, notificationIcon, notificationText);
        });

        row.getChildren().addAll(keyField, valueField, removeBtn);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-message");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        pairContainer.getChildren().addAll(row, errorLabel);
        pairsContainer.getChildren().add(pairContainer);
    }

    @FXML
    public void handleSavePairs() {
        boolean arePairsValid = this.pairValidationService.arePairsNotValid(pairsContainer);
        if (arePairsValid) {
            notifyService.showNotification("Please fix the errors before saving.", NotificationType.ERROR, notificationFooter, notificationIcon, notificationText);
            return;
        }

        replacementPairs.clear();

        List<String> pairs = new ArrayList<>();

        for (var pairRow : pairsContainer.getChildren()) {
            if (pairRow instanceof VBox row) {
                ObservableList<Node> children = row.getChildren();
                if (children.getFirst() instanceof HBox box) {
                    List<TextField> textFields = box.getChildren().stream()
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
        }

        try (PrintWriter writer = new PrintWriter("pairs.txt")) {
            for (String pair : pairs) {
                writer.println(pair);
            }
            notifyService.showNotification("Successfully saved " + pairs.size() + " pairs.", NotificationType.SUCCESS, notificationFooter, notificationIcon, notificationText);
        } catch (IOException e) {
            e.printStackTrace();
            notifyService.showNotification("Error saving pairs: " + e.getMessage(), NotificationType.ERROR, notificationFooter, notificationIcon, notificationText);
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
        notifyService.showNotification("Clipboard monitoring started", NotificationType.SUCCESS, notificationFooter, notificationIcon, notificationText);
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

        String replacedContent = this.replacementService.applyReplacements(content, getCurrentReplacementMode(), replacementPairs);

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
                        notifyService.showNotification("Clipboard content anonymized", NotificationType.SUCCESS, notificationFooter, notificationIcon, notificationText);
                        notifyService.showAnonymizationSuccessMessage();
                    });

                    Thread.sleep(200);

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> notifyService.showNotification("Error updating clipboard", NotificationType.ERROR, notificationFooter, notificationIcon, notificationText));
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

    public void shutdown() {
        if (clipboardMonitor != null) {
            clipboardMonitor.stop();
        }
    }
}