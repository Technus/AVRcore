package com.github.technus.avrClone.compiler;

import com.github.technus.avrClone.compiler.directives.IDirective;
import com.github.technus.avrClone.compiler.directives.Directive;
import com.github.technus.avrClone.compiler.directives.InvalidDirective;
import com.github.technus.avrClone.compiler.exceptions.*;
import com.github.technus.avrClone.compiler.js.CompilerBindings;
import com.github.technus.avrClone.compiler.js.CompilerContext;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.*;

public final class ProgramCompiler {
    public ProgramCompiler(){}

    private HashMap<String,ArrayList<String>> files =new HashMap<>();
    private HashMap<String,HashSet<Integer>> unusedLines=new HashMap<>();

    private ArrayList<String> intermediate =new ArrayList<>();
    private ArrayList<String> programOutput=new ArrayList<>();

    public ArrayList<String> writeProgram(ArrayList<String> lines) throws Exception{
        for (int i=0;i<lines.size();i++) {
            lines.set(i,sanitizeLine(lines.get(i)));
        }
        //work on lines
        //parse bindings,macro lengths, execute directives no forward reference allowed in directives
        compilerBindings.removeAllUnsafely(Binding.NameType.DEF,Binding.NameType.SET);
        //work on program
        //parse rest, back reference must be allowed only for labels
    }

    //region line consumer
    public ArrayList<String> firstPass(String line){

    }

    public ArrayList<String> secondPass(String line){

    }
    public String sanitizeLine(String line){
        return line
                .replaceAll("[\\s\\r\\n]+"," ")
                .replaceFirst("^ ","")
                .replaceFirst(" $","")
                .replaceFirst(" *: *",":")
                .replaceFirst(" *;.*$","")
                .replaceFirst(" *\\\\.*$","");
    }
    //endregion

    //region include
    public interface Includer {
        ArrayList<String> include(String path) throws CompilerException;
    }
    private Includer includer = path -> {
        File f=new File(path);
        if(!f.exists()){
            throw new InvalidInclude("File does not exist! "+path);
        }
        if(f.isDirectory()){
            throw new InvalidInclude("Cannot include a directory! "+path);
        }
        if(!f.canRead()){
            throw new InvalidInclude("File is not readable! "+path);
        }
        try{
            return (ArrayList<String>) Files.readAllLines(f.toPath());
        }catch (Exception e){
            throw new InvalidInclude("Failed to read file! "+path,e);
        }
    };
    public void setIncluder(Includer include) {
        this.includer = include;
    }
    public Includer getIncluder() {
        return includer;
    }
    public void include(String file) throws CompilerException{
        if (includer == null) {
            throw new InvalidInclude("No include processor set!");
        }
        files.put(file,includer.include(file));
    }
    //endregion

    //region segment
    public enum Segment{
        CSEG,DSEG,ESEG
    }
    private Segment currentSegment=Segment.CSEG;
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
    private int[] startOffset=new int[Segment.values().length];
    {
        startOffset[Segment.ESEG.ordinal()]=4096;
    }
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
    private boolean[] overlap=new boolean[Segment.values().length];
    public void setCurrentOverlap(boolean value){
        overlap[currentSegment.ordinal()]=value;
    }
    public boolean getCurrentOverlap() {
        return overlap[currentSegment.ordinal()];
    }
    public boolean getOverlap(Segment segment) {
        return overlap[segment.ordinal()];
    }
    //endregion

    //region origin
    private int[] origins =new int[Segment.values().length];
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

