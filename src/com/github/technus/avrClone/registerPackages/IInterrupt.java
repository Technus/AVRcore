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
    void setTrigger(AvrCore core, boolean value);

    /**
     * Assuming that SREG.I is always SET!, otherwise it is not called, should clear it's interrupt request flag
     * @param core
     * @return
     */
    default boolean tryInterrupt(AvrCore core){
        if(getTrigger(core)){
            setTrigger(core,false);
            return true;
        }
        return false;
    }
}
