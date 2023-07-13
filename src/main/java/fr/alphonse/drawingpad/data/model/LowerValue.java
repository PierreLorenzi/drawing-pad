package fr.alphonse.drawingpad.data.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.alphonse.drawingpad.data.model.value.GraduatedValue;
import fr.alphonse.drawingpad.data.model.value.LowerGraduation;
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
public final class LowerValue extends Vertex implements GraduatedValue<LowerGraduation> {

    @JsonIgnore
    private LowerValue.Id id;

    private LowerGraduation graduation;

    // > 1
    private Double numberInGraduation;

    public static final class Id extends Vertex.Id {

        public Id(int value) {
            super(value);
        }

        public Id(String string) {
            super(Integer.parseInt(string));
        }

        public Id(int value, LowerValue state) {
            super(value);
            this.state = state;
        }

        @JsonBackReference
        private transient LowerValue state;

        public LowerValue state() {
            return state;
        }
    }
}
