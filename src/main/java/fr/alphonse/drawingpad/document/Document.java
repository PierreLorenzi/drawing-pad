package fr.alphonse.drawingpad.document;

import com.fasterxml.jackson.databind.json.JsonMapper;
import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.DrawingJson;
import fr.alphonse.drawingpad.data.model.GraphElement;
import fr.alphonse.drawingpad.document.utils.ChangeDetector;
import fr.alphonse.drawingpad.document.utils.DocumentUtils;
import fr.alphonse.drawingpad.document.utils.GraphHandler;
import fr.alphonse.drawingpad.view.DrawingComponent;
import fr.alphonse.drawingpad.view.InfoComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class Document {

    private final Drawing model;

    private final ChangeDetector<Drawing, DrawingJson> changeDetector;

    private final List<DrawingJson> previousModels = new ArrayList<>();

    private Integer previousModelIndex;

    private String windowName;

    private Path path;

    private JFrame frame;

    private DrawingComponent drawingComponent;

    private Consumer<JFrame> closeListener;

    private boolean wasModifiedSinceLastSave = false;

    public Document(String windowName) {
        this.model = GraphHandler.makeEmptyModel();
        this.changeDetector = new ChangeDetector<>(model, GraphHandler::mapModelToJson);
        this.windowName = windowName;
        listenToChanges();
    }

    public Document(Path path) throws IOException {
        this.path = path;
        this.model = importFile(path);
        this.changeDetector = new ChangeDetector<>(model, GraphHandler::mapModelToJson);
        listenToChanges();
    }

    private static Drawing importFile(Path path) throws IOException {
        JsonMapper jsonMapper = new JsonMapper();
        DrawingJson json = jsonMapper.readValue(path.toFile(), DrawingJson.class);
        return GraphHandler.mapJsonToModel(json);
    }

    private void listenToChanges() {
        registerCurrentStateForUndo();
        changeDetector.addListener(this, Document::reactToChange);
    }

    private void registerCurrentStateForUndo() {
        DrawingJson currentState = changeDetector.getCurrentState();
        previousModels.add(currentState);
    }

    private void reactToChange() {
        if (previousModelIndex != null) {
            previousModels.subList(previousModelIndex+1, previousModels.size()).clear();
            previousModelIndex = null;
        }
        registerCurrentStateForUndo();
        changeModifiedFlag(true);
    }

    private void changeModifiedFlag(boolean newValue) {
        if (newValue == wasModifiedSinceLastSave) {
            return;
        }
        wasModifiedSinceLastSave = newValue;
        var name = findWindowName();
        String newName = (wasModifiedSinceLastSave ? name + "*" : name);
        frame.setTitle(newName);
    }

    public void addCloseListener(Consumer<JFrame> callback) {
        this.closeListener = callback;
    }

    public void displayWindow(JMenuBar menuBar) {
        frame = new JFrame();
        frame.setJMenuBar(menuBar);

        windowName = findWindowName();
        frame.setTitle(windowName);

        frame.setLayout(new BorderLayout());

        drawingComponent = new DrawingComponent(model, changeDetector);
        drawingComponent.setBounds(0, 0, 500, 600);
        frame.add(drawingComponent, BorderLayout.CENTER);
        InfoComponent infoComponent = new InfoComponent(drawingComponent.getSelection(), drawingComponent.getSelectionChangeDetector(), changeDetector, model);
        frame.add(infoComponent, BorderLayout.EAST);
        frame.setSize(800, 600);
        frame.setJMenuBar(menuBar);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (previousModels.size() > 1 && !Objects.equals(previousModelIndex, 0)) {
                    int response = JOptionPane.showConfirmDialog(frame, "Do you want to save changes before closing?");
                    switch (response) {
                        case JOptionPane.CANCEL_OPTION, JOptionPane.CLOSED_OPTION:
                            return;
                        case JOptionPane.OK_OPTION:
                            Document.this.save();
                        default:
                            break;
                    }
                }
                if (Document.this.closeListener != null) {
                    Document.this.closeListener.accept(frame);
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }
        });

        frame.setVisible(true); // making the frame visible
    }

    private String findWindowName() {
        if (path != null) {
            return DocumentUtils.findPathWindowName(path);
        }
        if (windowName != null) {
            return windowName;
        }
        return "";
    }

    public void moveToFront() {
        frame.toFront();
    }

    public void delete() {
        this.drawingComponent.delete();
    }

    public void selectAll() {
        this.drawingComponent.selectAll();
    }

    public Drawing copy() {
        List<GraphElement> selection = drawingComponent.getSelection();
        if (selection.isEmpty()) {
            return null;
        }
        Drawing copiedModel = GraphHandler.extractModelWithElements(model, selection);
        if (copiedModel == null) {
            // the user has selected elements but not their dependencies, so warn
            Toolkit.getDefaultToolkit().beep();
            return null;
        }
        return copiedModel;
    }

    public void paste(Drawing clipboard) {
        this.drawingComponent.paste(clipboard);
    }

    public void undo() {
        if (previousModelIndex == null && previousModels.size() == 1) {
            return;
        }
        if (previousModelIndex == null) {
            previousModelIndex = previousModels.size() - 1;
        }
        if (previousModelIndex == 0) {
            return;
        }
        previousModelIndex -= 1;
        changeModel(previousModels.get(previousModelIndex));
        if (previousModelIndex == 0) {
            changeModifiedFlag(false);
        }
    }

    private void changeModel(DrawingJson json) {
        GraphHandler.clearModel(this.model);
        GraphHandler.fillModelWithJson(this.model, json);
        this.changeDetector.notifyChangeCausedBy(this);
    }

    public void redo() {
        if (previousModelIndex == null) {
            return;
        }
        previousModelIndex += 1;
        changeModel(previousModels.get(previousModelIndex));
        if (previousModelIndex == previousModels.size()-1) {
            previousModelIndex = null;
        }
        changeModifiedFlag(true);
    }

    public void save() {
        changeModifiedFlag(false);
        previousModelIndex = null;
        previousModels.clear();
        registerCurrentStateForUndo();

        if (this.path != null) {
            writeFile();
            return;
        }

        Path savePath = DocumentUtils.chooseFile(this.frame, JFileChooser.SAVE_DIALOG);
        if (savePath == null) {
            return;
        }

        this.path = savePath;
        this.frame.setTitle(findWindowName());

        writeFile();
    }

    public void writeFile() {
        DrawingJson json = GraphHandler.mapModelToJson(model);
        try {
            new JsonMapper().writeValue(path.toFile(), json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }
}
