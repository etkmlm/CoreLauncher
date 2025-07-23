package com.laeben.corelauncher.util.java.entity;

public enum JavaSourceType {
    AZUL("Azul Zulu"),
    ADOPTIUM("Eclipse Adoptium");

    final String displayName;

    JavaSourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
