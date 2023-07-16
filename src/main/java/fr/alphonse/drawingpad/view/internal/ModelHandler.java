package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.Object;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ModelHandler {

    public static void addObject(Position position, Drawing drawing) {
        Object object = makeObject(drawing);
        drawing.getGraph().getObjects().add(object);
        drawing.getPositions().put(object, position);
    }

    private static Object makeObject(Drawing drawing) {
        var object = new Object();
        var id = new Object.Id(findAvailableVertexId(drawing.getGraph().getObjects(), Object.Id.MASK));
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

    public boolean addLink(Vertex origin, Vertex destination, Drawing drawing) {
        if (doesLinkExistWithObjects(origin, destination, drawing)) {
            return false;
        }
        Link link = makeLink(origin, destination, drawing);
        drawing.getGraph().getLinks().add(link);
        return true;
    }

    private static boolean doesLinkExistWithObjects(Vertex origin, Vertex destination, Drawing drawing) {
        // no link between the objects in either side
        return doesLinkExistWithOriginAndDestination(origin, destination, drawing)
                || doesLinkExistWithOriginAndDestination(destination, origin, drawing);
    }

    private static boolean doesLinkExistWithOriginAndDestination(Vertex origin, Vertex destination, Drawing drawing) {
        return drawing.getGraph().getLinks().stream()
                .anyMatch(link -> link.getOriginId().equals(origin.getId()) && link.getDestinationId().equals(destination.getId()));
    }

    private static Link makeLink(Vertex origin, Vertex destination, Drawing drawing) {
        var link = new Link();
        var id = new Link.Id(findAvailableVertexId(drawing.getGraph().getLinks(), Link.Id.MASK));
        var factorId = new WholeValue.Id(makeSameIdWithOtherMask(id, Link.Id.LINK_FACTOR_MASK));
        var quantityId = new WholeValue.Id(makeSameIdWithOtherMask(id, Link.Id.LINK_QUANTITY_MASK));
        link.setId(id);
        id.setState(link);
        link.setOrigin(origin);
        link.setDestination(destination);
        link.setFactor(WholeValue.builder()
                        .id(factorId)
                .build());
        link.setQuantity(WholeValue.builder()
                .id(quantityId)
                .build());
        return link;
    }

    public static int makeSameIdWithOtherMask(Vertex.Id id, int mask) {
        return (id.getValue() & ~Vertex.Id.TYPE_MASK) | mask;
    }

    public boolean addDefinition(Vertex vertex, Drawing drawing) {
        if (doesDefinitionExistWithVertex(vertex, drawing)) {
            return false;
        }
        Definition definition = makeDefinition(vertex, drawing);
        drawing.getGraph().getDefinitions().add(definition);
        return true;
    }

    private boolean doesDefinitionExistWithVertex(Vertex vertex, Drawing drawing) {
        return drawing.getGraph().getDefinitions().stream().anyMatch(definition -> definition.getBaseId().equals(vertex.getId()));
    }

    private static Definition makeDefinition(Vertex vertex, Drawing drawing) {
        var definition = new Definition();
        var id = new Definition.Id(findAvailableVertexId(drawing.getGraph().getDefinitions(), Definition.Id.MASK));
        var localCompletenessId = new LowerValue.Id(makeSameIdWithOtherMask(id, Definition.Id.DEFINITION_LOCAL_COMPLETENESS_MASK));
        var globalCompletenessId = new LowerValue.Id(makeSameIdWithOtherMask(id, Definition.Id.DEFINITION_GLOBAL_COMPLETENESS_MASK));
        definition.setId(id);
        id.setState(definition);
        definition.setBase(vertex);
        definition.setLocalCompleteness(LowerValue.builder().id(localCompletenessId).build());
        definition.setGlobalCompleteness(LowerValue.builder().id(globalCompletenessId).build());
        return definition;
    }

    public static void deleteObject(Object object, Drawing drawing) {
        List<Vertex> dependentVertices = listDependentVertices(object, drawing);
        removeVerticesFromDrawing(dependentVertices, drawing);
        drawing.getGraph().getObjects().remove(object);
        drawing.getPositions().remove(object);
    }

    private static List<Vertex> listDependentVertices(Vertex vertex, Drawing drawing) {
        List<Vertex> vertices = new ArrayList<>();

        for (Link link: drawing.getGraph().getLinks()) {
            if (link.getOrigin() == vertex || link.getDestination() == vertex) {
                vertices.addAll(listDependentVertices(link, drawing));
                vertices.add(link);
            }
        }

        for (Definition definition: drawing.getGraph().getDefinitions()) {
            if (definition.getBase() == vertex) {
                vertices.addAll(listDependentVertices(definition, drawing));
                vertices.add(definition);
            }
        }

        return vertices;
    }

    private static void removeVerticesFromDrawing(List<Vertex> vertices, Drawing drawing) {
        for (Vertex vertex: vertices) {
            switch (vertex) {
                case Object object -> drawing.getGraph().getObjects().remove(object);
                case Link link -> drawing.getGraph().getLinks().remove(link);
                case Definition definition -> drawing.getGraph().getDefinitions().remove(definition);
                case WholeValue ignored -> throw new Error("Whole values not handled as real vertices");
                case LowerValue ignored -> throw new Error("Lower values not handled as real vertices");
            }
        }
    }

    public static void deleteLink(Link link, Drawing drawing) {
        List<Vertex> dependentVertices = listDependentVertices(link, drawing);
        removeVerticesFromDrawing(dependentVertices, drawing);
        drawing.getGraph().getLinks().remove(link);
    }

    public static void deleteDefinition(Definition definition, Drawing drawing) {
        List<Vertex> dependentVertices = listDependentVertices(definition, drawing);
        removeVerticesFromDrawing(dependentVertices, drawing);
        drawing.getGraph().getDefinitions().remove(definition);
    }
}
