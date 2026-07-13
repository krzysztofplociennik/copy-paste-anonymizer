package com.plociennik.copypasteanonymizer.controller;

import com.plociennik.copypasteanonymizer.common.CopyPasteAnonymizerException;
import com.plociennik.copypasteanonymizer.util.StyleCssUtil;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class NotifyService {

    private Stage toastStage;

    public void showAnonymizationSuccessMessage() {
        Stage toastStage = this.toastStage == null ? initAnonymizationSuccessMessage() : this.toastStage;

        try {
            toastStage.show();

            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished(e -> toastStage.close());
            delay.play();
        } catch (Exception e) {
            throw new CopyPasteAnonymizerException("1412_09072026", "Error while trying to show anonymization success message.", e);
        }
    }

    // todo: some kind of differentiation should be going on - right now I have 2 ways of informing a user what
    //  is happening, the naming scheme maybe should change to reflect that
    public void showNotification(String message, NotificationType type, HBox notificationFooter,
                                 Label notificationIcon, Label notificationText) {
        if (notificationFooter == null || notificationIcon == null || notificationText == null) {
            System.out.println("Notification: " + message);
            return;
        }

        Platform.runLater(() -> {
            notificationText.setText(message);

            notificationFooter.getStyleClass().removeAll("info", "success", "warning", "error");

            switch (type) {
                case SUCCESS -> {
                    notificationIcon.setText("✓");
                    notificationFooter.getStyleClass().add("success");
                }
                case WARNING -> {
                    notificationIcon.setText("!");
                    notificationFooter.getStyleClass().add("warning");
                }
                case ERROR -> {
                    notificationIcon.setText("✗");
                    notificationFooter.getStyleClass().add("error");
                }
                case INFO -> {
                    notificationIcon.setText("i");
                    notificationFooter.getStyleClass().add("info");
                }
                default -> notificationText.setText("•");
            }

            if (type != NotificationType.DEFAULT) {
                PauseTransition pause = new PauseTransition(Duration.seconds(3));
                pause.setOnFinished(e -> returnToDefaultState(notificationFooter, notificationIcon, notificationText));
                pause.play();
            }
        });
    }

    private void returnToDefaultState(HBox notificationFooter, Label notificationIcon, Label notificationText) {
        if (notificationFooter != null) {
            notificationFooter.getStyleClass().removeAll("info", "success", "warning", "error");
            notificationIcon.setText("•");
            notificationText.setText("Ready");
        }
    }

    private Stage initAnonymizationSuccessMessage() {
        Stage toastStage = new Stage();
        toastStage.setResizable(false);
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.setAlwaysOnTop(true);

        Label label = new Label("Text anonymized");
        label.getStyleClass().add("anonymized-label");

        StackPane root = new StackPane(label);
        root.getStyleClass().add("anonymized-overlay");

        Scene scene = new Scene(root);
        scene.getStylesheets().add(StyleCssUtil.getResource());
        scene.setFill(null);

        toastStage.setScene(scene);

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        toastStage.setY(bounds.getMaxY() - 80);
        toastStage.setX(bounds.getMaxX() / 2 - 120);

        this.toastStage = toastStage;

        return this.toastStage;
    }
}
