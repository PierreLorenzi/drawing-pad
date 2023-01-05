package fr.alphonse.drawingpad.data.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public final class Link extends Vertex {

    @JsonManagedReference
    private Link.Id id;

    private Vertex.Id originId;

    private Vertex.Id destinationId;

    private LinkFactor originFactor;

    private LinkFactor destinationFactor;

    @JsonIgnore
    public Vertex getOrigin() {
        return originId.state();
    }

    public void setOrigin(Vertex vertex) {
        setOriginId(vertex.getId());
    }

    @JsonIgnore
    public Vertex getDestination() {
        return destinationId.state();
    }

    public void setDestination(Vertex vertex) {
        setDestinationId(vertex.getId());
    }

    public static final class Id extends Vertex.Id {

        public Id(int value) {
            super(value);
        }

        public Id(String string) {
            super(Integer.parseInt(string));
        }

        @JsonBackReference
        private transient Link state;

        public Link state() {
            return state;
        }

        public void setState(Link state) {
            this.state = state;
        }
    }
}
