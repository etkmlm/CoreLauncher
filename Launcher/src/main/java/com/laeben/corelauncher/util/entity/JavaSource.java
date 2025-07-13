package com.laeben.corelauncher.util.entity;

import com.laeben.corelauncher.util.java.AdoptiumJavaManager;
import com.laeben.corelauncher.util.java.AzulJavaManager;
import com.laeben.corelauncher.util.java.JavaManager;

public enum JavaSource {
    AZUL("Azul Zulu", AzulJavaManager.class),
    ADOPTIUM("Eclipse Adoptium", AdoptiumJavaManager.class);

    final Class<? extends JavaManager> managerType;
    final String displayName;

    JavaSource(String displayName, Class<? extends JavaManager> managerType) {
        this.displayName = displayName;
        this.managerType = managerType;
    }

    public Class<? extends JavaManager> getManagerType() {
        return managerType;
    }

    public String getDisplayName() {
        return displayName;
    }
}
