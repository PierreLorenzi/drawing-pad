package fr.alphonse.drawingpad.view;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.geometry.Vector;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.reference.LinkDirection;
import fr.alphonse.drawingpad.document.utils.ChangeDetector;
import fr.alphonse.drawingpad.document.utils.Graduations;
import fr.alphonse.drawingpad.document.utils.GraphHandler;
import fr.alphonse.drawingpad.view.internal.ModelHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DrawingComponent extends JComponent {

    private final Drawing model;

    private final ChangeDetector<?,?> changeDetector;

    private final List<GraphElement> selectedElements = new ArrayList<>();

    public static final Function<List<GraphElement>, ?> SELECTION_STATE_FUNCTION = elements -> elements.stream().map(ModelHandler::makeReferenceForElement).collect(Collectors.toSet());

    private final ChangeDetector<List<GraphElement>,?> selectionChangeDetector = new ChangeDetector<>(selectedElements, SELECTION_STATE_FUNCTION);

    private Position clickPosition;

    private Map<GraphElement, Vector> dragRelativeVectors;

    private boolean canDrag = false;

    private boolean hasDragged = false;

    private boolean hasDraggedElements = false;

    private GraphElement lastSelectedElement = null;

    private Position selectionRectangleOrigin = null;

    private Position selectionRectangleDestination = null;

    private GraphElement newLinkOrigin = null;

    private LinkDirection newOriginLinkDirection = null;

    private Position newLinkCenter = null;

    private Link draggedCenterLink = null;

    private Vector draggedCenterRelativePosition = null;

    private final List<Integer> guidesX = new ArrayList<>();

    private final List<Integer> guidesY = new ArrayList<>();

    private Drawing lastPastedModel;

    private int lastPastedCount = 0;

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

    private static final int INITIAL_DISTANCE_FROM_BASE = 30;

    private static final int CIRCLE_RADIUS = 6;

    private static  final Vector PASTE_SHIFT = new Vector(70, 30);

    public DrawingComponent(Drawing model, ChangeDetector<?,?> changeDetector) {
        super();
        this.model = model;
        this.changeDetector = changeDetector;
        changeDetector.addListener(this, DrawingComponent::reactToModelChange);
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

    public java.util.List<GraphElement> getSelection() {
        return selectedElements;
    }

    public ChangeDetector<?,?> getSelectionChangeDetector() {
        return selectionChangeDetector;
    }

    private void reactToModelChange() {
        // elements of the selection may have disappeared
        int selectionSizeBeforeFilter = this.selectedElements.size();
        this.selectedElements.removeIf(element -> !doesElementExistInModel(element, model));
        if (this.selectedElements.size() != selectionSizeBeforeFilter) {
            this.selectionChangeDetector.notifyChange();
        }

        this.repaint();
    }

    private static boolean doesElementExistInModel(GraphElement element, Drawing model) {
        Graph graph = model.getGraph();
        return switch (element) {
            case Object object -> graph.getObjects().contains(object);
            case Completion completion -> graph.getCompletions().contains(completion);
            case Quantity quantity -> graph.getQuantities().contains(quantity);
            case Link link -> graph.getLinks().contains(link);
        };
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
        this.changeDetector.notifyChangeCausedBy(this);
        repaint();
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
            var position1 = findVertexPosition(newLinkOrigin, newOriginLinkDirection);
            Position position2 = findMousePosition();
            var linePosition1 = computeArrowMeetingPositionWithElement(newLinkCenter != null ? newLinkCenter : position2, position1, newLinkOrigin);
            drawLinkBetweenPositions(linePosition1, newLinkCenter, position2, g, false);
        }

        g.translate(-translationX, -translationY);
    }

    private void drawBaseJoin(GraphElement element, GraphElement base, Graphics g) {
        var position1 = findElementPosition(element);
        var position2 = findElementPosition(base);
        var linePosition1 = computeArrowMeetingPositionWithElement(position2, position1, element);
        var linePosition2 = computeArrowMeetingPositionWithElement(position1, position2, base);
        ((Graphics2D)g).setStroke(BASE_LINE_STROKE);
        g.setColor(Color.BLACK);
        g.drawLine(linePosition1.x(), linePosition1.y(), linePosition2.x(), linePosition2.y());
    }

    private void drawLink(Link link, Graphics g, boolean isSelected) {
        var position1 = findVertexPosition(link.getOrigin(), link.getOriginLinkDirection());
        var position2 = findVertexPosition(link.getDestination(), link.getDestinationLinkDirection());
        var center = findLinkCenter(link);
        var linePosition1 = computeArrowMeetingPositionWithElement(center != null ? center : position2, position1, link.getOrigin());
        var linePosition2 = computeArrowMeetingPositionWithElement(center != null ? center : position1, position2, link.getDestination());
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

    private Position findVertexPosition(GraphElement element, LinkDirection linkDirection) {
        if (element instanceof Link link) {
            return findLinkVertexPosition(link, linkDirection);
        }
        return findElementPosition(element);
    }

    private Position findLinkVertexPosition(Link link, LinkDirection linkDirection) {
        return switch (linkDirection) {
            case DIRECT -> {
                var position1 = findVertexPosition(link.getOrigin(), link.getOriginLinkDirection());
                var center = model.getLinkCenters().get(link);
                if (center != null) {
                    yield Position.middle(position1, center);
                }
                var position2 = findVertexPosition(link.getDestination(), link.getDestinationLinkDirection());
                yield findFirstQuarter(position1, position2);
            }
            case REVERSE -> {
                var position2 = findVertexPosition(link.getDestination(), link.getDestinationLinkDirection());
                var center = model.getLinkCenters().get(link);
                if (center != null) {
                    yield Position.middle(center, position2);
                }
                var position1 = findVertexPosition(link.getOrigin(), link.getOriginLinkDirection());
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

    private Position computeArrowMeetingPositionWithElement(Position position1, Position position2, GraphElement element) {
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
        clickPosition = position;
        var clickedElement = findElementAtPosition(position);
        // if drawing a link
        if (this.newLinkOrigin != null) {
            this.repaint();
            if (clickedElement == null) {
                this.newLinkCenter = position;
                return;
            }
            var origin = this.newLinkOrigin;
            var originLinkDirection = this.newOriginLinkDirection;
            var center = this.newLinkCenter;
            this.newLinkOrigin = null;
            this.newOriginLinkDirection = null;
            this.newLinkCenter = null;
            this.repaint();
            if (clickedElement == origin) {
                return;
            }
            LinkDirection destinationLinkDirection = findLinkDirectionAtPosition(clickedElement, position);
            ModelHandler.addLink(origin, originLinkDirection, clickedElement, destinationLinkDirection, center, model);
            changeDetector.notifyChangeCausedBy(this);
            return;
        }
        // if press with command, add object or link
        if ((event.getModifiersEx() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) != 0) {
            if (clickedElement == null) {
                ModelHandler.addObject(position, model);
                changeDetector.notifyChangeCausedBy(this);
                repaint();
            }
            else {
                newLinkOrigin = clickedElement;
                newOriginLinkDirection = findLinkDirectionAtPosition(clickedElement, position);
            }
            return;
        }
        // if press with option, add completion
        if ((event.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
            if (clickedElement == null) {
                return;
            }
            Position newPosition = makePositionFromBase(findElementPosition(clickedElement));
            ModelHandler.addCompletion(clickedElement, newPosition, model);
            changeDetector.notifyChangeCausedBy(this);
            repaint();
            return;
        }
        // if press with control, add quantity
        if ((event.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
            if (clickedElement == null) {
                return;
            }
            Position newPosition = makePositionFromBase(findElementPosition(clickedElement));
            ModelHandler.addQuantity(clickedElement, newPosition, model);
            changeDetector.notifyChangeCausedBy(this);
            repaint();
            return;
        }
        boolean isShiftKeyPressed = (event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
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
        this.hasDraggedElements = false;
        boolean selectionDidChange = false;
        if (alreadySelected && isShiftKeyPressed) {
            selectedElements.remove(clickedElement);
            selectionDidChange = true;
        }
        if (!isShiftKeyPressed && !alreadySelected) {
            DrawingComponent.this.selectedElements.clear();
            selectionDidChange = true;
        }
        if (clickedElement != null && !alreadySelected) {
            this.selectedElements.add(clickedElement);
            selectionDidChange = true;
        }
        if (selectionDidChange) {
            this.selectionChangeDetector.notifyChange();
        }
        this.dragRelativeVectors = listElementsToDragAmong(this.selectedElements).stream()
                .filter(element -> !(element instanceof Link link && findLinkCenter(link) == null))
                .collect(Collectors.toMap(Function.identity(), element -> Vector.between(position, findElementPosition(element))));
        if (alreadySelected && !isShiftKeyPressed) {
            return;
        }
        this.repaint();
    }

    private Position findElementPosition(GraphElement element) {
        return switch (element) {
            case Object object -> findObjectPosition(object);
            case Completion completion -> findCompletionPosition(completion);
            case Quantity quantity -> findQuantityPosition(quantity);
            case Link link -> findLinkPosition(link);
        };
    }

    private Position findLinkPosition(Link link) {
        Position center = findLinkCenter(link);
        if (center != null) {
            return center;
        }
        var position1 = findVertexPosition(link.getOrigin(), link.getOriginLinkDirection());
        var position2 = findVertexPosition(link.getDestination(), link.getDestinationLinkDirection());
        return Position.middle(position1, position2);
    }

    private LinkDirection findLinkDirectionAtPosition(GraphElement element, Position position) {
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
        Completion completion = findElementAtPositionInList(position, model.getGraph().getCompletions());
        if (completion != null) {
            return completion;
        }
        Quantity quantity = findElementAtPositionInList(position, model.getGraph().getQuantities());
        if (quantity != null) {
            return quantity;
        }
        return findElementAtPositionInList(position, model.getGraph().getLinks());
    }

    private <T extends GraphElement> T findElementAtPositionInList(Position position, List<T> vertices) {
        Function<GraphElement,Integer> computeDistanceFunction = (element -> computePositionDistanceFromElement(position, element));
        return vertices.stream()
                .filter(element -> computeDistanceFunction.apply(element) < OBJECT_RADIUS)
                .min(Comparator.comparing(computeDistanceFunction))
                .orElse(null);
    }

    private int computePositionDistanceFromElement(Position position, GraphElement element) {
        return switch (element) {
            case Object object -> findPositionDistanceFromObject(position, object);
            case Completion completion -> findPositionDistanceFromCompletion(position, completion);
            case Quantity quantity -> findPositionDistanceFromQuantity(position, quantity);
            case Link link -> findPositionDistanceFromLink(position, link);
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

    private int findPositionDistanceFromLink(Position position, Link link) {

        var position1 = findVertexPosition(link.getOrigin(), link.getOriginLinkDirection());
        var position2 = findVertexPosition(link.getDestination(), link.getDestinationLinkDirection());
        var center = findLinkCenter(link);

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

    private List<GraphElement> listElementsToDragAmong(List<GraphElement> elements) {
        var newElements = new ArrayList<>(elements);
        addLinksBetweenElements(newElements);
        return newElements.stream()
                .filter(element -> !(element instanceof Link link && model.getLinkCenters().get(link) == null))
                .toList();
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
            this.changeDetector.notifyChangeCausedBy(this);
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
        if (hasDraggedElements) {
            changeDetector.notifyChangeCausedBy(this);
        }
    }

    private void reactToDrag(MouseEvent event) {
        Position position = findEventPosition(event);
        if (position.equals(clickPosition)) {
            return;
        }
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
        hasDraggedElements = false;
        for (GraphElement selectedElement: dragRelativeVectors.keySet()) {
            changeElementPosition(selectedElement, position.translate(this.dragRelativeVectors.get(selectedElement)));
            hasDraggedElements = true;
        }
        if (hasDraggedElements) {
            updateMagneticGuides();
            this.repaint();
        }
    }

    private void changeElementPosition(GraphElement element, Position position) {
        switch (element) {
            case Object object -> model.getPositions().put(object, position);
            case Completion completion -> model.getCompletionPositions().put(completion, position);
            case Quantity quantity -> model.getQuantityPositions().put(quantity, position);
            case Link link -> model.getLinkCenters().put(link, position);
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
                    .filter(completion -> !elements.contains(completion) && elements.contains(completion.getBase()))
                    .toList();
            quantitiesToAdd = model.getGraph().getQuantities().stream()
                    .filter(quantity -> !elements.contains(quantity) && elements.contains(quantity.getBase()))
                    .toList();
            linksToAdd = model.getGraph().getLinks().stream()
                    .filter(link -> !elements.contains(link) && elements.contains(link.getOrigin()) && elements.contains(link.getDestination()))
                    .toList();
            elements.addAll(completionsToAdd);
            elements.addAll(quantitiesToAdd);
            elements.addAll(linksToAdd);
        } while (!completionsToAdd.isEmpty() || !quantitiesToAdd.isEmpty() || !linksToAdd.isEmpty());
    }

    private void updateMagneticGuides() {
        this.clearGuides();
        List<Position> draggedPositions = dragRelativeVectors.keySet().stream()
                .filter(element -> !(element instanceof Link))
                .map(this::findElementPosition)
                .toList();
        List<Position> otherPositions = ModelHandler.streamElementsInModel(model)
                .filter(element -> !(element instanceof Link))
                .filter(element -> dragRelativeVectors.get(element) == null)
                .map(this::findElementPosition)
                .toList();
        Integer nearbyGuideDeltaX = findPossibleGuideAndComputeDelta(draggedPositions, otherPositions, Position::x);
        Integer nearbyGuideDeltaY = findPossibleGuideAndComputeDelta(draggedPositions, otherPositions, Position::y);
        if (nearbyGuideDeltaX == null && nearbyGuideDeltaY == null) {
            return;
        }
        int deltaX = nearbyGuideDeltaX == null ? 0 : nearbyGuideDeltaX;
        int deltaY = nearbyGuideDeltaY == null ? 0 : nearbyGuideDeltaY;
        Vector shift = new Vector(deltaX, deltaY);
        fillGuides(draggedPositions, shift, otherPositions);
        applyMagneticShiftToSelectedElements(shift);
    }

    private Integer findPossibleGuideAndComputeDelta(List<Position> draggedPositions, List<Position> otherPositions, Function<Position, Integer> coordinate) {
        return draggedPositions.stream()
                .map(draggedPosition -> findSinglePossibleGuideAndComputeDelta(draggedPosition, otherPositions, coordinate))
                .filter(Predicate.not(Objects::isNull))
                .min(Comparator.comparing(Math::abs))
                .orElse(null);
    }

    private Integer findSinglePossibleGuideAndComputeDelta(Position draggedPosition, List<Position> otherPositions, Function<Position, Integer> coordinate) {

        Integer draggedCoordinate = coordinate.apply(draggedPosition);

        return otherPositions.stream()
                .map(position -> coordinate.apply(position) - draggedCoordinate)
                .filter(delta -> Math.abs(delta) <= GUIDE_MAGNETISM_RADIUS)
                .min(Comparator.comparing(Math::abs))
                .orElse(null);
    }

    private void fillGuides(List<Position> draggedPositions, Vector draggedShift, List<Position> otherPositions) {
        this.guidesX.addAll(otherPositions.stream()
                .map(Position::x)
                .filter(x -> draggedPositions.stream().anyMatch(position -> position.x() + draggedShift.x() == x))
                .distinct()
                .toList());
        this.guidesY.addAll(otherPositions.stream()
                .map(Position::y)
                .filter(y -> draggedPositions.stream().anyMatch(position -> position.y() + draggedShift.y() == y))
                .distinct()
                .toList());
    }

    private void applyMagneticShiftToSelectedElements(Vector shift) {
        for (GraphElement selectedElement: selectedElements) {
            if (!(selectedElement instanceof Link)) {
                changeElementPosition(selectedElement, findElementPosition(selectedElement).translate(shift));
            }
        }
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
            this.changeDetector.notifyChangeCausedBy(this);
            repaint();
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

    public void selectAll() {
        this.selectedElements.clear();
        this.selectedElements.addAll(model.getGraph().getObjects());
        this.selectedElements.addAll(model.getGraph().getCompletions());
        this.selectedElements.addAll(model.getGraph().getQuantities());
        this.selectedElements.addAll(model.getGraph().getLinks());
        this.selectionChangeDetector.notifyChange();
        repaint();
    }

    public void paste(Drawing drawing) {
        Drawing pastedModel = GraphHandler.addModelToModel(drawing, this.model);
        List<GraphElement> newElements = ModelHandler.streamElementsInModel(pastedModel)
                .toList();

        // shift the new elements
        int shiftCount = computeShiftCount(drawing);
        Vector shift = PASTE_SHIFT.multiply(shiftCount);
        for (GraphElement newElement: newElements) {
            Position position = findElementPosition(newElement);
            if (position == null) {
                continue;
            }
            changeElementPosition(newElement, position.translate(shift));
        }

        this.selectedElements.clear();
        this.selectedElements.addAll(newElements);
        this.changeDetector.notifyChangeCausedBy(this);
        this.selectionChangeDetector.notifyChange();
        repaint();
    }

    private int computeShiftCount(Drawing pastedModel) {
        if (pastedModel != lastPastedModel) {
            lastPastedModel = pastedModel;
            lastPastedCount = 1;
            return lastPastedCount;
        }
        lastPastedCount += 1;
        return lastPastedCount;

    }
}
