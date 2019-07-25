package com.github.technus.avrClone.registerPackages;

import com.github.technus.avrClone.AvrCore;

public interface IRegisterPackageSync<T> extends IRegisterPackage {
    void sync(AvrCore core,int[] oldImage,T obj);
}
