package com.github.technus.avrClone.compiler;

public class Binding {
    public final NameType type;
    public Number value;

    public Binding(NameType type){
        this.type=type;
    }

    public Binding(NameType type, Number value){
        this(type);
        this.value=value;
    }

    public enum NameType{
        SET,EQU,DEF,LABEL,POINTER//is a label but for constants
    }

    @Override
    public int hashCode() {
        long v=Double.doubleToLongBits(value.doubleValue());
        return type.ordinal()^(int)v^(int)(v>>>32);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Binding && type == ((Binding) obj).type && value.equals(((Binding) obj).value);
    }
}

