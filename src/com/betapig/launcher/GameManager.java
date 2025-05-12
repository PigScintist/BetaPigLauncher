package com.betapig.launcher;

import jakarta.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.swing.SwingWorker;
import com.betapig.launcher.settings.LauncherSettings;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

public class GameManager {
    private static GameManager instance;

    public static final String MINECRAFT_VERSION = "b1.7.3";
    public static final String SERVER_ADDRESS = "147.185.221.26";
    public static final int SERVER_PORT = 50566;
    public static final String SERVER_NAME = "BetaPig Network";
    public static final String SERVER_VERSION = "Beta 1.7.3";

    private static final String GAME_DIR = System.getProperty("user.home") + File.separator + ".betapig";
    private static final String BIN_DIR = GAME_DIR + File.separator + "bin";
    private static final String NATIVES_DIR = GAME_DIR + File.separator + "natives";
    public static final String MODS_DIR = GAME_DIR + File.separator + "mods";

    private static final String MODLOADER_JAR = BIN_DIR + File.separator + "modloader.jar";
    private static final String MINECRAFT_JAR = BIN_DIR + File.separator + "minecraft.jar";
    private static final String LWJGL_JAR = BIN_DIR + File.separator + "lwjgl.jar";
    private static final String LWJGL_UTIL_JAR = BIN_DIR + File.separator + "lwjgl_util.jar";
    private static final String JINPUT_JAR = BIN_DIR + File.separator + "jinput.jar";

    private static final Map<String, String> REQUIRED_FILES = new HashMap<String, String>() {{
        put("minecraft.jar", "ba66e95ccac442279860f64573f976ff"); // b1.7.3
        put("lwjgl.jar", "6e55ddca0cb6375facfecf1c769b7d77"); // LWJGL 2.8.4
        put("lwjgl_util.jar", "0df9866d92e0f383c4753b28a80dd7aa"); // LWJGL 2.8.4
        put("jinput.jar", "b168b014be0186d9e95bf3d263e3a129"); // JInput 2.8.4
        put("modloader.jar", "585948655f59d7418795da3d658d2f7e"); // ModLoader b1.7.3
    }};

    public static class ModInfo {
        private final String name;
        private final String version;
        private final String description;
        private final File file;

        public ModInfo(String name, String version, String description, File file) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.file = file;
        }

        public String getName() { return name; }
        public String getVersion() { return version; }
        public String getDescription() { return description; }
        public File getFile() { return file; }

