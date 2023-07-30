package fr.alphonse.drawingpad.data;

import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.geometry.Vector;
import fr.alphonse.drawingpad.data.model.GraphElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Drawing {

    private List<GraphElement> elements;

    private Map<GraphElement, Position> positions;

    private Map<GraphElement, Vector> namePositions;

    private String note;
}
