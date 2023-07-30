package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.geometry.Vector;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.reference.LinkDirection;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class GeometryManager {

    private final Drawing model;

    public static final int OBJECT_RADIUS = 10;

    public static final int CIRCLE_RADIUS = 6;

    // the element types in the order they are displayed
    public static final List<Class<? extends GraphElement>> DISPLAYED_ELEMENT_TYPES = List.of(Link.class, Completion.class, Quantity.class, Object.class);

    public GeometryManager(Drawing model) {
        this.model = model;
    }

    public Position findElementPosition(GraphElement element) {
        if (element instanceof Link link) {
            return findLinkPosition(link);
        }
        return model.getPositions().get(element);
    }

    private Position findLinkPosition(Link link) {
        Position center = model.getPositions().get(link);
        if (center != null) {
            return center;
        }
        var position1 = findElementPosition(link.getOrigin());
        var position2 = findElementPosition(link.getDestination());
        return Position.middle(position1, position2);
    }

    public Position findVertexPosition(GraphElement element, LinkDirection linkDirection) {
        if (element instanceof Link link) {
            return findLinkVertexPosition(link, linkDirection);
        }
        return model.getPositions().get(element);
    }

    private Position findLinkVertexPosition(Link link, LinkDirection linkDirection) {
        return switch (linkDirection) {
            case DIRECT -> {
                var position1 = findVertexPosition(link.getOrigin(), link.getOriginLinkDirection());
                var center = model.getPositions().get(link);
                if (center != null) {
                    yield Position.middle(position1, center);
                }
                var position2 = findVertexPosition(link.getDestination(), link.getDestinationLinkDirection());
                yield findFirstQuarter(position1, position2);
            }
            case REVERSE -> {
                var position2 = findVertexPosition(link.getDestination(), link.getDestinationLinkDirection());
                var center = model.getPositions().get(link);
                if (center != null) {
                    yield Position.middle(center, position2);
                }
                var position1 = findVertexPosition(link.getOrigin(), link.getOriginLinkDirection());
                yield findFirstQuarter(position2, position1);
            }
        };
    }

    private static Position findFirstQuarter(Position p1, Position p2) {
        return new Position((p1.x()*3 + p2.x())/4, (p1.y()*3 + p2.y())/4);
    }

    public Position computeArrowMeetingPositionWithElement(Position position1, Position position2, GraphElement element) {
        return switch (element) {
            case Object ignored -> computeArrowMeetingPositionWithObject(position1, position2);
            case Completion ignored -> computeArrowMeetingPositionWithCircle(position1, position2);
            case Quantity ignored -> computeArrowMeetingPositionWithCircle(position1, position2);
            case Link ignored -> position2;
        };
    }

    private static Position computeArrowMeetingPositionWithObject(Position position1, Position position2) {
        var vector = Vector.between(position1, position2);
        boolean isHorizontal = Math.abs(vector.x()) > Math.abs(vector.y());
        if (isHorizontal) {
            if (vector.x() > 0) {
                return new Position(position2.x() - OBJECT_RADIUS, position2.y() - OBJECT_RADIUS * vector.y() / vector.x());
            }
            else if (vector.x() < 0) {
                return new Position(position2.x() + OBJECT_RADIUS, position2.y() + OBJECT_RADIUS * vector.y() / vector.x());
            }
        }
        else {
            if (vector.y() > 0) {
                return new Position(position2.x() - OBJECT_RADIUS * vector.x() / vector.y(), position2.y() - OBJECT_RADIUS);
            }
            else if (vector.y() < 0) {
                return new Position(position2.x() + OBJECT_RADIUS * vector.x() / vector.y(), position2.y() + OBJECT_RADIUS);
            }
        }
        return position2;
    }

    private Position computeArrowMeetingPositionWithCircle(Position position1, Position position2) {
        var vector = Vector.between(position1, position2);
        Vector relativeArrow = vector.multiply((float) -CIRCLE_RADIUS / vector.length());
        return position2.translate(relativeArrow);
    }

    public LinkDirection findLinkDirectionAtPosition(GraphElement element, Position position) {
        if (element instanceof Link link) {

            var position1 = findVertexPosition(link.getOrigin(), link.getOriginLinkDirection());
            var position2 = findVertexPosition(link.getDestination(), link.getDestinationLinkDirection());
            var center = findElementPosition(link);

            int distanceFromFirstHalf = findPositionDistanceFromLine(position, position1, center);
            int distanceFromSecondHalf = findPositionDistanceFromLine(position, center, position2);
            if (distanceFromFirstHalf <= distanceFromSecondHalf) {
                return LinkDirection.DIRECT;
            }
            else {
                return LinkDirection.REVERSE;
            }

        }
        return null;
    }

    public GraphElement findElementAtPosition(Position position) {
        // we look for an element in the reverse orrder they are drawn
        for (int i=DISPLAYED_ELEMENT_TYPES.size()-1 ; i >= 0 ; i--) {
            Class<? extends GraphElement> elementType = DISPLAYED_ELEMENT_TYPES.get(i);
            GraphElement element = findElementOfTypeAtPosition(position, elementType);
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    public GraphElement findElementOfTypeAtPosition(Position position, Class<? extends GraphElement> type) {
        Function<GraphElement,Integer> computeDistanceFunction = (element -> computePositionDistanceFromElement(position, element));
        return model.getElements().stream()
                .filter(type::isInstance)
                .filter(element -> computeDistanceFunction.apply(element) < OBJECT_RADIUS)
                .min(Comparator.comparing(computeDistanceFunction))
                .orElse(null);
    }

    private int computePositionDistanceFromElement(Position position, GraphElement element) {
        if (element instanceof Link link) {
            return findPositionDistanceFromLink(position, link);
        }

        Position elementPosition = model.getPositions().get(element);
        var relativePosition = Vector.between(elementPosition, position);
        return relativePosition.infiniteNormLength();
    }

    private int findPositionDistanceFromLink(Position position, Link link) {

        var position1 = findVertexPosition(link.getOrigin(), link.getOriginLinkDirection());
        var position2 = findVertexPosition(link.getDestination(), link.getDestinationLinkDirection());
        var center = model.getPositions().get(link);

        if (center == null) {
            return findPositionDistanceFromLine(position, position1, position2);
        }

        int distanceFromFirstHalf = findPositionDistanceFromLine(position, position1, center);
        int distanceFromSecondHalf = findPositionDistanceFromLine(position, center, position2);
        return Math.min(distanceFromFirstHalf, distanceFromSecondHalf);
    }

    private int findPositionDistanceFromLine(Position position, Position lineOrigin, Position lineDestination) {

        var positionVector = Vector.between(lineOrigin, position);
        var linkVector = Vector.between(lineOrigin, lineDestination);
        float linkLength = Math.round(linkVector.length());

        float relativePositionAlongVector = Vector.scalarProduct(positionVector, linkVector) / linkLength / linkLength;
        if (relativePositionAlongVector < 0) {
            return (int)Position.distance(lineOrigin, position);
        }
        else if (relativePositionAlongVector > 1) {
            return (int)Position.distance(lineDestination, position);
        }

        float distanceFromLine = Math.abs(Vector.discriminant(positionVector, linkVector)) / linkLength;
        return (int)distanceFromLine;
    }
}
