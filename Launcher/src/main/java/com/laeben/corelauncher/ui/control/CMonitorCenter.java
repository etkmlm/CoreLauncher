package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.ui.entity.monitor.MonitorData;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CMonitorCenter {
    public static class MonitorCell extends ListCell<MonitorData>{
        private final HBox root;
        private final ProgressBar progress;

        private final CShapefulButton btnAction; // remove or stop

        private Boolean isActionStop = null;

        private MonitorData item;

        public MonitorCell() {
            root = new HBox();
            root.setAlignment(Pos.CENTER);
            root.setSpacing(16);
            root.setPadding(new Insets(16));
            root.setStyle("-fx-background-color: -control-fill;-fx-background-radius: 16px");

            setPrefWidth(0);

            root.getChildren().add(new Region()); // placeholder

            progress = new ProgressBar();
            HBox.setHgrow(progress, Priority.ALWAYS);
            progress.setMinWidth(128);
            progress.setMaxWidth(Double.MAX_VALUE);
            root.getChildren().add(progress);

            btnAction = new CShapefulButton();
            //btnAction.enableTransparentAnimation();
            btnAction.setOnMouseClicked(a -> {
                if (this.item != null)
                    this.item.stop();
            });
            root.getChildren().add(btnAction);
        }

        private void onProgressChanged(double progress){
            Boolean oldActionStop = isActionStop;
            isActionStop = progress < 1;

            if (oldActionStop == null || !oldActionStop.equals(isActionStop)){
                if (isActionStop){
                    btnAction.setText(null);
                    btnAction.setStyle("-fx-background-color: transparent; -shape-fill: white; -shape-width: 16px; -shape-height: 16px; -shape: -shape-stop;");
                }
                else{
                    btnAction.setText("—");
                    btnAction.setStyle("-fx-background-color: transparent; -fx-font-size: 16pt");
                }
            }

            this.progress.setProgress(progress);
        }

        @Override
        protected void updateItem(MonitorData item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty){
                setGraphic(null);
                return;
            }

            if (this.item != item){
                root.getChildren().set(0, item.serializeMetadata());
                if (this.item != null)
                    this.item.setOnPercentageChanged(null);
            }

            this.item = item;

            this.item.setOnPercentageChanged(this::onProgressChanged);
            //progress.setProgress(0);

            setGraphic(root);
        }
    }

    public static class MonitorPanel {
        private final ListView<MonitorData> lvMonitors;
        private final ObservableList<MonitorData> monitors;
        private final Map<Object, MonitorData> setMonitors;

        public MonitorPanel(){
            monitors = FXCollections.observableArrayList();
            lvMonitors = new ListView<>(monitors);
            lvMonitors.setPrefWidth(512);
            lvMonitors.setMinHeight(256);
            lvMonitors.setCellFactory(a -> new MonitorCell());
            lvMonitors.setStyle("-fx-background-color: transparent!important;");
            setMonitors = new HashMap<>();
        }

        public ListView<MonitorData> getMonitorList(){
            return lvMonitors;
        }

        public ObservableList<MonitorData> getMonitors() {
            return monitors;
        }

        public boolean isEmpty(){
            return monitors.isEmpty();
        }

        public void putMonitor(MonitorData data){
            monitors.add(data);
            setMonitors.put(data.getKey(), data);
        }

        /**
         * Updates the monitor by key.
         * @return null if there was no monitor found, true if process should continue, false if stop requested
         */
        public Boolean updateMonitor(Object key, double percentage){
            MonitorData data = setMonitors.get(key);
            if (data == null) return null;

            boolean shouldContinue = data.setPercentage(percentage);
            if (!shouldContinue)
                removeMonitor(key);

            return shouldContinue;
        }

        public void removeMonitor(Object key){
            var data = setMonitors.remove(key);
            if (data != null)
                monitors.remove(data);
        }
    }

    public record MonitorChangeEvent(boolean isEmpty, boolean wasAdded){

    }

    private final CPopup popup;

    private final VBox root;

    private final MonitorPanel downloadsPanel;
    private final MonitorPanel uploadsPanel;

    private Consumer<MonitorChangeEvent> onMonitorChange;

    public CMonitorCenter() {
        popup = new CPopup();
        popup.setDirection(true);
        popup.setDuration(200);
        popup.setAutoHide(true);

        root = new VBox();
        popup.setContent(root);

        //root.setMinHeight(256);
        root.setMaxHeight(512);
        root.setMinWidth(256);
        root.setPadding(new Insets(8));
        root.setStyle("-fx-background-color: -pane-fill!important;-fx-background-radius: 16px");

        downloadsPanel = new MonitorPanel();
        uploadsPanel = new MonitorPanel();

        VBox.setVgrow(downloadsPanel.getMonitorList(), Priority.ALWAYS);
        VBox.setVgrow(uploadsPanel.getMonitorList(), Priority.ALWAYS);

        downloadsPanel.getMonitors().addListener(this::onLocalMonitorChange);
        uploadsPanel.getMonitors().addListener(this::onLocalMonitorChange);

        root.getChildren().add(downloadsPanel.getMonitorList());
        root.getChildren().add(uploadsPanel.getMonitorList());
    }

    private void onLocalMonitorChange(ListChangeListener.Change<? extends MonitorData> c) {
        if (onMonitorChange != null)
            onMonitorChange.accept(new MonitorChangeEvent(downloadsPanel.isEmpty() && uploadsPanel.isEmpty(), c.next() && c.wasAdded()));
    }

    public void setOnMonitorChange(Consumer<MonitorChangeEvent> onMonitorChange){
        this.onMonitorChange = onMonitorChange;
    }

    public MonitorPanel getDownloadsPanel(){
        return downloadsPanel;
    }

    public MonitorPanel getUploadsPanel(){
        return uploadsPanel;
    }

    public void show(Node target){
        popup.showRelative(target, 4, 4);
    }

    public void hide(){
        popup.hide();
    }

    public boolean isShowing(){
        return popup.isShowing();
    }
}
