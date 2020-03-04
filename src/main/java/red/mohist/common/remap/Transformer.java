// https://github.com/Maxqia/ReflectionRemapper/blob/master/src/main/java/com/maxqia/ReflectionRemapper/Transformer.java
package red.mohist.common.remap;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import red.mohist.common.remap.proxy.ProxyClassLoader;
import red.mohist.common.remap.proxy.ProxyMethodHandles_Lookup;
import red.mohist.common.remap.proxy.ProxyURLClassLoader;
import red.mohist.common.remap.remappers.RemapMethods;

public class Transformer { // This is kinda like RemapperProcessor from SpecialSource

    public static Map<String, Class> remapStaticMethod = Maps.newHashMap();
    public static Map<String, Class> remapVirtualMethod = Maps.newHashMap();
    public static Map<String, Class> remapVirtualMethodToStatic = Maps.newHashMap();
     public static Map<String, Class> remapSuperClass = Maps.newHashMap();

    static {
        remapStaticMethod.put("java/lang/Class;forName", RemapMethods.class);
        remapStaticMethod.put("java/lang/invoke/MethodType;fromMethodDescriptorString", ProxyMethodHandles_Lookup.class);

        remapVirtualMethodToStatic.put("java/lang/Class;getField", RemapMethods.class);
        remapVirtualMethodToStatic.put("java/lang/Class;getDeclaredField", RemapMethods.class);
        remapVirtualMethodToStatic.put("java/lang/Class;getMethod", RemapMethods.class);
        remapVirtualMethodToStatic.put("java/lang/Class;getDeclaredMethod", RemapMethods.class);
        remapVirtualMethodToStatic.put("java/lang/Class;getSimpleName", RemapMethods.class);
        remapVirtualMethodToStatic.put("java/lang/Class;getDeclaredMethods", RemapMethods.class);
        remapVirtualMethodToStatic.put("java/lang/reflect/Field;getName", RemapMethods.class);
        remapVirtualMethodToStatic.put("java/lang/reflect/Method;getName", RemapMethods.class);
        remapVirtualMethodToStatic.put("java/lang/ClassLoader;loadClass", RemapMethods.class);
        remapVirtualMethodToStatic.put("java/lang/invoke/MethodHandles$Lookup;findVirtual", ProxyMethodHandles_Lookup.class);
        remapVirtualMethodToStatic.put("java/lang/invoke/MethodHandles$Lookup;findStatic", ProxyMethodHandles_Lookup.class);
        remapVirtualMethodToStatic.put("java/lang/invoke/MethodHandles$Lookup;findSpecial", ProxyMethodHandles_Lookup.class);
        remapVirtualMethodToStatic.put("java/lang/invoke/MethodHandles$Lookup;unreflect", ProxyMethodHandles_Lookup.class);

        remapSuperClass.put("java/net/URLClassLoader", ProxyURLClassLoader.class);
        remapSuperClass.put("java/lang/ClassLoader", ProxyClassLoader.class);
    }

    public static byte[] process(InputStream inputStream) throws IOException {
        return process(new ClassReader(inputStream));
    }

    public static byte[] process(byte[] bytecode) {
        return process(new ClassReader(bytecode));
    }

    /**
     * Convert code from using Class.X methods to our remapped versions
     */
    public static byte[] process(ClassReader classReader) {
        ClassNode node = new ClassNode();
        classReader.accept(node, 0); // Visit using ClassNode

        boolean remapCL = false;
        Class<?> remappedSuperClass = remapSuperClass.get(node.superName);
        if (remappedSuperClass != null) {
            if (remappedSuperClass == ProxyClassLoader.class) {
                remapVirtualMethod.put(node.name + ";defineClass", ProxyClassLoader.class);
            }
            node.superName = Type.getInternalName(remappedSuperClass);
            remapCL = true;
        }

        for (MethodNode method : node.methods) { // Taken from SpecialSource
            ListIterator<AbstractInsnNode> insnIterator = method.instructions.iterator();
            while (insnIterator.hasNext()) {
                AbstractInsnNode next = insnIterator.next();

                if (next instanceof TypeInsnNode && next.getOpcode() == Opcodes.NEW) { // remap new URLClassLoader
                    TypeInsnNode insn = (TypeInsnNode) next;
                    Class<?> remappedClass = remapSuperClass.get(insn.desc);
                    if (remappedClass != null) {
                        insn.desc = Type.getInternalName(remappedClass);
                        remapCL = true;
                    }
                }

                if (next instanceof MethodInsnNode) {
                    MethodInsnNode insn = (MethodInsnNode) next;
                    switch (insn.getOpcode()) {
                        case Opcodes.INVOKEVIRTUAL:
                            remapVirtual(insn);
                            break;
                        case Opcodes.INVOKESTATIC:
                            remapStatic(insn);
                            break;
                        case Opcodes.INVOKESPECIAL:
                            if (remapCL) {
                                remapSuperClass(insn);
                            }
                            break;
                    }
                    if ("javax/script/ScriptEngineManager".equals(insn.owner) && "()V".equals(insn.desc) && "<init>".equals(insn.name)) {
                        insn.desc = "(Ljava/lang/ClassLoader;)V";
                        int opcode = Opcodes.INVOKESTATIC;
                        method.instructions.insertBefore(insn, new MethodInsnNode(opcode, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;", opcode == Opcodes.INVOKEINTERFACE));
                        method.maxStack++;
                    }
                }
            }
        }

        ClassWriter cw = new ClassWriter(0);
        node.accept(cw); // Convert back into bytes
        return cw.toByteArray();
    }

    public static void remapStatic(AbstractInsnNode insn) {
        MethodInsnNode method = (MethodInsnNode) insn;
        Class<?> remappedClass = remapStaticMethod.get((method.owner + ";" + method.name));
        if (remappedClass != null) {
            method.owner = Type.getInternalName(remappedClass);
        }
    }

    public static void remapVirtual(AbstractInsnNode insn) {
        MethodInsnNode method = (MethodInsnNode) insn;
        Class<?> remappedClass = remapVirtualMethodToStatic.get((method.owner + ";" + method.name));
        if (remappedClass != null) {
            Type returnType = Type.getReturnType(method.desc);
            ArrayList<Type> args = new ArrayList<>();
            args.add(Type.getObjectType(method.owner));
            args.addAll(Arrays.asList(Type.getArgumentTypes(method.desc)));

            method.setOpcode(Opcodes.INVOKESTATIC);
            method.owner = Type.getInternalName(remappedClass);
            method.desc = Type.getMethodDescriptor(returnType, args.toArray(new Type[0]));
        } else {
            remappedClass = remapVirtualMethod.get((method.owner + ";" + method.name));
            if (remappedClass != null) {
                method.name += "Remap";
                method.owner = Type.getInternalName(remappedClass);
            }
        }
    }

    private static void remapSuperClass(MethodInsnNode method) {
        Class<?> remappedClass = remapSuperClass.get(method.owner);
        if (remappedClass != null && "<init>".equals(method.name)) {
            method.owner = Type.getInternalName(remappedClass);
        }
    }
}
