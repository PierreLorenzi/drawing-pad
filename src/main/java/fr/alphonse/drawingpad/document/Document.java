package fr.alphonse.drawingpad.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.DrawingJson;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.document.utils.ChangeDetector;
import fr.alphonse.drawingpad.document.utils.DocumentUtils;
import fr.alphonse.drawingpad.view.DrawingComponent;
import fr.alphonse.drawingpad.view.InfoComponent;
import fr.alphonse.drawingpad.view.internal.ModelHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                        .amounts(new ArrayList<>())
                        .definitions(new ArrayList<>())
                        .build())
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

    private static Drawing importFile(Path path) throws IOException {
        DrawingJson json = new JsonMapper().readValue(path.toFile(), DrawingJson.class);
        return mapJsonToModel(json);
    }

    private static Drawing mapJsonToModel(DrawingJson json) throws IOException {

        // fill value ids
        fillValueIds(json);

        // correct links
        Map<Vertex.Id, Vertex> vertexMap = mapVertexIds(json);
        for (Link link: json.getGraph().getLinks()) {
            link.setOriginId(vertexMap.get(link.getOriginId()).getId());
            link.setDestinationId(vertexMap.get(link.getDestinationId()).getId());
        }

        // correct amounts
        for (Amount amount: json.getGraph().getAmounts()) {
            amount.setModelId(vertexMap.get(amount.getModelId()).getId());
        }

        // correct definitions
        for (Definition definition: json.getGraph().getDefinitions()) {
            definition.setBaseId(vertexMap.get(definition.getBaseId()).getId());
        }

        List<Object> objects = new ArrayList<>(json.getGraph().getObjects());
        List<Link> links = new ArrayList<>(json.getGraph().getLinks());
        List<Amount> amounts = new ArrayList<>(json.getGraph().getAmounts());
        List<Definition> definitions = new ArrayList<>(json.getGraph().getDefinitions());
        Map<Object, Position> positions = json.getPositions().keySet().stream().collect(Collectors.toMap(id -> (Object)vertexMap.get(id), json.getPositions()::get));

        return Drawing.builder()
                .graph(Graph.builder()
                        .objects(objects)
                        .links(links)
                        .amounts(amounts)
                        .definitions(definitions)
                        .build())
                .positions(positions)
                .build();
    }

    private static void fillValueIds(DrawingJson json) {
        Graph graph = json.getGraph();

        for (Link link: graph.getLinks()) {

            LowerValue originFactor = link.getOriginFactor();
            int originFactorIdValue = ModelHandler.makeSameIdWithOtherMask(link.getId(), LowerValue.Id.LINK_ORIGIN_FACTOR_MASK);
            originFactor.setId(new LowerValue.Id(originFactorIdValue, originFactor));

            LowerValue destinationFactor = link.getDestinationFactor();
            int destinationFactorIdValue = ModelHandler.makeSameIdWithOtherMask(link.getId(), LowerValue.Id.LINK_DESTINATION_FACTOR_MASK);
            destinationFactor.setId(new LowerValue.Id(destinationFactorIdValue, destinationFactor));
        }

        for (Amount amount: graph.getAmounts()) {

            WholeValue amountCount = amount.getCount();
            int amountCountIdValue = ModelHandler.makeSameIdWithOtherMask(amount.getId(), WholeValue.Id.AMOUNT_COUNT_MASK);
            amountCount.setId(new WholeValue.Id(amountCountIdValue, amountCount));

            WholeValue amountDistinctCount = amount.getDistinctCount();
            int amountDistinctCountIdValue = ModelHandler.makeSameIdWithOtherMask(amount.getId(), WholeValue.Id.AMOUNT_DISTINCT_COUNT_MASK);
            amountDistinctCount.setId(new WholeValue.Id(amountDistinctCountIdValue, amountDistinctCount));
        }

        for (Definition definition: graph.getDefinitions()) {

            LowerValue definitionCompleteness = definition.getCompleteness();
            int definitionCompletenessIdValue = ModelHandler.makeSameIdWithOtherMask(definition.getId(), LowerValue.Id.DEFINITION_COMPLETENESS_MASK);
            definitionCompleteness.setId(new LowerValue.Id(definitionCompletenessIdValue, definitionCompleteness));
        }
    }

    private static Map<Vertex.Id, Vertex> mapVertexIds(DrawingJson json) {
        Graph graph = json.getGraph();
        Stream<? extends Vertex> vertexStream = graph.getObjects().stream();
        vertexStream = Stream.concat(vertexStream, graph.getLinks().stream());
        vertexStream = Stream.concat(vertexStream, graph.getAmounts().stream());
        vertexStream = Stream.concat(vertexStream, graph.getDefinitions().stream());
        vertexStream = Stream.concat(vertexStream, graph.getLinks().stream().map(Link::getOriginFactor));
        vertexStream = Stream.concat(vertexStream, graph.getLinks().stream().map(Link::getDestinationFactor));
        vertexStream = Stream.concat(vertexStream, graph.getAmounts().stream().map(Amount::getCount));
        vertexStream = Stream.concat(vertexStream, graph.getAmounts().stream().map(Amount::getDistinctCount));
        vertexStream = Stream.concat(vertexStream, graph.getDefinitions().stream().map(Definition::getCompleteness));
        return vertexStream.collect(Collectors.toMap(Vertex::getId, Function.identity()));
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
                .build();
    }

    public void close() {
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }
}
