package com.plociennik.copypasteanonymizer.controller;

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

    public void showAnonymizationSuccessMessage() {
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

    public void showNotification(String message, NotificationType type, HBox notificationFooter, Label notificationIcon, Label notificationText) {
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
}
