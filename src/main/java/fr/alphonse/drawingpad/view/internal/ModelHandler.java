package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.reference.LinkDirection;
import fr.alphonse.drawingpad.data.model.value.Value;
import fr.alphonse.drawingpad.document.utils.GraphHandler;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.Stream;

@UtilityClass
public class ModelHandler {

    public static void addObject(Position position, Drawing drawing) {
        Object object = makeObject(drawing);
        drawing.getElements().add(object);
        drawing.getPositions().put(object, position);
    }

    private static Object makeObject(Drawing drawing) {
        var object = new Object();
        var id = GraphHandler.findAvailableId(drawing.getElements());
        object.setId(id);
        return object;
    }

    public static void addCompletion(GraphElement base, Position position, Drawing drawing) {
        Completion completion = makeCompletion(base, drawing);
        drawing.getElements().add(completion);
        drawing.getPositions().put(completion, position);
    }

    private static Completion makeCompletion(GraphElement base, Drawing drawing) {
        var completion = new Completion();
        var id = GraphHandler.findAvailableId(drawing.getElements());
        completion.setId(id);
        completion.setBase(base);
        completion.setBaseReference(GraphHandler.makeReferenceForElement(base));
        completion.setValue(new Value());
        completion.setLocalValue(new Value());
        return completion;
    }

    public static void addQuantity(GraphElement base, Position position, Drawing drawing) {
        Quantity quantity = makeQuantity(base, drawing);
        drawing.getElements().add(quantity);
        drawing.getPositions().put(quantity, position);
    }

    private static Quantity makeQuantity(GraphElement base, Drawing drawing) {
        var quantity = new Quantity();
        var id = GraphHandler.findAvailableId(drawing.getElements());
        quantity.setId(id);
        quantity.setBase(base);
        quantity.setBaseReference(GraphHandler.makeReferenceForElement(base));
        quantity.setValue(new Value());
        quantity.setLocalValue(new Value());
        return quantity;
    }

    public void addLink(GraphElement origin, LinkDirection originLinkDirection, GraphElement destination, LinkDirection destinationLinkDirection, Position center, Drawing drawing) {
        Link link = makeLink(origin, originLinkDirection, destination, destinationLinkDirection, drawing);
        drawing.getElements().add(link);
        if (center != null) {
            drawing.getPositions().put(link, center);
        }
    }

    private static Link makeLink(GraphElement origin, LinkDirection originLinkDirection, GraphElement destination, LinkDirection destinationLinkDirection, Drawing drawing) {
        var link = new Link();
        var id = GraphHandler.findAvailableId(drawing.getElements());
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

    public static void deleteElement(GraphElement element, Drawing drawing) {
        List<GraphElement> dependentElements = listDependentElements(element, drawing);
        drawing.getElements().removeAll(dependentElements);
        for (GraphElement dependentElement: dependentElements) {
            drawing.getPositions().remove(dependentElement);
        }
    }

    public static List<GraphElement> listDependentElements(GraphElement baseElement, Drawing drawing) {
        Stream<GraphElement> dependentElements = drawing.getElements().stream()
                .filter(element -> isElementAttachedTo(element, baseElement))
                .map(element -> listDependentElements(element, drawing))
                .flatMap(List::stream);
        return Stream.concat(Stream.of(baseElement), dependentElements)
                .toList();
    }

    private static boolean isElementAttachedTo(GraphElement element, GraphElement base) {
        return switch (element) {
            case Object ignored -> false;
            case Completion completion -> completion.getBase() == base;
            case Quantity quantity -> quantity.getBase() == base;
            case Link link -> link.getOrigin() == base || link.getDestination() == base;
        };
    }
}
