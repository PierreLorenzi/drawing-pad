package fr.alphonse.drawingpad.document;

import com.fasterxml.jackson.databind.json.JsonMapper;
import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.DrawingJson;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.document.utils.ChangeDetector;
import fr.alphonse.drawingpad.document.utils.DocumentUtils;
import fr.alphonse.drawingpad.document.utils.GraphHandler;
import fr.alphonse.drawingpad.document.utils.ModelStateManager;
import fr.alphonse.drawingpad.view.DrawingComponent;
import fr.alphonse.drawingpad.view.InfoComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
        this.model = makeEmptyModel();
        this.changeDetector = new ChangeDetector<>(model, Document::mapModelToJson);
        this.windowName = windowName;
        listenToChanges();
    }

    private static Drawing makeEmptyModel() {
        return Drawing.builder()
                .graph(Graph.builder()
                        .objects(new ArrayList<>())
                        .completions(new ArrayList<>())
                        .quantities(new ArrayList<>())
                        .links(new ArrayList<>())
                        .build())
                .positions(new HashMap<>())
                .completionPositions(new HashMap<>())
                .quantityPositions(new HashMap<>())
                .linkCenters(new HashMap<>())
                .note("")
                .build();
    }

    public Document(Path path) throws IOException {
        this.path = path;
        this.model = importFile(path);
        this.changeDetector = new ChangeDetector<>(model, Document::mapModelToJson);
        listenToChanges();
    }

    private static Drawing importFile(Path path) throws IOException {
        DrawingJson json = new JsonMapper().readValue(path.toFile(), DrawingJson.class);
        return mapJsonToModel(json);
    }

    private static Drawing mapJsonToModel(DrawingJson json) {
        Drawing model = makeEmptyModel();
        fillModelWithJson(model, json);
        return model;
    }

    private static void fillModelWithJson(Drawing model, DrawingJson json) {

        Graph jsonGraph = json.getGraph();
        Graph newGraph = ModelStateManager.deepCopy(jsonGraph, Graph.class);

        // resolve references
        fillLinkOutlets(newGraph);
        fillVertices(newGraph);

        model.setGraph(newGraph);

        Map<Object, Position> positions = json.getPositions().keySet().stream().collect(Collectors.toMap(id -> GraphHandler.findGraphElementWithId(newGraph.getObjects(), id), json.getPositions()::get));
        Map<Completion, Position> completionPositions = json.getCompletionPositions().keySet().stream().collect(Collectors.toMap(id -> GraphHandler.findGraphElementWithId(newGraph.getCompletions(), id), json.getCompletionPositions()::get));
        Map<Quantity, Position> quantityPositions = json.getQuantityPositions().keySet().stream().collect(Collectors.toMap(id -> GraphHandler.findGraphElementWithId(newGraph.getQuantities(), id), json.getQuantityPositions()::get));
        Map<Link, Position> linkCenters = json.getLinkCenters().keySet().stream().collect(Collectors.toMap(id -> GraphHandler.findGraphElementWithId(newGraph.getLinks(), id), json.getLinkCenters()::get));

        model.getPositions().putAll(positions);
        model.getCompletionPositions().putAll(completionPositions);
        model.getQuantityPositions().putAll(quantityPositions);
        model.getLinkCenters().putAll(linkCenters);

        model.setNote(json.getNote());
    }

    private static void fillLinkOutlets(Graph graph) {
        for (Link link : graph.getLinks()) {
            link.setDirectFactor(DirectFactor.builder()
                    .link(link)
                    .build());
            link.setReverseFactor(ReverseFactor.builder()
                    .link(link)
                    .build());
        }
    }

    private static void fillVertices(Graph graph) {
        for (Link link : graph.getLinks()) {
            link.setOrigin(GraphHandler.findVertexAtReference(link.getOriginReference(), graph));
            link.setDestination(GraphHandler.findVertexAtReference(link.getDestinationReference(), graph));
        }
        for (Completion completion: graph.getCompletions()) {
            completion.setBase(GraphHandler.findVertexAtReference(completion.getBaseReference(), graph));
        }
        for (Quantity quantity: graph.getQuantities()) {
            quantity.setBase(GraphHandler.findVertexAtReference(quantity.getBaseReference(), graph));
        }
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
        String displayName = (wasModifiedSinceLastSave ? windowName + "*" : windowName);
        frame.setTitle(displayName);
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

    public void duplicate() {
        this.drawingComponent.duplicate();
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
        clearModel(this.model);
        fillModelWithJson(this.model, json);
        this.changeDetector.notifyChangeCausedBy(this);
    }

    private void clearModel(Drawing drawing) {
        drawing.setGraph(null);

        drawing.getPositions().clear();
        drawing.getCompletionPositions().clear();
        drawing.getQuantityPositions().clear();
        drawing.getLinkCenters().clear();

        drawing.setNote("");
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
        DrawingJson json = mapModelToJson(model);
        try {
            new JsonMapper().writeValue(path.toFile(), json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static DrawingJson mapModelToJson(Drawing model) {
        return DrawingJson.builder()
                .graph(ModelStateManager.deepCopy(model.getGraph(), Graph.class))
                .positions(model.getPositions().keySet().stream()
                        .collect(Collectors.toMap(Object::getId,model.getPositions()::get)))
                .completionPositions(model.getCompletionPositions().keySet().stream()
                        .collect(Collectors.toMap(Completion::getId,model.getCompletionPositions()::get)))
                .quantityPositions(model.getQuantityPositions().keySet().stream()
                        .collect(Collectors.toMap(Quantity::getId,model.getQuantityPositions()::get)))
                .linkCenters(model.getLinkCenters().keySet().stream()
                        .collect(Collectors.toMap(Link::getId,model.getLinkCenters()::get)))
                .note(model.getNote())
                .build();
    }

    public void close() {
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }
}
