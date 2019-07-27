package com.github.technus.avrClone.registerPackages;

public abstract class RegisterPackageSync<T> extends RegisterPackage implements IRegisterPackageSync<T> {
    protected RegisterPackageSync(int offset, int size) {
        super(offset, size);
    }
}

