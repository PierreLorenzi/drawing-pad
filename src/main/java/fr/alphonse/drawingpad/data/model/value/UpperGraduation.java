package fr.alphonse.drawingpad.data.model.value;

public enum UpperGraduation implements Graduation<UpperGraduation> {
    ZERO(WholeGraduation.ZERO),
    ONE(WholeGraduation.ONE),
    UPPER_NUMBER(WholeGraduation.UPPER_NUMBER),
    INFINITY(WholeGraduation.INFINITY);

    private final WholeGraduation wholeGraduation;

    UpperGraduation(WholeGraduation graduation) {
        this.wholeGraduation = graduation;
    }

    public WholeGraduation getWholeGraduation() {
        return wholeGraduation;
    }
}
