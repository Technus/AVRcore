package com.github.technus.avrClone.memory.program;

public class ProgramException extends Exception {
    public ProgramException(String s){
        super(s);
    }
    public ProgramException(String s,Throwable cause){
        super(s,cause);
    }
}
