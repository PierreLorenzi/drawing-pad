package fr.alphonse.drawingpad.data.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
public final class Definition extends Vertex {

    @JsonManagedReference
    private Definition.Id id;

    private LowerValue completeness;

    private Vertex.Id baseId;

    @JsonIgnore
    public Vertex getBase() {
        return baseId.state();
    }

    public void setBase(Vertex vertex) {
        setBaseId(vertex.getId());
    }

    public static final class Id extends Vertex.Id {

        public static final int MASK = 0x4_0000;

        public static final int DEFINITION_COMPLETENESS_MASK = 0x41_0000;

        public Id(int value) {
            super(value);
        }

        public Id(String string) {
            super(Integer.parseInt(string));
        }

        @JsonBackReference
        private transient Definition state;

        public Definition state() {
            return state;
        }

        public void setState(Definition state) {
            this.state = state;
        }
    }
}
