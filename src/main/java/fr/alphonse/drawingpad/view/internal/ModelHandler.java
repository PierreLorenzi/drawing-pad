package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.GraphElement;
import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.reference.Reference;
import fr.alphonse.drawingpad.data.model.reference.ReferenceType;
import fr.alphonse.drawingpad.data.model.value.Value;
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
        var id = findAvailableId(drawing.getGraph().getObjects());
        object.setId(id);
        object.setCompletion(new Value());
        object.setQuantity(new Value());
        object.setQuantityCompletion(new Value());
        object.setLocalCompletion(new Value());
        return object;
    }

    private static int findAvailableId(List<? extends GraphElement> elements) {
        int maxId = elements.stream()
                .mapToInt(GraphElement::getId)
                .max()
                .orElse(0);
        return 1 + maxId;
    }

    public void addLink(GraphElement originElement, GraphElement destinationElement, Position center, Drawing drawing) {
        Link link = makeLink(originElement, destinationElement, drawing);
        drawing.getGraph().getLinks().add(link);
        if (center != null) {
            drawing.getLinkCenters().put(link, center);
        }
    }

    private static Link makeLink(GraphElement originElement, GraphElement destinationElement, Drawing drawing) {
        var link = new Link();
        var id = findAvailableId(drawing.getGraph().getLinks());
        link.setId(id);
        link.setOriginElement(originElement);
        link.setOriginReference(makeReferenceForElement(originElement));
        link.setDestinationElement(destinationElement);
        link.setDestinationReference(makeReferenceForElement(destinationElement));
        link.setFactor(new Value());
        link.setCompletion(new Value());
        return link;
    }

    private Reference makeReferenceForElement(GraphElement element) {
        return switch (element) {
            case Object ignored -> new Reference(ReferenceType.OBJECT, element.getId());
            case Link ignored -> new Reference(ReferenceType.DIRECT_LINK, element.getId());
        };
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

        for (Link link : drawing.getGraph().getLinks()) {
            if (link.getOriginElement() == element || link.getDestinationElement() == element) {
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
                case Link link -> deleteLinkWithoutDependents(link, drawing);
            }
        }
    }

    public static void deleteLink(Link link, Drawing drawing) {
        List<GraphElement> dependentElements = listDependentElements(link, drawing);
        removeElementsFromDrawing(dependentElements, drawing);
        deleteLinkWithoutDependents(link, drawing);
    }

    public static void deleteLinkWithoutDependents(Link link, Drawing drawing) {
        drawing.getGraph().getLinks().remove(link);
        drawing.getLinkCenters().remove(link);
    }
}
