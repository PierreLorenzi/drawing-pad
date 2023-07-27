package fr.alphonse.drawingpad.view;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.geometry.Vector;
import fr.alphonse.drawingpad.data.model.GraphElement;
import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.document.utils.ChangeDetector;
import fr.alphonse.drawingpad.document.utils.Graduations;
import fr.alphonse.drawingpad.view.internal.ModelHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DrawingComponent extends JComponent {

    private Drawing model;

    private final ChangeDetector changeDetector;

    private final List<GraphElement> selectedElements = new ArrayList<>();

    private final ChangeDetector selectionChangeDetector = new ChangeDetector(selectedElements);

    private Map<Object, Vector> dragRelativeVectors;

    private boolean canDrag = false;

    private boolean hasDragged = false;

    private boolean hasDraggedObjects = false;

    private GraphElement lastSelectedElement = null;

    private Position selectionRectangleOrigin = null;

    private Position selectionRectangleDestination = null;

    private GraphElement newLinkOrigin = null;

    private Position newLinkCenter = null;

    private Link draggedCenterLink = null;

    private Vector draggedCenterRelativePosition = null;

    private final List<Integer> guidesX = new ArrayList<>();

    private final List<Integer> guidesY = new ArrayList<>();

    private static final int OBJECT_RECTANGLE_RADIUS = 8;

    private static final int OBJECT_RADIUS = 10;

    private static final float ARROW_DISTANCE = 20;

    private static final double ARROW_ANGLE = Math.toRadians(25);

    private static final float ARROW_LENGTH = 10;

    private static final BasicStroke SHADOW_STROKE = new BasicStroke(3);

    private static final BasicStroke SELECTED_LINK_STROKE = new BasicStroke(3);

    private static final BasicStroke LINK_STROKE = new BasicStroke(1);

    private static final int ARROW_KEY_DELTA = 1;

    private static final Vector ARROW_KEY_UP_DELTA = new Vector(0, -ARROW_KEY_DELTA);

    private static final Vector ARROW_KEY_DOWN_DELTA = new Vector(0, ARROW_KEY_DELTA);

    private static final Vector ARROW_KEY_LEFT_DELTA = new Vector(-ARROW_KEY_DELTA, 0);

    private static final Vector ARROW_KEY_RIGHT_DELTA = new Vector(ARROW_KEY_DELTA, 0);

    private static final Color SELECTION_COLOR = Color.getHSBColor(206f/360, 1f, .9f);

    private static final int GUIDE_MAGNETISM_RADIUS = 3;

    private static final int LINK_CENTER_CLICK_RADIUS = 8;

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
                DrawingComponent.this.reactToMove();
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

    public java.util.List<GraphElement> getSelection() {
        return selectedElements;
    }

    public ChangeDetector getSelectionChangeDetector() {
        return selectionChangeDetector;
    }

    public void delete() {
        for (GraphElement selectedElement: selectedElements) {
            switch (selectedElement) {
                case Object object -> ModelHandler.deleteObject(object, model);
                case Link link -> ModelHandler.deleteLink(link, model);
            }
        }
        this.selectedElements.clear();
        this.selectionChangeDetector.notifyChange();
        lastSelectedElement = null;
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

            if (selectedElements.contains(object)) {
                g.setColor(SELECTION_COLOR);
            }
            else {
                g.setColor(Color.BLACK);
            }
            g.fillRect(position.x()-OBJECT_RECTANGLE_RADIUS, position.y()-OBJECT_RECTANGLE_RADIUS, 2*OBJECT_RECTANGLE_RADIUS, 2*OBJECT_RECTANGLE_RADIUS);
        }

        for (Link link : model.getGraph().getLinks()) {
            boolean isSelected = selectedElements.contains(link);
            drawLink(link, g, isSelected);
        }

        // draw link being dragged
        if (newLinkOrigin != null) {
            var position1 = findElementPosition(newLinkOrigin);
            Position position2 = findMousePosition();
            var linePosition1 = computeArrowMeetingPositionWithElement(newLinkCenter != null ? newLinkCenter : position2, position1, newLinkOrigin);
            drawLinkBetweenPositions(linePosition1, newLinkCenter, position2, g, false);
        }

        g.translate(-translationX, -translationY);
    }

    private void drawLink(Link link, Graphics g, boolean isSelected) {
        var position1 = findElementPosition(link.getOriginElement());
        var position2 = findElementPosition(link.getDestinationElement());
        var center = findLinkCenter(link);
        var linePosition1 = computeArrowMeetingPositionWithElement(center != null ? center : position2, position1, link.getOriginElement());
        var linePosition2 = computeArrowMeetingPositionWithElement(center != null ? center : position1, position2, link.getDestinationElement());
        drawLinkBetweenPositions(linePosition1, center, linePosition2, g, isSelected);
        if (Graduations.isStrictlyGreaterThanOne(link.getFactor().getGraduation())) {
            Position secondPosition = (center != null) ? center : position2;
            drawArrow(linePosition1, secondPosition, g);
        }
        else if (Graduations.isStrictlyLesserThanOne(link.getFactor().getGraduation())) {
            Position secondPosition = (center != null) ? center : position1;
            drawArrow(linePosition2, secondPosition, g);
        }
    }

    private void drawLinkBetweenPositions(Position position1, Position center, Position position2, Graphics g, boolean isSelected) {
        if (isSelected) {
            g.setColor(SELECTION_COLOR);
            ((Graphics2D) g).setStroke(SELECTED_LINK_STROKE);
        }
        else {
            g.setColor(Color.BLACK);
            ((Graphics2D) g).setStroke(LINK_STROKE);
        }
        if (center != null) {
            g.drawLine(position1.x(), position1.y(), center.x(), center.y());
            g.drawLine(center.x(), center.y(), position2.x(), position2.y());
        }
        else {
            g.drawLine(position1.x(), position1.y(), position2.x(), position2.y());
        }
    }

    private Position findMousePosition() {
        Point mousePoint = MouseInfo.getPointerInfo().getLocation();
        Point point = new Point(mousePoint);
        SwingUtilities.convertPointFromScreen(point, this);
        return findPointPosition(point);
    }

    private Position findElementPosition(GraphElement element) {
        return switch (element) {
            case Object object -> findObjectPosition(object);
            case Link link -> {
                var center = model.getLinkCenters().get(link);
                if (center != null) {
                    yield center;
                }
                var position1 = findElementPosition(link.getOriginElement());
                var position2 = findElementPosition(link.getDestinationElement());
                yield Position.middle(position1, position2);
            }
        };
    }

    private Position findObjectPosition(Object object) {
        return model.getPositions().get(object);
    }

    private Position computeArrowMeetingPositionWithElement(Position position1, Position position2, GraphElement element) {
        return switch (element) {
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
        return position1;
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
        var selectedElement = findElementAtPosition(position);
        // if drawing a link
        if (this.newLinkOrigin != null) {
            this.repaint();
            if (selectedElement == null) {
                this.newLinkCenter = position;
                return;
            }
            var origin = this.newLinkOrigin;
            var center = this.newLinkCenter;
            this.newLinkOrigin = null;
            this.newLinkCenter = null;
            this.repaint();
            if (selectedElement == origin) {
                return;
            }
            ModelHandler.addLink(origin, selectedElement, center, model);
            changeDetector.notifyChange();
            return;
        }
        // if press with command, add object or link
        if ((event.getModifiersEx() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) != 0) {
            if (selectedElement == null) {
                ModelHandler.addObject(position, model);
                changeDetector.notifyChange();
            }
            else {
                newLinkOrigin = selectedElement;
            }
            return;
        }
        boolean isShiftKeyPressed = (event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
        // link center drag
        if (!isShiftKeyPressed && selectedElement instanceof Link link && isPositionAtLinkCenter(position, link)) {
            this.draggedCenterLink = link;
            var center = findLinkCenter(link);
            this.draggedCenterRelativePosition = Vector.between(position, center);
            this.repaint();
        }
        this.lastSelectedElement = selectedElement;
        if (selectedElement == null && !isShiftKeyPressed) {
            this.selectionRectangleOrigin = position;
        }
        boolean alreadySelected = selectedElement != null && selectedElements.contains(selectedElement);
        this.canDrag = !(isShiftKeyPressed && (selectedElement == null || alreadySelected));
        this.hasDragged = false;
        this.hasDraggedObjects = false;
        if (alreadySelected && isShiftKeyPressed) {
            selectedElements.remove(selectedElement);
            this.selectionChangeDetector.notifyChange();
        }
        if (!isShiftKeyPressed && !alreadySelected) {
            DrawingComponent.this.selectedElements.clear();
            this.selectionChangeDetector.notifyChange();
        }
        if (selectedElement != null && !alreadySelected) {
            this.selectedElements.add(selectedElement);
            this.selectionChangeDetector.notifyChange();
        }
        this.dragRelativeVectors = this.selectedElements.stream()
                .filter(element -> element instanceof Object)
                .map(element -> (Object)element)
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

    private GraphElement findElementAtPosition(Position position) {
        Object object = findElementAtPositionInList(position, model.getGraph().getObjects());
        if (object != null) {
            return object;
        }
        return findElementAtPositionInList(position, model.getGraph().getLinks());
    }

    private <T extends GraphElement> T findElementAtPositionInList(Position position, List<T> elements) {
        Function<GraphElement,Integer> computeDistanceFunction = (element -> computePositionDistanceFromElement(position, element));
        return elements.stream()
                .filter(element -> computeDistanceFunction.apply(element) < OBJECT_RADIUS)
                .min(Comparator.comparing(computeDistanceFunction))
                .orElse(null);
    }

    private int computePositionDistanceFromElement(Position position, GraphElement element) {
        return switch (element) {
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

        var position1 = findElementPosition(link.getOriginElement());
        var position2 = findElementPosition(link.getDestinationElement());
        var center = findLinkCenter(link);

        if (center == null) {
            return findPositionDistanceFromLine(position, position1, position2);
        }
        int distance1 = findPositionDistanceFromLine(position, position1, center);
        int distance2 = findPositionDistanceFromLine(position, center, position2);
        return Math.min(distance1, distance2);
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

    private boolean isPositionAtLinkCenter(Position position, Link link) {
        Position center = findLinkCenter(link);
        if (center == null) {
            return false;
        }
        return Vector.between(position, center).length() <= LINK_CENTER_CLICK_RADIUS;
    }

    private Position findLinkCenter(Link link) {
        return model.getLinkCenters().get(link);
    }

    private void reactToRelease(MouseEvent event) {
        if (this.draggedCenterLink != null) {
            Position position = findEventPosition(event);
            var newCenter = position.translate(this.draggedCenterRelativePosition);
            var link = this.draggedCenterLink;
            this.draggedCenterLink = null;
            this.draggedCenterRelativePosition = null;
            this.model.getLinkCenters().put(link, newCenter);
            this.repaint();
            this.changeDetector.notifyChange();
            return;
        }
        if (selectionRectangleOrigin != null || selectionRectangleDestination != null) {
            this.selectionRectangleOrigin = null;
            this.selectionRectangleDestination = null;
            this.repaint();
        }
        if (!this.guidesX.isEmpty() || !this.guidesY.isEmpty()) {
            this.clearGuides();
            this.repaint();
        }
        // if several objects are selected, and we click on one of them, it becomes the only selected element on mouse up
        if (!hasDragged && lastSelectedElement != null && this.selectedElements.size() > 1 && (event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
            this.selectedElements.removeIf(Predicate.not(Predicate.isEqual(lastSelectedElement)));
            this.repaint();
            this.selectionChangeDetector.notifyChange();
        }
        if (hasDraggedObjects) {
            changeDetector.notifyChange();
        }
    }

    private void reactToDrag(MouseEvent event) {
        Position position = findEventPosition(event);
        if (this.draggedCenterLink != null) {
            var newCenter = position.translate(this.draggedCenterRelativePosition);
            this.model.getLinkCenters().put(this.draggedCenterLink, newCenter);
            this.repaint();
            return;
        }
        if (selectionRectangleOrigin != null) {
            this.selectionRectangleDestination = position;
            List<Object> objectsInRectangle = model.getGraph().getObjects().stream()
                    .filter(object -> isInRectangleBetweenPoints(model.getPositions().get(object), selectionRectangleOrigin, selectionRectangleDestination))
                    .toList();
            this.selectedElements.clear();
            this.selectedElements.addAll(objectsInRectangle);
            addLinksBetweenElements(this.selectedElements);
            this.selectionChangeDetector.notifyChange();
            this.repaint();
            return;
        }
        if (!canDrag || this.newLinkOrigin != null) {
            return;
        }
        this.hasDragged = true;
        boolean needsRepaint = false;
        for (GraphElement selectedElement: selectedElements) {
            if (selectedElement instanceof Object object) {
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
                for (GraphElement selectedElement: selectedElements) {
                    if (selectedElement instanceof Object object) {
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

    private void addLinksBetweenElements(List<GraphElement> elements) {
        List<Link> linksToAdd;
        do {
            linksToAdd = model.getGraph().getLinks().stream()
                    .filter(link -> !elements.contains(link) && elements.contains(link.getOriginElement()) && elements.contains(link.getDestinationElement()))
                    .toList();
            elements.addAll(linksToAdd);
        } while (!linksToAdd.isEmpty());
    }

    private List<Integer> findNearbyGuideDeltas(Function<Position, Integer> coordinate) {
        List<Position> selectedPositions = selectedElements.stream()
                .filter(element -> element instanceof Object)
                .map(element -> (Object)element)
                .map(model.getPositions()::get)
                .toList();
        return this.model.getGraph().getObjects().stream()
                .filter(Predicate.not(selectedElements::contains))
                .map(model.getPositions()::get)
                .map(coordinate)
                .flatMap(value -> selectedPositions.stream().map(position -> value - coordinate.apply(position)))
                .filter(delta -> Math.abs(delta) <= GUIDE_MAGNETISM_RADIUS)
                .distinct()
                .sorted()
                .toList();
    }

    private void fillGuides() {
        List<Position> selectedPositions = selectedElements.stream()
                .filter(element -> element instanceof Object)
                .map(element -> (Object)element)
                .map(model.getPositions()::get)
                .toList();
        this.guidesX.clear();
        this.guidesX.addAll(this.model.getGraph().getObjects().stream()
                .filter(Predicate.not(selectedElements::contains))
                .map(model.getPositions()::get)
                .map(Position::x)
                .filter(x -> selectedPositions.stream().anyMatch(position -> position.x() == x))
                .distinct()
                .toList());
        this.guidesY.clear();
        this.guidesY.addAll(this.model.getGraph().getObjects().stream()
                .filter(Predicate.not(selectedElements::contains))
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

    private void reactToMove() {
        // if a new linked is dragged, repaint
        if (newLinkOrigin != null) {
            this.repaint();
        }
    }

    private void reactToKey(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.VK_UP -> moveSelectedElementBy(ARROW_KEY_UP_DELTA);
            case KeyEvent.VK_DOWN -> moveSelectedElementBy(ARROW_KEY_DOWN_DELTA);
            case KeyEvent.VK_LEFT -> moveSelectedElementBy(ARROW_KEY_LEFT_DELTA);
            case KeyEvent.VK_RIGHT -> moveSelectedElementBy(ARROW_KEY_RIGHT_DELTA);
            case KeyEvent.VK_ESCAPE -> stopDraggingLink();
        }
    }

    private void moveSelectedElementBy(Vector delta) {
        boolean needsRefresh = false;
        for (GraphElement selectedElement: selectedElements) {
            if (selectedElement instanceof Object object) {
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

    private void stopDraggingLink() {
        if (this.newLinkOrigin == null) {
            return;
        }
        this.newLinkOrigin = null;
        this.newLinkCenter = null;
        this.repaint();
    }
}
