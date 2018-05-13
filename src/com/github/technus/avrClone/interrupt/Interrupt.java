package com.github.technus.avrClone.interrupt;

import com.github.technus.avrClone.AvrCore;

public abstract class Interrupt implements I_Interrupt {
    protected Interrupt(){

    }

    @Override
    public void setTrigger(AvrCore core, boolean value) {
        if(value){
            setTrigger(core);
        }else {
            clearTrigger(core);
        }
    }
}
