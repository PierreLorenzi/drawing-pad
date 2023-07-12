package fr.alphonse.drawingpad.view;

import fr.alphonse.drawingpad.data.model.Amount;
import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.Vertex;
import fr.alphonse.drawingpad.data.model.value.LowerGraduation;
import fr.alphonse.drawingpad.data.model.value.WholeGraduation;
import fr.alphonse.drawingpad.document.utils.ChangeDetector;
import fr.alphonse.drawingpad.view.internal.GraduatedValueComponent;

import javax.swing.*;
import java.awt.*;

public class InfoComponent extends JPanel {

    private final java.util.List<Vertex.Id> selection;

    private final ChangeDetector modelChangeDetector;

    private JTextArea multipleSelectionLabel;

    private Object selectedObject = null;

    private JTextField objectNameField;

    private Link selectedLink = null;

    private JTextField linkNameField;

    private GraduatedValueComponent<LowerGraduation> originFactorComponent;

    private GraduatedValueComponent<LowerGraduation> destinationFactorComponent;

    private Amount selectedAmount = null;

    private JTextField amountNameField;

    private GraduatedValueComponent<WholeGraduation> amountCountComponent;

    private GraduatedValueComponent<WholeGraduation> amountDistinctCountComponent;

    private static final String EMPTY_SELECTION_CARD = "empty";

    private static final String MULTIPLE_SELECTION_CARD = "multiple";

    private static final String OBJECT_SELECTION_CARD = "object";

    private static final String LINK_SELECTION_CARD = "link";

    private static final String AMOUNT_SELECTION_CARD = "amount";

    public InfoComponent(java.util.List<Vertex.Id> selection, ChangeDetector changeDetector, ChangeDetector modelChangeDetector) {
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

    private void reactToSelectionChange() {

        selectedObject = null;

        var count = selection.size();
        switch (count) {
            case 0 -> switchToCard(EMPTY_SELECTION_CARD);
            case 1 -> {
                switch (selection.get(0)) {
                    case Object.Id objectId -> {
                        switchToCard(OBJECT_SELECTION_CARD);
                        updateObjectSelectionView(objectId);
                    }
                    case Link.Id linkId -> {
                        switchToCard(LINK_SELECTION_CARD);
                        updateLinkSelectionView(linkId);
                    }
                    case Amount.Id amountId -> {
                        switchToCard(AMOUNT_SELECTION_CARD);
                        updateAmountSelectionView(amountId);
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
        long objectCount = selection.stream().filter(id -> id instanceof Object.Id).count();
        long linkCount = selection.stream().filter(id -> id instanceof Link.Id).count();
        long amountCount = selection.stream().filter(id -> id instanceof Amount.Id).count();
        long total = objectCount + linkCount + amountCount;
        multipleSelectionLabel.setText(total + " elements in selection: " + objectCount + " objects, " + amountCount + " amounts, " + linkCount + " links");
    }

    private void updateObjectSelectionView(Object.Id id) {
        Object object = id.state();
        selectedObject = object;
        objectNameField.setText(object.getName());
    }

    private void updateLinkSelectionView(Link.Id id) {
        Link link = id.state();
        selectedLink = link;
        linkNameField.setText(link.getName());
        originFactorComponent.setValue(link.getOriginFactor());
        destinationFactorComponent.setValue(link.getDestinationFactor());
    }

    private void updateAmountSelectionView(Amount.Id id) {
        Amount amount = id.state();
        selectedAmount = amount;
        amountNameField.setText(amount.getName());
        amountCountComponent.setValue(amount.getCount());
        amountDistinctCountComponent.setValue(amount.getDistinctCount());
    }
}
