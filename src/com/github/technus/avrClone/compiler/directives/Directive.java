package com.github.technus.avrClone.compiler.directives;

import com.github.technus.avrClone.compiler.Binding;
import com.github.technus.avrClone.compiler.ConditionalState;
import com.github.technus.avrClone.compiler.ProgramCompiler;
import com.github.technus.avrClone.compiler.exceptions.CompilerException;
import com.github.technus.avrClone.compiler.exceptions.EvaluationException;

import static com.github.technus.avrClone.compiler.LineConsumer.splitExpressionsString;
import static com.github.technus.avrClone.compiler.ListingMode.*;
import static com.github.technus.avrClone.compiler.Segment.*;

public abstract class Directive implements IDirective {
    private final boolean unskippable, repeatable, onlyFirst;

    public Directive(boolean unskippable) {
        this.unskippable = onlyFirst = unskippable;
        repeatable = false;
    }

    public Directive(boolean unskippable, boolean repeatable) {
        this.unskippable = unskippable;
        this.repeatable = repeatable;
        onlyFirst = false;
    }

    public Directive() {
        unskippable = repeatable = false;
        onlyFirst = true;
    }

    @Override
    public boolean isRepeatable() {
        return repeatable;
    }

    @Override
    public boolean isUnskippable() {
        return unskippable;
    }

    @Override
    public boolean isOnlyFirstPass() {
        return onlyFirst;
    }

    public static void makeDirectives() {
        DEFINED_DIRECTIVES.put("OVERLAP", new Directive(false, true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setCurrentOverlap(true);
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("NOOVERLAP", new Directive(false, true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setCurrentOverlap(false);
                return null;
            }
        });

        DEFINED_DIRECTIVES.put("CSEG", new Directive(false, true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setCurrentSegment(CSEG);
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("DSEG", new Directive(false, true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setCurrentSegment(DSEG);
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("ESEG", new Directive(false, true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setCurrentSegment(ESEG);
                return null;
            }
        });

        DEFINED_DIRECTIVES.put("ORG", new Directive(false, true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                int value = compiler.computeValue(args).intValue();
                compiler.setCurrentOrigin(value);
                return Integer.toString(value);
            }
        });

        //malloc
        DEFINED_DIRECTIVES.put("INT", new Directive(false) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.reserveMemory(compiler.computeValue(args).intValue());
                return null;

            }
        });
        DEFINED_DIRECTIVES.put("FLOAT", new Directive(false) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.reserveMemory(compiler.computeValue(args).intValue());
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("LONG", new Directive(false) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.reserveMemory(compiler.computeValue(args).intValue() * 2);
                return null;
            }
        });

        //consts
        DEFINED_DIRECTIVES.put("STRING", new Directive(false) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.putConstant(compiler.computeString(args));
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("DINT", new Directive(false) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                String[] arg = splitExpressionsString(args);
                for (String anArg : arg) {
                    compiler.putConstant(compiler.computeValue(anArg).intValue());
                }
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("DLONG", new Directive(false) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                String[] arg = splitExpressionsString(args);
                for (String anArg : arg) {
                    compiler.putConstant(compiler.computeValue(anArg).longValue());
                }
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("DFLOAT", new Directive(false) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                String[] arg = splitExpressionsString(args);
                for (String anArg : arg) {
                    compiler.putConstant(compiler.computeValue(anArg).floatValue());
                }
                return null;
            }
        });

        DEFINED_DIRECTIVES.put("MESSAGE", new Directive(false) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.write(compiler.computeString(args));
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("WARNING", new Directive(false) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.writeError("WARNING: " + compiler.computeString(args));
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("ERROR", new Directive(false) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.writeError("ERROR: " + compiler.computeString(args));
                return null;
            }
        });

        DEFINED_DIRECTIVES.put("EQU", new Directive(false) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                String[] argArr = args.replaceFirst("=", "\0").split("\\x00");
                if (argArr.length != 2) {
                    throw new InvalidDirective("Malformed directive! " + args);
                }
                compiler.putBinding(argArr[0], new Binding(Binding.NameType.EQU, compiler.computeValue(argArr[1])));
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("SET", new Directive(false, true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                String[] argArr = args.replaceFirst("=", "\0").split("\\x00");
                if (argArr.length != 2) {
                    throw new InvalidDirective("Malformed directive! " + args);
                }
                Number no = compiler.computeValue(argArr[1]);
                compiler.putBinding(argArr[0], new Binding(Binding.NameType.SET, no));
                return no.toString();
            }
        });
        DEFINED_DIRECTIVES.put("DEF", new Directive(false, true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                String[] argArr = args.replaceFirst("=", "\0").split("\\x00");
                if (argArr.length != 2) {
                    throw new InvalidDirective("Malformed directive! " + args);
                }
                Number no = compiler.computeValue(argArr[1]);
                compiler.putBinding(argArr[0], new Binding(Binding.NameType.DEF, compiler.computeValue(argArr[1])));
                return no.toString();
            }
        });
        DEFINED_DIRECTIVES.put("UNDEF", new Directive(false, true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                String[] defs = splitExpressionsString(args);
                for (String def : defs) {
                    compiler.removeBinding(def);
                }
                return args;
            }
        });

