package com.github.technus.avrClone.compiler;

import com.github.technus.avrClone.compiler.directives.ExitDirective;
import com.github.technus.avrClone.compiler.directives.IDirective;
import com.github.technus.avrClone.compiler.directives.Directive;
import com.github.technus.avrClone.compiler.directives.InvalidDirective;
import com.github.technus.avrClone.compiler.exceptions.*;
import com.github.technus.avrClone.compiler.js.CompilerBindings;
import com.github.technus.avrClone.compiler.js.CompilerContext;
import javafx.geometry.Pos;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.*;

import static com.github.technus.avrClone.compiler.LineConsumer.*;

public final class ProgramCompiler {
    private static final String SANDBOX =
            "var eval=function(){};var uneval=function(){};" +
                    "var decodeURI=function(){};var decodeURIComponent=function(){};" +
                    "var encodeURI=function(){};var encodeURIComponent=function(){};" +
                    "var escape=function(){};var unescape=function(){};" +
                    "var quit=function(){};var exit=function(){};" +
                    "var print=function(){};var echo = function(){};" +
                    "var readFully=function(){};" + "var readLine=function(){};" +
                    "var load=function(){};var loadWithNewGlobal=function(){};\n";

    //region fields
    private ArrayList<String> mainFile;
    private HashMap<String,ArrayList<String>> includedFiles;

    private int currentLine;
    private HashSet<String> labelsOrPointersToAssign;
    private HashMap<String,Binding> tempBindings;

    private ArrayList<String> argHolder;
    private ArrayList<String> lines;
    private ArrayList<Position> positions;
    private ArrayList<Boolean> processedLines;

    private HashMap<Integer,Integer>[] constants;

    private Segment currentSegment;

    private int[] startOffset,origins;

    private boolean[] overlap;
    private BitSet[] constantRanges;

    private CompilerBindings compilerBindings;
    private CompilerContext scriptContext;
    private NashornScriptEngine scriptEngine;

    private ArrayList<ConditionalState> compilationEnabled;

    private HashMap<String,ArrayList<String>> macros;
    private HashSet<String> editingMacros;

    private ListingMode listing;

    private Includer includer;

    private HashMap<String,IDirective> instanceDirectives=new HashMap<>();

    private ArrayList<String> madeFile;
    //endregion

    public ProgramCompiler(){
        reset();
    }

    @SuppressWarnings("unchecked")
    private void reset() {
        mainFile = null;
        includedFiles = new HashMap<>(8);

        currentLine=0;
        labelsOrPointersToAssign=new HashSet<>(8);
        tempBindings=new HashMap<>(32);
        argHolder=new ArrayList<>(128);
        positions=new ArrayList<>(512);
        processedLines=new ArrayList<>(512);
        lines=new ArrayList<>(512);

        currentSegment = Segment.CSEG;

        startOffset = new int[Segment.values().length];
        origins = new int[Segment.values().length];

        overlap = new boolean[Segment.values().length];

        constants=new HashMap[Segment.values().length];
        for(int i=0;i<constants.length;i++){
            constants[i]=new HashMap<>();
        }
        constantRanges = new BitSet[Segment.values().length];
        for (int i = 0; i < constantRanges.length; i++) {
            constantRanges[i] = new BitSet(4096);
        }

        compilerBindings = new CompilerBindings(64);
        SimpleBindings globalBindings = new SimpleBindings();
        scriptContext = new CompilerContext(compilerBindings, globalBindings);
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        ccl = ccl == null ? NashornScriptEngineFactory.class.getClassLoader() : ccl;
        scriptEngine = (NashornScriptEngine) new NashornScriptEngineFactory()
                .getScriptEngine(new String[]{"--no-java"}, ccl, s -> false);
        scriptEngine.setContext(scriptContext);

        compilationEnabled = new ArrayList<>();
        compilationEnabled.add(ConditionalState.ASSEMBLING);

        macros = new HashMap<>();
        editingMacros = new HashSet<>();

        listing = ListingMode.LIST;

        madeFile=null;
    }

