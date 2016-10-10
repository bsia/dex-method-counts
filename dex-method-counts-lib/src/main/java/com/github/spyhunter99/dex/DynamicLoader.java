package com.github.spyhunter99.dex;

import com.github.spyhunter99.dex.model.CountData;
import com.github.spyhunter99.dex.model.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by alex on 10/6/16.
 */
public class DynamicLoader {


    public static CountData getClasses(File jar, boolean includeClasses, String packageFilter, int maxDepth, DexCount.Filter filter) throws Exception {


        //aars are not supportable without android mockup jars
        // if (jar.getName().endsWith(".aar"))
        //   return processAndroidArchive(jar);
        if (jar.getName().endsWith(".jar")) {
            return processJavaArchive(jar, includeClasses, packageFilter, maxDepth, filter);
        }
        return null;
    }

    private static CountData processJavaArchive(File jar, boolean includeClasses, String packageFilter, int maxDepth, DexCount.Filter filter) throws Exception {
        CountData ret = new CountData();
        ret.fileName = jar.getName();

        //get the class list by examining the zip file structure of the jar

        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(jar));
        List<String> paths = new ArrayList<String>();
        paths.addAll(processZip(zipInputStream));
        zipInputStream.close();

        URLClassLoader child = new URLClassLoader(new URL[]{jar.toURL()}, Thread.currentThread().getContextClassLoader());
        for (int k = 0; k < paths.size(); k++) {
            String clz = paths.get(k);// classes to analze
            Class classToLoad = Class.forName(clz, true, child);

            Method[] methods = classToLoad.getDeclaredMethods();
            int methodsCount = 0;
            int fieldCount = 0;
            if (methods != null) {
                for (int i = 0; i < methods.length; i++) {
                    if (!methods[i].isSynthetic()) {
                        methodsCount++;
                    }
                }
            }
            ret.overallMetrics.methodCount += methodsCount;
            Field[] declaredFields = classToLoad.getDeclaredFields();
            if (declaredFields != null) {
                for (int i = 0; i < declaredFields.length; i++) {
                    if (!declaredFields[i].isSynthetic()) {
                        fieldCount++;
                    }
                }
            }

            ret.overallMetrics.fieldCount += fieldCount;
            String packageName = classToLoad.getPackage().toString();
            packageName = packageName.replace("package", "").trim();
            Node packageNode = ret.packageTree;
            if (packageNode == null) {
                packageNode = ret.packageTree = new Node();
            }
            String packageNamePieces[] = packageName.split("\\.");

            for (int i = 0; i < packageNamePieces.length && i < maxDepth; i++) {
                packageNode.count.methodCount+=methodsCount;
                packageNode.count.fieldCount += fieldCount;
                String name = packageNamePieces[i];
                if (packageNode.children.containsKey(name)) {
                    packageNode = packageNode.children.get(name);
                } else {
                    Node childPackageNode = new Node();
                    //childPackageNode.count.fieldCount += fieldCount;
                    //childPackageNode.count.methodCount += methodsCount;
                    if (name.length() == 0) {
                        // This method is declared in a class that is part of the default package.
                        // Typical examples are methods that operate on arrays of primitive data types.
                        name = "(default)";
                    }
                    packageNode.children.put(name, childPackageNode);
                    packageNode = childPackageNode;
                }
            }
            //TODO build the tree node based on packages

        }

        return ret;
    }

    private static CountData processAndroidArchive(File jar) throws Exception {
        //looking for classes.jar and a libs folder
        CountData data = new CountData();
        data.fileName = jar.getName();

        //get the class list by examining the zip file structure of the jar

        ZipInputStream zip = new ZipInputStream(new FileInputStream(jar));
        //first the classes.jar
        List<String> jars = new ArrayList<String>();
        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            if (entry.getName().equals("classes.jar") && !entry.isDirectory()) {
                File tmpfile = new File(UUID.randomUUID().toString() + ".jar");
                tmpfile.deleteOnExit();
                OutputStream out = new FileOutputStream(tmpfile);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = zip.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.close();
                jars.add(tmpfile.getName());
            }
            if (entry.getName().equals("libs") && entry.isDirectory()) {
                //TODO handle libs crap
            }
        }
        List<String> paths = new ArrayList<String>();
        for (int k = 0; k < jars.size(); k++) {
            FileInputStream fis = new FileInputStream(jars.get(k));
            ZipInputStream zis = new ZipInputStream(fis);
            paths.addAll(processZip(zis));
            zis.close();
            fis.close();


            URLClassLoader child = new URLClassLoader(new URL[]{new File(jars.get(k)).toURL()}, Thread.currentThread().getContextClassLoader());
            for (int i = 0; i < paths.size(); i++) {
                String clz = paths.get(i);
                try {
                    Class classToLoad = Class.forName(clz, true, child);

                    Method[] methods = classToLoad.getDeclaredMethods();
                    if (methods != null) {
                        data.overallMetrics.methodCount += methods.length;
                    }
                    Field[] declaredFields = classToLoad.getDeclaredFields();
                    if (declaredFields != null) {
                        data.overallMetrics.fieldCount += declaredFields.length;
                    }
                    String pkg = classToLoad.getPackage().toString();
                } catch (Throwable t) {
                    //happens looking for "provided" dependencies, like the android junk
                }
                //TODO build the tree node based on packages

            }
        }

        return data;
    }


    private static List<String> processZip(ZipInputStream zip) throws IOException, ClassNotFoundException {
        List<String> path = new ArrayList<String>();
        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            if (entry.getName().endsWith(".class") && !entry.isDirectory()) {
                final StringBuilder classname = new StringBuilder();
                for (final String part : entry.getName().split("/")) {
                    if (classname.length() != 0) {
                        classname.append(".");
                    }
                    classname.append(part);
                    if (part.endsWith(".class")) {
                        classname.setLength(classname.length() - ".class".length());
                    }
                }
                //Class<?> forName = Class.forName(classname.toString());
                path.add(classname.toString());
            }
        }
        return path;
    }

}
