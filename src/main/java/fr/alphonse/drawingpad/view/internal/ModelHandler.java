package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.value.GraduatedValue;
import fr.alphonse.drawingpad.document.utils.GraphHandler;
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
        var id = findAvailableVertexId(drawing.getGraph().getObjects());
        object.setId(id);
        object.setCompleteness(LowerValue.builder()
                .value(new GraduatedValue<>())
                .owner(object)
                .build());
        object.setQuantity(WholeValue.builder()
                .value(new GraduatedValue<>())
                .owner(object)
                .build());
        object.getQuantity().setCompleteness(LowerValue.builder()
                .value(new GraduatedValue<>())
                .owner(object.getQuantity())
                .build());
        object.setLocalCompleteness(LowerValue.builder()
                .value(new GraduatedValue<>())
                .owner(object)
                .build());
        return object;
    }

    private static int findAvailableVertexId(List<? extends Vertex> vertices) {
        int maxId = vertices.stream()
                .mapToInt(Vertex::getId)
                .max()
                .orElse(0);
        return 1 + maxId;
    }

    public boolean addPossessionLink(Vertex origin, Position center, Vertex destination, Drawing drawing) {
        PossessionLink possessionLink = makePossessionLink(origin, destination, drawing);
        drawing.getGraph().getPossessionLinks().add(possessionLink);
        if (center != null) {
            drawing.getPossessionLinkCenters().put(possessionLink, center);
        }
        return true;
    }

    private static PossessionLink makePossessionLink(Vertex origin, Vertex destination, Drawing drawing) {
        var link = new PossessionLink();
        var id = findAvailableVertexId(drawing.getGraph().getPossessionLinks());
        link.setId(id);
        link.setOrigin(origin);
        link.setOriginReference(GraphHandler.makeVertexReference(origin, drawing.getGraph()));
        link.setDestination(destination);
        link.setDestinationReference(GraphHandler.makeVertexReference(destination, drawing.getGraph()));
        link.setFactor(new GraduatedValue<>());
        link.setCompleteness(LowerValue.builder()
                .value(new GraduatedValue<>())
                .owner(link)
                .build());
        return link;
    }

    public boolean addComparisonLink(Vertex origin, Position center, Vertex destination, Drawing drawing) {
        ComparisonLink comparisonLink = makeComparisonLink(origin, destination, drawing);
        drawing.getGraph().getComparisonLinks().add(comparisonLink);
        if (center != null) {
            drawing.getComparisonLinkCenters().put(comparisonLink, center);
        }
        return true;
    }

    private static ComparisonLink makeComparisonLink(Vertex origin, Vertex destination, Drawing drawing) {
        var link = new ComparisonLink();
        var id = findAvailableVertexId(drawing.getGraph().getComparisonLinks());
        link.setId(id);
        link.setOrigin(origin);
        link.setOriginReference(GraphHandler.makeVertexReference(origin, drawing.getGraph()));
        link.setDestination(destination);
        link.setDestinationReference(GraphHandler.makeVertexReference(destination, drawing.getGraph()));
        link.setFactor(new GraduatedValue<>());
        link.setCompleteness(LowerValue.builder()
                .value(new GraduatedValue<>())
                .owner(link)
                .build());
        return link;
    }

    public static void deleteObject(Object object, Drawing drawing) {
        List<Vertex> dependentVertices = listDependentVertices(object, drawing);
        removeVerticesFromDrawing(dependentVertices, drawing);
        deleteObjectWithoutDependents(object, drawing);
    }

    public static void deleteObjectWithoutDependents(Object object, Drawing drawing) {
        drawing.getGraph().getObjects().remove(object);
        drawing.getPositions().remove(object);
    }

    private static List<Vertex> listDependentVertices(Vertex vertex, Drawing drawing) {
        List<Vertex> vertices = new ArrayList<>();

        // inner values
        if (vertex.getCompleteness() != null) {
            vertices.addAll(listDependentVertices(vertex.getCompleteness(), drawing));
        }
        if (vertex instanceof Object object) {
            vertices.addAll(listDependentVertices(object.getQuantity(), drawing));
        }

        for (PossessionLink possessionLink : drawing.getGraph().getPossessionLinks()) {
            if (possessionLink.getOrigin() == vertex || possessionLink.getDestination() == vertex) {
                vertices.addAll(listDependentVertices(possessionLink, drawing));
                vertices.add(possessionLink);
            }
        }

        for (ComparisonLink comparisonLink : drawing.getGraph().getComparisonLinks()) {
            if (comparisonLink.getOrigin() == vertex || comparisonLink.getDestination() == vertex) {
                vertices.addAll(listDependentVertices(comparisonLink, drawing));
                vertices.add(comparisonLink);
            }
        }

        return vertices;
    }

    private static void removeVerticesFromDrawing(List<Vertex> vertices, Drawing drawing) {
        for (Vertex vertex: vertices) {
            switch (vertex) {
                case Object object -> deleteObjectWithoutDependents(object, drawing);
                case PossessionLink possessionLink -> deletePossessionLinkWithoutDependents(possessionLink, drawing);
                case ComparisonLink comparisonLink -> deleteComparisonLinkWithoutDependents(comparisonLink, drawing);
                case WholeValue ignored -> throw new Error("Whole values not handled as real vertices");
                case LowerValue ignored -> throw new Error("Lower values not handled as real vertices");
            }
        }
    }

    public static void deletePossessionLink(PossessionLink possessionLink, Drawing drawing) {
        List<Vertex> dependentVertices = listDependentVertices(possessionLink, drawing);
        removeVerticesFromDrawing(dependentVertices, drawing);
        deletePossessionLinkWithoutDependents(possessionLink, drawing);
    }

    public static void deletePossessionLinkWithoutDependents(PossessionLink possessionLink, Drawing drawing) {
        drawing.getGraph().getPossessionLinks().remove(possessionLink);
        drawing.getPossessionLinkCenters().remove(possessionLink);
    }

    public static void deleteComparisonLink(ComparisonLink comparisonLink, Drawing drawing) {
        List<Vertex> dependentVertices = listDependentVertices(comparisonLink, drawing);
        removeVerticesFromDrawing(dependentVertices, drawing);
        deleteComparisonLinkWithoutDependents(comparisonLink, drawing);
    }

    public static void deleteComparisonLinkWithoutDependents(ComparisonLink comparisonLink, Drawing drawing) {
        drawing.getGraph().getComparisonLinks().remove(comparisonLink);
        drawing.getComparisonLinkCenters().remove(comparisonLink);
    }
}
