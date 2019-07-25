package com.github.technus.avrClone.registerPackages;

public abstract class RegisterPackageSync extends RegisterPackage implements IRegisterPackageSync {
    protected RegisterPackageSync(int offset, int size) {
        super(offset, size);
    }
}

