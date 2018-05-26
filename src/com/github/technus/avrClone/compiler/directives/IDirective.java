package com.github.technus.avrClone.compiler.directives;

import com.github.technus.avrClone.compiler.ProgramCompiler;
import com.github.technus.avrClone.compiler.exceptions.CompilerException;

import java.util.HashMap;

public interface IDirective {
    HashMap<String,IDirective> DEFINED_DIRECTIVES =new HashMap<>();

    void process(ProgramCompiler compiler, String args) throws CompilerException;
    boolean isUnskippable();
}
