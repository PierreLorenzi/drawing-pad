package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.reference.LinkDirection;
import fr.alphonse.drawingpad.data.model.value.Value;
import fr.alphonse.drawingpad.document.utils.GraphHandler;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@UtilityClass
public class ModelHandler {

    public static void addObject(Position position, Drawing drawing) {
        Object object = makeObject(drawing);
        drawing.getGraph().getObjects().add(object);
        drawing.getPositions().put(object, position);
    }

    private static Object makeObject(Drawing drawing) {
        var object = new Object();
        var id = GraphHandler.findAvailableId(drawing.getGraph().getObjects());
        object.setId(id);
        return object;
    }

    public static void addCompletion(GraphElement base, Position position, Drawing drawing) {
        Completion completion = makeCompletion(base, drawing);
        drawing.getGraph().getCompletions().add(completion);
        drawing.getPositions().put(completion, position);
    }

    private static Completion makeCompletion(GraphElement base, Drawing drawing) {
        var completion = new Completion();
        var id = GraphHandler.findAvailableId(drawing.getGraph().getCompletions());
        completion.setId(id);
        completion.setBase(base);
        completion.setBaseReference(GraphHandler.makeReferenceForElement(base));
        completion.setValue(new Value());
        completion.setLocalValue(new Value());
        return completion;
    }

    public static void addQuantity(GraphElement base, Position position, Drawing drawing) {
        Quantity quantity = makeQuantity(base, drawing);
        drawing.getGraph().getQuantities().add(quantity);
        drawing.getPositions().put(quantity, position);
    }

    private static Quantity makeQuantity(GraphElement base, Drawing drawing) {
        var quantity = new Quantity();
        var id = GraphHandler.findAvailableId(drawing.getGraph().getQuantities());
        quantity.setId(id);
        quantity.setBase(base);
        quantity.setBaseReference(GraphHandler.makeReferenceForElement(base));
        quantity.setValue(new Value());
        quantity.setLocalValue(new Value());
        return quantity;
    }

    public void addLink(GraphElement origin, LinkDirection originLinkDirection, GraphElement destination, LinkDirection destinationLinkDirection, Position center, Drawing drawing) {
        Link link = makeLink(origin, originLinkDirection, destination, destinationLinkDirection, drawing);
        drawing.getGraph().getLinks().add(link);
        if (center != null) {
            drawing.getPositions().put(link, center);
        }
    }

    private static Link makeLink(GraphElement origin, LinkDirection originLinkDirection, GraphElement destination, LinkDirection destinationLinkDirection, Drawing drawing) {
        var link = new Link();
        var id = GraphHandler.findAvailableId(drawing.getGraph().getLinks());
        link.setId(id);
        link.setOrigin(origin);
        link.setOriginLinkDirection(originLinkDirection);
        link.setOriginReference(GraphHandler.makeReferenceForElement(origin));
        link.setDestination(destination);
        link.setDestinationLinkDirection(destinationLinkDirection);
        link.setDestinationReference(GraphHandler.makeReferenceForElement(destination));
        link.setFactor(new Value());
        return link;
    }

    public static void deleteObject(Object object, Drawing drawing) {
        List<GraphElement> dependentElements = listDependentElements(object, drawing);
        removeElementsFromDrawing(dependentElements, drawing);
        deleteObjectWithoutDependents(object, drawing);
    }

    public static void deleteObjectWithoutDependents(Object object, Drawing drawing) {
        drawing.getGraph().getObjects().remove(object);
        drawing.getPositions().remove(object);
    }

    private static List<GraphElement> listDependentElements(GraphElement element, Drawing drawing) {
        List<GraphElement> elements = new ArrayList<>();

        for (Completion completion: drawing.getGraph().getCompletions()) {
            if (completion.getBase() == element) {
                elements.addAll(listDependentElements(completion, drawing));
                elements.add(completion);
            }
        }
        for (Quantity quantity: drawing.getGraph().getQuantities()) {
            if (quantity.getBase() == element) {
                elements.addAll(listDependentElements(quantity, drawing));
                elements.add(quantity);
            }
        }
        for (Link link : drawing.getGraph().getLinks()) {
            if (link.getOrigin() == element || link.getDestination() == element) {
                elements.addAll(listDependentElements(link, drawing));
                elements.add(link);
            }
        }

        return elements;
    }

    private static void removeElementsFromDrawing(List<GraphElement> elements, Drawing drawing) {
        for (GraphElement element: elements) {
            switch (element) {
                case Object object -> deleteObjectWithoutDependents(object, drawing);
                case Completion completion -> deleteCompletionWithoutDependents(completion, drawing);
                case Quantity quantity -> deleteQuantityWithoutDependents(quantity, drawing);
                case Link link -> deleteLinkWithoutDependents(link, drawing);
            }
        }
    }

    public static void deleteCompletion(Completion completion, Drawing drawing) {
        List<GraphElement> dependentElements = listDependentElements(completion, drawing);
        removeElementsFromDrawing(dependentElements, drawing);
        deleteCompletionWithoutDependents(completion, drawing);
    }

    public static void deleteCompletionWithoutDependents(Completion completion, Drawing drawing) {
        drawing.getGraph().getCompletions().remove(completion);
        drawing.getPositions().remove(completion);
    }

    public static void deleteQuantity(Quantity quantity, Drawing drawing) {
        List<GraphElement> dependentElements = listDependentElements(quantity, drawing);
        removeElementsFromDrawing(dependentElements, drawing);
        deleteQuantityWithoutDependents(quantity, drawing);
    }

    public static void deleteQuantityWithoutDependents(Quantity quantity, Drawing drawing) {
        drawing.getGraph().getQuantities().remove(quantity);
        drawing.getPositions().remove(quantity);
    }

    public static void deleteLink(Link link, Drawing drawing) {
        List<GraphElement> dependentElements = listDependentElements(link, drawing);
        removeElementsFromDrawing(dependentElements, drawing);
        deleteLinkWithoutDependents(link, drawing);
    }

    public static void deleteLinkWithoutDependents(Link link, Drawing drawing) {
        drawing.getGraph().getLinks().remove(link);
        drawing.getPositions().remove(link);
    }

    public static Stream<GraphElement> streamElementsInModel(Drawing model) {
        Graph graph = model.getGraph();
        return Stream.of(graph.getObjects(), graph.getCompletions(), graph.getQuantities(), graph.getLinks())
                .flatMap(List::stream)
                .map(GraphElement.class::cast);
    }
}
