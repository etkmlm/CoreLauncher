package com.laeben.corelauncher.ui.controller.worlds.cell;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.event.function.ProgressFunction;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.concurrency.ProgressTimer;
import com.laeben.corelauncher.api.concurrency.Tasker;
import com.laeben.corelauncher.api.concurrency.property.TaskRecordProperty;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.minecraft.modding.entity.resource.World;
import com.laeben.corelauncher.ui.control.CMsgBox;
import com.laeben.corelauncher.ui.controller.Main;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.util.Duration;

import java.io.IOException;
import java.time.Instant;
import java.util.function.Consumer;

public class WorldCellItem {
    private static final int INTERVAL = 50;

    private final Tasker tasker;
    private final ObjectProperty<World> world;
    private final TaskRecordProperty importRecord;
    private final TaskRecordProperty exportRecord;
    private final Consumer<String> deleteDuplicate;

    private ProgressTimer timer;
    private ProgressFunction onProgress;

    public WorldCellItem(World world, Tasker tasker, Consumer<String> deleteDuplicate) {
        this.world = new SimpleObjectProperty<>(world);
        this.tasker = tasker;
        this.importRecord = new TaskRecordProperty();
        this.exportRecord = new TaskRecordProperty();
        this.deleteDuplicate = deleteDuplicate;
    }

    public WorldCellItem(Tasker tasker, Consumer<String> deleteDuplicate) {
        this(null, tasker, deleteDuplicate);
    }

    public void setOnProgress(ProgressFunction onProgress) {
        this.onProgress = onProgress;
        if (timer != null) timer.setOnTick(onProgress);
    }

    public TaskRecordProperty importRecord(){
        return importRecord;
    }

    public TaskRecordProperty exportRecord(){
        return exportRecord;
    }

    public void handleWorldExport(Path path, Path saves) {
        if (exportRecord.isRunning() || getWorld() == null) return;

        final var worldPath = saves.to(getWorld().dirName);
        if (!worldPath.exists()){
            Main.getMain().announceLater(
                    Translator.translate("error.oops"),
                    Translator.translateFormat("world.backup.notFound", getWorld().levelName),
                    Announcement.AnnouncementType.ERROR,
                    Duration.seconds(2)
            );
            return;
        }

        exportRecord.set(tasker.await(() -> {
            try{
                if (timer == null) {
                    timer = new ProgressTimer(INTERVAL);
                    timer.start();
                }
                timer.setOnTick(onProgress);

                worldPath.zip(path, timer);
                Main.getMain().announceLater(
                        Translator.translate("world.title"),
                        Translator.translateFormat("world.backup", getWorld().levelName),
                        Announcement.AnnouncementType.INFO,
                        Duration.seconds(2)
                );
            } catch (StopException ignored) {
                path.delete();
            } catch (IOException e) {
                Logger.getLogger().log("World " + path + " cannot be exported.", e);
            } finally {
                if (timer != null) {
                    timer.stop();
                    timer = null;
                }
            }
        }, this, null));
    }

    public ProgressTimer getTimer() {
        return timer;
    }

    public void handleWorldImport(Path path, Path saves) {
        if (importRecord.isRunning() || getWorld() != null) return;

        importRecord.set(tasker.await(() -> {
            Path temp = null;
            try {
                if (timer == null) {
                    timer = new ProgressTimer(INTERVAL);
                    timer.start();
                }
                timer.setOnTick(onProgress);

                String dirName = path.getNameWithoutExtension();
                temp = Configurator.getConfig().getTemporaryFolder().to(dirName);

                path.extract(temp, null, timer);

                var files = temp.getFiles();
                if (files.size() == 1 && files.get(0).isDirectory()) {
                    dirName = files.get(0).getName();
                    files.get(0).move(temp, timer);
                }

                var world = World.fromGzip(null, temp.to("level.dat"));

                String name = world.levelName;
                final String finalDirName = dirName;

                if (name == null) {
                    temp.delete();
                    return;
                }

                boolean doDeleteDuplicate = false;

                if (saves.getFiles().stream().anyMatch(x -> x.getName().equals(finalDirName))) {
                    final var result = UI.runSync(() -> CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.sure"), Translator.translateFormat("world.ask.overwrite", finalDirName))
                            .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO, CMsgBox.ResultType.CANCEL)
                            .executeForResult()
                            .map(CMsgBox.Result::result));

                    if (result == null) {
                        temp.delete();
                        return;
                    }
                    final var confirmation = result.orElse(null);

                    if (confirmation == null || confirmation == CMsgBox.ResultType.CANCEL) {
                        temp.delete();
                        return;
                    } else if (confirmation.isPositive()) {
                        saves.to(finalDirName).delete();
                        doDeleteDuplicate = true;
                    } else {
                        String salt = String.valueOf(Instant.now().toEpochMilli());
                        dirName = finalDirName + salt;
                    }
                }

                final var p = saves.to(StrUtil.pure(dirName));
                temp.move(p, timer);

                if (doDeleteDuplicate && deleteDuplicate != null) deleteDuplicate.accept(finalDirName);

                final var imported = World.fromGzip(null, p.to("level.dat"));
                if (imported.levelName != null) this.world.set(imported);
            } catch (StopException ignored) {
                if (temp != null) temp.delete();
            } catch (IOException e) {
                Logger.getLogger().log("World " + path + " cannot be imported.", e);
            } finally {
                if (timer != null) {
                    timer.stop();
                    timer = null;
                }
            }
        }, this, null));
    }

    public World getWorld() {
        return world.get();
    }
}
