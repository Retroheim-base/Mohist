// https://github.com/Maxqia/ReflectionRemapper/blob/master/src/main/java/com/maxqia/ReflectionRemapper/ClassInheritanceProvider.java
package red.mohist.common.remap.remappers;

import java.util.Collection;
import java.util.HashSet;
import net.md_5.specialsource.provider.InheritanceProvider;
import static red.mohist.common.remap.remappers.RemapUtils.reverseMap;

public class MohistInheritanceProvider implements InheritanceProvider {

    @Override
    public Collection<String> getParents(String className) {
        className = RemapUtils.jarRemapper.map(className);

        try {
            Collection<String> parents = new HashSet<String>();
            Class<?> reference = Class.forName(className.replace('/', '.').replace('$', '.'), false, this.getClass().getClassLoader()/*RemappedMethods.loader*/);
            Class<?> extend = reference.getSuperclass();
            if (extend != null) {
                parents.add(reverseMap(extend));
            }

            for (Class<?> inter : reference.getInterfaces()) {
                if (inter != null) {
                    parents.add(reverseMap(inter));
                }
            }

            return parents;
        } catch (Exception e) {
            // Empty catch block
        }
        return null;
    }

}