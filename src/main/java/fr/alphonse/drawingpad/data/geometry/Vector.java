package fr.alphonse.drawingpad.data.geometry;

public record Vector(int x, int y) {

    public static Vector between(Position p1, Position p2) {
        return new Vector(p2.x() - p1.x(), p2.y() - p1.y());
    }

    public static Vector between(Vector p1, Vector p2) {
        return new Vector(p2.x() - p1.x(), p2.y() - p1.y());
    }

    public static int scalarProduct(Vector v1, Vector v2) {
        return v1.x() * v2.x() + v1.y() * v2.y();
    }

    public static int discriminant(Vector v1, Vector v2) {
        return v1.x() * v2.y() - v1.y() * v2.x;
    }

    public float length() {
        return (float)Math.sqrt(x*x + y*y);
    }

    public int infiniteNormLength() {
        return Math.max(Math.abs(x), Math.abs(y));
    }

    public Vector rotate(double angle) {
        return new Vector((int)(x * Math.cos(angle) - y * Math.sin(angle)), (int)(x * Math.sin(angle) + y * Math.cos(angle)));
    }

    public Vector multiply(float factor) {
        return new Vector((int)(x * factor), (int)(y * factor));
    }

    public Vector translate(Vector vector) {
        return new Vector(x + vector.x(), y + vector.y());
    }
}
