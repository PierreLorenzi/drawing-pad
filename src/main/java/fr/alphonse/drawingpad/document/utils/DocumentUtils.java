package fr.alphonse.drawingpad.document.utils;

import lombok.experimental.UtilityClass;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

@UtilityClass
public class DocumentUtils {

    private static final String APPLICATION_PREFERENCE_KEY = "drawing-pad";

    private static final String OPEN_CLOSE_DIALOG_KEY =  "open-close-dialog";

    private static final String DIALOG_PATH_KEY =  "path";

    private static final int RECENT_FILE_SIZE = 10;

    private static final String RECENT_FILE_KEY =  "recent-files";

    private static final String FILE_ICON = "\uD83D\uDCC4";

    public static Path chooseFile(JFrame frame, int mode) {
        var chooser = new JFileChooser();
        chooser.setFileSelectionMode(mode);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "JSON", "json");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(findFileDialogPath().toFile());
        int returnVal = ((mode & JFileChooser.SAVE_DIALOG) != 0) ? chooser.showSaveDialog(frame) : chooser.showOpenDialog(frame);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            var file = chooser.getSelectedFile();
            Path path = file.toPath();
            saveDialogPath(path);
            return path;
        }
        else {
            return null;
        }
    }

    public static List<Path> chooseFiles(JFrame frame, int mode) {
        var chooser = new JFileChooser();
        chooser.setFileSelectionMode(mode);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "JSON", "json");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(findFileDialogPath().toFile());
        chooser.setMultiSelectionEnabled(true);
        int returnVal = ((mode & JFileChooser.SAVE_DIALOG) != 0) ? chooser.showSaveDialog(frame) : chooser.showOpenDialog(frame);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            saveDialogPath(files[0].toPath());
            return Arrays.stream(files)
                    .map(File::toPath)
                    .toList();
        }
        else {
            return List.of();
        }
    }

    private static Path findFileDialogPath() {
        Preferences preferences = findOpenCloseDialogPreferences();
        String userHome = System.getProperty("user.home");
        String stringPath = preferences.get(DIALOG_PATH_KEY, userHome);
        return Path.of(stringPath);
    }

    private static Preferences findOpenCloseDialogPreferences() {
        Preferences preferences = findApplicationPreferences();
        return preferences.node(OPEN_CLOSE_DIALOG_KEY);
    }

    private static Preferences findApplicationPreferences() {
        Preferences preferences = Preferences.userRoot();
        return preferences.node(APPLICATION_PREFERENCE_KEY);
    }

    private static void saveDialogPath(Path path) {
        Preferences preferences = findOpenCloseDialogPreferences();
        preferences.put(DIALOG_PATH_KEY, path.getParent().toString());
    }

    public static void addToRecentFiles(Path path) {
        Preferences preferences = findRecentFilePreferences();
        Integer index = findRecentFileIndex(path, preferences);
        if (index != null) {
            if (index == 1) {
                return;
            }
            swapWithFirstRecentFile(index, preferences);
            return;
        }
        insertAtStartOfRecentFiles(path, preferences);
    }

    private static Preferences findRecentFilePreferences() {
        Preferences preferences = findOpenCloseDialogPreferences();
        return preferences.node(RECENT_FILE_KEY);
    }

    private static Integer findRecentFileIndex(Path path, Preferences preferences) {
        String pathString = path.toString();
        for (int i=1 ; i<=RECENT_FILE_SIZE ; i++) {
            String key = "" + i;
            String recentFile = preferences.get(key, null);
            if (recentFile == null) {
                break;
            }
            if (recentFile.equals(pathString)) {
                return i;
            }
        }
        return null;
    }

    private static void swapWithFirstRecentFile(int index, Preferences preferences) {
        String key1 = "1";
        String key2 = "" + index;
        String value1 = preferences.get(key1, null);
        String value2 = preferences.get(key2, null);
        preferences.put(key1, value2);
        preferences.put(key2, value1);
    }

    private static void insertAtStartOfRecentFiles(Path path, Preferences preferences) {
        shiftRecentFiles(preferences);
        preferences.put("1", path.toString());
    }

    private static void shiftRecentFiles(Preferences preferences) {
        String previousValue = preferences.get("1", null);
        for (int i=2; i<=RECENT_FILE_SIZE ; i++) {
            if (previousValue == null) {
                return;
            }
            String currentKey = "" + i;
            String currentValue = preferences.get(currentKey, null);
            preferences.put(currentKey, previousValue);
            previousValue = currentValue;
        }
    }

    public List<Path> findRecentFiles() {
        Preferences preferences = findRecentFilePreferences();
        List<Path> recentFiles = new ArrayList<>(RECENT_FILE_SIZE);
        for (int i=1 ; i <= RECENT_FILE_SIZE ; i++) {
            String key = "" + i;
            String stringValue = preferences.get(key, null);
            if (stringValue == null) {
                break;
            }
            Path path = Path.of(stringValue);
            recentFiles.add(path);
        }
        return recentFiles;
    }

    public String findPathWindowName(Path path) {

        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            return FILE_ICON + " " + fileName.substring(0, dotIndex);
        }
        return FILE_ICON + " " + fileName;
    }

    public void clearRecentFiles() {
        Preferences preferences = findRecentFilePreferences();
        for (int i=1 ; i<=RECENT_FILE_SIZE ; i++) {
            preferences.remove("" + i);
        }
    }
}
