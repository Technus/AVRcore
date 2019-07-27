package com.github.technus.avrClone.registerPackages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class RegisterPackage implements IRegisterPackage {
    protected final Map<String,IRegisterBit> bits=new HashMap<>();
    protected final Map<String,IRegister> registers =new HashMap<>();
    protected final Map<Integer, ArrayList<IRegister>> addresses =new HashMap<>();
    protected final Map<Integer, IInterrupt> interrupts=new HashMap<>();

    private final int offset,size;

    protected RegisterPackage(int offset, int size){
        this.offset=offset;
        this.size=size;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public Map<String,IRegister> nameRegisterMap() {
        return registers;
    }

    @Override
    public Map<Integer,ArrayList<IRegister>> addressesNamesMap() {
        return addresses;
    }

    @Override
    public Map<String,IRegisterBit> bitsMap() {
        return bits;
    }

    @Override
    public int[] getDataDefault() {
        return new int[getSize()];
    }

    @Override
    public Map<Integer, IInterrupt> interruptsMap() {
        return interrupts;
    }

    @SuppressWarnings("unchecked")
    protected void addRegisters(IRegister... registers){
        for(IRegister register: registers){
            this.registers.put(register.name(),register);
            addresses.computeIfAbsent(register.getOffset(this),ArrayList::new).add(register);
        }
    }

    @SuppressWarnings("unchecked")
    protected void addBits(IRegisterBit... bits){
        for(IRegisterBit bit: bits){
            this.bits.put(bit.name(),bit);
        }
    }

    protected void addInterrupts(IInterrupt... interrupts){
        for (IInterrupt interrupt :interrupts) {
            this.interrupts.put(interrupt.getVector(),interrupt);
        }
    }
}
