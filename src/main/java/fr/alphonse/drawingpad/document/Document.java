package fr.alphonse.drawingpad.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.DrawingJson;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.Graph;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.Link;
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
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Document {

    private Drawing model;

    private final ChangeDetector changeDetector;

    private final List<Drawing> previousModels = new ArrayList<>();

    private Integer previousModelIndex;

    private String windowName;

    private Path path;

    private JFrame frame;

    private DrawingComponent drawingComponent;

    private Consumer<JFrame> closeListener;

    private boolean wasModifiedSinceLastSave = false;

    public Document(String windowName) {
        this.model = Drawing.builder()
                .graph(Graph.builder()
                        .objects(new ArrayList<>())
                        .links(new ArrayList<>())
                        .build())
                .positions(new HashMap<>())
                .linkCenters(new HashMap<>())
                .build();
        this.changeDetector = new ChangeDetector(model);
        this.windowName = windowName;
        listenToChanges();
    }

    public Document(Path path) throws IOException {
        this.path = path;
        this.model = importFile(path);
        this.changeDetector = new ChangeDetector(model);
        listenToChanges();
    }

    private static Drawing importFile(Path path) throws IOException {
        DrawingJson json = new JsonMapper().readValue(path.toFile(), DrawingJson.class);
        return mapJsonToModel(json);
    }

    private static Drawing mapJsonToModel(DrawingJson json) throws IOException {

        Graph graph = json.getGraph();

        // resolve references
        fillLinkEnds(graph);

        // copy lists
        List<Object> objects = new ArrayList<>(graph.getObjects());
        List<Link> links = new ArrayList<>(graph.getLinks());
        Map<Object, Position> positions = json.getPositions().keySet().stream().collect(Collectors.toMap(id -> findObjectWithId(graph, id), json.getPositions()::get));
        Map<Link, Position> linkCenters = json.getLinkCenters().keySet().stream().collect(Collectors.toMap(id -> findLinkWithId(graph, id), json.getLinkCenters()::get));

        return Drawing.builder()
                .graph(Graph.builder()
                        .objects(objects)
                        .links(links)
                        .build())
                .positions(positions)
                .linkCenters(linkCenters)
                .build();
    }

    private static void fillLinkEnds(Graph graph) {
        for (Link link : graph.getLinks()) {
            link.setOriginElement(GraphHandler.findGraphElementAtReference(link.getOriginReference(), graph));
            link.setDestinationElement(GraphHandler.findGraphElementAtReference(link.getDestinationReference(), graph));
        }
    }

    private static Object findObjectWithId(Graph graph, int id) {
        return graph.getObjects().stream()
                .filter(object -> object.getId() == id)
                .findFirst().orElseThrow();
    }

    private static Link findLinkWithId(Graph graph, int id) {
        return graph.getLinks().stream()
                .filter(link -> link.getId() == id)
                .findFirst().orElseThrow();
    }

    private void listenToChanges() {
        this.previousModels.add(copyModel(model));
        changeDetector.addListener(this, Document::reactToChange);
    }

    private static Drawing copyModel(Drawing model) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            DrawingJson jsonContentInput = mapModelToJson(model);
            String jsonString = objectMapper.writeValueAsString(jsonContentInput);
            DrawingJson jsonContentOutput = objectMapper.readValue(jsonString, DrawingJson.class);
            return mapJsonToModel(jsonContentOutput);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void reactToChange() {
        if (previousModelIndex != null) {
            previousModels.subList(previousModelIndex+1, previousModels.size()).clear();
            previousModelIndex = null;
        }
        Drawing currentModel = copyModel(model);
        previousModels.add(currentModel);
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
        frame.add(new InfoComponent(drawingComponent.getSelection(), drawingComponent.getSelectionChangeDetector(), changeDetector), BorderLayout.EAST);
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
        changeModel(copyModel(previousModels.get(previousModelIndex)));
        if (previousModelIndex == 0) {
            changeModifiedFlag(false);
        }
    }

    private void changeModel(Drawing model) {
        this.model = model;
        this.changeDetector.reinitModel(model);
        drawingComponent.changeModel(model);
    }


    public void redo() {
        if (previousModelIndex == null) {
            return;
        }
        previousModelIndex += 1;
        changeModel(copyModel(previousModels.get(previousModelIndex)));
        if (previousModelIndex == previousModels.size()-1) {
            previousModelIndex = null;
        }
        changeModifiedFlag(true);
    }

    public void save() {
        changeModifiedFlag(false);
        previousModelIndex = null;
        previousModels.clear();
        previousModels.add(copyModel(model));

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
                .graph(model.getGraph())
                .positions(model.getPositions().keySet().stream()
                        .collect(Collectors.toMap(Object::getId,model.getPositions()::get)))
                .linkCenters(model.getLinkCenters().keySet().stream()
                        .collect(Collectors.toMap(Link::getId,model.getLinkCenters()::get)))
                .build();
    }

    public void close() {
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }
}
