package CodeExecution;

import java.util.Map;

public class ByteMapClassLoader extends ClassLoader {
    private final Map<String, byte[]> classes;

    public ByteMapClassLoader(ClassLoader parent, Map<String, byte[]> classes){
        super(parent);
        this.classes = classes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = classes.get(name);
        if (bytes == null) throw new ClassNotFoundException(name);
        return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c;
        synchronized (getClassLoadingLock(name)) {
            c = findLoadedClass(name);
            if (c == null) {
                if (classes.containsKey(name)){
                    c = findClass(name);
                } else {
                    c = getParent().loadClass(name);
                }
            }
        }
        if (resolve) resolveClass(c);
        return c;
    }
}
