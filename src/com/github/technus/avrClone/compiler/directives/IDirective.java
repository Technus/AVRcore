package com.github.technus.avrClone.compiler.directives;

import com.github.technus.avrClone.compiler.ProgramCompiler;
import com.github.technus.avrClone.compiler.exceptions.CompilerException;

import java.util.HashMap;

public interface IDirective {
    HashMap<String,IDirective> DEFINED_DIRECTIVES =new HashMap<>();

    String process(ProgramCompiler compiler, String args) throws CompilerException;//returns compiled args
    boolean isUnskippable();//for conditional assembly
    boolean isRepeatable();//execute every step with first valid values
    boolean isOnlyFirstPass();//execute every step with first valid values
}
