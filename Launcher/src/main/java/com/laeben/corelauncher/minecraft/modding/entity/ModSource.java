package com.laeben.corelauncher.minecraft.modding.entity;

import com.google.gson.*;
import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.entity.resource.CResource;
import com.laeben.corelauncher.minecraft.modding.entity.resource.Modpack;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.ui.controller.browser.Search;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public interface ModSource {

    class Options implements Cloneable{
        private final List<String> versionIds;
        private final List<LoaderType> loaders;

        private boolean incDeps;
        private boolean useMeta;
        private boolean incSelf = true;
        private boolean applyMp;
        private Options(final String versionId, final LoaderType loader){
            this.versionIds = versionId == null ? null : List.of(versionId);
            this.loaders = loader == null ? null : List.of(loader);
        }

        private Options(final String versionId, final Wrapper wr){
            this.versionIds = versionId == null ? null : List.of(versionId);
            this.loaders = wr == null ? null : List.of(wr.getType());
        }

        private Options(final List<String> versionIds, final List<LoaderType> loaders){
            this.versionIds = versionIds == null ? null : List.copyOf(versionIds);
            this.loaders = loaders == null ? null : List.copyOf(loaders);
        }

        public static Options create(final String versionId, final LoaderType loader){
            return new Options(versionId, loader);
        }

        public static Options create(final ResourcePreferences preferences) {
            return preferences.getProfile() != null ? Options.create(preferences.getProfile()) : new Options(preferences.getGameVersions(), preferences.getLoaderTypes());
        }

        public static Options create(final List<String> versionIds, final List<LoaderType> loaders) {
            return new Options(versionIds, loaders);
        }

        public static Options create(Profile p){
            return new Options(p.getVersionId(), p.getWrapper());
        }

        public Options dependencies(boolean include){
            incDeps = include;
            return this;
        }

        @Override
        public Options clone() {
            //return super.clone();

            return new Options(versionIds == null ? null : List.copyOf(versionIds), loaders == null ? null : List.copyOf(loaders));
        }

        public Options aggregateModpack(){
            applyMp = true;
            return this;
        }

        public Wrapper getWrapper(){
            if (loaders == null)
                return null;
            return loaders.isEmpty() ? null : Wrapper.getWrapper(loaders.get(0).getIdentifier());
        }

        public List<LoaderType> getLoaders(){
            return loaders;
        }

        public List<String> getVersionIds(){
            return versionIds;
        }

        public Options self(boolean include){
            incSelf = include;
            return this;
        }

        public Options meta(){
            useMeta = true;
            return this;
        }

        public String getVersionId() {
            return versionIds == null ? null : versionIds.get(0);
        }

        public LoaderType getLoaderType() {
            return loaders == null ? null : loaders.get(0);
        }

        public boolean hasGameVersion(){
            var vers = getVersionIds();
            return vers != null && !vers.isEmpty();
        }

        public boolean hasLoaderType(){
            var loaders = getLoaders();
            return loaders != null && !loaders.isEmpty();
        }

        public boolean getIncludeDependencies() {
            return incDeps;
        }

        public boolean useMeta() {
            return useMeta;
        }

        public boolean getIncludeSelf(){
            return incSelf;
        }

        public boolean getAggregateModpack(){
            return applyMp;
        }
    }

    final class TypeFactory implements JsonSerializer<Type>, JsonDeserializer<Type> {
        @Override
        public Type deserialize(JsonElement jsonElement, java.lang.reflect.Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (jsonElement.isJsonNull() || jsonElement.getAsString().isBlank())
                return null;

            Type t = null;
            try{
                t = Type.valueOf(jsonElement.getAsString().toUpperCase(Locale.US));
            }
            catch (IllegalArgumentException ignored){

            }
            return t;
        }

        @Override
        public JsonElement serialize(Type t, java.lang.reflect.Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(t.getId());
        }
    }
    enum Type{
        CURSEFORGE(CurseForge::getForge), MODRINTH(Modrinth::getModrinth);

        final Supplier<ModSource> sourceFact;

        Type(final Supplier<ModSource> source){
            this.sourceFact = source;
        }

        public String getId(){
            return name().toLowerCase(Locale.US);
        }

        public ModSource getSource(){
            return sourceFact.get();
        }
    }

    List<CResource> getCoreResources(List<Object> ids, Options opt) throws NoConnectionException, HttpException;
    List<CResource> getCoreResource(Object id, Options opt) throws NoConnectionException, HttpException;
    List<CResource> getAllCoreResources(Object id, Options opt) throws NoConnectionException, HttpException;
    List<CResource> getAllCoreResources(ModResource res, Options opt) throws NoConnectionException, HttpException;
    List<CResource> getCoreResource(ModResource res, Options opt) throws NoConnectionException, HttpException;
    List<CResource> getDependencies(List<CResource> crs, Options opt) throws NoConnectionException, HttpException;
    Type getType();
    <T extends Enum> Search<T> getSearch(Profile p);

    /**
     * Fills and extracts the modpack entity.
     *
     * @param mp modpack entity
     * @param path target directory
     * @param opt options entity which must contain a {@link Wrapper}
     */
    void applyModpack(Modpack mp, Path path, Options opt) throws NoConnectionException, HttpException, StopException;

    /**
     * Extracts the modpack entity.
     *
     * @param mp modpack entity
     * @param path target directory
     * @param overwriteManifest overwrite manifest file
     * @return the path of modpack manifest file
     */
    Path extractModpack(Modpack mp, Path path, boolean overwriteManifest) throws NoConnectionException, HttpException, StopException;
}
