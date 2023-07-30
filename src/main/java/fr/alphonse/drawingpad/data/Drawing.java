package fr.alphonse.drawingpad.data;

import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.Graph;
import fr.alphonse.drawingpad.data.model.GraphElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Drawing {

    private Graph graph;

    private Map<GraphElement, Position> positions;

    private String note;
}
