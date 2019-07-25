package com.github.technus.avrClone.registerPackages;

import java.util.HashMap;

public abstract class RegistersPackage implements IRegisterPackage {
    protected final HashMap<String,Integer> singles=new HashMap<>();
    protected final HashMap<Integer,String> names=new HashMap<>();
    protected final HashMap<String,Integer> pairs=new HashMap<>();
    protected final HashMap<String,int[]> bits=new HashMap<>();

    private final int offset,size;

    protected RegistersPackage(int offset,int size){
        this.offset=offset;
        this.size=size;
    }

    @Override
    public final int getOffset() {
        return offset;
    }

    @Override
    public final int getSize() {
        return size;
    }

    @Override
    public HashMap<String,Integer> registers() {
        return singles;
    }

    @Override
    public HashMap<Integer,String> names() {
        return names;
    }

    @Override
    public HashMap<String,Integer> registerPairs() {
        return pairs;
    }

    @Override
    public HashMap<String,int[]> registerBits() {
        return bits;
    }

    @Override
    public int[] getDataDefault() {
        return new int[getSize()];
    }
}