    //region mem use
    private BitSet[] usedMemoryRanges =new BitSet[Segment.values().length];
    {
        for(int i = 0; i< usedMemoryRanges.length; i++){
            usedMemoryRanges[i]=new BitSet(4096);
        }
    }
    public int getCurrentMemorySize(){
        return usedMemoryRanges[currentSegment.ordinal()].length();
    }
    public int getMemorySize(Segment segment){
        return usedMemoryRanges[segment.ordinal()].length();
    }
    public boolean isCurrentMemoryCellFree(){
        return usedMemoryRanges[currentSegment.ordinal()].get(getCurrentOrigin());
    }
    public boolean isCurrentMemoryCellFree(int address) throws InvalidMemoryAccess {
        if(address<0){
            throw new InvalidMemoryAccess("Memory address must be not negative! "+address);
        }
        return usedMemoryRanges[currentSegment.ordinal()].get(address);
    }
    public boolean isMemoryCellFree(Segment segment,int address) throws InvalidMemoryAccess {
        if(address<0){
            throw new InvalidMemoryAccess("Memory address must be not negative! "+address);
        }
        return usedMemoryRanges[segment.ordinal()].get(address);
    }
    public boolean isCurrentMemoryRangeFree(int toExclusive) throws InvalidMemoryAccess {
        if(getCurrentOrigin()>=toExclusive){
            throw new InvalidMemoryAccess("Range end must be greater than origin! "+getCurrentOrigin()+" !< "+toExclusive);
        }
        return usedMemoryRanges[currentSegment.ordinal()].nextClearBit(getCurrentOrigin())>=toExclusive;
    }
    public boolean isCurrentMemoryRangeFree(int fromInclusive,int toExclusive) throws InvalidMemoryAccess {
        if(fromInclusive<0){
            throw new InvalidMemoryAccess("Range start must be not negative! "+fromInclusive);
        }
        if(fromInclusive>=toExclusive){
            throw new InvalidMemoryAccess("Range end must be greater than range start! "+fromInclusive+" !< "+toExclusive);
        }
        return usedMemoryRanges[currentSegment.ordinal()].nextClearBit(fromInclusive)>=toExclusive;
    }
    public boolean isMemoryRangeFree(Segment segment,int fromInclusive,int toExclusive) throws InvalidMemoryAccess{
        if(fromInclusive<0){
            throw new InvalidMemoryAccess("Range start must be not negative! "+fromInclusive);
        }
        if(fromInclusive>=toExclusive){
            throw new InvalidMemoryAccess("Range end must be greater than range start! "+fromInclusive+" !< "+toExclusive);
        }
        return usedMemoryRanges[segment.ordinal()].nextClearBit(fromInclusive)>=toExclusive;
    }
    public boolean isCurrentMemoryBlockFree(int size) throws InvalidMemoryAccess {
        if(size<=0){
            throw new InvalidMemoryAccess("Block size must be positive! "+size);
        }
        int offset=getCurrentOrigin();
        return usedMemoryRanges[currentSegment.ordinal()].nextClearBit(offset)>=offset+size;
    }
    public boolean isCurrentMemoryBlockFree(int start,int size)throws InvalidMemoryAccess{
        if(start<0){
            throw new InvalidMemoryAccess("Block start must be not negative! "+start);
        }
        if(size<=0){
            throw new InvalidMemoryAccess("Block size must be positive! "+size);
        }
        return usedMemoryRanges[currentSegment.ordinal()].nextClearBit(start)>=start+size;
    }
    public boolean isMemoryBlockFree(Segment segment,int start,int size)throws InvalidMemoryAccess{
        if(start<0){
            throw new InvalidMemoryAccess("Block start must be not negative! "+start);
        }
        if(size<=0){
            throw new InvalidMemoryAccess("Block size must be positive! "+size);
        }
        return usedMemoryRanges[segment.ordinal()].nextClearBit(start)>=start+size;
    }
    //endregion

