package com.github.technus.avrClone.memory;

public interface IDataMemoryDefinition {
    int getOffset();
    int getSize();
    int[] getDataDefault();
}
