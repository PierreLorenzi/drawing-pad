package fr.alphonse.drawingpad.document.utils;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.DrawingJson;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.geometry.Vector;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.*;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class GraphHandler {

    public static Drawing makeEmptyModel() {
        return Drawing.builder()
                .elements(new ArrayList<>())
                .positions(new HashMap<>())
                .namePositions(new HashMap<>())
                .note("")
                .build();
    }

    public static Drawing mapJsonToModel(DrawingJson json) {
        Drawing model = makeEmptyModel();
        fillModelWithJson(model, json);
        return model;
    }

    public static void fillModelWithJson(Drawing model, DrawingJson json) {

        List<GraphElement> jsonElements = json.getElements();
        List<GraphElement> newElements = ModelStateManager.deepCopy(jsonElements, GraphElement.class);

        // resolve references
        fillVertices(newElements);

        model.setElements(newElements);

        Map<GraphElement, Position> positions = mapKeys(json.getPositions(), id -> findElementWithId(id, newElements));
        model.getPositions().putAll(positions);

        Map<GraphElement, Vector> namePositions = mapKeys(json.getNamePositions(), id -> findElementWithId(id, newElements));
        model.getNamePositions().putAll(namePositions);

        model.setNote(json.getNote());
    }

    private static void fillVertices(List<GraphElement> elements) {
        for (GraphElement element: elements) {
            switch (element) {
                case Object ignored -> doNothing();
                case Completion completion -> completion.setBase(GraphHandler.findElementWithId(completion.getBaseId(), elements));
                case Quantity quantity -> quantity.setBase(GraphHandler.findElementWithId(quantity.getBaseId(), elements));
                case Link link -> {
                    link.setOrigin(GraphHandler.findElementWithId(link.getOriginId(), elements));
                    link.setDestination(GraphHandler.findElementWithId(link.getDestinationId(), elements));
                }
            }
        }
    }

    private static GraphElement findElementWithId(int id, List<GraphElement> elements) {
        return elements.stream()
                .filter(element -> element.getId() == id)
                .findFirst()
                .orElseThrow();
    }

    public static void doNothing() {}

    private static <K1, K2, V> Map<K2,V> mapKeys(Map<K1,V> map, Function<K1, K2> function) {
        return map.keySet().stream()
                .collect(Collectors.toMap(function, map::get));
    }

    public static DrawingJson mapModelToJson(Drawing model) {
        return DrawingJson.builder()
                .elements(ModelStateManager.deepCopy(model.getElements(), GraphElement.class))
                .positions(mapKeys(model.getPositions(), GraphElement::getId))
                .namePositions(mapKeys(model.getNamePositions(), GraphElement::getId))
                .note(model.getNote())
                .build();
    }

    public static Drawing extractModelWithElements(Drawing model, List<GraphElement> elements) {
        if (areThereElementsWithoutDependencies(elements)) {
            return null;
        }
        return Drawing.builder()
                .elements(new ArrayList<>(elements))
                .positions(model.getPositions().keySet().stream().filter(elements::contains).collect(Collectors.toMap(Function.identity(), model.getPositions()::get)))
                .namePositions(model.getNamePositions().keySet().stream().filter(elements::contains).collect(Collectors.toMap(Function.identity(), model.getNamePositions()::get)))
                .note("")
                .build();
    }

    private boolean areThereElementsWithoutDependencies(List<GraphElement> elements) {
        return elements.stream().anyMatch(element -> switch (element) {
            case Object ignored -> false;
            case Completion completion -> !elements.contains(completion.getBase());
            case Quantity quantity -> !elements.contains(quantity.getBase());
            case Link link -> !elements.contains(link.getOrigin()) || !elements.contains(link.getDestination());
        });
    }

    public static Drawing addModelToModel(Drawing modelToAdd, Drawing model) {
        Drawing newModelToAdd = copyModel(modelToAdd);

        List<GraphElement> elements = model.getElements();
        List<GraphElement> elementsToAdd = newModelToAdd.getElements();

        changeElementsIdsSoTheyCanBeAdded(elements, elementsToAdd);

        correctVertexReferences(elementsToAdd);

        elements.addAll(elementsToAdd);

        model.getPositions().putAll(newModelToAdd.getPositions());
        model.getNamePositions().putAll(newModelToAdd.getNamePositions());

        return newModelToAdd;
    }

    private static Drawing copyModel(Drawing model) {
        DrawingJson json = mapModelToJson(model);
        return mapJsonToModel(json);
    }

    private static <T extends GraphElement> void changeElementsIdsSoTheyCanBeAdded(List<T> elements, List<T> elementsToAdd) {
        int id = findAvailableId(elements);
        for (T elementToAdd: elementsToAdd) {
            elementToAdd.setId(id);
            id += 1;
        }
    }

    public static int findAvailableId(List<? extends GraphElement> elements) {
        int maxId = elements.stream()
                .mapToInt(GraphElement::getId)
                .max()
                .orElse(0);
        return 1 + maxId;
    }

    private static void correctVertexReferences(List<GraphElement> elements) {
        for (GraphElement element: elements) {
            switch (element) {
                case Object ignored -> doNothing();
                case Completion completion -> completion.setBaseId(completion.getBase().getId());
                case Quantity quantity -> quantity.setBaseId(quantity.getBase().getId());
                case Link link -> {
                    link.setOriginId(link.getOrigin().getId());
                    link.setDestinationId(link.getDestination().getId());
                }
            }
        }
    }

    public void clearModel(Drawing drawing) {
        drawing.getElements().clear();
        drawing.getPositions().clear();
        drawing.getNamePositions().clear();
        drawing.setNote("");
    }
}