    //region constants
    @SuppressWarnings("unchecked")
    private TreeMap<Integer,Integer>[] constants=new TreeMap[Segment.values().length];
    {
        for(int i=0;i<constants.length;i++){
            constants[i]=new TreeMap<>();
        }
    }
    public boolean putConstant(int constant) throws InvalidOrigin{
        if(getCurrentOverlap() || isCurrentMemoryCellFree()){
            int segment=currentSegment.ordinal();
            int origin=getCurrentOrigin();
            usedMemoryRanges[segment].set(origin);
            constants[segment].put(origin,constant);
            offsetCurrentOrigin(1);
            return true;
        }
        return false;
    }
    public boolean putConstant(float constant) throws InvalidOrigin{
        if(getCurrentOverlap() || isCurrentMemoryCellFree()){
            int segment=currentSegment.ordinal();
            int origin=getCurrentOrigin();
            usedMemoryRanges[segment].set(origin);
            constants[segment].put(origin,Float.floatToIntBits(constant));
            offsetCurrentOrigin(1);
            return true;
        }
        return false;
    }
    public boolean putConstant(long constant) throws InvalidOrigin,InvalidMemoryAccess{
        if(getCurrentOverlap() || isCurrentMemoryBlockFree(2)){
            int segment=currentSegment.ordinal();
            int origin=getCurrentOrigin();
            usedMemoryRanges[segment].set(origin,origin+2);
            constants[segment].put(origin,(int)constant);
            constants[segment].put(origin+1,(int)(constant>>32));
            offsetCurrentOrigin(2);
            return true;
        }
        return false;
    }
    public boolean putConstant(String constant) throws InvalidOrigin,InvalidMemoryAccess,InvalidConstant {
        if(constant==null){
            throw new InvalidConstant("String constant cannot be null!");
        }
        if(constant.length()==0){
            throw new InvalidConstant("String constant must not be empty!");
        }
        int length=constant.length();
        if(getCurrentOverlap() || isCurrentMemoryBlockFree(length)){
            int segment=currentSegment.ordinal();
            int origin=getCurrentOrigin();
            usedMemoryRanges[segment].set(origin,origin+length);
            for(int i=0;i<constant.length();i++){
                constants[segment].put(origin+i,(int)constant.charAt(i));
            }
            offsetCurrentOrigin(length);
            return true;
        }
        return false;
    }
    public boolean reserveMemory(int cellCount) throws InvalidOrigin,InvalidMemoryAccess,InvalidMemoryAllocation {
        if(cellCount<=0){
            throw new InvalidMemoryAllocation("Memory block size must have positive size! "+cellCount);
        }
        if(getCurrentOverlap() || isCurrentMemoryBlockFree(cellCount)){
            int segment=currentSegment.ordinal();
            int origin=getCurrentOrigin();
            usedMemoryRanges[segment].set(origin,origin+cellCount);
            for(int i=0;i<cellCount;i++){
                constants[segment].put(origin+i,0);
            }
            offsetCurrentOrigin(cellCount);
            return true;
        }
        return false;
    }
    //endregion

