package fr.alphonse.drawingpad.document.utils;

import lombok.experimental.UtilityClass;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Path;

@UtilityClass
public class DocumentUtils {

    public static Path chooseFile(JFrame frame, int mode) {
        var chooser = new JFileChooser();
        chooser.setFileSelectionMode(mode);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "JSON", "json");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(Path.of("/Users/lorenzi/Pierre/IA/Documents").toFile());
        int returnVal = ((mode & JFileChooser.SAVE_DIALOG) != 0) ? chooser.showSaveDialog(frame) : chooser.showOpenDialog(frame);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            var file = chooser.getSelectedFile();
            return file.toPath();
        }
        else {
            return null;
        }
    }
}
