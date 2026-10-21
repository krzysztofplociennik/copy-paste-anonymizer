package com.plociennik.copypasteanonymizer.services;

import com.plociennik.copypasteanonymizer.common.CopyPasteAnonymizerException;
import com.plociennik.copypasteanonymizer.enums.NotificationType;
import com.plociennik.copypasteanonymizer.util.StyleCssUtil;

import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class NotifyService {

    private Stage toastStage;

    public void showAnonymizationSuccessMessage() {
        this.toastStage = null;
        Stage toastStage = createToastStage(NotificationType.SUCCESS, "Content anonymized");
        try {
            toastStage.show();
            centerStage(toastStage);
            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished(e -> toastStage.close());
            delay.play();
        } catch (Exception e) {
            throw new CopyPasteAnonymizerException("1412_09072026", "Error while trying to show anonymization success message.", e);
        }
    }

    public void showAnonymizationFailureMessage() {
        this.toastStage = null;
        Stage toastStage = createToastStage(NotificationType.ERROR, "Can't apply replacements - content is mixed");
        try {
            toastStage.show();
            centerStage(toastStage);
            PauseTransition delay = new PauseTransition(Duration.seconds(7));
            delay.setOnFinished(e -> toastStage.close());
            delay.play();
        } catch (Exception e) {
            throw new CopyPasteAnonymizerException("1158_20102026", "Error while trying to show anonymization failure message.", e);
        }
    }

    private Stage createToastStage(NotificationType type, String message) {
        Stage toastStage = new Stage();
        toastStage.setResizable(false);
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.setAlwaysOnTop(true);

        Label label = new Label(message);

        String labelClassName = type.equals(NotificationType.SUCCESS) ? "anonymized-label" : "anonymized-fail-label";
        label.getStyleClass().add(labelClassName);

        String overlayClassName = type.equals(NotificationType.SUCCESS) ? "anonymized-overlay" : "anonymized-fail-overlay";
        StackPane root = new StackPane(label);
        root.getStyleClass().add(overlayClassName);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(StyleCssUtil.getResource());
        scene.setFill(null);

        toastStage.setScene(scene);

        toastStage.centerOnScreen();

        this.toastStage = toastStage;

        return this.toastStage;
    }

    private void centerStage(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.centerOnScreen();
        ReadOnlyDoubleProperty height = stage.heightProperty();
        stage.setY(bounds.getHeight() / 2 + (height.doubleValue()) / 2);
    }
}
