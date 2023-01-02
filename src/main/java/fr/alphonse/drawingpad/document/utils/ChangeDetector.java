package fr.alphonse.drawingpad.document.utils;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChangeDetector {

    private Object model;

    private final ChangeDetector parent;

    private final List<SoftReference<ChangeDetector>> children = new ArrayList<>();

    private final List<ListenerReference<?>> listenerReferences = new ArrayList<>();

    private int oldHashcode;

    private record ListenerReference<T>(SoftReference<T> reference, Consumer<T> action) {}

    public ChangeDetector(Object model) {
        this(model, null);
    }

    private ChangeDetector(Object model, ChangeDetector parent) {
        this.model = model;
        this.parent = parent;
        this.oldHashcode = model.hashCode();
    }

    public <T> void addListener(T listener, Consumer<T> action) {
        var listenerReference = new ListenerReference<>(new SoftReference<>(listener), action);
        listenerReferences.add(listenerReference);
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
        for (ListenerReference<?> listenerReference: listenerReferences) {
            callListener(listenerReference);
        }
        listenerReferences.removeIf(listenerReference -> listenerReference.reference.get() == null);
        if (parent != null && parent != origin) {
            parent.notifyChange(this);
        }
    }

    private static <T> void callListener(ListenerReference<T> listenerReference) {
        var target = listenerReference.reference.get();
        if (target == null) {
            return;
        }
        listenerReference.action.accept(target);
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
