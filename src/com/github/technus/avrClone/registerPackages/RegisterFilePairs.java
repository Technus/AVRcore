package com.github.technus.avrClone.registerPackages;

public enum RegisterFilePairs {
    W(24), X(26), Y(28), Z(30);

    public final int offset;

    RegisterFilePairs(int offset) {
        this.offset = offset;
    }
}