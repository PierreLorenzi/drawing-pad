package fr.alphonse.drawingpad.data;

import fr.alphonse.drawingpad.data.json.LinkJson;
import fr.alphonse.drawingpad.data.json.ObjectJson;
import fr.alphonse.drawingpad.data.json.PositionJson;
import fr.alphonse.drawingpad.model.Link;
import fr.alphonse.drawingpad.model.Object;
import fr.alphonse.drawingpad.model.Vertex;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Map;

@Mapper
public interface JsonDataMapper {

    List<Object> mapObjects(List<ObjectJson> jsons);

    List<Link> mapLinks(List<LinkJson> jsons, @Context List<Vertex> vertices);
    
    @AfterMapping
    default Link registerLink(LinkJson json, @MappingTarget Link.LinkBuilder<?,?> linkBuilder, @Context List<Vertex> vertices) {
        var link = linkBuilder.build();
        vertices.add(link);
        return link;
    }

    default Vertex mapVertexReference(String id, @Context List<Vertex> vertices) {
        return vertices.stream().filter(vertex -> vertex.getId().equals(id)).findFirst().get();
    }

    Map<Object, Position> mapPositions(Map<String, PositionJson> jsons, @Context List<Object> objects);

    default Object mapObjectReference(String id, @Context List<Object> objects) {
        return objects.stream().filter(object -> object.getId().equals(id)).findFirst().get();
    }
}
