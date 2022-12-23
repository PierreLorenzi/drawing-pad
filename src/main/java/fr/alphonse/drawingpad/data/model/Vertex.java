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
        private final String string;

        protected Id(String string) {
            this.string = string;
        }

        @JsonCreator
        public static Vertex.Id makeVertexId(String string) {
            if (string.charAt(0) == '-') {
                return new Link.Id(string);
            }
            return new Object.Id(string);
        }

        @JsonValue
        public String getString() {
            return string;
        }

        public abstract Vertex state();

        @Override
        public boolean equals(java.lang.Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vertex.Id id = (Vertex.Id) o;
            return getString().equals(id.getString());
        }

        @Override
        public int hashCode() {
            return getString().hashCode();
        }
    }
}
