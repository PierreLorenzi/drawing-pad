package fr.alphonse.drawingpad.document.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class ModelCopier {

    private static final List<Class<?>> VALUE_CLASSES = List.of(Number.class, String.class, Enum.class);

    public static <T> T deepCopy(T model, Class<T> type) {
        return type.cast(copyModel(model));
    }

    private static Object copyModel(Object model) {

        if (model == null) {
            return null;
        }

        if (isModelValue(model)) {
            return model;
        }

        if (model instanceof List<?> list) {
            return list.stream()
                    .map(ModelCopier::copyModel)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        return copyModelWithFields(model);
    }

    private static boolean isModelValue(Object model) {
        Class<?> type = model.getClass();
        if (type.isRecord()) {
            return true;
        }
        return VALUE_CLASSES.stream().anyMatch(valueClass -> valueClass.isAssignableFrom(type));
    }

    private static Object readField(Field field, Object model) {
        try {
            field.setAccessible(true);
            return field.get(model);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object copyModelWithFields(Object model) {

        Class<?> modelClass = model.getClass();
        Object newModel = makeNewInstance(modelClass);

        Class<?> currentClass = modelClass;

        while (currentClass != null) {

            Field[] fields = currentClass.getDeclaredFields();
            for (Field field: fields) {

                if (!isFieldInState(field)) {
                    continue;
                }

                Object fieldValue = readField(field, model);
                Object newFieldValue = copyModel(fieldValue);
                changeFieldValue(newModel, field, newFieldValue);
            }

            currentClass = currentClass.getSuperclass();
        }

        return newModel;
    }

    private static Object makeNewInstance(Class<?> modelClass) {
        try {
            return modelClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isFieldInState(Field field) {
        return field.getAnnotation(JsonIgnore.class) == null;
    }

    private static void changeFieldValue(Object newModel, Field field, Object newFieldValue) {
        try {
            field.set(newModel, newFieldValue);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
