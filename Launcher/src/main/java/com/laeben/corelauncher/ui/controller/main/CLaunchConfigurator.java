package com.laeben.corelauncher.ui.controller.main;

import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.minecraft.loader.entity.RedownloadSettings;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CPopup;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class CLaunchConfigurator extends CPopup {
    private final VBox root;
    private final RedownloadSettings redownSettings;
    private final boolean disableResourceOptions;
    private Consumer<CLaunchConfigurator> onSubmitted;

    private VBox initializeRedownloadSettings(){
        final var box = new VBox();
        box.setSpacing(8);
        box.setAlignment(Pos.CENTER_LEFT);

        final var title = new Label(Translator.translate("launchConfigurator.redown"));
        box.getChildren().add(title);

        final var chkClient = new CheckBox(Translator.translate("launchConfigurator.redown.client"));
        chkClient.setSelected(true);
        chkClient.selectedProperty().addListener((observable, oldValue, newValue) -> redownSettings.client(newValue));
        box.getChildren().add(chkClient);

        final var chkLibraries = new CheckBox(Translator.translate("launchConfigurator.redown.libraries"));
        chkLibraries.setSelected(true);
        chkLibraries.selectedProperty().addListener((observable, oldValue, newValue) -> redownSettings.libraries(newValue));
        box.getChildren().add(chkLibraries);

        final var chkAssets = new CheckBox(Translator.translate("launchConfigurator.redown.assets"));
        chkAssets.setSelected(true);
        chkAssets.selectedProperty().addListener((observable, oldValue, newValue) -> redownSettings.assets(newValue));
        box.getChildren().add(chkAssets);

        final var chkResourcepacks = new CheckBox(Translator.translate("launchConfigurator.redown.resources"));
        chkResourcepacks.setSelected(true);
        chkResourcepacks.selectedProperty().addListener((observable, oldValue, newValue) -> redownSettings.resourcePacks(newValue));
        box.getChildren().add(chkResourcepacks);

        if (!disableResourceOptions){
            final var chkMods = new CheckBox(Translator.translate("launchConfigurator.redown.mods"));
            chkMods.setSelected(true);
            chkMods.selectedProperty().addListener((observable, oldValue, newValue) -> redownSettings.mods(newValue));
            box.getChildren().add(chkMods);

            final var chkShaders = new CheckBox(Translator.translate("launchConfigurator.redown.shaders"));
            chkShaders.setSelected(true);
            chkShaders.selectedProperty().addListener((observable, oldValue, newValue) -> redownSettings.shaders(newValue));
            box.getChildren().add(chkShaders);
        }

        return box;
    }

    public CLaunchConfigurator(boolean disableResourceOptions) {
        setDuration(200);
        this.disableResourceOptions = disableResourceOptions;
        this.redownSettings = RedownloadSettings.all();

        root = new VBox();
        root.setSpacing(16);
        root.setPadding(new Insets(16));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: -control-fill-secondary; -fx-background-radius: 16px");

        final var lblTitle = new Label(Translator.translate("launchConfigurator.title"));
        lblTitle.setStyle("-fx-font-size: 16pt");
        root.getChildren().add(lblTitle);

        root.getChildren().add(initializeRedownloadSettings());

        final var btnSubmit = new CButton();
        btnSubmit.setText(Translator.translate("option.ok"));
        btnSubmit.setOnMouseClicked(a -> {
            if (onSubmitted != null) onSubmitted.accept(this);

            hide();
        });
        root.getChildren().add(btnSubmit);

        setContent(root);
    }

    public void setOnSubmitted(Consumer<CLaunchConfigurator> onSubmitted) {
        this.onSubmitted = onSubmitted;
    }

    public RedownloadSettings getRedownSettings(){
        return redownSettings == null ? RedownloadSettings.none() : redownSettings;
    }

    private void syncLayout(Node owner, double xPad, double yPad){
        final var ownerBounds = owner.localToScreen(owner.getBoundsInLocal());

        setX(ownerBounds.getMinX() - xPad);
        setY(ownerBounds.getMinY() - getHeight() - yPad);
    }

    /**
     *
     * @param owner The owner Node of the popup. It must not be null
     *        and must be associated with a Window.
     * @param x the x position of the popup anchor relative to the owner
     * @param y the y position of the popup anchor relative to the owner
     */
    @Override
    public void show(Node owner, double x, double y) {
        root.heightProperty().addListener((observable, oldValue, newValue) -> {
            syncLayout(owner, x, y);
        });

        super.show(owner, 0, 0);

        syncLayout(owner, x, y);
    }
}
