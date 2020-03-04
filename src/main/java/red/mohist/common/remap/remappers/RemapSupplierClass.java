package red.mohist.common.remap.remappers;

import com.google.common.collect.Maps;
import java.util.Map;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 *  Remap the superclass
 *
 * @author pyz
 * @date 2019/7/2 11:24 PM
 */
public class RemapSupplierClass {

    public static Map<String, Class> map = Maps.newHashMap();

    /**
     * Get the Class that needs to be remapped from the map
     *
     * @param key superName
     * @return {@link Class<?>}
     */
    public static Class<?> get(String key) {
        return map.get(key);
    }

    public static void put(String key, Class value){
        map.put(key, value);
    }

    public static boolean isEmpty(String key) {
        return map.get(key) != null;
    }

    public static void remap(MethodInsnNode min) {
        if (isEmpty(min.owner) && "<init>".equals(min.name)) {
            Class<?> aclass = get(min.owner);
            min.owner = Type.getInternalName(aclass);
        }
    }

}
