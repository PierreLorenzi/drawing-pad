package fr.alphonse.drawingpad.data.model.value;

public enum WholeGraduation implements Graduation<WholeGraduation> {
    ZERO,
    LOWEST,
    LOWER,
    ONE,
    GREATER,
    GREATEST,
    INFINITY;

    @Override
    public WholeGraduation getWholeGraduation() {
        return this;
    }
}
