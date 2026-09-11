package com.laeben.corelauncher.ui.controller.browser.cell;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.event.context.EventContext;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.util.DateUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.minecraft.modding.entity.resource.CResource;
import com.laeben.corelauncher.minecraft.modding.event.ModdingContext;
import com.laeben.corelauncher.ui.control.CProgressBorder;
import com.laeben.corelauncher.ui.control.CShapefulButton;
import com.laeben.corelauncher.ui.control.CView;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.browser.ResourceOpti;
import com.laeben.corelauncher.ui.dialog.DModSelector;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.ui.entity.animation.ReverseBorderColorAnimation;
import com.laeben.corelauncher.ui.util.DisplayUtil;
import com.laeben.corelauncher.util.ImageUtil;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

public class ResourceCell extends ListCell<ResourceCellItem> {
    public static final double CELL_HORIZONTAL_PADDING = 60;
    public static final PseudoClass INSTALLED =  PseudoClass.getPseudoClass("installed");
    public static final PseudoClass INSTALLING =  PseudoClass.getPseudoClass("installing");
    private static final String CATEGORY_BOX_STYLE_CLASS = "catbox";

    private final Node gr;
    private ResourceCellItem item;

    final SimpleBooleanProperty installing;
    final ObjectProperty<CResource> existingResource;
    Consumer<Profile> onNewProfileCreated;

    private final ReverseBorderColorAnimation animation;

    private DModSelector selector;

    public ResourceCell(){
        setGraphic(gr = UI.getUI().load(CoreLauncherFX.class.getResource("layout/cells/resource.fxml"), this));
        var rg = (Region)gr;
        rg.setBorder(new Border(new BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.SOLID, new CornerRadii(16), new BorderWidths(2))));
        animation = new ReverseBorderColorAnimation();
        animation.setNode(rg);
        animation.setDuration(Duration.millis(2000));
        animation.setReverseDelay(100);

        existingResource = new SimpleObjectProperty<>();
        installing = new SimpleBooleanProperty();

