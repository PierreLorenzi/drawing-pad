package fr.alphonse.drawingpad.data.model.value;

import fr.alphonse.drawingpad.data.model.Vertex;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraduatedValue<T extends Enum<T> & Graduation<T>> {

    private T graduation;

    private Double numberInGraduation;
}
