package fr.alphonse.drawingpad.data.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public sealed abstract class Vertex permits Object, Link, Amount, Definition {

    public abstract Id getId();

    private String name;

    @Override
    public boolean equals(java.lang.Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public static sealed abstract class Id permits Object.Id, Link.Id, Amount.Id, Definition.Id {
        private final int value;

        public static final int TYPE_MASK = 0xF_0000;

        protected Id(int value) {
            this.value = value;
        }

        @JsonCreator
        public static Vertex.Id makeVertexId(String string) {
            int value = Integer.parseInt(string);
            int typeMaskValue = value & TYPE_MASK;
            return switch (typeMaskValue) {
                case Object.Id.MASK -> new Object.Id(value);
                case Link.Id.MASK -> new Link.Id(value);
                case Amount.Id.MASK -> new Amount.Id(value);
                case Definition.Id.MASK -> new Definition.Id(value);
                default -> throw new Error("Unknown type mask: " + typeMaskValue);
            };
        }

        @JsonValue
        public String getString() {
            return "" + value;
        }

        public int getValue() {
            return value;
        }

        public abstract Vertex state();

        @Override
        public boolean equals(java.lang.Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vertex.Id id = (Vertex.Id) o;
            return value == id.value;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }
}
