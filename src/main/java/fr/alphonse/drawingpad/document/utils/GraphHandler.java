package fr.alphonse.drawingpad.document.utils;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.reference.Reference;
import fr.alphonse.drawingpad.data.model.reference.ReferenceType;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
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

    public static Reference makeVertexReference(Vertex vertex, Graph graph) {
        Vertex owner = findUltimateOwner(vertex);
        ReferenceType referenceType = findReferenceTypeBetweenOwnerAndVertex(owner, vertex);
        return new Reference(referenceType, owner.getId());
    }

    private ReferenceType findReferenceTypeBetweenOwnerAndVertex(Vertex owner, Vertex vertex) {
        if (owner instanceof Object object) {
            if (vertex == object) {
                return ReferenceType.OBJECT;
            }
            else if (vertex == object.getCompleteness()) {
                return ReferenceType.OBJECT_COMPLETENESS;
            }
            else if (vertex == object.getQuantity()) {
                return ReferenceType.OBJECT_QUANTITY;
            }
            else if (vertex == object.getQuantity().getCompleteness()) {
                return ReferenceType.OBJECT_QUANTITY_COMPLETENESS;
            }
        }
        else if (owner instanceof PossessionLink possessionLink) {
            if (vertex == possessionLink) {
                return ReferenceType.POSSESSION_LINK;
            }
            else if (vertex == possessionLink.getCompleteness()) {
                return ReferenceType.POSSESSION_LINK_COMPLETENESS;
            }
        }
        else if (owner instanceof ComparisonLink comparisonLink) {
            if (vertex == comparisonLink) {
                return ReferenceType.COMPARISON_LINK;
            }
            else if (vertex == comparisonLink.getCompleteness()) {
                return ReferenceType.COMPARISON_LINK_COMPLETENESS;
            }
        }
        throw new Error("Unknown Reference Type!");
    }

    public Vertex findUltimateOwner(Vertex vertex) {
        if (vertex instanceof Value value && value.getOwner() != null) {
            return findUltimateOwner(value.getOwner());
        }
        return vertex;
    }
}
