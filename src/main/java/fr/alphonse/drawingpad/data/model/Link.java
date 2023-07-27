package fr.alphonse.drawingpad.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.alphonse.drawingpad.data.model.reference.Reference;
import fr.alphonse.drawingpad.data.model.value.Value;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class Link extends GraphElement {

    private Reference originReference;

    private Reference destinationReference;

    private Value factor;

    @JsonIgnore
    @ToString.Exclude
    private DirectFactor directFactor;

    @JsonIgnore
    @ToString.Exclude
    private ReverseFactor reverseFactor;

    @JsonIgnore
    @ToString.Exclude
    private Vertex origin;

    @JsonIgnore
    @ToString.Exclude
    private Vertex destination;
}
