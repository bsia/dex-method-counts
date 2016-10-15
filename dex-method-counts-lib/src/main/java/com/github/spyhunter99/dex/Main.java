/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.spyhunter99.dex;

import com.android.dexdeps.DexData;
import com.android.dexdeps.DexDataException;
import com.github.spyhunter99.dex.model.CountData;
import com.github.spyhunter99.dex.model.Node;
import com.github.spyhunter99.dex.writers.FormattedHtml;
import com.github.spyhunter99.dex.writers.FormattedText;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * this is a fork of https://github.com/mihaip/dex-method-counts
 */
public class Main {

    private boolean includeClasses;
    private String packageFilter;
    private int maxDepth = Integer.MAX_VALUE;
    private DexMethodCounts.Filter filter = DexMethodCounts.Filter.ALL;
    private List<CountData> data =new ArrayList<CountData>();
    private File outputDir = new File(".");
    private boolean fileout=true;
    private boolean stdout=true;

    public static void main(String[] args) {
        Main main = new Main();
        System.exit(main.run(args));
    }

    /**
     * processes the input files for field and method counts
     * @param args
     */
    public int run(String[] args) {
        try {
            String[] inputFileNames = parseArgs(args);
            data.clear();
            for (String fileName : collectFileNames(inputFileNames)) {
                System.out.println("Processing " + fileName);
                DexCount methods = new DexMethodCounts();

                Map<String, RandomAccessFile> dexFiles = openInputFiles(fileName);

                for (String dexFilenName : dexFiles.keySet()) {
                    RandomAccessFile dexFile = dexFiles.get(dexFilenName);
                    DexData dexData = new DexData(dexFilenName, dexFile);
                    try {
                        dexData.load();
                        CountData generate1 = methods.generate(dexData, includeClasses, packageFilter, maxDepth, filter);
                        generate1.fileName=fileName;
                        boolean merged=false;

                        //https://github.com/spyhunter99/dex-method-counts/issues/3
                        //search for existing report in the case of multidex setups
                        for (int i=0; i < data.size(); i++){
                            if (data.get(i).fileName.equals(generate1.fileName)){
                                //merge the data
                                data.get(i).isMultiDex=true;
                                merge(data.get(i), generate1);
                                merged=true;
                                break;
                            }
                        }

                        if (!merged)
                            data.add(generate1);
                        dexFile.close();
                        continue;
                    } catch (DexDataException d){
                        //not a dex'd file, plan b;
                    }
                    try {
                        data.add(DynamicLoader.getClasses(new File(fileName), includeClasses, packageFilter, maxDepth, filter));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }

                }
            }

            //then output the data

            processOutput(data);



        } catch (UsageException ue) {
            usage();
            return (2);
        } catch (IOException ioe) {
            if (ioe.getMessage() != null) {
                System.err.println("Failed: " + ioe);
            }
            return (1);
        }
        return 0;
    }

    private void merge(CountData output, CountData newrecord) {
        output.isMultiDex=true;
        output.overallMetrics.fieldCount+=newrecord.overallMetrics.fieldCount;
        output.overallMetrics.methodCount+=newrecord.overallMetrics.methodCount;
        merge(output.packageTree, newrecord.packageTree);
    }

