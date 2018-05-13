package com.github.technus.avrClone.instructions;

import com.sun.javaws.exceptions.InvalidArgumentException;

import java.util.ArrayList;
import java.util.HashMap;

public class InstructionRegistry {
    public static final InstructionRegistry INSTRUCTION_REGISTRY_OP=new InstructionRegistry(Instruction.INSTRUCTIONS_OP).setInstructionDefault(Instruction.NULL);
    public static final InstructionRegistry INSTRUCTION_REGISTRY_IMMERSIVE=new InstructionRegistry(Instruction.INSTRUCTIONS_IMMERSIVE).setInstructionDefault(Instruction.NULL);

    private HashMap<String,Integer> instructionsMap=new HashMap<>();
    private I_Instruction[] instructions;
    private int instructionDefault;

    public InstructionRegistry(ArrayList<? extends I_Instruction> array){
        instructions=new I_Instruction[array.size()];
        for(I_Instruction instructionAVRrc :array){
            addInstruction(instructionAVRrc);
        }
    }

    public InstructionRegistry print(){
        for (int i = 0, instructionsLength = instructions.length; i < instructionsLength; i++) {
            I_Instruction instruction = instructions[i];
            System.out.println(instruction.name() + " " + i);
        }
        return this;
    }

    public InstructionRegistry setInstructionDefault(I_Instruction instructionDefault) {
        this.instructionDefault = getID(instructionDefault.name());
        return this;
    }

    public void addInstruction(I_Instruction instruction){
        int id=instructionsMap.size();
        if(instructionsMap.put(instruction.name(),id)!=null){
            throw new Error(new InvalidArgumentException(new String[]{"Duplicate instruction name",instruction.name()}));
        }
        instructions[id]=instruction;
    }

    public I_Instruction getInstruction(int id){
        return instructions[id];
    }

    public I_Instruction getInstructionSlowly(String name){
        return instructions[instructionsMap.get(name)];
    }

    public int getID(String name){
        Integer id=instructionsMap.get(name);
        return id==null?instructionDefault:id;
    }

    public Integer getId(String name){
        Integer id=instructionsMap.get(name);
        return id;
    }

    public I_Instruction[] getInstructions() {
        return instructions.clone();
    }
}
