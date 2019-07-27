package com.github.technus.avrClone.registerPackages;

import com.github.technus.avrClone.AvrCore;

public interface IInterrupt extends IRegister {
    @Override
    @Deprecated
    default int getAddress(IRegisterPackage registerPackage) {
        return getVector();
    }

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
