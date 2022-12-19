package fr.alphonse.drawingpad.data;

import com.fasterxml.jackson.databind.json.JsonMapper;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.json.ExampleJson;
import fr.alphonse.drawingpad.model.Link;
import fr.alphonse.drawingpad.model.Object;
import fr.alphonse.drawingpad.model.Vertex;
import org.mapstruct.factory.Mappers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Document {

    private Path path;

    private final Example example = new Example();

    public Document() {}

    public Document(Path path) throws IOException {
        this.path = path;
        importFile(path, example);
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

    public Example getExample() {
        return example;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public void save() throws IOException {
        JsonDataMapper mapper = Mappers.getMapper(JsonDataMapper.class);
        ExampleJson json = mapper.mapToJson(example);
        new JsonMapper().writeValue(path.toFile(), json);
    }
}
