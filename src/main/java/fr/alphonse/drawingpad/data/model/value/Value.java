package fr.alphonse.drawingpad.data.model.value;

import lombok.Data;

@Data
public class Value {

    private GraduatedValue<WholeGraduation> wholeValue;

    private GraduatedValue<LowerGraduation> lowerValue;

    private GraduatedValue<UpperGraduation> upperValue;
}