    //region input file
    public void setInputFile(ArrayList<String> content) throws Exception{
        reset();
        if(content==null){
            throw new CompilerException("Cannot read file!");
        }
        loadMainFile(content);
    }
    public void setInputFile(String file) throws Exception {
        reset();
        if (includer == null) {
            throw new InvalidInclude("No include processor set!");
        }
        ArrayList<String> lines=includer.include(file);
        if(lines==null){
            throw new InvalidInclude("Unable to include file! "+file);
        }
        loadMainFile(lines);
    }
    private void loadMainFile(ArrayList<String> main){
        mainFile=sanitizeList(main);
        lines.addAll(mainFile);
        for(int i=0;i<lines.size();i++){
            positions.add(new Position(i,null));
            processedLines.add(false);
            argHolder.add(null);
        }
    }
    public void writeProgram() throws Exception {
        madeFile=null;
        try {
            if (mainFile == null) {
                throw new CompilerException("No main file loaded!");
            }

            boolean didSomething;
            Exception exception = null;
            {
                int firstUnskippableFail;
                do {//all directives it can, ALL conditionals
                    firstUnskippableFail = -1;
                    didSomething = false;

                    labelsOrPointersToAssign.clear();
                    setConditionalState(ConditionalState.ASSEMBLING);
                    setListing(ListingMode.LIST);

                    for (currentLine = 0; currentLine < lines.size(); currentLine++) {
                        if (!processedLines.get(currentLine)) {
                            String line = lines.get(currentLine);
                            String labelOrPointer = getLabelOrPointerName(line);
                            if (labelOrPointer != null) {
                                labelsOrPointersToAssign.add(labelOrPointer);
                            }

                            String directiveName = getDirectiveName(line);
                            String mnemonic = getMnemonic(line);
                            if (directiveName != null) {
                                if (mnemonic != null) {
                                    throw new CompilerException("Invalid line: " + line);
                                }

                                IDirective directive = getDirective(directiveName);
                                if (directive != null) {
                                    try {
                                        String expressions = argHolder.get(currentLine);
                                        if (expressions == null) {
                                            expressions = getExpressionsString(line);
                                        }
                                        String args = directive.process(this, expressions);
                                        if (directive.isRepeatable()) {
                                            argHolder.set(currentLine, args);
                                        } else {
                                            processedLines.set(currentLine, true);
                                        }
                                        didSomething = true;
                                    } catch (EvaluationException e) {
                                        if (directive.isUnskippable()) {
                                            setConditionalState(ConditionalState.DISABLED);
                                            if (firstUnskippableFail == -1) {
                                                exception = e;
                                                firstUnskippableFail = currentLine;
                                            }
                                            writeError("WARN: " + exception.getMessage());
                                        }
                                    } catch (ExitDirective e) {
                                        Position pos = positions.get(currentLine);
                                        String file = pos.file;
                                        processedLines.set(currentLine, true);
                                        for (currentLine++; currentLine < lines.size(); currentLine++) {
                                            if (file.equals(positions.get(currentLine).file)) {
                                                processedLines.set(currentLine, true);
                                            } else {
                                                currentLine--;
                                                break;
                                            }
                                        }
                                    }
                                }
                            } else if (mnemonic != null) {
                                labelsOrPointersToAssign.clear();
                                if (isCompilationEnabled()) {
                                    if (isEditingMacros()) {
                                        addToMacros(line);
                                    }
                                } else if (firstUnskippableFail == -1) {
                                    processedLines.set(currentLine, true);
                                    didSomething = true;
                                }
                            } else/*nothing of value...*/ {
                                processedLines.set(currentLine, true);
                            }
                        }
                    }
                } while (didSomething);
                if (firstUnskippableFail >= 0) {
                    Position pos = positions.get(firstUnskippableFail);
                    if (pos.file == null) {
                        throw new CompilerException("Cannot process directive! " + pos.line, exception);
                    } else {
                        throw new CompilerException("Cannot process directive! " + pos.file + " " + pos.line, exception);
                    }
                }
            }
            exception=null;

            if (isEditingMacros()) {
                throw new CompilerException("Is still trying to edit macros! " + Arrays.toString(editingMacros.toArray(new String[0])));
            }

            for (Map.Entry<String, Binding> entry : tempBindings.entrySet()) {
                putBinding(entry.getKey(), entry.getValue());
            }

            labelsOrPointersToAssign.clear();

            int programCounter=0;
            for (currentLine = 0; currentLine < lines.size(); currentLine++) {
                if(processedLines.get(currentLine)){
                    continue;
                }

                String line = lines.get(currentLine);
                line=line.replaceAll(NAME_FORMAT+"#","($1-"+programCounter+')');
                if(line.contains("#")){
                    throw new CompilerException("Unable to replace with PC difference! "+line);
                }
                lines.set(currentLine,line);

                String labelOrPointer = getLabelOrPointerName(line);
                if (labelOrPointer != null) {
                    labelsOrPointersToAssign.add(labelOrPointer);
                }

                String mnemonic=getMnemonic(line);
                if(mnemonic!=null) {
                    putProgramLabels(programCounter);

                    if(isMacroDefined(mnemonic)) {
                        injectMacro(mnemonic, getExpressionsString(line));
                        processedLines.set(currentLine,true);
                    }else{
                        programCounter++;
                    }
                }else if (containsDirectiveName(line)){
                    labelsOrPointersToAssign.clear();
                }
            }

            {
                int firstFail;
                do {
                    firstFail = -1;
                    didSomething = false;

                    for (currentLine = 0; currentLine < lines.size(); currentLine++) {
                        if (!processedLines.get(currentLine)) {
                            String line = lines.get(currentLine);
                            String directiveName = getDirectiveName(line);
                            String mnemonic = getMnemonic(line);
                            try {
                                if (directiveName != null) {
                                    if (mnemonic != null) {
                                        throw new CompilerException("Invalid line: " + line);
                                    }
                                    IDirective directive = getDirective(directiveName);
                                    if (directive != null) {
                                        if (directive.isOnlyFirstPass()) {
                                            throw new CompilerException("Failed to process directive in first pass! " + line);
                                        }
                                        directive.process(this, getExpressionsString(line));
                                    }
                                } else if (mnemonic != null) {
                                    String[] expressions = splitExpressionsString(getExpressionsString(line));
                                    StringBuilder sb = new StringBuilder(mnemonic);
                                    for (int i = 0; i < expressions.length; i++) {
                                        sb.append(' ').append(Integer.toString(computeValue(expressions[i]).intValue()));
                                    }
                                    lines.set(currentLine, sb.toString());
                                }
                                processedLines.set(currentLine, true);
                                didSomething = true;
                            } catch (EvaluationException e) {
                                if (firstFail == -1) {
                                    exception = e;
                                    firstFail = currentLine;
                                }
                                writeError("WARN: " + exception.getMessage());
                            }
                        }
                    }
                } while (didSomething);
                if (firstFail >= 0) {
                    Position pos = positions.get(firstFail);
                    if (pos.file == null) {
                        throw new CompilerException("Cannot finish assembling! " + pos.line, exception);
                    } else {
                        throw new CompilerException("Cannot finish assembling! " + pos.file + " " + pos.line, exception);
                    }
                }
            }

            ArrayList<String> made = new ArrayList<>();
            for(currentLine =0 ;currentLine<lines.size();currentLine++){
                String line = lines.get(currentLine);
                String mnemonic=getMnemonic(line);
                if(mnemonic!=null && !isMacroDefined(mnemonic)){
                    made.add(line);
                }
            }
            if(made.size()!=programCounter){
                throw new CompilerException("Invalid instruction count! "+programCounter+" "+made.size());
            }
            //exception=null;

            madeFile = made;
        } catch (CompilerException e) {
            throw new CompilerException(e);
        }
    }
    public ArrayList<String> getInputFile() {
        return mainFile;
    }
    public ArrayList<String> getMadeFile() {
        return madeFile;
    }
    //endregion

