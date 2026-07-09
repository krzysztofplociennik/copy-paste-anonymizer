package com.plociennik.copypasteanonymizer.common;

public class CopyPasteAnonymizerException extends RuntimeException {
    public CopyPasteAnonymizerException(String errorID, String message) {
        super("(%s) %s".formatted(errorID, message));
    }

    private DetailedError getDetailedErrorResponse(Exception e) {
        // todo
        return null;
    }
}
