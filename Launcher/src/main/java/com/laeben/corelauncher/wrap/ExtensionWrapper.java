package com.laeben.corelauncher.wrap;

import com.laeben.core.entity.Path;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.util.EventHandler;
import com.laeben.corelauncher.wrap.entity.Extension;
import com.laeben.corelauncher.wrap.exception.InvalidExtensionPropertiesException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ExtensionWrapper {
    public static final String EXTENSION_ADD = "extAdd";
    public static final String EXTENSION_REMOVE = "extRemove";

    private static ExtensionWrapper instance;

    private final Executor executor;
    private final List<Extension> loadedExtensions;

    private final EventHandler<KeyEvent> handler;

    public ExtensionWrapper(){
        executor = Executors.newSingleThreadExecutor();
        loadedExtensions = new ArrayList<>();

        handler = new EventHandler<>();

        instance = this;
    }

    public EventHandler<KeyEvent> getHandler(){
        return handler;
    }

    public static ExtensionWrapper getWrapper(){
        return instance;
    }

    public interface ExtensionInvokeRunnable{
        void run(Extension extension, URLClassLoader loader) throws ReflectiveOperationException;

        static ExtensionInvokeRunnable invokeForParameterlessMethod(String name, String clsName){
            return (ext, loader) -> {
                var cls = loader.loadClass(ext.getClassIdentifier(clsName));
                var instance = cls.getDeclaredConstructors()[0].newInstance();
                var mtd = cls.getMethod(name);
                mtd.invoke(instance);
            };
        }
    }

    public Path getPath(){
        return CoreLauncher.LAUNCHER_PATH.to("extensions");
    }

    public void reload(){
        var extFolder = getPath();
        loadedExtensions.forEach(x -> {
            if (x.getClassLoader() != null) {
                try {
                    x.getClassLoader().close();
                } catch (IOException ignored) {

                }
            }
        });
        loadedExtensions.clear();
        for (var extPath : extFolder.getFiles()){
            if (!extPath.getExtension().equals("jar"))
                continue;
            try{
                var loader = new URLClassLoader(new URL[]{extPath.toFile().toURI().toURL()});
                var ext = Extension.fromURLClassLoader(loader);
                if (ext != null){
                    var cls = loader.loadClass(ext.getMainClass());
                    var instance = cls.getDeclaredConstructors()[0].newInstance();
                    cls.getMethod("init", Extension.class).invoke(instance, ext);
                    loadedExtensions.add(ext);
                }
                else
                    throw new InvalidExtensionPropertiesException();

            } catch (Exception e) {
                Logger.getLogger().logHyph("Error while initializing " + extPath);
                Logger.getLogger().log(e);
            }
        }

        handler.execute(new KeyEvent(EventHandler.RELOAD));
    }

    public void removeExtension(Extension e){
        try {
            e.getClassLoader().close();
        } catch (IOException ignored) {

        }
        loadedExtensions.remove(e);
        if (e.getClassLoader() != null){
            var url = e.getClassLoader().getURLs()[0];
            try {
                Path.begin(new java.io.File(url.toURI()).toPath()).delete();
            } catch (URISyntaxException ex) {
                Logger.getLogger().log(ex);
            }
        }

        handler.execute((KeyEvent) new KeyEvent(EXTENSION_REMOVE).setSource(e));
    }

    public <T> void fireEvent(String eventName, T param){
        fireEvent(eventName, param, param.getClass());
    }

    public void fireEvent(String eventName, Object param, Class paramType){
        invokeAll((ext, loader) -> {
            if (ext.getListeners().isEmpty())
                return;
            for (var l : ext.getListeners()){
                l.getClass().getMethod(eventName, paramType).invoke(l, param);
            }
        });
    }

    public void fireEvent(String eventName){
        invokeAll((ext, loader) -> {
            if (ext.getListeners().isEmpty())
                return;
            for (var l : ext.getListeners()){
                l.getClass().getMethod(eventName).invoke(l);
            }
        });
    }

    private void invokeAll(ExtensionInvokeRunnable invoke){
        executor.execute(() -> {
            var extFolder = getPath();
            for (var ext : loadedExtensions){
                if (ext.getClassLoader() == null)
                    continue;
                try{
                    invoke.run(ext, ext.getClassLoader());
                } catch (Exception e) {
                    Logger.getLogger().logHyph("Error while loading " + ext.getName() + (ext.getClassLoader() != null ? " / " + ext.getClassLoader().getURLs()[0] : ""));
                    Logger.getLogger().log(e);
                }
            }
        });
    }

    public List<Extension> getLoadedExtensions(){
        return loadedExtensions;
    }
}