    //region include
    public void setIncluder(Includer include) {
        this.includer = include;
    }
    public Includer getIncluder() {
        return includer;
    }
    public void include(String file) throws CompilerException{
        if(file==null){
            throw new InvalidInclude("Cannot include null!");
        }
        ArrayList<String> list=includedFiles.get(file);
        if(list==null){
            if (includer == null) {
                throw new InvalidInclude("No include processor set!");
            }
            list=includer.include(file);
            if(list==null){
                throw new InvalidInclude("Unable to include file! "+file);
            }
            list=sanitizeList(list);
            includedFiles.put(file,list);
        }
        //inject lines
        int nextLine=currentLine+1;

        ArrayList<Position> pos=new ArrayList<>();
        ArrayList<Boolean> bools=new ArrayList<>();
        ArrayList<String> args=new ArrayList<>();
        for(int i=0;i<list.size();i++){
            pos.add(new Position(i,file));
            bools.add(false);
            args.add(null);
        }
        pos.add(new Position(-1,file));
        bools.add(true);
        args.add(null);
        list.add('\0'+file);
        positions.addAll(nextLine,pos);
        processedLines.addAll(nextLine,bools);
        lines.addAll(nextLine,list);
        argHolder.addAll(nextLine,args);
    }
    //endregion

    //region segment
    public void setCurrentSegment(Segment currentSegment) throws InvalidMemorySegment{
        if(currentSegment==null){
            throw new InvalidMemorySegment("Segment cannot be null!");
        }
        this.currentSegment = currentSegment;
    }
    public Segment getCurrentSegment() {
        return currentSegment;
    }
    //endregion

