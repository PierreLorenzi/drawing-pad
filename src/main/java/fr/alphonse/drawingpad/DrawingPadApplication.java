package fr.alphonse.drawingpad;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.document.Document;
import fr.alphonse.drawingpad.document.utils.DocumentUtils;
import fr.alphonse.drawingpad.view.internal.linkresize.LinkResizeModification;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DrawingPadApplication {

    private static int untitledDocumentIndex = 1;

    private static final List<DocumentRecord> documents = new ArrayList<>();

    private static final List<SoftReference<JMenu>> recentFileMenus = new ArrayList<>();

    private static JFrame ghostFrame = null;

    private static Drawing clipboard;

    private record DocumentRecord(Document document, Path path) {}

    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "Drawing Pad");
        createNewDocument();
    }

    private static void createNewDocument() {
        String windowName = "Untitled " + untitledDocumentIndex++;
        var document = new Document(windowName);
        displayDocument(document, null);
    }

    private static void displayDocument(Document document, Path path) {
        documents.add(new DocumentRecord(document, path));
        var menuBar = makeMenuBar(document);
        document.displayWindow(menuBar);
        document.addCloseListener(frame -> closeFrame(frame, document));
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

        JMenu recentFileMenu = new JMenu("Recent Files");
        recentFileMenus.add(new SoftReference<>(recentFileMenu));
        refreshRecentFiles();
        m.add(recentFileMenu);

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

        editMenu.addSeparator();

        var selectAllMenuItem = new JMenuItem("Select All");
        selectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        selectAllMenuItem.addActionListener(event -> document.selectAll());
        editMenu.add(selectAllMenuItem);

        editMenu.addSeparator();

        var copyMenuItem = new JMenuItem("Copy");
        copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        copyMenuItem.addActionListener(event -> copy(document) );
        editMenu.add(copyMenuItem);

        var pasteMenuItem = new JMenuItem("Paste");
        pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        pasteMenuItem.addActionListener(event -> paste(document) );
        editMenu.add(pasteMenuItem);

        var deleteMenuItem = new JMenuItem("Delete");
        deleteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
        deleteMenuItem.addActionListener(event -> document.delete());
        editMenu.add(deleteMenuItem);

        JMenu resizeMenu = new JMenu("Resize");
        mb.add(resizeMenu);

        var decrementWidthMenuItem = new JMenuItem("Decrease Width");
        decrementWidthMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        decrementWidthMenuItem.addActionListener(event -> document.resizeLinks(LinkResizeModification.DECREMENT_WIDTH));
        resizeMenu.add(decrementWidthMenuItem);

        var incrementWidthMenuItem = new JMenuItem("Increase Width");
        incrementWidthMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        incrementWidthMenuItem.addActionListener(event -> document.resizeLinks(LinkResizeModification.INCREMENT_WIDTH));
        resizeMenu.add(incrementWidthMenuItem);

        var decrementHeightMenuItem = new JMenuItem("Decrease Height");
        decrementHeightMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        decrementHeightMenuItem.addActionListener(event -> document.resizeLinks(LinkResizeModification.DECREMENT_HEIGHT));
        resizeMenu.add(decrementHeightMenuItem);

        var incrementHeightMenuItem = new JMenuItem("Increase Height");
        incrementHeightMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        incrementHeightMenuItem.addActionListener(event -> document.resizeLinks(LinkResizeModification.INCREMENT_HEIGHT));
        resizeMenu.add(incrementHeightMenuItem);

        return mb;
    }

    private static void copy(Document document) {
        Drawing copy = document.copy();
        if (copy == null) {
            return;
        }
        clipboard = copy;
    }

    private static void paste(Document document) {
        if (clipboard == null) {
            return;
        }
        document.paste(clipboard);
    }

    private static void openDocument() {
        List<Path> paths = DocumentUtils.chooseFiles(null, JFileChooser.OPEN_DIALOG);
        for (Path path: paths) {
            openDocumentAtPath(path);
        }
    }

    private static void openDocumentAtPath(Path path) {
        Document openedDocument = findOpenedDocumentWithPath(path);
        if (openedDocument != null) {
            openedDocument.moveToFront();
            return;
        }
        try {
            Document document = new Document(path);
            displayDocument(document, path);
            DocumentUtils.addToRecentFiles(path);
            refreshRecentFiles();
            disposeGhostFrameIfNecessary();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Document findOpenedDocumentWithPath(Path path) {
        return documents.stream()
                .filter(record -> path.equals(record.path))
                .map(DocumentRecord::document)
                .findFirst().orElse(null);
    }

    private static void refreshRecentFiles() {
        recentFileMenus.removeIf(reference -> reference.get() == null);

        List<Path> recentFiles = DocumentUtils.findRecentFiles();

        for (SoftReference<JMenu> menuReference: recentFileMenus) {
            JMenu menu = menuReference.get();
            if (menu == null) {
                continue;
            }
            menu.removeAll();
            for (Path path: recentFiles) {
                String name = DocumentUtils.findPathWindowName(path);
                var menuItem = new JMenuItem(name);
                menuItem.addActionListener(event -> openDocumentAtPath(path));
                menu.add(menuItem);
            }

            menu.addSeparator();
            var clearMenuItem = new JMenuItem("Clear menu");
            clearMenuItem.setEnabled(!recentFiles.isEmpty());
            clearMenuItem.addActionListener(event -> clearRecentFiles());
            menu.add(clearMenuItem);
        }
    }

    private static void clearRecentFiles() {
        DocumentUtils.clearRecentFiles();
        refreshRecentFiles();
    }

    private static void disposeGhostFrameIfNecessary() {
        if (ghostFrame == null) {
            return;
        }
        ghostFrame.dispose();
        ghostFrame = null;
    }

    private static void closeFrame(JFrame frame, Document document) {
        documents.removeIf(d -> d.document == document);

        if (documents.isEmpty()) {
            frame.setVisible(false);
            ghostFrame = frame;
            return;
        }

        frame.dispose();
    }
}
