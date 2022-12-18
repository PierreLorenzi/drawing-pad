package fr.alphonse.drawingpad.data.geometry;

public record Position(int x, int y) {

    public static Position middle(Position p1, Position p2) {
        return new Position((p1.x + p2.x)/2, (p1.y + p2.y)/2);
    }

    public Position translate(Vector vector) {
        return new Position(x + vector.x(), y + vector.y());
    }

    public static float distance(Position p1, Position p2) {
        return Vector.between(p1, p2).length();
    }
}
