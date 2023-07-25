package fr.alphonse.drawingpad.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.alphonse.drawingpad.data.model.link.Link;
import fr.alphonse.drawingpad.data.model.reference.Reference;
import fr.alphonse.drawingpad.data.model.value.GraduatedValue;
import fr.alphonse.drawingpad.data.model.value.LowerGraduation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class PossessionLink extends Vertex implements Link {

    private Reference originReference;

    private Reference destinationReference;

    private GraduatedValue<LowerGraduation> factor;

    @JsonIgnore
    public Vertex origin;

    @JsonIgnore
    public Vertex destination;
}
