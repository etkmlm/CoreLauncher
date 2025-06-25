package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.api.util.DateUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.corelauncher.minecraft.modding.entity.resource.CResource;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CView;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.dialog.DModSelector;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.ui.entity.animation.ReverseBorderColorAnimation;
import com.laeben.corelauncher.util.ImageUtil;
import com.laeben.corelauncher.util.entity.LogType;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ResourceCell extends ListCell<ResourceCell.Link> {

    public record Link(ResourcePreferences preferences, ModResource resource){}

    private final Node gr;
    private Link link;
    private final ObjectProperty<CResource> exists;

    private final ReverseBorderColorAnimation animation;

    private DModSelector selector;

    private Consumer<Profile> onNewProfileCreated;

    public ResourceCell(){
        setGraphic(gr = UI.getUI().load(CoreLauncherFX.class.getResource("layout/cells/resource.fxml"), this));
        var rg = (Region)gr;
        rg.setBorder(new Border(new BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(2))));
        animation = new ReverseBorderColorAnimation();
        animation.setNode(rg);
        animation.setDuration(Duration.millis(2000));
        animation.setReverseDelay(100);

        exists = new SimpleObjectProperty<>();

        exists.addListener(a -> UI.runAsync(() -> btnInstall.setText(exists.get() == null ? "⭳" : "—")));
    }

    private void playAnimation(boolean isPositive){
        UI.runAsync(() -> {
            animation.setColor(Color.web(isPositive ? "#ababab" : "#7f32a8"));

            animation.playFromStart();
        });
    }

    @FXML
    public CView icon;
    @FXML
    public Label lblName;
    @FXML
    public Label lblAuthor;
    @FXML
    public Label lblCategory;
    @FXML
    public TextArea txtDesc;
    @FXML
    public CButton btnInstall;
    @FXML
    public CButton btnMore;
    @FXML
    public HBox badges;

    @Override
    protected void updateItem(Link li, boolean empty) {
        super.updateItem(li, empty);

        if (empty || li == null){
            setGraphic(null);
            link = null;
            return;
        }

        link = li;

        var i = li.resource();
        var prefs = li.preferences();

        lblName.setText(i.getName());
        lblAuthor.setText(i.getAuthors() != null && i.getAuthors().length != 0 ? i.getAuthors()[0] : null);
        lblCategory.setText(Translator.translateFormat("mods.category", i.getCategories() != null ? String.join(",", i.getCategories()) : ""));
        txtDesc.setText((i.getDescription() != null ? i.getDescription() + "\n\n" : "") + DateUtil.toString(i.getCreationDate(), Configurator.getConfig().getLanguage()));
        if (i.getIcon() != null && !i.getIcon().isEmpty()){
            if (i.getIcon().startsWith("http"))
                icon.setImageAsync(ImageUtil.getNetworkImage(i.getIcon(), icon.getFitWidth(), icon.getFitHeight()));
            else
                icon.setImage(ImageUtil.getLocalImage(i.getIcon()));
        }

        var loaders = i.getLoaders();
        if (loaders != null && prefs.getProfile() == null){
            badges.getChildren().clear();
            for (var l : loaders){
                var img = ImageUtil.getLocalImage("wrapper/" + l.getIdentifier() + ".png");
                if (img == null || !l.isSupported())
                    continue;
                var view = new ImageView();
                view.setFitWidth(32);
                view.setFitHeight(32);
                view.setImage(img);
                badges.getChildren().add(view);
            }
        }

        if (i instanceof ResourceOpti){
            btnMore.setVisible(false);
            lblCategory.setVisible(false);
        }

        exists.set(prefs.getProfile() == null ? null : prefs.getProfile().getResource(i.getId()));

        if (exists.get() != null)
            playAnimation(true);

        selector = null;

        setGraphic(gr);
    }

    public ResourceCell bindWidth(DoubleExpression p){
        prefWidthProperty().bind(p);
        return this;
    }

    public ResourceCell setOnNewProfileCreated(Consumer<Profile> onNewProfileCreated){
        this.onNewProfileCreated = onNewProfileCreated;
        return this;
    }

    @FXML
    public void initialize(){
        //btnInstall.enableTransparentAnimation();
        //btnMore.enableTransparentAnimation();
        icon.setCornerRadius(72, 72, 10);
        btnInstall.setOnMouseClicked(a -> new Thread(() -> {
            var prefs = link.preferences();
            var res = link.resource();

            var profile = prefs.getProfile() == null ? null : prefs.getProfile();

            boolean isNewProfile = false;

            boolean useShaderSetup = false;

            if (profile == null){
                if (res.getResourceType() == ResourceType.SHADER && !prefs.hasLoaderTypes()){
                    List<String> versions = null;
                    if (prefs.hasGameVersions())
                        versions = prefs.getGameVersions();

                    prefs = ResourcePreferences.getShaderPreferences();

                    if (versions != null){
                        prefs.clearAnd().includeGameVersions(versions);
                    }

                    useShaderSetup = true;
                }

                try {
                    profile = ResourcePreferences.createProfileFromPreferences(prefs, res);

                    if (profile == null)
                        return;
                } catch (PerformException e) {
                    Logger.getLogger().log(LogType.ERROR, e.getMessage());
                    return;
                }

                isNewProfile = true;

                Main.getMain().selectProfile(profile);
            }

            if (exists.get() == null){
                try{
                    CResource resourceToInstall;
                    if (res instanceof ResourceOpti ro){
                        resourceToInstall = ro.getMod();

                        Profiler.getProfiler().setProfile(profile.getName(), x -> x.getAllResources().add(resourceToInstall));
                    }
                    else{
                        var opt = ModSource.Options
                                .create(profile.getVersionId(), ModResource.getGlobalSafeLoaders(res.getResourceType(), profile.getWrapper().getType()));
                        if (res.getResourceType() != ResourceType.MODPACK)
                            opt.dependencies(true);

                        var all = res.getSourceType().getSource()
                                .getCoreResource(res.getId(), opt);
                        if (all == null){
                            exists.set(null);
                            return;
                        }

                        resourceToInstall = all.get(0);

                        if (useShaderSetup){
                            var rrrr = Modrinth.getModrinth().getPreferredShaderMod(opt);
                            if (rrrr != null)
                                all = Stream.concat(all.stream(), rrrr.stream()).distinct().toList();
                        }

                        if (Modder.getModder().include(profile, all) == 0)
                            return;
                    }

                    exists.set(resourceToInstall);
                    playAnimation(true);
                } catch (NoConnectionException | HttpException | StopException e) {
                    exists.set(null);
                }
            }
            else{
                Modder.getModder().remove(profile, exists.get());
                exists.set(null);
                playAnimation(false);
            }

            if (isNewProfile && onNewProfileCreated != null){
                var finalProfile = profile;
                UI.runAsync(() -> onNewProfileCreated.accept(finalProfile));
            }
        }).start());
        lblName.setOnMouseClicked(a -> showSelector());
        btnMore.setOnMouseClicked(a -> showSelector());
    }

    private void showSelector(){
        if (!btnMore.isVisible())
            return;

        if (selector == null)
            selector = new DModSelector<>(link.resource(), link.preferences());
        Optional<DModSelector.ModSelection> r = Optional.empty();
        try {
            r = (Optional<DModSelector.ModSelection>)selector.select(exists.get());
        } catch (NoConnectionException | HttpException ignored) {

        }

        //selector = null;

        if (r.isPresent()){
            var g = r.get();

            if (exists.get() == null && g.resource() != null)
                playAnimation(true);
            if (exists.get() != null && g.resource() == null)
                playAnimation(false);

            exists.set(g.resource());

            if (g.profile() != null){
                Main.getMain().selectProfile(g.profile());
                if (onNewProfileCreated != null){
                    UI.runAsync(() -> onNewProfileCreated.accept(g.profile()));
                }
            }

        }

    }

    public ObjectProperty<CResource> existsProperty(){
        return exists;
    }

    public CResource getResource(){
        return exists.get();
    }
}
