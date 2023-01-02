package fr.alphonse.drawingpad.view;

import fr.alphonse.drawingpad.data.Example;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.geometry.Vector;
import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.Vertex;
import fr.alphonse.drawingpad.document.utils.ChangeDetector;
import fr.alphonse.drawingpad.view.internal.ModelHandler;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DrawingComponent extends JComponent {

    private Example model;

    private final ChangeDetector changeDetector;

    private final java.util.List<Vertex.Id> selectedVertices = new ArrayList<>();

    private Map<Object.Id, Vector> dragRelativeVectors;

    private boolean canDrag = false;

    private boolean hasDragged = false;

    private boolean hasDraggedObjects = false;

    private Vertex lastSelectedVertex = null;

    private Position selectionRectangleOrigin = null;

    private Position selectionRectangleDestination = null;

    private final List<Integer> guidesX = new ArrayList<>();

    private final List<Integer> guidesY = new ArrayList<>();

    private static final int OBJECT_RECTANGLE_RADIUS = 8;

    private static final int OBJECT_RADIUS = 10;

    private static final double ARROW_ANGLE = Math.toRadians(25);

    private static final double ARROW_LENGTH = 13;

    private static final BasicStroke SHADOW_STROKE = new BasicStroke(3);

    private static final BasicStroke SELECTED_LINK_STROKE = new BasicStroke(3);

    private static final BasicStroke BASIC_STROKE = new BasicStroke(1);

    private static final double LOOP_ANGLE = Math.toRadians(23);

    private static final int LOOP_LENGTH = 25;

    private static final int LOOP_CIRCLE_RADIUS = (int)(LOOP_LENGTH * Math.tan(LOOP_ANGLE));

    private static final int LOOP_CENTER_DISTANCE = (int)(LOOP_LENGTH / Math.cos(LOOP_ANGLE));

    private static final int ARROW_KEY_DELTA = 1;

    private static final Vector ARROW_KEY_UP_DELTA = new Vector(0, -ARROW_KEY_DELTA);

    private static final Vector ARROW_KEY_DOWN_DELTA = new Vector(0, ARROW_KEY_DELTA);

    private static final Vector ARROW_KEY_LEFT_DELTA = new Vector(-ARROW_KEY_DELTA, 0);

    private static final Vector ARROW_KEY_RIGHT_DELTA = new Vector(ARROW_KEY_DELTA, 0);

    private static final Color SELECTION_COLOR = Color.getHSBColor(206f/360, 1f, .9f);

    private static final int GUIDE_MAGNETISM_RADIUS = 3;

    public DrawingComponent(Example model, ChangeDetector changeDetector) {
        super();
        this.model = model;
        this.changeDetector = changeDetector;
        setBackground(Color.WHITE);
        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
                DrawingComponent.this.reactToClick(e);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (selectionRectangleOrigin != null && selectionRectangleDestination != null) {
                    DrawingComponent.this.repaint();
                }
                DrawingComponent.this.selectionRectangleOrigin = null;
                DrawingComponent.this.selectionRectangleDestination = null;
                if (!DrawingComponent.this.guidesX.isEmpty() || !DrawingComponent.this.guidesY.isEmpty()) {
                    DrawingComponent.this.repaint();
                }
                DrawingComponent.this.clearGuides();
                if (!hasDragged && lastSelectedVertex != null && DrawingComponent.this.selectedVertices.size() > 1 && (event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
                    DrawingComponent.this.selectedVertices.removeIf(Predicate.not(Predicate.isEqual(lastSelectedVertex.getId())));
                    DrawingComponent.this.repaint();
                }
                if (hasDraggedObjects) {
                    changeDetector.notifyChange();
                }
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
        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                DrawingComponent.this.reactToKey(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
        this.setFocusable(true);
    }

    public void changeModel(Example model) {
        this.model = model;
        this.changeDetector.reinitModel(model);
        this.repaint();
    }

    public void delete() {
        for (Vertex.Id selectedVertex: selectedVertices) {
            switch (selectedVertex) {
                case Object.Id objectId -> ModelHandler.deleteObject(objectId, model);
                case Link.Id linkId -> ModelHandler.deleteLink(linkId, model);
            }
        }
        this.selectedVertices.clear();
        lastSelectedVertex = null;
        this.changeDetector.notifyChange();
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        ((Graphics2D)g).setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        int translationX = getWidth() / 2;
        int translationY = getHeight() / 2;
        g.translate(translationX, translationY);

        if (this.selectionRectangleOrigin != null && this.selectionRectangleDestination != null) {
            g.setColor(Color.LIGHT_GRAY);
            var originX = Math.min(selectionRectangleOrigin.x(), selectionRectangleDestination.x());
            var originY = Math.min(selectionRectangleOrigin.y(), selectionRectangleDestination.y());
            var width = Math.abs(selectionRectangleOrigin.x() - selectionRectangleDestination.x());
            var height = Math.abs(selectionRectangleOrigin.y() - selectionRectangleDestination.y());
            g.fillRect(originX, originY, width, height);
        }

        g.setColor(Color.blue);
        for (Integer guideX: guidesX) {
            g.drawLine(guideX, -translationY, guideX, translationY);
        }
        for (Integer guideY: guidesY) {
            g.drawLine(-translationX, guideY, translationX, guideY);
        }

        g.setColor(Color.BLACK);
        Map<Object.Id, Position> positions = model.getPositions();
        for (Object object: model.getObjects().values()) {
            var position = positions.get(object.getId());

            g.setColor(Color.GRAY);
            ((Graphics2D)g).setStroke(SHADOW_STROKE);
            g.drawLine(position.x()-OBJECT_RECTANGLE_RADIUS+2, position.y()+OBJECT_RECTANGLE_RADIUS, position.x()+OBJECT_RECTANGLE_RADIUS, position.y()+OBJECT_RECTANGLE_RADIUS);
            g.drawLine(position.x()+OBJECT_RECTANGLE_RADIUS, position.y()-OBJECT_RECTANGLE_RADIUS+2, position.x()+OBJECT_RECTANGLE_RADIUS, position.y()+OBJECT_RECTANGLE_RADIUS);
            ((Graphics2D)g).setStroke(BASIC_STROKE);

            if (selectedVertices.contains(object.getId())) {
                g.setColor(SELECTION_COLOR);
            }
            else {
                g.setColor(Color.BLACK);
            }
            g.fillRect(position.x()-OBJECT_RECTANGLE_RADIUS, position.y()-OBJECT_RECTANGLE_RADIUS, 2*OBJECT_RECTANGLE_RADIUS, 2*OBJECT_RECTANGLE_RADIUS);
        }

        for (Link link: model.getLinks().values()) {
            if (selectedVertices.contains(link.getId())) {
                g.setColor(SELECTION_COLOR);
                ((Graphics2D)g).setStroke(SELECTED_LINK_STROKE);
            }
            else {
                g.setColor(Color.BLACK);
                ((Graphics2D)g).setStroke(BASIC_STROKE);
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
        return link.getOrigin().equals(link.getDestination());
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
        };
    }

    private Position findObjectPosition(Object object) {
        return model.getPositions().get(object.getId());
    }

    private Position computeArrowMeetingPositionWithVertex(Position position1, Position position2, Vertex vertex) {
        return switch (vertex) {
            case Object ignored -> computeArrowMeetingPositionWithObject(position1, position2);
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
        var selectedVertex = findVertexAtPosition(position);
        this.lastSelectedVertex = selectedVertex;
        boolean isShiftKeyPressed = (event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
        if (selectedVertex == null && !isShiftKeyPressed) {
            this.selectionRectangleOrigin = position;
        }
        boolean alreadySelected = selectedVertex != null && selectedVertices.contains(selectedVertex.getId());
        this.canDrag = !(isShiftKeyPressed && (selectedVertex == null || alreadySelected));
        this.hasDragged = false;
        this.hasDraggedObjects = false;
        if (alreadySelected && isShiftKeyPressed) {
            selectedVertices.remove(selectedVertex.getId());
        }
        if (!isShiftKeyPressed && !alreadySelected) {
            DrawingComponent.this.selectedVertices.clear();
        }
        if (selectedVertex != null && !alreadySelected) {
            this.selectedVertices.add(selectedVertex.getId());
        }
        this.dragRelativeVectors = this.selectedVertices.stream()
                .filter(vertex -> vertex instanceof Object.Id)
                .map(vertex -> (Object.Id)vertex)
                .collect(Collectors.toMap(Function.identity(), object -> Vector.between(position, model.getPositions().get(object))));
        if (alreadySelected && !isShiftKeyPressed) {
            return;
        }
        this.repaint();
    }

    private Vertex findVertexAtPosition(Position position) {
        Object object = findVertexAtPositionInList(position, model.getObjects().values());
        if (object != null) {
            return object;
        }
        return findVertexAtPositionInList(position, model.getLinks().values());
    }

    private <T extends Vertex> T findVertexAtPositionInList(Position position, Collection<T> vertices) {
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
        var position = new Position(event.getX() - this.getBounds().width/2, event.getY() - this.getBounds().height/2);
        if (selectionRectangleOrigin != null) {
            this.selectionRectangleDestination = position;
            List<Object.Id> objectsInRectangle = model.getObjects().keySet().stream()
                    .filter(object -> isInRectangleBetweenPoints(model.getPositions().get(object), selectionRectangleOrigin, selectionRectangleDestination))
                    .toList();
            this.selectedVertices.clear();
            this.selectedVertices.addAll(objectsInRectangle);
            addLinksBetweenVertices(this.selectedVertices);
            this.repaint();
            return;
        }
        if (!canDrag) {
            return;
        }
        this.hasDragged = true;
        boolean needsRepaint = false;
        for (Vertex.Id selectedVertex: selectedVertices) {
            if (selectedVertex instanceof Object.Id object) {
                model.getPositions().put(object, position.translate(this.dragRelativeVectors.get(object)));
                needsRepaint = true;
            }
        }
        if (needsRepaint) {
            List<Integer> nearbyGuidesX = findNearbyGuideDeltas(Position::x);
            List<Integer> nearbyGuidesY = findNearbyGuideDeltas(Position::y);
            if (Math.max(nearbyGuidesX.size(), nearbyGuidesY.size()) >= 1) {
                int deltaX = nearbyGuidesX.isEmpty() ? 0 : nearbyGuidesX.get(0);
                int deltaY = nearbyGuidesY.isEmpty() ? 0 : nearbyGuidesY.get(0);
                Vector magnetismVector = new Vector(deltaX, deltaY);
                for (Vertex.Id selectedVertex: selectedVertices) {
                    if (selectedVertex instanceof Object.Id object) {
                        model.getPositions().put(object, model.getPositions().get(object).translate(magnetismVector));
                    }
                }
            }
            fillGuides();
            this.repaint();
            this.hasDraggedObjects = true;
        }
    }

    private boolean isInRectangleBetweenPoints(Position position, Position corner1, Position corner2) {
        return position.x() >= Math.min(corner1.x(), corner2.x()) &&
                position.y() >= Math.min(corner1.y(), corner2.y()) &&
                position.x() < Math.max(corner1.x(), corner2.x()) &&
                position.y() < Math.max(corner1.y(), corner2.y());
    }

    private void addLinksBetweenVertices(List<Vertex.Id> vertices) {
        List<Link.Id> linksToAdd;
        do {
            linksToAdd = model.getLinks().values().stream()
                    .filter(link -> !vertices.contains(link.getId()) && vertices.contains(link.getOrigin().getId()) && vertices.contains(link.getDestination().getId()))
                    .map(Link::getId)
                    .toList();
            vertices.addAll(linksToAdd);
        } while (!linksToAdd.isEmpty());
    }

    private List<Integer> findNearbyGuideDeltas(Function<Position, Integer> coordinate) {
        List<Position> selectedPositions = selectedVertices.stream()
                .filter(vertex -> vertex instanceof Object.Id)
                .map(vertex -> (Object.Id)vertex)
                .map(model.getPositions()::get)
                .toList();
        return this.model.getObjects().keySet().stream()
                .filter(Predicate.not(selectedVertices::contains))
                .map(model.getPositions()::get)
                .map(coordinate)
                .flatMap(value -> selectedPositions.stream().map(position -> value - coordinate.apply(position)))
                .filter(delta -> Math.abs(delta) <= GUIDE_MAGNETISM_RADIUS)
                .distinct()
                .sorted()
                .toList();
    }

    private void fillGuides() {
        List<Position> selectedPositions = selectedVertices.stream()
                .filter(vertex -> vertex instanceof Object.Id)
                .map(vertex -> (Object.Id)vertex)
                .map(model.getPositions()::get)
                .toList();
        this.guidesX.clear();
        this.guidesX.addAll(this.model.getObjects().keySet().stream()
                .filter(Predicate.not(selectedVertices::contains))
                .map(model.getPositions()::get)
                .map(Position::x)
                .filter(x -> selectedPositions.stream().anyMatch(position -> position.x() == x))
                .distinct()
                .toList());
        this.guidesY.clear();
        this.guidesY.addAll(this.model.getObjects().keySet().stream()
                .filter(Predicate.not(selectedVertices::contains))
                .map(model.getPositions()::get)
                .map(Position::y)
                .filter(y -> selectedPositions.stream().anyMatch(position -> position.y() == y))
                .distinct()
                .toList());
    }

    private void clearGuides() {
        this.guidesX.clear();
        this.guidesY.clear();
    }

    private void reactToKey(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.VK_UP -> moveSelectedVertexBy(ARROW_KEY_UP_DELTA);
            case KeyEvent.VK_DOWN -> moveSelectedVertexBy(ARROW_KEY_DOWN_DELTA);
            case KeyEvent.VK_LEFT -> moveSelectedVertexBy(ARROW_KEY_LEFT_DELTA);
            case KeyEvent.VK_RIGHT -> moveSelectedVertexBy(ARROW_KEY_RIGHT_DELTA);
        }
    }

    private void moveSelectedVertexBy(Vector delta) {
        boolean needsRefresh = false;
        for (Vertex.Id selectedVertex: selectedVertices) {
            if (selectedVertex instanceof Object.Id object) {
                this.model.getPositions().put(object, this.model.getPositions().get(object).translate(delta));
                needsRefresh = true;
            }
        }
        if (needsRefresh) {
            fillGuides();
            Timer timer = new Timer(300, action -> {
                DrawingComponent.this.clearGuides();
                DrawingComponent.this.repaint();
            });
            timer.start();
            this.repaint();
            this.changeDetector.notifyChange();
        }
    }
}
