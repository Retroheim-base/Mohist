package red.mohist.common.remap.proxy;

import java.security.ProtectionDomain;
import net.md_5.specialsource.repo.RuntimeRepo;
import red.mohist.common.remap.remappers.RemapUtils;
import red.mohist.common.remap.Transformer;
import red.mohist.common.remap.remappers.MohistJarRemapper;

/**
 *
 * @author pyz
 * @date 2019/7/1 8:09 PM
 */
public class ProxyClassLoader extends ClassLoader {

    private MohistJarRemapper remapper;
    {
        this.remapper = RemapUtils.getMohistJarRemapper(this, true);
    }

    protected ProxyClassLoader() {
        super();
    }

    protected ProxyClassLoader(ClassLoader parent) {
        super(parent);
    }

    public final Class<?> defineClassRemap(byte[] b, int off, int len) throws ClassFormatError {
        return defineClassRemap(null, b, off, len, null);
    }

    public final Class<?> defineClassRemap(String name, byte[] b, int off, int len) throws ClassFormatError {
        return defineClassRemap(name, b, off, len, null);
    }

    public final Class<?> defineClassRemap(String name, java.nio.ByteBuffer b, ProtectionDomain protectionDomain) throws ClassFormatError {
        if (!b.isDirect() && b.hasArray()) {
            return remappedFindClass(name, b.array(), protectionDomain);
        }
        return defineClass(name, b, protectionDomain);
    }

    public final Class<?> defineClassRemap(String name, byte[] b, int off, int len, ProtectionDomain protectionDomain) throws ClassFormatError {
        if (off == 0) {
            return remappedFindClass(name, b, protectionDomain);
        }

        return defineClass(name, b, off, len, protectionDomain);
    }

    private Class<?> remappedFindClass(String name, byte[] stream, ProtectionDomain protectionDomain) throws ClassFormatError {
        Class<?> result = null;

        try {
            byte[] bytecode = remapper.remapClassFile(stream, RuntimeRepo.getInstance());
            bytecode = Transformer.process(bytecode);

            result = this.defineClass(name, bytecode, 0, bytecode.length, protectionDomain);
        } catch (Throwable t) {
            throw new ClassFormatError("Failed to remap class " + name);
        }

        return result;
    }
}
