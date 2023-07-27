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
    public GraphElement originElement;

    @JsonIgnore
    @ToString.Exclude
    public GraphElement destinationElement;
}
