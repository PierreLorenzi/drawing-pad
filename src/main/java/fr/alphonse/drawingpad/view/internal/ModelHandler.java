package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.Example;
import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.Vertex;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ModelHandler {

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
