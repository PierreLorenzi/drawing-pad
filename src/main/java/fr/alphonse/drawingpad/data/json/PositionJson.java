package fr.alphonse.drawingpad.data.json;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class PositionJson {

    private int x;

    private int y;
}
