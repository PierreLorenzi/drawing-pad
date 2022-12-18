package fr.alphonse.drawingpad.view;

import fr.alphonse.drawingpad.data.Example;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.geometry.Vector;
import fr.alphonse.drawingpad.model.Link;
import fr.alphonse.drawingpad.model.Object;
import fr.alphonse.drawingpad.model.Vertex;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;

public class DrawingComponent extends JComponent {
    private Example model;

    private Vertex selectedVertex;

    private static final int OBJECT_RECTANGLE_RADIUS = 6;

    private static final int OBJECT_RADIUS = 8;

    private static final double ARROW_ANGLE = Math.toRadians(25);

    private static final double ARROW_LENGTH = 10;

    private static final BasicStroke SHADOW_STROKE = new BasicStroke(2);

    private static final BasicStroke BASIC_STROKE = new BasicStroke(1);

    private static final double LOOP_ANGLE = Math.toRadians(23);

    private static final int LOOP_LENGTH = 20;

    private static final int LOOP_CIRCLE_RADIUS = (int)(LOOP_LENGTH * Math.tan(LOOP_ANGLE));

    private static final int LOOP_CENTER_DISTANCE = (int)(LOOP_LENGTH / Math.cos(LOOP_ANGLE));

    public DrawingComponent() {
        super();
        setBackground(Color.WHITE);
        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                DrawingComponent.this.reactToClick(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                DrawingComponent.this.reactToDrag(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {

            }
        });
    }

    public void setModel(Example model) {
        this.model = model;
    }

    @Override
    protected void paintComponent(Graphics g) {
        ((Graphics2D)g).setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        int translationX = getWidth() / 2;
        int translationY = getHeight() / 2;
        g.translate(translationX, translationY);

        g.setColor(Color.BLACK);
        Map<Object, Position> positions = model.getPositions();
        for (Object object: model.getObjects()) {
            var position = positions.get(object);

            g.setColor(Color.GRAY);
            ((Graphics2D)g).setStroke(SHADOW_STROKE);
            g.drawLine(position.x()-OBJECT_RECTANGLE_RADIUS+2, position.y()+OBJECT_RECTANGLE_RADIUS, position.x()+OBJECT_RECTANGLE_RADIUS, position.y()+OBJECT_RECTANGLE_RADIUS);
            g.drawLine(position.x()+OBJECT_RECTANGLE_RADIUS, position.y()-OBJECT_RECTANGLE_RADIUS+2, position.x()+OBJECT_RECTANGLE_RADIUS, position.y()+OBJECT_RECTANGLE_RADIUS);
            ((Graphics2D)g).setStroke(BASIC_STROKE);

            if (object == selectedVertex) {
                g.setColor(Color.RED);
            }
            else {
                g.setColor(Color.BLACK);
            }
            g.fillRect(position.x()-OBJECT_RECTANGLE_RADIUS, position.y()-OBJECT_RECTANGLE_RADIUS, 2*OBJECT_RECTANGLE_RADIUS, 2*OBJECT_RECTANGLE_RADIUS);
        }

        for (Link link: model.getLinks()) {
            if (link == selectedVertex) {
                g.setColor(Color.RED);
            }
            else {
                g.setColor(Color.BLACK);
            }
            if (isLoop(link)) {
                drawLoop(findVertexPosition(link.getOrigin()), link.getOrigin(), g);
                continue;
            }
            var position1 = findVertexPosition(link.getOrigin());
            var position2 = findVertexPosition(link.getDestination());
            var linePosition1 = computeArrowMeetingPositionWithVertex(position2, position1, link.getOrigin());
            var linePosition2 = computeArrowMeetingPositionWithVertex(position1, position2, link.getDestination());
            g.drawLine(linePosition1.x(), linePosition1.y(), linePosition2.x(), linePosition2.y());

            drawArrow(linePosition2, position1, g);
        }

        g.translate(-translationX, -translationY);
    }

    private static boolean isLoop(Link link) {
        return link.getOrigin() == link.getDestination();
    }

    private Position findVertexPosition(Vertex vertex) {
        return switch (vertex) {
            case Object object -> findObjectPosition(object);
            case Link link -> {
                if (isLoop(link)) {
                    var extremityVector = new Vector(0, -LOOP_CENTER_DISTANCE - LOOP_CIRCLE_RADIUS);
                    var basePosition = findVertexPosition(link.getOrigin());
                    yield basePosition.translate(extremityVector);
                }
                var position1 = findVertexPosition(link.getOrigin());
                var position2 = findVertexPosition(link.getDestination());
                yield Position.middle(position1, position2);
            }
            default ->  throw new Error("Unknown vertex class " + vertex.getClass().getSimpleName());
        };
    }

    private Position findObjectPosition(Object object) {
        return model.getPositions().get(object);
    }

    private Position computeArrowMeetingPositionWithVertex(Position position1, Position position2, Vertex vertex) {
        return switch (vertex) {
            case Object ignored -> computeArrowMeetingPositionWithObject(position1, position2);
            case Link ignored -> position2;
            default ->  throw new Error("Unknown vertex class " + vertex.getClass().getSimpleName());
        };
    }

