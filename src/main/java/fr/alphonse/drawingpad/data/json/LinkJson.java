package fr.alphonse.drawingpad.data.json;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@SuperBuilder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
public class LinkJson extends VertexJson {

    private String origin;

    private String destination;
}
