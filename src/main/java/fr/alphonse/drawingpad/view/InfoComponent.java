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

    private GraduatedValueComponent<LowerGraduation> objectCompletenessComponent;

    private GraduatedValueComponent<LowerGraduation> objectLocalCompletenessComponent;

    private GraduatedValueComponent<WholeGraduation> objectQuantityComponent;

    private PossessionLink selectedPossessionLink = null;

    private JTextField possessionLinkNameField;

    private GraduatedValueComponent<LowerGraduation> possessionLinkCompletenessComponent;

    private GraduatedValueComponent<LowerGraduation> possessionLinkFactorComponent;

    private ComparisonLink selectedComparisonLink = null;

    private JTextField comparisonLinkNameField;

    private GraduatedValueComponent<LowerGraduation> comparisonLinkCompletenessComponent;

    private GraduatedValueComponent<WholeGraduation> comparisonLinkFactorComponent;

    private static final String EMPTY_SELECTION_CARD = "empty";

    private static final String MULTIPLE_SELECTION_CARD = "multiple";

    private static final String OBJECT_SELECTION_CARD = "object";

    private static final String POSSESSION_LINK_SELECTION_CARD = "possessionLink";

    private static final String COMPARISON_LINK_SELECTION_CARD = "comparisonLink";

    public InfoComponent(java.util.List<Vertex> selection, ChangeDetector changeDetector, ChangeDetector modelChangeDetector) {
        super();
        this.selection = selection;
        this.modelChangeDetector = modelChangeDetector;

        changeDetector.addListener(this, InfoComponent::reactToSelectionChange);

        setLayout(new CardLayout());
        add(makeEmptySelectionView(), EMPTY_SELECTION_CARD);
        add(makeMultipleSelectionView(), MULTIPLE_SELECTION_CARD);
        add(makeObjectSelectionView(), OBJECT_SELECTION_CARD);
        add(makePossessionLinkSelectionView(), POSSESSION_LINK_SELECTION_CARD);
        add(makeComparisonLinkSelectionView(), COMPARISON_LINK_SELECTION_CARD);
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
        JPanel panel = makeInfoPanel();
        VertexFieldSet vertexFieldSet = makeVertexFields(panel);
        vertexFieldSet.name().addActionListener(event -> {if (this.selectedObject != null) {
            this.selectedObject.setName(vertexFieldSet.name().getText());
            this.modelChangeDetector.notifyChange();
        }});
        this.objectNameField = vertexFieldSet.name();
        this.objectCompletenessComponent = vertexFieldSet.completeness();

        // local completeness
        panel.add(Box.createVerticalStrut(30));
        JLabel localCompletenessLabel = new JLabel("Local Completeness:");
        localCompletenessLabel.setForeground(Color.WHITE);
        localCompletenessLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(localCompletenessLabel);
        GraduatedValueComponent<LowerGraduation> localCompletenessComponent = new GraduatedValueComponent<>(LowerGraduation.class);
        panel.add(localCompletenessComponent);
        this.objectLocalCompletenessComponent = localCompletenessComponent;

        // quantity
        panel.add(Box.createVerticalStrut(30));
        JLabel quantityLabel = new JLabel("Quantity:");
        quantityLabel.setForeground(Color.WHITE);
        quantityLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(quantityLabel);
        GraduatedValueComponent<WholeGraduation> quantityComponent = new GraduatedValueComponent<>(WholeGraduation.class);
        panel.add(quantityComponent);
        this.objectQuantityComponent = quantityComponent;

        return panel;
    }

    private JPanel makeInfoPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalStrut(50));
        return panel;
    }

    private record VertexFieldSet(JTextField name, GraduatedValueComponent<LowerGraduation> completeness) {}

    private VertexFieldSet makeVertexFields(JPanel panel) {

        // name
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(nameLabel);
        JTextField textField = new JTextField();
        textField.setMaximumSize(new Dimension(100000, 40));
        panel.add(textField);
        panel.add(Box.createVerticalGlue());

        // completeness
        panel.add(Box.createVerticalStrut(30));
        JLabel completenessLabel = new JLabel("Completeness:");
        completenessLabel.setForeground(Color.WHITE);
        completenessLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(completenessLabel);
        GraduatedValueComponent<LowerGraduation> completenessComponent = new GraduatedValueComponent<>(LowerGraduation.class);
        panel.add(completenessComponent);

        return new VertexFieldSet(textField, completenessComponent);
    }

    private JPanel makePossessionLinkSelectionView() {
        JPanel panel = makeInfoPanel();
        VertexFieldSet vertexFieldSet = makeVertexFields(panel);
        vertexFieldSet.name().addActionListener(event -> {if (this.selectedPossessionLink != null) {
            this.selectedPossessionLink.setName(vertexFieldSet.name().getText());
            this.modelChangeDetector.notifyChange();
        }});
        this.possessionLinkNameField = vertexFieldSet.name();
        this.possessionLinkCompletenessComponent = vertexFieldSet.completeness();

        // factor
        panel.add(Box.createVerticalStrut(30));
        JLabel factorLabel = new JLabel("Factor:");
        factorLabel.setForeground(Color.WHITE);
        factorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(factorLabel);
        possessionLinkFactorComponent = new GraduatedValueComponent<>(LowerGraduation.class);
        panel.add(possessionLinkFactorComponent);

        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel makeComparisonLinkSelectionView() {
        JPanel panel = makeInfoPanel();
        VertexFieldSet vertexFieldSet = makeVertexFields(panel);
        vertexFieldSet.name().addActionListener(event -> {if (this.selectedComparisonLink != null) {
            this.selectedComparisonLink.setName(vertexFieldSet.name().getText());
            this.modelChangeDetector.notifyChange();
        }});
        this.comparisonLinkNameField = vertexFieldSet.name();
        this.comparisonLinkCompletenessComponent = vertexFieldSet.completeness();

        // factor
        panel.add(Box.createVerticalStrut(30));
        JLabel factorLabel = new JLabel("Factor:");
        factorLabel.setForeground(Color.WHITE);
        factorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(factorLabel);
        comparisonLinkFactorComponent = new GraduatedValueComponent<>(WholeGraduation.class);
        panel.add(comparisonLinkFactorComponent);

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
                    case PossessionLink possessionLink -> {
                        switchToCard(POSSESSION_LINK_SELECTION_CARD);
                        updatePossessionLinkSelectionView(possessionLink);
                    }
                    case ComparisonLink comparisonLink -> {
                        switchToCard(COMPARISON_LINK_SELECTION_CARD);
                        updateComparisonLinkSelectionView(comparisonLink);
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
        long possessionLinkCount = selection.stream().filter(id -> id instanceof PossessionLink).count();
        long comparisonLinkCount = selection.stream().filter(id -> id instanceof ComparisonLink).count();
        long total = objectCount + possessionLinkCount + comparisonLinkCount;
        multipleSelectionLabel.setText(total + " elements in selection: " + objectCount + " objects, " + possessionLinkCount + " possessions, " + comparisonLinkCount + " comparisons");
    }

    private void updateObjectSelectionView(Object object) {
        selectedObject = object;
        objectNameField.setText(object.getName());
        objectCompletenessComponent.setValue(object.getCompleteness().getValue());
        objectLocalCompletenessComponent.setValue(object.getLocalCompleteness().getValue());
        objectQuantityComponent.setValue(object.getQuantity().getValue());
    }

    private void updatePossessionLinkSelectionView(PossessionLink possessionLink) {
        selectedPossessionLink = possessionLink;
        possessionLinkNameField.setText(possessionLink.getName());
        possessionLinkCompletenessComponent.setValue(possessionLink.getCompleteness().getValue());
        possessionLinkFactorComponent.setValue(possessionLink.getFactor());
    }

    private void updateComparisonLinkSelectionView(ComparisonLink comparisonLink) {
        selectedComparisonLink = comparisonLink;
        comparisonLinkNameField.setText(comparisonLink.getName());
        comparisonLinkCompletenessComponent.setValue(comparisonLink.getCompleteness().getValue());
        comparisonLinkFactorComponent.setValue(comparisonLink.getFactor());
    }
}
