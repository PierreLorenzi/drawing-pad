package fr.alphonse.drawingpad.data.model;

import fr.alphonse.drawingpad.data.model.value.Value;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract sealed class GraphElement permits Object, Link {

    public Integer id;

    private String name;

    // <= 1
    private Value completion;

    @Override
    public boolean equals(java.lang.Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
