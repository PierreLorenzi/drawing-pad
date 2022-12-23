package fr.alphonse.drawingpad.data;

import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Jacksonized
public class ExampleJson {

    private List<Object> objects;

    private List<Link> links;

    private Map<Object.Id, Position> positions;

}
