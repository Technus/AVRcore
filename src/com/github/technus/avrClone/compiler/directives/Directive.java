package com.github.technus.avrClone.compiler.directives;

import com.github.technus.avrClone.compiler.Binding;
import com.github.technus.avrClone.compiler.ProgramCompiler;
import com.github.technus.avrClone.compiler.exceptions.CompilerException;
import com.github.technus.avrClone.compiler.exceptions.InvalidMemoryAccess;

import static com.github.technus.avrClone.compiler.ProgramCompiler.Segment.*;

public abstract class Directive implements IDirective {
    private final boolean first,second,unskippable;
    
    public Directive(){
        unskippable=false;
        first=second=true;
    }

    public Directive(boolean firstRun){
        first=firstRun;
        second=!firstRun;
        unskippable=false;
    }

    public Directive(boolean firstRun,boolean secondRun,boolean isUnskippable){
        first=firstRun;
        second=secondRun;
        unskippable=isUnskippable;
    }

    @Override
    public boolean executeAtFirstPass() {
        return first;
    }

    @Override
    public boolean executeAtSecondPass() {
        return second;
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
        DEFINED_DIRECTIVES.put("INT",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    if (compiler.getCurrentSegment() != CSEG) {
                        compiler.reserveMemory(compiler.computeValue(args).intValue());
                    } else {
                        throw new InvalidMemoryAccess("Cannot put variables in program memory!");
                    }
                }
            }
        });
        DEFINED_DIRECTIVES.put("LONG",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    if (compiler.getCurrentSegment() != CSEG) {
                        compiler.reserveMemory(compiler.computeValue(args).intValue() * 2);
                    } else {
                        throw new InvalidMemoryAccess("Cannot put variables in program memory!");
                    }
                }
            }
        });

        //consts
        DEFINED_DIRECTIVES.put("STRING",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    if (compiler.getCurrentSegment() != DSEG) {
                        compiler.putConstant(compiler.computeString(args));
                    } else {
                        throw new InvalidMemoryAccess("Cannot put constants in volatile memory!");
                    }
                }
            }
        });
        DEFINED_DIRECTIVES.put("DINT",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    if (compiler.getCurrentSegment() != DSEG) {
                        String[] arg = args.split(",");
                        for (String anArg : arg) {
                            compiler.putConstant(compiler.computeValue(anArg).intValue());
                        }
                    } else {
                        throw new InvalidMemoryAccess("Cannot put constants in volatile memory!");
                    }
                }
            }
        });
        DEFINED_DIRECTIVES.put("DLONG",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    if (compiler.getCurrentSegment() != DSEG) {
                        String[] arg = args.split(",");
                        for (String anArg : arg) {
                            compiler.putConstant(compiler.computeValue(anArg).longValue());
                        }
                    } else {
                        throw new InvalidMemoryAccess("Cannot put constants in volatile memory!");
                    }
                }
            }
        });
        DEFINED_DIRECTIVES.put("DFLOAT",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    if (compiler.getCurrentSegment() != DSEG) {
                        String[] arg = args.split(",");
                        for (String anArg : arg) {
                            compiler.putConstant(compiler.computeValue(anArg).floatValue());
                        }
                    } else {
                        throw new InvalidMemoryAccess("Cannot put constants in volatile memory!");
                    }
                }
            }
        });

        DEFINED_DIRECTIVES.put("EXIT",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    throw new ExitDirective(args);
                }
            }
        });

        DEFINED_DIRECTIVES.put("MESSAGE",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.write(compiler.computeString(args));
            }
        });
        DEFINED_DIRECTIVES.put("WARNING",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.writeError("WARNING: " + compiler.computeString(args));
            }
        });
        DEFINED_DIRECTIVES.put("ERROR",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.writeError("ERROR: " + compiler.computeString(args));
            }
        });

        DEFINED_DIRECTIVES.put("EQU",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] argArr = args.replaceFirst("=", "\0").split("\\x00");
                    compiler.putBinding(argArr[0], new Binding(Binding.NameType.EQU, compiler.computeValue(argArr[1])));
                }
            }
        });
        DEFINED_DIRECTIVES.put("SET",new Directive() {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] argArr = args.replaceFirst("=", "\0").split("\\x00");
                    compiler.putBinding(argArr[0], new Binding(Binding.NameType.SET, compiler.computeValue(argArr[1])));
                }
            }
        });
        DEFINED_DIRECTIVES.put("DEF",new Directive() {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] argArr = args.replaceFirst("=", "\0").split("\\x00");
                    compiler.putBinding(argArr[0], new Binding(Binding.NameType.DEF, compiler.computeValue(argArr[1])));
                }
            }
        });
        DEFINED_DIRECTIVES.put("UNDEF",new Directive() {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] defs = args.split(",");
                    for (String def : defs) {
                        compiler.removeBinding(def);
                    }
                }
            }
        });

        DEFINED_DIRECTIVES.put("MACRO",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] arg = args.split(",");
                    for (String name : arg) {
                        compiler.addMacro(name);
                    }
                }
            }
        });
        DEFINED_DIRECTIVES.put("ENDMACRO",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] arg = args.split(",");
                    for (String name : arg) {
                        compiler.finishMacro(name);
                    }
                }
            }
        });
        DEFINED_DIRECTIVES.put("ENDM",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    String[] arg = args.split(",");
                    for (String name : arg) {
                        compiler.finishMacro(name);
                    }
                }
            }
        });

        DEFINED_DIRECTIVES.put("IF",new Directive(true,false,true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.openIf(compiler.computeBoolean(args));
            }
        });
        DEFINED_DIRECTIVES.put("IFDEF",new Directive(true,false,true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {//all def
                    String[] arg = args.split(",");
                    compiler.openIf(compiler.containsNotDefs(arg));
                }
            }
        });
        DEFINED_DIRECTIVES.put("IFNDEF",new Directive(true,false,true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {//all undef
                    String[] arg = args.split(",");
                    compiler.openIf(compiler.lacksNotDefs(arg));
                }
            }
        });
        DEFINED_DIRECTIVES.put("ENDIF",new Directive(true,false,true) {
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
                compiler.elseIf(null);
            }
        });

        DEFINED_DIRECTIVES.put("NOLIST",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setListing(ProgramCompiler.ListingMode.NO_LIST);
            }
        });
        DEFINED_DIRECTIVES.put("LIST",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setListing(ProgramCompiler.ListingMode.LIST);
            }
        });
        DEFINED_DIRECTIVES.put("LISTMAC",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setListing(ProgramCompiler.ListingMode.LIST_MACRO);
            }
        });

        DEFINED_DIRECTIVES.put("INCLUDE",new Directive(true) {
            @Override
            public void process(ProgramCompiler compiler, String args) throws CompilerException {
                {
                    compiler.include(args);
                }
            }
        });
    }
}
