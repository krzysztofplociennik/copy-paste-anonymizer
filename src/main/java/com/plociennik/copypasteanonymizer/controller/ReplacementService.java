package com.plociennik.copypasteanonymizer.controller;

import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ReplacementService {

    public String applyReplacements(String input, ReplacementMode mode, List<Pair<String, String>> replacementPairs) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;

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
}
