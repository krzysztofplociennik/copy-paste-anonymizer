package com.plociennik.copypasteanonymizer.services;

import com.plociennik.copypasteanonymizer.enums.NotificationType;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

public class FooterService {

    public void showFooterStatus(String message, NotificationType type, HBox notificationFooter,
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

            System.out.printf("[%s] painting error message", "1108_20102026");
            int pauseDuration = type.equals(NotificationType.ERROR) ? 10 : 5;
            PauseTransition pause = new PauseTransition(Duration.seconds(pauseDuration));
            pause.setOnFinished(e -> returnToDefaultState(notificationFooter, notificationIcon, notificationText));
            pause.play();

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
