package com.github.technus.avrClone.interrupt;

import com.github.technus.avrClone.AvrCore;
import com.github.technus.avrClone.registerPackages.I_Register;

public interface I_Interrupt extends I_Register {
    int getVector();
    boolean getTrigger(AvrCore core);
    void setTrigger(AvrCore core, boolean value);
    void setTrigger(AvrCore core);
    void clearTrigger(AvrCore core);

    /**
     * Assuming that SREG.I is always SET!, otherwise it is not called, should clear it's interrupt request flag
     * @param core
     * @return
     */
    boolean tryInterrupt(AvrCore core);
}
