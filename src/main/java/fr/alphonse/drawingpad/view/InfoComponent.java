package fr.alphonse.drawingpad.view;

import fr.alphonse.drawingpad.data.Drawing;
import fr.alphonse.drawingpad.data.geometry.Vector;
import fr.alphonse.drawingpad.data.model.Object;
import fr.alphonse.drawingpad.data.model.*;
import fr.alphonse.drawingpad.document.utils.ChangeDetector;
import fr.alphonse.drawingpad.view.internal.GraduatedValueComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Consumer;

public class InfoComponent extends JPanel {

    private final java.util.List<GraphElement> selection;

    private final Drawing model;

    private final ChangeDetector<?,?> modelChangeDetector;

    private JTextArea noteField;

    private JTextArea multipleSelectionLabel;

    private Object selectedObject = null;

    private JTextField objectNameField;

    private JCheckBox objectNameVisibleCheckBox;

    private Completion selectedCompletion = null;

    private JTextField completionNameField;

    private JCheckBox completionNameVisibleCheckBox;

    private GraduatedValueComponent completionValueComponent;

    private Link selectedLink = null;

    private JTextField linkNameField;

    private JCheckBox linkNameVisibleCheckBox;

    private GraduatedValueComponent linkFactorComponent;

    private static final String EMPTY_SELECTION_CARD = "empty";

    private static final String MULTIPLE_SELECTION_CARD = "multiple";

    private static final String OBJECT_SELECTION_CARD = "object";

    private static final String COMPLETION_SELECTION_CARD = "completion";

    private static final String LINK_SELECTION_CARD = "link";

    private final static Vector OBJECT_NAME_SHIFT = new Vector(14, 5);

    private final static Vector CIRCLE_NAME_SHIFT = new Vector(10, 4);

    private final static Vector LINK_NAME_SHIFT = new Vector(0, -6);

    public InfoComponent(java.util.List<GraphElement> selection, ChangeDetector<?,?> selectionChangeDetector, ChangeDetector<?,?> modelChangeDetector, Drawing model) {
        super();
        this.selection = selection;
        this.modelChangeDetector = modelChangeDetector;
        this.model = model;

        selectionChangeDetector.addListener(this, InfoComponent::reactToSelectionChange);
        modelChangeDetector.addListener(this, InfoComponent::reactToModelChange);

        setLayout(new CardLayout());
        add(makeEmptySelectionView(), EMPTY_SELECTION_CARD);
        add(makeMultipleSelectionView(), MULTIPLE_SELECTION_CARD);
        add(makeObjectSelectionView(), OBJECT_SELECTION_CARD);
        add(makeCompletionSelectionView(), COMPLETION_SELECTION_CARD);
        add(makeLinkSelectionView(), LINK_SELECTION_CARD);

        noteField.setText(model.getNote());
        switchToCard(EMPTY_SELECTION_CARD);

        setBackground(Color.DARK_GRAY);
        setBorder(BorderFactory.createLoweredBevelBorder());
        setPreferredSize(new Dimension(300, 300));
    }

