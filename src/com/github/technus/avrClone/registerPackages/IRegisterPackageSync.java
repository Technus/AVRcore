package com.github.technus.avrClone.registerPackages;

import com.github.technus.avrClone.AvrCore;

public interface IRegisterPackageSync extends IRegisterPackage {
    void sync(AvrCore core);
}
