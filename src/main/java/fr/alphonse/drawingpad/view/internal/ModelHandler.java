package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.Example;
import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.Vertex;
import fr.alphonse.drawingpad.data.model.value.GraduatedValue;
import fr.alphonse.drawingpad.data.model.value.Value;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ModelHandler {

    public static void addObject(Position position, Example example) {
        Object object = makeObject(example);
        example.getObjects().put(object.getId(), object);
        example.getPositions().put(object.getId(), position);
    }

    private static Object makeObject(Example example) {
        var object = new Object();
        var id = findAvailableObjectId(example);
        object.setId(id);
        id.setState(object);
        return object;
    }

    private static Object.Id findAvailableObjectId(Example example) {
        int maxId = example.getObjects().keySet().stream()
                .mapToInt(Object.Id::getValue)
                .max()
                .orElse(0);
        return new Object.Id(1 + maxId);
    }

    public boolean addLink(Vertex origin, Vertex destination, Example example) {
        if (doesLinkExistWithObjects(origin, destination, example)) {
            return false;
        }
        Link link = makeLink(origin, destination, example);
        example.getLinks().put(link.getId(), link);
        return true;
    }

    private static boolean doesLinkExistWithObjects(Vertex origin, Vertex destination, Example example) {
        // no link between the objects in either side
        return doesLinkExistWithOriginAndDestination(origin, destination, example)
                || doesLinkExistWithOriginAndDestination(destination, origin, example);
    }

    private static boolean doesLinkExistWithOriginAndDestination(Vertex origin, Vertex destination, Example example) {
        return example.getLinks().values().stream()
                .anyMatch(link -> link.getOriginId().equals(origin.getId()) && link.getDestinationId().equals(destination.getId()));
    }

    private static Link makeLink(Vertex origin, Vertex destination, Example example) {
        var link = new Link();
        var id = findAvailableLinkId(example);
        link.setId(id);
        id.setState(link);
        link.setOrigin(origin);
        link.setDestination(destination);
        link.setOriginFactor(makeValue());
        link.setDestinationFactor(makeValue());
        return link;
    }

    private static Link.Id findAvailableLinkId(Example example) {
        int minId = example.getLinks().keySet().stream()
                .mapToInt(Link.Id::getValue)
                .min()
                .orElse(0);
        return new Link.Id(minId - 1);
    }

    private static Value makeValue() {
        var value = new Value();
        value.setWholeValue(new GraduatedValue<>());
        value.setLowerValue(new GraduatedValue<>());
        value.setUpperValue(new GraduatedValue<>());
        return value;
    }

    public static void deleteObject(Object.Id id, Example example) {
        List<Link.Id> linkIds = listVertexLinks(id, example);
        for (Link.Id linkId: linkIds) {
            example.getLinks().remove(linkId);
        }
        example.getObjects().remove(id);
        example.getPositions().remove(id);
    }

    private static List<Link.Id> listVertexLinks(Vertex.Id id, Example example) {
        List<Link.Id> links = new ArrayList<>();

        for (Link link: example.getLinks().values()) {
            if (link.getOriginId().equals(id) || link.getDestinationId().equals(id)) {
                links.addAll(listVertexLinks(link.getId(), example));
                links.add(link.getId());
            }
        }
        return links;
    }

    public static void deleteLink(Link.Id id, Example example) {
        List<Link.Id> linkIds = listVertexLinks(id, example);
        for (Link.Id linkId: linkIds) {
            example.getLinks().remove(linkId);
        }
        example.getLinks().remove(id);
    }
}
