package com.laeben.clpatcher;

import javassist.*;

import java.lang.instrument.ClassFileTransformer;
import java.net.JarURLConnection;
import java.security.ProtectionDomain;

public class Transformer implements ClassFileTransformer {

    private final String injectListenerObfuscated;
    private final String injectListener;
    private final String injectTitle;

    private final String clzClientPacketListener;
    private final String clzScreenTitle;

    private final String mtdTitleInit;
    private final String mtdHandle;

    private final boolean argsConfirmed;

    private final String jarPath;

    public Transformer(String args, String jarPath) {
        argsConfirmed = args != null;
        this.jarPath = jarPath;

        if (argsConfirmed){
            System.out.println("CLPatcher - Args Confirmed");
            String[] spl = args.split(",");

            clzClientPacketListener = spl[0];
            mtdHandle = spl[9];
            injectListenerObfuscated = String.format(
                    "%s mc = %1$s.%s();" +
                            "boolean single = mc.%s() != null;" +
                            "String info = single ? mc.%3$s().%s() : mc.%s().%s;" +
                            "Communicator.send(single? Communicator.InGameType.SINGLEPLAYER : Communicator.InGameType.MULTIPLAYER, info);",
                    spl[1], spl[2], spl[3], spl[4], spl[5], spl[6]);

            clzScreenTitle = spl[7];
            mtdTitleInit = spl[8];
        }
        else
            clzClientPacketListener = injectListenerObfuscated = clzScreenTitle = mtdTitleInit = mtdHandle = null;

        injectTitle = "Communicator.send(Communicator.InGameType.IDLING, (String)null);";
        injectListener =
                "net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();" +
                        "boolean single = mc.getSingleplayerServer() != null;" +
                        "String info = single ? mc.getSingleplayerServer().getMotd() : mc.getCurrentServer().ip;" +
                        "Communicator.send(single? Communicator.InGameType.SINGLEPLAYER : Communicator.InGameType.MULTIPLAYER, info);";
    }


    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)  {
        if (className == null)
            return classfileBuffer;

        try{
            //System.out.println(className);
            if (className.equals(clzClientPacketListener) || className.equals("net/minecraft/client/multiplayer/ClientPacketListener") || className.equals("net/minecraft/client/network/play/ClientPlayNetHandler")){
                System.out.println("CLPatcher - Patching ClientPacketListener");

                ClassPool pool = ClassPool.getDefault();
                pool.insertClassPath(jarPath);
                pool.importPackage("com.laeben.clpatcher");
                CtClass cls = pool.get(className.replace('/', '.'));

                boolean a = className.equals(clzClientPacketListener);
                CtMethod handle = cls.getDeclaredMethod(a ? mtdHandle : "handleLogin");
                handle.insertAfter(a ? injectListenerObfuscated : injectListener);

                cls.freeze();
                classfileBuffer = cls.toBytecode();
            }
            else if (className.equals(clzScreenTitle) || className.equals("net/minecraft/client/gui/screens/TitleScreen") || className.equals("net/minecraft/client/gui/screen/MainMenuScreen")){
                System.out.println("CLPatcher - Patching TitleScreen");

                ClassPool pool = ClassPool.getDefault();
                pool.insertClassPath(jarPath);
                pool.importPackage("com.laeben.clpatcher");
                CtClass cls = pool.get(className.replace('/', '.'));

                CtMethod mtd = cls.getDeclaredMethod(className.equals(clzScreenTitle) ? mtdTitleInit : "init");
                mtd.insertAfter(injectTitle);
                cls.freeze();
                classfileBuffer = cls.toBytecode();
            }
            else if (className.startsWith("com/mojang/authlib/yggdrasil/response/PrivilegesResponse$Privileges$Privilege")){
                System.out.println("CLPatcher - Patching Multiplayer Privileges");

                ClassPool pool = ClassPool.getDefault();
                CtClass cls = pool.get(className.replace('/', '.'));
                CtField field = cls.getField("enabled");
                cls.removeField(field);
                cls.addField(CtField.make("private boolean enabled = true;", cls));

                classfileBuffer = cls.toBytecode();
            }
        }
        catch (Exception e){
            System.out.println("CLPatcher - Error");
            e.printStackTrace(System.out);
        }

        return classfileBuffer;
    }
}
