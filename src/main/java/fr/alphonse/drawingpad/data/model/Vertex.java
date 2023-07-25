package fr.alphonse.drawingpad.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public sealed abstract class Vertex permits Object, PossessionLink, ComparisonLink, WholeValue, LowerValue {

    public Integer id;

    private String name;

    // inside there is no completeness and no quantity
    private LowerValue completeness;

    @Override
    public boolean equals(java.lang.Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
