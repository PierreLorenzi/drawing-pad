package fr.alphonse.drawingpad.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Graph {

    private Map<Object.Id, Object> objects;

    private Map<Link.Id, Link> links;

    private Map<Amount.Id, Amount> amounts;

    private Map<Definition.Id, Definition> definitions;
}