    //region start offsets
    public int getCurrentSegmentOffset(){
        return startOffset[currentSegment.ordinal()];
    }
    public int getSegmentOffset(Segment segment){
        return startOffset[segment.ordinal()];
    }
    public void setCurrentSegmentOffset(int offset) throws InvalidStartOffset{
        if(offset<0){
            throw new InvalidStartOffset("Start offset must be not negative! "+offset);
        }
        startOffset[currentSegment.ordinal()]=offset;
    }
    public void setSegmentOffset(Segment segment,int offset) throws InvalidStartOffset{
        if(offset<0){
            throw new InvalidStartOffset("Start offset must be not negative! "+offset);
        }
        startOffset[segment.ordinal()]=offset;
    }
    //endregion

    //region overlap policy
    public void setCurrentOverlap(boolean value){
        overlap[currentSegment.ordinal()]=value;
    }
    public void setOverlap(Segment segment,boolean value){
        overlap[segment.ordinal()]=value;
    }
    public boolean getCurrentOverlap() {
        return overlap[currentSegment.ordinal()];
    }
    public boolean getOverlap(Segment segment) {
        return overlap[segment.ordinal()];
    }
    //endregion

    //region origin
    public void offsetCurrentOrigin(int value) throws InvalidOrigin {
        if(origins[currentSegment.ordinal()]+value<0){
            throw new InvalidOrigin("Origin must be not negative! "+origins[currentSegment.ordinal()]+"+"+value);
        }
        origins[currentSegment.ordinal()]+=value;
    }
    public void setCurrentOrigin(int value) throws InvalidOrigin{
        if(value<0){
            throw new InvalidOrigin("Origin must be not negative! "+value);
        }
        origins[currentSegment.ordinal()]=value;
    }
    public void setOrigin(int value, Segment segment) throws InvalidOrigin{
        if(value<0){
            throw new InvalidOrigin("Origin must be not negative! "+value);
        }
        origins[segment.ordinal()]=value;
    }
    public int getCurrentOrigin() {
        return origins[currentSegment.ordinal()];
    }
    public int getOrigin(Segment segment) {
        return origins[segment.ordinal()];
    }
    //endregion

    //region mem use - for CSEG after instructions!
    public int getCurrentMemorySize(){
        return constantRanges[currentSegment.ordinal()].length();
    }
    public int getMemorySize(Segment segment){
        return constantRanges[segment.ordinal()].length();
    }
    public boolean isCurrentMemoryCellFree(){
        return constantRanges[currentSegment.ordinal()].get(getCurrentOrigin());
    }
    public boolean isCurrentMemoryCellFree(int address) throws InvalidMemoryAccess {
        if(address<0){
            throw new InvalidMemoryAccess("Memory address must be not negative! "+address);
        }
        return constantRanges[currentSegment.ordinal()].get(address);
    }
    public boolean isMemoryCellFree(Segment segment,int address) throws InvalidMemoryAccess {
        if(address<0){
            throw new InvalidMemoryAccess("Memory address must be not negative! "+address);
        }
        return constantRanges[segment.ordinal()].get(address);
    }
    public boolean isCurrentMemoryRangeFree(int toExclusive) throws InvalidMemoryAccess {
        if(getCurrentOrigin()>=toExclusive){
            throw new InvalidMemoryAccess("Range end must be greater than origin! "+getCurrentOrigin()+" !< "+toExclusive);
        }
        return constantRanges[currentSegment.ordinal()].nextClearBit(getCurrentOrigin())>=toExclusive;
    }
    public boolean isCurrentMemoryRangeFree(int fromInclusive,int toExclusive) throws InvalidMemoryAccess {
        if(fromInclusive<0){
            throw new InvalidMemoryAccess("Range start must be not negative! "+fromInclusive);
        }
        if(fromInclusive>=toExclusive){
            throw new InvalidMemoryAccess("Range end must be greater than range start! "+fromInclusive+" !< "+toExclusive);
        }
        return constantRanges[currentSegment.ordinal()].nextClearBit(fromInclusive)>=toExclusive;
    }
    public boolean isMemoryRangeFree(Segment segment,int fromInclusive,int toExclusive) throws InvalidMemoryAccess{
        if(fromInclusive<0){
            throw new InvalidMemoryAccess("Range start must be not negative! "+fromInclusive);
        }
        if(fromInclusive>=toExclusive){
            throw new InvalidMemoryAccess("Range end must be greater than range start! "+fromInclusive+" !< "+toExclusive);
        }
        return constantRanges[segment.ordinal()].nextClearBit(fromInclusive)>=toExclusive;
    }
    public boolean isCurrentMemoryBlockFree(int size) throws InvalidMemoryAccess {
        if(size<=0){
            throw new InvalidMemoryAccess("Block size must be positive! "+size);
        }
        int offset=getCurrentOrigin();
        return constantRanges[currentSegment.ordinal()].nextClearBit(offset)>=offset+size;
    }
    public boolean isCurrentMemoryBlockFree(int start,int size)throws InvalidMemoryAccess{
        if(start<0){
            throw new InvalidMemoryAccess("Block start must be not negative! "+start);
        }
        if(size<=0){
            throw new InvalidMemoryAccess("Block size must be positive! "+size);
        }
        return constantRanges[currentSegment.ordinal()].nextClearBit(start)>=start+size;
    }
    public boolean isMemoryBlockFree(Segment segment,int start,int size)throws InvalidMemoryAccess{
        if(start<0){
            throw new InvalidMemoryAccess("Block start must be not negative! "+start);
        }
        if(size<=0){
            throw new InvalidMemoryAccess("Block size must be positive! "+size);
        }
        return constantRanges[segment.ordinal()].nextClearBit(start)>=start+size;
    }
    //endregion

