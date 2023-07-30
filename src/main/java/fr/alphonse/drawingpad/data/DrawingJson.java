package fr.alphonse.drawingpad.data;

import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.Graph;
import fr.alphonse.drawingpad.data.model.reference.Reference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class DrawingJson {

    private Graph graph;

    private Map<Reference, Position> positions;

    private String note;
}
