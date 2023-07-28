package fr.alphonse.drawingpad.document.utils;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ChangeDetector<T,S> {

    private final T model;

    private S currentState;

    private final Function<T, S> stateFunction;

    private final List<ListenerReference<?>> listenerReferences = new ArrayList<>();

    private record ListenerReference<U>(SoftReference<U> reference, Consumer<U> action) {}

    public ChangeDetector(T model, Function<T, S> stateFunction) {
        this.model = model;
        this.stateFunction = stateFunction;
        this.currentState = stateFunction.apply(model);
    }

    public <U> void addListener(U listener, Consumer<U> action) {
        var listenerReference = new ListenerReference<>(new SoftReference<>(listener), action);
        listenerReferences.add(listenerReference);
    }

    public void notifyChange() {
        notifyChangeCausedBy(null);
    }

    public void notifyChangeCausedBy(Object callingListener) {
        S newState = stateFunction.apply(model);
        if (ModelStateManager.areDeepEqual(newState, currentState)) {
            return;
        }
        currentState = newState;
        for (ListenerReference<?> listenerReference: listenerReferences) {
            if (callingListener != null && listenerReference.reference().get() == callingListener) {
                continue;
            }
            callListener(listenerReference);
        }
        listenerReferences.removeIf(listenerReference -> listenerReference.reference.get() == null);

    }

    private static <U> void callListener(ListenerReference<U> listenerReference) {
        var target = listenerReference.reference.get();
        if (target == null) {
            return;
        }
        listenerReference.action.accept(target);
    }

    public S getCurrentState() {
        return currentState;
    }
}
