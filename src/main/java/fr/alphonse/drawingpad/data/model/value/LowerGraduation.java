package fr.alphonse.drawingpad.data.model.value;

public enum LowerGraduation implements Graduation<LowerGraduation> {
    ZERO(WholeGraduation.ZERO),
    LOWEST(WholeGraduation.LOWEST),
    LOWER(WholeGraduation.LOWER),
    ONE(WholeGraduation.ONE);

    private final WholeGraduation wholeGraduation;

    LowerGraduation(WholeGraduation graduation) {
        this.wholeGraduation = graduation;
    }

    public WholeGraduation getWholeGraduation() {
        return wholeGraduation;
    }
}
