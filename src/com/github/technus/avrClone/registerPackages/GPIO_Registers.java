package com.github.technus.avrClone.registerPackages;

import com.github.technus.avrClone.interrupt.IInterrupt;

import java.util.TreeMap;

public class GPIO_Registers extends RegisterPackage {
    public GPIO_Registers(int offset){
        super(offset,0x10);
        for(Register r: Register.values()){
            singles.put(r.name(),r.ordinal()+offset);
            names.put(r.ordinal()+offset,r.name());
        }
    }

    public enum Register implements IRegister<GPIO_Registers> {
        GPIOR0(), GPIOR1(), GPIOR2(), GPIOR3(),
        GPIOR4(), GPIOR5(), GPIOR6(), GPIOR7(),
        GPIOR8(), GPIOR9(), GPIOR10(),GPIOR11(),
        GPIOR12(),GPIOR13(),GPIOR14(),GPIOR15();

        @Override
        public int getOffset(GPIO_Registers registers) {
            return ordinal()+registers.getOffset();
        }
    }

    @Override
    public TreeMap<Integer, IInterrupt> interrupts() {
        return null;
    }
}
