package com.github.technus.avrClone;

import com.github.technus.avrClone.instructions.ExecutionEvent;
import com.github.technus.avrClone.instructions.I_Instruction;
import com.github.technus.avrClone.instructions.InstructionRegistry;
import com.github.technus.avrClone.interrupt.I_Interrupt;
import com.github.technus.avrClone.memory.*;
import com.github.technus.avrClone.memory.program.ProgramMemory;
import com.github.technus.avrClone.registerPackages.CPU_Registers;
import com.github.technus.avrClone.registerPackages.I_RegisterPackage;
import com.github.technus.avrClone.registerPackages.RegisterFilePairs;
import com.github.technus.avrClone.registerPackages.RegisterFileSingles;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class AvrCore {
    private volatile boolean valid=false;

    private InstructionRegistry instructionRegistry;
    private boolean immersiveOperands;

    private ProgramMemory programMemory;
    public volatile int programCounter = 0;

    private IoMemory ioMemory;
    private SramMemory sramMemory;
    private SystemMemory systemMemory;

    private EepromMemory eepromMemory;
    public RemovableMemory<EepromMemory> eepromBackup;

    public int[] dataMemory;
    public BitSet accessibleMemory;

    public int[] registerFile=new int[32];

    private CPU_Registers cpuRegisters;


    private HashMap<String, I_RegisterPackage> packages = new HashMap<>();
    public HashMap<String, I_RegisterPackage> packagesBackup = new HashMap<>();

    private int lastVector=-1;//reset is default value
    private I_Interrupt[] interrupts = new I_Interrupt[256];

    public AvrCore(InstructionRegistry instructionRegistry, boolean immersiveOperands) {
        setUsingImmersiveOperands(immersiveOperands);
        setInstructionRegistry(instructionRegistry);
    }

    public AvrCore simpleInit(){
        setDataMemory(0x100,  0x100);//512 mem total
        setCpuRegisters(0x30);
        return this;
    }

    public void reset(){
        clearDataMemoryContent();
        clearRegisterFileContent();
        programCounter=0;
    }

    public void clearRegisterFileContent(){
        registerFile=new int[registerFile.length];
    }

    //region validity
    public boolean checkValid() {
        return valid = instructionRegistry != null &&
                programMemory != null &&
                ioMemory != null &&
                sramMemory != null &&
                systemMemory != null &&
                dataMemory != null &&
                accessibleMemory != null &&
                cpuRegisters != null;
    }

    public void invalidate(){
        valid=false;
    }

    public void invalidateProgramMemory(){
        programMemory=null;
        programCounter=0;
        invalidate();
    }

    public void invalidateDataMemory(){
        eepromBackup=removeEepromMemory();

        ioMemory=null;
        sramMemory=null;
        systemMemory=null;
        dataMemory=null;
        accessibleMemory =null;
        invalidate();
    }
    //endregion

    //region cpu logic
    public void setUsingImmersiveOperands(boolean immersiveOperands) {
        if(this.immersiveOperands!=immersiveOperands){
            this.immersiveOperands=immersiveOperands;
            invalidateProgramMemory();
        }
    }

    public boolean isUsingImmersiveOperands() {
        return immersiveOperands;
    }

    private void setInstructionRegistry(InstructionRegistry instructionRegistry) {
        if(instructionRegistry==null){
            return;
        }
        if(this.instructionRegistry!=instructionRegistry) {
            this.instructionRegistry = instructionRegistry;
            invalidateProgramMemory();
        }
    }

    public InstructionRegistry getInstructionRegistry(){
        return instructionRegistry;
    }
    //endregion

    //region dataMemory
    public boolean setDataMemory(int ioSize,int ramSize) {
        if(ioSize<=0 || ramSize<=0){
            return false;
        }
        ioMemory=new IoMemory(ioSize);
        sramMemory=new SramMemory(ramSize);
        systemMemory=new SystemMemory(ramSize);

        dataMemory=systemMemory.data;

        accessibleMemory =new BitSet(dataMemory.length);

        accessibleMemory.set(sramMemory.getOffset(),sramMemory.getOffset()+sramMemory.getSize());

        packagesBackup=new HashMap<>(packages);
        packages.clear();
        clearInterruptsConfiguration();

        cpuRegisters=null;

        invalidate();
        return true;
    }

    public void clearDataMemoryContent(){
        if(!valid){
            return;
        }
        eepromBackup=removeEepromMemory();
        dataMemory=new int[dataMemory.length];
        setEepromMemory(eepromBackup);
        initDataMemory();
    }

    public void initDataMemory(){
        for(I_RegisterPackage pack:packages.values()){
            System.arraycopy(pack.getDataDefault(),0,dataMemory,pack.getOffset(),pack.getSize());
        }
    }

    public boolean isDataAddressValid(int address){
        return accessibleMemory.get(address);
    }

    public boolean isDataRangeValid(int from,int to){
        return accessibleMemory.nextClearBit(from)>to;
    }

    public boolean isDataBlockValid(int offset,int size){
        return accessibleMemory.nextClearBit(offset)>offset+size;
    }

    public boolean setEepromMemory(EepromMemory eeprom) {
        if(!valid){
            return false;
        }
        removeEepromMemory();
        if(eeprom!=null){
            eepromMemory=eeprom;
            accessibleMemory.set(eeprom.getOffset(),eeprom.getOffset()+eeprom.getSize());
            System.arraycopy(eepromMemory.getDataDefault(),eepromMemory.getOffset(),
                    dataMemory,eepromMemory.getOffset()
                    ,eepromMemory.getSize());
        }
        return true;
    }

    public boolean setEepromMemory(RemovableMemory<EepromMemory> eeprom) {
        if(!valid){
            return false;
        }
        removeEepromMemory();
        if(eeprom!=null){
            eepromMemory=eeprom.getDefinition();
            accessibleMemory.set(eeprom.getOffset(),eeprom.getOffset()+eeprom.getSize());
            System.arraycopy(eeprom.getData(),eepromMemory.getOffset(),
                    dataMemory,eepromMemory.getOffset()
                    ,eepromMemory.getSize());
        }
        return true;
    }

    public RemovableMemory<EepromMemory> removeEepromMemory(){
        RemovableMemory<EepromMemory> removableMemory = getEepromMemory();
        eepromMemory=null;
        return removableMemory;
    }

    public RemovableMemory<EepromMemory> getEepromMemory(){
        if(!valid || eepromMemory==null){
            return null;
        }
        int[] eeprom=new int[eepromMemory.getSize()];
        System.arraycopy(dataMemory,eepromMemory.getOffset(),eeprom,0,eeprom.length);
        accessibleMemory.clear(eepromMemory.getOffset(),eepromMemory.getOffset()+eepromMemory.getSize());
        return RemovableMemory.makeWithoutCloning(eepromMemory,eeprom);
    }
    //endregion

    //region cpu registers
    public boolean setCpuRegisters(int offset) {
        if(offset<0 || dataMemory==null){
            return false;
        }
        if(cpuRegisters!=null) {
            removeRegistersBindings(cpuRegisters);
        }

        CPU_Registers cpu=new CPU_Registers(offset,dataMemory.length-1);
        if(cpu.getOffset()+cpu.getSize()>ioMemory.getOffset()+ioMemory.getSize()){
            return false;
        }
        if(putRegistersBindings(cpu,"CPU")){
            cpuRegisters=cpu;
        }
        invalidate();
        return true;
    }

    public CPU_Registers getCpuRegisters() {
        return cpuRegisters;
    }
    //endregion

    //region programMemory
    public void setProgramMemoryString(String lines) throws Exception {
        this.programMemory = new ProgramMemory(this, lines.split("\\n"));
    }

    public void setProgramMemory(String... lines) throws Exception {
        this.programMemory = new ProgramMemory(this, lines);
    }

    public void setProgramMemory(int length) {
        this.programMemory = new ProgramMemory(this,length);
    }

    public ProgramMemory getProgramMemory() {
        return programMemory;
    }

    public int getProgramSize(){
        return programMemory.instructions.length;
    }

    public I_Instruction getInstruction(){
        return instructionRegistry.getInstruction(getInstructionID());
    }

    public I_Instruction getInstruction(int addr){
        return instructionRegistry.getInstruction(getInstructionID(addr));
    }

    public int getInstructionID(){
        return programMemory.instructions[programCounter];
    }

    public int getInstructionID(int addr){
        return programMemory.instructions[addr];
    }

    public int getOperand0(){
        return programMemory.param0[programCounter];
    }

    public int getOperand0(int addr){
        return programMemory.param0[addr];
    }

    public int getOperand1(){
        return programMemory.param1[programCounter];
    }

    public int getOperand1(int addr){
        return programMemory.param1[addr];
    }
    //endregion

    //region register package handlers
    public void removeAllPackages(){
        for (Map.Entry<String,I_RegisterPackage> entry:packages.entrySet()) {
            removeRegistersBindings(entry.getValue(),entry.getKey());
        }
        clearInterruptsConfiguration();//to be sure...
    }

    public String getPackageName(int i){
        if(accessibleMemory.get(i)){
            for(Map.Entry<String,I_RegisterPackage> entry:packages.entrySet()){
                I_RegisterPackage registerPackage=entry.getValue();
                if(i<registerPackage.getOffset()+registerPackage.getSize() && i>=registerPackage.getOffset()){
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public String getDataName(int i){
        if(accessibleMemory.get(i)){
            for(I_RegisterPackage registerPackage:packages.values()){
                String name=registerPackage.names().get(i);
                if(name!=null){
                    return name;
                }
            }
        }
        return null;
    }

    public boolean putRegistersBindings(I_RegisterPackage registerPackage) {
        return putRegistersBindings(registerPackage,registerPackage.getClass().getSimpleName()+registerPackage.hashCode());
    }

    public boolean putRegistersBindings(I_RegisterPackage registerPackage, String prefix, String postfix) {
        return putRegistersBindings(registerPackage,prefix + registerPackage.getClass().getSimpleName() + postfix);
    }

    public boolean putRegistersBindings(I_RegisterPackage registerPackage, String name) {
        if(accessibleMemory.get(registerPackage.getOffset(),registerPackage.getOffset()+registerPackage.getSize()).isEmpty()) {
            if(registerPackage.interrupts()!=null) {
                for (Integer key : registerPackage.interrupts().keySet()) {
                    int k = key;
                    if (interrupts[k] != null || !accessibleMemory.get(k)) {
                        return false;
                    }
                }
                lastVector=Math.max(registerPackage.interrupts().lastKey(),lastVector);
                for (Map.Entry<Integer,I_Interrupt> entry:registerPackage.interrupts().entrySet()) {
                    interrupts[entry.getKey()]=entry.getValue();
                }
            }
            packages.put(name,registerPackage);
            accessibleMemory.set(registerPackage.getOffset(), registerPackage.getOffset() + registerPackage.getSize());
            System.arraycopy(registerPackage.getDataDefault(),0,dataMemory,registerPackage.getOffset(),registerPackage.getSize());
        }else{
            return false;
        }
        return true;
    }

    public boolean removeRegistersBindings(I_RegisterPackage registerPackage) {
        return removeRegistersBindings(registerPackage,registerPackage.getClass().getSimpleName()+registerPackage.hashCode());
    }

    public boolean removeRegistersBindings(I_RegisterPackage registerPackage,String prefix,String postfix){
        String name=prefix + registerPackage.getClass().getSimpleName() + postfix;
        return removeRegistersBindings(registerPackage,name);
    }

    public boolean removeRegistersBindings(I_RegisterPackage registerPackage,String name){
        if(packages.containsKey(name)) {
            if(registerPackage.interrupts()!=null) {
                for (Integer key : registerPackage.interrupts().keySet()) {
                    interrupts[key] = null;
                }
                int lastNotNull = -1;
                for (int i = 0; i < lastVector; i++) {
                    if (interrupts[i] != null) {
                        lastNotNull = i;
                    }
                }
                lastVector=lastNotNull;
            }
            packages.remove(name);
            accessibleMemory.clear(registerPackage.getOffset(), registerPackage.getOffset() + registerPackage.getSize());
            return true;
        }
        return false;
    }
    //endregion

    //region register bits
    public void setRegisterBits(int addr,int bitMask,boolean value){
        registerFile[addr] = value ? registerFile[addr] | bitMask : registerFile[addr] & ~bitMask;
    }

    public void setRegisterBits(int addr,int bitMask){
        registerFile[addr]|=bitMask;
    }

    public void clearRegisterBits(int addr,int bitMask){
        registerFile[addr]&=~bitMask;
    }

    public boolean getRegisterBitsOr(int addr, int bitMask) {
        return (registerFile[addr] & bitMask) != 0;
    }

    public boolean getRegisterBitsNotOr(int addr, int bitMask) {
        return (registerFile[addr] & bitMask) == 0;
    }

    public boolean getRegisterBitsNotAnd(int addr, int bitMask) {
        return (registerFile[addr] & bitMask) != bitMask;
    }

    public boolean getRegisterBitsAnd(int addr, int bitMask) {
        return (registerFile[addr] & bitMask) == bitMask;
    }
    //endregion

    //region data bits
    public void setDataBits(int addr,int bitMask,boolean value){
        dataMemory[addr] = value ? dataMemory[addr] | bitMask : dataMemory[addr] & ~bitMask;
    }

    public void setDataBits(int addr,int bitMask){
        dataMemory[addr]|=bitMask;
    }

    public void clearDataBits(int addr,int bitMask){
        dataMemory[addr]&=~bitMask;
    }

    public boolean getDataBitsOr(int addr, int bitMask) {
        return (dataMemory[addr] & bitMask) != 0;
    }

    public boolean getDataBitsNotOr(int addr, int bitMask) {
        return (dataMemory[addr] & bitMask) == 0;
    }

    public boolean getDataBitsNotAnd(int addr, int bitMask) {
        return (dataMemory[addr] & bitMask) != bitMask;
    }

    public boolean getDataBitsAnd(int addr, int bitMask) {
        return (dataMemory[addr] & bitMask) == bitMask;
    }
    //endregion

    //region register value
    public void setRegisterValue(int addr,int value) {
        registerFile[addr]=value;
    }

    public int orRegisterValue(int addr, int bits) {
        return registerFile[addr]|=bits;
    }

    public int andRegisterValue(int addr, int bits) {
        return registerFile[addr]&=bits;
    }

    public int xorRegisterValue(int addr, int bits) {
        return registerFile[addr]^=bits;
    }

    public int notRegisterValue(int addr){
        return registerFile[addr]=~registerFile[addr];
    }

    public int negRegisterValue(int addr){
        return registerFile[addr]=-registerFile[addr];
    }

    public int getRegisterValue(int addr) {
        return registerFile[addr];
    }
    //endregion

    //region register pair
    public long getRegisterPairValue(int addr) {
        return (((long) registerFile[addr + 1]) << 32) | (registerFile[addr]);
    }

    public void setRegisterPairValue(int addr,long value) {
        registerFile[addr+1]=(int)(value>>32);
        registerFile[addr]=(int)value;
    }
    //endregion

    //region data
    public void setDataValue(int addr,int value) {
        if(accessibleMemory.get(addr)) {
            dataMemory[addr] = value;
        }
    }

    public int getDataValue(int addr) {
        if(accessibleMemory.get(addr)){
            return dataMemory[addr];
        }
        return 0;
    }

    public int xorDataValue(int addr, int bits) {
        if(accessibleMemory.get(addr)) {
            return dataMemory[addr] ^= bits;
        }
        return 0;
    }
    //endregion

    //region status bits
    public void setStatusBits(int bitMask,boolean desiredValue){
        dataMemory[cpuRegisters.SREG]=desiredValue? dataMemory[cpuRegisters.SREG]|bitMask: dataMemory[cpuRegisters.SREG]&~bitMask;
    }

    public void setStatusBits(int bitMask){
        dataMemory[cpuRegisters.SREG]|=bitMask;
    }

    public void clearStatusBits(int bitMask){
        dataMemory[cpuRegisters.SREG]&=~bitMask;
    }

    public boolean getStatusBitsOr(int bitMask){
        return (dataMemory[cpuRegisters.SREG] & bitMask) != 0;
    }

    public boolean getStatusBitsNotOr(int bitMask){
        return (dataMemory[cpuRegisters.SREG] & bitMask) == 0;
    }

    public boolean getStatusBitsNotAnd(int bitMask){
        return (dataMemory[cpuRegisters.SREG] & bitMask) != bitMask;
    }

    public boolean getStatusBitsAnd(int bitMask){
        return (dataMemory[cpuRegisters.SREG] & bitMask) == bitMask;
    }
    //endregion

    //region status value
    public int getStatusValue(){
        return dataMemory[cpuRegisters.SREG];
    }

    public void setStatusValue(int value){
        dataMemory[cpuRegisters.SREG]=value;
    }
    //endregion

    //region stack
    public int getStackPointer(){
        return dataMemory[cpuRegisters.SP];
    }

    public void setStackPointer(int stackPosition){
        dataMemory[cpuRegisters.SP]=stackPosition;
    }

    public void incStackPointer(){
        dataMemory[cpuRegisters.SP]++;
    }

    public void decStackPointer(){
        dataMemory[cpuRegisters.SP]--;
    }

    public void pushValue(int value){
        if(accessibleMemory.get(dataMemory[cpuRegisters.SP])){
            dataMemory[dataMemory[cpuRegisters.SP]--]=value;
        }
    }

    public int popValue(){
        if(accessibleMemory.get(++dataMemory[cpuRegisters.SP])) {
            return dataMemory[dataMemory[cpuRegisters.SP]];
        }
        return 0;
    }
    //endregion

    //region pointers
    public int getW(){
        return registerFile[RegisterFileSingles.Wl.offset];
    }

    public int getX(){
        return registerFile[RegisterFileSingles.Xl.offset];
    }

    public int getY(){
        return registerFile[RegisterFileSingles.Yl.offset];
    }

    public int getZ(){
        return registerFile[RegisterFileSingles.Zl.offset];
    }

    public int setW(int value){
        return registerFile[RegisterFileSingles.Wl.offset]=value;
    }

    public int setX(int value){
        return registerFile[RegisterFileSingles.Xl.offset]=value;
    }

    public int setY(int value){
        return registerFile[RegisterFileSingles.Yl.offset]=value;
    }

    public int setZ(int value){
        return registerFile[RegisterFileSingles.Zl.offset]=value;
    }

    public long getWpair(){
        return registerFile[RegisterFileSingles.Wl.offset]&((long) registerFile[RegisterFileSingles.Wh.offset]<<32);
    }

    public long getXpair(){
        return registerFile[RegisterFileSingles.Xl.offset]&((long) registerFile[RegisterFileSingles.Xh.offset]<<32);
    }

    public long getYpair(){
        return registerFile[RegisterFileSingles.Yl.offset]&((long) registerFile[RegisterFileSingles.Yh.offset]<<32);
    }

    public long getZpair(){
        return registerFile[RegisterFileSingles.Zl.offset]&((long) registerFile[RegisterFileSingles.Zh.offset]<<32);
    }

    public long setWpair(long value){
        setRegisterPairValue(RegisterFilePairs.W.offset,value);
        return getRegisterPairValue(RegisterFilePairs.W.offset);
    }

    public long setXpair(long value){
        setRegisterPairValue(RegisterFilePairs.X.offset,value);
        return getRegisterPairValue(RegisterFilePairs.X.offset);
    }

    public long setYpair(long value){
        setRegisterPairValue(RegisterFilePairs.Y.offset,value);
        return getRegisterPairValue(RegisterFilePairs.Y.offset);
    }

    public long setZpair(long value){
        setRegisterPairValue(RegisterFilePairs.Z.offset,value);
        return getRegisterPairValue(RegisterFilePairs.Z.offset);
    }
    //endregion

    //region interrupt handling
    private void clearInterruptsConfiguration(){
        lastVector=-1;
        interrupts=new I_Interrupt[interrupts.length];
    }

    public void handleInterrupts(){
        for(int i=0;i<=lastVector;i++) {
            if (interrupts[i]!=null && interrupts[i].tryInterrupt(this)) {//if cool and good
                pushValue(programCounter);
                programCounter = interrupts[i].getVector();
                dataMemory[cpuRegisters.SREG] &= CPU_Registers._I;
            }
        }
    }
    //endregion

    public ExecutionEvent cpuCycle() throws IndexOutOfBoundsException,NullPointerException{
        if((dataMemory[cpuRegisters.SREG] & CPU_Registers.I) != 0) {
            handleInterrupts();
        }
        return instructionRegistry.getInstruction(getInstructionID()).execute(this);
    }

    public ExecutionEvent cpuCycle(int atPC,I_Instruction instruction) throws IndexOutOfBoundsException,NullPointerException{
        if((dataMemory[cpuRegisters.SREG] & CPU_Registers.I) != 0) {
            handleInterrupts();
        }
        return instruction.execute(this);
    }
}
