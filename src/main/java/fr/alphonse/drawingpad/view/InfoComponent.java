package fr.alphonse.drawingpad.view;

import fr.alphonse.drawingpad.data.model.GraphElement;
import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.document.utils.ChangeDetector;
import fr.alphonse.drawingpad.view.internal.GraduatedValueComponent;
import fr.alphonse.drawingpad.view.internal.LinkEndComponent;

import javax.swing.*;
import java.awt.*;

public class InfoComponent extends JPanel {

    private final java.util.List<GraphElement> selection;

    private final ChangeDetector modelChangeDetector;

    private JTextArea multipleSelectionLabel;

    private Object selectedObject = null;

    private JTextField objectNameField;

    private GraduatedValueComponent objectCompletionComponent;

    private GraduatedValueComponent objectLocalCompletionComponent;

    private GraduatedValueComponent objectQuantityComponent;

    private GraduatedValueComponent objectQuantityCompletionComponent;

    private Link selectedLink = null;

    private LinkEndComponent linkEndComponent;

    private JTextField linkNameField;

    private GraduatedValueComponent linkCompletionComponent;

    private GraduatedValueComponent linkFactorComponent;

    private static final String EMPTY_SELECTION_CARD = "empty";

    private static final String MULTIPLE_SELECTION_CARD = "multiple";

    private static final String OBJECT_SELECTION_CARD = "object";

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
        ElementFieldSet elementFieldSet = makeElementFields(panel);
        elementFieldSet.name().addActionListener(event -> {if (this.selectedObject != null) {
            this.selectedObject.setName(elementFieldSet.name().getText());
            this.modelChangeDetector.notifyChange();
        }});
        this.objectNameField = elementFieldSet.name();
        this.objectCompletionComponent = elementFieldSet.completion();

        // local completion
        panel.add(Box.createVerticalStrut(30));
        JLabel localCompletionLabel = new JLabel("Local Completion:");
        localCompletionLabel.setForeground(Color.WHITE);
        localCompletionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(localCompletionLabel);
        GraduatedValueComponent localCompletionComponent = new GraduatedValueComponent();
        panel.add(localCompletionComponent);
        this.objectLocalCompletionComponent = localCompletionComponent;

        // quantity
        panel.add(Box.createVerticalStrut(30));
        JLabel quantityLabel = new JLabel("Quantity:");
        quantityLabel.setForeground(Color.WHITE);
        quantityLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(quantityLabel);
        GraduatedValueComponent quantityComponent = new GraduatedValueComponent();
        panel.add(quantityComponent);
        this.objectQuantityComponent = quantityComponent;

        // quantity completion
        panel.add(Box.createVerticalStrut(30));
        JLabel quantityCompletionLabel = new JLabel("Quantity Completion:");
        quantityCompletionLabel.setForeground(Color.WHITE);
        quantityCompletionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(quantityCompletionLabel);
        GraduatedValueComponent quantityCompletionComponent = new GraduatedValueComponent();
        panel.add(quantityCompletionComponent);
        this.objectQuantityCompletionComponent = quantityCompletionComponent;

        return panel;
    }

    private JPanel makeInfoPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalStrut(50));
        return panel;
    }

    private record ElementFieldSet(JTextField name, GraduatedValueComponent completion) {}

    private ElementFieldSet makeElementFields(JPanel panel) {

        // name
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(nameLabel);
        JTextField textField = new JTextField();
        textField.setMaximumSize(new Dimension(100000, 40));
        panel.add(textField);
        panel.add(Box.createVerticalGlue());

        // completion
        panel.add(Box.createVerticalStrut(30));
        JLabel completionLabel = new JLabel("Completion:");
        completionLabel.setForeground(Color.WHITE);
        completionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(completionLabel);
        GraduatedValueComponent completionComponent = new GraduatedValueComponent();
        panel.add(completionComponent);

        return new ElementFieldSet(textField, completionComponent);
    }

    private JPanel makeLinkSelectionView() {
        JPanel panel = makeInfoPanel();

        // link end
        linkEndComponent = new LinkEndComponent();
        linkEndComponent.setChangeListener(this.modelChangeDetector::notifyChange);
        panel.add(linkEndComponent);

        // name completion
        ElementFieldSet elementFieldSet = makeElementFields(panel);
        elementFieldSet.name().addActionListener(event -> {if (this.selectedLink != null) {
            this.selectedLink.setName(elementFieldSet.name().getText());
            this.modelChangeDetector.notifyChange();
        }});
        this.linkNameField = elementFieldSet.name();
        this.linkCompletionComponent = elementFieldSet.completion();

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
        long linkCount = selection.stream().filter(id -> id instanceof Link).count();
        long total = objectCount + linkCount;
        multipleSelectionLabel.setText(total + " elements in selection: " + objectCount + " objects, " + linkCount + " links");
    }

    private void updateObjectSelectionView(Object object) {
        selectedObject = object;
        objectNameField.setText(object.getName());
        objectCompletionComponent.setValue(object.getCompletion());
        objectLocalCompletionComponent.setValue(object.getLocalCompletion());
        objectQuantityComponent.setValue(object.getQuantity());
        objectQuantityCompletionComponent.setValue(object.getQuantityCompletion());
    }

    private void updateLinkSelectionView(Link link) {
        selectedLink = link;
        linkEndComponent.setDisplayedLink(link);
        linkNameField.setText(link.getName());
        linkCompletionComponent.setValue(link.getCompletion());
        linkFactorComponent.setValue(link.getFactor());
    }
}
