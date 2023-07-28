package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.model.value.Graduation;
import fr.alphonse.drawingpad.data.model.value.Value;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

public class GraduatedValueComponent extends JPanel {

    private final Runnable changeCallback;

    private final JComboBox<String> comboBox;

    private final JTextField field;

    private Value value;

    private boolean isSettingValue = false;

    private static final Map<Graduation, String> GRADUATION_NAMES = Map.of(
            Graduation.ZERO, "0",
            Graduation.LOWEST, "<<",
            Graduation.LOWER, "<",
            Graduation.ONE, "1",
            Graduation.GREATER, ">",
            Graduation.GREATEST, ">>",
            Graduation.INFINITY, "∞"
    );

    public GraduatedValueComponent(Runnable changeCallback) {
        super();

        this.changeCallback = changeCallback;

        setBackground(null);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        comboBox = new JComboBox<>(findGraduationLabels());
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

    private String[] findGraduationLabels() {
        Stream<String> gradationValues = Arrays.stream(Graduation.values())
                .map(GRADUATION_NAMES::get);
        // add the null value at the beginning
        return Stream.concat(Stream.of(""), gradationValues)
                .toArray(String[]::new);
    }

    private void reactToGraduationChange() {
        if (isSettingValue) {
            return;
        }
        int index = comboBox.getSelectedIndex();
        value.setGraduation(index == 0 ? null : Graduation.values()[index-1]);
        value.setNumberInGraduation(null);
        updateField();
        changeCallback.run();
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
        changeCallback.run();
    }

    private static Double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    public void setValue(Value value) {
        if (this.value == value) {
            return;
        }
        this.value = value;

        this.isSettingValue = true;

        // select the graduation in the combo box
        Graduation graduation = value.getGraduation();
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

    private boolean doesGraduationNeedsValue(Graduation graduation) {
        return switch (graduation) {
            case ZERO, LOWEST, ONE, GREATEST, INFINITY -> false;
            case LOWER, GREATER -> true;
        };
    }
}
