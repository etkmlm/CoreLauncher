package com.laeben.corelauncher.minecraft.modding.entity;

import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Tool;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.entity.Version;
import com.laeben.corelauncher.minecraft.wrapper.Vanilla;
import com.laeben.corelauncher.minecraft.wrapper.entity.WrapperVersion;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResourcePreferences {
    private static ResourcePreferences SHADER_PREFERENCES;

    /**
     * Returns optimal no-profile shader preferences.
     *
     * <p>It is Fabric with the latest game version. --- Sodium & Iris</p>
     */
    public static ResourcePreferences getShaderPreferences(){
        if (SHADER_PREFERENCES == null)
            SHADER_PREFERENCES = ResourcePreferences.empty()
                    .includeGameVersion(Vanilla.getVanilla().getLatestRelease())
                    .includeLoaderType(LoaderType.FABRIC);

        return SHADER_PREFERENCES;
    }

    private final List<String> gameVersions;
    private final List<LoaderType> loaderTypes;

    private boolean doClear = false;

    private WeakReference<Profile> profile;

    public static ResourcePreferences empty() {
        return new ResourcePreferences();
    }

    public static ResourcePreferences fromProfile(Profile profile) {
        return new ResourcePreferences(profile);
    }

    private ResourcePreferences(){
        gameVersions = new ArrayList<>();
        loaderTypes = new ArrayList<>();
    }

    private ResourcePreferences(Profile profile) {
        this.profile = new WeakReference<>(profile);
        gameVersions = List.of(profile.getVersionId());
        loaderTypes = List.of(profile.getWrapper().getType());
    }

    public List<String> getGameVersions() {
        return gameVersions;
    }

    public ResourcePreferences clearAnd(){
        doClear = true;
        return this;
    }

    public ResourcePreferences includeGameVersions(List<String> gameVersions) {
        if (doClear){
            this.gameVersions.clear();
            doClear = false;
        }
        this.gameVersions.addAll(gameVersions);
        return this;
    }

    public ResourcePreferences includeGameVersion(String gameVersion) {
        if (doClear){
            this.gameVersions.clear();
            doClear = false;
        }
        this.gameVersions.add(gameVersion);
        return this;
    }

    public boolean hasGameVersions() {
        return !gameVersions.isEmpty();
    }

    public boolean hasLoaderTypes() {
        return !loaderTypes.isEmpty();
    }

    public ResourcePreferences includeLoaderTypes(List<LoaderType> loaderTypes) {
        if (doClear){
            this.loaderTypes.clear();
            doClear = false;
        }
        this.loaderTypes.addAll(loaderTypes);
        return this;
    }

    public ResourcePreferences includeLoaderType(LoaderType loaderType) {
        if (doClear){
            this.loaderTypes.clear();
            doClear = false;
        }
        this.loaderTypes.add(loaderType);
        return this;
    }

    public List<LoaderType> getLoaderTypes() {
        return loaderTypes;
    }

    public Profile getProfile() {
        return profile == null ? null : profile.get();
    }

    public static Profile createProfileFromPreferences(ResourcePreferences prefs, ModResource res) throws PerformException {
        String versionId = null;
        if (prefs.hasGameVersions())
            versionId = prefs.getGameVersions().stream().max(Version.VersionIdComparator.INSTANCE).orElse(null);

        if (versionId == null)
            versionId = Arrays.stream(res.getGameVersions()).max(Version.VersionIdComparator.INSTANCE).orElse(null);

        if (versionId == null)
            return null;

        LoaderType loader;

        if (prefs.hasLoaderTypes())
            loader = prefs.getLoaderTypes().get(0);
        else if (res.getLoaders() != null && res.getLoaders().length > 0)
            loader = res.getLoaders()[0];
        else
            return null;

        String name = res.getResourceType() == ResourceType.MODPACK ? Tool.beautifyString(res.getName(), Tool.ValidityDegree.HIGH) : null;

        return createProfileFromPreferences(prefs, versionId, loader, name);
    }

    public static Profile createProfileFromPreferences(ResourcePreferences prefs, String versionId, LoaderType loader, String name) throws PerformException {
        if (loader == null)
            loader = LoaderType.VANILLA;

        var wr = Wrapper.getWrapper(loader.getIdentifier());
        String wrId = null;
        if (loader != LoaderType.VANILLA){
            var wrVers = ((Wrapper<WrapperVersion>)wr).getVersions(versionId);
            if (wrVers != null && !wrVers.isEmpty())
                wrId = wrVers.get(0).getWrapperVersion();

            if (wrId == null){
                throw new PerformException("Wrapper identifier for " + loader + " cannot be found.");
            }
        }

        String finalWrId = wrId;

        if (name == null)
            name = StrUtil.toUpperFirst(loader.getIdentifier()) + " " + Translator.translate("profile");

        return Profiler.getProfiler().createAndSetProfile(Profiler.getProfiler().generateName(name), p ->
                p.setVersionId(versionId)
                        .setWrapper(wr)
                        .setWrapperVersion(finalWrId)
        );
    }
}
