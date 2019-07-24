package com.github.technus.avrClone.instructions;

import java.util.Arrays;

public class ExecutionEvent {
    public final I_Instruction instruction;
    public final int[] data;
    public final int programCounter;
    public final Throwable throwable;

    public ExecutionEvent(int programCounter, I_Instruction instruction,Throwable throwable, int... data){
        this.programCounter=programCounter;
        this.instruction=instruction;
        this.data=data;
        this.throwable =throwable;
    }

    @Override
    public String toString() {
        return programCounter+" ! "+ instruction.name()+" "+ Arrays.toString(data);
    }
}
