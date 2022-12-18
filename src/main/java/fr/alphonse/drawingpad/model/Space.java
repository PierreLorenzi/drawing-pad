package fr.alphonse.drawingpad.model;


public enum Space {
    NUMBER(Number.class);

    private final Class<?> valueClass;

    Space(Class<?> valueClass) {
        this.valueClass = valueClass;
    }

    public Class<?> getValueClass() {
        return valueClass;
    }
}
