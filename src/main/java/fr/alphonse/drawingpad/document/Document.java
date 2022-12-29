package fr.alphonse.drawingpad.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import fr.alphonse.drawingpad.data.Example;
import fr.alphonse.drawingpad.data.ExampleJson;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.Vertex;
import fr.alphonse.drawingpad.document.utils.ChangeDetector;
import fr.alphonse.drawingpad.document.utils.DocumentUtils;
import fr.alphonse.drawingpad.view.DrawingComponent;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Document {

    private Example model;

    private final ChangeDetector changeDetector;

    private final List<Example> previousModels = new ArrayList<>();

    private Integer previousModelIndex;

    private String windowName;

    private Path path;

    private JFrame frame;

    private DrawingComponent drawingComponent;

    private Consumer<JFrame> closeListener;

    private boolean wasModifiedSinceLastSave = false;

    public Document(String windowName) {
        this.model = Example.builder()
                .objects(new HashMap<>())
                .links(new HashMap<>())
                .positions(new HashMap<>())
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

    private static Example importFile(Path path) throws IOException {
        ExampleJson json = new JsonMapper().readValue(path.toFile(), ExampleJson.class);
        return mapJsonToModel(json);
    }

    private static Example mapJsonToModel(ExampleJson json) throws IOException {

        // correct links
        Map<Vertex.Id, Vertex> vertexMap = Stream.concat(json.getObjects().stream(), json.getLinks().stream()).collect(Collectors.toMap(Vertex::getId, Function.identity()));
        for (Link link: json.getLinks()) {
            link.setOriginId(vertexMap.get(link.getOriginId()).getId());
            link.setDestinationId(vertexMap.get(link.getDestinationId()).getId());
        }

        Map<Object.Id, Object> objects = json.getObjects().stream().collect(Collectors.toMap(Object::getId, Function.identity()));
        Map<Link.Id, Link> links = json.getLinks().stream().collect(Collectors.toMap(Link::getId, Function.identity()));
        Map<Object.Id, Position> positions = json.getPositions().entrySet().stream().collect(Collectors.toMap(entry -> json.getObjects().stream().map(Object::getId).filter(id -> id.getString().equals(entry.getKey().getString())).findFirst().get(), Map.Entry::getValue));

        return Example.builder()
                .objects(objects)
                .links(links)
                .positions(positions)
                .build();
    }

    private void listenToChanges() {
        this.previousModels.add(copyModel(model));
        var thisReference = new SoftReference<>(this);
        changeDetector.addListener(() -> {
            var thisDocument = thisReference.get();
            if (thisDocument == null) {
                return false;
            }
            thisDocument.reactToChange();
            return true;
        });
    }

    private static Example copyModel(Example model) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            ExampleJson jsonContentInput = mapModelToJson(model);
            String jsonString = objectMapper.writeValueAsString(jsonContentInput);
            ExampleJson jsonContentOutput = objectMapper.readValue(jsonString, ExampleJson.class);
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
        Example currentModel = copyModel(model);
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

        drawingComponent = new DrawingComponent(model, changeDetector);
        drawingComponent.setBounds(0, 0, 500, 600);
        frame.add(drawingComponent); // adding button in JFrame
        frame.setSize(500, 600); // 400 width and 500 height
        frame.setLayout(null); // using no layout managers
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

    private void changeModel(Example model) {
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
        ExampleJson json = mapModelToJson(model);
        try {
            new JsonMapper().writeValue(path.toFile(), json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ExampleJson mapModelToJson(Example model) {
        return ExampleJson.builder()
                .objects(model.getObjects().values().stream().toList())
                .links(model.getLinks().values().stream().toList())
                .positions(model.getPositions())
                .build();
    }

    public void close() {
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }
}
