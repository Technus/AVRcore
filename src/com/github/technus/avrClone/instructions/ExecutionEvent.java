package com.github.technus.avrClone.instructions;

import com.github.technus.avrClone.AvrCore;

import java.util.Arrays;

public class ExecutionEvent {
    public final I_Instruction instruction;
    private AvrCore core;
    public int[] data;
    public final int programCounter;

    /**
     * should adjust PC
     * @param core
     * @param instruction
     * @param data
     */
    public ExecutionEvent(AvrCore core, int newProgramCounter, I_Instruction instruction, int... data){
        this.core=core;
        programCounter=core.programCounter;
        core.programCounter=newProgramCounter;
        this.instruction=instruction;
        this.data=data;
    }

    @Override
    public String toString() {
        return programCounter+" ! "+ instruction.name()+" "+ Arrays.toString(data);
    }
}
