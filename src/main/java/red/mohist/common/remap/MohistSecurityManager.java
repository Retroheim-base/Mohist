package red.mohist.common.remap;

public class MohistSecurityManager extends SecurityManager{

    public Class<?> getCallerClass(int skip) {
        return getClassContext()[skip + 1];
    }
}
