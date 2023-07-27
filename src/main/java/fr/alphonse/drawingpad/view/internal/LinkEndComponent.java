package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.model.GraphElement;
import fr.alphonse.drawingpad.data.model.Link;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.reference.Reference;
import fr.alphonse.drawingpad.data.model.reference.ReferenceType;

import javax.swing.*;
import java.util.List;
import java.util.Map;

public class LinkEndComponent extends JPanel {

    private final JComboBox<String> originComboBox;

    private final JComboBox<String> destinationComboBox;

    private Link displayedLink;

    private List<ReferenceType> originReferenceTypes;

    private List<ReferenceType> destinationReferenceTypes;

    private Runnable changeListener;

    private boolean mustIgnoreCallbacks = false;

    private final static List<ReferenceType> OBJECT_REFERENCE_TYPES = List.of(
            ReferenceType.OBJECT,
            ReferenceType.OBJECT_COMPLETION,
            ReferenceType.OBJECT_QUANTITY,
            ReferenceType.OBJECT_QUANTITY_COMPLETION
    );

    private final static List<ReferenceType> LINK_REFERENCE_TYPES = List.of(
            ReferenceType.DIRECT_LINK,
            ReferenceType.DIRECT_LINK_COMPLETION,
            ReferenceType.REVERSE_LINK,
            ReferenceType.REVERSE_LINK_COMPLETION
    );

    private final static Map<ReferenceType, String> REFERENCE_TYPE_NAMES = Map.of(
            ReferenceType.OBJECT, "Object",
            ReferenceType.OBJECT_COMPLETION, "Completion",
            ReferenceType.OBJECT_QUANTITY, "Quantity",
            ReferenceType.OBJECT_QUANTITY_COMPLETION, "Quantity Completion",
            ReferenceType.DIRECT_LINK, "Direct Link",
            ReferenceType.DIRECT_LINK_COMPLETION, "Direct Completion",
            ReferenceType.REVERSE_LINK, "Reverse Link",
            ReferenceType.REVERSE_LINK_COMPLETION, "Reverse Completion"
    );

    public LinkEndComponent() {
        super();

        setBackground(null);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        originComboBox = new JComboBox<>();
        destinationComboBox = new JComboBox<>();
        this.add(originComboBox);
        this.add(destinationComboBox);
        originComboBox.addActionListener(arg -> reactToOriginChange());
        destinationComboBox.addActionListener(arg -> reactToDestinationChange());
    }

    private void reactToOriginChange() {
        if (mustIgnoreCallbacks) {
            return;
        }
        int index = originComboBox.getSelectedIndex();
        ReferenceType referenceType = originReferenceTypes.get(index);
        int originId = displayedLink.getOriginReference().id();
        displayedLink.setOriginReference(new Reference(referenceType, originId));
        if (changeListener != null) {
            changeListener.run();
        }
    }

    private void reactToDestinationChange() {
        if (mustIgnoreCallbacks) {
            return;
        }
        int index = destinationComboBox.getSelectedIndex();
        ReferenceType referenceType = destinationReferenceTypes.get(index);
        int destinationId = displayedLink.getDestinationReference().id();
        displayedLink.setDestinationReference(new Reference(referenceType, destinationId));
        if (changeListener != null) {
            changeListener.run();
        }
    }

    public void setDisplayedLink(Link displayedLink) {
        this.displayedLink = displayedLink;

        mustIgnoreCallbacks = true;

        this.originReferenceTypes = findElementReferenceTypes(displayedLink.getOriginElement());
        this.destinationReferenceTypes = findElementReferenceTypes(displayedLink.getDestinationElement());
        fillComboBoxWithReferenceTypes(originComboBox, originReferenceTypes);
        fillComboBoxWithReferenceTypes(destinationComboBox, destinationReferenceTypes);

        ReferenceType originReferenceType = displayedLink.getOriginReference().type();
        int originReferenceTypeIndex = originReferenceTypes.indexOf(originReferenceType);
        originComboBox.setSelectedIndex(originReferenceTypeIndex);

        ReferenceType destinationReferenceType = displayedLink.getDestinationReference().type();
        int destinationReferenceTypeIndex = destinationReferenceTypes.indexOf(destinationReferenceType);
        destinationComboBox.setSelectedIndex(destinationReferenceTypeIndex);

        mustIgnoreCallbacks = false;
    }

    private List<ReferenceType> findElementReferenceTypes(GraphElement element) {
        return switch (element) {
            case Object ignored -> OBJECT_REFERENCE_TYPES;
            case Link ignored -> LINK_REFERENCE_TYPES;
        };
    }

    private void fillComboBoxWithReferenceTypes(JComboBox<String> comboBox, List<ReferenceType> referenceTypes) {
        comboBox.removeAllItems();
        for (ReferenceType referenceType: referenceTypes) {
            String name = REFERENCE_TYPE_NAMES.get(referenceType);
            comboBox.addItem(name);
        }
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener;
    }
}
