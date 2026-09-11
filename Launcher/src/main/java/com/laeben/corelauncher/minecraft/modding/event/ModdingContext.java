package com.laeben.corelauncher.minecraft.modding.event;

import com.laeben.core.event.context.EventContext;

public class ModdingContext extends EventContext {
    public static final ModdingContext APPLYING_MODPACK = new ModdingContext("event.context.modding.applyModpack");
    public static final ModdingContext RETRIEVING_MODPACK_DETAILS = new ModdingContext("event.context.modding.modpackDetails");
    public static final ModdingContext EXTRACTING_MODPACK = new ModdingContext("event.context.modding.extractModpack");

    public ModdingContext(String label) {
        super(label);
    }
}
