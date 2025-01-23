package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.util.DateUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CMsgBox;
import com.laeben.corelauncher.ui.control.CView;
import com.laeben.corelauncher.ui.dialog.DModSelector;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.ui.entity.animation.ReverseBorderColorAnimation;
import com.laeben.corelauncher.util.ImageUtil;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.Optional;

public class ResourceCell extends ListCell<ResourceCell.Link> {

    public record Link(Profile profile, ModResource resource){}

    private final Node gr;
    private Link link;
    private final ObjectProperty<CResource> exists;

    private final ReverseBorderColorAnimation animation;

    private DModSelector selector;

    public ResourceCell(){
        setGraphic(gr = UI.getUI().load(CoreLauncherFX.class.getResource("layout/cells/resource.fxml"), this));
        var rg = (Region)gr;
        rg.setBorder(new Border(new BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(2))));
        animation = new ReverseBorderColorAnimation();
        animation.setNode(rg);
        animation.setDuration(Duration.millis(2000));
        animation.setReverseDelay(100);

        exists = new SimpleObjectProperty<>();

        exists.addListener(a -> UI.runAsync(() -> {
            var val = exists.get();
            if (val == null){
                animation.setColor(Color.web("#7f32a8"));
            }
            else
                animation.setColor(Color.web("#ababab"));
            animation.playFromStart();
            btnInstall.setText(val == null ? "⭳" : "—");
        }));
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
        var profile = li.profile();

        lblName.setText(i.getName());
        lblAuthor.setText(i.getAuthors() != null && !i.getAuthors().isEmpty() ? i.getAuthors().get(0) : null);
        lblCategory.setText(Translator.translateFormat("mods.category", i.getCategories() != null ? String.join(",", i.getCategories()) : ""));
        txtDesc.setText((i.getDescription() != null ? i.getDescription() + "\n\n" : "") + DateUtil.toString(i.getCreationDate(), Configurator.getConfig().getLanguage()));
        if (i.getIcon() != null && !i.getIcon().isEmpty()){
            if (i.getIcon().startsWith("http"))
                icon.setImageAsync(ImageUtil.getNetworkImage(i.getIcon(), icon.getFitWidth(), icon.getFitHeight()));
            else
                icon.setImage(ImageUtil.getLocalImage(i.getIcon()));
        }

        if (i instanceof ResourceOpti){
            btnMore.setVisible(false);
            lblCategory.setVisible(false);
        }

        exists.set(profile.getResource(i.getId()));
        selector = null;

        setGraphic(gr);
    }

    public ResourceCell bindWidth(DoubleExpression p){
        prefWidthProperty().bind(p);
        return this;
    }

    @FXML
    public void initialize(){
        //btnInstall.enableTransparentAnimation();
        //btnMore.enableTransparentAnimation();
        icon.setCornerRadius(72, 72, 10);
        btnInstall.setOnMouseClicked(a -> new Thread(() -> {
            var profile = link.profile();
            var res = link.resource();

            if (exists.get() == null){
                try{
                    CResource mod;
                    if (res instanceof ResourceOpti ro){
                        mod = ro.getMod();

                        Profiler.getProfiler().setProfile(profile.getName(), x -> x.getMods().add((Mod) mod));
                    }
                    else{
                        var opt = ModSource.Options
                                .create(profile.getVersionId(), profile.getLoaderType(res.getResourceType()));
                        if (res.getResourceType() != ResourceType.MODPACK)
                            opt.dependencies(true);
                        var all = res.getSourceType().getSource()
                                .getCoreResource(res.getId(), opt);
                        if (all == null){
                            exists.set(null);
                            return;
                        }

                        mod = all.get(0);

                        Modder.getModder().includeAll(profile, all);
                    }

                    exists.set(mod);
                } catch (NoConnectionException | HttpException | StopException e) {
                    exists.set(null);
                }
            }
            else{
                Modder.getModder().remove(profile, exists.get());
                exists.set(null);
            }
        }).start());
        lblName.setOnMouseClicked(a -> showSelector());
        btnMore.setOnMouseClicked(a -> showSelector());
    }

    private void showSelector(){
        if (selector == null)
            selector = new DModSelector<>(link.resource, link.profile);
        Optional<CResource> r = Optional.empty();
        try {
            r = (Optional<CResource>)selector.select(exists.get());
        } catch (NoConnectionException | HttpException ignored) {

        }
        exists.set(r.orElse(null));
    }

    public ObjectProperty<CResource> existsProperty(){
        return exists;
    }

    public CResource getResource(){
        return exists.get();
    }
}
