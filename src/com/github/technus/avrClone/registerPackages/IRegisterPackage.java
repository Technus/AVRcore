package com.github.technus.avrClone.registerPackages;

import com.github.technus.avrClone.memory.IDataMemoryDefinition;

import java.util.ArrayList;
import java.util.Map;

public interface IRegisterPackage extends IDataMemoryDefinition {
    /**
     * Register raw address to name
     * @return
     */
    Map<String,IRegister> nameRegisterMap();

    /**
     * Register name to raw address
     * @return
     */
    Map<Integer, ArrayList<IRegister>> addressesNamesMap();

    /**
     * Register bit name to Definition
     * @return
     */
    Map<String,IRegisterBit> bitsMap();

    /**
     * Interrupt vector to Definiton
     * @return
     */
    Map<Integer, IInterrupt> interruptsMap();
}
