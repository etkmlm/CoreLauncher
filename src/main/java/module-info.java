module com.cdev.corelauncher {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires org.apache.commons.compress;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens com.cdev.corelauncher to javafx.fxml;
    opens com.cdev.corelauncher.minecraft.entities to com.google.gson;
    opens com.cdev.corelauncher.data.entities to com.google.gson;
    opens com.cdev.corelauncher.data to com.google.gson;
    exports com.cdev.corelauncher;
    exports com.cdev.corelauncher.ui.controller;
    opens com.cdev.corelauncher.ui.controller to javafx.fxml;
    opens com.cdev.corelauncher.ui.controls to javafx.fxml;
    opens com.cdev.corelauncher.utils.entities to com.google.gson;
}