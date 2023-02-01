package fr.alphonse.drawingpad.data.model.value;

import lombok.Data;

@Data
public class GraduatedValue<T extends Enum<T> & Graduation<T>> {

    private T graduation;

    // > 1
    private Double numberInGraduation;
}
