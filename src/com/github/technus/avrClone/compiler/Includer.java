package com.github.technus.avrClone.compiler;


import com.github.technus.avrClone.compiler.exceptions.CompilerException;
import com.github.technus.avrClone.compiler.exceptions.InvalidInclude;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;

public interface Includer {
    Includer FILE_SYSTEM_INCLUDER = path -> {
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

    ArrayList<String> include(String path) throws CompilerException;
}
