package com.github.technus.avrClone.memory.program;

import com.github.technus.avrClone.AvrCore;
import com.github.technus.avrClone.instructions.I_Instruction;
import com.github.technus.avrClone.instructions.InstructionRegistry;

import java.io.PrintStream;

public class ProgramMemory implements Cloneable{
    public final int[] instructions, param0, param1;
    public final InstructionRegistry registry;

    public ProgramMemory(AvrCore core, String... lines) throws Exception{
        this.registry=core.getInstructionRegistry();
        instructions=new int[lines.length];
        param0 =new int[lines.length];
        param1 =new int[lines.length];

        compile(core, core.isUsingImmersiveOperands(), lines);
    }

    public ProgramMemory(AvrCore core, int size) {
        this.registry=core.getInstructionRegistry();
        instructions=new int[size];
        param0 =new int[size];
        param1 =new int[size];
    }

    private ProgramMemory(InstructionRegistry registry,int size){
        this.registry=registry;
        instructions=new int[size];
        param0 =new int[size];
        param1 =new int[size];
    }

    public void print(PrintStream printStream){
        for(int i=0;i<instructions.length;i++){
            I_Instruction instruction= registry.getInstruction(instructions[i]);
            switch (instruction.getOperandCount()){
                case 0:
                    printStream.println(i+" : " +instruction.name());
                    break;
                case 1:
                    printStream.println(i+" : " +instruction.name()+" "+param0[i]);
                    break;
                case 2:
                    printStream.println(i+" : " +instruction.name()+" "+param0[i]+","+ param1[i]);
                    break;
            }
        }
    }

    public String getProgram(int radix){
        String prefix="";
        switch (radix){
            case 2:prefix="0b";break;
            case 8:prefix="0";break;
            case 16:prefix="0x";break;
        }
        StringBuilder stringBuilder=new StringBuilder();
        for(int i=0;i<instructions.length;i++){
            I_Instruction instruction= registry.getInstruction(instructions[i]);
            switch (instruction.getOperandCount()){
                case 0:
                    stringBuilder.append(instruction.name());
                    break;
                case 1:
                    stringBuilder.append(instruction.name()).append(' ');
                    stringBuilder.append(prefix).append(Integer.toString(param0[i],radix));
                    break;
                case 2:
                    stringBuilder.append(instruction.name()).append(' ');
                    stringBuilder.append(prefix).append(Integer.toString(param0[i],radix)).append(',');
                    stringBuilder.append(prefix).append(Integer.toString(param1[i],radix));
                    break;
            }
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }

    public String getProgramWithLineNumbers(int radix,int radixLines){
        String prefix="";
        switch (radix){
            case 2:prefix="0b";break;
            case 8:prefix="0";break;
            case 16:prefix="0x";break;
        }
        String prefixLines="";
        switch (radixLines){
            case 2:prefixLines="0b";break;
            case 8:prefixLines="0";break;
            case 16:prefixLines="0x";break;
        }
        StringBuilder stringBuilder=new StringBuilder();
        int lenOfLineNumbers=Integer.toString(instructions.length-1,radixLines).length();
        for(int i=0;i<instructions.length;i++){
            I_Instruction instruction= registry.getInstruction(instructions[i]);
            stringBuilder.append(prefixLines).append(String.format("%1$-"+lenOfLineNumbers+"s",Integer.toString(i,radixLines))).append(" : ");
            switch (instruction.getOperandCount()){
                case 0:
                    stringBuilder.append(instruction.name());
                    break;
                case 1:
                    stringBuilder.append(instruction.name()).append(' ');
                    stringBuilder.append(prefix).append(Integer.toString(param0[i],radix));
                    break;
                case 2:
                    stringBuilder.append(instruction.name()).append(' ');
                    stringBuilder.append(prefix).append(Integer.toString(param0[i],radix)).append(',');
                    stringBuilder.append(prefix).append(Integer.toString(param1[i],radix));
                    break;
            }
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }

    private void compile(AvrCore core, boolean immersiveOperands, String... lines) throws Exception {
        int i = 0;
        int[] operandsReturn = new int[2];
        try {
            for (; i < lines.length; i++) {
                String[] values = lines[i].replaceAll("^.*:\\s*","").replaceAll("\\s*;.*$","").replaceAll("\\s*,\\s*"," ").split(" ");
                Integer id=registry.getId(values[0].toUpperCase());
                if (id == null) {
                    throw new InvalidMnemonic("Instruction " +values[0].toUpperCase()+ " At line "+i+" Mnemonic does not exist");
                }
                instructions[i] = id;
                operandsReturn[0]=operandsReturn[1]=0;

                registry.getInstruction(instructions[i]).compile(core, this, i, immersiveOperands, operandsReturn, values);

                param0[i] = operandsReturn[0];
                param1[i] = operandsReturn[1];
            }
        }catch (ProgramException e){
            throw e;
        }catch (Exception e){
            throw new ProgramException("Program is invalid! At line "+i,e);
        }
    }

    @Override
    public ProgramMemory clone() {
        ProgramMemory programMemory=new ProgramMemory(registry,instructions.length);
        System.arraycopy(instructions,0,programMemory.instructions,0,instructions.length);
        System.arraycopy(param0,0,programMemory.param0,0, param0.length);
        System.arraycopy(param1,0,programMemory.param1,0, param1.length);
        return programMemory;
    }
}
