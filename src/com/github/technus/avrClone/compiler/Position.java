package com.github.technus.avrClone.compiler;

public class Position {
    public final int line;
    public final String file;
    public Position(int line,String file){
        this.line=line;
        this.file=file;
    }
}
