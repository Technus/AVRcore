package com.github.technus.avrClone.registerPackages;

import com.github.technus.avrClone.interrupt.I_Interrupt;
import com.github.technus.avrClone.memory.I_DataMemoryDefinition;

import java.util.HashMap;
import java.util.TreeMap;

public interface I_RegisterPackage extends I_DataMemoryDefinition {
    HashMap<Integer,String> names();
    HashMap<String,Integer> registers();
    HashMap<String,Integer> registerPairs();
    HashMap<String,int[]> registerBits();
    TreeMap<Integer,I_Interrupt> interrupts();
}
