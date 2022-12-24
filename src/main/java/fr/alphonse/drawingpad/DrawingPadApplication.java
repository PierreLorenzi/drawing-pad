package fr.alphonse.drawingpad;

import fr.alphonse.drawingpad.document.Document;
import fr.alphonse.drawingpad.document.utils.DocumentUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DrawingPadApplication {

    private static int untitledDocumentIndex = 1;

    private static final List<Document> documents = new ArrayList<>();

    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "Drawing Pad");
        createNewDocument();
    }

    private static void createNewDocument() {
        String windowName = "Untitled " + untitledDocumentIndex++;
        var document = new Document(windowName);
        displayDocument(document);
    }

    private static void displayDocument(Document document) {
        documents.add(document);
        var menuBar = makeMenuBar(document);
        document.displayWindow(menuBar);
        document.addCloseListener(() -> documents.removeIf(d -> d == document));
    }

    private static JMenuBar makeMenuBar(Document document) {
        // Création du menu
        JMenuBar mb = new JMenuBar();

        JMenu m = new JMenu("File");  // Fichier
        mb.add(m);

        var newFileMenuItem = new JMenuItem("New Document");
        newFileMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        newFileMenuItem.addActionListener(event -> createNewDocument());
        m.add(newFileMenuItem);

        var menuItem = new JMenuItem("Open Document...");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        menuItem.addActionListener(event -> openDocument());
        m.add(menuItem);

        var saveMenuItem = new JMenuItem("Save");
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        saveMenuItem.addActionListener(event -> document.save());
        m.add(saveMenuItem);

        m.addSeparator();

        var closeMenuItem = new JMenuItem("Close");
        closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        closeMenuItem.addActionListener(event -> document.close());
        m.add(closeMenuItem);

        JMenu editMenu = new JMenu("Edit");
        mb.add(editMenu);

        var cancelMenuItem = new JMenuItem("Undo");
        cancelMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        cancelMenuItem.addActionListener(event -> document.undo());
        editMenu.add(cancelMenuItem);

        var redoMenuItem = new JMenuItem("Redo");
        redoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK));
        redoMenuItem.addActionListener(event -> document.redo());
        editMenu.add(redoMenuItem);

        return mb;
    }

    private static void openDocument() {
        Path path = DocumentUtils.chooseFile(null, JFileChooser.OPEN_DIALOG);
        if (path == null) {
            return;
        }
        try {
            Document document = new Document(path);
            displayDocument(document);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
