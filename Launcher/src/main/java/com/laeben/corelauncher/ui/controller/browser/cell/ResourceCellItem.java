package com.laeben.corelauncher.ui.controller.browser.cell;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.event.context.EventContext;
import com.laeben.core.event.function.ProgressFunction;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.concurrency.Tasker;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.entity.ModResource;
import com.laeben.corelauncher.minecraft.modding.entity.ModSource;
import com.laeben.corelauncher.minecraft.modding.entity.ResourcePreferences;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.minecraft.modding.entity.resource.CResource;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.browser.ResourceOpti;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ResourceCellItem {
    private final ResourcePreferences preferences;
    private final ModResource resource;

    private final ObjectProperty<CResource> existingResource;
    private final BooleanProperty installing;

    private ProgressFunction onProgress;
    private Consumer<Boolean> playAnimation;
    private Consumer<Profile> onNewProfileCreated;

    private Tasker.TaskRecord installationRecord;

    public ResourceCellItem(ResourcePreferences preferences, ModResource resource){
        this.preferences = preferences;
        this.resource = resource;

        this.existingResource = new SimpleObjectProperty<>();
        this.installing = new SimpleBooleanProperty();
    }

    public ModResource getResource(){
        return resource;
    }

    public ResourcePreferences getPreferences(){
        return preferences;
    }

    void bindCell(ResourceCell cell){
        cell.existingResource.bind(this.existingResource);
        cell.installing.bind(this.installing);
        this.onProgress = cell::onProgress;
        this.playAnimation = cell::playAnimation;
        this.onNewProfileCreated = cell.onNewProfileCreated; // no memory leak - function is derived from parent classes
    }
    void unbindCell(ResourceCell cell){
        cell.existingResource.unbind();
        cell.installing.unbind();
        this.onProgress = null;
        this.playAnimation = null;
    }

    public Tasker.TaskRecord getInstallationRecord(){
        return installationRecord;
    }

    BooleanProperty installing(){
        return installing;
    }

    ObjectProperty<CResource> existingResource(){
        return existingResource;
    }

    private void onProgress(long current, long total, EventContext context){
        if (this.onProgress != null)
            this.onProgress.onProgress(current, total, context);
    }

    private void playAnimation(boolean value){
        if (this.playAnimation != null)
            this.playAnimation.accept(value);
    }

    private CResource include(Profile profile, boolean isNewProfile, boolean useShaderSetup) throws NoConnectionException, HttpException, StopException, IOException {
        CResource resourceToInstall;
        if (resource instanceof ResourceOpti ro){
            resourceToInstall = ro.getMod();

            Profiler.getProfiler().setProfile(profile.getName(), x -> x.getAllResources().add(resourceToInstall));
        }
        else{
            var opt = ModSource.Options
                    .create(profile.getVersionId(), ModResource.getGlobalSafeLoaders(resource.getResourceType(), profile.getLoader().getType()))
                    .allowOverwrite(isNewProfile && !preferences.hasGameVersions())
                    .useProgressLogging(this::onProgress);
            if (resource.getResourceType() != ResourceType.MODPACK)
                opt.dependencies(true);

            var all = resource.getSourceType().getSource()
                    .getCoreResource(resource.getId(), opt);
            if (all == null || !installing.get()){
                //exists.set(null);
                // disabled, already returning null
                return null;
            }

            resourceToInstall = all.get(0);

            if (useShaderSetup){
                var rrrr = Modrinth.getModrinth().getPreferredShaderMod(opt);
                if (rrrr != null)
                    all = Stream.concat(all.stream(), rrrr.stream()).distinct().toList();
            }

            if (Modder.getModder().include(profile, all, isNewProfile ? Modder.IncludeMode.OVERWRITE_PROFILE : Modder.IncludeMode.DEFAULT, this::onProgress) == 0){
                return null;
            }
        }

        return resourceToInstall;
    }
    private void install(){
        var preferences = this.preferences;

        var profile = preferences.getProfile() == null ? null : preferences.getProfile();

        boolean isNewProfile = false;

        boolean useShaderSetup = false;

        if (profile == null){
            if (resource.getResourceType() == ResourceType.SHADER && !preferences.hasLoaderTypes()){
                List<String> versions = null;
                if (preferences.hasGameVersions())
                    versions = preferences.getGameVersions();

                preferences = ResourcePreferences.getShaderPreferences();

                if (versions != null){
                    preferences.clearAnd().includeGameVersions(versions);
                }

                useShaderSetup = true;
            }

            try {
                profile = ResourcePreferences.createProfileFromPreferences(preferences, resource);

                if (profile == null)
                    return;
            } catch (PerformException | IOException e) {
                Logger.getLogger().log(e);
                return;
            }
            catch (StopException ignored){
                return;
            }

            isNewProfile = true;
        }

        boolean installSuccessful = false;

        if (existingResource.get() == null){
            playAnimation(true);
            installing.setValue(true);

            try{
                final var included = include(profile, isNewProfile, useShaderSetup);
                existingResource.set(included);
                installSuccessful = included != null;
            } catch (NoConnectionException | IOException | HttpException | StopException e) {
                existingResource.set(null);
            }
            finally {
                Main.getMain().refreshStates();
                installing.setValue(false);
            }
        }
        else{
            Modder.getModder().remove(profile, existingResource.get());
            existingResource.set(null);
            playAnimation(false);
        }

        if (isNewProfile){
            final Profile finalProfile = profile;

            if (!installSuccessful) {
                Profiler.getProfiler().deleteProfile(finalProfile);
                return;
            }
            else Main.getMain().selectProfile(profile);

            if (onNewProfileCreated != null)
                UI.runAsync(() -> onNewProfileCreated.accept(finalProfile));
        }
    }

    public void performInstall(){
        if (installing.get()){
            if (installationRecord != null) installationRecord.stop();
            return;
        }

        installationRecord = Tasker.getDefault().await(this::install).onFinished(() -> installationRecord = null);
    }
}
