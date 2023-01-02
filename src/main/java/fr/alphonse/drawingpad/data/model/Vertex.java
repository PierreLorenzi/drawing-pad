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
public sealed abstract class Vertex permits Object, Link {

    public abstract Id getId();

    private String name;

    public static sealed abstract class Id permits Object.Id, Link.Id {
        private final int value;

        protected Id(int value) {
            this.value = value;
        }

        @JsonCreator
        public static Vertex.Id makeVertexId(String string) {
            int value = Integer.parseInt(string);
            if (value < 0) {
                return new Link.Id(value);
            }
            return new Object.Id(value);
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
