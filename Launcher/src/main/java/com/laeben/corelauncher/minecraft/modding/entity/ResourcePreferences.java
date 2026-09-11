package com.laeben.corelauncher.minecraft.modding.entity;

import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Tool;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.minecraft.Loader;
import com.laeben.corelauncher.minecraft.entity.Version;
import com.laeben.corelauncher.minecraft.loader.Vanilla;
import com.laeben.corelauncher.minecraft.loader.entity.LoaderVersion;

import java.io.IOException;
import java.io.InvalidObjectException;
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
                    .includeGameVersion(Vanilla.getVanilla().getLatestRelease(null))
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
        loaderTypes = List.of(profile.getLoader().getType());
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

    public static Profile createProfileFromPreferences(ResourcePreferences prefs, ModResource res) throws PerformException, InvalidObjectException, StopException {
        String versionId = null;
        if (prefs.hasGameVersions())
            versionId = prefs.getGameVersions().stream().max(Version.VersionIdComparator.INSTANCE).orElse(null);

        if (versionId == null)
            versionId = Arrays.stream(res.getGameVersions()).max(Version.VersionIdComparator.INSTANCE).orElse(null);

        if (versionId == null)
            return null;

        List<LoaderType> loaders;

        if (prefs.hasLoaderTypes())
            loaders = prefs.getLoaderTypes();
        else{
            var availableLoaders = res.getLoaders(List.of(versionId));
            if (availableLoaders != null && availableLoaders.length > 0)
                loaders = List.of(availableLoaders);
            else
                return null;
        }

        String name = res.getResourceType() == ResourceType.MODPACK ? Tool.beautifyString(res.getName(), Tool.ValidityDegree.HIGH) : null;

        return createProfileFromPreferences(prefs, versionId, loaders, name);
    }

    public static Profile createProfileFromPreferences(ResourcePreferences prefs, String versionId, List<LoaderType> loaders, String name) throws PerformException, InvalidObjectException, StopException {
        if (loaders == null)
            loaders = List.of(LoaderType.VANILLA);

        Loader loader = null;
        String loaderVersion = null;

        for (var loaderType : loaders) {
            var wr = Loader.getLoader(loaderType.getIdentifier());

            if (loaderType != LoaderType.VANILLA){
                var versions = ((Loader<LoaderVersion>)wr).getVersions(versionId);
                if (versions != null && !versions.isEmpty())
                    loaderVersion = versions.get(0).getLoaderVersion();

                if (loaderVersion != null) {
                    loader = wr;
                    break;
                }
            }
        }

        if (loaderVersion == null){
            throw new PerformException("There are no versions found for loaders '" + String.join(",", loaders.stream().map(LoaderType::getIdentifier).toList()) + "'");
        }

        final String finalLoaderVersion = loaderVersion;
        final Loader finalLoader = loader;

        if (name == null)
            name = StrUtil.toUpperFirst(loader.getType().getIdentifier()) + " " + Translator.translate("profile");

        name = Profiler.getProfiler().generateName(name);

        try {
            return Profiler.getProfiler().createAndSetProfile(name, p ->
                    p.setVersionId(versionId)
                            .setLoader(finalLoader)
                            .setLoaderVersion(finalLoaderVersion)
            );
        } catch (IOException e) {
            throw new PerformException("Could not create profile " + name, e);
        }
    }
}
