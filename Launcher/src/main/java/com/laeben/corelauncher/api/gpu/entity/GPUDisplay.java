package com.laeben.corelauncher.api.gpu.entity;

public record GPUDisplay(String displayName, GPUType type) {
    public static final GPUDisplay DEFAULT = new GPUDisplay(null, GPUType.DEFAULT);
}
