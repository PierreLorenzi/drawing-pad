package fr.alphonse.drawingpad.view;

import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.*;
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

    private GraduatedValueComponent<WholeGraduation> factorComponent;

    private GraduatedValueComponent<WholeGraduation> quantityComponent;

    private Definition selectedDefinition = null;

    private JTextField definitionNameField;

    private GraduatedValueComponent<LowerGraduation> definitionLocalCompletenessComponent;

    private GraduatedValueComponent<LowerGraduation> definitionGlobalCompletenessComponent;

    private static final String EMPTY_SELECTION_CARD = "empty";

    private static final String MULTIPLE_SELECTION_CARD = "multiple";

    private static final String OBJECT_SELECTION_CARD = "object";

    private static final String LINK_SELECTION_CARD = "link";

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

        // factor
        panel.add(Box.createVerticalStrut(30));
        JLabel factorLabel = new JLabel("Factor:");
        factorLabel.setForeground(Color.WHITE);
        factorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(factorLabel);
        factorComponent = new GraduatedValueComponent<>(WholeGraduation.class);
        panel.add(factorComponent);

        // quantity factor
        panel.add(Box.createVerticalStrut(30));
        JLabel quantityLabel = new JLabel("Quantity:");
        quantityLabel.setForeground(Color.WHITE);
        quantityLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(quantityLabel);
        quantityComponent = new GraduatedValueComponent<>(WholeGraduation.class);
        panel.add(quantityComponent);

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

        // local completeness
        panel.add(Box.createVerticalStrut(30));
        JLabel localCompletenessLabel = new JLabel("Local Completeness:");
        localCompletenessLabel.setForeground(Color.WHITE);
        localCompletenessLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(localCompletenessLabel);
        definitionLocalCompletenessComponent = new GraduatedValueComponent<>(LowerGraduation.class);
        panel.add(definitionLocalCompletenessComponent);

        // global completeness
        panel.add(Box.createVerticalStrut(30));
        JLabel globalCompletenessLabel = new JLabel("Global Completeness:");
        globalCompletenessLabel.setForeground(Color.WHITE);
        globalCompletenessLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(globalCompletenessLabel);
        definitionGlobalCompletenessComponent = new GraduatedValueComponent<>(LowerGraduation.class);
        panel.add(definitionGlobalCompletenessComponent);

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
        long definitionCount = selection.stream().filter(id -> id instanceof Definition).count();
        long total = objectCount + linkCount + definitionCount;
        multipleSelectionLabel.setText(total + " elements in selection: " + objectCount + " objects, " + definitionCount + " definitions, " + linkCount + " links");
    }

    private void updateObjectSelectionView(Object object) {
        selectedObject = object;
        objectNameField.setText(object.getName());
    }

    private void updateLinkSelectionView(Link link) {
        selectedLink = link;
        linkNameField.setText(link.getName());
        factorComponent.setValue(link.getFactor());
        quantityComponent.setValue(link.getQuantity());
    }

    private void updateDefinitionSelectionView(Definition definition) {
        selectedDefinition = definition;
        definitionNameField.setText(definition.getName());
        definitionLocalCompletenessComponent.setValue(definition.getLocalCompleteness());
        definitionGlobalCompletenessComponent.setValue(definition.getGlobalCompleteness());
    }
}
