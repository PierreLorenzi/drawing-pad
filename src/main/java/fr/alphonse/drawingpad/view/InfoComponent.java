package fr.alphonse.drawingpad.view;

import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.document.utils.ChangeDetector;
import fr.alphonse.drawingpad.view.internal.GraduatedValueComponent;

import javax.swing.*;
import java.awt.*;

public class InfoComponent extends JPanel {

    private final java.util.List<GraphElement> selection;

    private final ChangeDetector modelChangeDetector;

    private JTextArea multipleSelectionLabel;

    private Object selectedObject = null;

    private JTextField objectNameField;

    private Completion selectedCompletion = null;

    private JTextField completionNameField;

    private GraduatedValueComponent completionValueComponent;

    private GraduatedValueComponent completionLocalValueComponent;

    private Quantity selectedQuantity = null;

    private JTextField quantityNameField;

    private GraduatedValueComponent quantityValueComponent;

    private GraduatedValueComponent quantityLocalValueComponent;

    private Link selectedLink = null;

    private JTextField linkNameField;

    private GraduatedValueComponent linkFactorComponent;

    private static final String EMPTY_SELECTION_CARD = "empty";

    private static final String MULTIPLE_SELECTION_CARD = "multiple";

    private static final String OBJECT_SELECTION_CARD = "object";

    private static final String COMPLETION_SELECTION_CARD = "completion";

    private static final String QUANTITY_SELECTION_CARD = "quantity";

    private static final String LINK_SELECTION_CARD = "link";

    public InfoComponent(java.util.List<GraphElement> selection, ChangeDetector changeDetector, ChangeDetector modelChangeDetector) {
        super();
        this.selection = selection;
        this.modelChangeDetector = modelChangeDetector;

        changeDetector.addListener(this, InfoComponent::reactToSelectionChange);

        setLayout(new CardLayout());
        add(makeEmptySelectionView(), EMPTY_SELECTION_CARD);
        add(makeMultipleSelectionView(), MULTIPLE_SELECTION_CARD);
        add(makeObjectSelectionView(), OBJECT_SELECTION_CARD);
        add(makeCompletionSelectionView(), COMPLETION_SELECTION_CARD);
        add(makeQuantitySelectionView(), QUANTITY_SELECTION_CARD);
        add(makeLinkSelectionView(), LINK_SELECTION_CARD);
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
        JTextField nameField = makeNameField(panel);
        nameField.addActionListener(event -> {if (this.selectedObject != null) {
            this.selectedObject.setName(nameField.getText());
            this.modelChangeDetector.notifyChange();
        }});
        this.objectNameField = nameField;

        return panel;
    }

    private JPanel makeInfoPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalStrut(50));
        return panel;
    }

    private JTextField makeNameField(JPanel panel) {

        // name
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(nameLabel);
        JTextField textField = new JTextField();
        textField.setMaximumSize(new Dimension(100000, 40));
        panel.add(textField);
        panel.add(Box.createVerticalGlue());

        return textField;
    }

    private JPanel makeCompletionSelectionView() {
        JPanel panel = makeInfoPanel();
        JTextField nameField = makeNameField(panel);
        nameField.addActionListener(event -> {if (this.selectedCompletion != null) {
            this.selectedCompletion.setName(nameField.getText());
            this.modelChangeDetector.notifyChange();
        }});
        this.completionNameField = nameField;

        // value
        panel.add(Box.createVerticalStrut(30));
        JLabel valueLabel = new JLabel("Value:");
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(valueLabel);
        completionValueComponent = new GraduatedValueComponent();
        panel.add(completionValueComponent);

        // local value
        panel.add(Box.createVerticalStrut(30));
        JLabel localValueLabel = new JLabel("Local Value:");
        localValueLabel.setForeground(Color.WHITE);
        localValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(localValueLabel);
        completionLocalValueComponent = new GraduatedValueComponent();
        panel.add(completionLocalValueComponent);

        return panel;
    }

    private JPanel makeQuantitySelectionView() {
        JPanel panel = makeInfoPanel();
        JTextField nameField = makeNameField(panel);
        nameField.addActionListener(event -> {if (this.selectedQuantity != null) {
            this.selectedQuantity.setName(nameField.getText());
            this.modelChangeDetector.notifyChange();
        }});
        this.quantityNameField = nameField;

        // value
        panel.add(Box.createVerticalStrut(30));
        JLabel valueLabel = new JLabel("Value:");
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(valueLabel);
        quantityValueComponent = new GraduatedValueComponent();
        panel.add(quantityValueComponent);

        // local value
        panel.add(Box.createVerticalStrut(30));
        JLabel localValueLabel = new JLabel("Local Value:");
        localValueLabel.setForeground(Color.WHITE);
        localValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(localValueLabel);
        quantityLocalValueComponent = new GraduatedValueComponent();
        panel.add(quantityLocalValueComponent);

        return panel;
    }

    private JPanel makeLinkSelectionView() {
        JPanel panel = makeInfoPanel();

        JTextField nameField = makeNameField(panel);
        nameField.addActionListener(event -> {if (this.selectedLink != null) {
            this.selectedLink.setName(nameField.getText());
            this.modelChangeDetector.notifyChange();
        }});
        this.linkNameField = nameField;

        // factor
        panel.add(Box.createVerticalStrut(30));
        JLabel factorLabel = new JLabel("Factor:");
        factorLabel.setForeground(Color.WHITE);
        factorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(factorLabel);
        linkFactorComponent = new GraduatedValueComponent();
        panel.add(linkFactorComponent);

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
                    case Completion completion -> {
                        switchToCard(COMPLETION_SELECTION_CARD);
                        updateCompletionSelectionView(completion);
                    }
                    case Quantity quantity -> {
                        switchToCard(QUANTITY_SELECTION_CARD);
                        updateQuantitySelectionView(quantity);
                    }
                    case Link link -> {
                        switchToCard(LINK_SELECTION_CARD);
                        updateLinkSelectionView(link);
                    }
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
        long completionCount = selection.stream().filter(id -> id instanceof Completion).count();
        long quantityCount = selection.stream().filter(id -> id instanceof Quantity).count();
        long linkCount = selection.stream().filter(id -> id instanceof Link).count();
        long total = objectCount + completionCount + quantityCount + linkCount;
        multipleSelectionLabel.setText(total + " elements in selection: " + objectCount + " objects, " + completionCount + " completions, " + quantityCount + " quantities, " + linkCount + " links");
    }

    private void updateObjectSelectionView(Object object) {
        selectedObject = object;
        objectNameField.setText(object.getName());
    }

    private void updateCompletionSelectionView(Completion completion) {
        selectedCompletion = completion;
        completionNameField.setText(completion.getName());
        completionValueComponent.setValue(completion.getValue());
        completionLocalValueComponent.setValue(completion.getLocalValue());
    }

    private void updateQuantitySelectionView(Quantity quantity) {
        selectedQuantity = quantity;
        quantityNameField.setText(quantity.getName());
        quantityValueComponent.setValue(quantity.getValue());
        quantityLocalValueComponent.setValue(quantity.getLocalValue());
    }

    private void updateLinkSelectionView(Link link) {
        selectedLink = link;
        linkNameField.setText(link.getName());
        linkFactorComponent.setValue(link.getFactor());
    }
}
