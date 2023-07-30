package fr.alphonse.drawingpad.data.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonTypeInfo(
        use = JsonTypeInfo.Id.MINIMAL_CLASS,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Object.class),
        @JsonSubTypes.Type(value = Completion.class),
        @JsonSubTypes.Type(value = Quantity.class),
        @JsonSubTypes.Type(value = Link.class)
})
public abstract sealed class GraphElement permits Object, Completion, Quantity, Link {

    public Integer id;

    private String name;

    @Override
    public boolean equals(java.lang.Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
