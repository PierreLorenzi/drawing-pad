package fr.alphonse.drawingpad.data.model;

import fr.alphonse.drawingpad.data.model.value.Value;
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
public final class Object extends GraphElement {

    private Value quantity;

    // <= 1
    private Value quantityCompletion;

    // <= 1
    // this value can be set but can't be reached by links
    // takes 3/4 of the global completion
    private Value localCompletion;
}
