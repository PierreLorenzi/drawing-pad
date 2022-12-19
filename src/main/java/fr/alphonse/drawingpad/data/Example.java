package fr.alphonse.drawingpad.data;

import fr.alphonse.drawingpad.data.geometry.Position;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.Link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Example {

    private final List<Object> objects = new ArrayList<>();

    private final List<Link> links = new ArrayList<>();

    private final Map<Object, Position> positions = new HashMap<>();

    public List<Object> getObjects() {
        return objects;
    }

    public List<Link> getLinks() {
        return links;
    }

    public Map<Object, Position> getPositions() {
        return positions;
    }
}
