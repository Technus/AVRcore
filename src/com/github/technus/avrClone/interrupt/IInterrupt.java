package com.github.technus.avrClone.interrupt;

import com.github.technus.avrClone.AvrCore;
import com.github.technus.avrClone.registerPackages.IRegister;

public interface IInterrupt extends IRegister {
    int getVector();
    boolean getTrigger(AvrCore core);
    default void setTrigger(AvrCore core, boolean value){
        if(value){
            setTrigger(core);
        }else {
            clearTrigger(core);
        }
    }
    void setTrigger(AvrCore core);
    void clearTrigger(AvrCore core);

    /**
     * Assuming that SREG.I is always SET!, otherwise it is not called, should clear it's interrupt request flag
     * @param core
     * @return
     */
    boolean tryInterrupt(AvrCore core);
}