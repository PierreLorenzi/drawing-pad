package fr.alphonse.drawingpad.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Graph {

    private List<Object> objects;

    private List<PossessionLink> possessionLinks;

    private List<ComparisonLink> comparisonLinks;
}
