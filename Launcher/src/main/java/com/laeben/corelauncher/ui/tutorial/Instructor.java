package com.laeben.corelauncher.ui.tutorial;

import com.google.gson.JsonObject;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.LauncherConfig;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.ui.control.CMsgBox;
import com.laeben.corelauncher.ui.controller.page.EditProfilePage;
import com.laeben.corelauncher.ui.controller.page.MainPage;
import com.laeben.corelauncher.ui.controller.page.ProfilePage;
import com.laeben.corelauncher.ui.dialog.DProfileSelector;
import com.laeben.corelauncher.ui.tutorial.entity.Step;
import com.laeben.corelauncher.ui.tutorial.entity.Tutorial;
import com.laeben.corelauncher.util.GsonUtil;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseButton;

import java.util.ArrayList;
import java.util.List;

public class Instructor {
    private StepPopup popup;
    private Node baseNode;
    
    private Tutorial tutorial;

    private int currentStep;
    
    public Instructor(){

    }

    public void setPopup(StepPopup popup) {
        this.popup = popup;
    }

    public void setBaseNode(Node baseNode){
        this.baseNode = baseNode;
    }

    public void next(){
        var step = tutorial.getSteps().get(currentStep);
        Node n = null;
        if (step.getFocusSelector() != null && baseNode != null)
            n = baseNode.lookup(step.getFocusSelector());

        popup.set(step, a -> {
            if (a == StepPopup.ConfirmState.CANCEL || (a == StepPopup.ConfirmState.NEXT && step.isEndStep())){
                currentStep = 0;
                tutorial = null;
                return;
            }

            if (a == StepPopup.ConfirmState.NEXT)
                currentStep++;
            else if (currentStep > 0)
                currentStep--;
            next();
        });

        if (n != null){
            var l = n.localToScene(n.getBoundsInLocal());
            popup.move(l.getMaxX() + 20, l.getMinY() + 20);
        }
        else if (baseNode != null){
            var l = baseNode.localToScene(baseNode.getBoundsInLocal());
            popup.move(l.getMinX() + (l.getWidth() - popup.getWidth()) / 2, l.getMinY() + (l.getHeight() - popup.getHeight()) / 2);
        }
    }
    public boolean start(boolean overwrite){
        if (!overwrite){
            var lang = Configurator.getConfig().getLanguage();
            var opt = CMsgBox.msg(Alert.AlertType.CONFIRMATION, tutorial.getTitle(lang), tutorial.getDescription(lang))
                    .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                    .executeForResult();

            if (opt.isEmpty() || opt.get().result() != CMsgBox.ResultType.YES)
                return false;
        }

        currentStep = 0;
        next();

        return true;
    }

    public void load(Tutorial tutorial){
        this.tutorial = tutorial;
        currentStep = 0;
    }

    public List<Tutorial> getOnlineTutorials() throws NoConnectionException, HttpException {
        var xs = LauncherConfig.APPLICATION.getObject("tutorials", GsonUtil.DEFAULT_GSON, JsonObject.class);
        if (xs == null)
            return List.of();

        var lst = new ArrayList<Tutorial>();

        var lang = Configurator.getConfig().getLanguage();

        for (var x : xs.keySet()){
            var tuto = GsonUtil.DEFAULT_GSON.fromJson(xs.get(x), Tutorial.class);
            if (tuto.hasLocale(lang))
                lst.add(tuto);
        }

        return lst.stream().toList();
    }

    public static Tutorial generateGeneralTutorial(){
        return Tutorial
                .withInstructs(Translator.translate("welcome"), Translator.translate("tutorial.general.ask"))
                .withStep(Step.ofTranslated("tutorial.general.step1").toController(EditProfilePage.class).withMouseButton(MouseButton.SECONDARY).first())
                .withStep(Step.ofTranslated("tutorial.general.step2"))
                .withStep(Step.ofTranslated("tutorial.general.step3").withFocusSelector("#txtName"))
                .withStep(Step.ofTranslated("tutorial.general.step4").withFocusSelector("#cbGameVersion"))
                .withStep(Step.ofTranslated("tutorial.general.step5").withFocusSelector("#btnSave").withMouseButton(MouseButton.PRIMARY).toController(ProfilePage.class))
                .withStep(Step.ofTranslated("tutorial.general.step6"))
                .withStep(Step.ofTranslated("tutorial.general.step7").withFocusSelector(".tab-header-area .headers-region .text:first-child").withMouseButton(MouseButton.PRIMARY).toController(MainPage.class))
                .withStep(Step.ofTranslated("tutorial.general.step8").withMouseButton(MouseButton.SECONDARY).toDialog(DProfileSelector.class))
                .withStep(Step.ofTranslated("tutorial.general.step9").withMouseButton(MouseButton.PRIMARY).toController(MainPage.class))
                .withStep(Step.ofTranslated("tutorial.general.step10")/*.withFocusSelector(".cprofile:first-child")*/.withMouseButton(MouseButton.PRIMARY))
                .withStep(Step.ofTranslated("tutorial.general.step11").withFocusSelector("#btnPlay").withMouseButton(MouseButton.PRIMARY).last());
    }
}
