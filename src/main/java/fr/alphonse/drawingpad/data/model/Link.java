package fr.alphonse.drawingpad.data.model;

import fr.alphonse.drawingpad.data.model.reference.Reference;

public sealed interface Link permits PossessionLink, ComparisonLink {

    Reference getOriginReference();

    void setOriginReference(Reference reference);

    Reference getDestinationReference();

    void setDestinationReference(Reference reference);

    Vertex getOrigin();

    void setOrigin(Vertex vertex);

    Vertex getDestination();

    void setDestination(Vertex vertex);
}
