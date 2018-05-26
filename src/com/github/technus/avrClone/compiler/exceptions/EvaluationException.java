package com.github.technus.avrClone.compiler.exceptions;

public class EvaluationException extends CompilerException {
    public EvaluationException(String s,Throwable c){
        super(s,c);
    }
    public EvaluationException(String s){
        super(s);
    }
}
