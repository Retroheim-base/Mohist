package red.mohist.common.remap.remappers;

import com.google.common.collect.Maps;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.provider.ClassLoaderProvider;
import net.md_5.specialsource.provider.JointProvider;
import net.md_5.specialsource.transformer.MavenShade;
import org.objectweb.asm.Type;
import red.mohist.common.remap.MohistSecurityManager;

public class RemapUtils {

    private static MohistSecurityManager sm = new MohistSecurityManager();

    public static JarMapping jarMapping;
    public static MohistJarRemapper jarRemapper;

    public static JarMapping loadMapping() {
        JarMapping jarMapping = new JarMapping();
        try {
            jarMapping.packages.put("org/bukkit/craftbukkit/libs/it/unimi/dsi/fastutil/", "it/unimi/dsi/fastutil/");
            jarMapping.packages.put("org/bukkit/craftbukkit/libs/jline/", "jline/");
            jarMapping.packages.put("org/bukkit/craftbukkit/libs/joptsimple/", "joptsimple/");
            jarMapping.methods.put("org/bukkit/Bukkit/getOnlinePlayers ()[Lorg/bukkit/entity/Player;", "_INVALID_getOnlinePlayers");
            jarMapping.methods.put("org/bukkit/Server/getOnlinePlayers ()[Lorg/bukkit/entity/Player;", "_INVALID_getOnlinePlayers");
            jarMapping.methods.put("org/bukkit/craftbukkit/v1_12_R1/CraftServer/getOnlinePlayers ()[Lorg/bukkit/entity/Player;", "_INVALID_getOnlinePlayers");

            Map<String, String> relocations = Maps.newHashMap();
            relocations.put("net.minecraft.server", "net.minecraft.server.v1_12_R1");

            jarMapping.loadMappings(
                    getSrg(RemapUtils.class.getClassLoader()),
                    new MavenShade(relocations),
                    null, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jarMapping;
    }

    public static void startRemapping(){
        jarRemapper = getMohistJarRemapper(RemapUtils.class.getClassLoader(), false);
    }


    public static MohistJarRemapper getMohistJarRemapper(ClassLoader z, boolean classloader)
    {
        jarMapping = RemapUtils.loadMapping();
        final JointProvider provider = new JointProvider();
        provider.add(new MohistInheritanceProvider());
        if(classloader) {
            provider.add(new ClassLoaderProvider(z));
        }
        jarMapping.setFallbackInheritanceProvider(provider);
        final MohistJarRemapper mohistJarRemapper = new MohistJarRemapper(jarMapping);
        return mohistJarRemapper;
    }

    public static BufferedReader getSrg(ClassLoader z)
    {
        return new BufferedReader(new InputStreamReader(z.getResourceAsStream("mappings/nms.srg")));
    }

    public static boolean isNMSClass(String className){
        return className.startsWith("net.minecraft.server.v1_12_R1");
    }

    public static Class<?> getCallerClass(int skip) {
        return sm.getCallerClass(skip);
    }

    public static ClassLoader getCallerClassLoder() {
        return getCallerClass(3).getClassLoader(); // added one due to it being the caller of the caller;
    }

    // Classes
    public static String reverseMapExternal(Class<?> name) {
        return reverseMap(name).replace('$', '.').replace('/', '.');
    }

    public static String reverseMap(Class<?> name) {
        return reverseMap(Type.getInternalName(name));
    }

    public static String reverseMap(String check) {
        for (Map.Entry<String, String> entry : jarMapping.classes.entrySet()) {
            if (entry.getValue().equals(check))
                return entry.getKey();
        }
        return check;
    }

    // Methods
    public static String mapMethod(Class<?> inst, String name, Class<?>... parameterTypes) {
        String result = mapMethodInternal(inst, name, parameterTypes);
        if (result != null) {
            return result;
        }
        return name;
    }

    /**
     * Recursive method for finding a method from superclasses/interfaces
     */
    public static String mapMethodInternal(Class<?> inst, String name, Class<?>... parameterTypes) {
        String match = reverseMap(inst) + "/" + name;

        for (Map.Entry<String, String> entry : jarMapping.methods.entrySet()) {
            if (entry.getKey().startsWith(match)) {
                String[] str = entry.getKey().split("\\s+");
                int i = 0;
                boolean failed = false;
                for (Type type : Type.getArgumentTypes(str[1])) {
                    String typename = (type.getSort() == Type.ARRAY ? type.getInternalName() : type.getClassName());
                    if (!typename.equals(reverseMapExternal(parameterTypes[i]))) {
                        failed = true;
                        break;
                    }
                }

                if (!failed) {
                    return entry.getValue();
                }
            }
        }

        // Search superclass
        Class<?> superClass = inst.getSuperclass();
        if (superClass != null) {
            String superMethodName = mapMethodInternal(superClass, name, parameterTypes);
            if (superMethodName != null) {
                return superMethodName;
            }
        }

        // Search interfaces
        for (Class<?> interfaceClass : inst.getInterfaces()) {
            String superMethodName = mapMethodInternal(interfaceClass, name, parameterTypes);
            if (superMethodName != null) {
                return superMethodName;
            }
        }

        return null;
    }

    public static String mapFieldName(Class<?> inst, String name) {
        String key = reverseMap(inst) + "/" + name;
        String mapped = jarMapping.fields.get(key);
        if (mapped == null) {
            Class<?> superClass = inst.getSuperclass();
            if (superClass != null) {
                mapped = mapFieldName(superClass, name);
            }
        }
        return mapped != null ? mapped : name;
    }

    public static String mapClass(String className) {
        String remapped = JarRemapper.mapTypeName(className, jarMapping.packages, jarMapping.classes, className);
        if (remapped.equals(className) && className.startsWith("net/minecraft/server/") && !className.contains("v1_12_R1")) {
            String[] splitStr = className.split("/");
            return JarRemapper.mapTypeName("net/minecraft/server/v1_12_R1/" + splitStr[splitStr.length - 1], jarMapping.packages, jarMapping.classes, className);
        }
        return remapped;
    }

    public static String demapFieldName(Field field) {
        String name = field.getName();
        String match = reverseMap(field.getDeclaringClass()) + "/";

        for (Map.Entry<String, String> entry : jarMapping.fields.entrySet()) {
            if (entry.getKey().startsWith(match)) {
                String[] matched = entry.getKey().split("\\/");
                String rtr =  matched[matched.length - 1];
                return rtr;
            }
        }

        return name;
    }

    public static String demapMethodName(Method method) {
        String name = method.getName();
        String match = reverseMap(method.getDeclaringClass()) + "/";

        for (Map.Entry<String, String> entry : jarMapping.methods.entrySet()) {
            if (entry.getKey().startsWith(match)) {
                String[] matched = entry.getKey().split("\\s+")[0].split("\\/");
                String rtr =  matched[matched.length - 1];
                return rtr;
            }
        }

        return name;
    }

    public static boolean isClassNeedRemap(Class<?> clazz, boolean interfaces) {
        while (clazz != null && clazz.getClassLoader() != null) {
            if (clazz.getName().startsWith("net.minecraft.")) {
                return true;
            }
            if (interfaces) {
                for (Class<?> interfaceClass : clazz.getInterfaces()) {
                    if (isClassNeedRemap(interfaceClass, true)) {
                        return true;
                    }
                }
                clazz = clazz.getSuperclass();
            } else {
                return false;
            }
        }
        return false;
    }
}
