package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.reference.LinkDirection;
import fr.alphonse.drawingpad.data.model.value.Graduation;
import fr.alphonse.drawingpad.data.model.value.Value;
import fr.alphonse.drawingpad.document.utils.GraphHandler;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.Stream;

@UtilityClass
public class ModelHandler {

    public static Object addObject(Position position, Drawing drawing) {
        Object object = makeObject(drawing);
        drawing.getElements().add(object);
        drawing.getPositions().put(object, position);
        return object;
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
        completion.setBaseId(base.getId());
        completion.setValue(new Value());
        return completion;
    }

    public void addLink(GraphElement origin, LinkDirection originLinkDirection, GraphElement destination, LinkDirection destinationLinkDirection, Position center, Graduation graduation, Drawing drawing) {
        Link link = makeLink(origin, originLinkDirection, destination, destinationLinkDirection, graduation, drawing);
        drawing.getElements().add(link);
        if (center != null) {
            drawing.getPositions().put(link, center);
        }
    }

    private static Link makeLink(GraphElement origin, LinkDirection originLinkDirection, GraphElement destination, LinkDirection destinationLinkDirection, Graduation graduation, Drawing drawing) {
        var link = new Link();
        var id = GraphHandler.findAvailableId(drawing.getElements());
        link.setId(id);
        link.setOrigin(origin);
        link.setOriginLinkDirection(originLinkDirection);
        link.setOriginId(origin.getId());
        link.setDestination(destination);
        link.setDestinationLinkDirection(destinationLinkDirection);
        link.setDestinationId(destination.getId());
        link.setFactor(Value.builder()
                .graduation(graduation)
                .build());
        return link;
    }

    public static void deleteElement(GraphElement element, Drawing drawing) {
        List<GraphElement> dependentElements = listDependentElements(element, drawing);
        drawing.getElements().removeAll(dependentElements);
        for (GraphElement dependentElement: dependentElements) {
            drawing.getPositions().remove(dependentElement);
            drawing.getNamePositions().remove(dependentElement);
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
            case Link link -> link.getOrigin() == base || link.getDestination() == base;
        };
    }
}
