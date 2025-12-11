package com.RuleApi.common;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class Plugin {

    private Map<String, Class<?>> loadedClasses = new HashMap<>();

    public String  loadCode(String code, String className, String methodName, int forceReload) {
        Class<?> loadedClass = loadedClasses.get(className);

        if (loadedClass == null || forceReload == 1) {
            loadedClass = loadAndCacheCode(code, className);
        }

        if (loadedClass != null) {
            return invokeMethod(loadedClass, methodName);
        }
        return null;
    }

    private Class<?> loadAndCacheCode(String code, String className) {
        File outputDirectory = new File("PluginClass");
        outputDirectory.mkdirs();

        try {
            File sourceFile = new File(outputDirectory, className + ".java");
            Files.write(sourceFile.toPath(), code.getBytes(StandardCharsets.UTF_8));

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            List<String> options = new ArrayList<>(Arrays.asList("-d", outputDirectory.getPath()));
            options.add("-proc:none");

            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(sourceFile);
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, compilationUnits);
            task.call();
            fileManager.close();

            URLClassLoader classLoader = new URLClassLoader(new URL[]{outputDirectory.toURI().toURL()});
            Class<?> loadedClass = classLoader.loadClass(className);

            loadedClasses.put(className, loadedClass);

            return loadedClass;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String invokeMethod(Class<?> loadedClass, String methodName) {
        try {
            Object instance = loadedClass.getDeclaredConstructor().newInstance();
            Method method = loadedClass.getMethod(methodName);
            return (String) method.invoke(instance);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
