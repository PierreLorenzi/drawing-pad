package fr.alphonse.drawingpad.view;

import fr.alphonse.drawingpad.data.Example;
import fr.alphonse.drawingpad.data.Position;
import fr.alphonse.drawingpad.model.Link;
import fr.alphonse.drawingpad.model.Object;
import fr.alphonse.drawingpad.model.Vertex;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class DrawingComponent extends JComponent {

    private Example model;

    private static final int OBJECT_RADIUS = 6;

    private static final double ARROW_ANGLE = Math.toRadians(25);

    private static final double ARROW_LENGTH = 12;

    public DrawingComponent() {
        super();
        setBackground(Color.WHITE);
    }

    public void setModel(Example model) {
        this.model = model;
    }

    @Override
    protected void paintComponent(Graphics g) {
        int translationX = getWidth() / 2;
        int translationY = getHeight() / 2;
        g.translate(translationX, translationY);

        g.setColor(Color.BLACK);
        Map<Object, Position> positions = model.getPositions();
        for (Object object: model.getObjects()) {
            var position = positions.get(object);
            g.fillRect(position.x()-OBJECT_RADIUS, position.y()-OBJECT_RADIUS, 2*OBJECT_RADIUS, 2*OBJECT_RADIUS);
        }

        for (Link link: model.getLinks()) {
            var position1 = findVertexPosition(link.getOrigin());
            var position2 = findVertexPosition(link.getDestination());
            g.drawLine(position1.x(), position1.y(), position2.x(), position2.y());

            // draw arrow
            var lineAngle = Math.atan2(position2.y()-position1.y(), position2.x()-position1.x());
            var arrowAngle1 = lineAngle - ARROW_ANGLE;
            var arrowAngle2 = lineAngle + ARROW_ANGLE;
            var arrowPosition = (link.getDestination() instanceof Link) ? position2 : computeArrowMeetingPositionWithObject(position1, position2);
            g.drawLine(arrowPosition.x(), arrowPosition.y(), arrowPosition.x() - (int)Math.round(ARROW_LENGTH * Math.cos(arrowAngle1)) , arrowPosition.y() - (int)Math.round(ARROW_LENGTH * Math.sin(arrowAngle1)));
            g.drawLine(arrowPosition.x(), arrowPosition.y(), arrowPosition.x() - (int)Math.round(ARROW_LENGTH * Math.cos(arrowAngle2)), arrowPosition.y() - (int)Math.round(ARROW_LENGTH * Math.sin(arrowAngle2)));
        }

        g.translate(-translationX, -translationY);
    }

    private Position findVertexPosition(Vertex vertex) {
        return switch (vertex) {
            case Object ignored -> model.getPositions().get(vertex);
            case Link link -> {
                var position1 = findVertexPosition(link.getOrigin());
                var position2 = findVertexPosition(link.getDestination());
                yield new Position((position1.x() + position2.x())/2, (position1.y() + position2.y())/2);
            }
            default ->  throw new Error("Unknown vertex class " + vertex.getClass().getSimpleName());
        };
    }

    private static Position computeArrowMeetingPositionWithObject(Position position1, Position position2) {
        var x = position2.x() - position1.x();
        var y = position2.y() - position1.y();
        boolean isHorizontal = Math.abs(x) > Math.abs(y);
        if (isHorizontal) {
            if (x > 0) {
                return new Position(position2.x() - OBJECT_RADIUS, position2.y() - OBJECT_RADIUS * y / x);
            }
            else {
                return new Position(position2.x() + OBJECT_RADIUS, position2.y() + OBJECT_RADIUS * y / x);
            }
        }
        else {
            if (y > 0) {
                return new Position(position2.x() - OBJECT_RADIUS * x / y, position2.y() - OBJECT_RADIUS);
            }
            else {
                return new Position(position2.x() + OBJECT_RADIUS * x / y, position2.y() + OBJECT_RADIUS);
            }
        }
    }
}
