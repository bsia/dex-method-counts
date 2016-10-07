package com.github.spyhunter99;

import com.github.spyhunter99.model.CountData;

import java.io.BufferedInputStream;
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


    public static CountData getClasses(File jar) throws Exception{


        //aars are not supportable without android mockup jars
       // if (jar.getName().endsWith(".aar"))
         //   return processAndroidArchive(jar);
        if (jar.getName().endsWith(".jar"))
            return processJavaArchive(jar);
       return null;
    }

    private static CountData processJavaArchive(File jar) throws Exception{
        CountData data = new CountData();
        data.fileName=jar.getName();

        //get the class list by examining the zip file structure of the jar

        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(jar));
        List<String> paths = new ArrayList<String>();
        paths.addAll(processZip(zipInputStream));
        zipInputStream.close();

        URLClassLoader child = new URLClassLoader(new URL[] {jar.toURL()}, Thread.currentThread().getContextClassLoader());
        for (int i=0; i < paths.size(); i++) {
            String clz = paths.get(i);
            Class classToLoad = Class.forName(clz, true, child);

            Method[] methods = classToLoad.getDeclaredMethods();
            if (methods!=null)
                data.overallMetrics.methodCount+=methods.length;
            Field[] declaredFields = classToLoad.getDeclaredFields();
            if (declaredFields!=null)
                data.overallMetrics.fieldCount+=declaredFields.length;
            String pkg=classToLoad.getPackage().toString();

            //TODO build the tree node based on packages

        }

        return data;
    }

    private static CountData processAndroidArchive(File jar) throws Exception{
        //looking for classes.jar and a libs folder
        CountData data = new CountData();
        data.fileName=jar.getName();

        //get the class list by examining the zip file structure of the jar

        ZipInputStream zip = new ZipInputStream(new FileInputStream(jar));
        //first the classes.jar
        List<String> jars =new ArrayList<String>();
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
        for (int k=0; k < jars.size(); k++) {
            FileInputStream fis = new FileInputStream(jars.get(k));
            ZipInputStream zis = new ZipInputStream(fis);
            paths.addAll(processZip(zis));
            zis.close();
            fis.close();


            URLClassLoader child = new URLClassLoader(new URL[] {new File(jars.get(k)).toURL()}, Thread.currentThread().getContextClassLoader());
            for (int i=0; i < paths.size(); i++) {
                String clz = paths.get(i);
                try {
                    Class classToLoad = Class.forName(clz, true, child);

                    Method[] methods = classToLoad.getDeclaredMethods();
                    if (methods != null)
                        data.overallMetrics.methodCount += methods.length;
                    Field[] declaredFields = classToLoad.getDeclaredFields();
                    if (declaredFields != null)
                        data.overallMetrics.fieldCount += declaredFields.length;
                    String pkg = classToLoad.getPackage().toString();
                }catch (Throwable t){
                    //happens looking for "provided" dependencies, like the android junk
                }
                //TODO build the tree node based on packages

            }
        }

        return data;
    }



    private static List<String>  processZip(ZipInputStream zip) throws IOException, ClassNotFoundException {
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
