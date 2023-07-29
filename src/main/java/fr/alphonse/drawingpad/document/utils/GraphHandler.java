package fr.alphonse.drawingpad.document.utils;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.DrawingJson;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.reference.Reference;
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
                .graph(Graph.builder()
                        .objects(new ArrayList<>())
                        .completions(new ArrayList<>())
                        .quantities(new ArrayList<>())
                        .links(new ArrayList<>())
                        .build())
                .positions(new HashMap<>())
                .completionPositions(new HashMap<>())
                .quantityPositions(new HashMap<>())
                .linkCenters(new HashMap<>())
                .note("")
                .build();
    }

    public static Drawing mapJsonToModel(DrawingJson json) {
        Drawing model = makeEmptyModel();
        fillModelWithJson(model, json);
        return model;
    }

    public static void fillModelWithJson(Drawing model, DrawingJson json) {

        Graph jsonGraph = json.getGraph();
        Graph newGraph = ModelStateManager.deepCopy(jsonGraph, Graph.class);

        // resolve references
        fillLinkOutlets(newGraph);
        fillVertices(newGraph);

        model.setGraph(newGraph);

        Map<Object, Position> positions = json.getPositions().keySet().stream().collect(Collectors.toMap(id -> GraphHandler.findGraphElementWithId(newGraph.getObjects(), id), json.getPositions()::get));
        Map<Completion, Position> completionPositions = json.getCompletionPositions().keySet().stream().collect(Collectors.toMap(id -> GraphHandler.findGraphElementWithId(newGraph.getCompletions(), id), json.getCompletionPositions()::get));
        Map<Quantity, Position> quantityPositions = json.getQuantityPositions().keySet().stream().collect(Collectors.toMap(id -> GraphHandler.findGraphElementWithId(newGraph.getQuantities(), id), json.getQuantityPositions()::get));
        Map<Link, Position> linkCenters = json.getLinkCenters().keySet().stream().collect(Collectors.toMap(id -> GraphHandler.findGraphElementWithId(newGraph.getLinks(), id), json.getLinkCenters()::get));

        model.getPositions().putAll(positions);
        model.getCompletionPositions().putAll(completionPositions);
        model.getQuantityPositions().putAll(quantityPositions);
        model.getLinkCenters().putAll(linkCenters);

        model.setNote(json.getNote());
    }

    private static void fillLinkOutlets(Graph graph) {
        for (Link link : graph.getLinks()) {
            link.setDirectFactor(DirectFactor.builder()
                    .link(link)
                    .build());
            link.setReverseFactor(ReverseFactor.builder()
                    .link(link)
                    .build());
        }
    }

    private static void fillVertices(Graph graph) {
        for (Link link : graph.getLinks()) {
            link.setOrigin(GraphHandler.findVertexAtReference(link.getOriginReference(), graph));
            link.setDestination(GraphHandler.findVertexAtReference(link.getDestinationReference(), graph));
        }
        for (Completion completion: graph.getCompletions()) {
            completion.setBase(GraphHandler.findVertexAtReference(completion.getBaseReference(), graph));
        }
        for (Quantity quantity: graph.getQuantities()) {
            quantity.setBase(GraphHandler.findVertexAtReference(quantity.getBaseReference(), graph));
        }
    }

    public static DrawingJson mapModelToJson(Drawing model) {
        return DrawingJson.builder()
                .graph(ModelStateManager.deepCopy(model.getGraph(), Graph.class))
                .positions(model.getPositions().keySet().stream()
                        .collect(Collectors.toMap(Object::getId,model.getPositions()::get)))
                .completionPositions(model.getCompletionPositions().keySet().stream()
                        .collect(Collectors.toMap(Completion::getId,model.getCompletionPositions()::get)))
                .quantityPositions(model.getQuantityPositions().keySet().stream()
                        .collect(Collectors.toMap(Quantity::getId,model.getQuantityPositions()::get)))
                .linkCenters(model.getLinkCenters().keySet().stream()
                        .collect(Collectors.toMap(Link::getId,model.getLinkCenters()::get)))
                .note(model.getNote())
                .build();
    }

    public static Vertex findVertexAtReference(Reference reference, Graph graph) {
        final int id = reference.id();
        return switch (reference.type()) {
            case OBJECT -> findGraphElementWithId(graph.getObjects(), id);
            case COMPLETION -> findGraphElementWithId(graph.getCompletions(), id);
            case QUANTITY -> findGraphElementWithId(graph.getQuantities(), id);
            case DIRECT_LINK -> findGraphElementWithId(graph.getLinks(), id).getDirectFactor();
            case REVERSE_LINK -> findGraphElementWithId(graph.getLinks(), id).getReverseFactor();
        };
    }

    public static <T extends GraphElement> T findGraphElementWithId(List<T> vertices, int id) {
        return vertices.stream()
                .filter(element -> element.getId() == id)
                .findFirst().orElseThrow();
    }

    public static Drawing extractModelWithElements(Drawing model, List<GraphElement> elements) {
        if (areThereElementsWithoutDependencies(elements)) {
            return null;
        }
        Graph graph = model.getGraph();
        return Drawing.builder()
                .graph(Graph.builder()
                        .objects(graph.getObjects().stream().filter(elements::contains).toList())
                        .completions(graph.getCompletions().stream().filter(elements::contains).toList())
                        .quantities(graph.getQuantities().stream().filter(elements::contains).toList())
                        .links(graph.getLinks().stream().filter(elements::contains).toList())
                        .build())
                .positions(model.getPositions().keySet().stream().filter(elements::contains).collect(Collectors.toMap(Function.identity(), model.getPositions()::get)))
                .completionPositions(model.getCompletionPositions().keySet().stream().filter(elements::contains).collect(Collectors.toMap(Function.identity(), model.getCompletionPositions()::get)))
                .quantityPositions(model.getQuantityPositions().keySet().stream().filter(elements::contains).collect(Collectors.toMap(Function.identity(), model.getQuantityPositions()::get)))
                .linkCenters(model.getLinkCenters().keySet().stream().filter(elements::contains).collect(Collectors.toMap(Function.identity(), model.getLinkCenters()::get)))
                .note("")
                .build();
    }

    private boolean areThereElementsWithoutDependencies(List<GraphElement> elements) {
        return elements.stream().anyMatch(element -> switch (element) {
            case Object ignored -> false;
            case Completion completion -> !elements.contains(completion.getBase().getElement());
            case Quantity quantity -> !elements.contains(quantity.getBase().getElement());
            case Link link -> !elements.contains(link.getOrigin().getElement()) || !elements.contains(link.getDestination().getElement());
        });
    }

    public static Drawing addModelToModel(Drawing modelToAdd, Drawing model) {
        Drawing newModelToAdd = copyModel(modelToAdd);

        Graph graph = model.getGraph();
        Graph graphToAdd = newModelToAdd.getGraph();

        changeElementsIdsSoTheyCanBeAdded(graph.getObjects(), graphToAdd.getObjects());
        changeElementsIdsSoTheyCanBeAdded(graph.getCompletions(), graphToAdd.getCompletions());
        changeElementsIdsSoTheyCanBeAdded(graph.getQuantities(), graphToAdd.getQuantities());
        changeElementsIdsSoTheyCanBeAdded(graph.getLinks(), graphToAdd.getLinks());

        correctVertexReferences(graphToAdd);

        graph.getObjects().addAll(graphToAdd.getObjects());
        graph.getCompletions().addAll(graphToAdd.getCompletions());
        graph.getQuantities().addAll(graphToAdd.getQuantities());
        graph.getLinks().addAll(graphToAdd.getLinks());

        model.getPositions().putAll(newModelToAdd.getPositions());
        model.getCompletionPositions().putAll(newModelToAdd.getCompletionPositions());
        model.getQuantityPositions().putAll(newModelToAdd.getQuantityPositions());
        model.getLinkCenters().putAll(newModelToAdd.getLinkCenters());

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

    private static void correctVertexReferences(Graph graph) {
        for (Link link : graph.getLinks()) {
            link.setOriginReference(new Reference(link.getOriginReference().type(), link.getOrigin().getElement().getId()));
            link.setDestinationReference(new Reference(link.getDestinationReference().type(), link.getDestination().getElement().getId()));
        }
        for (Completion completion: graph.getCompletions()) {
            completion.setBaseReference(new Reference(completion.getBaseReference().type(), completion.getBase().getElement().getId()));
        }
        for (Quantity quantity: graph.getQuantities()) {
            quantity.setBaseReference(new Reference(quantity.getBaseReference().type(), quantity.getBase().getElement().getId()));
        }
    }
}
