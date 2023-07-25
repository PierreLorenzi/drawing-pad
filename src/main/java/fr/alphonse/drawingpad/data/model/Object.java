package fr.alphonse.drawingpad.data.model;

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
public final class Object extends Vertex {

    // this value can be set but can't be reached by links
    // takes 3/4 of the global completeness
    private LowerValue localCompleteness;

    private WholeValue quantity;
}
