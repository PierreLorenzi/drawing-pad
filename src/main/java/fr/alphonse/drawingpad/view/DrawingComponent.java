package fr.alphonse.drawingpad.view;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.geometry.Vector;
import fr.alphonse.drawingpad.data.model.*;
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
import java.util.stream.Stream;

public class DrawingComponent extends JComponent {

    private Drawing model;

    private final ChangeDetector changeDetector;

    private final List<GraphElement> selectedElements = new ArrayList<>();

    private final ChangeDetector selectionChangeDetector = new ChangeDetector(selectedElements);

    private Map<GraphElement, Vector> dragRelativeVectors;

    private boolean canDrag = false;

    private boolean hasDragged = false;

    private boolean hasDraggedObjects = false;

    private GraphElement lastSelectedElement = null;

    private Position selectionRectangleOrigin = null;

    private Position selectionRectangleDestination = null;

    private Vertex newLinkOrigin = null;

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

    private final static float[] BASE_LINE_DASH = new float[]{1.0f, 2.0f};

    private static final BasicStroke BASE_LINE_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, BASE_LINE_DASH, 0.0f);

    private static final int ARROW_KEY_DELTA = 1;

    private static final Vector ARROW_KEY_UP_DELTA = new Vector(0, -ARROW_KEY_DELTA);

    private static final Vector ARROW_KEY_DOWN_DELTA = new Vector(0, ARROW_KEY_DELTA);

    private static final Vector ARROW_KEY_LEFT_DELTA = new Vector(-ARROW_KEY_DELTA, 0);

    private static final Vector ARROW_KEY_RIGHT_DELTA = new Vector(ARROW_KEY_DELTA, 0);

    private static final Color SELECTION_COLOR = Color.getHSBColor(206f/360, 1f, .9f);

    private static final int GUIDE_MAGNETISM_RADIUS = 3;

    private static final int LINK_CENTER_CLICK_RADIUS = 8;

    public static final int INITIAL_DISTANCE_FROM_BASE = 30;

    public static final int CIRCLE_RADIUS = 6;

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
                case Completion completion -> ModelHandler.deleteCompletion(completion, model);
                case Quantity quantity -> ModelHandler.deleteQuantity(quantity, model);
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

        Map<Completion, Position> completionPositions = model.getCompletionPositions();
        for (Completion completion: model.getGraph().getCompletions()) {
            var position = completionPositions.get(completion);

            if (selectedElements.contains(completion)) {
                g.setColor(SELECTION_COLOR);
            }
            else {
                g.setColor(Color.BLUE);
            }
            g.fillOval(position.x()-CIRCLE_RADIUS, position.y()-CIRCLE_RADIUS, 2*CIRCLE_RADIUS, 2*CIRCLE_RADIUS);
            drawBaseJoin(completion, completion.getBase(), g);
        }

        Map<Quantity, Position> quantityPositions = model.getQuantityPositions();
        for (Quantity quantity: model.getGraph().getQuantities()) {
            var position = quantityPositions.get(quantity);

            if (selectedElements.contains(quantity)) {
                g.setColor(SELECTION_COLOR);
            }
            else {
                g.setColor(Color.ORANGE);
            }
            g.fillOval(position.x()-CIRCLE_RADIUS, position.y()-CIRCLE_RADIUS, 2*CIRCLE_RADIUS, 2*CIRCLE_RADIUS);
            drawBaseJoin(quantity, quantity.getBase(), g);
        }

        for (Link link : model.getGraph().getLinks()) {
            boolean isSelected = selectedElements.contains(link);
            drawLink(link, g, isSelected);
        }

        // draw link being dragged
        if (newLinkOrigin != null) {
            var position1 = findVertexPosition(newLinkOrigin);
            Position position2 = findMousePosition();
            var linePosition1 = computeArrowMeetingPositionWithVertex(newLinkCenter != null ? newLinkCenter : position2, position1, newLinkOrigin);
            drawLinkBetweenPositions(linePosition1, newLinkCenter, position2, g, false);
        }

        g.translate(-translationX, -translationY);
    }

    private void drawBaseJoin(Vertex vertex, Vertex base, Graphics g) {
        var position1 = findVertexPosition(vertex);
        var position2 = findVertexPosition(base);
        var linePosition1 = computeArrowMeetingPositionWithVertex(position2, position1, vertex);
        var linePosition2 = computeArrowMeetingPositionWithVertex(position1, position2, base);
        ((Graphics2D)g).setStroke(BASE_LINE_STROKE);
        g.setColor(Color.BLACK);
        g.drawLine(linePosition1.x(), linePosition1.y(), linePosition2.x(), linePosition2.y());
    }

    private void drawLink(Link link, Graphics g, boolean isSelected) {
        var position1 = findVertexPosition(link.getOrigin());
        var position2 = findVertexPosition(link.getDestination());
        var center = findLinkCenter(link);
        var linePosition1 = computeArrowMeetingPositionWithVertex(center != null ? center : position2, position1, link.getOrigin());
        var linePosition2 = computeArrowMeetingPositionWithVertex(center != null ? center : position1, position2, link.getDestination());
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

    private Position findVertexPosition(Vertex vertex) {
        return switch (vertex) {
            case Object object -> findObjectPosition(object);
            case Completion completion -> findCompletionPosition(completion);
            case Quantity quantity -> findQuantityPosition(quantity);
            case DirectFactor directFactor -> {
                var position1 = findVertexPosition(directFactor.getLink().getOrigin());
                var center = model.getLinkCenters().get(directFactor.getLink());
                if (center != null) {
                    yield Position.middle(position1, center);
                }
                var position2 = findVertexPosition(directFactor.getLink().getDestination());
                yield findFirstQuarter(position1, position2);
            }
            case ReverseFactor reverseFactor -> {
                var position2 = findVertexPosition(reverseFactor.getLink().getDestination());
                var center = model.getLinkCenters().get(reverseFactor.getLink());
                if (center != null) {
                    yield Position.middle(center, position2);
                }
                var position1 = findVertexPosition(reverseFactor.getLink().getOrigin());
                yield findFirstQuarter(position2, position1);
            }
        };
    }

    private Position findObjectPosition(Object object) {
        return model.getPositions().get(object);
    }

    private Position findCompletionPosition(Completion completion) {
        return model.getCompletionPositions().get(completion);
    }

    private Position findQuantityPosition(Quantity quantity) {
        return model.getQuantityPositions().get(quantity);
    }

    public static Position findFirstQuarter(Position p1, Position p2) {
        return new Position((p1.x()*3 + p2.x())/4, (p1.y()*3 + p2.y())/4);
    }

    private Position computeArrowMeetingPositionWithVertex(Position position1, Position position2, Vertex vertex) {
        return switch (vertex) {
            case Object ignored -> computeArrowMeetingPositionWithObject(position1, position2);
            case Completion ignored -> computeArrowMeetingPositionWithCircle(position1, position2);
            case Quantity ignored -> computeArrowMeetingPositionWithCircle(position1, position2);
            case DirectFactor ignored -> position2;
            case ReverseFactor ignored -> position2;
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
        var clickedVertex = findVertexAtPosition(position);
        // if drawing a link
        if (this.newLinkOrigin != null) {
            this.repaint();
            if (clickedVertex == null) {
                this.newLinkCenter = position;
                return;
            }
            var origin = this.newLinkOrigin;
            var center = this.newLinkCenter;
            this.newLinkOrigin = null;
            this.newLinkCenter = null;
            this.repaint();
            if (clickedVertex == origin) {
                return;
            }
            ModelHandler.addLink(origin, clickedVertex, center, model);
            changeDetector.notifyChange();
            return;
        }
        // if press with command, add object or link
        if ((event.getModifiersEx() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) != 0) {
            if (clickedVertex == null) {
                ModelHandler.addObject(position, model);
                changeDetector.notifyChange();
            }
            else {
                newLinkOrigin = clickedVertex;
            }
            return;
        }
        // if press with option, add completion
        if ((event.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
            if (clickedVertex == null) {
                return;
            }
            Position newPosition = makePositionFromBase(findVertexPosition(clickedVertex));
            ModelHandler.addCompletion(clickedVertex, newPosition, model);
            changeDetector.notifyChange();
            return;
        }
        // if press with control, add quantity
        if ((event.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
            if (clickedVertex == null) {
                return;
            }
            Position newPosition = makePositionFromBase(findVertexPosition(clickedVertex));
            ModelHandler.addQuantity(clickedVertex, newPosition, model);
            changeDetector.notifyChange();
            return;
        }
        boolean isShiftKeyPressed = (event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
        var clickedElement = clickedVertex == null ? null : clickedVertex.getElement();
        // link center drag
        if (!isShiftKeyPressed && clickedElement instanceof Link link && isPositionAtLinkCenter(position, link)) {
            this.draggedCenterLink = link;
            var center = findLinkCenter(link);
            this.draggedCenterRelativePosition = Vector.between(position, center);
            this.repaint();
        }
        this.lastSelectedElement = clickedElement;
        if (clickedElement == null && !isShiftKeyPressed) {
            this.selectionRectangleOrigin = position;
        }
        boolean alreadySelected = clickedElement != null && selectedElements.contains(clickedElement);
        this.canDrag = !(isShiftKeyPressed && (clickedElement == null || alreadySelected));
        this.hasDragged = false;
        this.hasDraggedObjects = false;
        if (alreadySelected && isShiftKeyPressed) {
            selectedElements.remove(clickedElement);
            this.selectionChangeDetector.notifyChange();
        }
        if (!isShiftKeyPressed && !alreadySelected) {
            DrawingComponent.this.selectedElements.clear();
            this.selectionChangeDetector.notifyChange();
        }
        if (clickedElement != null && !alreadySelected) {
            this.selectedElements.add(clickedElement);
            this.selectionChangeDetector.notifyChange();
        }
        this.dragRelativeVectors = this.selectedElements.stream()
                .filter(element -> !(element instanceof Link))
                .collect(Collectors.toMap(Function.identity(), element -> Vector.between(position, findVertexPosition((Vertex)element))));
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
        Object object = findVertexAtPositionInList(position, model.getGraph().getObjects().stream());
        if (object != null) {
            return object;
        }
        Completion completion = findVertexAtPositionInList(position, model.getGraph().getCompletions().stream());
        if (completion != null) {
            return completion;
        }
        Quantity quantity = findVertexAtPositionInList(position, model.getGraph().getQuantities().stream());
        if (quantity != null) {
            return quantity;
        }
        DirectFactor directFactor = findVertexAtPositionInList(position, model.getGraph().getLinks().stream().map(Link::getDirectFactor));
        if (directFactor != null) {
            return directFactor;
        }
        return findVertexAtPositionInList(position, model.getGraph().getLinks().stream().map(Link::getReverseFactor));
    }

    private <T extends Vertex> T findVertexAtPositionInList(Position position, Stream<T> vertices) {
        Function<Vertex,Integer> computeDistanceFunction = (element -> computePositionDistanceFromVertex(position, element));
        return vertices
                .filter(element -> computeDistanceFunction.apply(element) < OBJECT_RADIUS)
                .min(Comparator.comparing(computeDistanceFunction))
                .orElse(null);
    }

    private int computePositionDistanceFromVertex(Position position, Vertex vertex) {
        return switch (vertex) {
            case Object object -> findPositionDistanceFromObject(position, object);
            case Completion completion -> findPositionDistanceFromCompletion(position, completion);
            case Quantity quantity -> findPositionDistanceFromQuantity(position, quantity);
            case DirectFactor directFactor -> findPositionDistanceFromDirectFactor(position, directFactor);
            case ReverseFactor reverseFactor -> findPositionDistanceFromReverseFactor(position, reverseFactor);
        };
    }

    private int findPositionDistanceFromObject(Position position, Object object) {
        var objectPosition = findObjectPosition(object);
        var relativePosition = Vector.between(objectPosition, position);
        return relativePosition.infiniteNormLength();
    }

    private int findPositionDistanceFromCompletion(Position position, Completion completion) {
        var completionPosition = findCompletionPosition(completion);
        var relativePosition = Vector.between(completionPosition, position);
        return relativePosition.infiniteNormLength();
    }

    private int findPositionDistanceFromQuantity(Position position, Quantity quantity) {
        var quantityPosition = findQuantityPosition(quantity);
        var relativePosition = Vector.between(quantityPosition, position);
        return relativePosition.infiniteNormLength();
    }

    private int findPositionDistanceFromDirectFactor(Position position, DirectFactor directFactor) {

        Link link = directFactor.getLink();
        var position1 = findVertexPosition(link.getOrigin());
        var position2 = findVertexPosition(link.getDestination());
        var center = findLinkCenter(link);

        if (center == null) {
            Position realCenter = Position.middle(position1, position2);
            return findPositionDistanceFromLine(position, position1, realCenter);
        }
        return findPositionDistanceFromLine(position, position1, center);
    }

    private int findPositionDistanceFromReverseFactor(Position position, ReverseFactor reverseFactor) {

        Link link = reverseFactor.getLink();
        var position1 = findVertexPosition(link.getOrigin());
        var position2 = findVertexPosition(link.getDestination());
        var center = findLinkCenter(link);

        if (center == null) {
            Position realCenter = Position.middle(position1, position2);
            return findPositionDistanceFromLine(position, realCenter, position2);
        }
        return findPositionDistanceFromLine(position, center, position2);
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

    private Position makePositionFromBase(Position basePosition) {
        return new Position(basePosition.x() + INITIAL_DISTANCE_FROM_BASE, basePosition.y());
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
            if (!(selectedElement instanceof Link)) {
                changeVertexPosition((Vertex)selectedElement, position.translate(this.dragRelativeVectors.get(selectedElement)));
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

    private void changeVertexPosition(Vertex vertex, Position position) {
        switch (vertex) {
            case Object object -> model.getPositions().put(object, position);
            case Completion completion -> model.getCompletionPositions().put(completion, position);
            case Quantity quantity -> model.getQuantityPositions().put(quantity, position);
            case DirectFactor ignored -> throw new Error("Can't move DirectFactor");
            case ReverseFactor ignored -> throw new Error("Can't move ReverseFactor");
        }
    }

    private boolean isInRectangleBetweenPoints(Position position, Position corner1, Position corner2) {
        return position.x() >= Math.min(corner1.x(), corner2.x()) &&
                position.y() >= Math.min(corner1.y(), corner2.y()) &&
                position.x() < Math.max(corner1.x(), corner2.x()) &&
                position.y() < Math.max(corner1.y(), corner2.y());
    }

    private void addLinksBetweenElements(List<GraphElement> elements) {
        List<Completion> completionsToAdd;
        List<Quantity> quantitiesToAdd;
        List<Link> linksToAdd;
        do {
            completionsToAdd = model.getGraph().getCompletions().stream()
                    .filter(completion -> !elements.contains(completion) && elements.contains(completion.getBase().getElement()))
                    .toList();
            quantitiesToAdd = model.getGraph().getQuantities().stream()
                    .filter(quantity -> !elements.contains(quantity) && elements.contains(quantity.getBase().getElement()))
                    .toList();
            linksToAdd = model.getGraph().getLinks().stream()
                    .filter(link -> !elements.contains(link) && elements.contains(link.getOrigin().getElement()) && elements.contains(link.getDestination().getElement()))
                    .toList();
            elements.addAll(completionsToAdd);
            elements.addAll(quantitiesToAdd);
            elements.addAll(linksToAdd);
        } while (!completionsToAdd.isEmpty() || !quantitiesToAdd.isEmpty() || !linksToAdd.isEmpty());
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
