package com.laeben.corelauncher.ui.util;

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

    private Spinner<Integer> spnMin;
    private Spinner<Integer> spnMax;
    private Slider slider;

    private boolean sliderValueChangedAuto = false;

    public RAMManager() {
        this(512, 32);
    }

    public RAMManager(int stepMb, int maxGb){
        fMin = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE);
        fMax = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE);

        fMin.valueProperty().addListener(a -> needsToSave());
        fMax.valueProperty().addListener(a -> needsToSave());

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

        spnMax.addEventFilter(ScrollEvent.ANY, scroller);
        spnMin.addEventFilter(ScrollEvent.ANY, scroller);
    }

    public void setMin(int val){
        fMin.setValue(val);
    }

    public void setMax(int val){
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
    }
}
