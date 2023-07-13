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
        var originFactorId = new LowerValue.Id(makeSameIdWithOtherMask(id, LowerValue.Id.LINK_ORIGIN_FACTOR_MASK));
        var destinationFactorId = new LowerValue.Id(makeSameIdWithOtherMask(id, LowerValue.Id.LINK_DESTINATION_FACTOR_MASK));
        link.setId(id);
        id.setState(link);
        link.setOrigin(origin);
        link.setDestination(destination);
        link.setOriginFactor(LowerValue.builder()
                        .id(originFactorId)
                .build());
        link.setDestinationFactor(LowerValue.builder()
                .id(destinationFactorId)
                .build());
        return link;
    }

    public static int makeSameIdWithOtherMask(Vertex.Id id, int mask) {
        return (id.getValue() & ~Vertex.Id.TYPE_MASK) | mask;
    }

    public boolean addAmount(Vertex vertex, Drawing drawing) {
        if (doesAmountExistWithVertex(vertex, drawing)) {
            return false;
        }
        Amount amount = makeAmount(vertex, drawing);
        drawing.getGraph().getAmounts().add(amount);
        return true;
    }

    private boolean doesAmountExistWithVertex(Vertex vertex, Drawing drawing) {
        return drawing.getGraph().getAmounts().stream().anyMatch(amount -> amount.getModelId().equals(vertex.getId()));
    }

    private static Amount makeAmount(Vertex vertex, Drawing drawing) {
        var amount = new Amount();
        var id = new Amount.Id(findAvailableVertexId(drawing.getGraph().getAmounts(), Amount.Id.MASK));
        var countId = new WholeValue.Id(makeSameIdWithOtherMask(id, WholeValue.Id.AMOUNT_COUNT_MASK));
        var distinctCountId = new WholeValue.Id(makeSameIdWithOtherMask(id, WholeValue.Id.AMOUNT_DISTINCT_COUNT_MASK));
        amount.setId(id);
        id.setState(amount);
        amount.setModel(vertex);
        amount.setCount(WholeValue.builder().id(countId).build());
        amount.setDistinctCount(WholeValue.builder().id(distinctCountId).build());
        return amount;
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
        var completenessId = new LowerValue.Id(makeSameIdWithOtherMask(id, LowerValue.Id.DEFINITION_COMPLETENESS_MASK));
        definition.setId(id);
        id.setState(definition);
        definition.setBase(vertex);
        definition.setCompleteness(LowerValue.builder().id(completenessId).build());
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

        for (Amount amount: drawing.getGraph().getAmounts()) {
            if (amount.getModel() == vertex) {
                vertices.addAll(listDependentVertices(amount, drawing));
                vertices.add(amount);
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
                case Amount amount -> drawing.getGraph().getAmounts().remove(amount);
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

    public static void deleteAmount(Amount amount, Drawing drawing) {
        List<Vertex> dependentVertices = listDependentVertices(amount, drawing);
        removeVerticesFromDrawing(dependentVertices, drawing);
        drawing.getGraph().getAmounts().remove(amount);
    }

    public static void deleteDefinition(Definition definition, Drawing drawing) {
        List<Vertex> dependentVertices = listDependentVertices(definition, drawing);
        removeVerticesFromDrawing(dependentVertices, drawing);
        drawing.getGraph().getDefinitions().remove(definition);
    }
}
