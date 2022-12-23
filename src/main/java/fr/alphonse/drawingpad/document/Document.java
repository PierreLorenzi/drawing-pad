package fr.alphonse.drawingpad.document;

import com.fasterxml.jackson.databind.json.JsonMapper;
import fr.alphonse.drawingpad.data.Example;
import fr.alphonse.drawingpad.data.ExampleJson;
import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.Vertex;
import fr.alphonse.drawingpad.document.utils.DocumentUtils;
import fr.alphonse.drawingpad.view.DrawingComponent;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Document {

    private final Example model = new Example();

    private String windowName;

    private Path path;

    private JFrame frame;

    private Runnable closeListener;

    private static final String FILE_ICON = "\uD83D\uDCC4";

    public Document(String windowName) {
        this.windowName = windowName;
    }

    public Document(Path path) throws IOException {
        this.path = path;
        importFile(path, model);
    }

    private static void importFile(Path path, Example example) throws IOException {
        ExampleJson json = new JsonMapper().readValue(path.toFile(), ExampleJson.class);

        // correct links
        Map<Vertex.Id, Vertex> vertexMap = Stream.concat(json.getObjects().stream(), json.getLinks().stream()).collect(Collectors.toMap(Vertex::getId, Function.identity()));
        for (Link link: json.getLinks()) {
            link.setOriginId(vertexMap.get(link.getOriginId()).getId());
            link.setDestinationId(vertexMap.get(link.getDestinationId()).getId());
        }

        example.setObjects(json.getObjects().stream().collect(Collectors.toMap(Object::getId, Function.identity())));
        example.setLinks(json.getLinks().stream().collect(Collectors.toMap(Link::getId, Function.identity())));
        example.setPositions(json.getPositions().entrySet().stream().collect(Collectors.toMap(entry -> json.getObjects().stream().map(Object::getId).filter(id -> id.getString().equals(entry.getKey().getString())).findFirst().get(), Map.Entry::getValue)));
    }

    public void addCloseListener(Runnable callback) {
        this.closeListener = callback;
    }

    public void displayWindow(JMenuBar menuBar) {
        frame = new JFrame();
        frame.setJMenuBar(menuBar);

        var name = findWindowName();
        frame.setTitle(name);

        DrawingComponent drawingComponent = new DrawingComponent();
        drawingComponent.setBounds(0, 0, 500, 600);
        drawingComponent.setModel(model);
        frame.add(drawingComponent); // adding button in JFrame
        frame.setSize(500, 600); // 400 width and 500 height
        frame.setLayout(null); // using no layout managers
        frame.setJMenuBar(menuBar);

        frame.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (Document.this.closeListener != null) {
                    Document.this.closeListener.run();
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
            return findPathWindowName(path);
        }
        if (windowName != null) {
            return windowName;
        }
        return "";
    }

    private String findPathWindowName(Path path) {

        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            return FILE_ICON + " " + fileName.substring(0, dotIndex);
        }
        return FILE_ICON + " " + fileName;
    }

    public void save() {
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
        ExampleJson json = ExampleJson.builder()
                .objects(model.getObjects().values().stream().toList())
                .links(model.getLinks().values().stream().toList())
                .positions(model.getPositions())
                .build();
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
