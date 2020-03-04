// https://github.com/Maxqia/ReflectionRemapper/blob/master/src/main/java/com/maxqia/ReflectionRemapper/RemappedMethods.java
package red.mohist.common.remap.remappers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class RemapMethods {

    // Class.forName
    public static Class<?> forName(String className) throws ClassNotFoundException {
        return forName(className, true, RemapUtils.getCallerClassLoder());
    }

    public static Class<?> forName(String className, boolean initialize, ClassLoader classLoader) throws ClassNotFoundException {
        if (className.startsWith("net.minecraft.server.v1_12_R1")) {
            className = RemapUtils.jarMapping.classes.getOrDefault(className.replace('.', '/'), className).replace('/', '.');
        }
        return Class.forName(className, initialize, classLoader);
    }

    // Get Fields
    public static Field getField(Class<?> inst, String name) throws NoSuchFieldException, SecurityException {
        if (RemapUtils.isClassNeedRemap(inst, true)) {
            name = RemapUtils.mapFieldName(inst, name);
        }
        return inst.getField(name);
    }

    public static Field getDeclaredField(Class<?> inst, String name) throws NoSuchFieldException, SecurityException {
        if (RemapUtils.isClassNeedRemap(inst, false)) {
            name = RemapUtils.jarRemapper.mapFieldName(RemapUtils.reverseMap(inst), name, null);
        }
        return inst.getDeclaredField(name);
    }

    // Get Methods
    public static Method getMethod(Class<?> inst, String name, Class<?>...parameterTypes) throws NoSuchMethodException, SecurityException {
        if (RemapUtils.isClassNeedRemap(inst, true)) {
            name = RemapUtils.mapMethod(inst, name, parameterTypes);
        }
        try {
            return inst.getMethod(name, parameterTypes);
        } catch (NoClassDefFoundError e) {
            throw new NoSuchMethodException(e.toString());
        }
        
    }

    public static Method getDeclaredMethod(Class<?> inst, String name, Class<?>...parameterTypes) throws NoSuchMethodException, SecurityException {
        if (RemapUtils.isClassNeedRemap(inst, true)) {
            name = RemapUtils.mapMethod(inst, name, parameterTypes);
        }
        try {
            return inst.getDeclaredMethod(name, parameterTypes);
        } catch (NoClassDefFoundError e) {
            throw new NoSuchMethodException(e.toString());
        }
    }

    // getName
    public static String getName(Field field) {
        if (!RemapUtils.isClassNeedRemap(field.getDeclaringClass(), false)) {
            return field.getName();
        }
        return RemapUtils.demapFieldName(field);
    }

    public static String getName(Method method) {
        if (!RemapUtils.isClassNeedRemap(method.getDeclaringClass(), true)) {
            return method.getName();
        }
        return RemapUtils.demapMethodName(method);
    }

    // getSimpleName
    public static String getSimpleName(Class<?> inst) {
        if (!RemapUtils.isClassNeedRemap(inst, false)) {
            return inst.getSimpleName();
        }
        String[] name = RemapUtils.reverseMapExternal(inst).split("\\.");
        return name[name.length - 1];
    }

    // getDeclaredMethods
    public static Method[] getDeclaredMethods(Class<?> inst) {
        try {
            return inst.getDeclaredMethods();
        } catch (NoClassDefFoundError e) {
            return new Method[]{};
        }
    }

    // ClassLoader.loadClass
    public static Class<?> loadClass(ClassLoader inst, String className) throws ClassNotFoundException {
        if (className.startsWith("net.minecraft.")) {
            className = RemapUtils.mapClass(className.replace('.', '/')).replace('/', '.');
        }
        return inst.loadClass(className);
    }
}