    //region compute
    private CompilerBindings compilerBindings = new CompilerBindings();
    private static SimpleBindings globalBindings = new SimpleBindings();
    private CompilerContext scriptContext =new CompilerContext(compilerBindings,globalBindings);
    private NashornScriptEngine scriptEngine;
    private static String sb =
            "var eval=function(){};var uneval=function(){};" +
            "var decodeURI=function(){};var decodeURIComponent=function(){};" +
            "var encodeURI=function(){};var encodeURIComponent=function(){};" +
            "var escape=function(){};var unescape=function(){};" +
            "var quit=function(){};var exit=function(){};" +
            "var print=function(){};var echo = function(){};" +
            "var readFully=function(){};" + "var readLine=function(){};" +
            "var load=function(){};var loadWithNewGlobal=function(){};\n";
    {
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        ccl= ccl == null ? NashornScriptEngineFactory.class.getClassLoader() : ccl;
        scriptEngine=(NashornScriptEngine)new NashornScriptEngineFactory().getScriptEngine(
                new String[]{"--no-java"}, ccl, s -> false);
        scriptEngine.setContext(scriptContext);
    }
    public String computeString(String script) throws EvaluationException,PrintingException{
        try {
            Object value = scriptEngine.eval(sb + script);
            if(value instanceof String){
                return (String) value;
            }
            scriptContext.getErrorWriter().write("Returned type: "+value.getClass().getCanonicalName());
            return value.toString();
        }catch (ScriptException e){
            throw new EvaluationException("Cannot evaluate! "+script,e);
        }catch (IOException e){
            throw new PrintingException("Cannot print return type!",e);
        }
    }
    public Number computeValue(String script) throws EvaluationException,PrintingException{
        try {
            Object value = scriptEngine.eval(sb + script);
            if (value instanceof Number) {
                return (Number) value;
            } else if (value instanceof Boolean) {
                return (Boolean) value ? 1 : 0;
            }
            scriptContext.getErrorWriter().write("Returned type: " + value.getClass().getCanonicalName());
            return 0;
        } catch (ScriptException e) {
            throw new EvaluationException("Cannot evaluate! " + script, e);
        } catch (IOException e) {
            throw new PrintingException("Cannot print return type!", e);
        }
    }
    public Boolean computeBoolean(String script) throws EvaluationException,PrintingException{
        try {
            Object value = scriptEngine.eval(sb + script);
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
            scriptContext.getErrorWriter().write("Returned type: " + value.getClass().getCanonicalName());
            return false;
        } catch (ScriptException e) {
            throw new EvaluationException("Cannot evaluate! " + script, e);
        } catch (IOException e) {
            throw new PrintingException("Cannot print return type!", e);
        }
    }
    public Object computeObject(String script) throws EvaluationException,PrintingException{
        try {
            Object value = scriptEngine.eval(sb + script);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof Boolean) {
                return (Boolean) value ? 1 : 0;
            }
            scriptContext.getErrorWriter().write("Returned type: " + value.getClass().getCanonicalName());
            return value;
        } catch (ScriptException e) {
            throw new EvaluationException("Cannot evaluate! " + script, e);
        } catch (IOException e) {
            throw new PrintingException("Cannot print return type!", e);
        }
    }
    public Writer getErrorWriter(){
        return scriptContext.getErrorWriter();
    }
    public Writer getWriter(){
        return scriptContext.getWriter();
    }
    public void writeError(String s)throws PrintingException{
        try {
            scriptContext.getErrorWriter().write(s);
        }catch (IOException e){
            throw new PrintingException("Cannot print error! "+s,e);
        }
    }
    public void write(String s) throws PrintingException{
        try {
            scriptContext.getWriter().write(s);
        }catch (IOException e){
            throw new PrintingException("Cannot print! "+s,e);
        }
    }
    public void putBinding(String name,Binding binding) throws InvalidBinding{
        if(name==null || name.length()==0 || name.replaceFirst("[a-zA-Z][0-9a-zA-Z_]*","").length()>0){
            throw new InvalidBinding("Invalid binding name specified! "+name);
        }
        Binding old=compilerBindings.get(name);
        if(old!=null) {
            switch (old.type) {
                case SET: case DEF: break;
                default: throw new InvalidBinding("Cannot rewrite that binding! " + old.type.name()+" "+name);
            }
        }
        compilerBindings.put(name,binding);
    }
    public void removeBinding(String name) throws InvalidBinding{
        Binding old=compilerBindings.get(name);
        if(old==null){
            throw new InvalidBinding("Cannot remove binding name is unused! "+name);
        }
        switch (old.type){
            case SET: case DEF: break;
            default: throw new InvalidBinding("Cannot remove that binding! "+old.type.name()+" "+name);
        }
        compilerBindings.remove(name);
    }
    public boolean containsNotDefs(String... keys){
        return compilerBindings.containsNotDefs(keys);
    }
    public boolean lacksNotDefs(String... keys){
        return compilerBindings.lacksNotDefs(keys);
    }
    //endregion

    //region parse
    public Number parseValue(String value){
        if(compilerBindings.containsKey(value)) {
            return compilerBindings.get(value).value;
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
    //true - yes curretnly is doing it
    //false - did that on this level, or skipping this level
    //null - didn't do that on this level
    private ArrayList<Boolean> compilationEnabled=new ArrayList<>();
    public void openIf(boolean compilationEnable){
        if(compilationEnabled.size()>0){
            Boolean b=compilationEnabled.get(compilationEnabled.size()-1);
            if(b==null || !b){//if not compile
                compilationEnabled.add(false);
                return;
            }
        }
        compilationEnabled.add(compilationEnable?true:null);
    }
    //null for else
    public void elseIf(Boolean compilationEnable) throws InvalidConditionalStatement {
        if (compilationEnabled.size() <= 0) {
            throw new InvalidConditionalStatement("Missing if opening statement!");
        }
        Boolean b = compilationEnabled.get(compilationEnabled.size() - 1);
        if (b == null) {
            if (compilationEnable == null) {
                compilationEnabled.set(compilationEnabled.size() - 1, true);
            } else {
                compilationEnabled.set(compilationEnabled.size() - 1, compilationEnable ? true : null);
            }
        } else {
            compilationEnabled.set(compilationEnabled.size() - 1, false);
        }
    }
    public void endIf() throws InvalidConditionalStatement {
        if (compilationEnabled.size() <= 0) {
            throw new InvalidConditionalStatement("Missing if opening statement!");
        }
        compilationEnabled.remove(compilationEnabled.size()-1);
    }
    public boolean isCompilationEnabled(){
        if(compilationEnabled.size()==0){
            return true;
        }
        Boolean b=compilationEnabled.get(compilationEnabled.size()-1);
        return b!=null && b;
    }
    public int getDepth(){
        return compilationEnabled.size();
    }
    //endregion

    //region macro compiler
    private HashMap<String,ArrayList<String>> macros=new HashMap<>();
    private HashSet<String> editingMacros =new HashSet<>();
    public void addMacro(String name) throws InvalidMacroStatement{
        if(name==null || name.length()==0 || name.replaceFirst("[a-zA-Z][0-9a-zA-Z_]*","").length()>0){
            throw new InvalidMacroStatement("Invalid macro name specified! "+name);
        }
        macros.put(name,new ArrayList<>());
        editingMacros.add(name);
    }
    public void addToMacros(String line) throws InvalidMacroStatement{
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
    public boolean isNotEditingMacros(){
        return editingMacros.isEmpty();
    }
    public ArrayList<String> writeMacro(String name,String args) throws InvalidMacroStatement,EvaluationException,PrintingException{
        String[] arg=args.split(",");
        ArrayList<String> macro=macros.get(name);
        if(macro==null){
            throw new InvalidMacroStatement("Macro was never defined! "+name);
        }
        Object[] values=new Number[arg.length];
        for(int i=0;i<values.length;i++){
            values[i]=computeObject(arg[i]);
        }
        ArrayList<String> newLines=new ArrayList<>();
        for(String s:macro){
            String temp=s;
            for(int i=0;i<values.length;i++){
                temp=temp.replaceAll("@"+i,values[i].toString());
            }
            if(temp.contains("@")){
                throw new InvalidMacroStatement("Cannot write macro line! "+s);
            }
            newLines.add(temp);
        }
        return newLines;
    }
    //endregion

    //region listing
    public enum ListingMode{
        NO_LIST,LIST,LIST_MACRO
    }
    private ListingMode listing=ListingMode.LIST;
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
    public HashMap<String,IDirective> localDirectives=new HashMap<>();
    public HashSet<String> localUnskippableDirectives =new HashSet<>();
    public IDirective getDirective(String name) throws InvalidDirective {
        IDirective directive=localDirectives.get(name);
        if(directive!=null && (isCompilationEnabled() || localUnskippableDirectives.contains(name))){
            return directive;
        }
        directive=Directive.DEFINED_DIRECTIVES.get(name);
        if(directive!=null && (isCompilationEnabled() || Directive.UNSKIPPABLE_DIRECTIVES.contains(name))){
            return directive;
        }
        if(isCompilationEnabled()){
            throw new InvalidDirective("IDirective is not defined! "+name);
        }
        return null;
    }
    //endregion
}
