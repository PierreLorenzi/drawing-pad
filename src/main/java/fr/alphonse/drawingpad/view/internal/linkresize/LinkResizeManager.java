package fr.alphonse.drawingpad.view.internal.linkresize;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.geometry.Vector;
import fr.alphonse.drawingpad.data.model.Completion;
import fr.alphonse.drawingpad.data.model.GraphElement;
import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.document.utils.ChangeDetector;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class LinkResizeManager {

    private final Drawing model;

    private final ChangeDetector<?,?> changeDetector;

    public LinkResizeManager(Drawing model, ChangeDetector<?,?> changeDetector) {
        this.model = model;
        this.changeDetector = changeDetector;
    }

    public void resizeLinks(Link modelLink, LinkResizeModification modification) {
        HashMap<GraphElement, Vector> shifts = listObjectShifts(modelLink, modification);
        Map<GraphElement, Position> positions = model.getPositions();

        for (Map.Entry<GraphElement, Vector> entry: shifts.entrySet()) {
            GraphElement element = entry.getKey();
            Vector shift = entry.getValue();
            Position position = positions.get(element);
            Position newPosition = position.translate(shift);
            positions.put(element, newPosition);
        }

        changeDetector.notifyChange();
    }

    private HashMap<GraphElement, Vector> listObjectShifts(Link link, LinkResizeModification modification) {
        Object center = findResizeCenter(modification);
        Predicate<Link> mustLinkBeModifiedPredicate = makeMustLinkBeModifiedPredicate(link, modification);
        Vector shift = findModificationShift(modification);
        HashMap<GraphElement, Vector> shifts = new HashMap<>();
        fillObjectShiftsAround(shifts, center, new Vector(0,0), mustLinkBeModifiedPredicate, shift);
        return shifts;
    }

    private Object findResizeCenter(LinkResizeModification modification) {
        Stream<Object> objectStream = model.getElements().stream()
                .filter(Object.class::isInstance).map(Object.class::cast);
        return switch (modification) {
            case INCREMENT_WIDTH, DECREMENT_WIDTH -> objectStream.min(Comparator.comparing(this::findElementX)).orElseThrow();
            case INCREMENT_HEIGHT, DECREMENT_HEIGHT -> objectStream.min(Comparator.comparing(this::findElementY)).orElseThrow();
        };
    }

    private int findElementX(GraphElement object) {
        return model.getPositions().get(object).x();
    }

    private int findElementY(GraphElement object) {
        return model.getPositions().get(object).y();
    }

    private Predicate<Link> makeMustLinkBeModifiedPredicate(Link baseLink, LinkResizeModification modification) {
        Function<Link,Integer> linkCoordinate = findLinkCoordinate(modification);
        Integer baseCoordinate = linkCoordinate.apply(baseLink);
        return link -> link.getOrigin() instanceof Object
                && link.getDestination() instanceof Object
                && linkCoordinate.apply(link) == baseCoordinate.intValue();
    }

    private Function<Link, Integer> findLinkCoordinate(LinkResizeModification modification) {
        return switch (modification) {
            case INCREMENT_WIDTH, DECREMENT_WIDTH -> this::findLinkWidth;
            case INCREMENT_HEIGHT, DECREMENT_HEIGHT -> this::findLinkHeight;
        };
    }

    private int findLinkWidth(Link link) {
        return Math.abs(findElementX(link.getDestination()) - findElementX(link.getOrigin()));
    }

    private int findLinkHeight(Link link) {
        return Math.abs(findElementY(link.getDestination()) - findElementY(link.getOrigin()));
    }

    private Vector findModificationShift(LinkResizeModification modification) {
        return switch (modification) {
            case INCREMENT_WIDTH -> new Vector(1, 0);
            case DECREMENT_WIDTH -> new Vector(-1, 0);
            case INCREMENT_HEIGHT -> new Vector(0, 1);
            case DECREMENT_HEIGHT -> new Vector(0, -1);
        };
    }

    private void fillObjectShiftsAround(Map<GraphElement, Vector> shifts, GraphElement baseElement, Vector baseShift, Predicate<Link> mustLinkBeModifiedPredicate, Vector shift) {
        if (baseElement instanceof Object || baseElement instanceof Completion) {
            if (shifts.containsKey(baseElement)) {
                return;
            }
            shifts.put(baseElement, baseShift);
        }

        List<Completion> completions = listCompletionsAround(baseElement);
        for (Completion completion: completions) {
            fillObjectShiftsAround(shifts, completion, baseShift, mustLinkBeModifiedPredicate, shift);
        }

        List<Link> links = listLinksAround(baseElement);
        for (Link link: links) {

            GraphElement otherEnd = (link.getOrigin() == baseElement) ? link.getDestination() : link.getOrigin();
            boolean mustLinkBeModified = mustLinkBeModifiedPredicate.test(link);
            Vector newShift = mustLinkBeModified ? baseShift.translate(shift) : baseShift;
            fillObjectShiftsAround(shifts, otherEnd, newShift, mustLinkBeModifiedPredicate, shift);
        }
    }

    private List<Completion> listCompletionsAround(GraphElement element) {
        return model.getElements().stream()
                .filter(Completion.class::isInstance).map(Completion.class::cast)
                .filter(completion -> completion.getBase() == element)
                .toList();
    }

    private List<Link> listLinksAround(GraphElement element) {
        return model.getElements().stream()
                .filter(Link.class::isInstance).map(Link.class::cast)
                .filter(link -> link.getOrigin() == element || link.getDestination() == element)
                .toList();
    }
}
