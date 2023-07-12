package fr.alphonse.drawingpad.data;

import fr.alphonse.drawingpad.data.model.Amount;
import fr.alphonse.drawingpad.data.model.Definition;
import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder
@Jacksonized
public class GraphJson {

    private List<Object> objects;

    private List<Link> links;

    private List<Amount> amounts;

    private List<Definition> definitions;
}
