package fr.alphonse.drawingpad.view;

import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.value.LowerGraduation;
import fr.alphonse.drawingpad.data.model.value.WholeGraduation;
import fr.alphonse.drawingpad.document.utils.ChangeDetector;
import fr.alphonse.drawingpad.view.internal.GraduatedValueComponent;

import javax.swing.*;
import java.awt.*;

public class InfoComponent extends JPanel {

    private final java.util.List<Vertex> selection;

    private final ChangeDetector modelChangeDetector;

    private JTextArea multipleSelectionLabel;

    private Object selectedObject = null;

    private JTextField objectNameField;

    private Link selectedLink = null;

    private JTextField linkNameField;

    private GraduatedValueComponent<LowerGraduation> originFactorComponent;

    private GraduatedValueComponent<LowerGraduation> destinationFactorComponent;

    private Amount selectedAmount = null;

    private Definition selectedDefinition = null;

    private JTextField amountNameField;

    private GraduatedValueComponent<WholeGraduation> amountCountComponent;

    private GraduatedValueComponent<WholeGraduation> amountDistinctCountComponent;

    private JTextField definitionNameField;

    private GraduatedValueComponent<LowerGraduation> definitionCompletenessComponent;

    private static final String EMPTY_SELECTION_CARD = "empty";

    private static final String MULTIPLE_SELECTION_CARD = "multiple";

    private static final String OBJECT_SELECTION_CARD = "object";

    private static final String LINK_SELECTION_CARD = "link";

    private static final String AMOUNT_SELECTION_CARD = "amount";

    private static final String DEFINITION_SELECTION_CARD = "definition";

    public InfoComponent(java.util.List<Vertex> selection, ChangeDetector changeDetector, ChangeDetector modelChangeDetector) {
        super();
        this.selection = selection;
        this.modelChangeDetector = modelChangeDetector;

        changeDetector.addListener(this, InfoComponent::reactToSelectionChange);

        setLayout(new CardLayout());
        add(makeEmptySelectionView(), EMPTY_SELECTION_CARD);
        add(makeMultipleSelectionView(), MULTIPLE_SELECTION_CARD);
        add(makeObjectSelectionView(), OBJECT_SELECTION_CARD);
        add(makeLinkSelectionView(), LINK_SELECTION_CARD);
        add(makeAmountSelectionView(), AMOUNT_SELECTION_CARD);
        add(makeDefinitionSelectionView(), DEFINITION_SELECTION_CARD);
        switchToCard(EMPTY_SELECTION_CARD);

        setBackground(Color.DARK_GRAY);
        setBorder(BorderFactory.createLoweredBevelBorder());
        setPreferredSize(new Dimension(300, 300));
    }

    private static JPanel makeEmptySelectionView() {
        var view = new JPanel();
        view.setBackground(null);
        return view;
    }

    private JTextArea makeMultipleSelectionView() {
        JTextArea label = new JTextArea();
        label.setForeground(Color.WHITE);
        label.setAlignmentX(JTextArea.CENTER_ALIGNMENT);
        label.setAlignmentY(JTextArea.CENTER_ALIGNMENT);
        label.setBackground(null);
        label.setForeground(Color.WHITE);
        label.setLineWrap(true);
        label.setEditable(false);
        this.multipleSelectionLabel = label;
        return label;
    }

    private JPanel makeObjectSelectionView() {
        JPanel panel = new JPanel();
        panel.setBackground(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalStrut(50));
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(nameLabel);
        JTextField textField = new JTextField();
        textField.setMaximumSize(new Dimension(100000, 40));
        textField.addActionListener(event -> {if (this.selectedObject != null) {
            this.selectedObject.setName(textField.getText());
            this.modelChangeDetector.notifyChange();
        }});
        panel.add(textField);
        panel.add(Box.createVerticalGlue());
        this.objectNameField = textField;
        return panel;
    }

