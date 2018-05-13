package com.github.technus.avrClone.instructions;

import com.github.technus.avrClone.AvrCore;
import com.github.technus.avrClone.memory.program.ProgramException;
import com.github.technus.avrClone.memory.program.ProgramMemory;

public interface I_Instruction {
    String name();

    /**
     * must check if the current operands and adresses are valid
     * @param core
     * @param programMemory
     * @param addr
     * @param immersive
     * @param operandsReturn
     * @param values Instruction, Operand 0, Operand 1
     * @return
     */
    void compile(AvrCore core, ProgramMemory programMemory, int addr, boolean immersive, int[] operandsReturn, String[] values) throws ProgramException;

    /**
     * must check if the space that you want to acces EXISTS (in case of indirect operations)
     * {#link com.github.technus.avrClone.AvrCore#isDataAddressValid(int) isDataAddressValid}
     * @param core
     */
    ExecutionEvent execute(AvrCore core);
    int getClockCycles(AvrCore core);
    boolean isImmersive();
    int getOperandCount();

    OperandLimit getLimit0();
    OperandLimit getLimit1();
}
