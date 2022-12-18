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

    public static void main(String[] args) {
        openNewDocument();
    }

    private static void openNewDocument() {
        openDocumentWithContent(new Document());
    }

    private static void chooseDocumentToOpen(JFrame frame) {
        var chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "JSON", "json");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(Path.of("/Users/lorenzi/Pierre/IA/Documents").toFile());
        int returnVal = chooser.showOpenDialog(frame);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            var file = chooser.getSelectedFile();
            openDocument(file.toPath());
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

        // Création du menu
        JMenuBar mb = new JMenuBar();
        JMenu m = new JMenu("File");  // Fichier
        mb.add(m);

        var newFileMenuItem = new JMenuItem("New...");
        newFileMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        newFileMenuItem.addActionListener(event -> openNewDocument());
        m.add(newFileMenuItem);

        var menuItem = new JMenuItem("Open...");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        menuItem.addActionListener(event -> chooseDocumentToOpen(frame));
        m.add(menuItem);

        DrawingComponent drawingComponent = new DrawingComponent();
        drawingComponent.setBounds(0, 0, 500, 600);
        drawingComponent.setModel(document.getExample());
        frame.add(drawingComponent); // adding button in JFrame
        frame.setSize(500, 600); // 400 width and 500 height
        frame.setLayout(null); // using no layout managers
        frame.setJMenuBar(mb);
        frame.setVisible(true); // making the frame visible

    }
}
