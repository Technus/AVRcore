package com.github.technus.avrClone.registerPackages;

public interface I_RegisterBit<T extends I_RegisterPackage> extends I_Register<T>{
    int getBitPosition();
    int getBitMask();
}