    //region constants
    public boolean putConstant(int constant) throws InvalidOrigin,InvalidMemoryAllocation{
        if(currentSegment==Segment.DSEG){
            throw new InvalidMemoryAllocation("Cannot store constants in volatile memory! "+constant);
        }
        int segment=currentSegment.ordinal();//todo add labeling from labels to assign, todo add temp binding
        if(currentSegment==Segment.CSEG){
            constants[segment].put(constants[segment].size(),constant);
            return true;
        }
        if(getCurrentOverlap() || isCurrentMemoryCellFree()){
            int origin=getCurrentOrigin();
            constantRanges[segment].set(origin);
            constants[segment].put(origin,constant);
            offsetCurrentOrigin(1);
            return true;
        }
        return false;
    }
    public boolean putConstant(float constant) throws InvalidOrigin,InvalidMemoryAllocation{
        if(currentSegment==Segment.DSEG){
            throw new InvalidMemoryAllocation("Cannot store constants in volatile memory! "+constant);
        }
        int segment=currentSegment.ordinal();
        if(currentSegment==Segment.CSEG){
            constants[segment].put(constants[segment].size(),Float.floatToIntBits(constant));
            return true;
        }
        if(getCurrentOverlap() || isCurrentMemoryCellFree()){
            int origin=getCurrentOrigin();
            constantRanges[segment].set(origin);
            constants[segment].put(origin,Float.floatToIntBits(constant));
            offsetCurrentOrigin(1);
            return true;
        }
        return false;
    }
    public boolean putConstant(long constant) throws InvalidOrigin,InvalidMemoryAccess,InvalidMemoryAllocation{
        if(currentSegment==Segment.DSEG){
            throw new InvalidMemoryAllocation("Cannot store constants in volatile memory! "+constant);
        }
        int segment=currentSegment.ordinal();
        if(currentSegment==Segment.CSEG){
            int origin=constants[segment].size();
            constants[segment].put(origin,(int)constant);
            constants[segment].put(origin+1,(int)(constant>>32));
            return true;
        }
        if(getCurrentOverlap() || isCurrentMemoryBlockFree(2)){
            int origin=getCurrentOrigin();
            constantRanges[segment].set(origin,origin+2);
            constants[segment].put(origin,(int)constant);
            constants[segment].put(origin+1,(int)(constant>>32));
            offsetCurrentOrigin(2);
            return true;
        }
        return false;
    }
    public boolean putConstant(String constant) throws InvalidOrigin,InvalidMemoryAccess,InvalidMemoryAllocation,InvalidConstant {
        if(currentSegment==Segment.DSEG){
            throw new InvalidMemoryAllocation("Cannot store constants in volatile memory! "+constant);
        }
        if(constant==null){
            throw new InvalidConstant("String constant cannot be null!");
        }
        if(constant.length()==0){
            throw new InvalidConstant("String constant must not be empty!");
        }
        int segment=currentSegment.ordinal();
        if(currentSegment==Segment.CSEG){
            int origin=constants[segment].size();
            for(int i=0;i<constant.length();i++){
                constants[segment].put(origin+i,(int)constant.charAt(i));
            }
            return true;
        }
        int length=constant.length();
        if(getCurrentOverlap() || isCurrentMemoryBlockFree(length)){
            int origin=getCurrentOrigin();
            constantRanges[segment].set(origin,origin+length);
            for(int i=0;i<constant.length();i++){
                constants[segment].put(origin+i,(int)constant.charAt(i));
            }
            offsetCurrentOrigin(length);
            return true;
        }
        return false;
    }
    public boolean reserveMemory(int cellCount) throws InvalidOrigin,InvalidMemoryAccess,InvalidMemoryAllocation {
        if(currentSegment==Segment.CSEG){
            throw new InvalidMemoryAllocation("Cannot store variables in program memory! "+cellCount);
        }
        if(cellCount<=0){
            throw new InvalidMemoryAllocation("Memory block size must have positive size! "+cellCount);
        }
        if(getCurrentOverlap() || isCurrentMemoryBlockFree(cellCount)){
            int segment=currentSegment.ordinal();
            int origin=getCurrentOrigin();
            constantRanges[segment].set(origin,origin+cellCount);
            for(int i=0;i<cellCount;i++){
                constants[segment].put(origin+i,0);
            }
            offsetCurrentOrigin(cellCount);
            return true;
        }
        return false;
    }
    public void putBinding(String name,Binding binding) throws InvalidBinding{
        putBinding(compilerBindings,name,binding);
    }
    public static void putBinding(HashMap<String,Binding> map, String name, Binding binding) throws InvalidBinding{
        if(name==null || name.length()==0 || !name.matches(NAME_FORMAT)){
            throw new InvalidBinding("Invalid binding name specified! "+name);
        }
        Binding old=map.get(name);
        if(old!=null) {
            switch (old.type) {
                case SET: case DEF: break;
                default: throw new InvalidBinding("Cannot rewrite that binding! " + old.type.name()+" "+name);
            }
        }
        map.put(name,binding);
    }
    public static void putBinding(HashMap<String,Object> map, String name, Object binding) throws InvalidBinding{
        if(!(binding instanceof Binding)){
            throw new InvalidBinding("Invalid binding specified! "+name);
        }
        if(name==null || name.length()==0 || !name.matches(NAME_FORMAT)){
            throw new InvalidBinding("Invalid binding name specified! "+name);
        }
        Object objec=map.get(name);
        if(objec!=null && !(objec instanceof Binding)){
            throw new InvalidBinding("Invalid binding detected! "+name);
        }
        if(objec!=null) {
            Binding old=(Binding)objec;
            switch (old.type) {
                case SET: case DEF: break;
                default: throw new InvalidBinding("Cannot rewrite that binding! " + old.type.name()+" "+name);
            }
        }
        map.put(name,binding);
    }
    public void putProgramLabels(int currentPC) throws InvalidBinding{
        try {
            for (String name : labelsOrPointersToAssign) {
                putBinding(name, new Binding(Binding.NameType.LABEL, currentPC));
            }
        }finally {
            labelsOrPointersToAssign.clear();
        }
    }
    //endregion

