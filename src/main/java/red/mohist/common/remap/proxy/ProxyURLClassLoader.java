package red.mohist.common.remap.proxy;

import com.google.common.collect.Maps;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Map;
import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.provider.ClassLoaderProvider;
import net.md_5.specialsource.provider.JointProvider;
import net.md_5.specialsource.repo.RuntimeRepo;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.server.MinecraftServer;
import red.mohist.common.remap.remappers.RemapUtils;
import red.mohist.common.remap.Transformer;
import red.mohist.common.remap.remappers.MohistInheritanceProvider;
import red.mohist.common.remap.remappers.MohistJarRemapper;

public class ProxyURLClassLoader extends URLClassLoader
{

    private JarMapping jarMapping;
    private MohistJarRemapper remapper;
    private final Map<String, Class<?>> classes = Maps.newHashMap();
    private LaunchClassLoader launchClassLoader;

    {
        this.launchClassLoader = (LaunchClassLoader) MinecraftServer.getServerInst().getClass().getClassLoader();
        this.jarMapping = RemapUtils.loadMapping();
        final JointProvider provider = new JointProvider();
        provider.add(new MohistInheritanceProvider());
        provider.add(new ClassLoaderProvider(this));
        this.jarMapping.setFallbackInheritanceProvider(provider);
        this.remapper = new MohistJarRemapper(this.jarMapping);
    }

    public ProxyURLClassLoader(final URL[] urls, final ClassLoader parent) {
        super(urls, parent);
    }

    public ProxyURLClassLoader(final URL[] urls) {
        super(urls);
    }

    public ProxyURLClassLoader(final URL[] urls, final ClassLoader parent, final URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        if (name.replace("/", ".").startsWith("net.minecraft.server.v1_12_R1")) {
            final String remappedClass = this.jarMapping.classes.get(name.replaceAll("\\.", "\\/"));
            return launchClassLoader.findClass(remappedClass);
        }

        Class<?> result = this.classes.get(name);
        synchronized (name.intern()) {
            if (result == null) {
                result = this.remappedFindClass(name);

                if (result == null) {
                    try {
                        result = super.findClass(name);
                    } catch (ClassNotFoundException ignored) { }
                }

                if (result == null) {
                    try {
                        result = launchClassLoader.findClass(name);
                    } catch (ClassNotFoundException ignored) { }
                }

                if (result == null) {
                    try {
                        result = launchClassLoader.getClass().getClassLoader().loadClass(name);
                    } catch (Throwable throwable) {
                        throw new ClassNotFoundException(name, throwable);
                    }
                }

                if (result == null) {
                    throw new ClassNotFoundException(name);
                }
                this.classes.put(name, result);
            }
        }
        return result;
    }

    private Class<?> remappedFindClass(final String name) throws ClassNotFoundException {
        Class<?> result = null;
        try {
            final String path = name.replace('.', '/').concat(".class");
            final URL url = this.findResource(path);
            if (url != null) {
                final InputStream stream = url.openStream();
                if (stream != null) {
                    byte[] bytecode;
                    bytecode = this.remapper.remapClassFile(stream, RuntimeRepo.getInstance());
                    bytecode = Transformer.process(bytecode);
                    final JarURLConnection jarURLConneCtion = (JarURLConnection)url.openConnection();
                    final URL jarURL = jarURLConneCtion.getJarFileURL();
                    final CodeSource codeSource = new CodeSource(jarURL, new CodeSigner[0]);
                    result = this.defineClass(name, bytecode, 0, bytecode.length, codeSource);
                    if (result != null) {
                        this.resolveClass(result);
                    }
                }
            }
        }
        catch (Throwable t) {
            throw new ClassNotFoundException("Failed to remap class " + name, t);
        }
        return result;
    }
}
