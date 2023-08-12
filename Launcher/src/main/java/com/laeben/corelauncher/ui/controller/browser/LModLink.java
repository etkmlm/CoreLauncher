package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.ResourceForge;

import javax.sound.sampled.Port;

public record LModLink<T>(Profile profile, T resource) {

}
