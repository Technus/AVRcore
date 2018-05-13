package com.github.technus.avrCloneGui;

import com.bulenkov.darcula.DarculaLaf;
import com.github.technus.avrClone.AvrCore;
import com.github.technus.avrClone.instructions.InstructionRegistry;
import com.github.technus.avrClone.registerPackages.GPIO_Registers;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try{
            UIManager.setLookAndFeel(new DarculaLaf());
        }catch (Exception e){
            e.printStackTrace();
        }

        AvrCore core=new AvrCore(InstructionRegistry.INSTRUCTION_REGISTRY_OP,false);
        core.simpleInit();
        core.putRegistersBindings(new GPIO_Registers(0x00),"GPIOR");
        try {
            core.setProgramMemory("DES 0 0", "SBRS 0 8","JMP 0","nopt 0");
        }catch (Exception e){
            e.printStackTrace();
        }

        new AvrTest(core).show();
    }
}
