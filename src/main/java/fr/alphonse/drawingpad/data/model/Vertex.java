package fr.alphonse.drawingpad.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public sealed interface Vertex permits Object, Completion, Quantity, DirectFactor, ReverseFactor {

    @JsonIgnore
    default GraphElement getElement() {
        return switch (this) {
            case Object object -> object;
            case Completion completion -> completion;
            case Quantity quantity -> quantity;
            case DirectFactor directFactor -> directFactor.getLink();
            case ReverseFactor reverseFactor -> reverseFactor.getLink();
        };
    }
}
