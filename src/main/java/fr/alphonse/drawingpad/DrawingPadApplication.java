package fr.alphonse.drawingpad;

import fr.alphonse.drawingpad.data.Document;
import fr.alphonse.drawingpad.view.DrawingComponent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;

public class DrawingPadApplication {

    private static int untitledDocumentIndex = 1;

    private static final String FILE_ICON = "\uD83D\uDCC4";

    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "Drawing Pad");
        openNewDocument();
    }

    private static void openNewDocument() {
        openDocumentWithContent(new Document());
    }

    private static void chooseDocumentToOpen(JFrame frame) {
        var path = chooseDocument(frame, JFileChooser.OPEN_DIALOG);
        if (path == null) {
            return;
        }
        openDocument(path);
    }

    private static Path chooseDocument(JFrame frame, int mode) {
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

    private static void openDocument(Path path) {
        try {
            openDocumentWithContent(new Document(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void openDocumentWithContent(Document document) {
        JFrame frame = new JFrame();
        frame.setTitle(findDocumentTitle(document));

        // Création du menu
        JMenuBar mb = new JMenuBar();
        JMenu m = new JMenu("File");  // Fichier
        mb.add(m);

        var newFileMenuItem = new JMenuItem("New Document");
        newFileMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        newFileMenuItem.addActionListener(event -> openNewDocument());
        m.add(newFileMenuItem);

        var menuItem = new JMenuItem("Open Document...");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        menuItem.addActionListener(event -> chooseDocumentToOpen(frame));
        m.add(menuItem);

        var saveMenuItem = new JMenuItem("Save");
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        saveMenuItem.addActionListener(event -> {
            if (document.getPath() == null) {
                Path path = chooseDocument(frame, JFileChooser.SAVE_DIALOG);
                if (path == null) {
                    return;
                }
                document.setPath(path);
            }
            try {
                document.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        m.add(saveMenuItem);

        m.addSeparator();

        var closeMenuItem = new JMenuItem("Close");
        closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        closeMenuItem.addActionListener(event -> frame.setVisible(false));
        m.add(closeMenuItem);

        DrawingComponent drawingComponent = new DrawingComponent();
        drawingComponent.setBounds(0, 0, 500, 600);
        drawingComponent.setModel(document.getExample());
        frame.add(drawingComponent); // adding button in JFrame
        frame.setSize(500, 600); // 400 width and 500 height
        frame.setLayout(null); // using no layout managers
        frame.setJMenuBar(mb);
        frame.setVisible(true); // making the frame visible
    }

    private static String findDocumentTitle(Document document) {
        Path path = document.getPath();
        if (path == null) {
            return "Untitled " + untitledDocumentIndex++;
        }
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            return FILE_ICON + " " + fileName.substring(0, dotIndex);
        }
        return FILE_ICON + " " + fileName;
    }
}
