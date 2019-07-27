package com.github.technus.avrClone.registerPackages;

import com.github.technus.avrClone.AvrCore;

public interface IRegisterPackageSync<T> extends IRegisterPackage {
    /**
     * Sync before cpu cycle
     * @param core
     * @param objToSync
     */
    void preSync(AvrCore core,T objToSync);

    /**
     * Sync after cpu cycle
     * @param core
     * @param objToSync
     */
    void postSync(AvrCore core,T objToSync);
}
