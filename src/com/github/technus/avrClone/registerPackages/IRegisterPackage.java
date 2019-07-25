package com.github.technus.avrClone.registerPackages;

import com.github.technus.avrClone.interrupt.IInterrupt;
import com.github.technus.avrClone.memory.IDataMemoryDefinition;

import java.util.HashMap;
import java.util.TreeMap;

public interface IRegisterPackage extends IDataMemoryDefinition {
    HashMap<Integer,String> names();
    HashMap<String,Integer> registers();
    HashMap<String,Integer> registerPairs();
    HashMap<String,int[]> registerBits();
    TreeMap<Integer, IInterrupt> interrupts();
}
