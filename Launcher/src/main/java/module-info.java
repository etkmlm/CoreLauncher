module com.laeben.corelauncher {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires org.apache.commons.compress;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires org.jsoup;
    requires java.desktop;
    requires com.laeben.core;
    requires jdk.dynalink;
    requires jdk.jfr;
    requires java.management;
    requires java.sql;
    requires org.apache.commons.lang3;

    opens com.laeben.corelauncher.ui.tutorial to javafx.fxml, com.google.gson;
    opens com.laeben.corelauncher.ui.tutorial.entity to com.google.gson;
    opens com.laeben.corelauncher to javafx.fxml, com.laeben.core;

    opens com.laeben.corelauncher.util;
    opens com.laeben.corelauncher.util.entity;

    opens com.laeben.corelauncher.minecraft;
    opens com.laeben.corelauncher.minecraft.entity;
    opens com.laeben.corelauncher.minecraft.loader;
    opens com.laeben.corelauncher.minecraft.loader.entity;
    opens com.laeben.corelauncher.minecraft.loader.neoforge;
    opens com.laeben.corelauncher.minecraft.loader.neoforge.entity;
    opens com.laeben.corelauncher.minecraft.loader.fabric;
    opens com.laeben.corelauncher.minecraft.loader.fabric.entity;
    opens com.laeben.corelauncher.minecraft.loader.forge;
    opens com.laeben.corelauncher.minecraft.loader.forge.entity;
    opens com.laeben.corelauncher.minecraft.loader.forge.installer;
    opens com.laeben.corelauncher.minecraft.loader.optifine;
    opens com.laeben.corelauncher.minecraft.loader.optifine.entity;
    opens com.laeben.corelauncher.minecraft.loader.quilt;

    opens com.laeben.corelauncher.minecraft.util;
    opens com.laeben.corelauncher.minecraft.modding;
    opens com.laeben.corelauncher.minecraft.modding.entity;
    opens com.laeben.corelauncher.minecraft.modding.curseforge;
    opens com.laeben.corelauncher.minecraft.modding.curseforge.entity;
    opens com.laeben.corelauncher.minecraft.modding.modrinth;
    opens com.laeben.corelauncher.minecraft.modding.modrinth.entity;
    opens com.laeben.corelauncher.minecraft.mapping;
    opens com.laeben.corelauncher.minecraft.mapping.entity;



    exports com.laeben.corelauncher;
    exports com.laeben.corelauncher.ui.controller;
    exports com.laeben.corelauncher.ui.controller.page;
    exports com.laeben.corelauncher.ui.controller.cell;
    exports com.laeben.corelauncher.ui.controller.browser;

    opens com.laeben.corelauncher.api;
    opens com.laeben.corelauncher.api.util;
    opens com.laeben.corelauncher.api.util.entity;
    opens com.laeben.corelauncher.api.ui;
    opens com.laeben.corelauncher.api.ui.entity;
    opens com.laeben.corelauncher.api.entity;
    opens com.laeben.corelauncher.api.exception;
    opens com.laeben.corelauncher.api.nbt;
    opens com.laeben.corelauncher.api.nbt.util;
    opens com.laeben.corelauncher.api.nbt.entity;
    opens com.laeben.corelauncher.wrap;
    opens com.laeben.corelauncher.wrap.exception;
    opens com.laeben.corelauncher.wrap.entity;
    opens com.laeben.corelauncher.ui.controller;
    opens com.laeben.corelauncher.ui.control;
    opens com.laeben.corelauncher.ui.entity;
    opens com.laeben.corelauncher.ui.controller.page;
    opens com.laeben.corelauncher.ui.controller.browser;
    opens com.laeben.corelauncher.ui.controller.cell;
    opens com.laeben.corelauncher.ui.dialog;
    opens com.laeben.corelauncher.ui.entity.animation;

    opens com.laeben.corelauncher.discord;
    opens com.laeben.corelauncher.discord.channel;
    opens com.laeben.corelauncher.discord.entity;
    opens com.laeben.corelauncher.discord.entity.response;
    opens com.laeben.corelauncher.ui.entity.filter;
    opens com.laeben.corelauncher.minecraft.modding.entity.resource;
    opens com.laeben.corelauncher.util.java;
    opens com.laeben.corelauncher.util.java.entity;
}