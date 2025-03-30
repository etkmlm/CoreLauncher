package com.laeben.corelauncher.minecraft.modding.entity;

import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Tool;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.entity.Version;
import com.laeben.corelauncher.minecraft.wrapper.entity.WrapperVersion;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResourcePreferences {
    private List<String> gameVersions;
    private List<LoaderType> loaders;

    private WeakReference<Profile> profile;

    public static ResourcePreferences empty() {
        return new ResourcePreferences();
    }

    public static ResourcePreferences fromProfile(Profile profile) {
        return new ResourcePreferences(profile);
    }

    private ResourcePreferences(){

    }

    private ResourcePreferences(Profile profile) {
        this.profile = new WeakReference<>(profile);
        gameVersions = List.of(profile.getVersionId());
        loaders = List.of(profile.getWrapper().getType());
    }

    public List<String> getGameVersions() {
        return gameVersions;
    }

    public ResourcePreferences includeGameVersions(List<String> gameVersions) {
        if (this.gameVersions == null)
            this.gameVersions = new ArrayList<>();

        this.gameVersions.addAll(gameVersions);
        return this;
    }

    public boolean hasGameVersions() {
        return gameVersions != null && !gameVersions.isEmpty();
    }

    public boolean hasLoaders() {
        return loaders != null && !loaders.isEmpty();
    }

    public ResourcePreferences includeLoaders(List<LoaderType> loaders) {
        if (this.loaders == null)
            this.loaders = new ArrayList<>();

        this.loaders.addAll(loaders);
        return this;
    }

    public List<LoaderType> getLoaders() {
        return loaders;
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

        if (prefs.hasLoaders())
            loader = prefs.getLoaders().get(0);
        else if (res.getLoaders() != null && res.getLoaders().length > 0)
            loader = res.getLoaders()[0];
        else
            return null;

        String name = res.getResourceType() == ResourceType.MODPACK ? Tool.beautifyString(res.getName(), Tool.ValidityDegree.HIGH) : StrUtil.toUpperFirst(loader.getIdentifier()) + " " + Translator.translate("profile");

        return createProfileFromPreferences(prefs, versionId, loader, name);
    }

    public static Profile createProfileFromPreferences(ResourcePreferences prefs, String versionId, LoaderType loader, String name) throws PerformException {
        var wr = Wrapper.getWrapper(loader.getIdentifier());
        String wrId = null;
        if (!loader.isNative()){
            var wrVers = ((Wrapper<WrapperVersion>)wr).getVersions(versionId);
            if (wrVers != null && !wrVers.isEmpty())
                wrId = wrVers.get(0).getWrapperVersion();

            if (wrId == null){
                throw new PerformException("Wrapper identifier for " + loader + " cannot be found.");
            }
        }

        String finalWrId = wrId;

        return Profiler.getProfiler().createAndSetProfile(Profiler.getProfiler().generateName(name), p ->
                p.setVersionId(versionId)
                        .setWrapper(wr)
                        .setWrapperVersion(finalWrId)
        );
    }
}
