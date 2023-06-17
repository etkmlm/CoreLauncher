package com.cdev.clfixer;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class Transformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)  {
        if (className.startsWith("com/mojang/authlib/yggdrasil/response/PrivilegesResponse$Privileges$Privilege")){
            try{
                ClassPool pool = ClassPool.getDefault();
                CtClass cls = pool.get(className.replace('/', '.'));
                CtField field = cls.getField("enabled");
                cls.removeField(field);
                cls.addField(CtField.make("private boolean enabled = true;", cls));

                classfileBuffer = cls.toBytecode();
            }
            catch (Exception ignored){

            }
        }
        return classfileBuffer;
    }
}
