package com.github.technus.avrClone.compiler;

import java.util.ArrayList;

public class LineConsumer {
    public static final String NAME_FORMAT ="([a-zA-Z][0-9a-zA-Z_]*)";
    public static final String NAME_TERMINATOR ="[^0-9a-zA-Z_]";
    private static final String NO_EXPRESSIONS[] =new String[0],NO_EXPRESSION="";

    private LineConsumer(){}

    public static String sanitizeLine(String line){
        return line
                .replaceAll("[\\s\\r\\n]+"," ")
                .replaceFirst(" *;.*$","")
                .replaceFirst(" *\\\\\\\\.*$","")
                .replaceFirst("^ ","")
                .replaceFirst(" $","")
                .replaceAll(" *: *",":")
                .replaceAll(" *\\. *",".");
    }
    public static ArrayList<String> sanitizeList(ArrayList<String> list){
        ArrayList<String> newList=new ArrayList<>();
        for(String s:list){
            newList.add(sanitizeLine(s));
            //todo multi lines, what about pointers...
        }
        return newList;
    }
    public static boolean containsLabelOrPointerName(String line){
        return line.matches('^'+ NAME_FORMAT +":.*$");
    }
    public static String getLabelOrPointerName(String line){
        return containsLabelOrPointerName(line)?line.replaceFirst(":.*$",""):null;
    }
    public static boolean containsDirectiveName(String line){
        return line.matches("^(?:"+NAME_FORMAT+":)?\\."+NAME_FORMAT+"(?: .*)?$");
    }
    public static String getDirectiveName(String line){
        return containsDirectiveName(line)?line.replaceFirst("^(?:"+NAME_FORMAT+":)?\\.","").replaceFirst(" .*$",""):null;
    }
    public static boolean containsMnemonic(String line){//either macro or operand
        return line.matches("^(?:"+NAME_FORMAT+":)?"+NAME_FORMAT+"(?: .*)?$");
    }
    public static String getMnemonic(String line){//either macro or operand
        return containsMnemonic(line)?line.replaceFirst("^(?:"+NAME_FORMAT+":)?","").replaceFirst(" .*$",""):null;
    }
    public static String getExpressionsString(String line){
        if(containsMnemonic(line) || containsDirectiveName(line)){
            return line.replaceFirst("^(?:"+NAME_FORMAT+":)?\\.?"+NAME_FORMAT+" ?","");
        }else{
            return NO_EXPRESSION;
        }
    }
    public static String[] splitExpressionsString(String expressions){
        if(expressions==null){
            return NO_EXPRESSIONS;
        }
        if(expressions.contains("`")){
            return expressions.split("`");
        }else {
            return expressions.split(",");
        }
    }
}
