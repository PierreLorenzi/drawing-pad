package fr.alphonse.drawingpad.document;

import com.fasterxml.jackson.databind.json.JsonMapper;
import fr.alphonse.drawingpad.data.Example;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.json.ExampleJson;
import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.Vertex;
import fr.alphonse.drawingpad.document.utils.DocumentUtils;
import fr.alphonse.drawingpad.document.utils.JsonDataMapper;
import fr.alphonse.drawingpad.view.DrawingComponent;
import org.mapstruct.factory.Mappers;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        JsonDataMapper mapper = Mappers.getMapper(JsonDataMapper.class);
        List<Object> objects = mapper.mapObjects(json.getObjects());
        List<Vertex> vertices = new ArrayList<>(objects);
        List<Link> links = mapper.mapLinks(json.getLinks(), vertices);
        Map<Object, Position> positions = mapper.mapPositions(json.getPositions(), objects);

        example.getObjects().addAll(objects);
        example.getLinks().addAll(links);
        example.getPositions().putAll(positions);
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
        Path savePath = DocumentUtils.chooseFile(this.frame, JFileChooser.SAVE_DIALOG);
        if (savePath == null) {
            return;
        }

        JsonDataMapper mapper = Mappers.getMapper(JsonDataMapper.class);
        ExampleJson json = mapper.mapToJson(model);
        try {
            new JsonMapper().writeValue(savePath.toFile(), json);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        this.path = savePath;
        this.frame.setTitle(findWindowName());
    }

    public void close() {
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }
}
