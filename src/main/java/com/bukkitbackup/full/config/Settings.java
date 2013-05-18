package com.bukkitbackup.full.config;

import com.bukkitbackup.full.utils.LogUtils;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Backup - The simple server backup solution.
 *
 * @author Domenic Horner (gamerx)
 */
public final class Settings {

    private static Strings strings;
    private static FileConfiguration settings;
    public boolean useMaxSizeBackup = false;

    public Settings(File configFile, Strings strings) {
        
        // Populate the strings variable.
        Settings.strings = strings;

        try {

            // Checks if configuration file exists, creates it if it does not.
            if (!configFile.exists()) {
                LogUtils.sendLog(strings.getString("newconfigfile"));

                BufferedReader bReader = null;
                BufferedWriter bWriter = null;
                String line;

                try {

                    // Open a stream to the configuration file in the jar, because we can only access over the class loader.
                    bReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/resources/config.yml")));
                    bWriter = new BufferedWriter(new FileWriter(configFile));

                    // Writeout the new configuration file.
                    while ((line = bReader.readLine()) != null) {
                        bWriter.write(line);
                        bWriter.newLine();
                    }

                } catch (Exception e) {
                    LogUtils.exceptionLog(e, "Error opening stream.");
                } finally {
                    try {

                        // Confirm the streams are closed.
                        if (bReader != null) {
                            bReader.close();
                        }
                        if (bWriter != null) {
                            bWriter.close();
                        }
                    } catch (Exception e) {
                        LogUtils.exceptionLog(e, "Error closing configuration stream.");
                    }
                }
            }

            // Initialize the configuration, and populate with settings.
            settings = new YamlConfiguration();
            settings.load(configFile);

        } catch (Exception e) {
            LogUtils.exceptionLog(e, "Failed to load configuration.");
        }
    }

    public void checkSettingsVersion(String requiredVersion) {

        // Get the version information from the file.
        String configVersion = settings.getString("version", null);

        // Check we got a version from the config file.
        if (configVersion == null) {
            LogUtils.sendLog(strings.getString("failedtogetpropsver"));
        } else if (!configVersion.equals(requiredVersion)) {
            LogUtils.sendLog(strings.getString("configupdate"));
        }
    }

    /**
     * Gets the value of a integer property.
     *
     * @param property The name of the property.
     * @param defaultInt Set the default value of the integer.
     * @return The value of the property.
     */
    public int getIntProperty(String property, int defaultInt) {
        return settings.getInt(property, defaultInt);
    }

    /**
     * Gets the value of a boolean property.
     *
     * @param property The name of the property.
     * @param defaultBool Set the default value of the boolean.
     * @return The value of the property.
     */
    public boolean getBooleanProperty(String property, boolean defaultBool) {
        return settings.getBoolean(property, defaultBool);
    }

    /**
     * Gets a value of the string property.
     *
     * @param property The name of the property.
     * @param defaultString Set the default value of the string.
     * @return The value of the property.
     */
    public String getStringProperty(String property, String defaultString) {
        return settings.getString(property, defaultString);
    }

    public long getBackupLimits() {
        String limitSetting = getStringProperty("maxbackups", "25").trim().toLowerCase();

        // If it is null or set to disable.
        if (limitSetting.equals("-1")) {
            return 0L;
        }

        // If it is just a number, return minutes.
        if (limitSetting.matches("^[0-9]+$")) {
            LogUtils.sendDebug("Max Backups: Amount (M:0011)");

            return Long.parseLong(limitSetting);
        } else if (limitSetting.matches("^[0-9]+[a-z]$")) {
            LogUtils.sendDebug("Max Backups: Size (M:0010)");

            useMaxSizeBackup = true;
            Pattern timePattern = Pattern.compile("^([0-9]+)[a-z]$");
            Matcher amountTime = timePattern.matcher(limitSetting);
            Pattern letterPattern = Pattern.compile("^[0-9]+([a-z])$");
            Matcher letterTime = letterPattern.matcher(limitSetting);
            if (letterTime.matches() && amountTime.matches()) {
                String letter = letterTime.group(1);
                long bytes = Long.parseLong(amountTime.group(1));

                if (letter.equals("k")) {
                    return bytes * 1024L;
                } else if (letter.equals("m")) {
                    return bytes * 1048576L;
                } else if (letter.equals("g")) {
                    return bytes * 1073741824L;
                } else {
                    LogUtils.sendLog(strings.getString("unknownsizeident"));
                    return bytes;
                }
            } else {
                LogUtils.sendLog(strings.getString("checksizelimit"));
                return 0L;
            }
        } else {
            LogUtils.sendDebug("Max Backups: Unknown (M:0012)");

            LogUtils.sendLog(strings.getString("checksizelimit"));
            return 0L;
        }
    }
}
