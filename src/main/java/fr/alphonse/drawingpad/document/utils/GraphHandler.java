package fr.alphonse.drawingpad.document.utils;

import fr.alphonse.drawingpad.data.model.Graph;
import fr.alphonse.drawingpad.data.model.Vertex;
import fr.alphonse.drawingpad.data.model.reference.Reference;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class GraphHandler {

    public static Vertex findReference(Reference reference, Graph graph) {
        final int id = reference.id();
        return switch (reference.type()) {
            case OBJECT -> findVertexWithId(graph.getObjects(), id);
            case OBJECT_COMPLETENESS -> findVertexWithId(graph.getObjects(), id).getCompleteness();
            case OBJECT_QUANTITY -> findVertexWithId(graph.getObjects(), id).getQuantity();
            case OBJECT_QUANTITY_COMPLETENESS -> findVertexWithId(graph.getObjects(), id).getQuantity().getCompleteness();
            case POSSESSION_LINK -> findVertexWithId(graph.getPossessionLinks(), id);
            case POSSESSION_LINK_COMPLETENESS -> findVertexWithId(graph.getPossessionLinks(), id).getCompleteness();
            case COMPARISON_LINK -> findVertexWithId(graph.getComparisonLinks(), id);
            case COMPARISON_LINK_COMPLETENESS -> findVertexWithId(graph.getComparisonLinks(), id).getCompleteness();
        };
    }

    private static <T extends Vertex> T findVertexWithId(List<T> vertices, int id) {
        return vertices.stream()
                .filter(vertex -> vertex.getId() == id)
                .findFirst().orElseThrow();
    }
}
