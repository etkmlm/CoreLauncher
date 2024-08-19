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

    opens com.laeben.corelauncher.ui.controller.page to javafx.fxml;
    opens com.laeben.corelauncher to javafx.fxml, com.laeben.core;
    opens com.laeben.corelauncher.minecraft.entity to com.google.gson;
    opens com.laeben.corelauncher.util.entity to com.google.gson;
    opens com.laeben.corelauncher.minecraft.wrapper.forge.entity to com.google.gson;
    opens com.laeben.corelauncher.minecraft.wrapper.neoforge.entity to com.google.gson;
    opens com.laeben.corelauncher.minecraft.wrapper.optifine.entity to com.google.gson;
    opens com.laeben.corelauncher.minecraft.wrapper.fabric.entity to com.google.gson;
    opens com.laeben.corelauncher.minecraft.wrapper.entity to com.google.gson;
    opens com.laeben.corelauncher.minecraft.modding.curseforge.entity to com.google.gson, com.laeben.core;
    opens com.laeben.corelauncher.minecraft.modding.modrinth.entity to com.google.gson, com.laeben.core;

    exports com.laeben.corelauncher;
    exports com.laeben.corelauncher.ui.controller;
    exports com.laeben.corelauncher.ui.controller.page;
    exports com.laeben.corelauncher.ui.controller.cell;
    opens com.laeben.corelauncher.ui.controller to javafx.fxml;
    opens com.laeben.corelauncher.ui.control to javafx.fxml;
    opens com.laeben.corelauncher.api.ui.entity to javafx.fxml;
    opens com.laeben.corelauncher.minecraft.modding.entity to com.google.gson;
    exports com.laeben.corelauncher.ui.controller.browser;
    opens com.laeben.corelauncher.ui.entity to javafx.fxml;
    opens com.laeben.corelauncher.ui.controller.browser to com.google.gson, com.laeben.core, javafx.fxml;
    opens com.laeben.corelauncher.ui.controller.cell to javafx.fxml,javafx.base;
    opens com.laeben.corelauncher.api.ui to javafx.fxml;
    opens com.laeben.corelauncher.api.entity to com.google.gson;
    opens com.laeben.corelauncher.ui.dialog to javafx.fxml;
    opens com.laeben.corelauncher.api to com.google.gson;


}