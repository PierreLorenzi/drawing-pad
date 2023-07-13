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

        public static final int LINK_ORIGIN_FACTOR_MASK = 0x5_0000;

        public static final int LINK_DESTINATION_FACTOR_MASK = 0x6_0000;

        public static final int DEFINITION_COMPLETENESS_MASK = 0x7_0000;

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