    private void merge(Node lhs, Node rhs) {
        lhs.count.fieldCount+=rhs.count.fieldCount;
        lhs.count.methodCount+=rhs.count.methodCount;
        //foreach item in right, check to see if left has it
            //if not, add it
            //if yes, merge it
            //then merge all children of the node

        Iterator<Map.Entry<String, Node>> iterator = rhs.children.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<String, Node> next = iterator.next();
            Node rightNode = next.getValue();
            if (lhs.children.containsKey(next.getKey())){
                Node leftNode = lhs.children.get(next.getKey());
                merge (leftNode, rightNode);
            } else {
                //add to left node as is
                lhs.children.put(next.getKey(), next.getValue());
            }
        }

    }


    public void enableStdOut(boolean value){
        stdout=value;

    }

    public void enableFileOutput(boolean value){
        fileout=value;
    }


    /**
     * if set and text or html output is enabled, the output will end up this directory
     * @param directory
     */
    public void setOutputDirectory(File directory){
        outputDir=directory;
    }


    private void closeAndClear(List<OutputStream> outputStreams) {
        for (int i=0; i < outputStreams.size(); i++){
            try {
                outputStreams.get(i).close();
            } catch (Exception e) {
            }
        }
        outputStreams.clear();

    }

    /**
     * gets the results of the last analysis, useful for wiring in your own output mechanism
     * @return
     */
    public List<CountData> getData(){
        return data;
    }

    private void processOutput(List<CountData> data) {
        FormattedText text = new FormattedText();
        FormattedHtml html = new FormattedHtml();

        if (data.size()>1) {
            //one report for all files processed
            String report = text.getReport(data);
            if (stdout)
                System.out.println(report);
            if (fileout){
                try {
                    FileOutputStream fos = new FileOutputStream(outputDir.getAbsolutePath() + File.separator + "dex-count-report.txt");
                    fos.write(report.getBytes(Charset.defaultCharset()));
                    fos.close();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }

            report=null;
            report = html.getReport(data);
            if (fileout){
                try {
                    FileOutputStream fos = new FileOutputStream(outputDir.getAbsolutePath() + File.separator + "index.html");
                    fos.write(report.getBytes(Charset.defaultCharset()));
                    fos.close();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
            //individual reports for each file processed
            for (int i = 0; i < data.size(); i++) {
                report = text.getReport(data.get(i));
                if (stdout)
                    System.out.println(report);
                if (fileout) {
                    try {
                        FileOutputStream fos = new FileOutputStream(outputDir.getAbsolutePath() + File.separator + new File(data.get(i).fileName).getName() + ".txt");
                        fos.write(report.getBytes(Charset.defaultCharset()));
                        fos.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                report = null;
                report = html.getReport(data.get(i));
                if (fileout) {
                    try {
                        FileOutputStream fos = new FileOutputStream(outputDir.getAbsolutePath() + File.separator + new File(data.get(i).fileName).getName() + ".html");
                        fos.write(report.getBytes(Charset.defaultCharset()));
                        fos.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } else {
            //one report for all files processed
            String report = text.getReport(data);
            if (stdout)
                System.out.println(report);
            if (fileout){
                try {
                    FileOutputStream fos = new FileOutputStream(outputDir.getAbsolutePath() + File.separator + "dex-count-report.txt");
                    fos.write(report.getBytes(Charset.defaultCharset()));
                    fos.close();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }

            report=null;
            report = html.getReport(data);
            if (fileout){
                try {
                    FileOutputStream fos = new FileOutputStream(outputDir.getAbsolutePath() + File.separator + "dex-count-report.html");
                    fos.write(report.getBytes(Charset.defaultCharset()));
                    fos.close();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }
    }



    /**
     * Opens an input file, which could be a .dex or a .jar/.apk with a
     * classes.dex inside.  If the latter, we extract the contents to a
     * temporary file.
     */
    Map<String,RandomAccessFile> openInputFiles(String fileName) throws IOException {
        Map<String,RandomAccessFile> dexFiles = new HashMap<String, RandomAccessFile>();

        openInputFileAsZip(fileName, dexFiles);
        if (dexFiles.size() == 0) {
            File inputFile = new File(fileName);
            RandomAccessFile dexFile = new RandomAccessFile(inputFile, "r");
            dexFiles.put(fileName, dexFile);
        }

        return dexFiles;
    }

    /**
     * Tries to open an input file as a Zip archive (jar/apk) with a
     * "classes.dex" inside.
     */
    void openInputFileAsZip(String fileName, Map<String,RandomAccessFile> dexFiles) throws IOException {
        ZipFile zipFile;

        // Try it as a zip file.
        try {
            zipFile = new ZipFile(fileName);
        } catch (FileNotFoundException fnfe) {
            // not found, no point in retrying as non-zip.
            System.err.println("Unable to open '" + fileName + "': " +
                    fnfe.getMessage());
            throw fnfe;
        } catch (ZipException ze) {
            // not a zip
            return;
        }

        // Open and add all files matching "classes.*\.dex" in the zip file.
        for (ZipEntry entry : Collections.list(zipFile.entries())) {
            if (entry.getName().matches("classes.*\\.dex")) {
                dexFiles.put(entry.getName(), openDexFile(zipFile, entry));
            }
        }

        zipFile.close();
    }

    RandomAccessFile openDexFile(ZipFile zipFile, ZipEntry entry) throws IOException  {
        // We know it's a zip; see if there's anything useful inside.  A
        // failure here results in some type of IOException (of which
        // ZipException is a subclass).
        InputStream zis = zipFile.getInputStream(entry);

        // Create a temp file to hold the DEX data, open it, and delete it
        // to ensure it doesn't hang around if we fail.
        File tempFile = File.createTempFile("dexdeps", ".dex");
        RandomAccessFile dexFile = new RandomAccessFile(tempFile, "rw");
        tempFile.delete();

        // Copy all data from input stream to output file.
        byte copyBuf[] = new byte[32768];
        int actual;

        while (true) {
            actual = zis.read(copyBuf);
            if (actual == -1)
                break;

            dexFile.write(copyBuf, 0, actual);
        }

        dexFile.seek(0);

        return dexFile;
    }

    private String[] parseArgs(String[] args) {
        //TODO replace with commons cli, because this is just silly
        int idx;

        for (idx = 0; idx < args.length; idx++) {
            String arg = args[idx];

            if (arg.equals("--") || !arg.startsWith("--")) {
                break;
            } else if (arg.equals("--disableStdOut")) {
                stdout=false;
            } else if (arg.equals("--enableFileOut")) {
                fileout=true;
            } else if (arg.equals("--include-classes")) {
                includeClasses = true;
            } else if (arg.startsWith("--package-filter=")) {
                packageFilter = arg.substring(arg.indexOf('=') + 1);
            } else if (arg.startsWith("--max-depth=")) {
                maxDepth =
                    Integer.parseInt(arg.substring(arg.indexOf('=') + 1));
            } else if (arg.startsWith("--filter=")) {
                filter = Enum.valueOf(
                    DexMethodCounts.Filter.class,
                    arg.substring(arg.indexOf('=') + 1).toUpperCase());
            } else {
                System.err.println("Unknown option '" + arg + "'");
                throw new UsageException();
            }
        }

        // We expect at least one more argument (file name).
        int fileCount = args.length - idx;
        if (fileCount == 0) {
            throw new UsageException();
        }
        String[] inputFileNames = new String[fileCount];
        System.arraycopy(args, idx, inputFileNames, 0, fileCount);
        return inputFileNames;
    }

    private void usage() {
        System.err.print(
            "DEX per-package/class method counts v1.5\n" +
            "Usage: dex-method-counts [options] <file.{dex,apk,jar,directory}> ...\n" +
            "Options:\n" +
            "  --disableStdOut\n" +
            "  --enableFileOut\n" +
            "  --include-classes\n" +
            "  --package-filter=com.foo.bar\n" +
            "  --max-depth=N\n" +
            "  --filter=ALL|DEFINED_ONLY|REFERENCED_ONLY\n"
        );
    }

    /**
     * Checks if input files array contain directories and
     * adds it's contents to the file list if so.
     * Otherwise just adds a file to the list.
     *
     * @return a List of file names to process
     */
    private List<String> collectFileNames(String[] inputFileNames) {
        List<String> fileNames = new ArrayList<String>();
        for (String inputFileName : inputFileNames) {
            File file = new File(inputFileName);
            if (file.isDirectory()) {
                String dirPath = file.getAbsolutePath();
                for (String fileInDir: file.list()){
                    fileNames.add(dirPath + File.separator + fileInDir);
                }
            } else {
                fileNames.add(inputFileName);
            }
        }
        return fileNames;
    }

    private static class UsageException extends RuntimeException {}
}
