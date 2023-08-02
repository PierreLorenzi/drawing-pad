package fr.alphonse.drawingpad.data.geometry;

public record Rectangle(int x, int y, int width, int height) {

    public static Rectangle between(Position p1, Position p2) {
        return new Rectangle(Math.min(p1.x(), p2.x()), Math.min(p1.y(), p2.y()), Math.abs(p2.x()- p1.x()), Math.abs(p2.y() - p1.y()));
    }

    public boolean containsPosition(Position p) {
        return p.x() >= x && p.x() < x + width && p.y() >= y && p.y() < y + height;
    }

    public Position findCenter() {
        return new Position(x + width/2, y + height/2);
    }

    public Rectangle increaseByMargin(int margin) {
        return new Rectangle(x - margin, y - margin, width + 2 * margin, height + 2 * margin);
    }
}
