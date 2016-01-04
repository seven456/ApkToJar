package com.android.apkToJar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApkToJarFormat {
    private static final String PROPERTIES_FILE = "apkToJar-fromat.properties";
    private static final String ANDROID_SDK = "android.sdk";
    private static final String ANDROID_TARGET = "android.target";

    private static final String INPUT_DIR_SRC = "input.dir.src";
    private static final String INPUT_DIR_RES = "input.dir.res";
    private static final String INPUT_DIR_ASSETS = "input.dir.assets";
    private static final String INPUT_DIR_ANDROIDMANIFEST = "input.dir.androidmanifest";
    private static final String INPUT_DIR_LIBS = "input.dir.libs";
    private static final String INPUT_PACKAGE_CLASS_NAME = "input.package.class.name";
    private static final String INPUT_PACKAGE_DRAWABLE = "input.package.drawable";

    private static final String OUTPUT_JAR = "output.jar";
    private static final String OUTPUT_DIR = "output.dir";
    private static final String IGNORE_FILES = "ignore.files";

    private static Properties PROPERTIES;

    public static void main(String[] args) throws Exception {
        PROPERTIES = new Properties();
        String dir = ApkToJarFormat.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        File file = new File(new File(dir).getParentFile(), PROPERTIES_FILE); // 执行shell脚本时所在的目录
        System.out.println("current dir: " + file);
        if (!file.exists()) { // 在gradle中调试时的目录
            file = new File("./Core", PROPERTIES_FILE);
        }
        PROPERTIES.load(new FileInputStream(file));

        generateRJava();
        generateJar();
    }

    private static void generateRJava() throws FileNotFoundException, UnsupportedEncodingException {
        long time = System.currentTimeMillis();
        String inputDirSrc = PROPERTIES.getProperty(INPUT_DIR_SRC);
        String inputPackageDrawable = PROPERTIES.getProperty(INPUT_PACKAGE_DRAWABLE);
        String layout = "layout";
        String assets = "assets";
        String inputPackageClassName = PROPERTIES.getProperty(INPUT_PACKAGE_CLASS_NAME);
        String packageName = parsePackageName(PROPERTIES.getProperty(INPUT_DIR_ANDROIDMANIFEST));
        String className = inputPackageClassName.substring(0, inputPackageClassName.lastIndexOf("."));
        String packageDir = packageName.replaceAll("\\.", "/");
        File input = new File(inputDirSrc, packageDir + File.separator + inputPackageClassName);
        input.getParentFile().mkdirs();
        PrintWriter writer = new PrintWriter(input, "UTF-8");
        writer.println("package " + packageName + ";\n");
        writer.println("public class " + className + " {\n");

        File drawablesFile = new File(PROPERTIES.getProperty(INPUT_DIR_RES), inputPackageDrawable);
        if (drawablesFile != null && drawablesFile.list() != null && drawablesFile.list().length > 0) {
            writer.print("\tpublic static final class drawable {\n");
            for (File file : drawablesFile.listFiles()) {
                String name = file.getName();
                String fieldName = name.substring(0, name.lastIndexOf("."));
                String fieldValue = "res/" + inputPackageDrawable + "/" + name;
                writer.print("\t\tpublic static String " + fieldName + " = \"" + fieldValue + "\";\n");
            }
            File drawables = new File(PROPERTIES.getProperty(INPUT_DIR_RES), "drawable");
            if (drawables != null) {
                for (File file : drawables.listFiles()) {
                    String name = file.getName();
                    String fieldName = name.substring(0, name.lastIndexOf("."));
                    String fieldValue = "res/drawable" + "/" + name;
                    writer.print("\t\tpublic static String " + fieldName + " = \"" + fieldValue + "\";\n");
                }
            }
            writer.println("\t}\n");
        } else {
            System.out.println("[目录" + drawablesFile + "]不存在");
        }

        File layoutsFile = new File(PROPERTIES.getProperty(INPUT_DIR_RES), layout);
        if (layoutsFile != null && layoutsFile.list() != null && layoutsFile.list().length > 0) {
            writer.print("\tpublic static final class layout {\n");
            for (File file : layoutsFile.listFiles()) {
                String name = file.getName();
                String fieldName = name.substring(0, name.lastIndexOf("."));
                String fieldValue = "res/" + layout + "/" + name;
                writer.print("\t\tpublic static String " + fieldName + " = \"" + fieldValue + "\";\n");
            }
            writer.println("\t}\n");

            Set<String> tags = new LinkedHashSet<String>();
            for (File file : layoutsFile.listFiles()) {
                tags.addAll(parseLayoutViewTag(file.getPath()));
            }
            if (!tags.isEmpty()) {
                writer.print("\tpublic static final class tag {\n");
                for (String tag : tags) {
                    writer.print("\t\tpublic static String " + tag + " = \"" + tag + "\";\n");
                }
                writer.println("\t}\n");
            }
        } else {
            System.out.println("[目录" + layoutsFile + "]不存在");
        }

        File assetsFile = new File(PROPERTIES.getProperty(INPUT_DIR_ASSETS));
        if (assetsFile != null && assetsFile.listFiles() != null && assetsFile.list().length > 0) {
            writer.print("\tpublic static final class assets {\n");
            for (File file : assetsFile.listFiles()) {
                String name = file.getName();
                String fieldName = name.substring(0, name.lastIndexOf("."));
                String fieldValue = assets + "/" + name;
                writer.print("\t\tpublic static String " + fieldName + " = \"" + fieldValue + "\";\n");
            }
            writer.println("\t}\n");
        } else {
            System.out.println("[目录" + assetsFile + "]不存在");
        }

        writer.println("}");
        writer.close();
        System.out.println("[生成" + inputPackageClassName + "总共耗时：" + (System.currentTimeMillis() - time) + "]");
    }

    private static void generateJar() {
        long time = System.currentTimeMillis();
        String androidSdk = PROPERTIES.getProperty(ANDROID_SDK);
        String outputDir = PROPERTIES.getProperty(OUTPUT_DIR);
        String androidJar = androidSdk + File.separator + "platforms" + File.separator + PROPERTIES.getProperty(ANDROID_TARGET) + File.separator
                + "android.jar";
        String aapt = findAapt(androidSdk);
        String tempDir = outputDir + File.separator + "temp";
        deleteDir(new File(tempDir), null);
        final String ignores = PROPERTIES.getProperty(IGNORE_FILES, "").replaceAll("/", "\\" + File.separator);

        System.out.println("[编码layout和drawable资源]");
        new File(tempDir).mkdirs();
        String res = PROPERTIES.getProperty(INPUT_DIR_RES);
        String assets = PROPERTIES.getProperty(INPUT_DIR_ASSETS);
        String androidManifest = PROPERTIES.getProperty(INPUT_DIR_ANDROIDMANIFEST);
        String outApk = tempDir + File.separator + "temp.apk";
        String shellAssets = new File(assets).exists() ? "\" -A \"" + assets : "";
        String shell = "\"" + aapt + "\" package -f -I \"" + androidJar + "\" -S \"" + res + shellAssets + "\" -M \"" + androidManifest + "\" -F \"" + outApk + "\"";
        exec(shell);

        System.out.println("[解压layout和drawable资源]");
        String javaSdk = findJavaSDK(androidSdk);
        final String outPreJar = tempDir + File.separator + "prejar";
        new File(outPreJar).mkdirs();
        String jar = javaSdk + File.separator + "jar";
        shell = "cmd /c cd \"" + outPreJar + "\" && \"" + jar + "\" -xvf \"" + outApk + "\"";
        exec(shell);
        shell = "cmd /c cd \"" + outPreJar + "\" && move res/drawable-xhdpi-v4 res/drawable-xhdpi";
        exec(shell);

        System.out.println("[编译java文件]");
        final StringBuilder sources = new StringBuilder();
        scanDir(new File(PROPERTIES.getProperty(INPUT_DIR_SRC)), new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (!isIgnore(pathname) && pathname.getPath().endsWith(".java") || pathname.getPath().endsWith(".properties")) {
                    sources.append("\"").append(pathname.getPath()).append("\" ");
                }
                return true;
            }

            private boolean isIgnore(File pathname) {
                String[] ignoreList = ignores.split(" ");
                for (String string : ignoreList) {
                    return pathname.getPath().endsWith(string);
                }
                return false;
            }
        });
        final StringBuilder classpath = new StringBuilder(androidJar + File.pathSeparator);
        scanDir(new File(PROPERTIES.getProperty(INPUT_DIR_LIBS)), new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.getPath().endsWith(".jar")) {
                    classpath.append(pathname.getPath());
                }
                return true;
            }
        });
        String javac = javaSdk + File.separator + "javac";
        shell = "\"" + javac + "\" -verbose -encoding UTF-8 -classpath \"" + classpath.toString() + "\" " + sources.toString() + " -d \"" + outPreJar + "\"";
        sources.setLength(0);
        exec(shell);

        System.out.println("[构建jar文件]");
        final StringBuilder classes = new StringBuilder();
        scanDir(new File(outPreJar), new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isFile() && !pathname.getName().equals("AndroidManifest.xml") && !pathname.getName().equals("resources.arsc")) {
                    classes.append("\"").append(pathname.getPath().substring(pathname.getPath().indexOf(outPreJar) + outPreJar.length() + 2)).append("\" ");
                }
                return true;
            }
        });
        String jarFinal = outputDir + File.separator + PROPERTIES.getProperty(OUTPUT_JAR);
        shell = "cmd /c cd \"" + outPreJar + "\" && \"" + jar + "\" -cvf  \"" + jarFinal + "\" " + classes.toString();
        classes.setLength(0);
        exec(shell);

        System.out.println("[删除临时文件]");
        deleteDir(new File(tempDir), null);

        System.out.println("[构建" + jarFinal + "总共耗时：" + (System.currentTimeMillis() - time) + "]");
    }

    private static void exec(final String shell) {
        System.out.println("[执行命令：" + shell + "]");
        try {
            Process process = Runtime.getRuntime().exec(shell);
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            br.close();
            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String execAndGet(final String shell) {
        final StringBuilder sb = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(shell);
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                System.out.println(line);
            }
            br.close();
            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * 非递归方式扫描文件夹
     *
     * @param fileDir
     * @param filter
     */
    private static void scanDir(File fileDir, FileFilter filter) {
        LinkedList<File> linkedList = new LinkedList<File>();
        linkedList.addLast(fileDir);
        while (!linkedList.isEmpty()) {
            File[] files = linkedList.removeFirst().listFiles(filter);
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        linkedList.addLast(file);
                    }
                }
            }
        }
    }

    /**
     * 非递归方式删除文件夹
     */
    public static boolean deleteDir(File dir, FileFilter filter) {
        if (dir.exists() && dir.isDirectory()) {
            LinkedList<File> linkedList = new LinkedList<File>();
            linkedList.addLast(dir);
            while (!linkedList.isEmpty()) {
                File dirTemp = linkedList.getLast();
                File[] files = dirTemp.listFiles(filter);
                if (files == null || files.length == 0) {
                    System.out.println("[删除文件" + dirTemp + "]");
                    if (dirTemp.delete()) {
                        linkedList.removeLast();
                    }
                } else {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            linkedList.addLast(file);
                        } else {
                            System.out.println("[删除文件" + file + "]");
                            file.delete();
                        }
                    }
                }
            }
        }
        return dir.exists();
    }

    private static String findAapt(String androidSdk) {
        String name = "";
        for (File file : new File(androidSdk, "build-tools").listFiles()) {
            if (file.getName().compareTo(name) > 0) {
                name = file.getName();
            }
        }
        if (name == "") {
            new IllegalArgumentException("[查找aapt失败]");
        }
        return androidSdk + File.separator + "build-tools" + File.separator + name + File.separator + "aapt";
    }

    private static String findJavaSDK(String androidSdk) {
        String reslut = null;
        String javaHome = System.getenv().get("JAVA_HOME");
        if (javaHome != null) {
            reslut = javaHome + File.separator + "bin";
        }
        if (reslut == null) {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                String findJava = androidSdk + File.separator + "tools" + File.separator + "lib" + File.separator
                        + (System.getProperty("os.arch").contains("64") ? "find_java64" : "find_java32");
                String shell = "\"" + findJava + "\"";
                reslut = execAndGet(shell);
            } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                // TODO
            } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                // TODO
            }
            reslut = reslut.substring(0, reslut.lastIndexOf(File.separator));
        }
        if (reslut == null) {
            new IllegalArgumentException("[查找java sdk失败]");
        }
        return reslut;
    }

    private static String parsePackageName(String androidManifest) {
        String manifest = readFileToString(new File(androidManifest), "UTF-8");
        Matcher matcher = Pattern.compile(".*package=\"(.*)\".*").matcher(manifest);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static Set<String> parseLayoutViewTag(String layout) {
        Set<String> result = new LinkedHashSet<String>();
        String manifest = readFileToString(new File(layout), "UTF-8");
        Matcher matcher = Pattern.compile(".*android:tag=\"(.*)\".*").matcher(manifest);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    /**
     * 读取文件内容到字符串
     *
     * @param file
     * @param encoding
     * @return
     */
    public static String readFileToString(File file, String encoding) {
        String result = null;
        if (file.exists()) {
            char[] buffer = null;
            BufferedReader br = null;
            InputStreamReader isr = null;
            BufferedWriter bw = null;
            StringWriter sw = new StringWriter();
            try {
                isr = encoding == null ? new InputStreamReader(new FileInputStream(file)) : new InputStreamReader(new FileInputStream(file), encoding);
                br = new BufferedReader(isr);
                bw = new BufferedWriter(sw);
                buffer = new char[8 * 1024];
                int len = 0;
                while ((len = br.read(buffer, 0, buffer.length)) != -1) {
                    bw.write(buffer, 0, len);
                }
                bw.flush();
                result = sw.toString();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bw != null) {
                        bw.close();
                        bw = null;
                    }
                    if (br != null) {
                        br.close();
                        br = null;
                    }
                    if (isr != null) {
                        isr.close();
                        isr = null;
                    }
                    if (sw != null) {
                        sw.close();
                        sw = null;
                    }
                    buffer = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}