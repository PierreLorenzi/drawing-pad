package fr.alphonse.drawingpad.view;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.geometry.Rectangle;
import fr.alphonse.drawingpad.data.geometry.Vector;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.reference.LinkDirection;
import fr.alphonse.drawingpad.data.model.value.Graduation;
import fr.alphonse.drawingpad.document.utils.ChangeDetector;
import fr.alphonse.drawingpad.document.utils.Graduations;
import fr.alphonse.drawingpad.document.utils.GraphHandler;
import fr.alphonse.drawingpad.view.internal.GeometryManager;
import fr.alphonse.drawingpad.view.internal.ModelHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DrawingComponent extends JComponent {

    private final Drawing model;

    private final ChangeDetector<?,?> changeDetector;

    private final GeometryManager geometryManager;

    private final List<GraphElement> selectedElements = new ArrayList<>();

    public static final Function<List<GraphElement>, ?> SELECTION_STATE_FUNCTION = elements -> elements.stream().map(GraphElement::getId).collect(Collectors.toSet());

    private final ChangeDetector<List<GraphElement>,?> selectionChangeDetector = new ChangeDetector<>(selectedElements, SELECTION_STATE_FUNCTION);

    private Position clickPosition;

    private Map<GraphElement, Vector> dragRelativeVectors;

    private boolean hasDragged = false;

    private GraphElement lastSelectedElement = null;

    private Position selectionRectangleOrigin = null;

    private Position selectionRectangleDestination = null;

    private GraphElement newLinkOrigin = null;

    private LinkDirection newOriginLinkDirection = null;

    private Position newLinkCenter = null;

    private Graduation newLinkGraduation;

    private boolean mustLinkBeAligned = false;

    private boolean mustLinkBeAlignedOnLinkDirections = false;

    private GraphElement draggedNameElement;

    private Vector draggedNameRelativePosition;

    private Position draggedNameCenter;

    private final List<Integer> guidesX = new ArrayList<>();

    private final List<Integer> guidesY = new ArrayList<>();

    private Drawing lastPastedModel;

    private int lastPastedCount = 0;

    private FontMetrics nameFontMetrics;

    private static final int OBJECT_RECTANGLE_RADIUS = 8;

    private static final float ARROW_DISTANCE = 28;

    private static final double ARROW_ANGLE = Math.toRadians(25);

    private static final float ARROW_LENGTH = 10;

    private static final BasicStroke SHADOW_STROKE = new BasicStroke(3);

    private static final BasicStroke SELECTED_LINK_STROKE = new BasicStroke(3);

    private static final BasicStroke LINK_STROKE = new BasicStroke(2);

    private static final Color LINK_COLOR = new Color(70, 70, 70);

    private final static float[] BASE_LINE_DASH = new float[]{1.0f, 2.0f};

    private static final BasicStroke BASE_LINE_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, BASE_LINE_DASH, 0.0f);

    private static final int ARROW_KEY_DELTA = 1;

    private static final Vector ARROW_KEY_UP_DELTA = new Vector(0, -ARROW_KEY_DELTA);

    private static final Vector ARROW_KEY_DOWN_DELTA = new Vector(0, ARROW_KEY_DELTA);

    private static final Vector ARROW_KEY_LEFT_DELTA = new Vector(-ARROW_KEY_DELTA, 0);

    private static final Vector ARROW_KEY_RIGHT_DELTA = new Vector(ARROW_KEY_DELTA, 0);

    private static final Color SELECTION_COLOR = Color.getHSBColor(206f/360, 1f, .9f);

    private static final int GUIDE_MAGNETISM_RADIUS = 3;

    private static final int INITIAL_DISTANCE_FROM_BASE = 30;

    private static final  Vector PASTE_SHIFT = new Vector(70, 30);

    private static final Font NAME_FONT = new Font("Georgia", Font.PLAIN, 14);

    private static final int NAME_MARGIN = 3;

    public DrawingComponent(Drawing model, ChangeDetector<?,?> changeDetector) {
        super();
        this.model = model;
        this.changeDetector = changeDetector;
        this.geometryManager = new GeometryManager(model);
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
                DrawingComponent.this.reactToMove(e);
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
        this.selectedElements.removeIf(element -> !model.getElements().contains(element));
        if (this.selectedElements.size() != selectionSizeBeforeFilter) {
            this.selectionChangeDetector.notifyChange();
        }

        this.repaint();
    }

    public void delete() {
        for (GraphElement selectedElement: selectedElements) {
            ModelHandler.deleteElement(selectedElement, model);
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

        // draw selection rectangle
        if (this.selectionRectangleOrigin != null && this.selectionRectangleDestination != null) {
            g.setColor(Color.LIGHT_GRAY);
            var originX = Math.min(selectionRectangleOrigin.x(), selectionRectangleDestination.x());
            var originY = Math.min(selectionRectangleOrigin.y(), selectionRectangleDestination.y());
            var width = Math.abs(selectionRectangleOrigin.x() - selectionRectangleDestination.x());
            var height = Math.abs(selectionRectangleOrigin.y() - selectionRectangleDestination.y());
            g.fillRect(originX, originY, width, height);
        }

        // draw names
        drawNames(g);

        // draw guides
        g.setColor(Color.blue);
        for (Integer guideX: guidesX) {
            g.drawLine(guideX, -translationY, guideX, translationY);
        }
        for (Integer guideY: guidesY) {
            g.drawLine(-translationX, guideY, translationX, guideY);
        }

        // draw elements
        for (Class<? extends GraphElement> type: GeometryManager.DISPLAYED_ELEMENT_TYPES) {
            streamElementsOfType(model.getElements(), type)
                    .forEach(element -> drawElement(element, g));
        }

        // draw link being dragged
        if (newLinkOrigin != null) {
            drawLinkBeingDragged(g);
        }

        // save metrics
        if (nameFontMetrics == null) {
            nameFontMetrics = g.getFontMetrics(NAME_FONT);
        }

        g.translate(-translationX, -translationY);
    }

    private void drawNames(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(NAME_FONT);
        for (GraphElement element: model.getNamePositions().keySet()) {
            String name = element.getName();
            if (name == null || name.isEmpty()) {
                continue;
            }
            Position namePosition = computeNamePositionOfElement(element);
            g.drawString(name, namePosition.x(), namePosition.y());
        }
    }

    private Position computeNamePositionOfElement(GraphElement element) {
        Vector nameRelativePosition = model.getNamePositions().get(element);
        Position elementPosition = geometryManager.findElementPosition(element);
        return elementPosition.translate(nameRelativePosition);
    }

    private static <T extends GraphElement> Stream<T> streamElementsOfType(List<GraphElement> elements, Class<T> type) {
        return elements.stream()
                .filter(type::isInstance)
                .map(type::cast);
    }

    private void drawElement(GraphElement element, Graphics g) {
        switch (element) {
            case Object object -> drawObject(object, g);
            case Completion completion -> drawCompletion(completion, g);
            case Quantity quantity -> drawQuantity(quantity, g);
            case Link link -> drawLink(link, g);
        }
    }


    private void drawObject(Object object, Graphics g) {
        var position = model.getPositions().get(object);

        g.setColor(Color.GRAY);
        ((Graphics2D) g).setStroke(SHADOW_STROKE);
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

    private void drawCompletion(Completion completion, Graphics g) {
        var position = model.getPositions().get(completion);

        if (selectedElements.contains(completion)) {
            g.setColor(SELECTION_COLOR);
        }
        else {
            g.setColor(Color.BLUE);
        }
        g.fillOval(position.x()-GeometryManager.CIRCLE_RADIUS, position.y()-GeometryManager.CIRCLE_RADIUS, 2*GeometryManager.CIRCLE_RADIUS, 2*GeometryManager.CIRCLE_RADIUS);
        drawBaseJoin(completion, completion.getBase(), g);
    }

    private void drawBaseJoin(GraphElement element, GraphElement base, Graphics g) {
        var position1 = geometryManager.findElementPosition(element);
        var position2 = geometryManager.findElementPosition(base);
        var linePosition1 = geometryManager.computeArrowMeetingPositionWithElement(position2, position1, element);
        var linePosition2 = geometryManager.computeArrowMeetingPositionWithElement(position1, position2, base);
        ((Graphics2D)g).setStroke(BASE_LINE_STROKE);
        g.setColor(Color.BLACK);
        g.drawLine(linePosition1.x(), linePosition1.y(), linePosition2.x(), linePosition2.y());
    }

    private void drawQuantity(Quantity quantity, Graphics g) {
        var position = model.getPositions().get(quantity);

        if (selectedElements.contains(quantity)) {
            g.setColor(SELECTION_COLOR);
        }
        else {
            g.setColor(Color.ORANGE);
        }
        g.fillOval(position.x()-GeometryManager.CIRCLE_RADIUS, position.y()-GeometryManager.CIRCLE_RADIUS, 2*GeometryManager.CIRCLE_RADIUS, 2*GeometryManager.CIRCLE_RADIUS);
        drawBaseJoin(quantity, quantity.getBase(), g);
    }

    private void drawLink(Link link, Graphics g) {
        var position1 = geometryManager.findVertexPosition(link.getOrigin(), link.getOriginLinkDirection());
        var position2 = geometryManager.findVertexPosition(link.getDestination(), link.getDestinationLinkDirection());
        var center = findLinkCenter(link);
        var linePosition1 = geometryManager.computeArrowMeetingPositionWithElement(center != null ? center : position2, position1, link.getOrigin());
        var linePosition2 = geometryManager.computeArrowMeetingPositionWithElement(center != null ? center : position1, position2, link.getDestination());
        boolean isSelected = selectedElements.contains(link);
        drawLinkBetweenPositions(linePosition1, center, linePosition2, g, isSelected);
        if (Graduations.isStrictlyGreaterThanOne(link.getFactor().getGraduation())) {
            Position secondPosition = (center != null) ? center : position2;
            drawArrow(position1, secondPosition, g);
        }
        else if (Graduations.isStrictlyLesserThanOne(link.getFactor().getGraduation())) {
            Position secondPosition = (center != null) ? center : position1;
            drawArrow(position2, secondPosition, g);
        }
    }

    private void drawLinkBetweenPositions(Position position1, Position center, Position position2, Graphics g, boolean isSelected) {
        if (isSelected) {
            g.setColor(SELECTION_COLOR);
            ((Graphics2D) g).setStroke(SELECTED_LINK_STROKE);
        }
        else {
            g.setColor(LINK_COLOR);
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

    private void drawLinkBeingDragged(Graphics g) {
        var position1 = geometryManager.findVertexPosition(newLinkOrigin, newOriginLinkDirection);
        Position position2 = findMousePosition();
        if (mustLinkBeAligned) {
            position2 = alignOnPosition(position2, position1);
        }
        else if (mustLinkBeAlignedOnLinkDirections) {
            position2 = shiftDestinationForLinkShapeMagnetism(position1, position2);
        }
        var linePosition1 = geometryManager.computeArrowMeetingPositionWithElement(newLinkCenter != null ? newLinkCenter : position2, position1, newLinkOrigin);
        drawLinkBetweenPositions(linePosition1, newLinkCenter, position2, g, false);
    }

    private static Position alignOnPosition(Position position, Position basePosition) {
        Vector vector = Vector.between(basePosition, position);
        if (Math.abs(vector.x()) >= Math.abs(vector.y())) {
            return new Position(position.x(), basePosition.y());
        }
        else {
            return new Position(basePosition.x(), position.y());
        }
    }

    private Position findMousePosition() {
        Point mousePoint = MouseInfo.getPointerInfo().getLocation();
        Point point = new Point(mousePoint);
        SwingUtilities.convertPointFromScreen(point, this);
        return findPointPosition(point);
    }

    private Position shiftDestinationForLinkShapeMagnetism(Position source, Position destination) {
        List<Vector> linkVectors = model.getElements().stream()
                .filter(Link.class::isInstance)
                .map(Link.class::cast)
                .map(this::computeLinkVectorIfChosenByUser)
                .filter(Objects::nonNull)
                .toList();
        Vector draggedVector = Vector.between(source, destination);
        Vector magnetismShift = linkVectors.stream()
                .flatMap(vector -> Stream.of(Vector.between(draggedVector, vector), Vector.between(draggedVector, new Vector(-vector.x(), vector.y()))))
                .filter(vector -> vector.length() < 30)
                .min(Comparator.comparing(Vector::length))
                .orElse(null);
        if (magnetismShift == null) {
            return destination;
        }
        return destination.translate(magnetismShift);
    }

    private Vector computeLinkVectorIfChosenByUser(Link link) {
        GraphElement origin = link.getOrigin();
        GraphElement destination = link.getDestination();
        Position position1 = model.getPositions().get(origin);
        Position position2 = model.getPositions().get(destination);
        if (position1 == null || position2 == null) {
            return null;
        }
        return Vector.between(position1, position2);
    }

    private void reactToClick(MouseEvent event) {
        requestFocusInWindow();
        Position position = findEventPosition(event);
        clickPosition = position;
        var clickedElement = geometryManager.findElementAtPosition(position);
        // if drawing a link
        if (this.newLinkOrigin != null) {
            this.repaint();
            if (clickedElement == null) {
                if (isCommandPressedDuringEvent(event)) {
                    Position magneticPosition;
                    Position originPosition = geometryManager.findElementPosition(this.newLinkOrigin);
                    if (isShiftKeyPressed(event)) {
                        magneticPosition = alignOnPosition(position, originPosition);
                    }
                    else {
                        magneticPosition = shiftDestinationForLinkShapeMagnetism(originPosition, position);
                    }
                    clickedElement = ModelHandler.addObject(magneticPosition, model);
                }
                else {
                    this.newLinkCenter = position;
                    return;
                }
            }
            var origin = this.newLinkOrigin;
            var originLinkDirection = this.newOriginLinkDirection;
            var center = this.newLinkCenter;
            var graduation = this.newLinkGraduation;
            this.newLinkOrigin = null;
            this.newOriginLinkDirection = null;
            this.newLinkCenter = null;
            this.newLinkGraduation = null;
            this.repaint();
            if (clickedElement == origin) {
                return;
            }
            LinkDirection destinationLinkDirection = geometryManager.findLinkDirectionAtPosition(clickedElement, position);
            ModelHandler.addLink(origin, originLinkDirection, clickedElement, destinationLinkDirection, center, graduation, model);
            changeDetector.notifyChangeCausedBy(this);
            return;
        }
        // if press with command, add object or link
        if (isCommandPressedDuringEvent(event)) {
            if (clickedElement == null) {
                ModelHandler.addObject(position, model);
                changeDetector.notifyChangeCausedBy(this);
                repaint();
            }
            else {
                newLinkOrigin = clickedElement;
                newOriginLinkDirection = geometryManager.findLinkDirectionAtPosition(clickedElement, position);
                newLinkGraduation = isOptionKeyPressedDuringEvent(event) ? Graduation.ONE : Graduation.GREATER;
            }
            return;
        }
        // if press with control, add completion
        if (isControlPressed(event) && !isShiftKeyPressed(event)) {
            if (clickedElement == null) {
                return;
            }
            Position newPosition = makePositionFromBase(geometryManager.findElementPosition(clickedElement));
            ModelHandler.addCompletion(clickedElement, newPosition, model);
            changeDetector.notifyChangeCausedBy(this);
            repaint();
            return;
        }
        // if press with control and shift, add quantity
        if (isControlPressed(event) && isShiftKeyPressed(event)) {
            if (clickedElement == null) {
                return;
            }
            Position newPosition = makePositionFromBase(geometryManager.findElementPosition(clickedElement));
            ModelHandler.addQuantity(clickedElement, newPosition, model);
            changeDetector.notifyChangeCausedBy(this);
            repaint();
            return;
        }
        boolean isShiftKeyPressed = isShiftKeyPressed(event);
        // name drag
        if (!isShiftKeyPressed && clickedElement == null) {
            GraphElement element = findElementWhoseNameContainsPosition(position);
            if (element != null) {
                this.draggedNameElement = element;
                this.draggedNameCenter = geometryManager.findElementPosition(element);
                Position namePosition = computeNamePositionOfElement(element);
                this.draggedNameRelativePosition = Vector.between(position, namePosition);
                return;
            }
        }
        this.lastSelectedElement = clickedElement;
        if (clickedElement == null && !isShiftKeyPressed) {
            this.selectionRectangleOrigin = position;
        }
        boolean alreadySelected = clickedElement != null && selectedElements.contains(clickedElement);
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
        hasDragged = false;
        var canDrag = !(isShiftKeyPressed && (clickedElement == null || alreadySelected));
        if (canDrag) {
            this.dragRelativeVectors = listElementsToDragAmong(this.selectedElements).stream()
                .collect(Collectors.toMap(Function.identity(), element -> Vector.between(position, geometryManager.findElementPosition(element))));
        }
        this.repaint();
    }

    private static boolean isControlPressed(MouseEvent event) {
        return (event.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0;
    }

    private static boolean isShiftKeyPressed(MouseEvent event) {
        return (event.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
    }

    private static boolean isCommandPressedDuringEvent(MouseEvent event) {
        return (event.getModifiersEx() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) != 0;
    }

    private static boolean isOptionKeyPressedDuringEvent(MouseEvent event) {
        return (event.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0;
    }

    private Position findEventPosition(MouseEvent event) {
        return findPointPosition(event.getPoint());
    }

    private Position findPointPosition(Point point) {
        return new Position(point.x - this.getBounds().width/2, point.y - this.getBounds().height/2);
    }

    private GraphElement findElementWhoseNameContainsPosition(Position position) {
        for (GraphElement element: model.getNamePositions().keySet()) {
            String name = element.getName();
            if (name == null || name.isEmpty()) {
                continue;
            }
            Position namePosition = computeNamePositionOfElement(element);
            Rectangle titleRectangle = findRectangleOfNameAtPosition(name, namePosition);
            Rectangle clickableTitleRectangle = titleRectangle.increaseByMargin(NAME_MARGIN);
            if (clickableTitleRectangle.containsPosition(position)) {
                return element;
            }
        }
        return null;
    }

    private Rectangle findRectangleOfNameAtPosition(String name, Position position) {
        int width = nameFontMetrics.stringWidth(name);
        int ascent = nameFontMetrics.getAscent();
        int descent = nameFontMetrics.getDescent();
        return new Rectangle(position.x(), position.y() - ascent, width, ascent + descent);
    }

    private Position findLinkCenter(Link link) {
        return model.getPositions().get(link);
    }

    private Position makePositionFromBase(Position basePosition) {
        return new Position(basePosition.x() + INITIAL_DISTANCE_FROM_BASE, basePosition.y());
    }

    private List<GraphElement> listElementsToDragAmong(List<GraphElement> elements) {
        var newElements = elements.stream()
                .filter(element -> model.getPositions().get(element) != null)
                .collect(Collectors.toCollection(ArrayList::new));
        addDependentElements(newElements);
        return newElements.stream()
                .filter(element -> !(element instanceof Link link && model.getPositions().get(link) == null))
                .toList();
    }

    private void reactToRelease(MouseEvent event) {
        if (this.draggedNameElement != null) {
            this.draggedNameElement = null;
            this.draggedNameCenter = null;
            this.draggedNameRelativePosition = null;
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
        if (!hasDragged && lastSelectedElement != null && this.selectedElements.size() > 1 && !isShiftKeyPressed(event)) {
            this.selectedElements.removeIf(Predicate.not(Predicate.isEqual(lastSelectedElement)));
            this.repaint();
            this.selectionChangeDetector.notifyChange();
        }
        this.dragRelativeVectors = null;
        if (hasDragged) {
            changeDetector.notifyChangeCausedBy(this);
        }
    }

    private void reactToDrag(MouseEvent event) {
        Position position = findEventPosition(event);
        if (position.equals(clickPosition)) {
            return;
        }
        if (this.draggedNameElement != null) {
            Position newNamePosition = position.translate(this.draggedNameRelativePosition);
            Rectangle nameRectangle = findRectangleOfNameAtPosition(this.draggedNameElement.getName(), newNamePosition);
            Position nameCenter = nameRectangle.findCenter();
            Position elementPosition = geometryManager.findElementPosition(this.draggedNameElement);
            Vector shift = applyMagneticShift(List.of(nameCenter), List.of(elementPosition));
            newNamePosition = newNamePosition.translate(shift);
            Vector newNameRelativePosition = Vector.between(this.draggedNameCenter, newNamePosition);
            this.model.getNamePositions().put(this.draggedNameElement, newNameRelativePosition);
            this.repaint();
            return;
        }
        if (selectionRectangleOrigin != null) {
            this.selectionRectangleDestination = position;
            Rectangle selectionRectangle = Rectangle.between(selectionRectangleOrigin, selectionRectangleDestination);
            List<GraphElement> elementsInRectangle = model.getElements().stream()
                    .filter(element -> !(element instanceof Link))
                    .filter(object -> selectionRectangle.containsPosition(model.getPositions().get(object)))
                    .toList();
            this.selectedElements.clear();
            this.selectedElements.addAll(elementsInRectangle);
            addLinksBetweenElements(selectedElements);
            this.selectionChangeDetector.notifyChange();
            this.repaint();
            return;
        }
        if (dragRelativeVectors != null) {
            this.hasDragged = !dragRelativeVectors.isEmpty();
            for (GraphElement selectedElement: dragRelativeVectors.keySet()) {
                model.getPositions().put(selectedElement, position.translate(this.dragRelativeVectors.get(selectedElement)));
            }
            updateMagneticGuides();
        }
        this.repaint();
    }

    private void addLinksBetweenElements(List<GraphElement> elements) {
        int sizeBefore;
        do {
            sizeBefore = elements.size();
            model.getElements().stream()
                    .filter(Link.class::isInstance).map(Link.class::cast)
                    .filter(Predicate.not(elements::contains))
                    .filter(link -> elements.contains(link.getOrigin()) && elements.contains(link.getDestination()))
                    .forEach(elements::add);
        } while(elements.size() > sizeBefore);
    }

    private void addDependentElements(List<GraphElement> elements) {
        int size = elements.size();
        for (int i=0 ; i<size ; i++) {
            GraphElement element = elements.get(i);
            List<GraphElement> dependentElements = ModelHandler.listDependentElements(element, model);
            for (GraphElement dependentElement: dependentElements) {
                if (!elements.contains(dependentElement)) {
                    elements.add(dependentElement);
                }
            }
        }
    }

    private void updateMagneticGuides() {
        List<Position> draggedPositions = dragRelativeVectors.keySet().stream()
                .filter(element -> !(element instanceof Link))
                .map(geometryManager::findElementPosition)
                .toList();
        List<Position> otherPositions = model.getElements().stream()
                .filter(element -> !(element instanceof Link))
                .filter(element -> dragRelativeVectors.get(element) == null)
                .map(geometryManager::findElementPosition)
                .toList();
        Vector shift = applyMagneticShift(draggedPositions, otherPositions);
        if (shift.length() == 0) return;
        applyMagneticShiftToDraggedElements(shift);
    }

    private Vector applyMagneticShift(List<Position> draggedPositions, List<Position> otherPositions) {
        this.clearGuides();
        Integer nearbyGuideDeltaX = findPossibleGuideAndComputeDelta(draggedPositions, otherPositions, Position::x);
        Integer nearbyGuideDeltaY = findPossibleGuideAndComputeDelta(draggedPositions, otherPositions, Position::y);
        if (nearbyGuideDeltaX == null && nearbyGuideDeltaY == null) {
            return new Vector(0, 0);
        }
        int deltaX = nearbyGuideDeltaX == null ? 0 : nearbyGuideDeltaX;
        int deltaY = nearbyGuideDeltaY == null ? 0 : nearbyGuideDeltaY;
        Vector shift = new Vector(deltaX, deltaY);
        fillGuides(draggedPositions, shift, otherPositions);
        return shift;
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

    private void applyMagneticShiftToDraggedElements(Vector shift) {
        for (GraphElement selectedElement: dragRelativeVectors.keySet()) {
            if (!(selectedElement instanceof Link link && model.getPositions().get(link) == null)) {
                model.getPositions().put(selectedElement, geometryManager.findElementPosition(selectedElement).translate(shift));
            }
        }
    }

    private void clearGuides() {
        this.guidesX.clear();
        this.guidesY.clear();
    }

    private void reactToMove(MouseEvent e) {
        // if a new linked is dragged, repaint
        if (newLinkOrigin != null) {
            this.mustLinkBeAligned = isShiftKeyPressed(e);
            this.mustLinkBeAlignedOnLinkDirections = isCommandPressedDuringEvent(e);
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
        ArrayList<GraphElement> elementsToMove = new ArrayList<>(selectedElements);
        addDependentElements(elementsToMove);
        for (GraphElement element: elementsToMove) {
            if (element instanceof Link link && model.getPositions().get(link) == null) {
                continue;
            }
            this.model.getPositions().put(element, this.model.getPositions().get(element).translate(delta));
            needsRefresh = true;
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
        this.selectedElements.addAll(model.getElements());
        this.selectionChangeDetector.notifyChange();
        repaint();
    }

    public void paste(Drawing drawing) {
        Drawing pastedModel = GraphHandler.addModelToModel(drawing, this.model);
        List<GraphElement> newElements = pastedModel.getElements();

        // shift the new elements
        int shiftCount = computeShiftCount(drawing);
        Vector shift = PASTE_SHIFT.multiply(shiftCount);
        for (GraphElement newElement: newElements) {
            if (newElement instanceof Link link && model.getPositions().get(link) == null) {
                continue;
            }
            Position position = geometryManager.findElementPosition(newElement);
            model.getPositions().put(newElement, position.translate(shift));
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
