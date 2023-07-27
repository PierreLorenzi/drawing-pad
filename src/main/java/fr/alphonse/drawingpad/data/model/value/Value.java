package fr.alphonse.drawingpad.data.model.value;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Value {

    private Graduation graduation;

    private Double numberInGraduation;
}
