package fr.alphonse.drawingpad.view.internal;

import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.data.model.reference.Reference;
import fr.alphonse.drawingpad.data.model.reference.ReferenceType;

import javax.swing.*;
import java.util.List;
import java.util.Map;

public class LinkEndComponent extends JPanel {

    private final JComboBox<String> originComboBox;

    private final JComboBox<String> destinationComboBox;

    private Link displayedLink;

    private Vertex originOwner;

    private Vertex destinationOwner;

    private List<ReferenceType> originReferenceTypes;

    private List<ReferenceType> destinationReferenceTypes;

    private Runnable changeListener;

    private boolean mustIgnoreCallbacks = false;

    private final static List<ReferenceType> OBJECT_REFERENCE_TYPES = List.of(
            ReferenceType.OBJECT,
            ReferenceType.OBJECT_COMPLETENESS,
            ReferenceType.OBJECT_QUANTITY,
            ReferenceType.OBJECT_QUANTITY_COMPLETENESS
    );

    private final static List<ReferenceType> POSSESSION_LINK_REFERENCE_TYPES = List.of(
            ReferenceType.POSSESSION_LINK,
            ReferenceType.POSSESSION_LINK_COMPLETENESS
    );

    private final static List<ReferenceType> COMPARISON_LINK_REFERENCE_TYPES = List.of(
            ReferenceType.COMPARISON_LINK,
            ReferenceType.COMPARISON_LINK_COMPLETENESS
    );

    private final static Map<ReferenceType, String> REFERENCE_TYPE_NAMES = Map.of(
            ReferenceType.OBJECT, "Object",
            ReferenceType.OBJECT_COMPLETENESS, "Completeness",
            ReferenceType.OBJECT_QUANTITY, "Quantity",
            ReferenceType.OBJECT_QUANTITY_COMPLETENESS, "Quantity Completeness",
            ReferenceType.POSSESSION_LINK, "Link",
            ReferenceType.POSSESSION_LINK_COMPLETENESS, "Completeness",
            ReferenceType.COMPARISON_LINK, "Link",
            ReferenceType.COMPARISON_LINK_COMPLETENESS, "Completeness"
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
        Vertex newOrigin = findVertexWithReferenceType(referenceType, originOwner);
        displayedLink.setOrigin(newOrigin);
        displayedLink.setOriginReference(new Reference(referenceType, originOwner.getId()));
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
        Vertex newDestination = findVertexWithReferenceType(referenceType, destinationOwner);
        displayedLink.setDestination(newDestination);
        displayedLink.setDestinationReference(new Reference(referenceType, destinationOwner.getId()));
        if (changeListener != null) {
            changeListener.run();
        }
    }

    private Vertex findVertexWithReferenceType(ReferenceType referenceType, Vertex owner) {
        return switch (referenceType) {
            case OBJECT -> (Object)owner;
            case OBJECT_COMPLETENESS -> ((Object)owner).getCompleteness();
            case OBJECT_QUANTITY -> ((Object)owner).getQuantity();
            case OBJECT_QUANTITY_COMPLETENESS -> ((Object)owner).getQuantity().getCompleteness();
            case POSSESSION_LINK -> (PossessionLink)owner;
            case POSSESSION_LINK_COMPLETENESS -> ((PossessionLink)owner).getCompleteness();
            case COMPARISON_LINK -> (ComparisonLink)owner;
            case COMPARISON_LINK_COMPLETENESS -> ((ComparisonLink)owner).getCompleteness();
        };
    }

    public void setDisplayedLink(Link displayedLink) {
        this.displayedLink = displayedLink;
        this.originOwner = findOwner(displayedLink.getOrigin());
        this.destinationOwner = findOwner(displayedLink.getDestination());

        mustIgnoreCallbacks = true;

        this.originReferenceTypes = findVertexReferenceTypes(this.originOwner);
        this.destinationReferenceTypes = findVertexReferenceTypes(this.destinationOwner);
        fillComboBoxWithReferenceTypes(originComboBox, originReferenceTypes);
        fillComboBoxWithReferenceTypes(destinationComboBox, destinationReferenceTypes);

        ReferenceType originReferenceType = displayedLink.getOriginReference().type();
        int originReferenceTypeIndex = originReferenceTypes.indexOf(originReferenceType);
        originComboBox.setSelectedIndex(originReferenceTypeIndex);

        ReferenceType destinationReferenceType = displayedLink.getDestinationReference().type();;
        int destinationReferenceTypeIndex = destinationReferenceTypes.indexOf(destinationReferenceType);
        destinationComboBox.setSelectedIndex(destinationReferenceTypeIndex);

        mustIgnoreCallbacks = false;
    }

    private Vertex findOwner(Vertex vertex) {
        if (vertex instanceof Value value) {
            return findOwner(value.getOwner());
        }
        return vertex;
    }

    private List<ReferenceType> findVertexReferenceTypes(Vertex vertex) {
        return switch (vertex) {
            case Object ignored -> OBJECT_REFERENCE_TYPES;
            case PossessionLink ignored -> POSSESSION_LINK_REFERENCE_TYPES;
            case ComparisonLink ignored -> COMPARISON_LINK_REFERENCE_TYPES;
            case WholeValue ignored -> throw new Error("Can't use whole value as link end");
            case LowerValue ignored -> throw new Error("Can't use lower value as link end");
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