        DEFINED_DIRECTIVES.put("MACRO", new Directive() {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                String[] arg = splitExpressionsString(args);
                for (String name : arg) {
                    compiler.addMacro(name);
                }
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("ENDMACRO", new Directive() {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                String[] arg = splitExpressionsString(args);
                for (String name : arg) {
                    compiler.finishMacro(name);
                }
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("ENDM", new Directive() {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                String[] arg = splitExpressionsString(args);
                for (String name : arg) {
                    compiler.finishMacro(name);
                }
                return null;
            }
        });

        DEFINED_DIRECTIVES.put("IF", new Directive(true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                try {
                    compiler.openIf(compiler.computeBoolean(args));
                } catch (EvaluationException e) {
                    compiler.openIf(false);
                    compiler.setConditionalState(ConditionalState.DISABLED);
                    throw e;
                }
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("IFDEF", new Directive(true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                //all def
                compiler.openIf(compiler.containsNotDefs(splitExpressionsString(args)));
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("IFNDEF", new Directive(true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                //all undef
                compiler.openIf(compiler.lacksNotDefs(splitExpressionsString(args)));
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("ENDIF", new Directive(true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.endIf();
                return null;
            }
        });

        DEFINED_DIRECTIVES.put("ELIF", new Directive(true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                try {
                    compiler.elseIf(compiler.computeBoolean(args));
                } catch (EvaluationException e) {
                    compiler.setConditionalState(ConditionalState.DISABLED);
                    throw e;
                }
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("ELDEF", new Directive(true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                //all def
                compiler.elseIf(compiler.containsNotDefs(splitExpressionsString(args)));
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("ELNDEF", new Directive(true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                //all undef
                compiler.elseIf(compiler.lacksNotDefs(splitExpressionsString(args)));
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("ELSE", new Directive(true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.elseIf(true);
                return null;
            }
        });

        DEFINED_DIRECTIVES.put("NOLIST", new Directive(false, true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setListing(NO_LIST);
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("LIST", new Directive(false, true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setListing(LIST);
                return null;
            }
        });
        DEFINED_DIRECTIVES.put("LISTMAC", new Directive(false, true) {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.setListing(LIST_MACRO);
                return null;
            }
        });

        DEFINED_DIRECTIVES.put("INCLUDE", new Directive() {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.include(args);
                return null;
            }
        });

        DEFINED_DIRECTIVES.put("EXIT", new Directive() {
            @Override
            public String process(ProgramCompiler compiler, String args) throws CompilerException {
                compiler.exit();
            }
        });
    }
}
