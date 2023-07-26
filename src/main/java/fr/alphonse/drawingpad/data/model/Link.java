package fr.alphonse.drawingpad.data.model;

import fr.alphonse.drawingpad.data.model.Vertex;

public sealed interface Link permits PossessionLink, ComparisonLink {

    Vertex getOrigin();

    void setOrigin(Vertex vertex);

    Vertex getDestination();

    void setDestination(Vertex vertex);
}
