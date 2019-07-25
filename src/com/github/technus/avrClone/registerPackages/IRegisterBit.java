package com.github.technus.avrClone.registerPackages;

public interface IRegisterBit<T extends IRegisterPackage> extends IRegister<T> {
    int getBitPosition();
    int getBitMask();
}
