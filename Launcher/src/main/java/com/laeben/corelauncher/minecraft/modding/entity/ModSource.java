package com.laeben.corelauncher.minecraft.modding.entity;

import com.google.gson.*;
import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.ui.controller.browser.Search;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public interface ModSource {

    class Options implements Cloneable{
        private final String versionId;
        private final LoaderType loader;
        private final Wrapper<?> wrapper;
        private boolean incDeps;
        private boolean useMeta;
        private boolean incSelf = true;
        private boolean applyMp;
        private Options(final String versionId, final LoaderType loader){
            this.versionId = versionId;
            this.loader = loader;
            this.wrapper = loader == null ? null : Wrapper.getWrapper(loader.getIdentifier());
        }

        private Options(final String versionId, final Wrapper wr){
            this.versionId = versionId;
            this.wrapper = wr;
            this.loader = wr.getType();
        }
        public static Options create(final String versionId, final LoaderType loader){
            return new Options(versionId, loader);
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

            try {
                return (Options) super.clone();
            } catch (CloneNotSupportedException e) {
                return this;
            }
        }

        public Options aggregateModpack(){
            applyMp = true;
            return this;
        }

        public Wrapper getWrapper(){
            return wrapper;
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
            return versionId;
        }

        public LoaderType getLoaderType() {
            return loader;
        }

        public boolean hasLoaderType(){
            return loader != null && loader.getIdentifier() != null;
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
