package com.laeben.corelauncher.minecraft.modding.entity;

import com.google.gson.*;
import com.laeben.core.concurrency.CancellableToken;
import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.event.function.ProgressFunction;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.Loader;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.entity.resource.CResource;
import com.laeben.corelauncher.minecraft.modding.entity.resource.Modpack;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.ui.controller.browser.search.Search;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface ModSource {

    class Options {
        private final List<String> versionIds;
        private final List<LoaderType> loaders;

        // some mod authors interestingly make their library mods dependent to descendent mods
        // to solve it, need to store previously included mods
        private boolean useHandlingIds;
        private Set<Object> handledIds;

        private boolean incDeps;
        private boolean useMeta;
        private boolean incSelf = true;
        private boolean applyMp;
        private boolean allowOverwrite;
        private CancellableToken<?> cancellationToken;
        private ProgressFunction onProgress;
        private Options(final String versionId, final LoaderType loader){
            this.versionIds = versionId == null ? null : List.of(versionId);
            this.loaders = loader == null ? null : List.of(loader);
        }

        private Options(final String versionId, final Loader wr){
            this.versionIds = versionId == null ? null : List.of(versionId);
            this.loaders = wr == null ? null : List.of(wr.getType());
        }

        private Options(final List<String> versionIds, final List<LoaderType> loaders, Set<Object> handledIds){
            this.versionIds = versionIds == null ? null : List.copyOf(versionIds);
            this.loaders = loaders == null ? null : List.copyOf(loaders);
            this.handledIds = handledIds;
        }

        public static Options create(final String versionId, final LoaderType loader){
            return new Options(versionId, loader);
        }

        public static Options create(final ResourcePreferences preferences) {
            return preferences.getProfile() != null ? Options.create(preferences.getProfile()) : new Options(preferences.getGameVersions(), preferences.getLoaderTypes(), null);
        }

        public static Options create(final List<String> versionIds, final List<LoaderType> loaders) {
            return new Options(versionIds, loaders, null);
        }

        public static Options create(Profile p){
            return new Options(p.getVersionId(), p.getLoader());
        }

        public Options dependencies(boolean include){
            incDeps = include;
            return this;
        }

        public Options allowOverwrite(boolean value){
            allowOverwrite = value;
            return this;
        }

        public Options useCancellationToken(CancellableToken<?> token){
            this.cancellationToken = token;
            return this;
        }

        public Options useProgressLogging(ProgressFunction onProgress){
            this.onProgress = onProgress;
            return this;
        }

        public Options clearHandledIds(){
            this.handledIds = null;
            return this;
        }

        /**
         * Filters the id list by the handled ids and returns the filtered stream.
         * @param list original id list
         * @return filtered id stream
         * @param <T> id type
         */
        public <T> Stream<T> streamIdList(List<T> list){
            return handledIds == null ? list.stream() : list.stream().filter(id -> !handledIds.contains(id));
        }
        public void handleId(Object id){
            if (handledIds == null) handledIds = new HashSet<>();
            handledIds.add(id);
        }
        public boolean wasIdHandled(Object id){
            return id != null && handledIds != null && handledIds.contains(id);
        }

        /**
         * Creates a new instance with the same version ids and loaders.
         */
        public Options cloneFromPlatform() {
            //return super.clone();

            return new Options(versionIds == null ? null : List.copyOf(versionIds), loaders == null ? null : List.copyOf(loaders), handledIds)
                    .useCancellationToken(cancellationToken);
        }

        /**
         * Creates a new instance with the same properties but different platform preferences.
         */
        public Options cloneFromProperties(List<String> versionIds, List<LoaderType> loaders) {
            var opt = new Options(versionIds, loaders, handledIds).useCancellationToken(cancellationToken);
            opt.incSelf = incSelf;
            opt.incDeps = incDeps;
            opt.applyMp = applyMp;
            opt.allowOverwrite = allowOverwrite;
            opt.useMeta = useMeta;
            return opt;
        }

        public Options aggregateModpack(){
            applyMp = true;
            return this;
        }

        public Loader getLoader(){
            if (loaders == null)
                return null;
            return loaders.isEmpty() ? null : Loader.getLoader(loaders.get(0).getIdentifier());
        }

        public List<LoaderType> getLoaders(){
            return loaders;
        }

        public List<String> getVersionIds(){
            return versionIds;
        }

        public CancellableToken<?> getCancellationToken(){
            return cancellationToken;
        }

        public ProgressFunction getOnProgress(){
            return onProgress != null ? onProgress : ProgressFunction.EMPTY;
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
            return versionIds == null || versionIds.isEmpty() ? null : versionIds.get(0);
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

        public boolean doesAllowOverwrite(){
            return allowOverwrite;
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

    List<CResource> getCoreResources(List<Object> ids, Options opt) throws NoConnectionException, HttpException, StopException, IOException;
    List<CResource> getCoreResource(Object id, Options opt) throws NoConnectionException, HttpException, StopException, IOException;
    List<CResource> getAllCoreResources(Object id, Options opt) throws NoConnectionException, HttpException, StopException, IOException;
    List<CResource> getAllCoreResources(ModResource res, Options opt) throws NoConnectionException, HttpException, IOException, StopException;
    List<CResource> getCoreResource(ModResource res, Options opt) throws NoConnectionException, HttpException, StopException, IOException;
    List<CResource> getDependencies(List<CResource> crs, Options opt) throws NoConnectionException, HttpException, StopException, IOException;
    Type getType();
    <T extends Enum> Search<T> getSearch(Profile p);

    /**
     * Fills and extracts the modpack entity.
     *
     * @param mp modpack entity
     * @param path target directory
     * @param opt options entity which must contain a {@link Loader}
     */
    void applyModpack(Modpack mp, Path path, Options opt) throws NoConnectionException, HttpException, StopException, IOException;

    /**
     * Extracts the modpack entity.
     *
     * @param mp modpack entity
     * @param path target directory
     * @param overwriteManifest overwrite manifest file
     * @param token cancellation token
     * @return the path of modpack manifest file
     */
    Path extractModpack(Modpack mp, Path path, boolean overwriteManifest, ProgressFunction onProgress, CancellableToken<?> token) throws NoConnectionException, HttpException, StopException, IOException;
}
