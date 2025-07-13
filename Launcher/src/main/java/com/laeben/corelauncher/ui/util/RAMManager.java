package com.laeben.corelauncher.ui.util;

import com.laeben.corelauncher.ui.controller.Main;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.ScrollEvent;
import javafx.util.StringConverter;

public abstract class RAMManager {
    private final SpinnerValueFactory.IntegerSpinnerValueFactory fMin;
    private final SpinnerValueFactory.IntegerSpinnerValueFactory fMax;

    private final int maxGb;
    private final int step;

    private final EventHandler<ScrollEvent> scroller;
    private final ChangeListener<? super Integer> valueListener;

    private Spinner<Integer> spnMin;
    private Spinner<Integer> spnMax;
    private Slider slider;

    private boolean sliderValueChangedAuto = false;
    private boolean attendedAuto = false;

    public RAMManager() {
        this(512, 32);
    }

    public RAMManager(int stepMb, int maxGb){
        fMin = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE);
        fMax = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE);

        valueListener = (a, ol, ne) -> {
            if (attendedAuto){
                attendedAuto = false;
                return;
            }

            if (ol == null && ne == null)
                return;

            if ((ol != null && ne == null) || (ol == null) || !ol.equals(ne))
                needsToSave();
        };

        fMin.valueProperty().addListener(valueListener);
        fMax.valueProperty().addListener(valueListener);

        fMin.setAmountToStepBy(stepMb);
        fMax.setAmountToStepBy(stepMb);

        this.step = stepMb;
        this.maxGb = maxGb;

        scroller = ControlUtil::scroller;
    }

    public abstract void needsToSave();

    public void setControls(Spinner<Integer> spnMin, Spinner<Integer> spnMax, Slider slider) {
        this.spnMin = spnMin;
        this.spnMax = spnMax;
        this.slider = slider;
    }

    public void setup(){
        if (spnMin == null || spnMax == null || slider == null)
            return;

        spnMax.setValueFactory(fMax);
        spnMin.setValueFactory(fMin);

        slider.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double aDouble) {
                return (aDouble / 1024) + "GB";
            }

            @Override
            public Double fromString(String s) {
                String[] f = s.split(" ");
                return Double.parseDouble(f[0]) * 1024;
            }
        });
        slider.valueProperty().addListener(x -> {
            if (sliderValueChangedAuto){
                sliderValueChangedAuto = false;
                return;
            }
            var v = Math.floor(slider.getValue() / (double) step) * step;

            slider.valueProperty().set(v);

            fMax.setValue((int) slider.getValue());
        });

        /*spnMax.focusedProperty().addListener((a, b, c) -> {
            if (!c)
                spnMax.commitValue();
        });

        spnMin.focusedProperty().addListener((a, b, c) -> {
            if (!c)
                spnMin.commitValue();
        });*/

        spnMax.valueProperty().addListener((a, oldVal, newVal) -> {
            try{
                if (newVal == null)
                    fMax.setValue(oldVal);

                if (fMax.getValue() > maxGb * 1024)
                    return;

                sliderValueChangedAuto = true;
                slider.setValue(fMax.getValue());
            }
            catch (Exception e){
                fMax.setValue(oldVal);
            }
        });
        spnMin.valueProperty().addListener((a, oldVal, newVal) -> {
            try{
                if (newVal == null)
                    fMin.setValue(oldVal);
            }
            catch (Exception e){
                fMin.setValue(oldVal);
            }
        });

        spnMin.setOnMouseEntered(a -> Main.getMain().setPreventScrollFilter(true));
        spnMax.setOnMouseEntered(a -> Main.getMain().setPreventScrollFilter(true));
        spnMin.setOnMouseExited(a -> Main.getMain().setPreventScrollFilter(false));
        spnMax.setOnMouseExited(a -> Main.getMain().setPreventScrollFilter(false));
        spnMax.addEventFilter(ScrollEvent.SCROLL, scroller);
        spnMin.addEventFilter(ScrollEvent.SCROLL, scroller);
    }

    public void setMin(int val){
        //attendedAuto = false;
        fMin.setValue(val);
    }

    public void setDefaultMin(int val){
        attendedAuto = true;
        fMin.setValue(val);
    }

    public void setMax(int val){
        //attendedAuto = false;
        fMax.setValue(val);
    }

    public void setDefaultMax(int val){
        attendedAuto = true;
        fMax.setValue(val);
    }

    public int getMin(){
        return fMin.getValue();
    }

    public int getMax(){
        return fMax.getValue();
    }

    public void dispose(){
        spnMax.removeEventFilter(ScrollEvent.ANY, scroller);
        spnMin.removeEventFilter(ScrollEvent.ANY, scroller);
        spnMin.setOnMouseEntered(null);
        spnMin.setOnMouseExited(null);
        spnMax.setOnMouseEntered(null);
        spnMax.setOnMouseExited(null);

        fMin.valueProperty().removeListener(valueListener);
        fMax.valueProperty().removeListener(valueListener);
    }
}