        @Override
        public String toString() {
            return name + (version != null ? " v" + version : "");
        }
    }

    public List<ModInfo> getInstalledMods() {
        List<ModInfo> mods = new ArrayList<>();
        File modsDir = new File(MODS_DIR);

        if (modsDir.exists() && modsDir.isDirectory()) {
            File[] modFiles = modsDir.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip"));

            if (modFiles != null) {
                for (File modFile : modFiles) {
                    try (java.util.jar.JarFile jar = new java.util.jar.JarFile(modFile)) {
                        // Try to read mod info from mod_info.txt if it exists
                        java.util.zip.ZipEntry infoEntry = jar.getEntry("mod_info.txt");
                        String name = modFile.getName();
                        String version = null;
                        String description = null;

                        if (infoEntry != null) {
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(jar.getInputStream(infoEntry)))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (line.startsWith("name=")) {
                                        name = line.substring(5).trim();
                                    } else if (line.startsWith("version=")) {
                                        version = line.substring(8).trim();
                                    } else if (line.startsWith("description=")) {
                                        description = line.substring(12).trim();
                                    }
                                }
                            }
                        }

                        // If no mod_info.txt, try to find a class that extends BaseMod
                        if (name.equals(modFile.getName())) {
                            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                            while (entries.hasMoreElements()) {
                                java.util.jar.JarEntry entry = entries.nextElement();
                                String entryName = entry.getName();
                                if (entryName.endsWith(".class") && !entryName.contains("$")) {
                                    try (InputStream in = jar.getInputStream(entry)) {
                                        // Read first 4KB of class file to look for "BaseMod" string
                                        byte[] buffer = new byte[4096];
                                        int read = in.read(buffer);
                                        String content = new String(buffer, 0, read);
                                        if (content.contains("BaseMod")) {
                                            name = entryName.substring(0, entryName.length() - 6)
                                                    .replace('/', '.')
                                                    .replaceAll("^mod_", "");
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        mods.add(new ModInfo(name, version, description, modFile));
                    } catch (IOException e) {
                        System.err.println("Error reading mod file: " + modFile.getName());
                    }
                }
            }
        }

        return mods;
    }

    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public interface ProgressCallback {
        void onProgress(String status, int progress);
    }

    public void downloadAndLaunchGame(String username, ProgressCallback callback) {
        callback.onProgress("Checking Minecraft version " + MINECRAFT_VERSION, 0);
        new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    install(callback);
                    // Launch game
                    callback.onProgress("Launching game...", 95);
                    getInstance().launchGame(username);

                    callback.onProgress("Game launched!", 100);
                } catch (Exception e) {
                    callback.onProgress("Error: " + e.getMessage(), -1);
                    throw e;
                }
                return null;
            }
        }.execute();
    }

    public void launchGame(String username) throws IOException {
        // Verify we're using the correct version
        File minecraftJar = new File(BIN_DIR, "minecraft.jar");
        if (!minecraftJar.exists()) {
            throw new IOException("Minecraft JAR not found");
        }
        LauncherSettings settings = new LauncherSettings();
        String memoryMB = settings.getEffectiveMemory();

        // Build classpath
        String classpath = String.join(File.pathSeparator,
                MINECRAFT_JAR,
                LWJGL_JAR,
                LWJGL_UTIL_JAR,
                JINPUT_JAR
        );

        // Find Java 8
        String javaHome = System.getenv("JAVA_HOME_8");
        String javaPath = null;

        if (javaHome != null) {
            File java = new File(javaHome, "bin/java.exe");
            if (java.exists()) {
                javaPath = java.getAbsolutePath();
            }
        }

        if (javaPath == null) {
            // Try Program Files
            String[] programDirs = {"C:\\Program Files\\Java", "C:\\Program Files (x86)\\Java"};
            for (String dir : programDirs) {
                File javaDir = new File(dir);
                if (javaDir.exists() && javaDir.isDirectory()) {
                    File[] versions = javaDir.listFiles((d, name) -> name.startsWith("jdk1.8") || name.startsWith("jre1.8"));
                    if (versions != null && versions.length > 0) {
                        File java = new File(versions[0], "bin/java.exe");
                        if (java.exists()) {
                            javaPath = java.getAbsolutePath();
                            break;
                        }
                    }
                }
            }
        }

        if (javaPath == null) {
            throw new IOException("Could not find Java 8. Please install Java 8 or set JAVA_HOME_8 environment variable.");
        }

        // Build command
        ProcessBuilder pb = new ProcessBuilder(
                javaPath,
                "-Xmx" + memoryMB + "M",
                "-Djava.library.path=" + NATIVES_DIR,
                "-cp", classpath,
                "net.minecraft.client.Minecraft",
                username
        );

        pb.directory(new File(GAME_DIR));
        pb.start();
    }

    private void createDirectories() throws IOException {
        Files.createDirectories(Paths.get(GAME_DIR));
        Files.createDirectories(Paths.get(BIN_DIR));
        Files.createDirectories(Paths.get(NATIVES_DIR));
        Files.createDirectories(Paths.get(MODS_DIR));
    }

    public void install(ProgressCallback callback) throws Exception {
        callback.onProgress("Подготовка...", 0);
        createDirectories();

        // Download and install everything with a single progress message
        callback.onProgress("Загрузка файлов игры...", 20);

        // Download minecraft.jar
        File minecraftJar = new File(BIN_DIR, "minecraft.jar");
        if (!minecraftJar.exists() || !verifyMd5(minecraftJar, REQUIRED_FILES.get("minecraft.jar"))) {
            copyResourceToFile("lib/minecraft.jar", minecraftJar);
        }

        // Download and install ModLoader
        File modloaderJar = new File(MODS_DIR, "modloader.jar");
        if (!modloaderJar.exists() || !verifyMd5(modloaderJar, REQUIRED_FILES.get("modloader.jar"))) {
            copyResourceToFile("lib/modloader.jar", modloaderJar);
            installModLoader(callback);
        }

        callback.onProgress("Загрузка файлов игры...", 60);

        // Download libraries
        downloadLibraries(callback);
        downloadNatives(callback);

        callback.onProgress("Готово!", 100);
    }

    private void installModLoader(ProgressCallback callback) throws Exception {
        callback.onProgress("Installing ModLoader...", 80);
        File modLoaderJar = new File(MODS_DIR, "modloader.jar");
        if (!modLoaderJar.exists() || !verifyMd5(modLoaderJar, REQUIRED_FILES.get("modloader.jar"))) {
            // Copy modloader.jar from resources
            try (InputStream in = getClass().getResourceAsStream("/lib/modloader.jar")) {
                if (in == null) {
                    throw new IOException("Could not find bundled ModLoader");
                }
                Files.copy(in, modLoaderJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // Create temporary jar
        File minecraftJar = new File(BIN_DIR, "minecraft.jar");
        File tempJar = new File(BIN_DIR, "minecraft.jar.tmp");

        // Copy minecraft.jar to temp file, excluding META-INF
        try (java.util.jar.JarFile sourceJar = new java.util.jar.JarFile(minecraftJar);
             java.util.jar.JarOutputStream tempJarStream = new java.util.jar.JarOutputStream(
                     new FileOutputStream(tempJar))) {

            // Track entries to avoid duplicates
            HashSet<String> entries = new HashSet<>();

            // First copy ModLoader files
            try (java.util.jar.JarFile modLoader = new java.util.jar.JarFile(modLoaderJar)) {
                java.util.Enumeration<java.util.jar.JarEntry> modLoaderEntries = modLoader.entries();
                while (modLoaderEntries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = modLoaderEntries.nextElement();
                    if (!entry.getName().startsWith("META-INF/")) {
                        entries.add(entry.getName());
                        tempJarStream.putNextEntry(new java.util.jar.JarEntry(entry.getName()));
                        IOUtils.copy(modLoader.getInputStream(entry), tempJarStream);
                    }
                }
            }

            // Then copy minecraft.jar files, skipping duplicates
            java.util.Enumeration<java.util.jar.JarEntry> sourceEntries = sourceJar.entries();
            while (sourceEntries.hasMoreElements()) {
                java.util.jar.JarEntry entry = sourceEntries.nextElement();
                if (!entry.getName().startsWith("META-INF/") && !entries.contains(entry.getName())) {
                    tempJarStream.putNextEntry(new java.util.jar.JarEntry(entry.getName()));
                    IOUtils.copy(sourceJar.getInputStream(entry), tempJarStream);
                }
            }
        }

        // Replace minecraft.jar with the modified version
        if (!minecraftJar.delete()) {
            throw new IOException("Could not delete old minecraft.jar");
        }
        if (!tempJar.renameTo(minecraftJar)) {
            throw new IOException("Could not rename temporary jar to minecraft.jar");
        }
    }

    private void downloadLibraries(ProgressCallback callback) throws Exception {
        // Download remaining libraries
        String[] libraryFiles = {"lwjgl.jar", "lwjgl_util.jar", "jinput.jar"};
        int fileCount = 0;

        for (String fileName : libraryFiles) {
            String expectedMd5 = REQUIRED_FILES.get(fileName);
            File targetFile = new File(BIN_DIR, fileName);

            if (!targetFile.exists() || !verifyMd5(targetFile, expectedMd5)) {
                callback.onProgress("Installing " + fileName + "...", 50 + (fileCount * 10));

                // First try resources, then lib directory
                File libFile = new File("lib", fileName);
                if (libFile.exists()) {
                    Files.copy(libFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    try (InputStream is = GameManager.class.getResourceAsStream("/lib/" + fileName)) {
                        if (is == null) {
                            throw new IOException("Could not find bundled resource: " + fileName);
                        }
                        Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                if (!verifyMd5(targetFile, expectedMd5)) {
                    throw new IOException("File " + fileName + " has incorrect MD5 hash");
                }
            }
            fileCount++;
        }
    }

    private void downloadNatives(ProgressCallback callback) throws Exception {
        String osName = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch");
        boolean is64Bit = arch.contains("64");

        String nativesName = osName.contains("windows") ? "windows" :
                osName.contains("mac") ? "macos" :
                        "linux";

        // List of native files needed for Windows
        String[] nativeFiles = {
                "jinput-dx8.dll",
                "jinput-raw.dll",
                "lwjgl.dll",
                "OpenAL32.dll"
        };

        // Copy each native file
        for (String fileName : nativeFiles) {
            File targetFile = new File(NATIVES_DIR, fileName);
            if (!targetFile.exists()) {
                callback.onProgress("Installing " + fileName + "...", 90);
                try (InputStream is = GameManager.class.getResourceAsStream("/natives/" + nativesName + "/" + fileName)) {
                    if (is == null) {
                        throw new IOException("Could not find native file: " + fileName);
                    }
                    Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private boolean verifyMd5(File file, String expectedMd5) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] hash = MessageDigest.getInstance("MD5").digest(bytes);
            String actualMd5 = DatatypeConverter.printHexBinary(hash).toLowerCase();
            return actualMd5.equals(expectedMd5.toLowerCase());
        } catch (Exception e) {
            return false;
        }
    }

    private void copyResourceToFile(String resourcePath, File targetFile) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/" + resourcePath)) {
            if (in == null) {
                throw new IOException("Could not find resource: " + resourcePath);
            }
            Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String buildClasspath() {
        StringBuilder classpath = new StringBuilder();
        classpath.append(MINECRAFT_JAR).append(File.pathSeparator);
        classpath.append(LWJGL_JAR).append(File.pathSeparator);
        classpath.append(LWJGL_UTIL_JAR).append(File.pathSeparator);
        classpath.append(JINPUT_JAR);

        // Add all mods from mods directory
        File modsDir = new File(MODS_DIR);
        if (modsDir.exists()) {
            File[] modFiles = modsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (modFiles != null) {
                for (File modFile : modFiles) {
                    classpath.append(File.pathSeparator).append(modFile.getAbsolutePath());
                }
            }
        }

        return classpath.toString();
    }
}
