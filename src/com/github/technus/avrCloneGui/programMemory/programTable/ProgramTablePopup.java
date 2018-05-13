package com.github.technus.avrCloneGui.programMemory.programTable;

import com.github.technus.avrCloneGui.AvrTest;
import com.github.technus.avrCloneGui.presentation.PresentationCellRenderer;
import com.github.technus.avrCloneGui.presentation.Presentations;
import jsyntaxpane.components.LineNumbersRuler;

import javax.swing.*;

public class ProgramTablePopup extends JPopupMenu {
    public ProgramTablePopup(AvrTest test, JTable table, ProgramTableModel model, PresentationCellRenderer renderer){
        JMenuItem item=new JMenuItem("Set PC");
        item.addActionListener(e -> {
            int pc=table.getSelectedRow();
            if (pc>=0) {
                test.core.programCounter = pc;
                test.refreshRegistersDataPc();
            }
        });
        add(item);

        item=new JMenuItem("Write ASM HEX");
        item.addActionListener(e -> {
            if(test.core.getProgramMemory()!=null){
                LineNumbersRuler.enableASM(16);
                test.writeASM(test.core.getProgramMemory().getProgram(16));
                test.refreshASM();
            }
        });
        add(item);

        item=new JMenuItem("Write ASM OCT");
        item.addActionListener(e -> {
            if(test.core.getProgramMemory()!=null){
                LineNumbersRuler.enableASM(8);
                test.writeASM(test.core.getProgramMemory().getProgram(8));
                test.refreshASM();
            }
        });
        add(item);

        item=new JMenuItem("Write ASM BIN");
        item.addActionListener(e -> {
            if(test.core.getProgramMemory()!=null){
                LineNumbersRuler.enableASM(2);
                test.writeASM(test.core.getProgramMemory().getProgram(2));
                test.refreshASM();
            }
        });
        add(item);

        item=new JMenuItem("Write ASM DEC");
        item.addActionListener(e -> {
            if(test.core.getProgramMemory()!=null){
                LineNumbersRuler.enableASM(10);
                test.writeASM(test.core.getProgramMemory().getProgram(10));
                test.refreshASM();
            }
        });
        add(item);

        for(Presentations p:Presentations.values()){
            item=new JMenuItem(p.name());
            item.addActionListener(e -> {
                int[] c= table.getSelectedColumns();
                for (int i:c){
                    renderer.presentations[i]=p;
                }
                if(c.length>0){
                    model.refreshProgramMemoryView();
                }
            });
            add(item);

        }
    }
}
