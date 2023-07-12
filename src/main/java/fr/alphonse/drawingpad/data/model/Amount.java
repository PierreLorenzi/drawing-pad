package fr.alphonse.drawingpad.data.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import fr.alphonse.drawingpad.data.model.value.GraduatedValue;
import fr.alphonse.drawingpad.data.model.value.WholeGraduation;
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
public final class Amount extends Vertex {

    @JsonManagedReference
    private Amount.Id id;

    private GraduatedValue<WholeGraduation> count;

    private GraduatedValue<WholeGraduation> distinctCount;

    private Vertex.Id modelId;

    @JsonIgnore
    public Vertex getModel() {
        return modelId.state();
    }

    public void setModel(Vertex vertex) {
        setModelId(vertex.getId());
    }

    public static final class Id extends Vertex.Id {

        public static final int MASK = 0x3_0000;

        public Id(int value) {
            super(value);
        }

        public Id(String string) {
            super(Integer.parseInt(string));
        }

        @JsonBackReference
        private transient Amount state;

        public Amount state() {
            return state;
        }

        public void setState(Amount state) {
            this.state = state;
        }
    }
}