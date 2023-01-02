package fr.alphonse.drawingpad.data.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public final class Object extends Vertex {

    @JsonManagedReference
    private Object.Id id;

    private Space space;

    private java.lang.Object value;

    public static final class Id extends Vertex.Id {

        public Id(int value) {
            super(value);
        }

        public Id(String string) {
            super(Integer.parseInt(string));
        }

        @JsonBackReference
        private transient Object state;

        public Object state() {
            return state;
        }
    }
}
