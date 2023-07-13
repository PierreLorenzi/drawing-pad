package fr.alphonse.drawingpad.data.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
public final class WholeValue extends Vertex implements GraduatedValue<WholeGraduation> {

    @JsonIgnore
    private WholeValue.Id id;

    private WholeGraduation graduation;

    // > 1
    private Double numberInGraduation;

    public static final class Id extends Vertex.Id {

        public static final int AMOUNT_COUNT_MASK = 0x8_0000;

        public static final int AMOUNT_DISTINCT_COUNT_MASK = 0x9_0000;

        public Id(int value) {
            super(value);
        }

        public Id(String string) {
            super(Integer.parseInt(string));
        }

        public Id(int value, WholeValue state) {
            super(value);
            this.state = state;
        }

        @JsonBackReference
        private transient WholeValue state;

        public WholeValue state() {
            return state;
        }
    }
}
