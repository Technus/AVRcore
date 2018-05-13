package com.github.technus.avrClone.memory;

public class SystemMemory implements I_DataMemoryInstance,Cloneable{
    public final int[] data;

    public SystemMemory(){
        this(4096);
    }

    public SystemMemory(int ramSize){
        data=new int[8192+ramSize];//init default
    }

    private SystemMemory(int[] data){
        this.data=data.clone();//init default
    }

    @Override
    public int[] getDataDefault() {
        return new int[data.length];
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public int getSize() {
        return data.length;
    }

    @Override
    public int[] getData() {
        return data;
    }

    @Override
    public SystemMemory clone() {
        return new SystemMemory(data);
    }
}