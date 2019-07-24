package com.github.technus.avrClone.instructions.exceptions;

public class UnhandledEvent extends Exception {
    public UnhandledEvent(String message) {
        super(message);
    }

    public UnhandledEvent(String message, Throwable cause) {
        super(message, cause);
    }
}
