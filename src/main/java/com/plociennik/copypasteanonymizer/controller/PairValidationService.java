package com.plociennik.copypasteanonymizer.controller;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PairValidationService {

    public boolean arePairsNotValid(VBox pairsContainer) {
        boolean hasErrors = false;
        Map<String, List<String>> valueLocations = new HashMap<>();

        for (int i = 0; i < pairsContainer.getChildren().size(); i++) {
            var node = pairsContainer.getChildren().get(i);
            if (node instanceof VBox pairContainer) {
                HBox row = (HBox) pairContainer.getChildren().getFirst();

                List<TextField> textFields = row.getChildren().stream()
                        .filter(n -> n instanceof TextField)
                        .map(n -> (TextField) n)
                        .toList();

                if (textFields.size() >= 2) {
                    String key = textFields.get(0).getText().trim();
                    String value = textFields.get(1).getText().trim();

                    if (!key.isEmpty()) {
                        valueLocations.computeIfAbsent(key, k -> new ArrayList<>())
                                .add("Pair " + (i + 1) + " (left)");
                    }
                    if (!value.isEmpty()) {
                        valueLocations.computeIfAbsent(value, k -> new ArrayList<>())
                                .add("Pair " + (i + 1) + " (right)");
                    }
                }
            }
        }

        for (int i = 0; i < pairsContainer.getChildren().size(); i++) {
            var node = pairsContainer.getChildren().get(i);
            if (node instanceof VBox pairContainer) {
                HBox row = (HBox) pairContainer.getChildren().get(0);
                Label errorLabel = (Label) pairContainer.getChildren().get(1);

                List<TextField> textFields = row.getChildren().stream()
                        .filter(n -> n instanceof TextField)
                        .map(n -> (TextField) n)
                        .toList();

                if (textFields.size() >= 2) {
                    TextField keyField = textFields.get(0);
                    TextField valueField = textFields.get(1);

                    String key = keyField.getText().trim();
                    String value = valueField.getText().trim();

                    keyField.getStyleClass().remove("error");
                    valueField.getStyleClass().remove("error");
                    row.getStyleClass().remove("has-error");
                    errorLabel.setVisible(false);
                    errorLabel.setManaged(false);

                    String errorMessage = "";

                    boolean isAnyFieldBlank = StringUtils.isBlank(key) || StringUtils.isBlank(value);
                    if (isAnyFieldBlank) {
                        hasErrors = true;
                        errorMessage = "Both fields must be filled.";

                        if (key.isEmpty()) keyField.getStyleClass().add("error");
                        if (value.isEmpty()) valueField.getStyleClass().add("error");
                    }

                    else if (!key.isEmpty() && !value.isEmpty()) {
                        boolean keyDuplicate = valueLocations.get(key).size() > 1;
                        boolean valueDuplicate = valueLocations.get(value).size() > 1;

                        if (keyDuplicate || valueDuplicate) {
                            hasErrors = true;

                            if (keyDuplicate && valueDuplicate) {
                                errorMessage = "Both values are already used elsewhere";
                                keyField.getStyleClass().add("error");
                                valueField.getStyleClass().add("error");
                            } else if (keyDuplicate) {
                                int finalI = i;
                                errorMessage = "Left value '" + key + "' is already used in: " +
                                        String.join(", ", valueLocations.get(key).stream()
                                                .filter(loc -> !loc.equals("Pair " + (finalI + 1) + " (left)"))
                                                .toList());
                                keyField.getStyleClass().add("error");
                            } else {
                                int finalI1 = i;
                                errorMessage = "Right value '" + value + "' is already used in: " +
                                        String.join(", ", valueLocations.get(value).stream()
                                                .filter(loc -> !loc.equals("Pair " + (finalI1 + 1) + " (right)"))
                                                .toList());
                                valueField.getStyleClass().add("error");
                            }
                        }
                    }

                    if (!errorMessage.isEmpty()) {
                        row.getStyleClass().add("has-error");
                        errorLabel.setText(errorMessage);
                        errorLabel.setVisible(true);
                        errorLabel.setManaged(true);
                    }
                }
            }
        }

        return hasErrors;
    }
}
