package com.github.technus.avrClone.registerPackages;

public interface IRegister<T extends IRegisterPackage> {
    String name();
    int getOffset(T registerPackage);
}