    //region compute
    public String computeString(String script) throws EvaluationException{
        try {
            Object value = scriptEngine.eval(SANDBOX + script);
            if(value instanceof String){
                return (String) value;
            }else if(value instanceof Number || value instanceof Boolean){
                return value.toString();
            }
            writeError("Returned type: " + value.getClass().getCanonicalName());
            return value.toString();
        }catch (ScriptException e){
            throw new EvaluationException("Cannot evaluate! "+script,e);
        }
    }
    public Number computeValue(String script) throws EvaluationException{
        try {
            Object value = scriptEngine.eval(SANDBOX + script);
            if (value instanceof Number) {
                return (Number) value;
            } else if (value instanceof Boolean) {
                return (Boolean) value ? 1 : 0;
            }
            writeError("Returned type: " + value.getClass().getCanonicalName());
            return 0;
        } catch (ScriptException e) {
            throw new EvaluationException("Cannot evaluate! " + script, e);
        }
    }
    public boolean computeBoolean(String script) throws EvaluationException{
        try {
            Object value = scriptEngine.eval(SANDBOX + script);
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
            writeError("Returned type: " + value.getClass().getCanonicalName());
            return false;
        } catch (ScriptException e) {
            throw new EvaluationException("Cannot evaluate! " + script, e);
        }
    }
    public Object computeObject(String script) throws EvaluationException{
        try {
            Object value = scriptEngine.eval(SANDBOX + script);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof Boolean) {
                return (Boolean) value ? 1 : 0;
            }
            writeError("Returned type: " + value.getClass().getCanonicalName());
            return null;
        } catch (ScriptException e) {
            throw new EvaluationException("Cannot evaluate! " + script, e);
        }
    }
    public void writeError(String s)throws PrintingException{
        try {
            scriptContext.getErrorWriter().write(s+"\n");
        }catch (IOException e){
            throw new PrintingException("Cannot print error! "+s,e);
        }
    }
    public void write(String s) throws PrintingException{
        try {
            scriptContext.getWriter().write(s+"\n");
        }catch (IOException e){
            throw new PrintingException("Cannot print! "+s,e);
        }
    }
    public void removeBinding(String name) throws InvalidBinding{
        Binding old=compilerBindings.getBinding(name);
        if(old==null){
            throw new InvalidBinding("Cannot remove binding name is unused! "+name);
        }
        switch (old.type){
            case SET: case DEF: break;
            default: throw new InvalidBinding("Cannot remove that binding! "+old.type.name()+" "+name);
        }
        compilerBindings.remove(name);
    }
    public boolean containsNotDefs(String... keys) throws InvalidBinding{
        for(String name:keys){
            if(name==null || name.length()==0 || !name.matches(NAME_FORMAT)){
                throw new InvalidBinding("Invalid binding name specified! "+name);
            }
        }
        return compilerBindings.containsNotDefs(keys);
    }
    public boolean lacksNotDefs(String... keys) throws InvalidBinding{
        for(String name:keys){
            if(name==null || name.length()==0 || !name.matches(NAME_FORMAT)){
                throw new InvalidBinding("Invalid binding name specified! "+name);
            }
        }
        return compilerBindings.lacksNotDefs(keys);
    }
    //endregion

    //region parse
    public Number parseValue(String value){
        if(compilerBindings.containsKey(value)) {
            return compilerBindings.getBinding(value).value;
        }
        return parseNumberAdvanced(value);
    }

    public static Number parseNumberAdvanced(String str){
        str=str.replaceAll("_","");
        if(str.contains(".")) {
            return Double.parseDouble(str);
        }if (str.contains("0x") | str.contains("0X")) {
            str = str.replaceAll("0[xX]", "");
            return Integer.parseInt(str, 16);
        } else if (str.contains("0b") | str.contains("0B")) {
            str = str.replaceAll("0[bB]", "");
            return Integer.parseInt(str, 2);
        } else if (str.startsWith("-0") | str.startsWith("0")) {
            return Integer.parseInt(str, 8);
        } else {
            return Integer.parseInt(str, 10);
        }
    }
    //endregion

    //region conditional compiler
    public void openIf(boolean compilationEnable){
        if(compilationEnabled.size()>1){
            if(compilationEnabled.get(compilationEnabled.size()-1)!=ConditionalState.ASSEMBLING){//if not compile
                compilationEnabled.add(ConditionalState.DISABLED);
                return;
            }
        }
        compilationEnabled.add(compilationEnable?ConditionalState.ASSEMBLING:ConditionalState.CHECKING);
    }
    public void elseIf(boolean compilationEnable) throws InvalidConditionalAssembly,InvalidConditionalEvaluation {
        if (compilationEnabled.size() <= 1) {
            throw new InvalidConditionalAssembly("Missing if opening statement!");
        }
        ConditionalState b = compilationEnabled.get(compilationEnabled.size() - 1);
        if (b == ConditionalState.CHECKING) {
            compilationEnabled.set(compilationEnabled.size()-1,compilationEnable?ConditionalState.ASSEMBLING:ConditionalState.CHECKING);
        } else if (b == ConditionalState.ASSEMBLING) {
            compilationEnabled.set(compilationEnabled.size() - 1, ConditionalState.DISABLED);
        }else{
            throw new InvalidConditionalEvaluation("Not ready to evaluate!");
        }
    }
    public void endIf() throws InvalidConditionalAssembly {
        if (compilationEnabled.size() <= 1) {
            throw new InvalidConditionalAssembly("Missing if opening statement!");
        }
        compilationEnabled.remove(compilationEnabled.size()-1);
    }
    public boolean isCompilationEnabled(){
        return compilationEnabled.get(compilationEnabled.size()-1)==ConditionalState.ASSEMBLING;
    }
    public void setConditionalState(ConditionalState state){
        compilationEnabled.set(compilationEnabled.size()-1,state);
    }
    public int getDepth(){
        return compilationEnabled.size();
    }
    //endregion

    //region macro compiler
    public void addMacro(String name) throws InvalidMacroStatement{
        if(name==null || name.length()==0 || !name.matches(NAME_FORMAT)){
            throw new InvalidMacroStatement("Invalid macro name specified! "+name);
        }
        macros.put(name,new ArrayList<>());
        editingMacros.add(name);
    }
    private void addToMacros(String line) throws InvalidMacroStatement{
        if(line==null){
            throw new InvalidMacroStatement("Invalid macro line specified!");
        }
        if(editingMacros.isEmpty()){
            throw new InvalidMacroStatement("Is not editing any macros!");
        }
        editingMacros.forEach(s -> macros.get(s).add(line));
    }
    public void finishMacro(String name) throws InvalidMacroStatement{
        if (!editingMacros.contains(name)) {
            throw new InvalidMacroStatement("Macro is not being edited! "+name);
        }
        editingMacros.remove(name);
    }
    public boolean isEditingMacros(){
        return editingMacros.size()>0;
    }
    private ArrayList<String> writeMacro(String name,String args) throws InvalidMacroStatement{
        ArrayList<String> macro=macros.get(name);
        if(macro==null){
            throw new InvalidMacroStatement("Macro was never defined! "+name);
        }
        String[] arg=splitExpressionsString(args);
        for (int i = 0; i < arg.length; i++) {
            arg[i]='('+arg[i]+')';
        }
        ArrayList<String> newLines=new ArrayList<>();
        ArrayList<String> labels=new ArrayList<>();
        for(String s:macro) {
            String label = getLabelOrPointerName(s);
            if(label!=null){
                labels.add(label);
            }
        }
        for(String s:macro){
            String temp=s;
            for(int i=arg.length-1;i>=0;i--){
                temp=temp.replaceAll("@"+i+"(?:[^0-9].*)?",arg[i]);
            }
            if(temp.contains("@")){
                throw new InvalidMacroStatement("Cannot resolve all parameters! "+s+" "+currentLine);
            }
            for(String l:labels){
                s=s.replaceAll("(?:.*"+NAME_TERMINATOR+")?"+l+"(?:"+NAME_TERMINATOR+".*)?",l+"__MACRO__"+currentLine);
            }
            newLines.add(temp);
        }
        return newLines;
    }
    private void injectMacro(String name,String args) throws InvalidMacroStatement{
        ArrayList<String> macro=writeMacro(name,args);
        int nextLine=currentLine+1;

        ArrayList<Position> pos=new ArrayList<>();
        ArrayList<Boolean> bools=new ArrayList<>();
        for(int i=0;i<macro.size();i++){
            pos.add(new Position(i,"@"+macro));
            bools.add(false);
        }
        positions.addAll(nextLine,pos);
        processedLines.addAll(nextLine,bools);
        lines.addAll(nextLine,macro);
    }
    public boolean isMacroDefined(String name){
        return macros.containsKey(name) && !editingMacros.contains(name);
    }
    //endregion

    //region listing
    public void setListing(ListingMode mode) throws InvalidListingMode{
        if(mode==null){
            throw new InvalidListingMode("Listing mode cannot be null!");
        }
        listing=mode;
    }
    public boolean isListing(){
        return listing!=ListingMode.NO_LIST;
    }
    public boolean isMacroListing(){
        return listing==ListingMode.LIST_MACRO;
    }
    //endregion

    //region directive
    public IDirective putDirective(String name,IDirective directive) throws InvalidDirective {
        if(name==null || name.length()==0 || !name.matches(NAME_FORMAT)){
            throw new InvalidDirective("Invalid directive name specified! "+name);
        }
        return instanceDirectives.put(name,directive);
    }
    public IDirective getDirective(String name) throws InvalidDirective {
        if(name==null || name.length()==0 || !name.matches(NAME_FORMAT)){
            throw new InvalidDirective("Invalid directive name specified! "+name);
        }
        IDirective directive= instanceDirectives.get(name);
        if(directive!=null && (isCompilationEnabled() || directive.isUnskippable())){
            return directive;
        }
        directive=Directive.DEFINED_DIRECTIVES.get(name);
        if(directive!=null && (isCompilationEnabled() || directive.isUnskippable())){
            return directive;
        }
        if(isCompilationEnabled() && directive==null){
            throw new InvalidDirective("Directive is not defined! "+name);
        }
        return null;
    }
    //endregion
}
