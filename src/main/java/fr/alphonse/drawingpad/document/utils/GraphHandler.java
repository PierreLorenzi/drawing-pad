package fr.alphonse.drawingpad.document.utils;

import fr.alphonse.drawingpad.data.model.Graph;
import fr.alphonse.drawingpad.data.model.GraphElement;
import fr.alphonse.drawingpad.data.model.Vertex;
import fr.alphonse.drawingpad.data.model.reference.Reference;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class GraphHandler {

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
}
