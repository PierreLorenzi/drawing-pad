package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.Example;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.value.GraduatedValue;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ModelHandler {

    public static void addObject(Position position, Example example) {
        Object object = makeObject(example);
        example.getGraph().getObjects().add(object);
        example.getPositions().put(object, position);
    }

    private static Object makeObject(Example example) {
        var object = new Object();
        var id = new Object.Id(findAvailableVertexId(example.getGraph().getObjects(), Object.Id.MASK));
        object.setId(id);
        id.setState(object);
        return object;
    }

    private static int findAvailableVertexId(List<? extends Vertex> vertices, int mask) {
        int maxId = vertices.stream()
                .map(Vertex::getId)
                .mapToInt(Vertex.Id::getValue)
                .max()
                .orElse(mask);
        return 1 + maxId;
    }

    public boolean addLink(Vertex origin, Vertex destination, Example example) {
        if (doesLinkExistWithObjects(origin, destination, example)) {
            return false;
        }
        Link link = makeLink(origin, destination, example);
        example.getGraph().getLinks().add(link);
        return true;
    }

    private static boolean doesLinkExistWithObjects(Vertex origin, Vertex destination, Example example) {
        // no link between the objects in either side
        return doesLinkExistWithOriginAndDestination(origin, destination, example)
                || doesLinkExistWithOriginAndDestination(destination, origin, example);
    }

    private static boolean doesLinkExistWithOriginAndDestination(Vertex origin, Vertex destination, Example example) {
        return example.getGraph().getLinks().stream()
                .anyMatch(link -> link.getOriginId().equals(origin.getId()) && link.getDestinationId().equals(destination.getId()));
    }

    private static Link makeLink(Vertex origin, Vertex destination, Example example) {
        var link = new Link();
        var id = new Link.Id(findAvailableVertexId(example.getGraph().getLinks(), Link.Id.MASK));
        link.setId(id);
        id.setState(link);
        link.setOrigin(origin);
        link.setDestination(destination);
        link.setOriginFactor(new GraduatedValue<>());
        link.setDestinationFactor(new GraduatedValue<>());
        return link;
    }

    public boolean addAmount(Vertex vertex, Example example) {
        if (doesAmountExistWithVertex(vertex, example)) {
            return false;
        }
        Amount amount = makeAmount(vertex, example);
        example.getGraph().getAmounts().add(amount);
        return true;
    }

    private boolean doesAmountExistWithVertex(Vertex vertex, Example example) {
        return example.getGraph().getAmounts().stream().anyMatch(amount -> amount.getModelId().equals(vertex.getId()));
    }

    private static Amount makeAmount(Vertex vertex, Example example) {
        var amount = new Amount();
        var id = new Amount.Id(findAvailableVertexId(example.getGraph().getAmounts(), Amount.Id.MASK));
        amount.setId(id);
        id.setState(amount);
        amount.setModel(vertex);
        amount.setCount(new GraduatedValue<>());
        amount.setDistinctCount(new GraduatedValue<>());
        return amount;
    }

    public boolean addDefinition(Vertex vertex, Example example) {
        if (doesDefinitionExistWithVertex(vertex, example)) {
            return false;
        }
        Definition definition = makeDefinition(vertex, example);
        example.getGraph().getDefinitions().add(definition);
        return true;
    }

    private boolean doesDefinitionExistWithVertex(Vertex vertex, Example example) {
        return example.getGraph().getDefinitions().stream().anyMatch(definition -> definition.getBaseId().equals(vertex.getId()));
    }

    private static Definition makeDefinition(Vertex vertex, Example example) {
        var definition = new Definition();
        var id = new Definition.Id(findAvailableVertexId(example.getGraph().getDefinitions(), Definition.Id.MASK));
        definition.setId(id);
        id.setState(definition);
        definition.setBase(vertex);
        definition.setCompleteness(new GraduatedValue<>());
        return definition;
    }

    public static void deleteObject(Object object, Example example) {
        List<Vertex> dependentVertices = listDependentVertices(object, example);
        removeVerticesFromExample(dependentVertices, example);
        example.getGraph().getObjects().remove(object);
        example.getPositions().remove(object);
    }

    private static List<Vertex> listDependentVertices(Vertex vertex, Example example) {
        List<Vertex> vertices = new ArrayList<>();

        for (Link link: example.getGraph().getLinks()) {
            if (link.getOrigin() == vertex || link.getDestination() == vertex) {
                vertices.addAll(listDependentVertices(link, example));
                vertices.add(link);
            }
        }

        for (Amount amount: example.getGraph().getAmounts()) {
            if (amount.getModel() == vertex) {
                vertices.addAll(listDependentVertices(amount, example));
                vertices.add(amount);
            }
        }

        for (Definition definition: example.getGraph().getDefinitions()) {
            if (definition.getBase() == vertex) {
                vertices.addAll(listDependentVertices(definition, example));
                vertices.add(definition);
            }
        }

        return vertices;
    }

    private static void removeVerticesFromExample(List<Vertex> vertices, Example example) {
        for (Vertex vertex: vertices) {
            switch (vertex) {
                case Object object -> example.getGraph().getObjects().remove(object);
                case Link link -> example.getGraph().getLinks().remove(link);
                case Amount amount -> example.getGraph().getAmounts().remove(amount);
                case Definition definition -> example.getGraph().getDefinitions().remove(definition);
            }
        }
    }

    public static void deleteLink(Link link, Example example) {
        List<Vertex> dependentVertices = listDependentVertices(link, example);
        removeVerticesFromExample(dependentVertices, example);
        example.getGraph().getLinks().remove(link);
    }

    public static void deleteAmount(Amount amount, Example example) {
        List<Vertex> dependentVertices = listDependentVertices(amount, example);
        removeVerticesFromExample(dependentVertices, example);
        example.getGraph().getAmounts().remove(amount);
    }

    public static void deleteDefinition(Definition definition, Example example) {
        List<Vertex> dependentVertices = listDependentVertices(definition, example);
        removeVerticesFromExample(dependentVertices, example);
        example.getGraph().getDefinitions().remove(definition);
    }
}
