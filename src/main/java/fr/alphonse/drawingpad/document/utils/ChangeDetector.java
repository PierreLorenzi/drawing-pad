package fr.alphonse.drawingpad.document.utils;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class ChangeDetector {

    private Object model;

    private final ChangeDetector parent;

    private final List<SoftReference<ChangeDetector>> children = new ArrayList<>();

    private final List<BooleanSupplier> listeners = new ArrayList<>();

    private int oldHashcode;

    public ChangeDetector(Object model) {
        this(model, null);
    }

    private ChangeDetector(Object model, ChangeDetector parent) {
        this.model = model;
        this.parent = parent;
        this.oldHashcode = model.hashCode();
    }

    public void addListener(BooleanSupplier listener) {
        listeners.add(listener);
    }

    public void notifyChange() {
        notifyChange(this);
    }

    private void notifyChange(ChangeDetector origin) {
        int newHashcode = model.hashCode();
        if (newHashcode == oldHashcode) {
            return;
        }
        oldHashcode = newHashcode;
        for (SoftReference<ChangeDetector> childReference: children) {
            ChangeDetector child = childReference.get();
            if (child == null || child == origin) {
                continue;
            }
            child.notifyChange(this);
        }
        listeners.removeIf(Predicate.not(BooleanSupplier::getAsBoolean));
        if (parent != null && parent != origin) {
            parent.notifyChange(this);
        }
    }

    public ChangeDetector makeSubDetector(Object submodel) {
        ChangeDetector child = new ChangeDetector(submodel, this);
        this.children.add(new SoftReference<>(child));
        return child;
    }

    public void reinitModel(Object model) {
        this.model = model;
        this.oldHashcode = model.hashCode();
    }
}