    private JPanel makeLinkSelectionView() {
        JPanel panel = new JPanel();
        panel.setBackground(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalStrut(50));

        // name
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(nameLabel);
        JTextField textField = new JTextField();
        textField.setMaximumSize(new Dimension(100000, 40));
        textField.addActionListener(event -> {if (this.selectedLink != null) {
            this.selectedLink.setName(textField.getText());
            this.modelChangeDetector.notifyChange();
        }});
        panel.add(textField);
        this.linkNameField = textField;

        // origin factor
        panel.add(Box.createVerticalStrut(30));
        JLabel originFactorLabel = new JLabel("Origin Factor:");
        originFactorLabel.setForeground(Color.WHITE);
        originFactorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(originFactorLabel);
        originFactorComponent = new GraduatedValueComponent<>(LowerGraduation.class);
        panel.add(originFactorComponent);

        // destination factor
        panel.add(Box.createVerticalStrut(30));
        JLabel destinationFactorLabel = new JLabel("Destination Factor:");
        destinationFactorLabel.setForeground(Color.WHITE);
        destinationFactorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(destinationFactorLabel);
        destinationFactorComponent = new GraduatedValueComponent<>(LowerGraduation.class);
        panel.add(destinationFactorComponent);

        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel makeAmountSelectionView() {
        JPanel panel = new JPanel();
        panel.setBackground(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalStrut(50));

        // name
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(nameLabel);
        JTextField textField = new JTextField();
        textField.setMaximumSize(new Dimension(100000, 40));
        textField.addActionListener(event -> {if (this.selectedAmount != null) {
            this.selectedAmount.setName(textField.getText());
            this.modelChangeDetector.notifyChange();
        }});
        panel.add(textField);
        this.amountNameField = textField;

        // count
        panel.add(Box.createVerticalStrut(30));
        JLabel countLabel = new JLabel("Count:");
        countLabel.setForeground(Color.WHITE);
        countLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(countLabel);
        amountCountComponent = new GraduatedValueComponent<>(WholeGraduation.class);
        panel.add(amountCountComponent);

        // distinct count
        panel.add(Box.createVerticalStrut(30));
        JLabel distinctCountLabel = new JLabel("Distinct Count:");
        distinctCountLabel.setForeground(Color.WHITE);
        distinctCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(distinctCountLabel);
        amountDistinctCountComponent = new GraduatedValueComponent<>(WholeGraduation.class);
        panel.add(amountDistinctCountComponent);

        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel makeDefinitionSelectionView() {
        JPanel panel = new JPanel();
        panel.setBackground(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalStrut(50));

        // name
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(nameLabel);
        JTextField textField = new JTextField();
        textField.setMaximumSize(new Dimension(100000, 40));
        textField.addActionListener(event -> {if (this.selectedDefinition != null) {
            this.selectedDefinition.setName(textField.getText());
            this.modelChangeDetector.notifyChange();
        }});
        panel.add(textField);
        this.definitionNameField = textField;

        // count
        panel.add(Box.createVerticalStrut(30));
        JLabel countLabel = new JLabel("Completeness:");
        countLabel.setForeground(Color.WHITE);
        countLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(countLabel);
        definitionCompletenessComponent = new GraduatedValueComponent<>(LowerGraduation.class);
        panel.add(definitionCompletenessComponent);

        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private void reactToSelectionChange() {

        selectedObject = null;

        var count = selection.size();
        switch (count) {
            case 0 -> switchToCard(EMPTY_SELECTION_CARD);
            case 1 -> {
                switch (selection.get(0)) {
                    case Object object -> {
                        switchToCard(OBJECT_SELECTION_CARD);
                        updateObjectSelectionView(object);
                    }
                    case Link link -> {
                        switchToCard(LINK_SELECTION_CARD);
                        updateLinkSelectionView(link);
                    }
                    case Amount amount -> {
                        switchToCard(AMOUNT_SELECTION_CARD);
                        updateAmountSelectionView(amount);
                    }
                    case Definition definition -> {
                        switchToCard(DEFINITION_SELECTION_CARD);
                        updateDefinitionSelectionView(definition);
                    }
                    case WholeValue ignored -> throw new Error("Whole values not handled as real vertices");
                    case LowerValue ignored -> throw new Error("Lower values not handled as real vertices");
                }
            }
            default -> {
                updateMultipleSelectionView();
                switchToCard(MULTIPLE_SELECTION_CARD);
            }
        }
    }

    private void switchToCard(String name) {
        var layout = (CardLayout)getLayout();
        layout.show(this, name);
    }

    private void updateMultipleSelectionView() {
        long objectCount = selection.stream().filter(id -> id instanceof Object).count();
        long linkCount = selection.stream().filter(id -> id instanceof Link).count();
        long amountCount = selection.stream().filter(id -> id instanceof Amount).count();
        long definitionCount = selection.stream().filter(id -> id instanceof Definition).count();
        long total = objectCount + linkCount + amountCount + definitionCount;
        multipleSelectionLabel.setText(total + " elements in selection: " + objectCount + " objects, " + amountCount + " amounts, " + definitionCount + " definitions, " + linkCount + " links");
    }

    private void updateObjectSelectionView(Object object) {
        selectedObject = object;
        objectNameField.setText(object.getName());
    }

    private void updateLinkSelectionView(Link link) {
        selectedLink = link;
        linkNameField.setText(link.getName());
        originFactorComponent.setValue(link.getOriginFactor());
        destinationFactorComponent.setValue(link.getDestinationFactor());
    }

    private void updateAmountSelectionView(Amount amount) {
        selectedAmount = amount;
        amountNameField.setText(amount.getName());
        amountCountComponent.setValue(amount.getCount());
        amountDistinctCountComponent.setValue(amount.getDistinctCount());
    }

    private void updateDefinitionSelectionView(Definition definition) {
        selectedDefinition = definition;
        definitionNameField.setText(definition.getName());
        definitionCompletenessComponent.setValue(definition.getCompleteness());
    }
}
