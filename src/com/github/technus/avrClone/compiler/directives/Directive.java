package com.github.technus.avrClone.compiler.directives;

import com.github.technus.avrClone.compiler.Binding;
import com.github.technus.avrClone.compiler.ProgramCompiler;
import com.github.technus.avrClone.compiler.exceptions.CompilerException;

import static com.github.technus.avrClone.compiler.LineConsumer.splitExpressionsString;
import static com.github.technus.avrClone.compiler.ListingMode.*;
import static com.github.technus.avrClone.compiler.Segment.*;

public abstract class Directive implements IDirective {
    private final boolean unskippable;

    public Directive(boolean unskippable){
        this.unskippable=unskippable;
    }

    @Override
    public boolean isUnskippable() {
        return unskippable;
    }

    public static void makeDirectives(){
        DEFINED_DIRECTIVES.put("OVERLAP", new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setCurrentOverlap(true);
            }
        });
        DEFINED_DIRECTIVES.put("NOOVERLAP",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setCurrentOverlap(false);
            }
        });

        DEFINED_DIRECTIVES.put("CSEG",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setCurrentSegment(CSEG);
            }
        });
        DEFINED_DIRECTIVES.put("DSEG",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setCurrentSegment(DSEG);
            }
        });
        DEFINED_DIRECTIVES.put("ESEG",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setCurrentSegment(ESEG);
            }
        });

        DEFINED_DIRECTIVES.put("ORG",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setCurrentOrigin((int) compiler.computeValue(args));
            }
        });

        //malloc
        DEFINED_DIRECTIVES.put("INT",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    compiler.reserveMemory(compiler.computeValue(args).intValue());
                }
            }
        });
        DEFINED_DIRECTIVES.put("FLOAT",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    compiler.reserveMemory(compiler.computeValue(args).intValue());
                }
            }
        });
        DEFINED_DIRECTIVES.put("LONG",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    compiler.reserveMemory(compiler.computeValue(args).intValue() * 2);
                }
            }
        });

        //consts
        DEFINED_DIRECTIVES.put("STRING",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    compiler.putConstant(compiler.computeString(args));
                }
            }
        });
        DEFINED_DIRECTIVES.put("DINT",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] arg = splitExpressionsString(args);
                    for (String anArg : arg) {
                        compiler.putConstant(compiler.computeValue(anArg).intValue());
                    }
                }
            }
        });
        DEFINED_DIRECTIVES.put("DLONG",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] arg = splitExpressionsString(args);
                    for (String anArg : arg) {
                        compiler.putConstant(compiler.computeValue(anArg).longValue());
                    }
                }
            }
        });
        DEFINED_DIRECTIVES.put("DFLOAT",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] arg = splitExpressionsString(args);
                    for (String anArg : arg) {
                        compiler.putConstant(compiler.computeValue(anArg).floatValue());
                    }
                }
            }
        });

        DEFINED_DIRECTIVES.put("EXIT",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    throw new ExitDirective(args);
                }
            }
        });

        DEFINED_DIRECTIVES.put("MESSAGE",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.write(compiler.computeString(args));
            }
        });
        DEFINED_DIRECTIVES.put("WARNING",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.writeError("WARNING: " + compiler.computeString(args));
            }
        });
        DEFINED_DIRECTIVES.put("ERROR",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.writeError("ERROR: " + compiler.computeString(args));
            }
        });

        DEFINED_DIRECTIVES.put("EQU",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] argArr = args.replaceFirst("=", "\0").split("\\x00");
                    if(argArr.length!=2){
                        throw new InvalidDirective("Malformed directive! "+args);
                    }
                    compiler.putBinding(argArr[0], new Binding(Binding.NameType.EQU, compiler.computeValue(argArr[1])));
                }
            }
        });
        DEFINED_DIRECTIVES.put("SET",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] argArr = args.replaceFirst("=", "\0").split("\\x00");
                    if(argArr.length!=2){
                        throw new InvalidDirective("Malformed directive! "+args);
                    }
                    compiler.putBinding(argArr[0], new Binding(Binding.NameType.SET, compiler.computeValue(argArr[1])));
                }
            }
        });
        DEFINED_DIRECTIVES.put("DEF",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] argArr = args.replaceFirst("=", "\0").split("\\x00");
                    if(argArr.length!=2){
                        throw new InvalidDirective("Malformed directive! "+args);
                    }
                    compiler.putBinding(argArr[0], new Binding(Binding.NameType.DEF, compiler.computeValue(argArr[1])));
                }
            }
        });
        DEFINED_DIRECTIVES.put("UNDEF",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] defs = splitExpressionsString(args);
                    for (String def : defs) {
                        compiler.removeBinding(def);
                    }
                }
            }
        });

        DEFINED_DIRECTIVES.put("MACRO",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] arg = splitExpressionsString(args);
                    for (String name : arg) {
                        compiler.addMacro(name);
                    }
                }
            }
        });
        DEFINED_DIRECTIVES.put("ENDMACRO",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] arg = splitExpressionsString(args);
                    for (String name : arg) {
                        compiler.finishMacro(name);
                    }
                }
            }
        });
        DEFINED_DIRECTIVES.put("ENDM",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] arg = splitExpressionsString(args);
                    for (String name : arg) {
                        compiler.finishMacro(name);
                    }
                }
            }
        });

        DEFINED_DIRECTIVES.put("IF",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.openIf(compiler.computeBoolean(args));
            }
        });
        DEFINED_DIRECTIVES.put("IFDEF",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {//all def
                    String[] arg = args.split(",");
                    compiler.openIf(compiler.containsNotDefs(arg));
                }
            }
        });
        DEFINED_DIRECTIVES.put("IFNDEF",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {//all undef
                    String[] arg = args.split(",");
                    compiler.openIf(compiler.lacksNotDefs(arg));
                }
            }
        });
        DEFINED_DIRECTIVES.put("ENDIF",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.endIf();
            }
        });

        DEFINED_DIRECTIVES.put("ELIF",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.elseIf(compiler.computeBoolean(args));
            }
        });
        DEFINED_DIRECTIVES.put("ELDEF",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {//all def
                    String[] arg = args.split(",");
                    compiler.elseIf(compiler.containsNotDefs(arg));
                }
            }
        });
        DEFINED_DIRECTIVES.put("ELNDEF",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {//all undef
                    String[] arg = args.split(",");
                    compiler.elseIf(compiler.lacksNotDefs(arg));
                }
            }
        });
        DEFINED_DIRECTIVES.put("ELSE",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.elseIf(true);
            }
        });

        DEFINED_DIRECTIVES.put("NOLIST",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setListing(NO_LIST);
            }
        });
        DEFINED_DIRECTIVES.put("LIST",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setListing(LIST);
            }
        });
        DEFINED_DIRECTIVES.put("LISTMAC",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setListing(LIST_MACRO);
            }
        });

        DEFINED_DIRECTIVES.put("INCLUDE",new Directive(false) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    compiler.include(args);
                }
            }
        });
    }
}