    private JPanel makeEmptySelectionView() {
        var view = makeInfoPanel();
        JTextArea textArea = new JTextArea();
        textArea.setBackground(null);
        textArea.setForeground(Color.WHITE);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setCaretColor(Color.WHITE);
        disableTabbingInTextAreas(textArea);
        textArea.setFont(new Font("Verdana", Font.PLAIN, 14));
        textArea.setMargin(new Insets(0, 2, 0, 0));
        textArea.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                InfoComponent.this.model.setNote(textArea.getText());
                InfoComponent.this.modelChangeDetector.notifyChangeCausedBy(InfoComponent.this);
            }
        });
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.getViewport().setBackground(null);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBackground(null);
        scrollPane.setOpaque(false);
        view.add(scrollPane);
        this.noteField = textArea;
        return view;
    }

    private static void disableTabbingInTextAreas(JTextArea textArea){
        textArea.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyChar() == '\t'){
                    textArea.transferFocus();
                    e.consume();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        });
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
        this.objectNameField = makeNameField(panel, text -> {if (this.selectedObject != null) {
            this.selectedObject.setName(text);
            changeNameVisible(selectedObject, !text.isEmpty());
            objectNameVisibleCheckBox.setSelected(!text.isEmpty());
            this.modelChangeDetector.notifyChangeCausedBy(InfoComponent.this);
        }});

        JCheckBox nameVisibleCheckBox = makeNameVisibleCheckBox(panel);
        nameVisibleCheckBox.addActionListener(event -> {
            if (this.selectedObject != null) {
                changeNameVisible(this.selectedObject, nameVisibleCheckBox.isSelected());
                this.modelChangeDetector.notifyChangeCausedBy(InfoComponent.this);
            }
        });
        this.objectNameVisibleCheckBox = nameVisibleCheckBox;

        return panel;
    }

    private JPanel makeInfoPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalStrut(50));
        return panel;
    }

    private JTextField makeNameField(JPanel panel, Consumer<String> action) {

        // name
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(nameLabel);
        JTextField textField = new JTextField();
        textField.setMaximumSize(new Dimension(100000, 40));
        panel.add(textField);
        panel.add(Box.createVerticalGlue());
        textField.addActionListener(e -> action.accept(textField.getText()));
        textField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                action.accept(textField.getText());
            }
        });

        return textField;
    }

    private JCheckBox makeNameVisibleCheckBox(JPanel panel) {

        // name
        var checkbox = new JCheckBox();
        checkbox.setText("Visible Name");
        checkbox.setForeground(Color.WHITE);
        checkbox.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(checkbox);

        return checkbox;
    }

    private void changeNameVisible(GraphElement element, boolean newValue) {
        if (newValue && model.getNamePositions().get(element) == null) {
            Vector shift = switch (element) {
                case Object ignored -> OBJECT_NAME_SHIFT;
                case Completion ignored -> CIRCLE_NAME_SHIFT;
                case Link ignored -> LINK_NAME_SHIFT;
            };
            model.getNamePositions().put(element, shift);
        }
        else if (!newValue && model.getNamePositions().get(element) != null) {
            model.getNamePositions().remove(element);
        }
    }

    private JPanel makeCompletionSelectionView() {
        JPanel panel = makeInfoPanel();
        this.completionNameField = makeNameField(panel, text -> {if (this.selectedCompletion != null) {
            this.selectedCompletion.setName(text);
            changeNameVisible(selectedCompletion, !text.isEmpty());
            completionNameVisibleCheckBox.setSelected(!text.isEmpty());
            this.modelChangeDetector.notifyChangeCausedBy(InfoComponent.this);
        }});

        JCheckBox nameVisibleCheckBox = makeNameVisibleCheckBox(panel);
        nameVisibleCheckBox.addActionListener(event -> {
            if (this.selectedCompletion != null) {
                changeNameVisible(this.selectedCompletion, nameVisibleCheckBox.isSelected());
                this.modelChangeDetector.notifyChangeCausedBy(InfoComponent.this);
            }
        });
        this.completionNameVisibleCheckBox = nameVisibleCheckBox;

        // value
        panel.add(Box.createVerticalStrut(30));
        JLabel valueLabel = new JLabel("Value:");
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(valueLabel);
        completionValueComponent = new GraduatedValueComponent(() -> modelChangeDetector.notifyChangeCausedBy(InfoComponent.this));
        panel.add(completionValueComponent);

        return panel;
    }

    private JPanel makeLinkSelectionView() {
        JPanel panel = makeInfoPanel();

        this.linkNameField = makeNameField(panel, text -> {if (this.selectedLink != null) {
            this.selectedLink.setName(text);
            changeNameVisible(selectedLink, !text.isEmpty());
            linkNameVisibleCheckBox.setSelected(!text.isEmpty());
            this.modelChangeDetector.notifyChangeCausedBy(InfoComponent.this);
        }});

        JCheckBox nameVisibleCheckBox = makeNameVisibleCheckBox(panel);
        nameVisibleCheckBox.addActionListener(event -> {
            if (this.selectedLink != null) {
                changeNameVisible(this.selectedLink, nameVisibleCheckBox.isSelected());
                this.modelChangeDetector.notifyChangeCausedBy(InfoComponent.this);
            }
        });
        this.linkNameVisibleCheckBox = nameVisibleCheckBox;

        // factor
        panel.add(Box.createVerticalStrut(30));
        JLabel factorLabel = new JLabel("Factor:");
        factorLabel.setForeground(Color.WHITE);
        factorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(factorLabel);
        linkFactorComponent = new GraduatedValueComponent(() -> modelChangeDetector.notifyChangeCausedBy(InfoComponent.this));
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
        long linkCount = selection.stream().filter(id -> id instanceof Link).count();
        long total = objectCount + completionCount + linkCount;
        multipleSelectionLabel.setText(total + " elements in selection: " + objectCount + " objects, " + completionCount + " completions, " + linkCount + " links");
    }

    private void updateObjectSelectionView(Object object) {
        selectedObject = object;
        objectNameField.setText(object.getName());
        objectNameVisibleCheckBox.setSelected(checkIfNameVisible(object));
    }

    private boolean checkIfNameVisible(GraphElement element) {
        return model.getNamePositions().get(element) != null;
    }

    private void updateCompletionSelectionView(Completion completion) {
        selectedCompletion = completion;
        completionNameField.setText(completion.getName());
        completionValueComponent.setValue(completion.getValue());
        completionNameVisibleCheckBox.setSelected(checkIfNameVisible(completion));
    }

    private void updateLinkSelectionView(Link link) {
        selectedLink = link;
        linkNameField.setText(link.getName());
        linkFactorComponent.setValue(link.getFactor());
        linkNameVisibleCheckBox.setSelected(checkIfNameVisible(link));
    }

    private void reactToModelChange() {
        // we don't need to react to object changes, because they are deselected on undo/redo
        // But we must update the note if necessary
        if (!model.getNote().equals(noteField.getText())) {
            noteField.setText(model.getNote());
            noteField.transferFocus();
        }
    }
}
