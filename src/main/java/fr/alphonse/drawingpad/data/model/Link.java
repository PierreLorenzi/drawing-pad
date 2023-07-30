package fr.alphonse.drawingpad.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.alphonse.drawingpad.data.model.reference.LinkDirection;
import fr.alphonse.drawingpad.data.model.value.Value;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class Link extends GraphElement {

    private Integer originId;

    // nullable
    private LinkDirection originLinkDirection;

    private Integer destinationId;

    // nullable
    private LinkDirection destinationLinkDirection;

    private Value factor;

    @JsonIgnore
    @ToString.Exclude
    private GraphElement origin;

    @JsonIgnore
    @ToString.Exclude
    private GraphElement destination;
}
