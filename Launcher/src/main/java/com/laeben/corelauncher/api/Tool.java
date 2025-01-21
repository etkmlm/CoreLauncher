package com.laeben.corelauncher.api;

import com.laeben.corelauncher.api.ui.Controller;
import com.laeben.corelauncher.ui.entity.LScene;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Tool {

    /**
     * Get the resource from the class {@link Tool}.
     * @param name name of the resource
     * @return absolute url
     */
    public static URL getResource(String name){
        return Tool.class.getResource(name);
    }

    /**
     * Get the resource from the given class or
     * a new URL directs to the JAR file.
     * @param name name of the resource
     * @param clz source class for the loader
     * @return absolute url
     */
    public static <T> URL getResource(String name, Class<T> clz){
        var first = clz.getResource(name);
        if (first != null)
            return first;

        if (clz.getProtectionDomain() == null)
            return null;

        if (!name.startsWith("/"))
            name = "/" + name;

        try {
            return new URL("jar:file:///" + clz.getProtectionDomain().getCodeSource().getLocation().getPath() + "!" + name);
        } catch (MalformedURLException e) {
            return null;
        }
    }


    /**
     * Find the controller from a featured node.
     * @param n source node
     * @return null if the controller could not be found.
     */
    public static Controller findNodeController(Node n){
        Parent p = n.getParent();

        while(p != null){
            if (p.getProperties().containsKey("controller")){
                return (Controller) p.getProperties().get("controller");
            }

            p = p.getParent();
        }

        var scene = n.getScene();
        if (scene == null)
            return null;

        return scene instanceof LScene<?> ls && ls.getController() instanceof Controller c ? c : null;
    }


    /**
     * gjerngjrengje
     */
    public enum ValidityDegree{
        /**
         * Only lowercase and uppercase English alphabetic characters are allowed.
         */
        HIGHEST,
        /**
         * It is same with HIGHEST, but slashes and colons are included.
         */
        HIGHEST_PATH,
        /**
         * Only numbers, lowercase and uppercase English alphabetic characters, spaces, underscores, dashes and dots are allowed.
         */
        HIGH,
        /**
         * It is same with HIGH, but slashes and colons are included.
         */
        HIGH_PATH,
        /**
         * It includes all ASCII characters.
         */
        NORMAL,
        /**
         * It includes all Unicode characters.
         */
        LOW,
        /**
         * Anyone can pass!
         */
        NONE
    }

    /**
     * Check the validity of the given string.
     * Blank strings are directly considered invalid.
     * @param s source string
     * @param degree validity degree
     * @return absolute url
     */
    public static boolean checkStringValidity(String s, ValidityDegree degree){
        if (s == null || s.isBlank())
            return false;

        if (degree == ValidityDegree.NONE)
            return true;

        if (degree == ValidityDegree.NORMAL){
            for (char c : s.toCharArray()){
                if (c > 127)
                    return false;
            }
            return true;
        }

        if (degree == ValidityDegree.LOW){
            var buffer = ByteBuffer.wrap(s.getBytes());
            try {
                StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .decode(buffer);
            } catch (CharacterCodingException ignored) {
                return false;
            }
            return true;
        }

        String regex = switch (degree){
            case HIGHEST -> "^[a-zA-Z]*$";
            case HIGHEST_PATH -> "^[a-zA-Z\\\\/:]*$";
            case HIGH -> "^[a-zA-Z0-9_\\-.\\s]*$";
            case HIGH_PATH -> "^[a-zA-Z0-9_\\-.\\s\\\\/:]*$";
            default -> ".*";
        };

        var pattern = Pattern.compile(regex);
        return pattern.matcher(s).matches();
    }
}
