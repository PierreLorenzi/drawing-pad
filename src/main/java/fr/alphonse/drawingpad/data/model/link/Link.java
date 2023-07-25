package fr.alphonse.drawingpad.data.model.link;

import fr.alphonse.drawingpad.data.model.Vertex;

public interface Link {

    Vertex getOrigin();

    void setOrigin(Vertex vertex);

    Vertex getDestination();

    void setDestination(Vertex vertex);
}
