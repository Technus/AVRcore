package com.github.technus.avrClone.compiler.directives;

import com.github.technus.avrClone.compiler.exceptions.CompilerException;

public class ExitDirective extends CompilerException {
    public ExitDirective(String s){
        super(s);
    }
}
