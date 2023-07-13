package fr.alphonse.drawingpad.data.model.value;

public interface GraduatedValue<T extends Enum<T> & Graduation<T>> {

    T getGraduation();

    void setGraduation(T value);

    Double getNumberInGraduation();

    void setNumberInGraduation(Double value);
}
