package com.laeben.corelauncher.ui.controller.page;

import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.web.EmbeddedBrowser;
import com.laeben.corelauncher.web.WebComplex;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;

import java.util.function.Consumer;

public class WebPage extends HandlerController {
    public static final String KEY = "pgweb";

    private WebComplex complex;
    private boolean isComplexAttached = false;

    private Consumer<WebPage> onDisposed;

    public WebPage() {
        super(KEY);
    }

    public static WebPage open(String title){
        return Main.getMain().addTab("pages/web", title, true, WebPage.class);
    }

    public WebPage reload(){
        complex = EmbeddedBrowser.getInstance().getWebComplex();
        tryToAttachComplex();
        return this;
    }

    public WebPage reload(WebComplex complex){
        this.complex = complex;
        tryToAttachComplex();
        return this;
    }

    public WebPage navigate(String url){
        if (complex == null)
            reload();

        if (complex != null)
            complex.getView().getEngine().load(url);

        return this;
    }

    public WebPage onDisposed(Consumer<WebPage> onDisposed){
        this.onDisposed = onDisposed;
        return this;
    }

    private void tryToAttachComplex(){
        if (complex != null && !isComplexAttached){
            complex.attachPage(this);

            var view = complex.getView();
            if (root != null && !root.getChildren().contains(view)){
                AnchorPane.setBottomAnchor(view, 0.0);
                AnchorPane.setTopAnchor(view, 0.0);
                AnchorPane.setLeftAnchor(view, 0.0);
                AnchorPane.setRightAnchor(view, 0.0);
                root.getChildren().add(view);

                isComplexAttached = true;
            }
        }
    }

    @FXML
    private AnchorPane root;

    @Override
    public void preInit() {
        tryToAttachComplex();
    }

    @Override
    public void dispose(){
        if (onDisposed != null)
            onDisposed.accept(this);

        if (complex != null){
            complex.attachPage(null);
            complex.getView().getEngine().load("about:blank");
        }
    }
}
