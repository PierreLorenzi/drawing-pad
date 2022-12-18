package fr.alphonse.drawingpad.data.json;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Jacksonized
public class ExampleJson {

    private List<ObjectJson> objects;

    private List<LinkJson> links;

    private Map<String, PositionJson> positions;

}
