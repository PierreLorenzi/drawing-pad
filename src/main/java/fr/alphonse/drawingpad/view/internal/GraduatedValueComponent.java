package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.model.value.GraduatedValue;
import fr.alphonse.drawingpad.data.model.value.Graduation;
import fr.alphonse.drawingpad.data.model.value.WholeGraduation;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

public class GraduatedValueComponent<T extends Enum<T> & Graduation<T>> extends JPanel {

    private final T[] graduations;

    private final JComboBox<String> comboBox;

    private final JTextField field;

    private GraduatedValue<T> value;

    private boolean isSettingValue = false;

    private static final Map<WholeGraduation, String> GRADUATION_NAMES = Map.of(
            WholeGraduation.ZERO, "0",
            WholeGraduation.ZERO_INFINITY, "0+",
            WholeGraduation.LOWER_NUMBER, "<",
            WholeGraduation.ONE, "1",
            WholeGraduation.UPPER_NUMBER, ">",
            WholeGraduation.INFINITY, "∞"
    );

    public GraduatedValueComponent(Class<T> graduationClass) {
        super();

        setBackground(null);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        graduations = graduationClass.getEnumConstants();
        comboBox = new JComboBox<>(findLabels(graduations));
        comboBox.setMinimumSize(new Dimension(70, 40));
        comboBox.setMaximumSize(new Dimension(70, 40));
        comboBox.addActionListener(arg -> reactToGraduationChange());
        this.add(comboBox);

        field = new JTextField();
        field.setMinimumSize(new Dimension(100, 40));
        field.setMaximumSize(new Dimension(100, 40));
        field.addActionListener(arg -> reactToNumberChange());
        this.add(field);

        setValue(null);
    }

    private String[] findLabels(T[] graduations) {
        Stream<String> gradationValues = Arrays.stream(graduations)
                .map(Graduation::getWholeGraduation)
                .map(GRADUATION_NAMES::get);
        // on ajoute la valeur nulle au début
        return Stream.concat(Stream.of(""), gradationValues)
                .toArray(String[]::new);
    }

    private void reactToGraduationChange() {
        if (isSettingValue) {
            return;
        }
        int index = comboBox.getSelectedIndex();
        value.setGraduation(index == 0 ? null : graduations[index-1]);
        value.setNumberInGraduation(null);
        updateField();
    }

    private void reactToNumberChange() {
        if (isSettingValue) {
            return;
        }
        String stringValue = field.getText();
        if (stringValue.isEmpty()) {
            this.value.setNumberInGraduation(null);
            return;
        }
        Double doubleValue = parseDouble(stringValue);
        if (doubleValue == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        this.value.setNumberInGraduation(doubleValue);
    }

    private static Double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    public void setValue(GraduatedValue<T> value) {
        if (this.value == value) {
            return;
        }
        this.value = value;

        this.isSettingValue = true;

        // select the graduation in the combo box
        T graduation = value.getGraduation();
        comboBox.setSelectedIndex(graduation == null ? 0 : graduation.ordinal() + 1);

        updateField();

        this.isSettingValue = false;
    }

    private void updateField() {
        if (value.getGraduation() == null || !doesGraduationNeedsValue(value.getGraduation())) {
            field.setText("");
            field.setEnabled(false);
            field.setBackground(Color.DARK_GRAY);
            return;
        }
        field.setEnabled(true);
        field.setBackground(Color.WHITE);
        var number = value.getNumberInGraduation();
        if (number == null) {
            field.setText("");
            return;
        }
        field.setText("" + number);
    }

    private boolean doesGraduationNeedsValue(T graduation) {
        return switch (graduation.getWholeGraduation()) {
            case ZERO, ZERO_INFINITY, ONE, INFINITY -> false;
            case LOWER_NUMBER, UPPER_NUMBER -> true;
        };
    }
}
