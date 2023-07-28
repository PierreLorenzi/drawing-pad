package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.reference.Reference;
import fr.alphonse.drawingpad.data.model.reference.ReferenceType;
import fr.alphonse.drawingpad.data.model.value.Value;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ModelHandler {

    public static Object addObject(Position position, Drawing drawing) {
        Object object = makeObject(drawing);
        drawing.getGraph().getObjects().add(object);
        drawing.getPositions().put(object, position);
        return object;
    }

    private static Object makeObject(Drawing drawing) {
        var object = new Object();
        var id = findAvailableId(drawing.getGraph().getObjects());
        object.setId(id);
        return object;
    }

    private static int findAvailableId(List<? extends GraphElement> elements) {
        int maxId = elements.stream()
                .mapToInt(GraphElement::getId)
                .max()
                .orElse(0);
        return 1 + maxId;
    }

    public static Completion addCompletion(Vertex base, Position position, Drawing drawing) {
        Completion completion = makeCompletion(base, drawing);
        drawing.getGraph().getCompletions().add(completion);
        drawing.getCompletionPositions().put(completion, position);
        return completion;
    }

    private static Completion makeCompletion(Vertex base, Drawing drawing) {
        var completion = new Completion();
        var id = findAvailableId(drawing.getGraph().getCompletions());
        completion.setId(id);
        completion.setBase(base);
        completion.setBaseReference(makeReferenceForVertex(base));
        completion.setValue(new Value());
        completion.setLocalValue(new Value());
        return completion;
    }

    public static Quantity addQuantity(Vertex base, Position position, Drawing drawing) {
        Quantity quantity = makeQuantity(base, drawing);
        drawing.getGraph().getQuantities().add(quantity);
        drawing.getQuantityPositions().put(quantity, position);
        return quantity;
    }

    private static Quantity makeQuantity(Vertex base, Drawing drawing) {
        var quantity = new Quantity();
        var id = findAvailableId(drawing.getGraph().getQuantities());
        quantity.setId(id);
        quantity.setBase(base);
        quantity.setBaseReference(makeReferenceForVertex(base));
        quantity.setValue(new Value());
        quantity.setLocalValue(new Value());
        return quantity;
    }

    public Link addLink(Vertex origin, Vertex destination, Position center, Drawing drawing) {
        Link link = makeLink(origin, destination, drawing);
        drawing.getGraph().getLinks().add(link);
        if (center != null) {
            drawing.getLinkCenters().put(link, center);
        }
        return link;
    }

    private static Link makeLink(Vertex origin, Vertex destination, Drawing drawing) {
        var link = new Link();
        var id = findAvailableId(drawing.getGraph().getLinks());
        link.setId(id);
        link.setOrigin(origin);
        link.setOriginReference(makeReferenceForVertex(origin));
        link.setDestination(destination);
        link.setDestinationReference(makeReferenceForVertex(destination));
        link.setDirectFactor(DirectFactor.builder().link(link).build());
        link.setReverseFactor(ReverseFactor.builder().link(link).build());
        link.setFactor(new Value());
        return link;
    }

    private Reference makeReferenceForVertex(Vertex vertex) {
        return switch (vertex) {
            case Object object -> new Reference(ReferenceType.OBJECT, object.getId());
            case Completion completion -> new Reference(ReferenceType.COMPLETION, completion.getId());
            case Quantity quantity -> new Reference(ReferenceType.QUANTITY, quantity.getId());
            case DirectFactor directFactor -> new Reference(ReferenceType.DIRECT_LINK, directFactor.getLink().getId());
            case ReverseFactor reverseFactor -> new Reference(ReferenceType.REVERSE_LINK, reverseFactor.getLink().getId());
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

        for (Completion completion: drawing.getGraph().getCompletions()) {
            if (completion.getBase().getElement() == element) {
                elements.addAll(listDependentElements(completion, drawing));
                elements.add(completion);
            }
        }
        for (Quantity quantity: drawing.getGraph().getQuantities()) {
            if (quantity.getBase().getElement() == element) {
                elements.addAll(listDependentElements(quantity, drawing));
                elements.add(quantity);
            }
        }
        for (Link link : drawing.getGraph().getLinks()) {
            if (link.getOrigin().getElement() == element || link.getDestination().getElement() == element) {
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
        drawing.getCompletionPositions().remove(completion);
    }

    public static void deleteQuantity(Quantity quantity, Drawing drawing) {
        List<GraphElement> dependentElements = listDependentElements(quantity, drawing);
        removeElementsFromDrawing(dependentElements, drawing);
        deleteQuantityWithoutDependents(quantity, drawing);
    }

    public static void deleteQuantityWithoutDependents(Quantity quantity, Drawing drawing) {
        drawing.getGraph().getQuantities().remove(quantity);
        drawing.getQuantityPositions().remove(quantity);
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

    public static List<GraphElement> copyGraph(List<GraphElement> elements, Drawing drawing) {
        List<GraphElement> sortedElements = sortElementsInDependenceOrder(elements);
        List<GraphElement> newElements = new ArrayList<>(elements.size());
        for (GraphElement element: sortedElements) {
            GraphElement newElement = switch (element) {
                case Object object -> copyObject(object, drawing);
                case Completion completion -> {
                    Vertex newBase = findCopiedVertex(completion.getBase(), sortedElements, newElements);
                    yield copyCompletion(completion, newBase, drawing);
                }
                case Quantity quantity -> {
                    Vertex newBase = findCopiedVertex(quantity.getBase(), sortedElements, newElements);
                    yield copyQuantity(quantity, newBase, drawing);
                }
                case Link link -> {
                    Vertex newOrigin = findCopiedVertex(link.getOrigin(), sortedElements, newElements);
                    Vertex newDestination = findCopiedVertex(link.getDestination(), sortedElements, newElements);
                    yield copyLink(link, newOrigin, newDestination, drawing);
                }
            };
            newElements.add(newElement);
        }
        return newElements;
    }

    private static List<GraphElement> sortElementsInDependenceOrder(List<GraphElement> elements) {
        List<GraphElement> sortedElements = new ArrayList<>(elements.size());
        while (sortedElements.size() < elements.size()) {
            for (GraphElement element: elements) {
                if (sortedElements.contains(element)) {
                    continue;
                }
                boolean canAdd = switch (element) {
                    case Object ignored -> true;
                    case Completion completion -> sortedElements.contains(completion.getBase().getElement());
                    case Quantity quantity -> sortedElements.contains(quantity.getBase().getElement());
                    case Link link -> sortedElements.contains(link.getOrigin().getElement()) && sortedElements.contains(link.getDestination().getElement());
                };
                if (!canAdd) {
                    continue;
                }
                sortedElements.add(element);
            }
        }
        return sortedElements;
    }

    private static Object copyObject(Object object, Drawing drawing) {
        Position position = drawing.getPositions().get(object);
        Object newObject = addObject(position, drawing);
        newObject.setName(object.getName());
        return newObject;
    }

    private static Vertex findCopiedVertex(Vertex vertex, List<GraphElement> elements, List<GraphElement> newElements) {
        int baseIndex = elements.indexOf(vertex.getElement());
        GraphElement newBaseElement = newElements.get(baseIndex);
        return switch (vertex) {
            case Object ignored -> (Object)newBaseElement;
            case Completion ignored -> (Completion)newBaseElement;
            case Quantity ignored -> (Quantity)newBaseElement;
            case DirectFactor ignored -> ((Link)newBaseElement).getDirectFactor();
            case ReverseFactor ignored -> ((Link)newBaseElement).getReverseFactor();
        };
    }

    private static Completion copyCompletion(Completion completion, Vertex newBase, Drawing drawing) {
        Position position = drawing.getCompletionPositions().get(completion);
        Completion newCompletion = addCompletion(newBase, position, drawing);
        newCompletion.setName(completion.getName());
        newCompletion.setValue(copyValue(completion.getValue()));
        newCompletion.setLocalValue(copyValue(completion.getLocalValue()));
        return newCompletion;
    }

    private static Value copyValue(Value value) {
        return Value.builder()
                .graduation(value.getGraduation())
                .numberInGraduation(value.getNumberInGraduation())
                .build();
    }

    private static Quantity copyQuantity(Quantity quantity, Vertex newBase, Drawing drawing) {
        Position position = drawing.getQuantityPositions().get(quantity);
        Quantity newQuantity = addQuantity(newBase, position, drawing);
        newQuantity.setName(quantity.getName());
        newQuantity.setValue(copyValue(quantity.getValue()));
        newQuantity.setLocalValue(copyValue(quantity.getLocalValue()));
        return newQuantity;
    }

    private static Link copyLink(Link link, Vertex newOrigin, Vertex newDestination, Drawing drawing) {
        Position center = drawing.getLinkCenters().get(link);
        Link newLink = addLink(newOrigin, newDestination, center, drawing);
        newLink.setName(link.getName());
        newLink.setFactor(copyValue(link.getFactor()));
        return newLink;
    }
}
