package com.example.me;

import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.ui.control.CTab;
import com.laeben.corelauncher.ui.dialog.CDialog;

public class TestListener {

    public void onUILoad(){
        System.out.println("UI Loaded");
        System.out.println(Translator.translate("test"));
        //UI.runAsync(() -> UI.getUI().get("main").getFrame().setContent(new TextField(){{setText("hahahahahahahaha");}}));
    }

    public void onTabLoad(CTab tab){
        /*var content = tab.getContent();

        System.out.println(content);

        Pane innerPane;

        if (content instanceof ScrollPane sp)
            innerPane = (Pane) sp.getContent();
        else if (content instanceof Pane p)
            innerPane = p;
        else
            innerPane = null;

        if (innerPane == null)
            return;

        var location = Tool.getResource("test.fxml", TestListener.class);
        UI.runAsync(() -> innerPane.getChildren().add(UI.getUI().load(location, new NMX())));*/


    }

    public void onWindowCreate(Object controller){
        /*if (!(controller instanceof Main main))
            return;

        var l = (CMenu)main.getRootNode().lookup("#cMenu");
        l.addItem(null, "TestMenu", a -> {
            var profile = Profiler.getProfiler().getProfile("fefe");
            Main.getMain().launch(profile, false, null);
        });*/
    }

    public void onDialogCreate(CDialog d){
        //d.getDialogPane().getScene().setRoot(new AnchorPane());
    }
}
