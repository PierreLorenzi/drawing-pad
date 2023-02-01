package fr.alphonse.drawingpad.data.model.value;

public enum WholeGraduation implements Graduation<WholeGraduation> {
    ZERO,
    ZERO_INFINITY,
    LOWER_NUMBER,
    ONE,
    UPPER_NUMBER,
    INFINITY;

    @Override
    public WholeGraduation getWholeGraduation() {
        return this;
    }
}
