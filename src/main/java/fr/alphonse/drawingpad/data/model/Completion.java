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
public final class Completion extends GraphElement implements Vertex {

    private Reference baseReference;

    // <= 1
    private Value localValue;

    // <= 1
    private Value value;

    @JsonIgnore
    @ToString.Exclude
    private Vertex base;
}