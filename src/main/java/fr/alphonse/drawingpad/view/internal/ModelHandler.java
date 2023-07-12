package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.Example;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.Amount;
import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.Vertex;
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

        return vertices;
    }

    private static void removeVerticesFromExample(List<Vertex.Id> ids, Example example) {
        for (Vertex.Id id: ids) {
            switch (id) {
                case Object.Id objectId -> example.getGraph().getObjects().remove(objectId);
                case Link.Id linkId -> example.getGraph().getLinks().remove(linkId);
                case Amount.Id amountId -> example.getGraph().getAmounts().remove(amountId);
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
}
