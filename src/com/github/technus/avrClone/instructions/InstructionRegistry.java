package com.github.technus.avrClone.instructions;

import com.github.technus.avrClone.memory.program.exceptions.InvalidMnemonic;

import java.util.ArrayList;
import java.util.HashMap;

public class InstructionRegistry {
    public static final ArrayList<InstructionRegistry> REGISTRIES=new ArrayList<>();

    public static final InstructionRegistry INSTRUCTION_REGISTRY_OP;
    public static final InstructionRegistry INSTRUCTION_REGISTRY_IMMERSIVE;
    static {
        InstructionRegistry instructionRegistry;

        try{
            instructionRegistry=new InstructionRegistry(Instruction.INSTRUCTIONS_OP).setInstructionDefaultAndName(Instruction.NULL,"OP");
        }catch (InvalidMnemonic e){
            instructionRegistry=null;
        }
        INSTRUCTION_REGISTRY_OP=instructionRegistry;

        try{
            instructionRegistry=new InstructionRegistry(Instruction.INSTRUCTIONS_IMMERSIVE).setInstructionDefaultAndName(Instruction.NULL,"IMMERSIVE");
        }catch (InvalidMnemonic e){
            instructionRegistry=null;
        }
        INSTRUCTION_REGISTRY_IMMERSIVE=instructionRegistry;
    }

    private HashMap<String,Integer> instructionsMap=new HashMap<>();
    private IInstruction[] instructions;
    private int instructionDefault;

    private String name;

    public InstructionRegistry(ArrayList<? extends IInstruction> array) throws InvalidMnemonic{
        instructions=new IInstruction[array.size()];
        for(IInstruction instructionAVRrc :array){
            addInstruction(instructionAVRrc);
        }
        REGISTRIES.add(this);
    }

    public InstructionRegistry print(){
        for (int i = 0, instructionsLength = instructions.length; i < instructionsLength; i++) {
            IInstruction instruction = instructions[i];
            System.out.println(instruction.name() + " " + i);
        }
        return this;
    }

    public InstructionRegistry setInstructionDefaultAndName(IInstruction instructionDefault, String name) {
        this.instructionDefault = getID(instructionDefault.name());
        this.name=name;
        return this;
    }

    public void addInstruction(IInstruction instruction) throws InvalidMnemonic{
        int id=instructionsMap.size();
        if(instructionsMap.put(instruction.name(),id)!=null){
            throw new InvalidMnemonic("Duplicate instruction "+instruction.name());
        }
        instructions[id]=instruction;
    }

    public IInstruction getInstruction(int id){
        return instructions[id];
    }

    public IInstruction getInstructionSlowly(String name){
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

    public IInstruction[] getInstructions() {
        return instructions.clone();
    }

    @Override
    public String toString() {
        return name;
    }
}
