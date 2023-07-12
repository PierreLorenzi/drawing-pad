package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.Example;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.value.GraduatedValue;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@UtilityClass
public class ModelHandler {

    public static void addObject(Position position, Example example) {
        Object object = makeObject(example);
        example.getGraph().getObjects().put(object.getId(), object);
        example.getPositions().put(object.getId(), position);
    }

    private static Object makeObject(Example example) {
        var object = new Object();
        var id = new Object.Id(findAvailableVertexId(example.getGraph().getObjects().keySet(), Object.Id.MASK));
        object.setId(id);
        id.setState(object);
        return object;
    }

    private static int findAvailableVertexId(Collection<? extends Vertex.Id> ids, int mask) {
        int maxId = ids.stream()
                .mapToInt(Vertex.Id::getValue)
                .max()
                .orElse(mask);
        return 1 + maxId;
    }

    public boolean addLink(Vertex origin, Vertex destination, Example example) {
        if (doesLinkExistWithObjects(origin, destination, example)) {
            return false;
        }
        Link link = makeLink(origin, destination, example);
        example.getGraph().getLinks().put(link.getId(), link);
        return true;
    }

    private static boolean doesLinkExistWithObjects(Vertex origin, Vertex destination, Example example) {
        // no link between the objects in either side
        return doesLinkExistWithOriginAndDestination(origin, destination, example)
                || doesLinkExistWithOriginAndDestination(destination, origin, example);
    }

    private static boolean doesLinkExistWithOriginAndDestination(Vertex origin, Vertex destination, Example example) {
        return example.getGraph().getLinks().values().stream()
                .anyMatch(link -> link.getOriginId().equals(origin.getId()) && link.getDestinationId().equals(destination.getId()));
    }

    private static Link makeLink(Vertex origin, Vertex destination, Example example) {
        var link = new Link();
        var id = new Link.Id(findAvailableVertexId(example.getGraph().getLinks().keySet(), Link.Id.MASK));
        link.setId(id);
        id.setState(link);
        link.setOrigin(origin);
        link.setDestination(destination);
        link.setOriginFactor(new GraduatedValue<>());
        link.setDestinationFactor(new GraduatedValue<>());
        return link;
    }

    public boolean addAmount(Vertex vertex, Example example) {
        if (doesAmountExistWithVertex(vertex, example)) {
            return false;
        }
        Amount amount = makeAmount(vertex, example);
        example.getGraph().getAmounts().put(amount.getId(), amount);
        return true;
    }

    private boolean doesAmountExistWithVertex(Vertex vertex, Example example) {
        return example.getGraph().getAmounts().values().stream().anyMatch(amount -> amount.getModelId().equals(vertex.getId()));
    }

    private static Amount makeAmount(Vertex vertex, Example example) {
        var amount = new Amount();
        var id = new Amount.Id(findAvailableVertexId(example.getGraph().getAmounts().keySet(), Amount.Id.MASK));
        amount.setId(id);
        id.setState(amount);
        amount.setModel(vertex);
        amount.setCount(new GraduatedValue<>());
        amount.setDistinctCount(new GraduatedValue<>());
        return amount;
    }

    public boolean addDefinition(Vertex vertex, Example example) {
        if (doesDefinitionExistWithVertex(vertex, example)) {
            return false;
        }
        Definition definition = makeDefinition(vertex, example);
        example.getGraph().getDefinitions().put(definition.getId(), definition);
        return true;
    }

    private boolean doesDefinitionExistWithVertex(Vertex vertex, Example example) {
        return example.getGraph().getDefinitions().values().stream().anyMatch(definition -> definition.getBaseId().equals(vertex.getId()));
    }

    private static Definition makeDefinition(Vertex vertex, Example example) {
        var definition = new Definition();
        var id = new Definition.Id(findAvailableVertexId(example.getGraph().getDefinitions().keySet(), Definition.Id.MASK));
        definition.setId(id);
        id.setState(definition);
        definition.setBase(vertex);
        definition.setCompleteness(new GraduatedValue<>());
        return definition;
    }

    public static void deleteObject(Object.Id id, Example example) {
        List<Vertex.Id> dependentIds = listDependentVertices(id, example);
        removeVerticesFromExample(dependentIds, example);
        example.getGraph().getObjects().remove(id);
        example.getPositions().remove(id);
    }

    private static List<Vertex.Id> listDependentVertices(Vertex.Id id, Example example) {
        List<Vertex.Id> vertices = new ArrayList<>();

        for (Link link: example.getGraph().getLinks().values()) {
            if (link.getOriginId().equals(id) || link.getDestinationId().equals(id)) {
                vertices.addAll(listDependentVertices(link.getId(), example));
                vertices.add(link.getId());
            }
        }

        for (Amount amount: example.getGraph().getAmounts().values()) {
            if (amount.getModelId().equals(id)) {
                vertices.addAll(listDependentVertices(amount.getId(), example));
                vertices.add(amount.getId());
            }
        }

        for (Definition definition: example.getGraph().getDefinitions().values()) {
            if (definition.getBaseId().equals(id)) {
                vertices.addAll(listDependentVertices(definition.getId(), example));
                vertices.add(definition.getId());
            }
        }

        return vertices;
    }

    private static void removeVerticesFromExample(List<Vertex.Id> ids, Example example) {
        for (Vertex.Id id: ids) {
            switch (id) {
                case Object.Id objectId -> example.getGraph().getObjects().remove(objectId);
                case Link.Id linkId -> example.getGraph().getLinks().remove(linkId);
                case Amount.Id amountId -> example.getGraph().getAmounts().remove(amountId);
                case Definition.Id definitionId -> example.getGraph().getDefinitions().remove(definitionId);
            }
        }
    }

    public static void deleteLink(Link.Id id, Example example) {
        List<Vertex.Id> dependentIds = listDependentVertices(id, example);
        removeVerticesFromExample(dependentIds, example);
        example.getGraph().getLinks().remove(id);
    }

    public static void deleteAmount(Amount.Id id, Example example) {
        List<Vertex.Id> dependentIds = listDependentVertices(id, example);
        removeVerticesFromExample(dependentIds, example);
        example.getGraph().getAmounts().remove(id);
    }

    public static void deleteDefinition(Definition.Id id, Example example) {
        List<Vertex.Id> dependentIds = listDependentVertices(id, example);
        removeVerticesFromExample(dependentIds, example);
        example.getGraph().getDefinitions().remove(id);
    }
}