    private static Position computeArrowMeetingPositionWithObject(Position position1, Position position2) {
        var vector = Vector.between(position1, position2);
        boolean isHorizontal = Math.abs(vector.x()) > Math.abs(vector.y());
        if (isHorizontal) {
            if (vector.x() > 0) {
                return new Position(position2.x() - OBJECT_RADIUS, position2.y() - OBJECT_RADIUS * vector.y() / vector.x());
            }
            else {
                return new Position(position2.x() + OBJECT_RADIUS, position2.y() + OBJECT_RADIUS * vector.y() / vector.x());
            }
        }
        else {
            if (vector.y() > 0) {
                return new Position(position2.x() - OBJECT_RADIUS * vector.x() / vector.y(), position2.y() - OBJECT_RADIUS);
            }
            else {
                return new Position(position2.x() + OBJECT_RADIUS * vector.x() / vector.y(), position2.y() + OBJECT_RADIUS);
            }
        }
    }

    private void drawLoop(Position position, Vertex vertex, Graphics g) {
        var baseVector = new Vector(0, -LOOP_LENGTH);
        var startVector = baseVector.rotate(-LOOP_ANGLE);
        var endVector = baseVector.rotate(LOOP_ANGLE);
        var startDestination = position.translate(startVector);
        var endDestination = position.translate(endVector);
        var startDestinationAnchor = computeArrowMeetingPositionWithVertex(startDestination, position, vertex);
        var endDestinationAnchor = computeArrowMeetingPositionWithVertex(endDestination, position, vertex);
        g.drawLine(startDestinationAnchor.x(), startDestinationAnchor.y(), startDestination.x(), startDestination.y());
        g.drawLine(endDestinationAnchor.x(), endDestinationAnchor.y(), endDestination.x(), endDestination.y());

        drawArrow(endDestinationAnchor, endDestination, g);

        var centerVector = new Vector(0, -LOOP_CENTER_DISTANCE);
        var center = position.translate(centerVector);
        g.drawArc(center.x()-LOOP_CIRCLE_RADIUS, center.y()-LOOP_CIRCLE_RADIUS, LOOP_CIRCLE_RADIUS*2, LOOP_CIRCLE_RADIUS*2, (int)-Math.toDegrees(LOOP_ANGLE), (int)Math.toDegrees(Math.PI + 2 * LOOP_ANGLE));
    }

    private void drawArrow(Position position, Position origin, Graphics g) {
        Vector lineVector = Vector.between(position, origin);
        Vector baseVector = lineVector.multiply((float)ARROW_LENGTH / lineVector.length());
        var arrowVector1 = baseVector.rotate(ARROW_ANGLE);
        var arrowVector2 = baseVector.rotate(-ARROW_ANGLE);
        var arrowPosition1 = position.translate(arrowVector1);
        var arrowPosition2 = position.translate(arrowVector2);
        g.drawLine(position.x(), position.y(), arrowPosition1.x(), arrowPosition1.y());
        g.drawLine(position.x(), position.y(), arrowPosition2.x(), arrowPosition2.y());
    }



    private void reactToClick(MouseEvent event) {
        var position = new Position(event.getX() - this.getBounds().width/2, event.getY() - this.getBounds().height/2);
        this.selectedVertex = findVertexAtPosition(position);
        this.repaint();
    }

    private Vertex findVertexAtPosition(Position position) {
        Object object = findVertexAtPositionInList(position, model.getObjects());
        if (object != null) {
            return object;
        }
        return findVertexAtPositionInList(position, model.getLinks());
    }

    private <T extends Vertex> T findVertexAtPositionInList(Position position, java.util.List<T> vertices) {
        Function<Vertex,Integer> computeDistanceFunction = (vertex -> computePositionDistanceFromVertex(position, vertex));
        return vertices.stream()
                .filter(vertex -> computeDistanceFunction.apply(vertex) < OBJECT_RADIUS)
                .min(Comparator.comparing(computeDistanceFunction))
                .orElse(null);
    }

    private int computePositionDistanceFromVertex(Position position, Vertex vertex) {
        return switch (vertex) {
            case Object object -> findPositionDistanceFromObject(position, object);
            case Link link -> findPositionDistanceFromLink(position, link);
            default -> throw new Error("Unknown vertex class " + vertex.getClass().getSimpleName());
        };
    }

    private int findPositionDistanceFromObject(Position position, Object object) {
        var objectPosition = findObjectPosition(object);
        var relativePosition = Vector.between(objectPosition, position);
        return relativePosition.infiniteNormLength();
    }

    private int findPositionDistanceFromLink(Position position, Link link) {

        if (isLoop(link)) {
            var basePosition = findVertexPosition(link.getOrigin());
            var circleCenterVector = new Vector(0, -LOOP_CENTER_DISTANCE);
            var circleCenter = basePosition.translate(circleCenterVector);
            return (int)Position.distance(circleCenter, position);
        }

        var position1 = findVertexPosition(link.getOrigin());
        var position2 = findVertexPosition(link.getDestination());
        var positionVector = Vector.between(position1, position);
        var linkVector = Vector.between(position1, position2);
        float linkLength = Math.round(linkVector.length());

        float relativePositionAlongVector = Vector.scalarProduct(positionVector, linkVector) / linkLength / linkLength;
        if (relativePositionAlongVector < 0) {
            return (int)Position.distance(position1, position);
        }
        else if (relativePositionAlongVector > 1) {
            return (int)Position.distance(position2, position);
        }

        float distanceFromLine = Math.abs(Vector.discriminant(positionVector, linkVector)) / linkLength;
        return (int)distanceFromLine;
    }

    private void reactToDrag(MouseEvent event) {
        if (this.selectedVertex != null && this.selectedVertex instanceof Object object) {
            var position = new Position(event.getX() - this.getBounds().width/2, event.getY() - this.getBounds().height/2);
            model.getPositions().put(object, position);
            this.repaint();
        }
    }

}
