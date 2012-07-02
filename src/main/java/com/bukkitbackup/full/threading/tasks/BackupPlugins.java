package com.bukkitbackup.full.threading.tasks;

import com.bukkitbackup.full.config.Settings;
import com.bukkitbackup.full.config.Strings;
import com.bukkitbackup.full.utils.FileUtils;
import static com.bukkitbackup.full.utils.FileUtils.FILE_SEPARATOR;
import com.bukkitbackup.full.utils.LogUtils;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * [Backup] BackupPlugins.java (Plugin Backup) Backup plugins when the function
 * doPlugins() is called.
 *
 * @author Domenic Horner
 */
public class BackupPlugins {

    private final Strings strings;
    private final String backupPath;
    private final boolean shouldZIP;
    private final boolean splitBackup;
    private final boolean useTemp;
    private final String tempDestination;
    private final boolean pluginListMode;
    private final List<String> pluginList;

    public BackupPlugins(Settings settings, Strings strings) {

        this.strings = strings;

        // Get the backup destination.
        backupPath = settings.getStringProperty("backuppath");

        // Get backup properties.
        shouldZIP = settings.getBooleanProperty("zipbackup");
        splitBackup = settings.getBooleanProperty("splitbackup");
        useTemp = settings.getBooleanProperty("usetemp");
        pluginListMode = settings.getBooleanProperty("pluginlistmode");
        pluginList = Arrays.asList(settings.getStringProperty("pluginlist").split(";"));

        // Generate the worldStore.
        if (useTemp) {
            if (!settings.getStringProperty("tempfoldername").equals("")) { // Absolute.
                tempDestination = settings.getStringProperty("tempfoldername").concat(FILE_SEPARATOR);
            } else { // Relative.
                tempDestination = backupPath.concat(FILE_SEPARATOR).concat("temp").concat(FILE_SEPARATOR);
            }
        } else { // No temp folder.
            tempDestination = backupPath.concat(FILE_SEPARATOR);
        }
    }

    // The actual backup should be done here.
    public void doPlugins(String backupName) throws IOException {

        // The FileFilter instance for skipped/enabled plugins.
        FileFilter pluginsFileFilter = new FileFilter() {

            @Override
            public boolean accept(File name) {

                // Check if there are plugins listed.
                if (pluginList.size() > 0 && !pluginList.get(0).isEmpty()) {

                    // Loop each plugin.
                    for (int i = 0; i < pluginList.size(); i++) {

                        String findMe = "plugins".concat(FILE_SEPARATOR).concat(pluginList.get(i));

                        int isFound = name.getPath().indexOf(findMe);

                        // Check if the current plugin matches the string.
                        if (isFound != -1) {

                            // Return false for exclude, true to include.
                            if (pluginListMode) {
                                return false;
                            } else {
                                return true;
                            }
                        }
                    }
                }

                if (pluginListMode) {
                    return true;
                } else {
                    return false;
                }
            }
        };

        // Setup Source and destination DIR's.
        File pluginsFolder = new File("plugins");

        // Touch the folder to update the modified date.
        pluginsFolder.setLastModified(System.currentTimeMillis());


        String thisTempDestination;
        if (!splitBackup) {
             thisTempDestination = tempDestination.concat(backupName).concat(FILE_SEPARATOR).concat("plugins");
        } else {
            thisTempDestination = backupPath.concat(FILE_SEPARATOR).concat("plugins").concat(FILE_SEPARATOR).concat(backupName);
        }
        FileUtils.checkFolderAndCreate(new File(thisTempDestination));

        // Perform plugin backup.
            if (pluginList.size() > 0 && !pluginList.get(0).isEmpty()) {
                if (pluginListMode) {
                    LogUtils.sendLog(strings.getString("disabledplugins"));
                } else {
                    LogUtils.sendLog(strings.getString("enabledplugins"));
                }
                LogUtils.sendLog(pluginList.toString());
            }
        FileUtils.copyDirectory(pluginsFolder, new File(thisTempDestination), pluginsFileFilter, true);

        // Check if ZIP is required.
        if (splitBackup) {
            FileUtils.doCopyAndZIP(thisTempDestination, backupPath.concat(FILE_SEPARATOR).concat("plugins").concat(FILE_SEPARATOR).concat(backupName), shouldZIP, useTemp);
        }
    }
}