        existingResource.addListener(a -> UI.runAsync(this::onInstallAction));
        installing.addListener((a, o, n) -> UI.runAsync(this::onInstallAction));
    }

    /* Package-Private API */
    void onProgress(long current, long len, EventContext context){
        if (len == 0) len = 1;
        final long length = len;

        UI.runAsync(() -> {
            root.setProgress(current * 1.0 / length);
            if (current == 0){
                if (context instanceof ModdingContext ctx)
                    setStatus(Translator.translate(ctx.getLabel()));
                else
                    setStatus(null);
            }
            else if (current >= length)
                setStatus(null);
            else
                setStatus(DisplayUtil.parseDownloadProgress(current) + " / " + DisplayUtil.parseDownloadProgress(length));
        });
    }
    void clearProgress(){
        onProgress(0, 0, null);
    }
    void setStatus(String text){
        if (text == null && lblStatus.isVisible()){
            lblStatus.setVisible(false);
            lblStatus.setManaged(false);
            return;
        }
        else if (text != null && !lblStatus.isVisible()){
            lblStatus.setVisible(true);
            lblStatus.setManaged(true);
        }

        lblStatus.setText(text);
    }
    void playAnimation(boolean isPositive){
        UI.runAsync(() -> {
            animation.setColor(Color.web(isPositive ? "#ababab" : "#7f32a8"));

            animation.playFromStart();
        });
    }
    void resetAnimation(){
        UI.runAsync(() -> {
            animation.jumpTo(animation.getTotalDuration());
            animation.stop();
        });
    }
    /* API End */

    private void onInstallAction(){
        final boolean inled = existingResource.get() != null;
        final boolean insling = installing.get();

        //btnInstall.setText(inled ? "—" : (insling ? "X" : "+"));
        btnInstall.pseudoClassStateChanged(INSTALLED, inled);
        info.pseudoClassStateChanged(INSTALLED, inled);
        btnInstall.pseudoClassStateChanged(INSTALLING, insling);
        info.pseudoClassStateChanged(INSTALLING, insling);

        if (!insling) clearProgress();
    }


    @FXML
    private CView icon;
    @FXML
    private Label lblName;
    @FXML
    private Text lblAuthor;
    @FXML
    private Text lblStatus;
    @FXML
    private Label lblDate;
    @FXML
    private Text lblDesc;
    @FXML
    private CShapefulButton btnInstall;
    @FXML
    private FlowPane categories;
    @FXML
    private HBox header;
    @FXML
    private HBox badges;

    @FXML
    private VBox info;

    @FXML
    private HBox content;
    @FXML
    private CProgressBorder root;

    @Override
    protected void updateItem(ResourceCellItem li, boolean empty) {
        super.updateItem(li, empty);

        if (this.item != null)
            this.item.unbindCell(this);

        resetAnimation();

        if (empty || li == null){
            setGraphic(null);
            this.item = null;
            return;
        }

        this.item = li;
        this.item.bindCell(this);

        var i = li.getResource();
        var prefs = li.getPreferences();

        lblName.setText(i.getName());
        lblAuthor.setText(i.getAuthors() != null && i.getAuthors().length != 0 ? i.getAuthors()[0] : null);
        lblDate.setText(DateUtil.toString(i.getCreationDate(), Configurator.getConfig().getLanguage()));


        setPrefWidth(0);
        //prefWidthProperty().bind(getListView().widthProperty().subtract(CELL_HORIZONTAL_PADDING));
        //txtCategory.setText(Translator.translateFormat("mods.category", i.getCategories() != null ? String.join(",", i.getCategories()) : ""));
        lblDesc.setText(StrUtil.sub(i.getDescription(), 0, 200));

        if (i.getIcon() != null && !i.getIcon().isEmpty()){
            if (i.getIcon().startsWith("http"))
                icon.setImageAsync(ImageUtil.getNetworkImage(i.getIcon(), icon.getFitWidth(), icon.getFitHeight()), true);
            else
                icon.setImage(ImageUtil.getLocalImage(i.getIcon()));
        }
        else icon.setImage(ImageUtil.getDefaultImage(128));

        badges.getChildren().clear();

        var loaders = i.getLoaders(prefs.getGameVersions());
        if (loaders != null && prefs.getProfile() == null){
            for (var l : loaders){
                var img = ImageUtil.getLocalImage("loader/" + l.getIdentifier() + ".png");
                if (img == null || !l.isSupported())
                    continue;
                var view = new ImageView();
                view.setFitWidth(32);
                view.setFitHeight(32);
                view.setImage(img);
                badges.getChildren().add(view);
            }
        }

        categories.getChildren().clear();

        if (i.getCategories() != null){
            for(var c : i.getCategories()){
                var catBox = new Label();
                catBox.getStyleClass().add(CATEGORY_BOX_STYLE_CLASS);
                catBox.setText(c);
                categories.getChildren().add(catBox);
            }
        }

        if (existingResource.get() == null)
            li.existingResource().set(prefs.getProfile() == null ? null : prefs.getProfile().getResource(i.getId()));
        if (!installing.get()) clearProgress();

        /*if (existingResource.get() != null)
            playAnimation(true);*/

        selector = null;

        setGraphic(gr);
    }

    @Deprecated
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
        icon.setCornerRadius(72, 72, 16);
        btnInstall.setOnMouseClicked(a -> {
            if (a.getButton() == MouseButton.PRIMARY) item.performInstall();
        });
        header.setOnMouseClicked(a -> {
            if (a.getButton() == MouseButton.PRIMARY) showSelector();
        });
        setOnMouseClicked(a -> {
            if (a.getButton() == MouseButton.SECONDARY) showSelector();
        });
        //btnMore.setOnMouseClicked(a -> showSelector());
    }

    private void showSelector(){
        if (item.getResource() instanceof ResourceOpti)
            return;

        final var exists = item.existingResource().get();

        if (selector == null)
            selector = new DModSelector<>(item.getResource(), item.getPreferences(), getScene() == null ? null : getScene().getWindow());
        Optional<DModSelector.ModSelection> r = Optional.empty();
        try {
            r = (Optional<DModSelector.ModSelection>)selector.select(exists);
        } catch (IOException | HttpException e) {
            Logger.getLogger().log(e);
        } catch (StopException | NoConnectionException ignored) {

        }

        //selector = null;

        if (r.isPresent()){
            var g = r.get();

            if (exists == null && g.resource() != null)
                playAnimation(true);
            if (exists != null && g.resource() == null)
                playAnimation(false);

            item.existingResource().set(g.resource());

            if (g.profile() != null){
                Main.getMain().selectProfile(g.profile());
                if (onNewProfileCreated != null){
                    UI.runAsync(() -> onNewProfileCreated.accept(g.profile()));
                }
            }

        }

    }
}
