package fr.alphonse.drawingpad.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.alphonse.drawingpad.data.model.reference.Reference;
import fr.alphonse.drawingpad.data.model.value.GraduatedValue;
import fr.alphonse.drawingpad.data.model.value.WholeGraduation;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class ComparisonLink extends Vertex implements Link {

    private Reference originReference;

    private Reference destinationReference;

    private GraduatedValue<WholeGraduation> factor;

    @JsonIgnore
    @ToString.Exclude
    public Vertex origin;

    @JsonIgnore
    @ToString.Exclude
    public Vertex destination;
}
