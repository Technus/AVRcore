package com.github.technus.avrClone.registerPackages;

public interface I_Register<T extends I_RegisterPackage> {
    String name();
    int getOffset(T registerPackage);
}
