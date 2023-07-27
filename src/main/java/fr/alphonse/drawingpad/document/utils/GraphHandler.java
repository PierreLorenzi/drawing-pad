package fr.alphonse.drawingpad.document.utils;

import fr.alphonse.drawingpad.data.model.Graph;
import fr.alphonse.drawingpad.data.model.GraphElement;
import fr.alphonse.drawingpad.data.model.reference.Reference;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class GraphHandler {

    public static GraphElement findGraphElementAtReference(Reference reference, Graph graph) {
        final int id = reference.id();
        return switch (reference.type()) {
            case OBJECT, OBJECT_COMPLETION, OBJECT_QUANTITY, OBJECT_QUANTITY_COMPLETION -> findGraphElementWithId(graph.getObjects(), id);
            case DIRECT_LINK, REVERSE_LINK, DIRECT_LINK_COMPLETION, REVERSE_LINK_COMPLETION -> findGraphElementWithId(graph.getLinks(), id);
        };
    }

    private static <T extends GraphElement> T findGraphElementWithId(List<T> vertices, int id) {
        return vertices.stream()
                .filter(element -> element.getId() == id)
                .findFirst().orElseThrow();
    }
}
