package com.github.technus.avrClone.instructions.exceptions;

import com.github.technus.avrClone.instructions.exceptions.EventException;

public class SleepEvent extends EventException {
    public SleepEvent(String message) {
        super(message);
    }

    public SleepEvent(String message, Throwable cause) {
        super(message, cause);
    }
}
