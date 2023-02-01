package fr.alphonse.drawingpad.view;

import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.Vertex;
import fr.alphonse.drawingpad.data.model.value.LowerGraduation;
import fr.alphonse.drawingpad.data.model.value.UpperGraduation;
import fr.alphonse.drawingpad.data.model.value.WholeGraduation;
import fr.alphonse.drawingpad.document.utils.ChangeDetector;
import fr.alphonse.drawingpad.view.internal.GraduatedValueComponent;

import javax.swing.*;
import java.awt.*;

public class InfoComponent extends JPanel {

    private final java.util.List<Vertex.Id> selection;

    private final ChangeDetector modelChangeDetector;

    private JLabel multipleSelectionLabel;

    private Object selectedObject = null;

    private JTextField objectNameField;

    private Link selectedLink = null;

    private JTextField linkNameField;

    private GraduatedValueComponent<WholeGraduation> originFactorComponent;

    private GraduatedValueComponent<LowerGraduation> originLowerFactorComponent;

    private GraduatedValueComponent<UpperGraduation> originUpperFactorComponent;

    private GraduatedValueComponent<WholeGraduation> destinationFactorComponent;

    private GraduatedValueComponent<LowerGraduation> destinationLowerFactorComponent;

    private GraduatedValueComponent<UpperGraduation> destinationUpperFactorComponent;

    private static final String EMPTY_SELECTION_CARD = "empty";

    private static final String MULTIPLE_SELECTION_CARD = "multiple";

    private static final String OBJECT_SELECTION_CARD = "object";

    private static final String LINK_SELECTION_CARD = "link";

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

    private JLabel makeMultipleSelectionView() {
        JLabel label = new JLabel();
        label.setForeground(Color.WHITE);
        label.setHorizontalAlignment(SwingConstants.CENTER);
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
        originFactorComponent = new GraduatedValueComponent<>(WholeGraduation.class);
        panel.add(originFactorComponent);
        originLowerFactorComponent = new GraduatedValueComponent<>(LowerGraduation.class);
        panel.add(originLowerFactorComponent);
        originUpperFactorComponent = new GraduatedValueComponent<>(UpperGraduation.class);
        panel.add(originUpperFactorComponent);

        // destination factor
        panel.add(Box.createVerticalStrut(30));
        JLabel destinationFactorLabel = new JLabel("Destination Factor:");
        destinationFactorLabel.setForeground(Color.WHITE);
        destinationFactorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(destinationFactorLabel);
        destinationFactorComponent = new GraduatedValueComponent<>(WholeGraduation.class);
        panel.add(destinationFactorComponent);
        destinationLowerFactorComponent = new GraduatedValueComponent<>(LowerGraduation.class);
        panel.add(destinationLowerFactorComponent);
        destinationUpperFactorComponent = new GraduatedValueComponent<>(UpperGraduation.class);
        panel.add(destinationUpperFactorComponent);

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
        multipleSelectionLabel.setText(objectCount + " objects and " + linkCount + " links selected");
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
        originFactorComponent.setValue(link.getOriginFactor().getWholeValue());
        originLowerFactorComponent.setValue(link.getOriginFactor().getLowerValue());
        originUpperFactorComponent.setValue(link.getOriginFactor().getUpperValue());
        destinationFactorComponent.setValue(link.getDestinationFactor().getWholeValue());
        destinationLowerFactorComponent.setValue(link.getDestinationFactor().getLowerValue());
        destinationUpperFactorComponent.setValue(link.getDestinationFactor().getUpperValue());
    }
}
