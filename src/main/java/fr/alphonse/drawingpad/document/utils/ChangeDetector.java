package fr.alphonse.drawingpad.document.utils;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChangeDetector {

    private Object model;

    private final List<ListenerReference<?>> listenerReferences = new ArrayList<>();

    private int oldHashcode;

    private record ListenerReference<T>(SoftReference<T> reference, Consumer<T> action) {}

    public ChangeDetector(Object model) {
        this.model = model;
        this.oldHashcode = model.hashCode();
    }

    public <T> void addListener(T listener, Consumer<T> action) {
        var listenerReference = new ListenerReference<>(new SoftReference<>(listener), action);
        listenerReferences.add(listenerReference);
    }

    public void notifyChange() {
        int newHashcode = model.hashCode();
        if (newHashcode == oldHashcode) {
            return;
        }
        oldHashcode = newHashcode;
        for (ListenerReference<?> listenerReference: listenerReferences) {
            callListener(listenerReference);
        }
        listenerReferences.removeIf(listenerReference -> listenerReference.reference.get() == null);
    }

    private static <T> void callListener(ListenerReference<T> listenerReference) {
        var target = listenerReference.reference.get();
        if (target == null) {
            return;
        }
        listenerReference.action.accept(target);
    }

    public void reinitModel(Object model) {
        this.model = model;
        this.oldHashcode = model.hashCode();
    }
}
