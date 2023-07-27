package fr.alphonse.drawingpad.data;

import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.Object;
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

    private Map<Object, Position> positions;

    private Map<Completion, Position> completionPositions;

    private Map<Quantity, Position> quantityPositions;

    private Map<Link, Position> linkCenters;
}
