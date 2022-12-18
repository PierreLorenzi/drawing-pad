package fr.alphonse.drawingpad.data.json;

import lombok.Data;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@SuperBuilder
@NonFinal
@Jacksonized
public class VertexJson {

    private String id;

    private String name;
}
