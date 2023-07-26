package fr.alphonse.drawingpad.data.model;

public sealed interface Value permits WholeValue, LowerValue {

    Vertex getOwner();
}
