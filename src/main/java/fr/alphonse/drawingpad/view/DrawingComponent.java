package fr.alphonse.drawingpad.view;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.geometry.Vector;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.link.Link;
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

    private Drawing model;

    private final ChangeDetector changeDetector;

    private final List<Vertex> selectedVertices = new ArrayList<>();

    private final ChangeDetector selectionChangeDetector = new ChangeDetector(selectedVertices);

    private Map<Object, Vector> dragRelativeVectors;

    private boolean canDrag = false;

    private boolean hasDragged = false;

    private boolean hasDraggedObjects = false;

    private Vertex lastSelectedVertex = null;

    private Position selectionRectangleOrigin = null;

    private Position selectionRectangleDestination = null;

    private Vertex newLinkOrigin = null;

    private Class<? extends Link> newLinkType = null;

    private final List<Integer> guidesX = new ArrayList<>();

    private final List<Integer> guidesY = new ArrayList<>();

    private static final int OBJECT_RECTANGLE_RADIUS = 8;

    private static final int OBJECT_RADIUS = 10;

    private static final float ARROW_DISTANCE = 20;

    private static final double ARROW_ANGLE = Math.toRadians(25);

    private static final float ARROW_LENGTH = 10;

    private static final BasicStroke SHADOW_STROKE = new BasicStroke(3);

    private static final BasicStroke SELECTED_LINK_STROKE = new BasicStroke(3);

    private static final BasicStroke BASIC_STROKE = new BasicStroke(1);

    private static final int ARROW_KEY_DELTA = 1;

    private static final Vector ARROW_KEY_UP_DELTA = new Vector(0, -ARROW_KEY_DELTA);

    private static final Vector ARROW_KEY_DOWN_DELTA = new Vector(0, ARROW_KEY_DELTA);

    private static final Vector ARROW_KEY_LEFT_DELTA = new Vector(-ARROW_KEY_DELTA, 0);

    private static final Vector ARROW_KEY_RIGHT_DELTA = new Vector(ARROW_KEY_DELTA, 0);

    private static final Color SELECTION_COLOR = Color.getHSBColor(206f/360, 1f, .9f);

    private static final int GUIDE_MAGNETISM_RADIUS = 3;

    public DrawingComponent(Drawing model, ChangeDetector changeDetector) {
        super();
        this.model = model;
        this.changeDetector = changeDetector;
        changeDetector.addListener(this, DrawingComponent::repaint);
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
                DrawingComponent.this.reactToRelease(event);
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

    public void changeModel(Drawing model) {
        this.model = model;
        this.changeDetector.reinitModel(model);
        this.repaint();
    }

    public java.util.List<Vertex> getSelection() {
        return selectedVertices;
    }

    public ChangeDetector getSelectionChangeDetector() {
        return selectionChangeDetector;
    }

    public void delete() {
        for (Vertex selectedVertex: selectedVertices) {
            switch (selectedVertex) {
                case Object object -> ModelHandler.deleteObject(object, model);
                case PossessionLink possessionLink -> ModelHandler.deletePossessionLink(possessionLink, model);
                case ComparisonLink comparisonLink -> ModelHandler.deleteComparisonLink(comparisonLink, model);
                case WholeValue ignored -> throw new Error("Whole values not handled as real vertices");
                case LowerValue ignored -> throw new Error("Lower values not handled as real vertices");
            }
        }
        this.selectedVertices.clear();
        this.selectionChangeDetector.notifyChange();
        lastSelectedVertex = null;
        this.changeDetector.notifyChange();
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
        Map<Object, Position> positions = model.getPositions();
        for (Object object: model.getGraph().getObjects()) {
            var position = positions.get(object);

            g.setColor(Color.GRAY);
            ((Graphics2D)g).setStroke(SHADOW_STROKE);
            g.drawLine(position.x()-OBJECT_RECTANGLE_RADIUS+2, position.y()+OBJECT_RECTANGLE_RADIUS, position.x()+OBJECT_RECTANGLE_RADIUS, position.y()+OBJECT_RECTANGLE_RADIUS);
            g.drawLine(position.x()+OBJECT_RECTANGLE_RADIUS, position.y()-OBJECT_RECTANGLE_RADIUS+2, position.x()+OBJECT_RECTANGLE_RADIUS, position.y()+OBJECT_RECTANGLE_RADIUS);
            ((Graphics2D)g).setStroke(BASIC_STROKE);

            if (selectedVertices.contains(object)) {
                g.setColor(SELECTION_COLOR);
            }
            else {
                g.setColor(Color.BLACK);
            }
            g.fillRect(position.x()-OBJECT_RECTANGLE_RADIUS, position.y()-OBJECT_RECTANGLE_RADIUS, 2*OBJECT_RECTANGLE_RADIUS, 2*OBJECT_RECTANGLE_RADIUS);
        }

        for (PossessionLink possessionLink : model.getGraph().getPossessionLinks()) {
            boolean isSelected = selectedVertices.contains(possessionLink);
            drawLink(possessionLink, g, isSelected);
        }

        for (ComparisonLink comparisonLink : model.getGraph().getComparisonLinks()) {
            boolean isSelected = selectedVertices.contains(comparisonLink);
            drawLink(comparisonLink, g, isSelected);
        }

        ((Graphics2D)g).setStroke(BASIC_STROKE);

        // draw link being dragged
        if (newLinkOrigin != null) {
            var position1 = findVertexPosition(newLinkOrigin);
            Position position2 = findMousePosition();
            var linePosition1 = computeArrowMeetingPositionWithVertex(position2, position1, newLinkOrigin);
            drawLinkBetweenPositions(linePosition1, position2, newLinkType, g, false);
        }

        g.translate(-translationX, -translationY);
    }

    private void drawLink(Link link, Graphics g, boolean isSelected) {
        var position1 = findVertexPosition(link.getOrigin());
        var position2 = findVertexPosition(link.getDestination());
        var linePosition1 = computeArrowMeetingPositionWithVertex(position2, position1, link.getOrigin());
        var linePosition2 = computeArrowMeetingPositionWithVertex(position1, position2, link.getDestination());
        drawLinkBetweenPositions(linePosition1, linePosition2, link.getClass(), g, isSelected);
    }

    private void drawLinkBetweenPositions(Position linePosition1, Position linePosition2, Class<? extends  Link> type, Graphics g, boolean isSelected) {
        if (isSelected) {
            g.setColor(SELECTION_COLOR);
            ((Graphics2D) g).setStroke(SELECTED_LINK_STROKE);
        }
        else {
            Color color = (type.equals(PossessionLink.class)) ? Color.BLACK : Color.GRAY;
            g.setColor(color);
            ((Graphics2D) g).setStroke(BASIC_STROKE);
        }
        g.drawLine(linePosition1.x(), linePosition1.y(), linePosition2.x(), linePosition2.y());
        if (type.equals(PossessionLink.class)) {
            drawArrow(linePosition1, linePosition2, g);
        }
    }

    private Position findMousePosition() {
        Point mousePoint = MouseInfo.getPointerInfo().getLocation();
        Point point = new Point(mousePoint);
        SwingUtilities.convertPointFromScreen(point, this);
        return findPointPosition(point);
    }

    private Position findVertexPosition(Vertex vertex) {
        return switch (vertex) {
            case Object object -> findObjectPosition(object);
            case PossessionLink possessionLink -> {
                var position1 = findVertexPosition(possessionLink.getOrigin());
                var position2 = findVertexPosition(possessionLink.getDestination());
                yield Position.middle(position1, position2);
            }
            case ComparisonLink comparisonLink -> {
                var position1 = findVertexPosition(comparisonLink.getOrigin());
                var position2 = findVertexPosition(comparisonLink.getDestination());
                yield Position.middle(position1, position2);
            }
            case WholeValue ignored -> throw new Error("Whole values not handled as real vertices");
            case LowerValue ignored -> throw new Error("Lower values not handled as real vertices");
        };
    }

    private Position findObjectPosition(Object object) {
        return model.getPositions().get(object);
    }

    private Position computeArrowMeetingPositionWithVertex(Position position1, Position position2, Vertex vertex) {
        return switch (vertex) {
            case Object ignored -> computeArrowMeetingPositionWithObject(position1, position2);
            case PossessionLink ignored -> position2;
            case ComparisonLink ignored -> position2;
            case WholeValue ignored -> throw new Error("Whole values not handled as real vertices");
            case LowerValue ignored -> throw new Error("Lower values not handled as real vertices");
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

    private void drawArrow(Position origin, Position destination, Graphics g) {
        Vector lineVector = Vector.between(origin, destination);
        Vector arrowBaseVector = lineVector.multiply(ARROW_DISTANCE / lineVector.length());
        Position arrowBase = origin.translate(arrowBaseVector);

        Vector arrowFeatherBaseVector = lineVector.multiply(-ARROW_LENGTH / lineVector.length());
        var arrowVector1 = arrowFeatherBaseVector.rotate(ARROW_ANGLE);
        var arrowVector2 = arrowFeatherBaseVector.rotate(-ARROW_ANGLE);
        var arrowPosition1 = arrowBase.translate(arrowVector1);
        var arrowPosition2 = arrowBase.translate(arrowVector2);
        g.drawLine(arrowBase.x(), arrowBase.y(), arrowPosition1.x(), arrowPosition1.y());
        g.drawLine(arrowBase.x(), arrowBase.y(), arrowPosition2.x(), arrowPosition2.y());
    }

    private void reactToClick(MouseEvent event) {
        Position position = findEventPosition(event);
        var selectedVertex = findVertexAtPosition(position);
        // if press with command, add object or possession link
        if ((event.getModifiersEx() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) != 0) {
            if (selectedVertex == null) {
                ModelHandler.addObject(position, model);
                changeDetector.notifyChange();
            }
            else {
                newLinkOrigin = selectedVertex;
                newLinkType = PossessionLink.class;
            }
            return;
        }
        // if press with option, add comparison link
        if ((event.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0) {
            if (selectedVertex == null) {
                return;
            }
            newLinkOrigin = selectedVertex;
            newLinkType = ComparisonLink.class;
            return;
        }
        this.lastSelectedVertex = selectedVertex;
        boolean isShiftKeyPressed = (event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
        if (selectedVertex == null && !isShiftKeyPressed) {
            this.selectionRectangleOrigin = position;
        }
        boolean alreadySelected = selectedVertex != null && selectedVertices.contains(selectedVertex);
        this.canDrag = !(isShiftKeyPressed && (selectedVertex == null || alreadySelected));
        this.hasDragged = false;
        this.hasDraggedObjects = false;
        if (alreadySelected && isShiftKeyPressed) {
            selectedVertices.remove(selectedVertex);
            this.selectionChangeDetector.notifyChange();
        }
        if (!isShiftKeyPressed && !alreadySelected) {
            DrawingComponent.this.selectedVertices.clear();
            this.selectionChangeDetector.notifyChange();
        }
        if (selectedVertex != null && !alreadySelected) {
            this.selectedVertices.add(selectedVertex);
            this.selectionChangeDetector.notifyChange();
        }
        this.dragRelativeVectors = this.selectedVertices.stream()
                .filter(vertex -> vertex instanceof Object)
                .map(vertex -> (Object)vertex)
                .collect(Collectors.toMap(Function.identity(), object -> Vector.between(position, model.getPositions().get(object))));
        if (alreadySelected && !isShiftKeyPressed) {
            return;
        }
        this.repaint();
    }

    private Position findEventPosition(MouseEvent event) {
        return findPointPosition(event.getPoint());
    }

    private Position findPointPosition(Point point) {
        return new Position(point.x - this.getBounds().width/2, point.y - this.getBounds().height/2);
    }

    private Vertex findVertexAtPosition(Position position) {
        Object object = findVertexAtPositionInList(position, model.getGraph().getObjects());
        if (object != null) {
            return object;
        }
        PossessionLink possessionLink = findVertexAtPositionInList(position, model.getGraph().getPossessionLinks());
        if (possessionLink != null) {
            return possessionLink;
        }
        return findVertexAtPositionInList(position, model.getGraph().getComparisonLinks());
    }

    private <T extends Vertex> T findVertexAtPositionInList(Position position, List<T> vertices) {
        Function<Vertex,Integer> computeDistanceFunction = (vertex -> computePositionDistanceFromVertex(position, vertex));
        return vertices.stream()
                .filter(vertex -> computeDistanceFunction.apply(vertex) < OBJECT_RADIUS)
                .min(Comparator.comparing(computeDistanceFunction))
                .orElse(null);
    }

    private int computePositionDistanceFromVertex(Position position, Vertex vertex) {
        return switch (vertex) {
            case Object object -> findPositionDistanceFromObject(position, object);
            case PossessionLink possessionLink -> findPositionDistanceFromLink(position, possessionLink);
            case ComparisonLink comparisonLink -> findPositionDistanceFromLink(position, comparisonLink);
            case WholeValue ignored -> throw new Error("Whole values not handled as real vertices");
            case LowerValue ignored -> throw new Error("Lower values not handled as real vertices");
        };
    }

    private int findPositionDistanceFromObject(Position position, Object object) {
        var objectPosition = findObjectPosition(object);
        var relativePosition = Vector.between(objectPosition, position);
        return relativePosition.infiniteNormLength();
    }

    private int findPositionDistanceFromLink(Position position, Link possessionLink) {

        var position1 = findVertexPosition(possessionLink.getOrigin());
        var position2 = findVertexPosition(possessionLink.getDestination());
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

    private void reactToRelease(MouseEvent event) {
        if (selectionRectangleOrigin != null && selectionRectangleDestination != null) {
            this.repaint();
        }
        this.selectionRectangleOrigin = null;
        this.selectionRectangleDestination = null;
        if (!this.guidesX.isEmpty() || !this.guidesY.isEmpty()) {
            this.repaint();
        }
        this.clearGuides();
        if (this.newLinkOrigin != null) {
            Position position = findEventPosition(event);
            var destination = findVertexAtPosition(position);
            var origin = this.newLinkOrigin;
            this.newLinkOrigin = null;
            if (destination != null && destination != origin) {
                if (this.newLinkType.equals(PossessionLink.class) && ModelHandler.addPossessionLink(origin, destination, model)) {
                    changeDetector.notifyChange();
                }
                if (this.newLinkType.equals(ComparisonLink.class) && ModelHandler.addComparisonLink(origin, destination, model)) {
                    changeDetector.notifyChange();
                }
                this.repaint();
            }
            else {
                this.repaint();
            }
        }
        if (!hasDragged && lastSelectedVertex != null && this.selectedVertices.size() > 1 && (event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
            this.selectedVertices.removeIf(Predicate.not(Predicate.isEqual(lastSelectedVertex)));
            this.repaint();
            this.selectionChangeDetector.notifyChange();
        }
        if (hasDraggedObjects) {
            changeDetector.notifyChange();
        }
    }

    private void reactToDrag(MouseEvent event) {
        // if a new linked is dragged, just repaint
        if (newLinkOrigin != null) {
            this.repaint();
            return;
        }
        Position position = findEventPosition(event);
        if (selectionRectangleOrigin != null) {
            this.selectionRectangleDestination = position;
            List<Object> objectsInRectangle = model.getGraph().getObjects().stream()
                    .filter(object -> isInRectangleBetweenPoints(model.getPositions().get(object), selectionRectangleOrigin, selectionRectangleDestination))
                    .toList();
            this.selectedVertices.clear();
            this.selectedVertices.addAll(objectsInRectangle);
            addLinksBetweenVertices(this.selectedVertices);
            this.selectionChangeDetector.notifyChange();
            this.repaint();
            return;
        }
        if (!canDrag) {
            return;
        }
        this.hasDragged = true;
        boolean needsRepaint = false;
        for (Vertex selectedVertex: selectedVertices) {
            if (selectedVertex instanceof Object object) {
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
                for (Vertex selectedVertex: selectedVertices) {
                    if (selectedVertex instanceof Object object) {
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

    private void addLinksBetweenVertices(List<Vertex> vertices) {
        List<PossessionLink> possessionLinksToAdd;
        List<ComparisonLink> comparisonLinksToAdd;
        // TODO links from values
        do {
            possessionLinksToAdd = model.getGraph().getPossessionLinks().stream()
                    .filter(link -> !vertices.contains(link) && vertices.contains(link.getOrigin()) && vertices.contains(link.getDestination()))
                    .toList();
            comparisonLinksToAdd = model.getGraph().getComparisonLinks().stream()
                    .filter(link -> !vertices.contains(link) && vertices.contains(link.getOrigin()) && vertices.contains(link.getDestination()))
                    .toList();
            vertices.addAll(possessionLinksToAdd);
            vertices.addAll(comparisonLinksToAdd);
        } while (!possessionLinksToAdd.isEmpty() && !comparisonLinksToAdd.isEmpty());
    }

    private List<Integer> findNearbyGuideDeltas(Function<Position, Integer> coordinate) {
        List<Position> selectedPositions = selectedVertices.stream()
                .filter(vertex -> vertex instanceof Object)
                .map(vertex -> (Object)vertex)
                .map(model.getPositions()::get)
                .toList();
        return this.model.getGraph().getObjects().stream()
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
                .filter(vertex -> vertex instanceof Object)
                .map(vertex -> (Object)vertex)
                .map(model.getPositions()::get)
                .toList();
        this.guidesX.clear();
        this.guidesX.addAll(this.model.getGraph().getObjects().stream()
                .filter(Predicate.not(selectedVertices::contains))
                .map(model.getPositions()::get)
                .map(Position::x)
                .filter(x -> selectedPositions.stream().anyMatch(position -> position.x() == x))
                .distinct()
                .toList());
        this.guidesY.clear();
        this.guidesY.addAll(this.model.getGraph().getObjects().stream()
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
        for (Vertex selectedVertex: selectedVertices) {
            if (selectedVertex instanceof Object object) {
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
            this.changeDetector.notifyChange();
        }
    }
}
