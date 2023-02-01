package fr.alphonse.drawingpad.data.model.value;

public enum LowerGraduation implements Graduation<LowerGraduation> {
    ZERO(WholeGraduation.ZERO),
    ZERO_INFINITY(WholeGraduation.ZERO_INFINITY),
    LOWER_NUMBER(WholeGraduation.LOWER_NUMBER),
    ONE(WholeGraduation.ONE);

    private final WholeGraduation wholeGraduation;

    LowerGraduation(WholeGraduation graduation) {
        this.wholeGraduation = graduation;
    }

    public WholeGraduation getWholeGraduation() {
        return wholeGraduation;
    }
}
