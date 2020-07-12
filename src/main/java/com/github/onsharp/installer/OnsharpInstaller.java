package com.github.onsharp.installer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class OnsharpInstaller {

    private static final String INSTALL_URL = "https://eternitylife.de/onsharp_update.json";
    private static final Gson GSON = new Gson();

    public static void main(String[] args) throws Exception {
        String destPath = new File(OnsharpInstaller.class.getProtectionDomain().getCodeSource().getLocation()
                .toURI()).getParentFile().getAbsolutePath();
        String configPath = destPath + "/server_config.json";
        if (!new File(configPath).exists()) {
            System.out.println("[Onsharp-Installer] NO SERVER CONFIG FILE FOUND: THE INSTALLER MUST BE IN THE SERVER DIRECTORY!");
            return;
        }
        OsCheck.OSType type = OsCheck.getOperatingSystemType();
        if (type != OsCheck.OSType.Windows && type != OsCheck.OSType.Linux) {
            System.out.println("[Onsharp-Installer] THE OS IS NOT SUPPORTED: ONSHARP ONLY SUPPORTS WINDOWS AND LINUX!");
            return;
        }
        System.out.println("[Onsharp-Installer] Retrieving dist meta...");
        InstallData data = getData();
        System.out.println("[Onsharp-Installer] Download install file...");
        Path filePath = Paths.get(destPath, "install.zip");
        downloadFile(type == OsCheck.OSType.Windows ? data.getWin() : data.getUnix(), filePath);
        System.out.println("[Onsharp-Installer] Installing files...");
        Unzipper.unzip(filePath, Paths.get(destPath));
        System.out.println("[Onsharp-Installer] Adjusting server...");
        JsonParser parser = new JsonParser();
        JsonObject config = parser.parse(new String(Files.readAllBytes(Paths.get(configPath)))).getAsJsonObject();
        JsonArray plugins = config.getAsJsonArray("plugins");
        if (contains(plugins, "onsharp-runtime"))
            plugins.add("onsharp-runtime");
        JsonArray packages = config.getAsJsonArray("packages");
        if (contains(packages, "onsharp"))
            packages.add("onsharp");
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(config);
        Files.write(Paths.get(configPath), json.getBytes());
        System.out.println("[Onsharp-Installer] Cleaning environment...");
        filePath.toFile().delete();
        System.out.println("[Onsharp-Installer] Successfully installed!");
    }

    private static boolean contains(JsonArray array, String item) {
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).getAsString().equals(item)) {
                return false;
            }
        }

        return true;
    }

    private static InstallData getData() throws Exception {
        URL url = new URL(INSTALL_URL);
        HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
        httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");
        BufferedReader reader = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
        StringBuilder json = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
            json.append(line);
        reader.close();
        httpcon.disconnect();
        return GSON.fromJson(json.toString(), InstallData.class);
    }

    private static void downloadFile(String fileUrl, Path file) throws Exception {
        URL url = new URL(fileUrl);
        HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
        httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");
        try (InputStream in = httpcon.getInputStream()) {
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        }
        httpcon.disconnect();
    }
}
