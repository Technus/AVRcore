package com.github.technus.avrClone.registerPackages;

import com.github.technus.avrClone.interrupt.IInterrupt;

import java.util.TreeMap;

public class CPU_Registers extends RegisterPackage {
    public final int SREG,SP,defaultSP;
    public final static int
            I=RegisterBit.I.getBitMask(),
            _I=~RegisterBit.I.getBitMask(),
            T=RegisterBit.T.getBitMask(),
            H=RegisterBit.H.getBitMask(),
            S=RegisterBit.S.getBitMask(),
            V=RegisterBit.V.getBitMask(),
            N=RegisterBit.N.getBitMask(),
            Z=RegisterBit.Z.getBitMask(),
            C=RegisterBit.C.getBitMask(),
            U=RegisterBit.U.getBitMask(),//cheeky boye disables H status compute
            H1=RegisterBit.H1.getBitMask(),//is H
            H2=RegisterBit.H2.getBitMask(),
            H3=RegisterBit.H3.getBitMask(),
            H4=RegisterBit.H4.getBitMask(),
            H5=RegisterBit.H5.getBitMask(),
            H6=RegisterBit.H6.getBitMask(),
            H7=RegisterBit.H7.getBitMask(),
            H8=RegisterBit.H8.getBitMask();//is C
    
    public CPU_Registers(int offset,int defaultSP){
        super(offset,0x10);
        this.defaultSP=defaultSP;
        for(Register r:Register.values()){
            singles.put(r.name(),r.relativeOffset+offset);
            names.put(r.relativeOffset+offset,r.name());
        }
        for(RegisterBit r:RegisterBit.values()){
            bits.put(r.name(),new int[]{offset,r.bit});
        }
        SREG=Register.SREG.getOffset(this);
        SP=Register.SP.getOffset(this);
    }

    @Override
    public int[] getDataDefault() {
        int[] dataDefault=new int[getSize()];
        dataDefault[Register.SP.relativeOffset]=defaultSP;
        return dataDefault;
    }

    public enum Register implements IRegister<CPU_Registers> {
        //lets assume the core doesn't need more than 64bit addressing space...
        //RAMPD(0x08),RAMPX(0x09),RAMPY(0x0a),RAMPZ(0x0b),
        //EIND(0x0c),
        CCP(0x04),
        SP(0x0d),
        SREG(0x0f);

        public final int relativeOffset;

        Register(int relativeOffset){
            this.relativeOffset =relativeOffset;
        }

        @Override
        public int getOffset(CPU_Registers registers) {
            return relativeOffset +registers.getOffset();
        }
    }

    public enum RegisterBit implements IRegisterBit<CPU_Registers> {
        C,Z,N,V,S,H,T,I,H1,H2,H3,H4,H5,H6,H7,H8,U;
        public final int bit,mask;

        RegisterBit(){
            this.bit=ordinal();
            this.mask=1<<bit;
        }

        public int getOffset(CPU_Registers registers) {
            return registers.getOffset();
        }

        @Override
        public int getBitPosition() {
            return bit;
        }

        public int getBitMask() {
            return mask;
        }
    }

    @Override
    public TreeMap<Integer, IInterrupt> interrupts() {
        return null;
    }
}
