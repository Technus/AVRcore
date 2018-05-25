package com.github.technus.avrCloneGui;

import com.bulenkov.darcula.DarculaLaf;
import com.github.technus.avrClone.AvrCore;
import com.github.technus.avrClone.instructions.InstructionRegistry;
import com.github.technus.avrClone.compiler.ProgramCompiler;
import com.github.technus.avrClone.registerPackages.GPIO_Registers;

import javax.swing.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ProgramCompiler programCompiler= new ProgramCompiler();
        Scanner sc = new Scanner(System.in);
        String val="";
        while(true) {
            try {
                System.out.println("programCompiler.computeValue(System.in.read()) = " + programCompiler.computeValue(val));
            } catch (Exception e) {
                e.printStackTrace();
                if(0!=0)break;
            }
        }

        try{
            UIManager.setLookAndFeel(new DarculaLaf());
        }catch (Exception e){
            e.printStackTrace();
        }

        AvrCore core=new AvrCore(InstructionRegistry.INSTRUCTION_REGISTRY_OP,false);
        core.simpleInit();
        core.putRegistersBindings(new GPIO_Registers(0x00),"GPIOR");

        new AvrTest(core).show();
    }
}
